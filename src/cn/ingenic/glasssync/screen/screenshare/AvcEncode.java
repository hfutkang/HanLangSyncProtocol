package cn.ingenic.glasssync.screen.screenshare;

import android.content.Context;
import android.os.RemoteException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import android.util.Log;

import cn.ingenic.glasssync.R;    
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import java.io.InputStream;

import cn.ingenic.glasssync.screen.screenshare.ScreenModule;

public class AvcEncode {
    private static String TAG = "AvcEncode";
    private static final boolean DEBUG = true;
    private static final boolean TEST = false;
    private static final String MIME_TYPE = "video/avc"; // H.264 AVC encoding
    private static final String ENCODER_NAME = "OMX.google.h264.encoder";
    private static final int BIT_RATE = 2000000;            // 2Mbps
    private static final int FRAME_RATE = 25; // 30fps
    private static final int IFRAME_INTERVAL = 10; // 1 seconds between I-frames
    private static final int TIMEOUT_USEC = 5000; // Timeout value 10ms.
    private static final int FRAME_NUM = 200; 

    private int mWidth, mHeight;
    private Context mContext;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private BufferedOutputStream mOutputStream;
    private ScreenModule mScreenModule;
    private boolean isTransFinish = false;
    
    static {
       	System.loadLibrary("screencap_jni");
    }

    static native int get_width();
    static native int get_height();
    static native void get_picture();
    static native int get_frameData(byte[] data, int width, int height);

    public AvcEncode(Context context) {
	this.mContext = context;
	mScreenModule = ScreenModule.getInstance(context);
	Log.v(TAG, "new AvcEncode");
    }

    public void setPictureSize() {
	get_picture();
	mWidth = get_width();
	mHeight = get_height();
	if (DEBUG) Log.v(TAG, "mWidth = " + mWidth + " mHeight = " + mHeight);
    }


    public MediaFormat createMediaFormat() {
	if (DEBUG) Log.v(TAG, "createMediaFormat");
	MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

	format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
			  MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        return format;
    }

    public void configureMediaCodecEncoder() {
	if (DEBUG) Log.v(TAG, "configureMediaCodecEncoder"); 
	if (TEST) {
	    String path = "/data/data/raw.264";
	    File file = new File(path);
	    try {
		if (!file.exists()) {
		    if (DEBUG) Log.v(TAG, "create new one file");
		    file.createNewFile();
		}
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    
	    try {
		mOutputStream = new BufferedOutputStream(new FileOutputStream(file));
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}
	//mMediaCodec = MediaCodec.createByCodecName(ENCODER_NAME);
	mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
	if (mMediaCodec == null) {
	    if (DEBUG) Log.v(TAG, "MediaCodec create failed");
	}
	
        mMediaFormat = createMediaFormat();
	mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

	if (DEBUG) Log.v(TAG, "before start");
	mMediaCodec.start();
    }


    private void yuvtonv12(byte[] src, byte[] dst) {
	int frameSize = mWidth*mHeight;
	int i = 0, j = 0;
	
	for(i=0; i<frameSize; i++ )
	    dst[i] = src[i];
	
	for(i=0, j=0; i<frameSize/4; i++, j+=2)
	    dst[frameSize + j] = src[frameSize + i];

	for(i=0, j=1; i<frameSize/4; i++, j+=2)
	    dst[frameSize + j] = src[frameSize +  frameSize / 4 + i];
    }

    private byte[] getFramedataFromFb() {
	if (DEBUG) Log.v(TAG, "getFramedataFromFb");
	int frameSize = mWidth * mHeight * 3 / 2;
	byte[] frameData = new byte[frameSize];
	get_frameData(frameData, mWidth, mHeight);
	if (DEBUG) Log.v(TAG, "frameData.length = " + frameData.length);
	return frameData;
    }

    public void getFrameData() throws Exception {
	int frameSize = mWidth * mHeight * 3 / 2;
	if (DEBUG) Log.v(TAG, "getFrameData frameSize = " + frameSize);
	MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	ByteBuffer[] mInputBuffers = mMediaCodec.getInputBuffers();
	ByteBuffer[] mOutputBuffers = mMediaCodec.getOutputBuffers();

	try {
	    long presentationTimeUs = 0;
            int frameIndex = 0;
	    int outFrameNum = 0;
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;

	    mScreenModule.sendRequestData(true, mWidth, mHeight);

	    while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
		    if (DEBUG) Log.v(TAG, "inputBufIndex = " + inputBufIndex);
                    if (inputBufIndex >= 0) {
			byte[] frame = getFramedataFromFb();
			int bytesRead = frameSize;
		
			isTransFinish = mScreenModule.getFinishTag();
			if (!isTransFinish/*frameIndex == FRAME_NUM*/) {
			    if (DEBUG) Log.v(TAG, "+++InputEOS");
			    sawInputEOS = true;
			    bytesRead = 0;
			}

                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
                        mInputBuffers[inputBufIndex].rewind();

                        presentationTimeUs = (frameIndex * 1000000) / FRAME_RATE;
                        if (DEBUG) Log.d(TAG, "Encoding frame at index " + frameIndex);
                        mMediaCodec.queueInputBuffer(
                                inputBufIndex,
                                0,  // offset
                                bytesRead,  // size
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        frameIndex++;
                    }
                }

                int result = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (result >= 0) {
		    outFrameNum++;
                    int outputBufIndex = result;
                    byte[] buffer = new byte[mBufferInfo.size];
                    mOutputBuffers[outputBufIndex].rewind();
                    mOutputBuffers[outputBufIndex].get(buffer, 0, mBufferInfo.size);

		    if (DEBUG) Log.v(TAG, "mBufferInfo.size = " + mBufferInfo.size + " buffer.length = " +  buffer.length);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    } else {
			if (TEST) mOutputStream.write(buffer, 0, buffer.length);
			mScreenModule.sendData(buffer);
			mScreenModule.sendFrameNum(outFrameNum);
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufIndex,false);  // render
                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
                }
		if (DEBUG) Log.v(TAG, "outFrameNum = " + outFrameNum);
            }

	    mScreenModule.finishData(true);
            mMediaCodec.stop();
            mMediaCodec.release();
	}finally {
	    if (TEST) {
		if (mOutputStream != null) {
		    mOutputStream.flush();
		    mOutputStream.close();
		}
	    }
	}
	if (DEBUG) Log.v(TAG, "Encode end");
    }


    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }
}

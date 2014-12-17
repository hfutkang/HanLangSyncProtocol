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
    private static final boolean DEBUG = false;
    private static final boolean TEST = false;
    private static final boolean MTIMER = false;
    private static final String MIME_TYPE = "video/avc"; // H.264 AVC encoding
    private static final String ENCODER_NAME = "OMX.google.h264.encoder";
    private static final int BIT_RATE = 2000000;            // 2Mbps
    private static final int FRAME_RATE = 25; // 30fps
    private static final int IFRAME_INTERVAL = 10; // 1 seconds between I-frames
    private static final int TIMEOUT_USEC = 5000;//5000; // Timeout value 10ms.
    private static final int FRAME_NUM = 200; 

    private int mWidth, mHeight;
    private int outFrameNum, inFrameNum;
    private long start;
    private long time, time1;
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
    static native void get_frameData(byte[] data);

    public AvcEncode(Context context) {
	this.mContext = context;
	this.inFrameNum = 0;
	this.outFrameNum = 0;
	this.time =0;
	this.time1 = 0;
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


    public void getFrameData() throws Exception {
	int frameSize = mWidth * mHeight * 3 / 2;
	if (DEBUG) Log.v(TAG, "getFrameData frameSize = " + frameSize);
	MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	ByteBuffer[] mInputBuffers = mMediaCodec.getInputBuffers();
	ByteBuffer[] mOutputBuffers = mMediaCodec.getOutputBuffers();
	int lastInputBufIndex = -1;

	byte[][] frameData = new byte[6][frameSize];
	byte[] buffer1 = null;
	byte[] buffer2 = null;

	try {
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
	    mScreenModule.sendRequestData(true, mWidth, mHeight);

	    while (!sawOutputEOS) {
                if (!sawInputEOS) {

                    int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
		    if (inputBufIndex >= 0) {
			if (MTIMER) start = System.currentTimeMillis();
			inFrameNum++;
			byte[] frame = frameData[(inFrameNum-1) % 6];
			get_frameData(frame);

			isTransFinish = mScreenModule.getFinishTag();
			if (!isTransFinish) {
			    if (DEBUG) Log.v(TAG, "+++InputEOS");
			    sawInputEOS = true;
			    frameSize = 0;
			}

			Log.v(TAG, "inFrameNum =" + inFrameNum);
                        mInputBuffers[inputBufIndex].clear();
                        mInputBuffers[inputBufIndex].put(frame);
			mInputBuffers[inputBufIndex].rewind();

                        mMediaCodec.queueInputBuffer(inputBufIndex, 0, frameSize, 0,
				sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

			if (MTIMER) {
			    long elapsed11 = System.currentTimeMillis() - start;
			    time += elapsed11;
			    Log.v(TAG, "frame: "+ inFrameNum + " getFramedataFromFb costtime: " + elapsed11 + "ms");
			    Log.v(TAG, "getFramedataFromFb average costtime: " + time/inFrameNum + "ms");
			}
                    }
                }

                int outputBufIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
		if (outputBufIndex >= 0) {
		    if (MTIMER) start = System.currentTimeMillis();
		    if ((outFrameNum % 2) == 0) {
			buffer1 = new byte[mBufferInfo.size];
			mOutputBuffers[outputBufIndex].rewind();
			mOutputBuffers[outputBufIndex].get(buffer1, 0, mBufferInfo.size);
		    }else{
			buffer2 = new byte[mBufferInfo.size];
			mOutputBuffers[outputBufIndex].rewind();
			mOutputBuffers[outputBufIndex].get(buffer2, 0, mBufferInfo.size);
		    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    } else {
			if ((outFrameNum % 2) == 1) {
			    byte[] buffer3 = new byte[buffer1.length + buffer2.length];
			    System.arraycopy(buffer1, 0, buffer3, 0, buffer1.length);
			    System.arraycopy(buffer2, 0, buffer3, buffer1.length, buffer2.length);
			    mScreenModule.sendData(buffer3, buffer1.length);
			}
                    }
		    outFrameNum++;
                    mMediaCodec.releaseOutputBuffer(outputBufIndex,false);  // render

		    if (MTIMER) {
			long elapsed22 = System.currentTimeMillis() - start;
			time1 += elapsed22;
			Log.v(TAG, "output send Data  costtime: " + elapsed22 + "ms");
			Log.v(TAG, "output send Data  average costtime: " + time1/outFrameNum + "ms");
		    }

		    Thread.sleep(20);

                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
		}
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

}

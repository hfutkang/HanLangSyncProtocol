package cn.ingenic.glasssync.screen.screenshare;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

import cn.ingenic.glasssync.screen.screenshare.AvcEncode;

public class ScreenModule extends SyncModule {
    private static final String TAG = "ScreenModule";
    private boolean DEBUG = false;
    
    private Context mContext;
    private static AvcEncode mAvcEncode;
    private static BluetoothAdapter mAdapter;
    private static ScreenModule sInstance;

    private static final String SCREEN_NAME = "screen_module";
    private static final String SCREEN_SHARE = "screen_share";
    private static final String TRANSPORT_SCREEN_CMD = "screen_cmd";    
    private  static final String GET_SCREEN_CMD = "get_screen_cmd";    
    private static final String SEND_FRAME = "video_stream";
    private static final String END_FRAME = "end_stream";
    private static final String FRAME_WIDTH = "video_width";
    private static final String FRAME_HEIGHT = "video_height";
    private static final String FRAME_LENGTH = "first_frame_length";

    private static final int TRANSPORT_DATA_READY = 0;
    private static final int TRANSPORT_DATA_IN = 1;
    private static final int TRANSPORT_DATA_FINISH = 2;

    public boolean mBTState = true;
    public boolean isTransData = false;

    private ScreenModule(Context context){
	super(SCREEN_NAME, context);
	mContext = context;
    }

    public static ScreenModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new ScreenModule(c);
	Log.v(TAG, "new ScreenModule");
	return sInstance;
    }


    @Override
    protected void onCreate() {
    }

    @Override
    protected void onConnectionStateChanged(boolean connect) {
	Log.v(TAG, "onConnectionStateChanged connect = " + connect);
	mBTState = connect; 
    }


    @Override
    protected void onRetrive(SyncData data) {
	if (DEBUG) Log.v(TAG, "---onRetrive");
	int choose = 0;
	choose = data.getInt(SCREEN_SHARE);
	if (DEBUG) Log.v(TAG, "choose = " + choose);

	isTransData = data.getBoolean(TRANSPORT_SCREEN_CMD, false);
        Log.v(TAG, "isTransData = " + isTransData);

	if (isTransData) {
	    try {
		if (DEBUG) Log.v(TAG,"begin start data");
		if (mAvcEncode == null) {
		    Log.v(TAG, "need new one mAvcEncode");
		    mAvcEncode = new AvcEncode(mContext);
		}
		mAvcEncode.setPictureSize();
		mAvcEncode.configureMediaCodecEncoder();
		startTransData();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}else{
	    if (mAvcEncode != null)  {
		mAvcEncode = null;
	    }	
	    if (DEBUG) Log.v(TAG, "do not send screen data");
	}
    }

    private void startTransData() {
	if (DEBUG) Log.v(TAG, "startTransData");
	Thread thread = new DataTransThread();
	thread.start();
    }

    class DataTransThread extends Thread {
	private Object mLock = new Object();
	@Override
	public void run() {
	    super.run();
	    if (DEBUG) Log.v(TAG, "DataTransThread");

	    mAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mAdapter == null || (!mAdapter.isEnabled()) ) {
		Log.v(TAG, "---bluetooth is no exist or no open");
		return;
	    }else{
		Log.v(TAG, "---bluetooth exist");
	    }

	    try {
		mAvcEncode.getFrameData();
	    }catch(Exception e){
		e.printStackTrace();
	    }

	    if (mAvcEncode != null)  {
		mAvcEncode = null;
	    }	

	}
    }

    
    public boolean getFinishTag() {
	if (DEBUG) Log.v(TAG, "isTransData = " + isTransData + " mBTState =" + mBTState);
	return (isTransData && mBTState);
    }

    public void sendData(byte[] frame, int length) {
	if (DEBUG) Log.v(TAG, "sendData byte[]");

	SyncData data = new SyncData();
	data.putInt(SCREEN_SHARE, TRANSPORT_DATA_IN);
	data.putInt(FRAME_LENGTH, length);
	data.putByteArray(SEND_FRAME, frame);

	try{
	    send(data);
	}catch (SyncException e){
	    Log.e(TAG, "---send frame data failed:" + e);
	    Toast.makeText(mContext,  "蓝牙传送失败！", Toast.LENGTH_LONG).show();
	}
    }

    public void sendRequestData(boolean bool, int width, int height) {
	if (DEBUG) Log.v(TAG, "sendRequestData");
	SyncData data = new SyncData();

	data.putInt(SCREEN_SHARE, TRANSPORT_DATA_READY);
	data.putBoolean(GET_SCREEN_CMD, bool);
	data.putInt(FRAME_WIDTH, width);
	data.putInt(FRAME_HEIGHT, height);
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send cmd failed:" + e);
	}
    }


    public void finishData(boolean bool) {
	if (DEBUG) Log.v(TAG, "finishData");
	SyncData data = new SyncData();
	data.putInt(SCREEN_SHARE, TRANSPORT_DATA_FINISH);
	data.putBoolean(END_FRAME, bool);
	try {
	    if (DEBUG)
		Log.e(TAG, "---send data " + bool);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send finish signal failed:" + e);
	}
    }
}

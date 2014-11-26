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

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.screen.screenshare.AvcEncode;

public class ScreenModule extends SyncModule {
    private static final String TAG = "ScreenModule";
    private boolean DEBUG = true;
    
    private Context mContext;
    private AvcEncode mAvcEncode;
    public static ScreenModule sInstance;

    private static final String SCREEN_NAME = "screen_module";
    public static final String SCREEN_SHARE = "screen_share";
    public static final String TRANSPORT_SCREEN_CMD = "screen_cmd";    
    public static final String GET_SCREEN_CMD = "get_screen_cmd";    
    public static final String SEND_FRAME = "video_stream";
    public static final String END_FRAME = "end_stream";
    public static final String SEND_FRAME_NUM = "video_frame_num";
    public static final String FRAME_WIDTH = "video_width";
    public static final String FRAME_HEIGHT = "video_height";

    public static final int TRANSPORT_DATA_READY = 0;
    public static final int TRANSPORT_DATA_IN = 1;
    public static final int TRANSPORT_DATA_FINISH = 2;
    public static final int TRANSPORT_DATA_NUM = 3;

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
    protected void onRetrive(SyncData data) {
	Log.v(TAG, "---onRetrive");
	int choose = 0;
	choose = data.getInt(SCREEN_SHARE);
	if (DEBUG) Log.v(TAG, "choose = " + choose);

	isTransData = data.getBoolean(TRANSPORT_SCREEN_CMD, false);
	if (DEBUG) Log.v(TAG, "isTransData = " + isTransData);

	if (isTransData) {
	    try {
		if (DEBUG) Log.v(TAG,"begin start data");
		mAvcEncode = new AvcEncode(mContext);
		mAvcEncode.setPictureSize();
		mAvcEncode.configureMediaCodecEncoder();
		startTransData();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}else{
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
	    try {
		mAvcEncode.getFrameData();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}
    }

    
    public boolean getFinishTag() {
	return isTransData;
    }

    public boolean sendData(byte[] frame) {
	if (DEBUG) Log.v(TAG, "sendData byte[]");
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	if (adapter == null || (!adapter.isEnabled()) ) {
	    Log.e(TAG, "---bluetooth is no exist or no open");
	    return false;
	}
	SyncData data = new SyncData();
	data.putInt(SCREEN_SHARE, TRANSPORT_DATA_IN);
	data.putByteArray(SEND_FRAME, frame);

	try{
	    send(data);
	}catch (SyncException e){
	    Log.e(TAG, "---send frame data failed:" + e);
	    Toast.makeText(mContext,  "蓝牙传送失败！", Toast.LENGTH_LONG).show();
	}
	return true;
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


    public void sendFrameNum(int frameNum) {
	if (DEBUG) Log.v(TAG, "sendFrameNum");
	SyncData data = new SyncData();

	data.putInt(SCREEN_SHARE, TRANSPORT_DATA_NUM);
	data.putInt(SEND_FRAME_NUM, frameNum);
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send frame index failed:" + e);
	}
    }

}

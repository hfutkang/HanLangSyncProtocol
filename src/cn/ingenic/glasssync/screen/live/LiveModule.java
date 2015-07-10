package cn.ingenic.glasssync.screen.live;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.net.Uri;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

import android.content.Context;
import android.content.Intent;

import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import java.net.UnknownHostException;

import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;


public class LiveModule extends SyncModule {
    private static final String TAG = "LiveModule";
    private boolean DEBUG = true;

    public static final String NEED_OPEN_PACKET_NAME = "com.ingenic.glass.camera";
    public static final String NEED_OPEN_PACKET_CLASS_NAME = "com.ingenic.glass.camera.live.CameraLive";

    private static final String LIVE_QUIT = "com.ingenic.glass.camera.live.LIVE_QUIT";

    public static final String LIVE_NAME = "live_module";
    public static final String LIVE_SHARE = "live_share";
    public static final String LIVE_TRANSPORT_CMD = "live_transport_cmd";
    public static final String LIVE_QUIT_CMD = "live_quit_cmd";
    public static final String LIVE_RTSP_URL = "live_rtsp_url";

    public static final String LIVE_WIFI_CONNECTED = "live_wifi_connected";
    public static final String LIVE_WIFI_UNCONNECTED = "live_wifi_unconnected";

    public static final String LIVE_CAMERA_OPENED = "live_camera_opened";
    public static final String LIVE_CAMERA_NOT_OPENED = "live_camera_not_opened";
    
    
    public static final int TRANSPORT_WIFI_CONNECTED = 0;
    public static final int TRANSPORT_WIFI_UNCONNECTED = 1;
    public static final int TRANSPORT_CAMERA_OPENED = 2;
    public static final int TRANSPORT_CAMERA_NOT_OPENED = 3;

    
    private Context mContext;
    private static LiveModule sInstance;
    private boolean isTransData = false;
    private boolean isLiveQuit = false;
    private String mIPAddress = null;
    private Handler mDelayHandler;


    private LiveModule(Context context){
	super(LIVE_NAME, context);
	mContext = context;
    }

    public static LiveModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new LiveModule(c);
	Log.e(TAG, "new LiveModule");
	return sInstance;
    }


    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "---onRetrive");
	int choose = 0;
	choose = data.getInt(LIVE_SHARE);
	if (DEBUG) Log.e(TAG, "choose = " + choose);

	if (mDelayHandler == null)
	    mDelayHandler = new Handler();

	isTransData = data.getBoolean(LIVE_TRANSPORT_CMD, false);
	Log.e(TAG, "isTransData = " + isTransData);
	if (isTransData) {
	    WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
	    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	    if (wifiInfo != null) {
	    	int ipAddress = wifiInfo.getIpAddress();
	    	mIPAddress = String.format("%d.%d.%d.%d",
	    				   (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
	    				   (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)).toString();
	    }else{
	    	Log.e(TAG, "WIFI not connected, connect first");
	    	sendWifiStatus(false, null);
	    	return;
	    }

	    if (DEBUG) Log.e(TAG, "Glass's wifi ip =" + mIPAddress);
	    if (mIPAddress != null) {
	    	String url = "rtsp://" + mIPAddress + ":8554" + "/recorderLive";
	    	sendWifiStatus(true, url);
	    }else{
	    	Log.e(TAG, "WIFI ip is not valid");
	    	sendWifiStatus(false, null);
	    	return;
	    }

	    ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE); 
	    List<RunningTaskInfo> list = am.getRunningTasks(100); 
	    boolean isAppRunning = false; 
	    for (RunningTaskInfo info : list) { 
	    	//if (DEBUG) Log.e(TAG, "info.topActivity = " + info.topActivity.getClassName());
	    	//if (DEBUG) Log.e(TAG, "info.baseActivity = " + info.baseActivity.getClassName());
	    	if (info.topActivity.getClassName().equals(NEED_OPEN_PACKET_CLASS_NAME) || info.baseActivity.getClassName().equals(NEED_OPEN_PACKET_CLASS_NAME)) { 
	    	    isAppRunning = true; 
	    	    break; 
	    	} 
	    } 

	    if (!isAppRunning) {
	    	ComponentName componentName = new ComponentName(NEED_OPEN_PACKET_NAME, NEED_OPEN_PACKET_CLASS_NAME);
	    	startApp(componentName, null);
	    }else{
	    	if (DEBUG) Log.e(TAG, "Application is aleady running");
		// is running or is finishing
		stopApp();
	    	sendCameraStatus(false);
	    }
	}

	isLiveQuit = data.getBoolean(LIVE_QUIT_CMD, false);
	Log.e(TAG, "isLiveQuit = " + isLiveQuit);
	if (isLiveQuit)
		stopApp();
    }

    private void startApp(ComponentName com, String param) {
	Log.e(TAG, "startApp");
	if (com != null) {
	    PackageInfo packageInfo;
	    try {
		packageInfo = mContext.getPackageManager().getPackageInfo(com.getPackageName(), 0);
	    } catch (NameNotFoundException e) {
		packageInfo = null;
		Log.e(TAG, "uninstalled");
		Toast.makeText(mContext, "uninstalled package", Toast.LENGTH_SHORT).show();
		e.printStackTrace();
	    }

	    try {
		Log.e(TAG, "start app");
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setComponent(com);
		if (param != null) {
		    Bundle bundle = new Bundle(); 
		    bundle.putString("flag", param); 
		    intent.putExtras(bundle); 
		}

		mDelayHandler.postDelayed(new DelayTimer(), 3000);
		mContext.startActivity(intent);
	    } catch (Exception e) {
		Toast.makeText(mContext, "Unexpected error", Toast.LENGTH_SHORT).show();
	    }
	}
    }

    private void stopApp() {
	    final Intent intent = new Intent(LIVE_QUIT, Uri.parse("file://LiveModule"));
	    intent.setPackage(NEED_OPEN_PACKET_NAME);
	    Log.e(TAG, "sendStorageIntent " + intent);
	    mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    class DelayTimer implements Runnable {
	public void run() {
	    Log.e(TAG, "DelayTimer++");
	    sendCameraStatus(true);
	}
    }

    public void sendWifiStatus(boolean bool,  String url) {
	if (DEBUG) Log.e(TAG, "sendWifiStatus url=" + url);
	SyncData data = new SyncData();

	if (bool) {
	    data.putInt(LIVE_SHARE, TRANSPORT_WIFI_CONNECTED);
	    data.putString(LIVE_RTSP_URL, url);
	}else {
	    Log.e(TAG, "TRANSPORT_WIFI_UNCONNECTED");
	    data.putInt(LIVE_SHARE, TRANSPORT_WIFI_UNCONNECTED);
	}

	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---sendWifiConnected:" + e);
	}
    }



    public void sendCameraStatus(boolean bool) {
	if (DEBUG) Log.e(TAG, "sendCameraStatus bool = " + bool);
	SyncData data = new SyncData();

	if (bool) {
	    data.putInt(LIVE_SHARE, TRANSPORT_CAMERA_OPENED);
	}else{
	    data.putInt(LIVE_SHARE, TRANSPORT_CAMERA_NOT_OPENED);
	}

	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---sendCameraStatus:" + e);
	}
    }

}

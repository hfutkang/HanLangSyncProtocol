package cn.ingenic.glasssync.camera;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.Transaction;

import android.util.Log;

import java.util.UUID;

public class CameraModule extends Module {

    public static final String TAG = "CameraModule";
    public static final boolean V = true;

    public static final String CAMERA = "CAMERA";
    private static CameraClient mClient;

    public CameraModule() {
	super(CAMERA);
    }

    @Override
    protected Transaction createTransaction() {
	return new CameraTransaction();
    }

    @Override
    public OnChannelCallBack getChannelCallBack(UUID uuid) {
	if (uuid.compareTo(CameraBase.PREVIEW_UUID) == 0  && mClient != null){
	    return mClient;
	}
	return null;
    }

    public static void setChannelCallBack(CameraClient client){
	mClient = client;
    }

    @Override
    public void onConnectivityStateChange(boolean connected) {
	Log.i(TAG,"Watch: connetivity state change: connection is "+connected);
	if (!connected && mClient != null){
	    Log.i(TAG,"will disconnectCameraFromBT");
	    mClient.disconnectCameraFromBT();
	}
    }
}

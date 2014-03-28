package cn.ingenic.glasssync.camera;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag.Client;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;

public class CameraTransaction extends Transaction {
    public static final String TAG = "CameraTransaction >> watch";

    //the requests which will be send from watch to phone. 
    public static final int OPEN_CAMERA_REQUEST = 0;
    public static final int TAKE_PICTURE_REQUEST = 1;
    public static final int SWITCH_CAMERA_REQUEST = 2;
    public static final int EXIT_CAMERA_REQUEST = 3;

    //the responses from phone.
    public static final int OPEN_RESULT_RESPONSE = 0;
    public static final int TAKE_RESULT_RESPONSE = 1;
    public static final int EXIT_CAMERA_RESPONSE = 2;

	//the content of open result.
	public static final int OPEN_RESULT_SUCCESS = 0;
	public static final int OPEN_RESULT_FAILED = 1;
	public static final int OPEN_RESULT_FAILED_POWER = 2;
	public static final int OPEN_RESULT_FAILED_SENSOR = 3;
    public static final int OPEN_RESULT_FAILED_CHANNEL = 4;
	public static final int OPEN_RESULT_FAILED_DISCONN = 5;
	public static final int OPEN_RESULT_FAILED_TIMEOUT = 6;

    private static CameraClient mClient;

    public static void setCameraClient(CameraClient c){
	mClient = c;
    }
    
    public static void reset(){
	mClient = null;
    }

    @Override
    public void onStart(ArrayList<Projo> datas) {
        super.onStart(datas);
        
	Projo data = datas.get(0);
	if (data == null) {
	    Client.e("datas[0] is null.");
	    return;
	}
	
	// watch client
	if (mClient == null || mClient.disConnectState()) {
	    return;
	}
	Integer title = (Integer) data.get(CameraColumn.phoneResponseState);
	Log.i(TAG, "receive a response from phone---------------------------------" + title.intValue());
	Projo content = null;
	if (datas.size() > 1){
	    content = datas.get(1);
	    if (content == null) {
		Client.e("datas[1] is null.");
		return;
	    }
	}
	switch (title.intValue()){
	case OPEN_RESULT_RESPONSE:
	    Integer openResult = (Integer) content.get(CameraColumn.openCameraResult);
	    mClient.showOpenedResult(openResult.intValue());
	    break;
	case TAKE_RESULT_RESPONSE:
	    Boolean takeResult = (Boolean) content.get(CameraColumn.takePictureResult);
	    mClient.takePictureResult(takeResult.booleanValue());
	    if (takeResult.booleanValue()){
		Projo picData = datas.get(2);
		byte[] picture = (byte[]) picData.get(CameraColumn.pictureData);
		mClient.setPictureData(picture);
		
		Projo picPath = datas.get(3);
		String mServerSavePath = (String) picPath.get(CameraColumn.picturePath);
	    }
	    break;
	case EXIT_CAMERA_RESPONSE:
	    mClient.disconnectCameraFromPhone();
	    break;
	default :
	    
	}
    }
}

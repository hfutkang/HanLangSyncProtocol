package cn.ingenic.glasssync.services;

import cn.ingenic.glasssync.services.SyncData;
import android.os.ParcelUuid;

interface IModuleCallback {
	void onInit();

	void onCreate();

	void onClear(String address);
	
	void onConnectivityStateChange(boolean connected);
	
	void onModeChanged(int mode);
	
	void onRetrive(in SyncData data);
	
	void onFileSendComplete(String fileName, boolean success);
	
	void onFileRetriveComplete(String fileName, boolean success);
	
	void onSendCallback(long sort, int result);
	
	void onChannelCreateComplete(in ParcelUuid uuid, boolean success, boolean local);
	
	void onChannelRetrive(in ParcelUuid uuid, in SyncData data);
	
	void onChannelDestroy(in ParcelUuid uuid);
}
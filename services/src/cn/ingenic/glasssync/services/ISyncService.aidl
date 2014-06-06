package cn.ingenic.glasssync.services;

import cn.ingenic.glasssync.services.IModuleCallback;
import cn.ingenic.glasssync.services.SyncData;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;

interface ISyncService {
	boolean isConnected();
	
	String getLockedAddress();

	boolean registModule(String name, IModuleCallback callback);
	
	boolean send(String module, in SyncData bundle);
	
	void sendOnChannel(String module, in SyncData data, in ParcelUuid uuid);
	
	boolean sendFile(String module, in ParcelFileDescriptor des, String name, int length);

	boolean sendFileByPath(String module, in ParcelFileDescriptor des, String name, int length, String path);
	
	void createChannel(String module, in ParcelUuid uuid);
	
	void destroyChannel(String module, in ParcelUuid uuid);
}
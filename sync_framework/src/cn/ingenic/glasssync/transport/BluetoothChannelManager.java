package cn.ingenic.glasssync.transport;

import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.ProjoList;

interface BluetoothChannelManager {
	static final int BASE = 0;
	static final int SETUP_MSG = BASE + 1;
	static final int CLIENT_CLOSE_MSG = BASE + 2;
	static final int RETRIVE_MSG = BASE + 3;
	static final int CLEAR_MSG = BASE + 4;
	static final int CREATE_CHANNLE_MSG = BASE + 5;
	static final int DESTORY_CHANNEL_MSG = BASE + 6;
	
	static final int NEW_CLIENT_MSG = BASE + 7;
	
	void prepare(String address);
	
	void send(ProjoList projoList, boolean isSync);
	
	BluetoothChannel getChannel(UUID uuid);
	
//	boolean hasAvailiableChannel();
	
	boolean listenChannel(UUID uuid, OnChannelCallBack callback);
	
	void createChannel(UUID uuid, OnChannelCallBack callback);
	
	void destoryChannle(UUID uuid);
}

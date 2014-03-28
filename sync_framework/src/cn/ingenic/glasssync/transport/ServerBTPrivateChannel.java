package cn.ingenic.glasssync.transport;

import java.io.IOException;
import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.ProjoList;

import android.os.Handler;

public class ServerBTPrivateChannel extends BluetoothServer {
	
	private final OnChannelCallBack mCallback;

	public ServerBTPrivateChannel(Handler handler, String name, UUID uuid, OnChannelCallBack callback)
			throws IOException {
		super(handler, name, uuid);
		mCallback = callback; 
	}

	@Override
	public void onRetrive(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		mCallback.onRetrive(projoList);
	}

	@Override
	public void close() {
		boolean destory = !mClosed;
		super.close();
		if (destory) {
			mCallback.onDestory();
		}
	}
}

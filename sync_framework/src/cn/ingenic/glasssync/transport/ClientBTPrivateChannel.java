package cn.ingenic.glasssync.transport;

import java.io.IOException;
import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.ProjoList;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

class ClientBTPrivateChannel extends BluetoothClient {
	
	private final OnChannelCallBack mCallback;

	ClientBTPrivateChannel(BluetoothDevice device, UUID uuid, Handler handler, OnChannelCallBack callback)
			throws IOException {
		super(device, uuid, handler);
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

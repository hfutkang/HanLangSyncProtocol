package cn.ingenic.glasssync;

import java.util.UUID;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.ILocalBinder;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.RemoteChannelManagerService;
import cn.ingenic.glasssync.transport.TransportManager;

public class RemoteChannelManagerImpl implements ILocalBinder, RemoteChannelManagerService {
	
	private static final String TAG = "RemoteChannelManagerImpl";
	private TransportManager mManager;
	
	private static final int BASE = 0;
	private static final int RemoteChannelManagerImpl_listenChannel = BASE + 1;
	private static final int RemoteChannelManagerImpl_sendClearMessage = BASE + 2;

	private static final int MSG_CLEAR = 100;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_CLEAR:
				DefaultSyncManager mgr = DefaultSyncManager.getDefault();
				mgr.setLockedAddress("");
				break;
			}
		}
		
	};
	
	public RemoteChannelManagerImpl() {
		mManager = TransportManager.getDefault();
	}
	
	@Override
	public void sendClearMessage() {
		mHandler.sendEmptyMessage(MSG_CLEAR);
	}

	@Override
	public boolean listenChannel(String module, UUID uuid) {
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		Module m = manager.getModule(module);
		if (m != null) {
			OnChannelCallBack callback = m.getChannelCallBack(uuid);
			if (callback != null) {
				return mManager.listenChannel(uuid, callback);
			} else {
				Log.e(TAG, "Module :" + module + " not support onChannelListen");
			}
		} else {
			Log.e(TAG, "no Module named " + module);
		}
		
		return false;

	}

	@Override
	public RemoteParcel onTransact(int code, RemoteParcel request) {
		RemoteParcel reply = new RemoteParcel();
		switch (code) {
		case RemoteChannelManagerImpl_listenChannel:
			String module = request.readString();
			UUID uuid = (UUID) request.readObject();
			
			boolean result = listenChannel(module, uuid);
			reply.writeBoolean(result);
			break;
			
		case RemoteChannelManagerImpl_sendClearMessage:
			sendClearMessage();
			break;
		}
		return reply;
	}
	
	public static RemoteChannelManagerService asRemoteInterface(IRemoteBinder binder) {
		return new RemoteChannelManagerProxy(binder);
	}

	public static class RemoteChannelManagerProxy implements RemoteChannelManagerService {
		
		private final IRemoteBinder mRemoteBinder;
		
		private RemoteChannelManagerProxy(IRemoteBinder remoteBinder) {
			mRemoteBinder = remoteBinder;
		}

		@Override
		public boolean listenChannel(String module, UUID uuid) {
			RemoteParcel request = new RemoteParcel();
			
			request.writeString(module);
			request.wtrieObject(uuid);
			
			try {
				RemoteParcel reply = mRemoteBinder.transact(RemoteChannelManagerImpl_listenChannel, request);
				boolean result = reply.readBoolean();
				return result;
			} catch (RemoteBinderException e) {
				Log.e(LogTag.APP, "RemoteBinderException occurs in listenChannel(" + module + ")");
				return false;
			}
		}

		@Override
		public void sendClearMessage() {
			RemoteParcel request = new RemoteParcel();
			
			try {
				mRemoteBinder.transact(RemoteChannelManagerImpl_sendClearMessage, request);
			} catch (RemoteBinderException e) {
				Log.e(LogTag.APP, "RemoteBinderException occurs in sendClearMessage");
			}
		}
	}
}

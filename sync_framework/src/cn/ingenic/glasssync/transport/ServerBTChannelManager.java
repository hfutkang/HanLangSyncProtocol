package cn.ingenic.glasssync.transport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.transport.ClientBTChannelManager.ChannelCreateData;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ServerBTChannelManager implements BluetoothChannelManager {
	private static final String TAG = "<SBM>";
	static final String SETUP= "setup";

	private final Handler mTransportHandler;
	private MyHandler mHandler;
	private Executor mExecutor;
	private final UUID mCustomUUID;
	private final UUID mServiceUUID;
	private Map<UUID, BluetoothChannel> mChannelMap;
//	private final Context mContext;

	ServerBTChannelManager(Handler handler, Executor executor, Context context) {
//		mContext = context;
		mTransportHandler = handler;
		Enviroment env = Enviroment.getDefault();
		mCustomUUID = env.getUUID(BluetoothChannel.CUSTOM, false);
		mServiceUUID = env.getUUID(BluetoothChannel.SERVICE, false);
		mExecutor = executor;
		mChannelMap = new HashMap<UUID, BluetoothChannel>();

		HandlerThread thread = new HandlerThread("ClientBTChannelManager");
		thread.start();

		mHandler = new MyHandler(thread.getLooper());
	}

	@Override
	public void prepare(String address) {
		log("ServerBTChannelManager prepare:" + address);
		if (SETUP.equals(address)) {
			Message msg = mHandler.obtainMessage(SETUP_MSG);
			msg.obj = address;
			msg.sendToTarget();
		} else {
			Message msg = mHandler.obtainMessage(CLEAR_MSG);
			msg.sendToTarget();
		}
	}

	@Override
	public BluetoothChannel getChannel(UUID uuid) {
		return mChannelMap.get(uuid);
	}

	class MyHandler extends Handler {

		MyHandler(Looper looper) {
			super(looper);
		}
		
		private String convertMessage(int msg) {
			switch (msg) {
			case SETUP_MSG:
				return "SETUP_MSG";
			case NEW_CLIENT_MSG:
				return "NEW_CLIENT_MSG";
			case RETRIVE_MSG:
				return "RETRIVE_MSG";
			case CLIENT_CLOSE_MSG:
				return "CLIENT_CLOSE_MSG";
			case CLEAR_MSG:
				return "CLEAR_MSG";
			case DESTORY_CHANNEL_MSG:
				return "DESTORY_CHANNEL_MSG";
			}
			return "";
		}

		private void listenUp() {
			if (!mChannelMap.isEmpty()) {
				logw("channelMap not empty in SETUP_MSG, clear it.");
//				for (BluetoothChannel channle : mChannelMap.values()) {
//					channle.close();
//				}
//				mChannelMap.clear();
				return;
			}
			
			BluetoothServer custom = null;
			try {
				custom = new BluetoothServer(mHandler,
						BluetoothChannel.CUSTOM_NAME, mCustomUUID);
				logi("CUSTOM server listen SUCCESS in SETUP_MSG");

				BluetoothServer service = new BluetoothServiceServer(
						mHandler, BluetoothChannel.SERVICE_NAME, mServiceUUID);
				logi("SERVER server listen SUCCESS in SETUP_MSG");
				mExecutor.execute(custom);
				mExecutor.execute(service);
				
				mChannelMap.put(mCustomUUID, custom);
				mChannelMap.put(mServiceUUID, service);
			} catch (IOException e) {
				loge("Server listen error in SETUP_MSG:" + e.getMessage());
				LogTag.printExp(TAG, e);
				
				if (custom != null) {
					custom.close();
				}
				
				clearChannel();
			}
		}
		
		private void clearChannel() {
			for (BluetoothChannel channle : mChannelMap.values()) {
				channle.close();
			}
			mChannelMap.clear();
			notifyStateChanged(TransportStateMachine.S_IDLE);
		}
		
		Runnable mmRunnable = new Runnable() {
			
			@Override
			public void run() {
				logw("another channel can not be ready in 3s, run clear msg.");
				clearChannel();
			}
			
		};
		
		@Override
		public void handleMessage(Message msg) {
			logv("handleMessage:" + convertMessage(msg.what));
			Enviroment env = Enviroment.getDefault();
			switch (msg.what) {
			case SETUP_MSG: {
				listenUp();
				break;
			}

			case NEW_CLIENT_MSG: {
				BluetoothServer server = (BluetoothServer) msg.obj;
				if (!env.isMainChannel(server.getUUID(), false)) {
					log("not main channel, ingore it");
					break;
				}
				
				BluetoothChannel otherOne = env.getAnotherMainChannel(server.getUUID(), false, mChannelMap);
				if (otherOne != null && otherOne.isAvailiable()) {
					removeCallbacks(mmRunnable);
					
					synchronized(BluetoothServer.sReadLock) {
						log("ReadLock notifyAll");
						BluetoothServer.sReadLock.notifyAll();
					}
					notifyStateChanged(TransportStateMachine.S_CONNECTED);
				} else {
					log("wait another new client.");
					postDelayed(mmRunnable, 3000);
				}
				break;
			}

			case RETRIVE_MSG: {
				Message message = mTransportHandler
						.obtainMessage(TransportStateMachine.MSG_S_RETRIVE);
				message.obj = msg.obj;
				message.sendToTarget();
				break;
			}
				
			case CLEAR_MSG: {
				clearChannel();
				break;
			}
				
			case CLIENT_CLOSE_MSG: {
				BluetoothChannel channel = (BluetoothChannel) msg.obj;
				UUID uuid = channel.getUUID();
				logv("CLIENT_CLOSE_MSG on server:" + uuid);
				if (env.isMainChannel(channel.getUUID(), false)) {
					mChannelMap.remove(uuid);
					BluetoothChannel otherOne = env.getAnotherMainChannel(channel.getUUID(), false, mChannelMap);
					if (otherOne == null) {
						clearChannel();
					} else {
						logv("wait another CLIENT_CLOSE_MSG");
					}
				} 
				break;
			}
			
			case DESTORY_CHANNEL_MSG: {
				UUID uuid = (UUID) msg.obj;
				BluetoothChannel privateChannel = mChannelMap.get(uuid);
				if (privateChannel != null) {
					privateChannel.close();
					mChannelMap.remove(uuid);
				}
				break;
			}
			
			case CREATE_CHANNLE_MSG:
				ChannelCreateData data = (ChannelCreateData) msg.obj;
				DefaultSyncManager manager = DefaultSyncManager.getDefault();
				String s = manager.getLockedAddress();
				if (BluetoothAdapter.checkBluetoothAddress(s)) {
					BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
					adapter.cancelDiscovery();
					BluetoothDevice d = adapter.getRemoteDevice(s);
					try {
						BluetoothChannel privateChannel = 
								new ClientBTPrivateChannel(d, data.uuid, mHandler, data.callback);
						mChannelMap.put(data.uuid, privateChannel);
						data.callback.onCreateComplete(true, true);
					} catch (IOException e) {
						data.callback.onCreateComplete(false, true);
						e.printStackTrace();
					}
				} else {
					logw("create channel error in ClientBTChannelManager because of invalid address");
				}
				break;
			}
		}
		
		private void notifyStateChanged(int state) {
			Message msg = mTransportHandler
					.obtainMessage(TransportStateMachine.MSG_STATE_CHANGE);
			msg.arg1 = state;
			msg.sendToTarget();
		}

	}

//	@Override
//	public boolean hasAvailiableChannel() {
//		BluetoothChannel custom = mChannelMap.get(BluetoothChannel.CUSTOM_UUID);
//		BluetoothChannel service = mChannelMap
//				.get(BluetoothChannel.SERVICE_UUID);
//		return custom != null && custom.isAvailiable() && service != null
//				&& service.isAvailiable();
//	}

	@Override
	public void createChannel(UUID uuid, OnChannelCallBack callback) {
		Message msg = mHandler.obtainMessage(CREATE_CHANNLE_MSG);
		ChannelCreateData data = new ChannelCreateData(uuid, callback);
		msg.obj = data;
		msg.sendToTarget();
	}

	@Override
	public void destoryChannle(UUID uuid) {
		Message msg = mHandler.obtainMessage(DESTORY_CHANNEL_MSG);
		msg.obj = uuid;
		msg.sendToTarget();
	}

	@Override
	public boolean listenChannel(UUID uuid, OnChannelCallBack callback) {
		try {
			BluetoothChannel channel = mChannelMap.get(uuid);
			if (channel != null) {
				logw("channel:" + uuid + " was already exist, close it now.");
				channel.close();
			}
			BluetoothServer server = new ServerBTPrivateChannel(mHandler, uuid.toString(), uuid, callback);
			mExecutor.execute(server);
			mChannelMap.put(uuid, server);
			callback.onCreateComplete(true, false);
			return true;
		} catch (IOException e) {
			callback.onCreateComplete(false, false);
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void send(ProjoList projoList, boolean isSync) {
		BluetoothChannel channel;
		channel = isSync ? mChannelMap.get(mServiceUUID) : mChannelMap.get(mCustomUUID);
		if (channel != null) {
			channel.send(projoList);
		} else {
			logw("channel not ready");
			Message msg = projoList.getCallbackMsg();
			if (msg != null) {
				msg.arg1 = DefaultSyncManager.NO_CONNECTIVITY;
				msg.sendToTarget();
			}
		}
	}
	
	private static void logv(String s) {
		Log.v(LogTag.APP, TAG + s);
	}
	
	private static void log(String s) {
		Log.d(LogTag.APP, TAG + s);
	}
	
	private static void logw(String s) {
		Log.w(LogTag.APP, TAG + s);
	}
	
	private static void logi(String s) {
		Log.i(LogTag.APP, TAG + s);
	}
	
	private static void loge(String s) {
		Log.e(LogTag.APP, TAG + s);
	}
}

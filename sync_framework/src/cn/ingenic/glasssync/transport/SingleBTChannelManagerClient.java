package cn.ingenic.glasssync.transport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.ProjoList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

class SingleBTChannelManagerClient implements BluetoothChannelManager {
	
	private static final String TAG = "<SCMC>";

	private final Handler mTransportHandler;
	private MyHandler mHandler;
	private Map<UUID, BluetoothChannel> mChannelMap;
	private BluetoothAdapter mAdapter;
	private final UUID mCustomUUID;
//	private final Context mContext;
//	private final DefaultSyncManager mMgr;
	
	private long mLastCloseTime = -1l;
	private static final long MIN_INTERVAL_MILLIS = 6 * 1000;

	SingleBTChannelManagerClient(Handler handler, Context context) {
//		mContext = context;
		mTransportHandler = handler;
		Enviroment env = Enviroment.getDefault();
		mCustomUUID = env.getUUID(BluetoothChannel.CUSTOM, true);
		
//		mMgr = DefaultSyncManager.getDefault();
		mChannelMap = new HashMap<UUID, BluetoothChannel>();
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		HandlerThread thread = new HandlerThread("SCMC");
		thread.start();

		mHandler = new MyHandler(thread.getLooper());
	}
	
	private void updateLastCloseTime() {
		logv("update last close time");
		mLastCloseTime = System.currentTimeMillis();
	}
	
	private void waitBeforeConnect() {
		long past = System.currentTimeMillis() - mLastCloseTime;
		if (past < MIN_INTERVAL_MILLIS) {
			long rest = MIN_INTERVAL_MILLIS - past;
			long wait = rest + (1000 - (rest % 1000));
			log("wait " + wait + " to connect");
			synchronized (this) {
				try {
					wait(wait);
				} catch (InterruptedException e) {
					loge("wait error");
				}
			}
		}
	}

	@Override
	public void prepare(String address) {
		if (BluetoothAdapter.checkBluetoothAddress(address)) {
			Message msg = mHandler.obtainMessage(SETUP_MSG);
			msg.obj = address;
			msg.sendToTarget();
		} else {
			Message msg = mHandler.obtainMessage(CLEAR_MSG);
			msg.obj = address;
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
		
		private void notifyStateChange(int state) {
			Message msg = mTransportHandler.obtainMessage(TransportStateMachine.MSG_STATE_CHANGE);
			msg.arg1 = state;
			msg.sendToTarget();
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SETUP_MSG:
				if (!mChannelMap.isEmpty()) {
					logw("channelMap not empty in SETUP_MSG, clear it.");
					for (BluetoothChannel channle : mChannelMap.values()) {
						channle.close();
					}
					mChannelMap.clear();
					updateLastCloseTime();
				}
				
				waitBeforeConnect();
				String address = (String) msg.obj;
				BluetoothDevice device = mAdapter.getRemoteDevice(address);
				if(mAdapter.isDiscovering())
					mAdapter.cancelDiscovery();
				BluetoothChannel custom = null;
				try {
					custom = new BluetoothClient(device,
							mCustomUUID, mHandler);
					logv("CUSTOM_CHANNEL created success in SEUP_MSG.");
					
					mChannelMap.put(mCustomUUID, custom);
					
					notifyStateChange(TransportStateMachine.C_CONNECTED);
				} catch (IOException e) {
					loge("CHANNEL create failed in SEUP_MSG:" + e.getMessage());
					LogTag.printExp(TAG, e);
					mAdapter.cancelDiscovery();
					Message m = mTransportHandler.obtainMessage(TransportStateMachine.MSG_STATE_CHANGE);
					m.arg1 = TransportStateMachine.C_IDLE;
					m.arg2 = DefaultSyncManager.CONNECT_FAILED;
					m.sendToTarget();
				}

				break;
				
			case RETRIVE_MSG:
				Message message = mTransportHandler.obtainMessage(TransportStateMachine.MSG_C_RETRIVE);
				message.obj = msg.obj;
				message.sendToTarget();
				break;
				
			case CLEAR_MSG:
				for (BluetoothChannel channle : mChannelMap.values()) {
					channle.close();
				}
				mChannelMap.clear();
				updateLastCloseTime();
				notifyStateChange(TransportStateMachine.C_IDLE);
				break;
				
			case CLIENT_CLOSE_MSG:
				updateLastCloseTime();
				Enviroment env = Enviroment.getDefault();
				BluetoothChannel client = (BluetoothChannel) msg.obj;
				UUID clientUUID = client.getUUID();
				logi("CLIENT_CLOSE_MSG comes with uuid:" + clientUUID);
				if (!env.isMainChannel(clientUUID, true)) {
					logi("private channel colse, do nothing");
					break;
				}
				
				if (mChannelMap.containsKey(clientUUID)) {
					mChannelMap.remove(clientUUID);
					if (env.getUUID(BluetoothChannel.CUSTOM, true).equals(clientUUID)) {
						notifyStateChange(TransportStateMachine.C_IDLE);
					} else {
						logw("main channel:" + clientUUID + " closed msg comes, but it is not custom channel.");
					}
				} else {
					logw("closed channel:" + clientUUID + " CLIENT_CLOSE_MSG comes");
				}
				break;
				
			case CREATE_CHANNLE_MSG:
				ChannelCreateData data = (ChannelCreateData) msg.obj;
				DefaultSyncManager manager = DefaultSyncManager.getDefault();
				String s = manager.getLockedAddress();
				if (BluetoothAdapter.checkBluetoothAddress(s)) {
					waitBeforeConnect();
					mAdapter.cancelDiscovery();
					BluetoothDevice d = mAdapter.getRemoteDevice(s);
					try {
						BluetoothChannel privateChannel = 
								new ClientBTPrivateChannel(d, data.uuid, mHandler, data.callback);
						mChannelMap.put(data.uuid, privateChannel);
						data.callback.onCreateComplete(true, true);
					} catch (IOException e) {
						data.callback.onCreateComplete(false, true);
						e.printStackTrace();
						mAdapter.cancelDiscovery();
					}
				} else {
					logw("create channel error in ClientBTChannelManager because of invalid address");
				}
				break;
				
			case DESTORY_CHANNEL_MSG:
				UUID uuid = (UUID) msg.obj;
				BluetoothChannel privateChannel = mChannelMap.get(uuid);
				if (privateChannel != null) {
					privateChannel.close();
					mChannelMap.remove(uuid);
					updateLastCloseTime();
				}
				break;
			
			case NEW_CLIENT_MSG:
				BluetoothChannel c = (BluetoothChannel) msg.obj;
				logi("this must be private channel:" + c.getUUID());
				break;
			}
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
	
	static class ChannelCreateData {
		UUID uuid;
		OnChannelCallBack callback;
		
		ChannelCreateData(UUID uuid, OnChannelCallBack callback) {
			this.uuid = uuid;
			this.callback = callback;
		}
	}
	
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
			TransportManager.THREAD_POOL_EXECUTOR.execute(server);
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
		BluetoothChannel channel = mChannelMap.get(mCustomUUID);
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

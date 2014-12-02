package cn.ingenic.glasssync.services;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.text.TextUtils;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.DefaultSyncManager.OnFileChannelCallBack;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.SyncSerializable;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.transport.ext.TransportManagerExt.OnRetriveCallback;

public class SyncService extends Service {
	private static final String TAG = "SyncService";
	private IBinder mService;

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate be called.");
		mService = new SyncServiceImpl().asBinder();
		Intent intent = new Intent(
				SyncModule.ACTION_SYNC_SERVICE_READY_RECEIVER);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Bundle b = new Bundle();
			b.putBinder(SyncModule.KEY_BINDER_EXTRA_FROM_BUNDLE, mService);
			intent.putExtra(SyncModule.KEY_BUNDLE_EXTRA_FROM_INTENT, b);
		}
		sendStickyBroadcast(intent);
		// set service to foreground
		if(cn.ingenic.glasssync.Enviroment.getDefault().isWatch())
			return;
		Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.app_name),
		        System.currentTimeMillis());
		notification.setLatestEventInfo(this, getText(R.string.app_name),
		        getText(R.string.service_msg), null);
		startForeground(0, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		logv("onBind be called");
		return mService;
	}
	
	@Override
	public void onDestroy() {
		logv("onDestroy be called.");
	}
	
	private void restartProcess(){
		sendBroadcast(new Intent(SyncModule.ACTION_MODULE_DIED));
	}

	private  class SyncServiceImpl extends ISyncService.Stub {
		
		private DefaultSyncManager mmManager;
		private Handler mmHandler;
		
		private static final int MSG_CALLBACK = 0;
		private static final int MSG_RESTART_MODULE=1;
		
		private  class Arg {
			final String module;
			final long sort;
			
			Arg(String module, long sort) {
				this.module = module;
				this.sort = sort;
			}
		}
		
		SyncServiceImpl() {
			mmManager = DefaultSyncManager.getDefault();
			mmHandler = new Handler() {

				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case MSG_CALLBACK:
						Arg arg = (Arg) msg.obj;
						ThirdPartyModule m = (ThirdPartyModule) mmManager.getModule(arg.module);
						if (m == null) {
							loge("can not find moudle:" + arg.module);
							return;
						}
						
						try {
							m.mCallback.onSendCallback(arg.sort, msg.arg1);
						} catch (RemoteException e) {
							loge("RemoteException:" + e.getMessage());
						}
						return;
					case MSG_RESTART_MODULE:
						restartProcess();
						break;
					default:
						throw new RuntimeException("SyncServiceImpl::mmHandler unknow msg:" + msg.what);
					}
				}
				
			};
		}
		
		@Override
		public boolean registModule(final String name, IModuleCallback callback)
				throws RemoteException {
			final IBinder binder = callback.asBinder();
			binder.linkToDeath(new DeathRecipient() {
				
				@Override
				public void binderDied() {
					logw("Process where module:" + name + " lives died.");
					mmTooLargeTranMap.remove(name);
					binder.unlinkToDeath(this, 0);
					mmHandler.sendEmptyMessageDelayed(MSG_RESTART_MODULE, 1000);
				}
				
			}, 0);
			return mmManager.registModule(new ThirdPartyModule(name, callback));
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return DefaultSyncManager.isConnect();
		}
		
		private Map<String, TooLargeSyncDataBuilder> mmTooLargeTranMap = new HashMap<String, TooLargeSyncDataBuilder>();

		@Override
		public boolean send(String module, SyncData data)
				throws RemoteException {
			if (TextUtils.isEmpty(module)) {
				logw("null module is invalid.");
				return false;
			}
			
			if (data == null) {
				logw("null syncData is invalid.");
				return false;
			}
			int totalLen = data.getInt(SyncData.KEY_TOTAL_LEN, 0);
			if (totalLen > 0) {
				logd("Receive(onRetrive) serializable SyncData start ...");
				mmTooLargeTranMap.put(module, new TooLargeSyncDataBuilder(totalLen, data));
				return true;
			}
			
			if (mmTooLargeTranMap.containsKey(module)) {
				TooLargeSyncDataBuilder builder = mmTooLargeTranMap.get(module);
				if (!builder.add(data)) {
					loge("TooLargeSyncDataBuilder overflowed, give up the large SyncData.");
					mmTooLargeTranMap.remove(module);
					return true;
				}
				if (builder.isFinish()) {
					data = builder.build();
					mmTooLargeTranMap.remove(module);
					logd("Receive(send) serializable SyncData end ...");
				} else {
					return true;
				}
			}
			
			SyncData.Config c = data.getConfig();
			if (c != null) {
				long sort = c.getSort();
				if (sort != SyncData.INVALID_SORT) {
					Message msg = mmHandler.obtainMessage(MSG_CALLBACK);
					msg.obj = new Arg(module, sort);
					c.mmCallback = msg;
				}
			}
			return DefaultSyncManager.SUCCESS == mmManager.send(module, data);
		}
		
		@Override
		public boolean sendFile(String module, ParcelFileDescriptor des, String name,
				int length) throws RemoteException {
			InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(des);
			mmManager.sendFile(module, name, length, in);
			return true;
		}

		@Override
		public boolean sendFileByPath(String module, ParcelFileDescriptor des, String name,
					      int length, String path) throws RemoteException {
		    InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(des);
		    mmManager.sendFileByPath(module, name, length, in, path);
		    return true;
		}

		@Override
		public void createChannel(String moudle, ParcelUuid uuid) throws RemoteException {
			mmManager.createChannel(moudle, uuid.getUuid());
		}

		@Override
		public String getLockedAddress() throws RemoteException {
			return mmManager.getLockedAddress().toUpperCase();
		}

		@Override
		public void sendOnChannel(String module, SyncData data,
				ParcelUuid uuid) throws RemoteException {
			mmManager.send(module, data, uuid.getUuid());
		}

		@Override
		public void destroyChannel(String module, ParcelUuid uuid)
				throws RemoteException {
			mmManager.destoryChannel(module, uuid.getUuid());
		}
	}
	
	public static class ThirdPartyModule extends Module implements OnRetriveCallback {

		private final IModuleCallback mCallback;
		
		public ThirdPartyModule(String name, IModuleCallback callback) {
			super(name);
			if (callback == null) {
				throw new NullPointerException("IModuleCallback can not be null!");
			}
			mCallback = callback;
		}
		
		@Override
		protected void onInit() {
			try {
				mCallback.onInit();
			} catch (Exception e) {
				loge("Exception:", e);
			}
		}

		@Override
		public void setSyncEnable(boolean enabled){
		    try {
			mCallback.setSyncEnable(enabled);
		    } catch (Exception e) {
			loge("Exception:", e);
		    }
		}

		@Override
		public boolean getSyncEnable(){
		    boolean ret = false;
		    try {
			ret = mCallback.getSyncEnable();
		    } catch (Exception e) {
			loge("Exception:", e);
		    }
		    return ret;
		}

		@Override
		protected void onCreate(Context context) {
			try {
				mCallback.onCreate();
			} catch (Exception e) {
				loge("Exception:", e);
			}
		}
		
		@Override
		protected void onClear(String address) {
			try {
				mCallback.onClear(address);
			} catch (Exception e) {
				loge("Exception:", e);
			}
		}

		@Override
		protected void onModeChanged(int mode) {
			try {
				mCallback.onModeChanged(mode);
			} catch (Exception e) {
				loge("Exception:", e);
			}
		}

		@Override
		protected void onConnectivityStateChange(boolean connected) {
			try {
				mCallback.onConnectivityStateChange(connected);
			} catch (Exception e) {
				loge("Exception:", e);
			}
		}
		
		@Override
		public OnChannelCallBack getChannelCallBack(UUID uuid) {
			loge("GetChannelCallBack unimplemented.");
			return null;
		}

		@Override
		protected Transaction createTransaction() {
			return new ThirdPartyTransaction();
		}
		
		private OnFileChannelCallBack mFileCallback = new OnFileChannelCallBack() {

			@Override
			public void onSendComplete(String name, boolean success) {
				try {
					mCallback.onFileSendComplete(name, success);
				} catch (Exception e) {
					loge("Exception:", e);
				}
			}

			@Override
			public void onRetriveComplete(String name, boolean success) {
				try {
					mCallback.onFileRetriveComplete(name, success);
				} catch (Exception e) {
					loge("Exception:", e);
				}
			}
			
		};
		
		@Override
		public OnFileChannelCallBack getFileChannelCallBack() {
			return mFileCallback;
		}

		private class ThirdPartyTransaction extends Transaction {
			
			@Override
			public void onStart(ArrayList<Projo> datas) {
				super.onStart(datas);
				loge("ThirdPartyTransaction onStart unplemented.");
			}
			
		}

		@Override
		public void onRetrive(SyncSerializable serial) {
			SyncData data = SyncSerializableTools.serial2Data(serial, true);
			data.setFlowDirrect(false);
			UUID uuid = serial.getDescriptor().mUUID;
			if (uuid != null) {
				try {
					mCallback.onChannelRetrive(new ParcelUuid(uuid), data);
				} catch (Exception e) {
					loge("Exception occurs in ThirdPartyException onRetrive:", e);
				}
			} else {
				try {
					if (serial.getDescriptor().isMid) {
						data.setConfig(new SyncData.Config(true));
					}
					mCallback.onRetrive(data);
				} catch (Exception e) {
					loge("Module:" + getName() + " exception occurs in onRetrive", e);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
						if (e instanceof TransactionTooLargeException) {
							logw("TransactionTooLargeException occurs.");
							byte[] serialDatas = data.getSerialDatas();
							if (serialDatas == null) {
								loge("Can not get serialDatas when parceling TooLargeSyncData.");
								return;
							}
							
							int sended = 0;
							SyncData sd = new SyncData();
							sd.setConfig(data.getConfig());
							sd.putInt(SyncData.KEY_TOTAL_LEN, serialDatas.length);
							try {
								do {
									int temp = serialDatas.length - sended;
									int prepare = (temp >= SyncData.MAX_LEN_PER_DATA) ? SyncData.MAX_LEN_PER_DATA
											: temp;
									byte[] datas = new byte[prepare];
									System.arraycopy(serialDatas, sended, datas, 0,
											prepare);
									sd.setSerialDatas(datas);
									mCallback.onRetrive(sd);
								} while (sended < serialDatas.length);
							} catch (Exception e1) {
								Log.e(TAG, "", e1);
							}
							return;
						}
					} else {
						// FIXME: checkout whether the reason of this exception
						// same from TransactionTooLargeException and do some operation like above.
						logw("Does this TransactionTooLargeException result in below API 15?");
					}
					loge("Exception occurs in ThirdPartyException onRetrive:", e);
				}
			}
		}
	}
	
	private static final String PRE = "<SS>";
	private static void logv(String msg) {
		Log.v(Constants.TAG, PRE + msg);
	}
	
	private static void logd(String msg) {
		Log.d(Constants.TAG, PRE + msg);
	}
	
	private static void logw(String msg) {
		Log.w(Constants.TAG, PRE + msg);
	}
	
	private static void loge(String msg) {
		Log.e(Constants.TAG, PRE + msg);
	}
	
	private static void loge(String msg, Throwable t) {
		Log.e(Constants.TAG, PRE + msg, t);
	}
}
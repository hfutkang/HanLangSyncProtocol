package cn.ingenic.glasssync.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import cn.ingenic.glasssync.services.mid.MidTableManager;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Log;

public abstract class SyncModule {
	private static final String TAG = "SyncModule";
	private static final boolean V = false;

	private static final String SERVICE = "cn.ingenic.glasssync.SYNC_SERVICE";
	public static final String ACTION_SYNC_SERVICE_READY_RECEIVER = "action.sync_service_ready.RECEIVER";
	/** send this action when any module's process died,*/
	public static final String ACTION_MODULE_DIED="action.sync_service_ready.restart_module";
	public static final String KEY_BUNDLE_EXTRA_FROM_INTENT = "key_bundle_from_intent";
	public static final String KEY_BINDER_EXTRA_FROM_BUNDLE = "key_binder_from_bundle";
	
	public static final int SAVING_POWER_MODE = 0;
	public static final int RIGHT_NOW_MODE = 1;

	// send status(force be consistent with DefaultSyncManager)
	public static final int SUCCESS = 0;
	public static final int NO_CONNECTIVITY = SUCCESS - 1;
	public static final int FEATURE_DISABLED = SUCCESS - 2;
	public static final int NO_LOCKED_ADDRESS = SUCCESS - 3;
	
        private boolean mSyncEnabled = true;
	private class ModuleCallback extends IModuleCallback.Stub {
		@Override
		public void setSyncEnable(boolean enabled){
		    Log.v(TAG, "---ModuleCallback--setSyncEnable---" + enabled);
		    mSyncEnabled = enabled;
		}

		@Override
		public boolean getSyncEnable(){
		    Log.v(TAG, "---ModuleCallback--getSyncEnable---" + mSyncEnabled);
		    return mSyncEnabled;
		}

		@Override
		public void onClear(String address) throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onClear(" + address + ") be called for " + mName);
			}
			
			MidTableManager mgr = getMidTableManager();
			if (mgr != null) {
				mgr.onModuleClear(address);
			}
			
			SyncModule.this.onClear(address);
		}

		@Override
		public void onConnectivityStateChange(boolean connected)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onConnectivityStateChange(" + connected + ") be called for " + mName);
			}
			
			MidTableManager mgr = getMidTableManager();
			if (mgr != null) {
				mgr.onModuleConnectivityChange(connected);
			}
			
			SyncModule.this.onConnectionStateChanged(connected);
		}

		@Override
		public void onModeChanged(int mode) throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onModeChanged(" + mode + ") be called for " + mName);
			}
			SyncModule.this.onModeChanged(mode);
		}

		@Override
		public void onCreate() throws RemoteException {
			Log.d(TAG, "Module:" + mName + " onCreated.");
			
			SyncModule.this.onCreate();
		}

		private TooLargeSyncDataBuilder mmTooLargeSyncDataBuilder;

		@Override
		public void onRetrive(SyncData data) throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onRetrive() be called for " + mName);
			}
			data.retain();
			int totalLen = data.getInt(SyncData.KEY_TOTAL_LEN, 0);
			if (totalLen > 0) {
				Log.d(TAG, "Receive(onRetrive) serializable SyncData start ...");
				mmTooLargeSyncDataBuilder = new TooLargeSyncDataBuilder(totalLen, data);
				return;
			} 
			
			if (mmTooLargeSyncDataBuilder != null) {
				if (!mmTooLargeSyncDataBuilder.add(data)) {
					Log.e(TAG, "TooLargeSyncDataBuilder overflowed, give up the large SyncData.");
					mmTooLargeSyncDataBuilder = null;
					return;
				}
				if (mmTooLargeSyncDataBuilder.isFinish()) {
					data = mmTooLargeSyncDataBuilder.build();
					mmTooLargeSyncDataBuilder = null;
					Log.d(TAG, "Receive(onRetrive) serializable SyncData end ...");
				} else {
					return;
				}
			}
			
			SyncData.Config c = data.getConfig();
			if (c != null && c.mmIsMid) {
				MidTableManager mgr = getMidTableManager();
				if (mgr == null) {
					Log.e(TAG, "can not get MidTableManager from getMidTableManager()");
					return;
				}
				
				mgr.onRetriveMidSyncData(data);
				return;
			}
			
			SyncModule.this.onRetrive(data);
		}

		@Override
		public void onFileSendComplete(String fileName, boolean success)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onFileSendComplete(" + fileName
						+ ", " + success + ") be called for " + mName);
			}
			SyncModule.this.onFileSendComplete(fileName, success);
		}

		@Override
		public void onFileRetriveComplete(String fileName, boolean success)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onFileRetriveComplete(" + fileName
						+ ", " + success + ") be called for " + mName);
			}
			SyncModule.this.onFileRetriveComplete(fileName, success);
		}

		@Override
		public void onSendCallback(long sort, int result)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onSendCallback(" + sort + ", "
						+ result + ") be called for " + mName);
			}
			Message m = mCallbackMap.get(sort);
			if (m != null) {
				m.arg1 = result;
				m.sendToTarget();
			}
		}

		@Override
		public void onInit() throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onInit() be called for " + mName);
			}
			
			MidTableManager mgr = getMidTableManager();
			if (mgr != null) {
				mgr.onModuleInit();
			}
			
			SyncModule.this.onInit();
		}

		@Override
		public void onChannelCreateComplete(ParcelUuid uuid, boolean success, boolean local)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onChannelCreateComplete() be called for " + mName);
			}
			SyncModule.this.onChannelCreateComplete(uuid, success, local);
		}

		@Override
		public void onChannelRetrive(ParcelUuid uuid, SyncData data)
				throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onChannelRetrive() be called for " + mName);
			}
			data.retain();
			SyncModule.this.onChannelRetrive(uuid, data);
		}

		@Override
		public void onChannelDestroy(ParcelUuid uuid) throws RemoteException {
			if (V) {
				Log.v(TAG, "ModuleCallback onChannelDestroy() be called for " + mName);
			}
			SyncModule.this.onChannelDestroy(uuid);
		}

	}

	protected final Context mContext;
	private final String mName;
	private ISyncService mService;
	private AtomicLong mSort = new AtomicLong(SyncData.INIT_SORT);
	private Map<Long, Message> mCallbackMap = new HashMap<Long, Message>();
	private final ModuleCallback mCallback = new ModuleCallback();
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (V) {
				Log.v(TAG, "onServiceConnected for " + mName);
			}
			mService = ISyncService.Stub.asInterface(service);
			if (mService != null) {
				try {
					service.linkToDeath(new DeathRecipient() {
						
						@Override
						public void binderDied() {
							mService = null;
						}
					}, 0);
					regist();
				} catch (Exception e) {
					Log.e(TAG, "Exception:", e);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (V) {
				Log.v(TAG, "onServiceDisconnected for " + mName);
			}
			mService = null;
		}

	};

	public SyncModule(String name, Context context) {
		this(name, context, true);
	}
	
	public SyncModule(String name, Context context, boolean autoBind) {
		mName = name;
		mContext = context.getApplicationContext();
		if (autoBind) {
			bind();
		}
	}
	
	public final void bind() {
		bind(null);
	}
	
	public final void bind(IBinder service) {
		if (mService != null) {
			Log.d(TAG, "Module:" + mName + " already bonded.");
			return;
		}
		if (service == null) {
			Intent intent = new Intent(SERVICE);
			mContext.bindService(intent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		} else {
			mService = ISyncService.Stub.asInterface(service);
			if (mService != null) {
				try {
					mService.registModule(mName, mCallback);
				} catch (RemoteException e) {
					Log.e(TAG, "", e);
				}
			}
		}
	}
	
	public final String getName() {
		return mName;
	}

        public void setSyncEnable(boolean enabled){
	    Log.v(TAG, "---SyncMode--setSyncEnable---" + enabled);
	    mSyncEnabled = enabled;
	}
    
	public boolean getSyncEnable(){
	Log.v(TAG, "---SyncMode--getSyncEnable---" + mSyncEnabled);
	return mSyncEnabled;
	}

	public boolean isConnected() throws SyncException {
		try {
			return getISyncService().isConnected();
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}
	
	public boolean hasLockedAddress() throws SyncException {
			return BluetoothAdapter.checkBluetoothAddress(getLockedAddress());
	}
	
	public String getLockedAddress() throws SyncException {
		try {
			return getISyncService().getLockedAddress();
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}

	private ISyncService getISyncService() throws SyncException {
		if (mService == null) {
			throw new SyncException("SyncService not found, call bind() to setup Service");
		}
		return mService;
	}

	private void regist() throws SyncException {
		try {
			getISyncService().registModule(mName, mCallback);
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}
	
	protected void onInit() {
	}
	
	protected abstract void onCreate();

	protected void onClear(String address) {
	}

	protected void onConnectionStateChanged(boolean connect) {
	}

	protected void onModeChanged(int mode) {
	}

	public boolean sendFile(File file, String name, int length)
			throws SyncException, FileNotFoundException {
		try {
			int modeBits = modeToMode(null, "r");
			ParcelFileDescriptor pfd = ParcelFileDescriptor
					.open(file, modeBits);
			return getISyncService().sendFile(mName, pfd, name, length);
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}

    public boolean sendFileByPath(File file, String name, int length, String path)throws SyncException, FileNotFoundException {
	try {
	    int modeBits = modeToMode(null, "r");
	    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, modeBits);
	    return getISyncService().sendFileByPath(mName, pfd, name, length, path);
	} catch (RemoteException e) {
	    Log.e(TAG, "SyncException:", e);
	    throw new SyncException("RemoteException occurs");
	}
    }
	
	//just simulate ContentResolver.modeToMode
	private static int modeToMode(Uri uri, String mode) throws FileNotFoundException {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new FileNotFoundException("Bad mode for " + uri + ": "
                    + mode);
        }
        return modeBits;
    }

	private void processConfig(SyncData.Config config) {
		if (config == null) {
			return;
		}

		if (config.mmCallback != null) {
			config.mmSort = mSort.getAndIncrement();
			mCallbackMap.put(config.mmSort, config.mmCallback);
		}
	}

	public synchronized boolean send(SyncData data) throws SyncException {
		try {
			processConfig(data.getConfig());
			return getISyncService().send(mName, data);
		} catch (RemoteException e) {
			if (e instanceof TransactionTooLargeException) {
				Log.w(TAG, "TransactionTooLargeException occurs in send().");
				byte[] serialDatas = data.getSerialDatas();
				if (serialDatas == null) {
					throw new SyncWrapperExcetpion(e);
				}
				int sended = 0;
				SyncData sd = new SyncData();
				sd.setConfig(data.getConfig());
				sd.putInt(SyncData.KEY_TOTAL_LEN, serialDatas.length);
				boolean r = false;
				do {
					int temp = serialDatas.length - sended;
					int prepare = (temp >= SyncData.MAX_LEN_PER_DATA) ? SyncData.MAX_LEN_PER_DATA : temp;
					byte[] datas = new byte[prepare];
					System.arraycopy(serialDatas, sended, datas, 0, prepare);
					sd.setSerialDatas(datas);
					r = send(sd);
					sended += prepare;
				} while (sended < serialDatas.length);
				return r;
			} else {
				Log.e(TAG, "SyncException:", e);
				throw new SyncWrapperExcetpion(e);
			}
		}
	}
	
	public void sendOnChannel(SyncData data, ParcelUuid uuid) throws SyncException {
		try {
			getISyncService().sendOnChannel(mName, data, uuid);
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}
	
	public void createChannel(ParcelUuid uuid) throws SyncException {
		try {
			getISyncService().createChannel(mName, uuid);
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		} 
	}
	
	public void destroyChannel(ParcelUuid uuid) throws SyncException {
		try {
			getISyncService().destroyChannel(mName, uuid);
		} catch (RemoteException e) {
			Log.e(TAG, "SyncException:", e);
			throw new SyncException("RemoteException occurs");
		}
	}
	
	public MidTableManager getMidTableManager() {
		return null;
	}

	protected void onRetrive(SyncData data) {
	}

	protected void onFileSendComplete(String fileName, boolean success) {
	}

	protected void onFileRetriveComplete(String fileName, boolean success) {
	}
	
	protected void onChannelCreateComplete(ParcelUuid uuid, boolean success, boolean local) {
	}

	protected void onChannelRetrive(ParcelUuid uuid, SyncData data) {
	}

	protected void onChannelDestroy(ParcelUuid uuid) {
	}

}

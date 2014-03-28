package cn.ingenic.glasssync.transport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.LogTag.Transport;
import cn.ingenic.glasssync.data.ControlProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.transport.ext.TransportManagerExt;

public class TransportManager {
	private MyHandler mHandler;
	protected final Context mContext;
	private FileChannelManager mFileChannelManager;
	
	private static TransportManager sManager;

	public static TransportManager init(Context context, String system,
			Handler managerHandler) {
		if (sManager == null) {
			if (LogTag.V) {
				Transport.d("create Manager.");
			}

			sManager = new TransportManagerExt(context, system, managerHandler);
		} else {
			Transport.w("Manager alread created.");
		}

		return sManager;
	}
	
	private final String mSystemModuleName; 
	static String getSystemMoudleName() {
		return sManager.mSystemModuleName;
	}
	
	public static TransportManager getDefault() {
		if (sManager == null) {
			throw new 
			NullPointerException("TransportManager must be inited before getDefault().");
		}
		
		return sManager;
	}
	
	private volatile boolean mTimeoutMsgLock = false;
	private final PowerManager.WakeLock mWakeLock;
	static void sendTimeoutMsg() {
		Handler handler = sManager.mHandler;
		if (handler.hasMessages(MSG_TIME_OUT)) {
			handler.removeMessages(MSG_TIME_OUT);
		}
		
		synchronized (sManager.mWakeLock) {
			if (!sManager.mWakeLock.isHeld()) {
				Transport.i("acquire WakeLock.");
				sManager.mWakeLock.acquire();
			} else {
				Transport.v("WakeLock already acquire.");
			}
		}
		
		if (!sManager.mTimeoutMsgLock) {
			Transport.v("send timeout msg");
			handler.sendMessageDelayed(handler.obtainMessage(MSG_TIME_OUT),
					DefaultSyncManager.TIMEOUT);
		} else {
			Transport.w("Time out msg locked, do not send time out msg.");
		}
	}
	
	private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 16;
    private static final int KEEP_ALIVE = 1;
	
	private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(10);
	
	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "TransportManager #" + mCount.getAndIncrement());
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                    TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
	
	static final int MSG_BASE = 0;
	static final int MSG_TIME_OUT = MSG_BASE + 3;
	static final int MSG_FILE_CHANNEL_CLOSE = MSG_BASE + 4;
	
	protected final Handler mMgrHandler;
	private TransportStateMachine mStateMachine;
	
	protected TransportManager(Context context, String sysetmModuleName, Handler mgrHandler){
		mContext = context;
		mSystemModuleName = sysetmModuleName;
		mMgrHandler = mgrHandler;
		
		PowerManager powerMgr = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"TransportManager");
		mWakeLock.setReferenceCounted(false);
		
		mHandler = new MyHandler();
		init();
	}	
	
	protected void init() {
		mFileChannelManager = new FileChannelManager(mContext, mHandler);
		mStateMachine = new TransportStateMachine(mContext, this);
		mStateMachine.start();
	}
	
	protected final void releaseWakeLock() {
		synchronized (mWakeLock) {
			if (mWakeLock.isHeld()) {
				Transport.i("release WakeLock");
				mWakeLock.release();
			} else {
				Transport.v("WakeLock not locked");
			}
		}
	}
	
	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIME_OUT:
				if (mTimeoutMsgLock) {
					Transport.w("Time out msg locked, do not send msg.");
					return;
				}
				
				releaseWakeLock();
				
				Transport.i("MSG_TIME_OUT msg comes, send disconnect msg");
				prepare("");
//				Message mgrMsg = mMgrHandler.obtainMessage(DefaultSyncManager.MSG_TIME_OUT);
//				mgrMsg.sendToTarget();
				break;
				
			case MSG_FILE_CHANNEL_CLOSE:
				Transport.d("File Channel close, release time out msg Lock");
				mTimeoutMsgLock = false;
				sendTimeoutMsg();
				break;
				
			}
			
		}
	}
	
	public void prepare(String address) {
		if (BluetoothAdapter.checkBluetoothAddress(address)) {
			Message msg = mStateMachine.obtainMessage(TransportStateMachine.MSG_CONNECT);
			msg.obj = address;
			msg.sendToTarget();
		} else {
			Message msg = mStateMachine.obtainMessage(TransportStateMachine.MSG_DISCONNECT);
			msg.sendToTarget();
		}
	}
	
	void sendClearLockedAddressMsg() {
		mMgrHandler.sendEmptyMessage(DefaultSyncManager.MSG_CLEAR_ADDRESS);
	}
	
	public void notifyMgrState(boolean success) {
		notifyMgrState(success, 0);
	}
	
	public void notifyMgrState(boolean success, int arg2) {
		if (!success) {
			releaseWakeLock();
			
			DefaultSyncManager mgr = DefaultSyncManager.getDefault();
			if (DefaultSyncManager.IDLE != mgr.getState()) {
				mFileChannelManager.close(false);
			}
		}
		
		int state = success ? DefaultSyncManager.CONNECTED : DefaultSyncManager.IDLE;
		Message msg = mMgrHandler.obtainMessage(DefaultSyncManager.MSG_STATE_CHANGE);
		msg.arg1 = state;
		msg.arg2 = arg2;
		msg.sendToTarget();
	}
	
	void retrive(final ProjoList projoList) {
		Runnable runnable = new Runnable () {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				ControlProjo control = 
						(ControlProjo) projoList.get(ProjoListColumn.control);
				
				DefaultSyncManager m = DefaultSyncManager.getDefault();
				m.response(Config.getConfig(control), 
						(ArrayList<Projo>) projoList.get(ProjoListColumn.datas));
			}
			
		};
		mHandler.post(runnable);
	}
	
	public void sendBondResponse(boolean pass) {
		mStateMachine.sendBondResponse(pass);
	}
	
	public void request(final ProjoList projoList) {
		mStateMachine.sendRequest(projoList);
	}
	
	public void requestSync(ProjoList projoList) {
		mStateMachine.sendRequestSync(projoList);
	}
	
	public void requestUUID(UUID uuid, ProjoList projoList) {
		mStateMachine.sendRequestByUUID(uuid, projoList);
	}
	
	public void sendFile(String module, String name, int length, InputStream in) {
		mTimeoutMsgLock = true;
		mFileChannelManager.sendFile(module, name, length, in);
	}
	
	public void retriveFile(String module, String name, int length, String address) {
		mTimeoutMsgLock = true;
		mFileChannelManager.retriveFile(module, name, length, address);
	}
	
	public void createChannel(UUID uuid, OnChannelCallBack callback) {
		mStateMachine.createChannel(uuid, callback);
	}
	
	public boolean listenChannel(UUID uuid, OnChannelCallBack callback) {
		return mStateMachine.listenChannel(uuid, callback);
	}
	
	public void destoryChannel(UUID uuid) {
		mStateMachine.destoryChannel(uuid);
	}
	
	public void closeFileChannel() {
		mFileChannelManager.close();
	}
	
}

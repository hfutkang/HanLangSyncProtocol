package cn.ingenic.glasssync.transport.ext;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

class ConnectionTimeoutManager {
	private final PkgEncodingWorkspace mPe;
	private final PkgDecodingWorkspace mPd;
	private Message mTimeoutMsg;
	private final Handler mHandler;
	private final long mTimeout;
	private long mPeLastTimeStamp;
	private long mPdLastTimeStamp;
	private final PowerManager.WakeLock mWakeLock;
	
	private static ConnectionTimeoutManager sManager = null;
	static void init(Context ctx, PkgEncodingWorkspace pe, PkgDecodingWorkspace pd,
			Message msg, long timeout) {
		if (sManager == null) {
			sManager = new ConnectionTimeoutManager(ctx, pe, pd, msg, timeout);
		}
	}
	
	static ConnectionTimeoutManager getInstance() {
		if (sManager == null) {
			throw new IllegalStateException("ConnectionTimeoutManager have not initialization.");
		}
		
		return sManager;
	}
	
	private ConnectionTimeoutManager(Context context, PkgEncodingWorkspace pe,
			PkgDecodingWorkspace pd, Message msg, long timeout) {
		mPe = pe;
		mPd = pd;
		mTimeoutMsg = msg;
		mTimeout = timeout;
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				long cur = System.currentTimeMillis();
				if (((cur - mPeLastTimeStamp) >= mTimeout)
						&& ((cur - mPdLastTimeStamp) >= mTimeout)) {
					if(mPd.isDecoding()){
						logd("is decoding NOW, so delay TimeOut msg ");
						pushTimeoutMsg();
						return;
					}
					if (mPe.isEmpty() && mPd.isEmpty()) {
						if (mWakeLock.isHeld()) {
							logd("Release WakeLock.");
							mWakeLock.release();
						}
						Message timeout = Message.obtain(mTimeoutMsg);
						mTimeoutMsg.sendToTarget();
						mTimeoutMsg = timeout;
					} else {
						pushTimeoutMsg();
					}
				}
			}
			
		};
		PowerManager powerMgr = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"ConnectionTimeoutManager");
		mWakeLock.setReferenceCounted(false);
	}
	
	private void pushTimeoutMsg() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				if (mHandler.hasMessages(0)) {
					mHandler.removeMessages(0);
				}
				if (!mWakeLock.isHeld()) {
					logd("Acquire WakeLock.");
					mWakeLock.acquire();
				}
				mHandler.sendEmptyMessageDelayed(0, mTimeout);
			}
			
		});
	}
	
	void peEmpty() {
		mPeLastTimeStamp = System.currentTimeMillis();
		if (mPd.isEmpty()) {
			pushTimeoutMsg();
		}
	}
	
	void pdEmpty() {
		mPdLastTimeStamp = System.currentTimeMillis();
		if (mPe.isEmpty()) {
			pushTimeoutMsg();
		}
	}
	
	private static final String PRE = "<TO>";
	private static void logd(String msg) {
		Log.d(ProLogTag.TAG, PRE + msg);
	}
}

package cn.ingenic.glasssync.screen.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class ScreenControlModule extends SyncModule {
	private static final String TAG = "ScreenControlModule";
	public static final Boolean VDBG = true;

	private static final String SCREENCONTROL_NAME = "scnctrl_module";
	private static final String GESTURE_CMD = "gesture_cmd";

	private static ScreenControlModule sInstance;
	private GestureHandler mHandler;

	private ScreenControlModule(Context context) {
		super(SCREENCONTROL_NAME, context);
	}

	public static ScreenControlModule getInstance(Context c) {
		if (null == sInstance) {
			sInstance = new ScreenControlModule(c);
		}
		return sInstance;
	}

	@Override
	protected void onCreate() {
		HandlerThread thread = new HandlerThread("gesture_retrive");
		thread.start();
		mHandler = new GestureHandler(thread.getLooper());
	}

	@Override
	protected void onRetrive(SyncData data) {
		int gesture = data.getInt(GESTURE_CMD);
		try {
			mHandler.obtainMessage(MSG_GESTURE, gesture, 0).sendToTarget();
		} catch (Exception e) {
			Log.d(TAG, "-exception e" + e);
		}
	}

	private final int MSG_GESTURE = 0;

	private class GestureHandler extends Handler {

		public GestureHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_GESTURE:
			        if (VDBG)
				    Log.d(TAG, "handle msg " + getGestureStr(msg.arg1));
				Intent intent = new Intent("com.ingenic.glass.screencontrol.gesture");
				intent.putExtra("gesture", msg.arg1);
				mContext.sendBroadcast(intent);
				break;
			default:
				break;
			}
		}
	}
	
	private String getGestureStr(int gesture) {
		switch (gesture) {
		case 0:
			return "tap";
		case 1:
			return "double";
		case 2:
			return "long";
		case 3:
			return "up";
		case 4:
			return "down";
		case 5:
			return "left";
		case 6:
			return "right";
		default:
			break;
		}
		return null;
	}
}
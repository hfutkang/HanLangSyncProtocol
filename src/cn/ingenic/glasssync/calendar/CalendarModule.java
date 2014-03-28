package cn.ingenic.glasssync.calendar;



import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.Transaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class CalendarModule extends Module {

	public static final String TAG = "CalendarModule";
	public static final boolean V = true;

	public static final String CALENDAR = "calendar";
	private Context mContext;
	CalendarController mCalendarController;
	IntentFilter filter;
	CalendarSyncReceiver mReceiver;
	private DefaultSyncManager mManager;
	private boolean isRegister = false;

	public CalendarModule() {
		super(CALENDAR);
	}

	@Override
	protected Transaction createTransaction() {
		return new CalendarTransaction();
	}

	@Override
	protected void onCreate(Context context) {
		if (V) {
			Log.d(TAG, "CalendarModule created.");
		}
		mContext = context;
		mCalendarController = CalendarController.getInstance(mContext);
		mManager = DefaultSyncManager.getDefault();

		filter = new IntentFilter();
		filter.addAction(CalendarSyncReceiver.ACTION_DELETE_EVENT_FROM_WATCH);
		filter.addAction(CalendarSyncReceiver.ACTION_REQUEST_UPDATE_FROM_WATCH);
		mReceiver = new CalendarSyncReceiver(mCalendarController);

		boolean isEnable = mManager.isFeatureEnabled(CALENDAR);
//		notifyCurrentSyncState(isEnable);
		if(isEnable){
			mContext.registerReceiver(mReceiver, filter);
			isRegister = true;
		}
	}

	@Override
	protected void onConnectivityStateChange(boolean connected) {
		super.onConnectivityStateChange(connected);
//		if (connected) {
//			mContext.registerReceiver(mReceiver, filter);
//		} else {
//			mContext.unregisterReceiver(mReceiver);
//		}
//
//		notifyCurrentSyncState();
	}

	@Override
	protected void onModeChanged(int mode) {
//		notifyCurrentSyncState();
		super.onModeChanged(mode);
	}

	@Override
	protected void onFeatureStateChange(String feature, boolean enabled) {
		Log.d(TAG,"onFeatureStateChange enabled:"+enabled);
		if (enabled&&!isRegister) {
			mContext.registerReceiver(mReceiver, filter);
			isRegister = true;
		} else if(isRegister){
			mContext.unregisterReceiver(mReceiver);
			isRegister = false;
		}
//		notifyCurrentSyncState(enabled);
		super.onFeatureStateChange(feature, enabled);
	}

	public static void recallBackSyncMsg() {
		if (CalendarController.mHandler != null) {
			CalendarController.mHandler
					.removeMessages(CalendarController.MSG_UPDATE_ALL);
		}
	}

	public static void sendMsgToSync(long delayMillis) {
		if (CalendarController.mHandler != null) {
			Message mMessage = new Message();
			mMessage.what = CalendarController.MSG_UPDATE_ALL;
			CalendarController.mHandler.sendMessageDelayed(mMessage,
					delayMillis);
		}
	}

//	private boolean syncEnabled = false;
	private final static String ACTION_NOTIFY_CURRENT_SYNC_STATES = "cn.ingenic.glasssync.calendar.notify_current_sync_states";
	private final static String HAND_SYNC_STATE = "hand_sync_state";

	private void notifyCurrentSyncState(boolean isEnable) {

//		boolean isConnected = mManager.isConnect();
//		int mode = mManager.getCurrentMode();
//		// boolean moduleEnable = mManager.isFeatureEnabled(CALENDAR);

//		boolean currentSyncEnabled = syncEnabled;
//		Log.i(TAG, "moduleEnabled:" + moduleEnabled + " isConnected:"
//				+ isConnected + " mode" + mode);
//		if (moduleEnabled && isConnected
//				&& mode == DefaultSyncManager.SAVING_POWER_MODE) {
//			currentSyncEnabled = true;
//		} else {
//			currentSyncEnabled = false;
//		}
//		currentSyncEnabled = isEnable;
		Log.i(TAG, "currentSyncEnabled:" + isEnable /*+ " syncEnabled:"
				+ syncEnabled*/);
//		if (currentSyncEnabled ^ syncEnabled) {
//			syncEnabled = currentSyncEnabled;
			Intent intent = new Intent();
			intent.setAction(ACTION_NOTIFY_CURRENT_SYNC_STATES);
			intent.putExtra(HAND_SYNC_STATE, isEnable);
			mContext.sendStickyBroadcast(intent);
//		}
	}
}

package cn.ingenic.glasssync.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CalendarSyncReceiver extends BroadcastReceiver {
	static CalendarController mController;
	private static final String TAG = "CalendarSyncReceiver";
	public final static String ACTION_DELETE_EVENT_FROM_WATCH = "com.android.calendar.action_delete_event_from_watch";
	public final static String ACTION_REQUEST_UPDATE_FROM_WATCH = "com.android.calendar.action_request_update_from_watch";

	public CalendarSyncReceiver() {
		super();
	}

	public CalendarSyncReceiver(CalendarController mController) {
		super();
		this.mController = mController;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		entrueCalendarControllerExist(context);
		Log.i(TAG, "receiver action:" + action);
		if (action.equals(ACTION_DELETE_EVENT_FROM_WATCH)) {
			int eventId = intent.getExtras().getInt("glasssyncId", -1);

			mController.deleteEventNotify(eventId);
		} else if (action.equals(ACTION_REQUEST_UPDATE_FROM_WATCH)) {
			mController.requestUpdate();
		}
	}

	private void entrueCalendarControllerExist(Context contex) {
		if (mController == null) {
			mController = CalendarController.getInstance(contex);
		}
	}
}

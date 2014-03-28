package cn.ingenic.glasssync.calllog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RemoveMissedCallsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// see Dialer/...CallLogFragment.removeMissedCallNotifications()
		if(intent.getAction().equals("cn.ingenic.sync.remove_missed_calls")){
			android.util.Log.i("sw2df", "remove missed calls , from Dialer/CallLogFragment.java");
			MissedCallNotify.getInstance(context).cancelMissedCallNotification();
		}
	}

}

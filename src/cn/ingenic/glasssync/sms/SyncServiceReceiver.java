package cn.ingenic.glasssync.sms;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import cn.ingenic.glasssync.services.SyncModule;
import android.util.Log;

@SuppressLint("NewApi")
public class SyncServiceReceiver extends BroadcastReceiver {
	private final String RESTART="action.sync_service_ready.restart_module";
	private final String RECEIVER="action.sync_service_ready.RECEIVER";

	@Override
	public void onReceive(Context ctx, Intent intent) {
		String action=intent.getAction();
		Log.e("sms_log","IndroidMms receiver action is :"+action);
		if(action.equals(RECEIVER)){
			Bundle b = intent.getBundleExtra(SyncModule.KEY_BUNDLE_EXTRA_FROM_INTENT);
			IBinder binder = b.getBinder(SyncModule.KEY_BINDER_EXTRA_FROM_BUNDLE);
			SyncModule m = SmsModule.getInstance(ctx);
			m.bind(binder);
		}else if(action.equals(RESTART)){
			
		}
	}
}

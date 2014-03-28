package cn.ingenic.glasssync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BondClearReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LogTag.APP, "BondClearReceiver receives " + intent.getAction());
		if ("cn.ingenic.glasssync.BOND_CLEAR".equals(intent.getAction())) {
			DefaultSyncManager.getDefault().setLockedAddress("");
		}
	}

}

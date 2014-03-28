package cn.ingenic.glasssync.devicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;

public class AppsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		int cmd = intent.getIntExtra("cmd", 0);
		String data = intent.getStringExtra("data");
		klilog.i("broadcast received. cmd:"+cmd+", data:"+data);
		ConnectionManager.getInstance(context).apps2Device(cmd, data);
	}

}

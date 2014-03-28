package cn.ingenic.glasssync.devicemanager;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class DeviceReceiver extends DeviceAdminReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	}

	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
		setAdminEnable(context, true);
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		super.onDisabled(context, intent);
		setAdminEnable(context, false);
	}

	private void setAdminEnable(Context context, boolean enable){
		SharedPreferences pref = context.getSharedPreferences("device_manager", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("lock_screen", enable);
		editor.commit();
	}
}

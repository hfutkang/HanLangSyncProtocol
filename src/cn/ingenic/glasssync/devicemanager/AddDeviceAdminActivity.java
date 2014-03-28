package cn.ingenic.glasssync.devicemanager;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class AddDeviceAdminActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		ComponentName cn = new ComponentName(this, DeviceReceiver.class);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
		startActivity(intent);
		finish();
	}


}

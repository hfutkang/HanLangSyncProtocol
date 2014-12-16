package cn.ingenic.glasssync;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.phone.PhoneModule;
import cn.ingenic.glasssync.appmanager.AppManagerModule;
import cn.ingenic.glasssync.calendar.CalendarModule;
import cn.ingenic.glasssync.camera.CameraModule;
import cn.ingenic.glasssync.screen.control.ScreenControlModule;
import cn.ingenic.glasssync.services.SyncService;
import cn.ingenic.glasssync.updater.UpdaterModule;
import cn.ingenic.glasssync.notify.GlassSyncNotifyModule;
import cn.ingenic.glasssync.wifi.GlassSyncWifiModule;
import cn.ingenic.glasssync.multimedia.MultiMediaManager;
import cn.ingenic.glasssync.lbs.GlassSyncLbsManager;
import cn.ingenic.glasssync.blmanager.GlassSyncBLManager;
import cn.ingenic.glasssync.screen.screenshare.ScreenModule;
import cn.ingenic.glasssync.contact.ContactsLiteModule;
import cn.ingenic.glasssync.sms.SmsModule;


public class SyncApp extends Application implements
		Enviroment.EnviromentCallback {
	private BluetoothAdapter mBluetoothAdapter;
	private static final String INTENT_DISCOVERABLE_TIMEOUT = "android.bluetooth.intent.DISCOVERABLE_TIMEOUT";
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LogTag.APP, "rc=="+intent.getAction());
			 if (BluetoothAdapter.ACTION_STATE_CHANGED
						.equals(intent.getAction())) {
				boolean scan = mBluetoothAdapter
						.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
				mBluetoothAdapter.setDiscoverableTimeout(120*300);//10h
				int discoverabletimeout = mBluetoothAdapter
						.getDiscoverableTimeout();
				Log.d(LogTag.APP, discoverabletimeout + "discoverabletimeout");
                long enableTime =System.currentTimeMillis() + discoverabletimeout * 1000L;
                if(discoverabletimeout!=-1){
                	setDiscoverableAlarm(getApplicationContext(),enableTime);
                }
			} else if (INTENT_DISCOVERABLE_TIMEOUT.equals(intent.getAction())) {
				if (mBluetoothAdapter != null
						&& mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
					Log.d(LogTag.APP, "Disable discoverable...");
					mBluetoothAdapter
							.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
					unregisterReceiver(mReceiver);

				}
			}		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		
		if (LogTag.V) {
			Log.d(LogTag.APP, "Sync App created.");
		}
		Enviroment.init(this);
		DefaultSyncManager manager = DefaultSyncManager.init(this);
		
		SystemModule systemModule = new SystemModule();
		if (manager.registModule(systemModule)) {
            Log.i(LogTag.APP, "SystemModule is registed.");
        }

	    GlassSyncNotifyModule mGsnm = new GlassSyncNotifyModule(this);
	    GlassSyncWifiModule mGswm = GlassSyncWifiModule.getInstance(this);
	    MultiMediaManager mMMM = MultiMediaManager.getInstance(this);
	    //GlassSyncLbsManager mGslbs = GlassSyncLbsManager.getInstance(this);
	    GlassSyncBLManager gsblm = GlassSyncBLManager.getInstance(this);

	    Log.v(LogTag.APP,"before ScreenModule");
	    //if (android.os.Build.VERSION.SDK_INT >= 16) {
		ScreenModule mSM= ScreenModule.getInstance(this);
		// }
        UpdaterModule um = new UpdaterModule();
        if (manager.registModule(um)) {
            Log.i(LogTag.APP, "UpdaterModule is registed.");
        }

//        PhoneModule pm = new PhoneModule();
//        if(manager.registModule(pm)){
//            Log.i(LogTag.APP, "PhoneModule  registed");
//        }

        CameraModule cm = new CameraModule();
        if(manager.registModule(cm)){
            Log.i(LogTag.APP, "CameraModule  registed");
        }
        //app manager
        AppManagerModule.getInstance(this);
        
        //screen controller
        ScreenControlModule.getInstance(this);
	    ContactsLiteModule.getInstance(this);
    	SmsModule.getInstance(this);
        
        DeviceModule dm = DeviceModule.getInstance();
        if(manager.registModule(dm)){
            Log.i(LogTag.APP, "DeviceModule  registed");
        }
        
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            CalendarModule calm = new CalendarModule();
            if (manager.registModule(calm)) {
                Log.i(LogTag.APP, "CalendarModule registed");
            }
        }
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.enable();
			Log.d(LogTag.APP, mBluetoothAdapter.isEnabled() + "enable");
			    }
		Intent intent = new Intent(this, SyncService.class);
		startService(intent);
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_DISCOVERABLE_TIMEOUT);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);

	}

	@Override
	public Enviroment createEnviroment() {
		return new WatchEnviroment(this);
	}

	void setDiscoverableAlarm(Context context, long alarmTime) {
		Log.d(LogTag.APP, "setDiscoverableAlarm(): alarmTime = " + alarmTime);

		Intent intent = new Intent(INTENT_DISCOVERABLE_TIMEOUT);
		//intent.setClass(context, BluetoothDiscoverableTimeoutReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent,
				0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		if (pending != null) {
			// Cancel any previous alarms that do the same thing.
			alarmManager.cancel(pending);
			Log.d(LogTag.APP, "setDiscoverableAlarm(): cancel prev alarm");
		}
		pending = PendingIntent.getBroadcast(context, 0, intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pending);
	}

}

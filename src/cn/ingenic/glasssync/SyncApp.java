package cn.ingenic.glasssync;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.phone.PhoneModule;
import cn.ingenic.glasssync.appmanager.AppManagerModule;
import cn.ingenic.glasssync.calendar.CalendarModule;
import cn.ingenic.glasssync.camera.CameraModule;
import cn.ingenic.glasssync.services.SyncService;
import cn.ingenic.glasssync.updater.UpdaterModule;
import cn.ingenic.glasssync.notify.GlassSyncNotifyModule;
import cn.ingenic.glasssync.wifi.GlassSyncWifiModule;
import cn.ingenic.glasssync.multimedia.MultiMediaManager;
import cn.ingenic.glasssync.lbs.GlassSyncLbsManager;
import cn.ingenic.glasssync.blmanager.GlassSyncBLManager;


public class SyncApp extends Application implements
		Enviroment.EnviromentCallback {

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
		Intent intent = new Intent(this, SyncService.class);
		startService(intent);
	}

	@Override
	public Enviroment createEnviroment() {
		return new WatchEnviroment(this);
	}
	
}

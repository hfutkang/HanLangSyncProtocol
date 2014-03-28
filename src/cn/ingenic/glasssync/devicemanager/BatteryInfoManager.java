package cn.ingenic.glasssync.devicemanager;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ingenic.glasssync.DefaultSyncManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

public class BatteryInfoManager {
	private static BatteryInfoManager sInstance;
	private JSONObject mStoreStatus;
	private static boolean mBatterySync=true;
	
	private BatteryInfoManager(Context context){
        //register battery listener
	}
	
	public static BatteryInfoManager init(Context context){
		if(sInstance == null){
			sInstance = new BatteryInfoManager(context);
		}
		return sInstance;
	}
	
	public static BatteryInfoManager getInstance(){
		return sInstance;
	}
	
    public static void setFeature(Context c, boolean enable) {
        if(!enable){
            JSONObject batteryInfo = new JSONObject();
            try {
            batteryInfo.put("level", 111);
            batteryInfo.put("scale", 100);
            batteryInfo.put("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
            ConnectionManager.device2Device(Commands.CMD_GET_BATTERY, batteryInfo.toString());
            }catch(JSONException e){
                klilog.e("battery info JSONException:"+e);
            }
        } else {
            //init(c).sendBatteryInfo(); //watch need no send battery info
            
        }
        mBatterySync = enable;
    }
}

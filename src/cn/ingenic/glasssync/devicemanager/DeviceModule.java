package cn.ingenic.glasssync.devicemanager;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.RemoteBinderImpl;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.calllog.CallLogManager;
/**
 * for device sync, to get watch's name & version info
 * @author kli*/
public class DeviceModule extends Module {

    public static final String TAG = "DEVICE";
    public static final boolean V = true;

    static final String MODULE = "DEVICE";
    
    public static final String FEATURE_CALLLOG = "calllog";
    public static final String FEATURE_WEATHER = "weather";
    public static final String FEATURE_BATTERY = "battery";
    public static final String FEATURE_TIME = "time";
    public static final String FEATURE_UNBIND = "unbind";

    private static final String CLEAE_ACTION= "cn.ingenic.action.sync_clear";
    private static DeviceModule sInstance;
    
    private Context mContext;
    private ConnectionManager mConnectionManager;
	private CallLogManager mCallLogManager;
	private BatteryInfoManager mBatteryManager;
    
    public ConnectionManager getConnectionManager() {
		return mConnectionManager;
	}

	public CallLogManager getCallLogManager() {
		return mCallLogManager;
	}

    private DeviceModule() {
        super(MODULE, new String[]{FEATURE_CALLLOG, FEATURE_WEATHER, FEATURE_BATTERY, FEATURE_TIME, FEATURE_UNBIND});
    }
    
    public static DeviceModule getInstance(){
    	if(sInstance == null){
    		sInstance = new DeviceModule();
    	}
    	return sInstance;
    }

    protected void onCreate(Context context) {
        if (V) {
            Log.d(TAG, "DeviceModule created.");
        }
        mContext = context;
        
        //init connection manager
        mConnectionManager = ConnectionManager.getInstance(context);
        
        //init calllog manager
        mCallLogManager = CallLogManager.getInstance(context);
        
        //init battery manager
        mBatteryManager = BatteryInfoManager.init(mContext);
        
        
        if (!DefaultSyncManager.isWatch()) {
            registService(IDeviceRemoteService.DESPRITOR,
                    new DeviceRemoteServiceImpl(context));
        } else {
            registRemoteService(IDeviceRemoteService.DESPRITOR,
                    new RemoteBinderImpl(MODULE,
                    		IDeviceRemoteService.DESPRITOR));
        }
    }
    
    @Override
	protected Transaction createTransaction() {
		return new DeviceTransaction();
	}

	@Override
	protected void onConnectivityStateChange(boolean connected) {
		super.onConnectivityStateChange(connected);
		klilog.i("connection changed:" + connected);
		Intent intent = new Intent();
        intent.setAction(Commands.ACTION_BLUETOOTH_STATUS);
        intent.putExtra("data", connected);
        mContext.sendBroadcast(intent);
	}

	@Override
	protected void onFeatureStateChange(String feature, boolean enabled) {
        klilog.i("[watch] received feature changed to "+enabled+", "+feature);
        if (FEATURE_BATTERY.equals(feature) && !enabled) {
            Intent intent = new Intent();
            intent.setAction(Commands.getCmdAction(7));
            intent.putExtra("cmd", 7);
            JSONObject batteryInfo = new JSONObject();
            try {
                batteryInfo.put("level", 111);
                batteryInfo.put("scale", 100);
                batteryInfo.put("plugged", 0);
                batteryInfo.put("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
            } catch (JSONException e) {
                klilog.e("json, "+e.toString());
            }
            intent.putExtra("data", batteryInfo.toString());
            mContext.sendBroadcast(intent);
        }else if (FEATURE_UNBIND.equals(feature) && enabled) {
	        ConnectionManager.getInstance().device2Device(Commands.CMD_GLASS_UNBIND, Commands.ACTION_GLASS_UNBIND);
	    }
	}

	@Override
	protected void onClear(String address) {
		klilog.i("onClear.  address:"+address);
		mCallLogManager.clearReceived("onclear");
        mContext.sendBroadcast(new Intent(CLEAE_ACTION));//to notify BatteryControllerAnother
	}
	
	protected void onInit() {
		//send to IndroidWeather.apk; src/cn/ingenic/weather/CommandReceiver.java
		mContext.sendBroadcast(new Intent("cn.ingenic.action.weather.first.bind"));
		klilog.i("(first bind) send to notify IndroidWeather to refresh");
	}

}

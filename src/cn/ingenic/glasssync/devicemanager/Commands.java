package cn.ingenic.glasssync.devicemanager;

public class Commands {
	
	//sync
	public final static String ACTION_SYNC = "cn.ingenic.action.sync";
	
	//weather
	public final static String ACTION_WEATHER = "cn.ingenic.action.weather";
	public final static int CMD_INTERNET_REQUEST = 1;
	
	//device manager
	public final static String ACTION_DEVICE_MANAGER = "cn.ingenic.action.devicemanager";
	public final static int CMD_LOCK_SCREEN = 2;
	
	//call log
	public final static String ACTION_CALL_LOG= "cn.ingenic.action.calllog";
	public final static int CMD_CLEAR_CALL_LOG = 3;
	public final static int CMD_SYNC_MISS_CALL_NOTIFY = 4;
	public final static int CMD_SYNC_CALL_LOG = 5;
	public final static int CMD_SYNC_HAS_NEW_SYNC = 10;
	
	public final static int CMD_SYNC_CALL_LOG_FROM_WATCH = 9;
	public final static int CMD_SYNC_CALL_LOG_REQUEST = 11;
	public final static int CMD_SYNC_WATCH_ON_CLEAR = 12;
	
	//time
	public final static String ACTION_TIME = "cn.ingenic.action.time";
	public final static int CMD_GET_TIME = 6;
	
	//battery
	public final static String ACTION_BATTERY = "cn.ingenic.action.battery";
	public final static int CMD_GET_BATTERY = 7;
    public final static int CMD_REQUEST_BATTERY = 13;
    
    //ring and vibrat
    public final static int CMD_RING_AND_VIBRAT = 14;

	//bluetooth connection status 
	public final static String ACTION_BLUETOOTH_STATUS="cn.ingenic.action.bluetooth_status";
	public final static int CMD_GETBLUETOOTH_STATUS=8;

    //glass unbind
	public final static String ACTION_GLASS_UNBIND = "cn.ingenic.action.glass.unbind";
	public final static int CMD_GLASS_UNBIND = 15;

	//error code
	public final static String ERROR_NOT_CONNECT = "error_not_connect";
	
	public static String getCmdAction(int cmd) {
		String action = null;
		switch (cmd) {
		case CMD_INTERNET_REQUEST:
			action = ACTION_WEATHER;
			break;
		case CMD_LOCK_SCREEN:
		case CMD_RING_AND_VIBRAT:
			action = ACTION_DEVICE_MANAGER;
			break;
		case CMD_GET_BATTERY:
			action = ACTION_BATTERY;
			break;
		case CMD_GET_TIME:
			action = ACTION_TIME;
			break;
		case CMD_GETBLUETOOTH_STATUS:
		    action = ACTION_BLUETOOTH_STATUS;
		    break;

		case CMD_CLEAR_CALL_LOG:
		case CMD_SYNC_MISS_CALL_NOTIFY:
		case CMD_SYNC_CALL_LOG:
		case CMD_SYNC_CALL_LOG_FROM_WATCH:
		case CMD_SYNC_HAS_NEW_SYNC:
		case CMD_SYNC_CALL_LOG_REQUEST:
			action = ACTION_CALL_LOG;
			break;
		case CMD_GLASS_UNBIND:
			action = ACTION_GLASS_UNBIND;
			break;
		}
		return action;
	}
	
	public static String getCmdFeature(int cmd){
		String feature = DeviceModule.MODULE;
		switch (cmd) {
		case CMD_INTERNET_REQUEST:
			feature = DeviceModule.FEATURE_WEATHER;
			break;
		case CMD_SYNC_CALL_LOG:
			feature = DeviceModule.FEATURE_CALLLOG;
			break;
		case CMD_GET_BATTERY:
			feature = DeviceModule.FEATURE_BATTERY;
			break;
		case CMD_GET_TIME:
			feature = DeviceModule.FEATURE_TIME;
			break;
		case CMD_GLASS_UNBIND:
			feature = DeviceModule.FEATURE_UNBIND;
			break;
		}
		return feature;
		
	}
}

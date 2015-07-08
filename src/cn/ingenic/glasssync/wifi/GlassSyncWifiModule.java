package cn.ingenic.glasssync.wifi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.content.BroadcastReceiver;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.ConnectivityManager;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.net.NetworkInfo;
import java.util.List;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
public class GlassSyncWifiModule extends SyncModule {
    private static final String TAG = "GlassSyncWifiModule";
    private static final String LETAG = "GSWFMD";

    private static final String GSWFMD_SSID = "gswfmd_ssid";
    private static final String GSWFMD_PSD = "gswfmd_psd";
    private static final String GSWFMD_MGMT = "gswfmd_mgmt";
    private static final String GSWFMD_PROP = "gswfmd_prop";
    private static final String GSWFMD_GRP = "gswfmd_grp";
    private static final String GSWFMD_CONN = "gswfmd_conn";

    private static final String GSWFMD_CMD = "gswfmd_cmd";
    private static final String GSWFMD_RQWF = "gswfmd_rqwf";
    private static final String GSWFMD_SDWF = "gswfmd_sdwf";
    private static final String GSWFMD_RESULT = "gswfmd_result";
    private Context mContext;
    private static GlassSyncWifiModule sInstance;
    private final IntentFilter mIntentFilter;
    private String mSsid;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		NetworkInfo networkInfo = (NetworkInfo)
		    intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

		WifiManager wifiService = (WifiManager)mContext.getSystemService(mContext.WIFI_SERVICE); 
		String wifiSSID = wifiService.getConnectionInfo().getSSID();
		wifiSSID = wifiSSID.substring(1, wifiSSID.length()-1);
		if(!wifiSSID.equals(mSsid)){
		    return;
		}
		if(networkInfo.getState() == NetworkInfo.State.CONNECTED){
		    sendResult(true);
		}else if(networkInfo.getState() == NetworkInfo.State.DISCONNECTED){
		    sendResult(false);
		}
	    }
	};
    private GlassSyncWifiModule(Context context){
	super(LETAG, context);
	mContext = context;
	mIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
	mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public static GlassSyncWifiModule getInstance(Context c) {
	if (null == sInstance){
	    sInstance = new GlassSyncWifiModule(c);
	}
	return sInstance;
    }

    @Override
	protected void onCreate() {
    }

    private void process_wifi(SyncData data){
	mSsid = data.getString(GSWFMD_SSID);
	String pwd = data.getString(GSWFMD_PSD);

	WifiManager wifiManager = (WifiManager)mContext.getSystemService(mContext.WIFI_SERVICE); 
	WifiConfiguration newwfcfg = new WifiConfiguration();

	if (wifiManager.isWifiEnabled() == false){
	    wifiManager.setWifiEnabled(true);
	}

	newwfcfg.SSID = "\"" + mSsid + "\"";
 
	final List<ScanResult> results = wifiManager.getScanResults();
	if(results != null){
	    for(ScanResult result : results){
		if (result.SSID == null || result.SSID.length() == 0
		    || result.capabilities.contains("[IBSS]")) {
		    continue;
		}
		if(result.SSID.equals(mSsid)){
		    newwfcfg = checkWifi(result,newwfcfg,pwd);
		    wifiManager.disconnect();
		    wifiManager.connect(newwfcfg, null);
		}

	    }
	}
    }
    private WifiConfiguration checkWifi(ScanResult result,WifiConfiguration config,String pwd){
        if (result.capabilities.contains("WEP")) {
	    config.allowedKeyManagement.set(KeyMgmt.NONE);
	    config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
	    config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
	    int length = pwd.length();
	    if ((length == 10 || length == 26 || length == 58)
		&& pwd.matches("[0-9A-Fa-f]*")) {
		config.wepKeys[0] = pwd;
	    } else {
		config.wepKeys[0] = '"' + pwd + '"';
	    }
        } else if (result.capabilities.contains("PSK")) {
	    config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
	    if (pwd.matches("[0-9A-Fa-f]{64}")) {
		config.preSharedKey = pwd;
	    } else {
		config.preSharedKey = '"' + pwd + '"';
	    }
        } else if (result.capabilities.contains("EAP")) {
        }else{
	    config.allowedKeyManagement.set(KeyMgmt.NONE);
	}
	return config;
    }

    @Override
	protected void onRetrive(SyncData data) {
	String cmd = data.getString(GSWFMD_CMD);
	if (cmd.equals(GSWFMD_SDWF)){
	    process_wifi(data);
	}
    }
    private void sendResult(boolean result){

	SyncData data = new SyncData();
	data.putString(GSWFMD_CMD,GSWFMD_SDWF);
	data.putBoolean(GSWFMD_RESULT, result);
	try{
	    send(data);
	}catch(SyncException e) {
	}
    }
}
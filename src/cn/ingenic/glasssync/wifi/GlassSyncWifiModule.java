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
import java.util.List;

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

    private Context mContext;
    private static GlassSyncWifiModule sInstance;
    private WifiModuleReceiver nReceiver;

    private GlassSyncWifiModule(Context context){
	super(LETAG, context);
	mContext = context;
    }

    public static GlassSyncWifiModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncWifiModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
        nReceiver = new WifiModuleReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("cn.ingenic.glasssync.wifi.REQUEST_WIFI_INFO");
        mContext.registerReceiver(nReceiver,filter);
    }

    // public void send_Wifi(String ssid, String psd, int connect){
    // 	SyncData data = new SyncData();

    // 	data.putString(GSWFMD_SSID, ssid);
    // 	data.putString(GSWFMD_PSD, psd);
    // 	data.putInt(GSWFMD_CONN, connect);

    // 	try {
    // 	    Log.e(TAG, "send wifi() " + ssid);
    // 	    send(data);
    // 	} catch (SyncException e) {
    // 	    Log.e(TAG, "" + e);
    // 	}
    // }

    private void process_wifi(SyncData data){
	Log.e(TAG, "process_wifi");

	String ssid = data.getString(GSWFMD_SSID);
	String pswd = data.getString(GSWFMD_PSD);
	String prot = data.getString(GSWFMD_PROP);
	String mgmt = data.getString(GSWFMD_MGMT);
	String group = data.getString(GSWFMD_GRP);
	int connect = data.getInt(GSWFMD_CONN);

	Log.e(TAG, "ssid:" + ssid + " pswd:" + pswd + " prot:" + prot + " mgnt:" + mgmt + " group:" + group + " connect:" + connect);

	WifiManager wifi_service = (WifiManager)mContext.getSystemService(mContext.WIFI_SERVICE); 
	WifiConfiguration newwfcfg = new WifiConfiguration();

	if (wifi_service.isWifiEnabled() == false){
	    Log.e(TAG, "enable the wifi");
	    wifi_service.setWifiEnabled(true);
	}

	newwfcfg.SSID = "\"" + ssid + "\"";

	if (mgmt.contains("WPA_PSK")){
	    newwfcfg.preSharedKey = "\"" + pswd + "\""; 
            newwfcfg.hiddenSSID = true;
	    if (connect == 0)
		newwfcfg.status = WifiConfiguration.Status.ENABLED;
	    else
		newwfcfg.status = WifiConfiguration.Status.CURRENT;

	    if (group.contains("CCMP")){
		newwfcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		newwfcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	    }
	    if (group.contains("TKIP")){
		newwfcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		newwfcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
	    }

	    newwfcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

	    if (prot.contains("RSN"))
		newwfcfg.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	    if (prot.contains("WPA"))
		newwfcfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	}

	wifi_service.addNetwork(newwfcfg);

	List<WifiConfiguration> wfcfg = wifi_service.getConfiguredNetworks();
	for (WifiConfiguration w : wfcfg){
	    //Log.e(TAG, "ssid:" + w.SSID + " id:" + w.networkId);
	    if (w.SSID.equals(newwfcfg.SSID)){
		Log.e(TAG, "enable network " + newwfcfg.SSID);
		wifi_service.enableNetwork(w.networkId, true);
	    }
	}
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String cmd = data.getString(GSWFMD_CMD);
	if (cmd.equals(GSWFMD_SDWF)){
	    process_wifi(data);
	}
    }

    private void request_Wifi(){
	SyncData data = new SyncData();

	data.putString(GSWFMD_CMD, GSWFMD_RQWF);

	try {
	    Log.e(TAG, "send request wifi");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    class WifiModuleReceiver extends BroadcastReceiver{
	private String TAG = "WifiModuleReceiver";

        @Override
	public void onReceive(Context context, Intent intent) {
	    Log.e(TAG, "onReceive");
	    request_Wifi();
        }
    }
}
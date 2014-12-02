package cn.ingenic.glasssync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.DefaultSyncManager.OnFileChannelCallBack;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public abstract class Module {
	private static final String TAG = "Module";
	
	private final String mName;
	private final String[] mFeatures;
    private boolean mSyncEnabled;
	public Module(String name) {
		this(name, new String[]{name});
	}
	
	public Module(String name, String[] features) {
		if (TextUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Module name can not be empty.");
		}
		
		if (features == null || features.length == 0) {
			throw new IllegalArgumentException("Module features can not be empty.");
		}
		
		mName = name;
		mFeatures = features;
		mSyncEnabled = true;
	}
	
    public void setSyncEnable(boolean enabled){
	    mSyncEnabled = enabled;
	}
    public boolean getSyncEnable(){
	    return mSyncEnabled;
	}

	protected void onFeatureStateChange(String feature, boolean enabled) {
	}
	
	protected void onInit() {
	}

	protected void onClear(String address) {
	}
	
	protected void onConnectivityStateChange(boolean connected) {
		for (RemoteBinderImpl rbl : mRemoteServiceMap.values()) {
			rbl.onConnectivityChange(connected);
		}
	}
	
	protected void onModeChanged(int mode) {
	}
	
	public boolean hasFeature(String feature) {
		for (String f : mFeatures) {
			if (f.equals(feature)) {
				return true;
			}
		}
		
		return false;
	}
	
	public OnFileChannelCallBack getFileChannelCallBack() {
		return null;
	}
	
	public OnChannelCallBack getChannelCallBack(UUID uuid) {
		return null;
	}
	
	protected Transaction createTransaction() {
		return null;
	}
	
	public ILocalBinder getService(String descriptor) {
		return mServiceMap.get(descriptor);
	}
	
	public IRemoteBinder getRemoteService(String descriptor) {
		return mRemoteServiceMap.get(descriptor);
	}
	
	public final String getName() {
		return mName;
	}
	
	public final String[] getFeatures() {
		return mFeatures;
	}
	
	private Map<String, ILocalBinder> mServiceMap = new HashMap<String, ILocalBinder>();
	private Map<String, RemoteBinderImpl> mRemoteServiceMap = new HashMap<String, RemoteBinderImpl>();
	
	protected void onCreate(Context context) {
	}
	
	private boolean checkServiceDespritor(String despritor) {
		if (mServiceMap.containsKey(despritor) || TextUtils.isEmpty(despritor)) {
			Log.e(TAG, "invalid despritor:" + despritor);
			return false;
		}
		
		return true;
	}
	
	private boolean checkRemoteServiceDespritor(String despritor) {
		if (mRemoteServiceMap.containsKey(despritor) || TextUtils.isEmpty(despritor)) {
			Log.e(TAG, "invalid despritor:" + despritor);
			return false;
		}
		
		return true;
	}
	
	protected boolean registService(String descriptor, ILocalBinder service) {
		if (!checkServiceDespritor(descriptor)) {
			return false;
		}
		
		mServiceMap.put(descriptor, service);
		return true;
	}
	
	protected boolean registRemoteService(String descriptor, RemoteBinderImpl remote) {
		if (!checkRemoteServiceDespritor(descriptor)) {
			return false;
		}
		
		mRemoteServiceMap.put(descriptor, remote);
		return true;
	}
}

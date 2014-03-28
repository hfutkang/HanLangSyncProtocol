package cn.ingenic.glasssync.appmanager;

import java.io.File;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

import android.content.pm.IPackageStatsObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.format.Formatter;
import android.util.Log;

public class ApplicationManager {

	private final boolean DEBUG = true;
	private final String APP = "ApplicationManager";

	private static ApplicationManager mApplicationManager = null;
	private Context mContext;
	private PackageReceiver mPackageReceiver;

	private int mRetrieveFlags;
	private Map<String, AppEntry> mEntriesMap = new HashMap<String, AppEntry>();
	private PackageEntryListListener mPackageEntryListListener;
	private PackageManager mPm;
	private CallBackHandler mCallBackHandler;

	private boolean mMapIsOk = false;
	private ArrayList<Integer> mCommonList = new ArrayList<Integer>();
	public ArrayList<String> mAddList = new ArrayList<String>();
	public Map<String,AppEntry> mRemoveMap=new HashMap<String,AppEntry>();
	public ArrayList<String> mChangeList = new ArrayList<String>();

	public interface MainCommon {
		public final int GET_APPLICATION_DATAS = 0;
	}

	public interface CallBack {
		public final int APPLICATION_INFO_SEND = 0;
	}

	public class CallBackHandler extends Handler {

		public CallBackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (DEBUG)
				Log.i(APP, "CallBackHandler msg is : " + msg.arg1);
			switch (msg.what) {
			case CallBack.APPLICATION_INFO_SEND:
				int c = msg.arg1;
				if (msg.arg1 != SyncModule.SUCCESS) {
					Log.e(APP,
							"when send application infos wrong!!! wrong number is :"
									+ c);
				}
				break;
			}
		}

	}

	public static ApplicationManager getInstance(Context context) {
		if (mApplicationManager == null) {
			mApplicationManager = new ApplicationManager(context);
		}
		return mApplicationManager;
	}

	public ApplicationManager(Context context) {
		this.mContext = context;
		if (mPackageReceiver == null) {
			mPackageReceiver = new PackageReceiver();
			mPackageReceiver.registerReceiver();
			if (DEBUG)
				Log.i(APP, "ApplicationManager instance registerReceiver .");
		}
		HandlerThread ht = new HandlerThread("callback_thread");
		ht.start();
		mCallBackHandler = new CallBackHandler(ht.getLooper());

		// cacheApplication(Common.Application.INSTALL_APPLICATION_CACHE_COMMIN);//
		// yangliu
		// test
	}

	// private int mSendCommon = -1;

	public void cacheApplication(int common) {
		if (DEBUG)
			Log.i(APP,
					"cacheApplication common is :"
							+ (common == Common.Application.ALL_APPLICATION_CACHE_COMMON ? " all application ."
									: " installed application"));
		Log.i(APP, "mMapIsOK is :" + mMapIsOk);
		synchronized (mEntriesMap) {
			if (mMapIsOk) {
				AppEntryMapIsReady(common);
			} else {
				mCommonList.add(common);
				mEntriesMap.clear();
				init();
			}
		}

	}

	public void sendUninstallIntent(String packageName) {
		Uri packageURI = Uri.parse("package:" + packageName);
		Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE,
				packageURI);
		uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
		uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		uninstallIntent.putExtra("show_dialog", false);
		mContext.startActivity(uninstallIntent);
		Log.e("ApplicationManager", "after send intent package Name is :"
				+ packageName);
	}

	public void onConnect(boolean connect) {
		if (DEBUG)
			Log.i(APP, "onConnect  connect is :" + connect);
		if (connect) {
			if (mPackageReceiver == null) {
				mPackageReceiver = new PackageReceiver();
				mPackageReceiver.registerReceiver();
			}
		} else {
			if (mPackageReceiver != null) {
				mPackageReceiver.unRegisterReceiver();
				mPackageReceiver = null;
			}
			// synchronized (mEntriesMap) {
			// mEntriesMap.clear();
			// }
		}

	}

	private void init() {
		if (DEBUG)
			Log.i(APP, "init .");
		if (UserHandle.myUserId() == 0) {
			mRetrieveFlags = PackageManager.GET_UNINSTALLED_PACKAGES
					| PackageManager.GET_DISABLED_COMPONENTS
					| PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS;
		} else {
			mRetrieveFlags = PackageManager.GET_DISABLED_COMPONENTS
					| PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS;
		}
		mPm = mContext.getPackageManager();

		mPackageEntryListListener = new PackageEntryListListener();

		List<ApplicationInfo> mApplications = mPm
				.getInstalledApplications(mRetrieveFlags);
        
		mPackageEntryListListener.onInit();
		
		int size = mApplications.size();
		Log.e("yangliu", "size is :" + size);
		for (int i = 0; i < size; i++) {
			ApplicationInfo info = mApplications.get(i);
			mPackageEntryListListener.onEnterOneEntry(info);
		}

	}

	private void AppEntryMapIsReady(int common) {
		SyncData sendData = new SyncData();
		ArrayList<AppSendEntry> eList = new ArrayList<AppSendEntry>();

		for (String pkgName : mEntriesMap.keySet()) {
			AppEntry entry = (AppEntry) mEntriesMap.get(pkgName);

			if (common == Common.Application.INSTALL_APPLICATION_CACHE_COMMIN
					&& !entry.isSystem) {

				AppSendEntry se = new AppSendEntry(entry.icon, entry.label,
							entry.sizeStr, pkgName,entry.isSystem, entry.info.enabledSetting);
				
				eList.add(se);
			} else if (common == Common.Application.ALL_APPLICATION_CACHE_COMMON) {
				AppSendEntry se = new AppSendEntry(entry.icon, entry.label,
						entry.sizeStr, pkgName,entry.isSystem, entry.info.enabledSetting);
				eList.add(se);
			}

		}
		if (DEBUG)
			Log.i(APP, "finally send entry size is :" + eList.size());

		sendData.putString(
				Common.AppDataKey.COMMON,
				(common == Common.Application.INSTALL_APPLICATION_CACHE_COMMIN) ? Common.RECEIVE_INSTALL_DATAS
						: Common.RECEIVE_ALL_DATAS);
		sendData.putDataArray(Common.AppDataKey.ALL_APPLICATION,
				eList.toArray(new SyncData[eList.size()]));
		send(sendData);
	}

	private boolean AppEntryMapIsReady(int mode, String pkgName) {
		String common=null;
		if(mode==Common.Operate.ADD){
			common=Common.ADD_APPLICATION_MESSAGE;
		}else if(mode==Common.Operate.REMOVE){
			common=Common.REMOVE_APPLICATION_MESSAGE;
			AppEntry ae=mRemoveMap.get(pkgName);
			AppSendEntry se = new AppSendEntry(ae.icon, ae.label,
					ae.sizeStr, pkgName,ae.isSystem, ae.info.enabledSetting);
			SyncData data=new SyncData();
			data.putString(Common.AppDataKey.COMMON, common);
			data.putDataArray(Common.AppDataKey.ALL_APPLICATION, new SyncData[]{se});
			send(data);
			return true;
			
		}else if(mode==Common.Operate.CHANGE){
			common=Common.CHANGE_APPLICATION_MESSAGE;
		}else{
			Log.e("ApplicationManager","find wrong mode mode is :"+mode);
		}
		
		AppEntry entry = mEntriesMap.get(pkgName);
		if (entry == null) {
			Log.e("ApplicationManager",
					"when send mode is :"
							+ mode
							+ " application info find entry is null !!!!!! and package name is :"
							+ pkgName);
			return false;
		}
		
		AppSendEntry se = new AppSendEntry(entry.icon, entry.label,
				entry.sizeStr, pkgName,entry.isSystem, entry.info.enabledSetting);
		SyncData data=new SyncData();
		data.putString(Common.AppDataKey.COMMON, common);
		data.putDataArray(Common.AppDataKey.ALL_APPLICATION, new SyncData[]{se});
		send(data);
		return true;

	}

	private void send(SyncData data) {
		if (DEBUG)
			Log.i(APP, "Send .");
		AppManagerModule amm = AppManagerModule.getInstance(mContext);
		SyncData.Config config = new SyncData.Config();

		Message m = mCallBackHandler.obtainMessage();
		m.what = CallBack.APPLICATION_INFO_SEND;
		config.mmCallback = m;
		data.setConfig(config);
		try {
			amm.send(data);
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
		public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
			boolean sizeChanged = false;
			synchronized (mEntriesMap) {
				AppEntry entry = (AppEntry) mEntriesMap.get(stats.packageName);
				if (entry != null) {
						entry.sizeStale = false;
						entry.sizeLoadStart = 0;
						long externalCodeSize = stats.externalCodeSize
								+ stats.externalObbSize;
						long externalDataSize = stats.externalDataSize
								+ stats.externalMediaSize;
						long newSize = externalCodeSize + externalDataSize
								+ getTotalInternalSize(stats);
						if (entry.size != newSize
								|| entry.cacheSize != stats.cacheSize
								|| entry.codeSize != stats.codeSize
								|| entry.dataSize != stats.dataSize
								|| entry.externalCodeSize != externalCodeSize
								|| entry.externalDataSize != externalDataSize
								|| entry.externalCacheSize != stats.externalCacheSize) {
							entry.size = newSize;
							entry.cacheSize = stats.cacheSize;
							entry.codeSize = stats.codeSize;
							entry.dataSize = stats.dataSize;
							entry.externalCodeSize = externalCodeSize;
							entry.externalDataSize = externalDataSize;
							entry.externalCacheSize = stats.externalCacheSize;
							entry.sizeStr = getSizeStr(entry.size);
							entry.internalSize = getTotalInternalSize(stats);
							entry.internalSizeStr = getSizeStr(entry.internalSize);
							entry.externalSize = getTotalExternalSize(stats);
							entry.externalSizeStr = getSizeStr(entry.externalSize);
							sizeChanged = true;
						}

					if (sizeChanged)
						mPackageEntryListListener.onMapFillOneEntrySize(stats);
				}

			}
		}
	};

	private long getTotalInternalSize(PackageStats ps) {
		if (ps != null) {
			return ps.codeSize + ps.dataSize;
		}
		return SIZE_INVALID;
	}

	private String getSizeStr(long size) {
		if (size >= 0) {
			return Formatter.formatFileSize(mContext, size);
		}
		return null;
	}

	private long getTotalExternalSize(PackageStats ps) {
		if (ps != null) {
			// We also include the cache size here because for non-emulated
			// we don't automtically clean cache files.
			return ps.externalCodeSize + ps.externalDataSize
					+ ps.externalCacheSize + ps.externalMediaSize
					+ ps.externalObbSize;
		}
		return SIZE_INVALID;
	}

	// private void reloadOneAppEntry(PackageStats stats){
	// AppEntry entry = mEntriesMap.get(stats.packageName);
	// synchronized(entry){
	//
	// Log.d("yangliu", "entry is :" + entry.cacheSize);
	// Log.d("yangliu", "codeSize is :" + entry.codeSize);
	// Log.d("yangliu", "dataSize is :" + entry.dataSize);
	// Log.d("yangliu", "externalCacheSize is :" + entry.externalCacheSize);
	// Log.d("yangliu", "externalCodeSize is :" + entry.externalCodeSize);
	// Log.d("yangliu", "externalDataSize is :" + entry.externalDataSize);
	// Log.d("yangliu", "externalSize is :" + entry.externalSize);
	// Log.d("yangliu", "externalSizeStr is :" + entry.externalSizeStr);
	// Log.d("yangliu", "internalSize is :" + entry.internalSize);
	// Log.d("yangliu", "internalSizeStr is :" + entry.internalSizeStr);
	// Log.d("yangliu", "label is :" + entry.label);
	// Log.d("yangliu", "normalizedLabel is :" + entry.normalizedLabel);
	// Log.d("yangliu", "size is :" + entry.size);
	// Log.d("yangliu", "sizeLoadStart is :" + entry.sizeLoadStart);
	// Log.d("yangliu", "sizeStr is :" + entry.sizeStr);
	// Log.d("yangliu","icon is :"+entry.icon);
	// }
	//
	// }

	// private void refaush() {
	// synchronized (mEntriesMap) {
	// if (mEntriesMap.size() != 0)
	// mEntriesMap.clear();
	// mMapIsOk=false;
	// init();
	// }
	// }

	private void reInit() {
		synchronized (mEntriesMap) {
			mEntriesMap.clear();
			mMapIsOk = false;
			init();
		}
	}

	private class PackageReceiver extends BroadcastReceiver {

		public void registerReceiver() {
			IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
			filter.addDataScheme("package");
			mContext.registerReceiver(this, filter);
			// Register for events related to sdcard installation.
			IntentFilter sdFilter = new IntentFilter();
			sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
			sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
			mContext.registerReceiver(this, sdFilter);
		}

		public void unRegisterReceiver() {
			mContext.unregisterReceiver(this);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String actionStr = intent.getAction();
			Log.i(APP,"onReceiver actionStr is :"+actionStr);
			if (Intent.ACTION_PACKAGE_ADDED.equals(actionStr)) {
				Uri data = intent.getData();
				String pkgName = data.getEncodedSchemeSpecificPart();			
//				reInit();
				synchronized(mEntriesMap){
					mPackageEntryListListener.onAddOneApp(pkgName);
				}
				mAddList.add(pkgName);
			} else if (Intent.ACTION_PACKAGE_REMOVED.equals(actionStr)) {
				Uri data = intent.getData();
				String pkgName = data.getEncodedSchemeSpecificPart();
				AppEntry ae=mEntriesMap.get(pkgName);
				
				reInit();
				mRemoveMap.put(pkgName, ae);
			} else if (Intent.ACTION_PACKAGE_CHANGED.equals(actionStr)) {
				Uri data = intent.getData();
				String pkgName = data.getEncodedSchemeSpecificPart();
//				reInit();
				synchronized(mEntriesMap){
					mPackageEntryListListener.onChangeOneApp(pkgName);
				}
				mChangeList.add(pkgName);
			} else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
					.equals(actionStr)
					|| Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
							.equals(actionStr)) {
				// When applications become available or unavailable (perhaps
				// because
				// the SD card was inserted or ejected) we need to refresh the
				// AppInfo with new label, icon and size information as
				// appropriate
				// given the newfound (un)availability of the application.
				// A simple way to do that is to treat the refresh as a package
				// removal followed by a package addition.
				// String pkgList[] = intent
				// .getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
				// if (pkgList == null || pkgList.length == 0) {
				// // Ignore
				// return;
				// }
				// boolean avail = Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
				// .equals(actionStr);
				// if (avail) {
				// for (String pkgName : pkgList) {
				// invalidatePackage(pkgName);
				// }
				// }
			}

		}

	};

	private interface IPackageEntryMapListener {
		public void onMapClear();
		public void onInit();
		public void onAddOneApp(String pkgName);
		public void onRemoveOneApp(String pkgName);
		public void onChangeOneApp(String pkgName);

		public void onMapFillOneEntrySize(PackageStats stats);

		public void onMapSuccess();

		public void onEnterOneEntry(ApplicationInfo info);

	}

	private class PackageEntryListListener implements IPackageEntryMapListener {

		private ArrayList<String> mmFirstEntiresKeyList ;

		@Override
		public void onMapClear() {
		}

		@Override
		public void onMapFillOneEntrySize(PackageStats stats) {
			AppEntry entry = (AppEntry) mEntriesMap.get(stats.packageName);

			synchronized (entry) {
				mmFirstEntiresKeyList.remove(stats.packageName);
				if (DEBUG)
					Log.i(APP, "fill all size in a entry package name is :"
							+ stats.packageName + " and last size is :"
							+ mmFirstEntiresKeyList.size());
				if (mmFirstEntiresKeyList.size() == 0) {
					onMapSuccess();
				}
			}
		}

		@Override
		public void onMapSuccess() {
			if (DEBUG)
				Log.i(APP, "onMapSuccess .");
			mMapIsOk = true;
			mapSuccess();
		}

		@Override
		public void onEnterOneEntry(ApplicationInfo info) {
			AppEntry entry = new AppEntry(mContext, info, mEntriesMap.size());
			mEntriesMap.put(info.packageName, entry);
			entry.ensureIconLocked(mContext, mPm);
			mPm.getPackageSizeInfo(info.packageName, mStatsObserver);
			mmFirstEntiresKeyList.add(info.packageName);
			if (DEBUG)
				Log.i(APP, "after enter one appEntry package name is :"
						+ info.packageName);
		}

		@Override
		public void onInit() {
			mmFirstEntiresKeyList = new ArrayList<String>();
		}

		@Override
		public void onAddOneApp(String pkgName) {
			try {
				ApplicationInfo info=mPm.getApplicationInfo(pkgName, mRetrieveFlags);
				AppEntry entry = new AppEntry(mContext, info, mEntriesMap.size());
				mEntriesMap.put(info.packageName, entry);
				entry.ensureIconLocked(mContext, mPm);
				
				mPm.getPackageSizeInfo(info.packageName, mStatsObserver);
				if(mmFirstEntiresKeyList==null)mmFirstEntiresKeyList = new ArrayList<String>();
				mmFirstEntiresKeyList.add(pkgName);
				
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			
		}

		@Override
		public void onRemoveOneApp(String pkgName) {
			
		}

		@Override
		public void onChangeOneApp(String pkgName) {
			try {
				mEntriesMap.remove(pkgName);
				ApplicationInfo info=mPm.getApplicationInfo(pkgName, mRetrieveFlags);
				AppEntry entry = new AppEntry(mContext, info, mEntriesMap.size());
				
				mEntriesMap.put(info.packageName, entry);
				entry.ensureIconLocked(mContext, mPm);
				
				mPm.getPackageSizeInfo(info.packageName, mStatsObserver);
				if(mmFirstEntiresKeyList==null)mmFirstEntiresKeyList = new ArrayList<String>();
				mmFirstEntiresKeyList.add(pkgName);
				
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

			
		}

	}

	private void mapSuccess() {
		if (mCommonList.size() != 0) {
			// normal common set
			ArrayList<Integer> normal = new ArrayList<Integer>();
			normal.add(Common.SimpleBase.INSTALL);
			normal.add(Common.SimpleBase.ALL);

			for (int i = 0; i < mCommonList.size(); i++) {
				int common = mCommonList.get(i);
				if (normal.contains(common)) {
					normal.remove(common);
					AppEntryMapIsReady(common);
				}
			}
			mCommonList.clear();
		}

		if (mAddList.size() != 0) {
			for (int i = 0; i < mAddList.size(); i++) {
				String pkgName = mAddList.get(i);
				boolean result=AppEntryMapIsReady(Common.Operate.ADD,pkgName);
				if(result)mAddList.remove(pkgName);
			}
			
		}
		
		if (mRemoveMap.size()!=0) {
			for (String pkgName:mRemoveMap.keySet()) {
				boolean result=AppEntryMapIsReady(Common.Operate.REMOVE,pkgName);
				if(result)mRemoveMap.remove(pkgName);
			}
			
		}
		
		if (mChangeList.size() != 0) {
			for (int i = 0; i < mChangeList.size(); i++) {
				String pkgName = mChangeList.get(i);
				boolean result=AppEntryMapIsReady(Common.Operate.CHANGE,pkgName);
				if(result)mChangeList.remove(pkgName);
			}
		}
	}

	static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern
			.compile("\\p{InCombiningDiacriticalMarks}+");

	static final int SIZE_UNKNOWN = -1;
	static final int SIZE_INVALID = -2;

	public static String normalize(String str) {
		String tmp = Normalizer.normalize(str, Form.NFD);
		return REMOVE_DIACRITICALS_PATTERN.matcher(tmp).replaceAll("")
				.toLowerCase();
	}

	public static class SizeInfo {
		long cacheSize;
		long codeSize;
		long dataSize;
		long externalCodeSize;
		long externalDataSize;

		// This is the part of externalDataSize that is in the cache
		// section of external storage. Note that we don't just combine
		// this with cacheSize because currently the platform can't
		// automatically trim this data when needed, so it is something
		// the user may need to manage. The externalDataSize also includes
		// this value, since what this is here is really the part of
		// externalDataSize that we can just consider to be "cache" files
		// for purposes of cleaning them up in the app details UI.
		long externalCacheSize;
	}

	public static class AppEntry extends SizeInfo {
		public final File apkFile;
		public final long id;
		public String label;
		public long size;
		long internalSize;
		long externalSize;

		boolean mounted;

		String getNormalizedLabel() {
			if (normalizedLabel != null) {
				return normalizedLabel;
			}
			normalizedLabel = normalize(label);
			return normalizedLabel;
		}

		// Need to synchronize on 'this' for the following.
		ApplicationInfo info;
		Drawable icon;
		String sizeStr;
		String internalSizeStr;
		String externalSizeStr;
		boolean sizeStale;
		long sizeLoadStart;

		String normalizedLabel;
		boolean isSystem = true;

		AppEntry(Context context, ApplicationInfo info, long id) {
			apkFile = new File(info.sourceDir);
			this.id = id;
			this.info = info;
			this.size = SIZE_UNKNOWN;
			this.sizeStale = true;
			ensureLabel(context);
			ensureAttribute();
			
		}

		void ensureAttribute() {
			int s = info.flags & ApplicationInfo.FLAG_SYSTEM;
			if (s == 1) {
				isSystem = true;
			} else {
				isSystem = false;
			}
		}

		void ensureLabel(Context context) {
			if (this.label == null || !this.mounted) {
				if (!this.apkFile.exists()) {
					this.mounted = false;
					this.label = info.packageName;
				} else {
					this.mounted = true;
					CharSequence label = info.loadLabel(context
							.getPackageManager());
					this.label = label != null ? label.toString()
							: info.packageName;
				}
			}
		}

		boolean ensureIconLocked(Context context, PackageManager pm) {
			if (this.icon == null) {
				if (this.apkFile.exists()) {
					this.icon = this.info.loadIcon(pm);
					return true;
				} else {
					this.mounted = false;
					this.icon = context
							.getResources()
							.getDrawable(
									com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon);
				}
			} else if (!this.mounted) {
				// If the app wasn't mounted but is now mounted, reload
				// its icon.
				if (this.apkFile.exists()) {
					this.mounted = true;
					this.icon = this.info.loadIcon(pm);
					return true;
				}
			}
			return false;
		}
	}

}

package cn.ingenic.glasssync.appmanager;

import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public class AppManagerModule extends SyncModule{
	private final boolean DEBUG=true;
	private final String APP="ApplicationManager";
	
	private static AppManagerModule mAppManagerModule=null;
	private static final String APP_MANAGER_NAME="application_manager";
	private Context mContext;
	private ApplicationManager mApplicationManager;
	
	public static AppManagerModule getInstance(Context context){
		if(mAppManagerModule==null){
			mAppManagerModule=new AppManagerModule(context);
		}
		return mAppManagerModule;
	}

	public AppManagerModule(Context context) {
		super(APP_MANAGER_NAME, context);
		this.mContext=context;
	}

	@Override
	protected void onCreate() {
		mApplicationManager=new ApplicationManager(mContext);
	}

	@Override
	protected void onConnectionStateChanged(boolean connect) {
		super.onConnectionStateChanged(connect);
		mApplicationManager.onConnect(connect);
	}

	@Override
	protected void onRetrive(SyncData data) {
		super.onRetrive(data);
		String common=data.getString(Common.AppDataKey.COMMON);
		if(DEBUG)Log.i(APP,"onRetrive common is :"+common);
		Common.parse(mApplicationManager,data, common);
	}
	
	
	
	
}

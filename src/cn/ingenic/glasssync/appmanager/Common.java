package cn.ingenic.glasssync.appmanager;

import java.util.ArrayList;

import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

public class Common {
	
	public static final String RECEIVE_ALL_DATAS="receiver_all_app_datas";
	public static final String RECEIVE_INSTALL_DATAS="receiver_install_app_datas";
	public static final String MESSAGE="message";
	public static final String UNINSTALL_MESSAGE="uninstall_message";
	public static final String ADD_APPLICATION_MESSAGE="add_application_message";
	public static final String REMOVE_APPLICATION_MESSAGE="remove_application_message";
	public static final String CHANGE_APPLICATION_MESSAGE="change_application_message";
	
	public static interface SimpleBase{
		public static final int INSTALL=0;
		public static final int ALL=1;
	}
	
	public static interface Operate{
		public static final int ADD=0;
		public static final int REMOVE=1;
		public static final int CHANGE=2;
	}
	
	public static interface Application{
		public static final int ALL_APPLICATION_CACHE_COMMON=SimpleBase.ALL;
		public static final int INSTALL_APPLICATION_CACHE_COMMIN=SimpleBase.INSTALL;
	}
	
	public static interface AppEntryKey{
		public static final String DRAWABLE="drawable";
		public static final String APPLICATION_NAME="application_name";
		public static final String APPLICATION_SIZE="application_size";
		public static final String PACKAGE_NAME="package_name";
		public static final String IS_SYSTEM="is_system";
		public static final String SYSTEM_SETTING_ENABLE="system_setting_enable";
		
	}
	
	public static interface AppDataKey{
		public static final String ALL_APPLICATION="all_application_datas";
		public static final String COMMON="common";
		public static final String PACKAGE_NAME="package_name";
	}
	
	public static interface MessageKey{
		public static final String GET_APP_INFOS_KEY="mode";
	}
	
	public static interface MessageValue{
		public static final int INSTALL=SimpleBase.INSTALL;
		public static final int ALL=SimpleBase.ALL;
	}
	
	public static void parse(ApplicationManager am,SyncData data,String common){
		if(common.equals(MESSAGE)){
			int mode=data.getInt(Common.MessageKey.GET_APP_INFOS_KEY);
			Log.i("ApplicationManager","when parse common accept mode is :"+mode);
			am.cacheApplication(mode);
		}else if(common.equals(UNINSTALL_MESSAGE)){
			String packageName=data.getString(AppDataKey.PACKAGE_NAME);
			am.sendUninstallIntent(packageName);
		}

	}
	

}

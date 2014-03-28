package cn.ingenic.glasssync.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
/** 
 * @author kli & dfdun */
class UpdateUtils {
    
    static final String FILENAME_SAVED ="update.zip";
    static final String ENCODE="UTF-8";
    static final int FLAG_CHECK_UPDATE=1;
    static final int FLAG_DOWNLOAD_UPDATE=2;
    /** the URL to check update
     * */
    static final String URL_PRODUCTS_UPDATE="http://bocaidong.sinaapp.com/tmp/update_products_list.xml";

    private static final String PREFERENCE_NAME = "update_config";
    private static final String CONFIG_DOWNLOAD_ID = "download_id";
    private static final String CONFIG_UPDATE_INFO = "update_info";
    
    static String getCurrentVersion(){
        return get("ro.build.display.id","unknow");
    }

    static String get(String key){
        return SystemProperties.get(key);
    }

    static String get(String key, String def){
        String value = SystemProperties.get(key);
        if (TextUtils.isEmpty(value))
            return def;
        else
            return value;
    }
    
    public static void putDownloadInfo(Context context, long id, UpdateInfo info){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	SharedPreferences.Editor editor = pref.edit();
    	editor.putLong(CONFIG_DOWNLOAD_ID, id);
    	editor.putString(CONFIG_UPDATE_INFO, info == null ? "" : info.toString());
    	editor.commit();
    }
    
    public static long getDownloadId(Context context){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	return pref.getLong(CONFIG_DOWNLOAD_ID, 0);
    }
    
    public static UpdateInfo getUpdateInfoCache(Context context){
    	SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
    	return UpdateInfo.createFromString(pref.getString(CONFIG_UPDATE_INFO, ""));
    }
    
    public static void setUpdateInfoCacheNull(Context context){
        SharedPreferences pref= context.getSharedPreferences(PREFERENCE_NAME,0);
        SharedPreferences.Editor e=pref.edit();
        e.putString(CONFIG_UPDATE_INFO, "");
        e.commit();
    }
    
    public static void putStringToSP(Context con,String key, String value){
        SharedPreferences pref=con.getSharedPreferences(PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }
    
    public static String getStringFromSP(Context con,String key){
        SharedPreferences pref=con.getSharedPreferences(PREFERENCE_NAME, 0);
        return pref.getString(key, " ");
    }
    
    //:TODO
    public static boolean isNeedUpdate(String newVersion, String oldVersion) {
        return true;
    }
}

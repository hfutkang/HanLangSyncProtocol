package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;

import cn.ingenic.glasssync.lbs.GlassSyncLbsModule;

public class GlassSyncLbsManager{
    private static final String TAG = "GlassSyncLbsManager";
    public static String GLASS_SYNC_LBS_BD_SYNC = "cn.ingenic.glasssync.lbs.SYNC";

    private Context mContext;
    private static GlassSyncLbsManager sInstance;

    private GlassSyncLbsManager(Context context){
	mContext = context;

	GlassSyncLbsModule m = GlassSyncLbsModule.getInstance(mContext);
	Log.e(TAG, "GlassSyncLbsManager in");
    }

    public static GlassSyncLbsManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncLbsManager(c);

	return sInstance;
    }
}
package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class GlassSyncLbsModule extends SyncModule {
    private static final String TAG = "GlassSyncLbsModule";
    private static final String LETAG = "GSLBSMD";

    private static final String FLT1 = "FLT1";
    private static final String FLT2 = "FLT2";
    private static final String FLT3 = "FLT3";

    private Context mContext;
    private static GlassSyncLbsModule sInstance;

    private GlassSyncLbsModule(Context context){
	super(LETAG, context);
	mContext = context;
    }

    public static GlassSyncLbsModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncLbsModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	float f1 = data.getFloat(FLT1);
	float f2 = data.getFloat(FLT2);
	float f3 = data.getFloat(FLT3);

	Log.e(TAG, "f1:" + f1 + " f2:" + f2 + " f3:" + f3);
    }
}
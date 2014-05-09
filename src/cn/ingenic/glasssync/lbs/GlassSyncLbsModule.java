package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class GlassSyncLbsModule extends SyncModule {
    private static final String TAG = "GlassSyncLbsModule";
    private static final String LETAG = "GSLBSMD";

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String BEARING = "bearing";

    private Context mContext;
    private static GlassSyncLbsModule sInstance;

	MockLocation mMockLocation;

    private GlassSyncLbsModule(Context context){
		super(LETAG, context);
		mContext = context;
		mMockLocation = new MockLocation(MockLocation.ProviderType.GPS, context);
		mMockLocation.init();
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

		Double lat = data.getDouble(LATITUDE);
		Double lon = data.getDouble(LONGITUDE);
		float bearing = data.getFloat(BEARING);

		MockLocationInfo info = new MockLocationInfo();
		info.setLatitude(lat);
		info.setLongitude(lon);
		info.setBearing(bearing);
		info.setTime(java.lang.System.currentTimeMillis());
		
		mMockLocation.sendLocation(info);
		
		Log.e(TAG, "lat:" + lat + " lon:" + lon + " bearing:" + bearing);
    }
}
package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
// import android.provider.Settings;

public class GlassSyncLbsModule extends SyncModule {
    private static final String TAG = "GlassSyncLbsModule";
    private static final boolean DEBUG = false;
    private static final String LETAG = "GSLBSMD";

    private static final String CMD_TYPE = "cmd_type";
    private static final int TYPE_OPEN = 1;
    private static final int TYPE_CLOSE = 2;
    private static final int TYPE_DATA = 3;

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String BEARING = "bearing";

    private Context mContext;
    private static GlassSyncLbsModule sInstance;

	MockLocation mMockLocation;

    private GlassSyncLbsModule(Context context){
	super(LETAG, context);
	mContext = context;
	// try {
	//     Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 1);  
	// } catch (Exception e) {
	//     Log.e(TAG, "write error", e);
	// }
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
	if(DEBUG) Log.d(TAG, "onRetrive--gettype="+data.getInt(CMD_TYPE));

	switch(data.getInt(CMD_TYPE)){
	case TYPE_OPEN:
	    mMockLocation.enableTestProvider();
	    break;
	case TYPE_CLOSE:
	    mMockLocation.unenableTestProvider();
	    break;
	case TYPE_DATA:
	    Double lat = data.getDouble(LATITUDE);
	    Double lon = data.getDouble(LONGITUDE);
	    float bearing = data.getFloat(BEARING);

	    MockLocationInfo info = new MockLocationInfo();
	    info.setLatitude(lat);
	    info.setLongitude(lon);
	    info.setBearing(bearing);
	    info.setTime(java.lang.System.currentTimeMillis());
		
	    mMockLocation.sendLocation(info);
		
	    if(DEBUG) Log.d(TAG, "lat:" + lat + " lon:" + lon + " bearing:" + bearing);
	    break;	
	}
    }

    @Override
	protected void onConnectionStateChanged(boolean connect) {
	if(DEBUG) Log.d(TAG,"onConnectionStateChanged connect="+connect);
	if(connect == false) {
	    mMockLocation.unenableTestProvider();
	}
    }
}
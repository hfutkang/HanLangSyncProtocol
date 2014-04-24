package cn.ingenic.glasssync.lbs;

import android.util.Log;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.os.Bundle;


/**
 * Collect location data and send to specific provider for mocking.
 */
public class MockLocation {
	private Context mContext;
	private LocationManager mLocationManager;
	private static final String TAG = "MockGPS";
	private ProviderType mProviderType;
	private String mProviderStr = null;
	private Boolean mInited = false;
	
	private final static int MESSAGE_MOCK_SEND = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {   
			switch (msg.what) {
			case MockLocation.MESSAGE_MOCK_SEND:
				Location location = new Location(mProviderStr);
				MockLocationInfo info = (MockLocationInfo)msg.obj;

				if(info.getAccuracyValid())
					location.setAccuracy(info.getAccuracy());

				if(info.getLatitudeValid())
					location.setLatitude(info.getLatitude());

				if(info.getLongitudeValid())
					location.setLongitude(info.getLongitude());

				if(info.getBearingValid())
					location.setBearing(info.getBearing());
				
				if(info.getTimeValid())
					location.setTime(info.getTime());
				
				if(info.getSpeedValid())
					location.setSpeed(info.getSpeed());
				
				mLocationManager.setTestProviderLocation(mProviderStr, location);
				
				break;
			}
			super.handleMessage(msg);   
		}
	};
	
	public enum ProviderType {
		GPS, NETWORK,
	};
	
	public MockLocation(ProviderType type, Context context) {
		mProviderType = type;
		mContext = context;
	}

	public Boolean init() {
		mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		if(mLocationManager == null) {
			Log.e(TAG, "Fail to get LocationManager from system service!!!!!");
			mInited = false;
			return false;
		}
		
		Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mProviderStr = mLocationManager.getBestProvider(criteria, true);		

		if(mProviderStr != null)
			Log.v(TAG, "Best location provider as " + mProviderStr);

		if(mProviderType == ProviderType.GPS)
			mProviderStr = LocationManager.GPS_PROVIDER;
		else if(mProviderType == ProviderType.NETWORK)
			mProviderStr = LocationManager.NETWORK_PROVIDER;
		else {
			Log.e(TAG, "unknown provider type!!!!!");
			mInited = false;
			return false;
		}

		mLocationManager.addTestProvider(mProviderStr,
										 "requiresNetwork" == "", "requiresSatellite" == "",
										 "requiresCell" == "", "hasMonetaryCost" == "",
										 "supportsAltitude" == "", "supportsSpeed" == "",
										 "supportsBearing" == "supportsBearing",
										 Criteria.POWER_LOW,
										 Criteria.ACCURACY_FINE);
		
		mLocationManager.setTestProviderEnabled(mProviderStr, true);

		mInited = true;
		return true;
	}

	public Boolean sendLocation(MockLocationInfo info) {
		if(mInited == false) {
			Log.e(TAG, "sendLocation failed because of not inited yet!!!!");
			return false;
		}
		
		Message msg = mHandler.obtainMessage();
		msg.what = MESSAGE_MOCK_SEND;
		msg.obj = info;
		
		mHandler.sendMessage(msg);

		return true;
	}
	
}

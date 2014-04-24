package cn.ingenic.glasssync.lbs;

import android.util.Log;
import android.os.Build;
import android.os.Bundle;


/**
 * The info structure sending to MockLocation.
 */
public class MockLocationInfo {
	private Boolean mAccuracyValid = false;
	private float mAccuracy = 0.0F;
	
	private Boolean mLatitudeValid = false;
	private double mLatitude = 0.0;
	
	private Boolean mLongitudeValid = false;
	private double mLongitude = 0.0;
	
	private Boolean mBearingValid = false;
	private float mBearing = 0.0F;
	
	private Boolean mTimeValid = false;
	private long mTime = 0L;
	
	private Boolean mSpeedValid = false;
	private float mSpeed = 0.0F;
	
	public MockLocationInfo() {
		
	}
	
	public void setAccuracy(float a) {
		mAccuracy = a;
		mAccuracyValid = true;
	}		
	public float getAccuracy() { return mAccuracy; }
	public Boolean getAccuracyValid() { return mAccuracyValid; }
	
	public void setLatitude(double a) {
		mLatitude = a;
		mLatitudeValid = true;
	}
	public double getLatitude() { return mLatitude; }
	public Boolean getLatitudeValid() { return mLatitudeValid; }
	
	public void setLongitude(double a) {
		mLongitude = a;
		mLongitudeValid = true;
	}
	public double getLongitude() { return mLongitude; }
	public Boolean getLongitudeValid() { return mLongitudeValid; }
	
	public void setBearing(float a) {
		mBearing = a;
		mBearingValid = true;
	}
	public float getBearing() { return mBearing; }
	public Boolean getBearingValid() { return mBearingValid; }
	
	public void setTime(long a) {
		mTime = a;
		mTimeValid = true;
	}
	public long getTime() { return mTime; }
	public Boolean getTimeValid() { return mTimeValid; }
	
	public void setSpeed(float a) {
		mSpeed = a;
		mSpeedValid = true;
	}
	public float getSpeed() { return mSpeed; }
	public Boolean getSpeedValid() { return mSpeedValid; }
}

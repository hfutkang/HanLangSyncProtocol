package cn.ingenic.glasssync.appmanager;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

public class AppSendEntry extends SyncData {
	
	
	public AppSendEntry(Drawable drawable,
			String appName,String appSize,String packageName,boolean isSystem,int enableSetting){
		setDrawable(drawable);
		setAppName(appName);
		setAppSize(appSize);
		setPackageName(packageName);
		setIsSystem(isSystem);
		setSystemSettingEnable(enableSetting);
	}
	
	private void setDrawable(Drawable drawable){
		if(drawable!=null){
			BitmapDrawable bm=(BitmapDrawable) drawable;
			Bitmap bitmap=bm.getBitmap();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
			byte[] b=baos.toByteArray();
			putByteArray(Common.AppEntryKey.DRAWABLE,b);
		}else{
			putByteArray(Common.AppEntryKey.DRAWABLE,null);
		}
		
	}
	
	private void setAppName(String appName){
		putString(Common.AppEntryKey.APPLICATION_NAME,appName);
	}
	
	private void setAppSize(String appSize){
		putString(Common.AppEntryKey.APPLICATION_SIZE,appSize);
	}
	
	private void setPackageName(String packageName){
		Log.i("ApplicationManager","AppSendEntry setPackageName pkgName is :"+packageName);
		putString(Common.AppEntryKey.PACKAGE_NAME,packageName);
	}
	private void setIsSystem(boolean isSystem){
		Log.e("ApplicationManager","setIsSystem is System is :"+isSystem);
		this.putBoolean(Common.AppEntryKey.IS_SYSTEM, isSystem);
	}
	
	private void setSystemSettingEnable(int status){
		this.putInt(Common.AppEntryKey.SYSTEM_SETTING_ENABLE, status);
	}
	
}
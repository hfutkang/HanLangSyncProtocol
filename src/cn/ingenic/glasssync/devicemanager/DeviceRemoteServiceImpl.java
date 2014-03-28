package cn.ingenic.glasssync.devicemanager;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import cn.ingenic.glasssync.ILocalBinder;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.RemoteBinderException;
import cn.ingenic.glasssync.data.RemoteParcel;

/**
 * for run on watch client; 
 * @author dfdun*/
public class DeviceRemoteServiceImpl implements ILocalBinder, IDeviceRemoteService {
	
	private static final int BASE = 0;
	private static final int INTERNET_REQUEST = BASE + 1;

	private Context mContext;
	public DeviceRemoteServiceImpl(Context context){
	    mContext = context;
	}

	public RemoteParcel onTransact(int code, RemoteParcel request) {
		/*
		RemoteParcel reply = new RemoteParcel();
		switch (code) {
		case Commands.CMD_INTERNET_REQUEST:
			String url = request.readString();
			String result = request(url);
			reply.writeString(result);
			break;
		}
		return reply;
		*/
		String result = request(code, request.readString());
		
		RemoteParcel reply = new RemoteParcel();
		reply.writeString(result);
		return reply;
	}
	
	static IDeviceRemoteService asRemoteInterface(IRemoteBinder binder) {
		return new DeviceRemoteServiceProxy(binder);
	}

	public static class DeviceRemoteServiceProxy implements IDeviceRemoteService {
		
		private final IRemoteBinder mRemoteBinder;
		
		private DeviceRemoteServiceProxy(IRemoteBinder remoteBinder) {
			mRemoteBinder = remoteBinder;
		}

		@Override
		public String request(int cmd, String url) {
			RemoteParcel request = new RemoteParcel();
			
			request.writeString(url);
			RemoteParcel reply;
			try {
				reply = mRemoteBinder.transact(cmd, request);
				return reply.readString();
			} catch (RemoteBinderException e) {
				return null;
			}
		}
		
	}

	@Override
	public String request(int cmd, String request) {
		klilog.i("cmd:"+cmd+", request:"+request);
		String result = null;
		switch(cmd){
		case Commands.CMD_INTERNET_REQUEST:
			result = requestInternet(request);
			break;
		case Commands.CMD_LOCK_SCREEN:
			result = lockScreen();
			break;
		}
		return result;
	}
	
	private String lockScreen(){
		try{
			DevicePolicyManager devicePolicyManager = (DevicePolicyManager)mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
			devicePolicyManager.lockNow();
			return "true";
		}catch(Exception e){
			klilog.e(e.toString());
			Intent intent = new Intent(mContext, AddDeviceAdminActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			return "false";
		}
		
		/*
		SharedPreferences pref = mContext.getSharedPreferences("device_manager", Context.MODE_PRIVATE);
		boolean enable = pref.getBoolean("lock_screen", false);
		if(enable){
			DevicePolicyManager devicePolicyManager = (DevicePolicyManager)mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
			devicePolicyManager.lockNow();
		}else{
			Intent intent = new Intent(mContext, AddDeviceAdminActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
		}
		return String.valueOf(enable);
		*/
	}
	
	private String requestInternet(String url){
		klilog.i("Internet request: " + url);
		HttpGet get = new HttpGet(url);
		String result = null;
		HttpClient mHttpClient = new DefaultHttpClient();
		try {
			HttpResponse response = mHttpClient.execute(get);
			if(response.getStatusLine().getStatusCode() == 200){
				result = EntityUtils.toString(response.getEntity(), "UTF-8");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}

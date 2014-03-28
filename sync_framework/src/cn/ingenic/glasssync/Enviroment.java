package cn.ingenic.glasssync;

import java.util.Map;
import java.util.UUID;

import cn.ingenic.glasssync.transport.BluetoothChannel;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public abstract class Enviroment {
	
	private static final String TAG = "Enviroment";
	
	static interface EnviromentCallback {
		Enviroment createEnviroment();
	}
	
	public static abstract class ResourceManager {
		
		protected Context mContext;
		
		ResourceManager(Context context) {
			mContext = context;
		}
		
		abstract Notification getRetryFailedNotification();
		
		abstract Toast getRetryToast(int reason);
	}
	
	private static Enviroment sInstance = null;
	
	protected final Context mContext;
	protected ResourceManager mResMgr;
	
	static Enviroment init(EnviromentCallback listener) {
		if (sInstance == null) {
			Log.i(TAG, "create Enviroment");
			sInstance = listener.createEnviroment();
		} else {
			Log.w(TAG, "enviroment already exists.");
		}
		
		return sInstance;
	}
	
	public static Enviroment getDefault() {
		if (sInstance == null) {
			throw new NullPointerException("Enviroment must be inited before getDefault().");
		}
		
		return sInstance;
	}
	
	protected Dialog createBondDialog(final String address, final String title, final String msg) {
		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				DefaultSyncManager mgr = DefaultSyncManager.getDefault();
				
				switch(which) {
				case DialogInterface.BUTTON_POSITIVE:
					processBondResponse(true);
					mgr.setLockedAddress(address);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					processBondResponse(false);
					break;
				}
			}
			
		};
		
		AlertDialog dialog = new AlertDialog.Builder(mContext)
				.setTitle(title)
				.setMessage(msg).setPositiveButton(android.R.string.ok, listener)
				.setNegativeButton(android.R.string.cancel, listener).create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		return dialog;
	}
	
	Enviroment(Context context) {
		mContext = context;
	}
	
	ResourceManager getResourceManager() {
		return mResMgr;
	}
	
	public abstract boolean isWatch();
	
	public abstract void processBondRequest(String address);
	
	public abstract void processBondResponse(boolean success);
	
	public abstract UUID getUUID(int type, boolean remote);
	
	public boolean isMainChannel(UUID uuid, boolean remote) {
		if (uuid != null) {
			return uuid.equals(getUUID(BluetoothChannel.CUSTOM, remote))
					|| uuid.equals(getUUID(BluetoothChannel.SERVICE, remote));
		}
		return false;
	}
	
	public BluetoothChannel getAnotherMainChannel(UUID uuid, boolean remote, Map<UUID, BluetoothChannel> map) {
		if (uuid == null) {
			return null;
		}
		
		UUID custom = getUUID(BluetoothChannel.CUSTOM, remote);
		UUID service = getUUID(BluetoothChannel.SERVICE, remote);
		if (uuid.equals(custom)) {
			return map.get(service);
		} else if (uuid.equals(service)) {
			return map.get(custom);
		}
		
		return null;
	}
}

package cn.ingenic.glasssync;

import java.util.UUID;

import cn.ingenic.glasssync.Enviroment.ResourceManager;
import cn.ingenic.glasssync.transport.BluetoothChannel;
import cn.ingenic.glasssync.transport.TransportManager;
import android.app.Dialog;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.widget.Toast;

public class WatchEnviroment extends Enviroment {
	private Dialog mDialog;
	
	WatchEnviroment(Context context) {
		super(context);
		mResMgr = new ResourceManager(context) {

			@Override
			Notification getRetryFailedNotification() {
				Notification noti = new Notification();
				noti.flags |= Notification.FLAG_ONGOING_EVENT;
				noti.icon = android.R.drawable.ic_dialog_alert;
				noti.vibrate = new long[]{0, 500, 500, 500};
				noti.setLatestEventInfo(mContext, mContext.getString(R.string.timout_noti_title), mContext.getString(R.string.timout_noti_content), null);
				
//				Notification.Builder build = new Notification.Builder(mContext);
//				build.setOngoing(true);
//				build.setContentTitle("Glasssync Timeout");
//				build.setContentText("Glasssync running without BT connectivity for more than two hours");
////				build.setContentInfo("touch me to connect");
//				build.setTicker("Glasssync disable auto connect");
//				build.setSmallIcon(android.R.drawable.ic_dialog_alert);
//				build.setVibrate(new long[]{500, 500, 500, 500});
//				return build.getNotification();
				return noti;
			}

			@Override
			Toast getRetryToast(int reason) {
				String str = "UNKNOW";
				switch (reason) {
				case DefaultSyncManager.CONNECTING:
					str = mContext.getString(R.string.retry_connect);
					break;
				case DefaultSyncManager.SUCCESS:
					str = mContext.getString(R.string.retry_connect_success);
					break;
				case DefaultSyncManager.NO_CONNECTIVITY:
					str = mContext.getString(R.string.retry_connect_failed);
					break;
				case DefaultSyncManager.FEATURE_DISABLED:
					throw new IllegalArgumentException("System module should ever not be disable.");
				case DefaultSyncManager.NO_LOCKED_ADDRESS:
					str = mContext.getString(R.string.retry_connect_failed_with_no_address);
					break;
				}
				return Toast.makeText(mContext, str, Toast.LENGTH_SHORT);
			}
			
		};
	}
	
	@Override
	public boolean isWatch() {
		return true;
	}

	@Override
	public void processBondRequest(String address) {
		String title = mContext.getString(R.string.bond_request);
		String name = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address).getName();
		String msg = mContext.getString(R.string.bond_request_message, name);
		mDialog = createBondDialog(address, title, msg);
		mDialog.show();
		Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(500);
	}
	
	
	@Override
	public void processBondResponse(boolean success) {
		TransportManager tm = TransportManager.getDefault();
		tm.sendBondResponse(success);
		if (mDialog != null && mDialog.isShowing()) {
			mDialog.dismiss();
		}
	}
	
	@Override
	public UUID getUUID(int type, boolean remote) {
		switch (type) {
		case BluetoothChannel.CUSTOM:
			return remote ? BluetoothChannel.CUSTOM_UUID
					: BluetoothChannel.W_CUSTOM_UUID;
		case BluetoothChannel.SERVICE:
			return remote ? BluetoothChannel.SERVICE_UUID
					: BluetoothChannel.W_SERVICE_UUID;
		default:
			return null;
		}
	}

}

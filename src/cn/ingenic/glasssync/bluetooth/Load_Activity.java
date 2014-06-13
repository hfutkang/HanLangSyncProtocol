package cn.ingenic.glasssync.bluetooth;

import java.io.IOException;

import cn.ingenic.glasssync.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.android.ex.editstyledtext.EditStyledText.EditModeActions.StartEditAction;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import cn.ingenic.glasssync.DefaultSyncManager;

public class Load_Activity extends Activity {
	private String TAG = "Load_Activity";
	private BluetoothAdapter mBLADPServer = Welcome_Activity.sBluetoothAdapter;
	private BluetoothSocket mSocketServer;
	private BluetoothServerSocket mServerSocket;
	private Thread mServerThread;
	private Thread mClientThread;
	private BluetoothDevice mdevice;
	private String mAddress;
	final String SPP_UUID = "3385a711-ced3-470d-a1df-1e88ac06c29f";
	final String SDP_UUID = "4219ffae-0c05-4c5f-8f30-bc43a58f7f1d";
	private String mAddress_connect;
	private DefaultSyncManager manger;
	private String mDevice_info;
	private String mIsConnect;
	private String mBind_Address;
	private String[] strarray;
	private Timer mtimer;
	private String mOnbind_Address="00:00:00:00:00:00";
	private String mSendMsg;
        private Context mContext;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Log.e("Tag", msg.what+"msg.what");
			if (msg.what == 1) {
				Toast.makeText(getApplicationContext(), "您已绑定该设备，无需重新绑定",
						Toast.LENGTH_LONG).show();
				Intent onbind = new Intent(Load_Activity.this,
						Welcome_Activity.class);
				onbind.putExtra("Tag", 2);
				onbind.putExtra("mdevice", mdevice.getName());
				startActivity(onbind);
				Load_Activity.this.finish();
				
			} 
//			else if (msg.what == 2) {
//				Intent unbind = new Intent(Load_Activity.this,
//						Unbind_Activity.class);
//				startActivity(unbind);
//				Load_Activity.this.finish();
			//}
		else if (msg.what==3){
			Log.d("Tag", "Connect_Failure");
				Intent unbind = new Intent(Load_Activity.this,
						Unbind_Activity.class);
				startActivity(unbind);
				Load_Activity.this.finish();
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_load);
		manger = DefaultSyncManager.getDefault();
		Intent intent = getIntent();
		//mDevice_info = intent.getStringExtra("result");
		//Log.d("Tag", mDevice_info);
		//strarray = mDevice_info.split(",");
		//mAddress = strarray[0];
		mAddress = "98:FF:D0:8D:1C:73";
		//mIsConnect = strarray[1];
		Log.d("Tag", mBind_Address + "===============================");
		//mBind_Address = strarray[2];
		mBind_Address = "00:00:00:00:00:00";
		mAddress_connect = mAddress;
		mdevice = mBLADPServer.getRemoteDevice(mAddress);
		mtimer = new Timer();

		mClientThread = new Thread() {
			@Override
			public void run() {
			    if (mdevice.getBondState() != BluetoothDevice.BOND_BONDED) {
				try {
				    Log.d(TAG, "NOT BOND_BONDED");
				    byte[] b = new byte[4];
				    b[0] = '0';
				    b[1] = '0';
				    b[2] = '0';
				    b[3] = '0';
				    mdevice.setPin(b);
				    try {
					Thread.sleep(500);
				    } catch (InterruptedException e) {
				    }
				    mdevice.createBond();
				} catch (Exception e) {
				    // TODO Auto-generated catch block
				    Log.d("mylog", "setPiN failed!");
				    e.printStackTrace();
				}
			    }
			    Log.e(TAG, "createBond start");
			    long sbondtime = System.currentTimeMillis() / 1000l;
			    while (mdevice.getBondState() != BluetoothDevice.BOND_BONDED) {
				try {
				    Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				if (((System.currentTimeMillis() / 1000l) - 60) > sbondtime){
				    Log.e(TAG, "Bond timeout");
				    break;
				}
				//Log.e(TAG, "connect mAddress" + mAddress);
				mdevice = mBLADPServer.getRemoteDevice(mAddress);
			    };

			    if (mdevice.getBondState() != BluetoothDevice.BOND_BONDED){
				mdevice.setBondState(BluetoothDevice.BOND_NONE);
				    Message msg = new Message();
				    msg.what = 3;
				    mHandler.sendMessage(msg);
				    return;
			    }
			    Log.e(TAG, "now bonded");

			    try {
				mSocketServer = mdevice
				    .createRfcommSocketToServiceRecord(UUID
								       .fromString(SPP_UUID));
				Log.e(TAG, "connect start");
				try {
				    mSocketServer.connect();
				} catch (Exception e) {
				    Log.e("Tag", "e:" + e);
				}		
				Log.e(TAG, "connect end");

				long scontime = System.currentTimeMillis() / 1000l;
				while (mSocketServer.isConnected() == false){
				    try {
					Thread.sleep(50);
				    } catch (InterruptedException e) {
				    }

				    if (((System.currentTimeMillis() / 1000l) - 60) > sbondtime){
					Log.e(TAG, "Connect timeout");
					break;
				    }
				}

				if (mSocketServer.isConnected() == false){
				    Log.e(TAG, "isConnected false");
				    mdevice.setBondState(BluetoothDevice.BOND_NONE);
				    Message msg = new Message();
				    msg.what = 3;
				    mHandler.sendMessage(msg);
				    Log.e(TAG, "sendMessage end");
				    return;
				}else{
				    Log.e(TAG, "isConnected true");
				}

				if(manger.getLockedAddress()!=""){
				    mOnbind_Address=manger.getLockedAddress();
				}
				mSendMsg=mOnbind_Address+","+mBLADPServer.getAddress();
				OutputStream opsm = mSocketServer.getOutputStream();
				opsm.write(mSendMsg.getBytes());
				Log.e(TAG, "write:" + mBLADPServer.getAddress().getBytes());	
				Log.e(TAG, "Address" +mBind_Address+"------"+mBLADPServer.getAddress()+"========="+manger.getLockedAddress()+"======"+mAddress);	
				if (mBind_Address.equals(mBLADPServer.getAddress()) && manger.getLockedAddress().equals(mAddress)) {
				    Message message = new Message();
				    message.what = 1;
				    mHandler.sendMessage(message);
				    return;
				}	
				Log.d("Tag", mBind_Address
				      + "======mBind_Address==================");
				if (manger.getLockedAddress() != null) {
				    Log.d("Tag", manger.getLockedAddress()
					  + "manger.getLockedAddress()==============");
				    manger.disconnect();
				    try {
					Thread.sleep(1000);
				    } catch (InterruptedException e) {
				    }
				    manger.setLockedAddress("", true);
				}
				//					if(manger.getLockedAddress()!=""){
				//						OutputStream ops = mSocketServer.getOutputStream();
				//						ops.write(manger.getLockedAddress().getBytes());
				//						Log.e(TAG, "write:manger.getLockedAddress()" +manger.getLockedAddress());
				//					}					
					
			    } catch (IOException i) {
				Log.e(TAG, "e:", i);
			    }
			    try {
				Thread.sleep(2000);
			    } catch (InterruptedException e) {
			    }
			    try {
				mSocketServer.close();
			    } catch (Exception e) {
				// TODO: handle exception
			    }
			    Log.e(TAG, "connect mAddress" + mAddress_connect);
			    // manger.hasLockedAddress();
			    manger.glass_connect(mAddress_connect);
			    Log.e(TAG, "timer_start");
			    timer_start();
			    Log.e(TAG, "timer_end");
			}
		    }; 

		long ttime = System.currentTimeMillis() / 1000l;
		while (mBLADPServer.getState() != BluetoothAdapter.STATE_ON){
		    if ((System.currentTimeMillis() / 1000l) - 30 > ttime){
			Log.e(TAG, "BT ON timeout");
			break;
		    }

		    try {
			Thread.sleep(10);
		    } catch (InterruptedException e) {
		    }

		};

		if (mBLADPServer.getState() != BluetoothAdapter.STATE_ON){
		    Log.e(TAG, "can not turn on the bluetooth");
		    
		    Message msg = new Message();
		    msg.what = 3;
		    mHandler.sendMessage(msg);
		    return;
		}

		Log.e(TAG, "bl state on");
		mBLADPServer.cancelDiscovery();
		mClientThread.start();
		
	}

    private void timer_start(){
    	mtimer.schedule(new TimerTask() {
		@Override
		public void run() {
		    Log.e(TAG, "timer_Start");
		    Message msg = new Message();
		    msg.what = 3;
		    mHandler.sendMessage(msg);
		}   	 
	    }, 60000);
    }   

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
		filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
		registerReceiver(mBluetoothReceiver, filter);

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mBluetoothReceiver);
		super.onDestroy();
	}

	private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e(TAG, "rcv " + intent.getAction());
			if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				Log.e(TAG, "find ad bl devices");
				BluetoothDevice btDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (btDevice != null) {
					Log.v(TAG, "Name : " + btDevice.getName() + " Address: "
							+ btDevice.getAddress());
				}
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
					.getAction())) {
				Log.e(TAG, "ACTION_BOND_STATE_CHANGED");
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device != null) {
					Log.v(TAG, "Name : " + device.getName() + " Address: "
							+ device.getAddress());
				}
				int connectState = device.getBondState();
				Log.e(TAG, "connectState:" + connectState);
			} else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent
					.getAction())) {
				Log.e(TAG, "ACTION_PAIRING_REQUEST");
				BluetoothDevice btDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int type = intent.getIntExtra(
						BluetoothDevice.EXTRA_PAIRING_VARIANT,
						BluetoothDevice.ERROR);
				Log.e(TAG, "type:" + type);
				if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION
						|| type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY
						|| type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
					int pairingKey = intent.getIntExtra(
							BluetoothDevice.EXTRA_PAIRING_KEY,
							BluetoothDevice.ERROR);
					Log.e(TAG, "pairingKey:" + pairingKey);
				}
				Log.e(TAG, "setPairingConfirmation true");
				btDevice.setPairingConfirmation(true);
			} else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
					.equals(intent.getAction())) {
				int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						DefaultSyncManager.IDLE);
				boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true
						: false;
				Log.d("Tag", isConnect+"    isConnect");
				if (isConnect) {
					Log.d("Tag", "Connect_Success");
					Intent bind = new Intent(Load_Activity.this,
							Welcome_Activity.class);
					bind.putExtra("mdevice", mdevice.getName());
					bind.putExtra("mAddress", mAddress_connect);
					////
					SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
					Editor editor = tsp.edit();
					editor.putString("mDevice", mdevice.getName());
					editor.commit();
					////
					bind.putExtra("Tag", 2);
					Log.e(TAG, "mdevice:" + mdevice.getName());
					mtimer.cancel();
					startActivity(bind);
					finish();
				}
			} 
//			else {
//				   Message message = new Message();
//				   message.what = 2;
//				   mHandler.sendMessage(message);
//			}
		}
	};
}

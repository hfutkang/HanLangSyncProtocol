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
	private Thread m_SonThread;
	private BluetoothDevice mdevice;
	private String mAddress;
	final String SPP_UUID = "3385a711-ced3-470d-a1df-1e88ac06c29f";
	final String SDP_UUID = "4219ffae-0c05-4c5f-8f30-bc43a58f7f1d";
	private String mAddress_connect;
	private DefaultSyncManager mManger;
	private String mDevice_info;
	private String mIsConnect;
	private String mBind_Address;
	private String[] strarray;
	private Timer mtimer;
	private String mOnbind_Address="00:00:00:00:00:00";
	private String mSendMsg;
    private Context mContext;
    
	private Handler mHandler = new Handler() {

//This method is to realize the internal thread of information exchange, and make action
		@Override
		public void handleMessage(Message msg) {
			Log.e(TAG, msg.what+"msg.what");
			if (msg.what == 1) {
				Toast.makeText(getApplicationContext(), "您已绑定该设备，无需重新绑定",
						Toast.LENGTH_LONG).show();
				Intent onbind = new Intent(Load_Activity.this,
						Bind_Activity.class);
				SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
				
				Editor editor = tsp.edit();
				Log.d("load_activity",mAddress);
				editor.putString("mAddress", mAddress_connect);
				editor.commit();
				onbind.putExtra("Tag", 2);
				Log.d(TAG, mdevice.getName());
				editor.putString("mDevices", mdevice.getName());
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
			Log.d(TAG, "Connect_Failure");
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
		mManger = DefaultSyncManager.getDefault();
		Intent intent = getIntent();
		SysApplication.getInstance().addActivity(this); 
	
		mDevice_info = intent.getStringExtra("result");
		Log.d("Tag", mDevice_info);
		strarray = mDevice_info.split(",");
		mAddress = strarray[0];
		mIsConnect = strarray[1];
		mBind_Address = strarray[2];
		Log.d("Tag", mBind_Address + "===============================");
		
		//mBind_Address = "00:00:00:00:00:00";
		//Log.d(TAG, mBind_Address + "===============================");
		//mAddress = "98:FF:D0:8D:1C:73";
		//mAddress = "50:3C:C4:C1:2E:D7"; //llqing
		//mAddress = "00:01:DA:F9:F3:B0"; //malata
		//mAddress = "D8:50:E6:7E:23:6C"; //N7
		//mAddress = "BC:F5:AC:7A:1E:53"; //N5
		//mAddress = "78:52:1A:DE:56:08";   //S4
		//mAddress = "00:18:30:59:B8:D4"; //htc
		//mAddress = "30:9B:AD:5B:5E:CA"; //vivo
		
		
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction("load_connect");
		filter.addAction("load_message");
		//filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
		filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
		registerReceiver(mBluetoothReceiver, filter);
		mAddress_connect = mAddress;
		Log.d("load_maddress",mAddress_connect);
		mdevice = mBLADPServer.getRemoteDevice(mAddress);
		
		
		
		mtimer = new Timer();//

		mClientThread = new Thread() {
			@Override
			public void run() {
			    TextView tv = (TextView) findViewById(R.id.tv_load);
			    TextView en = (TextView) findViewById(R.id.tv_en);
			    tv.setText(R.string.load_cn);
			    en.setText(R.string.load_en);
			    
			    if (mdevice.getBondState() == BluetoothDevice.BOND_BONDED){
				mdevice.setBondState(BluetoothDevice.BOND_NONE);
			    }
			    mManger.disconnect();
			    mManger.setLockedAddress(null);
			    try {
			    	Thread.sleep(10000);
			    } catch (InterruptedException e) {
			    	
			    }
				
			    Log.d(TAG, "load_message_s");
			    Intent intent=new Intent();
			    intent.setAction("load_message");
			    sendBroadcast(intent);
			    Log.d(TAG, "load_message_e");
			    if (mdevice.getBondState() != BluetoothDevice.BOND_BONDED) {
				try {
				    Log.d(TAG, "NOT BOND_BONDED");
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
			    //tv.setText(R.string.load_cfm);
			 //   try{
			   // tv = (TextView) findViewById(R.id.tv_load);
			  // tv.setText(R.string.load_connect);
			   //Log.d(TAG, "load_connect");
			  // en.setText(" ");
			  // }catch(Exception e){
			//	  	Log.d(TAG,e+"Exception");
			 //  }finally{
				   //tv.setText(R.string.load_connect);
				  // Log.d(TAG, "load_connect");
				  // en.setText(" ");   
			  // }
			    Log.d(TAG,"intent_connect");
			    
			    intent.setAction("load_connect");
                sendBroadcast(intent);
                Log.d(TAG,"intent_connect");
			    try {
				Thread.sleep(2000);
			    } catch (InterruptedException e) {
			    	
			    }

			    Log.e(TAG, "connect mAddress" + mAddress_connect);
			    // manger.hasLockedAddress();
			    mManger.glass_connect(mAddress_connect);
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
		}

		if (mBLADPServer.getState() != BluetoothAdapter.STATE_ON){
		    Log.e(TAG, "can not turn on the bluetooth");
		    
		    Message msg = new Message();
		    msg.what = 3;
		    mHandler.sendMessage(msg);
		    return;
		}

		Log.e(TAG, "bl state on");
		mBLADPServer.cancelDiscovery();//?
		mClientThread.start();

		//Intent bind = new Intent(Load_Activity.this,
		//			 Bind_Activity.class);

		//bind.putExtra("mdevice", "Lenovo A880");
		//bind.putExtra("mAddress", "30:9B:AD:5B:5E:CA");
		//bind.putExtra("Tag", 2);
		//Log.e(TAG, "mdevice:" + "vivo");
		//startActivity(bind);
		//finish();
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
	

	}
	protected void onPause() {
	    unregisterReceiver(mBluetoothReceiver);
	    super.onPause();
	    }
	@Override
	protected void onDestroy() {
	    //unregisterReceiver(mBluetoothReceiver);
		super.onDestroy();
	}

	private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String load_connect="load_connect";
			String load_message="load_message";
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
			} else if(load_connect.equals(intent.getAction())){
				TextView tv = (TextView) findViewById(R.id.tv_load);
			    TextView en = (TextView) findViewById(R.id.tv_en);
				tv.setText(R.string.load_connect);
				en.setText(" ");
			}else if(load_message.equals(intent.getAction())){
				TextView tv = (TextView) findViewById(R.id.tv_load);
			    TextView en = (TextView) findViewById(R.id.tv_en);
				tv.setText(R.string.load_message);
				en.setText(" ");
				
			}else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
					.equals(intent.getAction())) {
				int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						DefaultSyncManager.IDLE);
				boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true
						: false;
				Log.d(TAG, isConnect+"isConnect");
				if (isConnect) {
					Log.d(TAG, "Connect_Success");
					Intent bind = new Intent(Load_Activity.this,
							Bind_Activity.class);
					//bind.putExtra("mdevice", mdevice.getName());
					//bind.putExtra("mAddress", mAddress_connect);
	                
					
					SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
					Log.d(TAG,mAddress_connect);
					Editor editor = tsp.edit();
					editor.putString("mAddress", mAddress_connect);
					//editor.commit();
				    editor.putString("mDevice", mdevice.getName());
				   
					editor.commit();
					 mAddress=tsp.getString("mAddress",null);
						Log.e(TAG, "mAddress:" + mAddress);
					String	mDevice_load=tsp.getString("mDevice",null);
							Log.e(TAG, "mDevice_load:" + mDevice_load);
					bind.putExtra("Tag", 2);
					Log.e(TAG, "mdevice:" + mdevice.getName());
					mtimer.cancel();
					startActivity(bind);
					finish();
				}
			} 
		}
	
};
}
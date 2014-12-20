package cn.ingenic.glasssync.bluetooth;

import cn.ingenic.glasssync.R;
import java.io.IOException;
import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.GestureDetector;
import android.widget.GestureDetector.SimpleOnGestureListener;

import cn.ingenic.glasssync.DefaultSyncManager;

public class Load_Activity extends Activity implements OnTouchListener{
    private String TAG = "Load_Activity";
    private static final boolean DEBUG = true;

    private static final int MESSAGE_BT_ON = 1;
    private static final int MESSAGE_BT_OFF = 2;

    private static final int MESSAGE_BOND_BONDED = 11;
    private static final int MESSAGE_BOND_NONE = 12;

    private static final int MESSAGE_CONNECT_SUCCESS = 21;
    private static final int MESSAGE_CONNECT_FAILED = 22;

    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private DefaultSyncManager mManger;

    private GestureDetector mGestureDetector;
    private String mAddress;
    private String[] strarray;
    private Context mContext;
    private boolean mCanTouch = false;
    private boolean mRemoveBondFlag = false;

    private Handler mHandler = new Handler() {
	    @Override
		public void handleMessage(Message msg) {
		switch (msg.what) {   
		case MESSAGE_BT_ON:
		    startBondWorker();
		case MESSAGE_BOND_NONE:
		case MESSAGE_CONNECT_FAILED:
		    // mDevice.removeBond();
		    TextView tv = (TextView) findViewById(R.id.tv_load);
		    if(msg.what == MESSAGE_BOND_NONE)
				tv.setText(R.string.unbind);
		    else
				tv.setText(R.string.unconnect);
		    
		    mCanTouch = true;
            break;    
		case MESSAGE_BOND_BONDED:
		      /*Notice:connect must be called by mobile*/
		    // mManger.glass_connect(mAddress);
                    break;    
		case MESSAGE_CONNECT_SUCCESS:
		    bluetoothBonded();
		    break;
                default:    
                    break;    
                }    
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
	
	String device_info = intent.getStringExtra("result");
	if(DEBUG)Log.d(TAG,"----device_info="+device_info);
	strarray = device_info.split(",");
	mAddress = strarray[0];
	String isConnect = strarray[1];
	
	mBTAdapter = BluetoothAdapter.getDefaultAdapter();
	mDevice = mBTAdapter.getRemoteDevice(mAddress);

	IntentFilter filter = new IntentFilter();
	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	filter.addAction(BluetoothDevice.ACTION_FOUND);
	filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

	  //filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
	filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
	registerReceiver(mBluetoothReceiver, filter);
	
	gestureDetectorWorker();
	LinearLayout root = (LinearLayout) findViewById(R.id.root);
	root.setOnTouchListener(this);

	if (mBTAdapter.isEnabled()) {
	    startBondWorker();
	}
    }
    
    @Override
	protected void onDestroy() {
	unregisterReceiver(mBluetoothReceiver);
	super.onDestroy();
    }

    private void startBondWorker(){
	mBTAdapter.cancelDiscovery();
	new Thread() {
	    @Override
		public void run() {
		TextView tv = (TextView) findViewById(R.id.tv_load);
		tv.setText(R.string.loading);
		if(DEBUG)Log.d(TAG, mManger.getLockedAddress()+"mManger.getLockedAddress()");
		
		  //remove last bonded
		Set<BluetoothDevice> devices=mBTAdapter.getBondedDevices();
		if(devices!=null){
		    for(BluetoothDevice device:devices){
			if(device.getAddress().equals(mAddress)){
			    if(DEBUG)Log.d(TAG, "-------need remove devies had boned last");
			    device.removeBond();
			    mRemoveBondFlag = true;
			    return;
			}
		    }
		}
		  //start BOND
		try {
		    Log.e(TAG, "createBond start");
		    mDevice.createBond();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}.start();	
    }

    private void bluetoothBonded(){
	Intent bind = new Intent(Load_Activity.this,Bind_Activity.class);
	startActivity(bind);
	finish();
    }
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		if(DEBUG) Log.e(TAG, "rcv " + intent.getAction());
		if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
		    Message msg = new Message();
		    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {    
		    case BluetoothAdapter.STATE_ON:    
			if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_ON");    
			msg.what = MESSAGE_BT_ON;
			mHandler.sendMessage(msg);
			break;    
		    case BluetoothAdapter.STATE_OFF:    
			if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_OFF");    
			msg.what = MESSAGE_BT_OFF;
			mHandler.sendMessage(msg);
			break;    
		    default:    
			break;    
		    }    
		}else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
		    if(DEBUG) Log.e(TAG, "ACTION_FOUND");
		} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
		    if(DEBUG) Log.e(TAG, "ACTION_BOND_STATE_CHANGED");
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (device != null)
		    if(DEBUG)Log.d(TAG, "Name : " + device.getName() + " Address:"+ device.getAddress());
		    Message msg = new Message();
		    switch (device.getBondState()) {    
		    case BluetoothDevice.BOND_BONDING:    
			if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bonding");    
			TextView tv = (TextView) findViewById(R.id.tv_load);
			tv.setText(R.string.load_message);
			break;    
		    case BluetoothDevice.BOND_BONDED:    
			if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bonded");    
			break;    
		    case BluetoothDevice.BOND_NONE:    
			if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bond none");    
			if(mRemoveBondFlag){ 
			    if(DEBUG) Log.d(TAG, "------can rebond");    
			    mRemoveBondFlag = false;
			    try {
				mDevice.createBond();
			    } catch (Exception e) {
				e.printStackTrace();
			    }
			    break;
			}
			msg.what = MESSAGE_BOND_NONE;
			mHandler.sendMessage(msg);
			break;
		    default:    
			break;    
		    }        
		}else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
			  .equals(intent.getAction())) {
		    int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						   DefaultSyncManager.IDLE);
		    boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true
			: false;
		    if(DEBUG) Log.d(TAG,"--state="+state+"--isConnect="+isConnect);
		    Message msg = new Message();
		    if (isConnect) {
			msg.what = MESSAGE_CONNECT_SUCCESS;
			mHandler.sendMessage(msg);
		    }else{
			msg.what = MESSAGE_CONNECT_FAILED;
		    	mHandler.sendMessage(msg);
		    }
		}// 
	    }
	    
	};

    private void gestureDetectorWorker(){
	mGestureDetector =  new GestureDetector(this, new SimpleOnGestureListener() {
		@Override
		public boolean onDown(boolean fromPhone) {
			return true;
					}

		@Override
		public boolean onSlideDown(boolean fromPhone){		    
			if(DEBUG)Log.e(TAG, "onSlideDown ....................");
		    if(mCanTouch){
			SysApplication.getInstance().exit();
		    }
		    return true;
		}
	    });
    }

    @Override
	public boolean onTouch(View v, MotionEvent event) {
	return mGestureDetector.onTouchEvent(event);
    }	

}
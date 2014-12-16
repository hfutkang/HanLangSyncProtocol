package cn.ingenic.glasssync.blmanager;

import java.util.Set;
import cn.ingenic.glasssync.R;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.bluetooth.BluetoothDevice;

import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import android.widget.GestureDetector;
import android.widget.GestureDetector.SimpleOnGestureListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.media.MediaPlayer;

import android.bluetooth.BluetoothAdapter;

public class GlassSyncBLManager {
    private static final String TAG = "GlassSyncBLManager";
    public static final boolean DEBUG = true;
    private Context mContext;
    private static GlassSyncBLManager sInstance;
    private BLManagerReceiver nReceiver;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private View mPairingView;
    private BluetoothDevice mDevice;

    public static final int PAIRING_REQUEST_MESSAGE = 1;
    public static final int TIMEOUT_MESSAGE = 2;
    public static final int DISBOND_REQUEST_MESSAGE = 3;

    public static final int TIMEOUT_DELAY = 20000;//20s
    private static final String ACTION_CONNECT = "cn.ingenic.glasssync.bond_changed";

    private Handler mHandler = new Handler() {
	    @Override
		public void handleMessage(Message msg) {
		switch (msg.what) {
		case PAIRING_REQUEST_MESSAGE:
		    Intent intent = (Intent)msg.obj;
		    mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	
		    Log.d(TAG, "mobile request connect glass");
		    MediaPlayer player = MediaPlayer.create(mContext, R.raw.pair);
		    player.start();
		    
		    LayoutInflater inflater = 
			(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    mPairingView = inflater.inflate(R.layout.bt_pairing_request, null);
		    FrameLayout root=(FrameLayout)mPairingView.findViewById(R.id.root);
		    TextView info_tv =(TextView)mPairingView.findViewById(R.id.device_info);
		    root.setOnTouchListener(mOnTouchListener);
		    info_tv.setText(mDevice.getName());

		    mWindowManager.addView(mPairingView, mWindowParams);    
		    break;

		case TIMEOUT_MESSAGE:
		    if(DEBUG) Log.d(TAG,"---timeout");
		    mWindowManager.removeView(mPairingView);
		    mDevice.setPairingConfirmation(false);
		    break;
		case DISBOND_REQUEST_MESSAGE:
		    Log.d(TAG,"------removeBond start");
		    Set<BluetoothDevice> device=BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		    if(device.size()!=0){
			for(BluetoothDevice bluetoothDevice:device){
			    bluetoothDevice.removeBond();
			    if(DEBUG)Log.d(TAG, "bonddevice_name="+bluetoothDevice.getName());
			}
		    }
		    Log.d(TAG,"------removeBond end");
		    break;
		default:
		    throw new RuntimeException("Unknown message " + msg); //never
		}
	    }	    
	};

    private OnTouchListener mOnTouchListener = new OnTouchListener(){
	    @Override
	    public boolean onTouch(View v, MotionEvent event){
		return mGestureDetector.onTouchEvent(event);
	    }	
	};
    
    private GestureDetector mGestureDetector = new GestureDetector(mContext,new SimpleOnGestureListener() {
	    @Override
		public boolean onSlideDown(boolean fromPhone){		    
		if(DEBUG) Log.d(TAG,"---onSlideDown in");
		mHandler.removeMessages(TIMEOUT_MESSAGE);
		mWindowManager.removeView(mPairingView);
		mDevice.setPairingConfirmation(false);
		return true;
	    }
	    
	    @Override
		public boolean onTap(boolean fromPhone){
		if(DEBUG) Log.d(TAG,"---onTap in");
		mHandler.removeMessages(TIMEOUT_MESSAGE);
		mWindowManager.removeView(mPairingView);
		mDevice.setPairingConfirmation(true);
		  //
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if(DEBUG) Log.d(TAG,"Adapter name="+adapter.getName()+"-mac="+adapter.getAddress());
		if(DEBUG) Log.d(TAG,"mDevice name="+adapter.getName()+"-mac="+mDevice.getAddress());
		return true;
	    }
	    
	});

    public static GlassSyncBLManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncBLManager(c);
	return sInstance;
    }

    private GlassSyncBLManager(Context context){
	Log.e(TAG, "GlassSyncBLManager");
	mContext = context;
	mWindowParams = new WindowManager.LayoutParams();
	mWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE;	    
	mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
	mWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
	
	mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
	
	init_receiver(context);
    }

    private void init_receiver(Context c){
        nReceiver = new BLManagerReceiver();
	nReceiver.setHandler(mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	filter.addAction(BluetoothDevice.ACTION_FOUND);
	filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

	/*listen broadcast from DefaultSyncManager*/
	// filter.addAction(ACTION_CONNECT);
        c.registerReceiver(nReceiver,filter);
    }

    class BLManagerReceiver extends BroadcastReceiver{
	private String TAG = "BLManagerReceiver";
	private String mMACAddress;	
	private Handler mHandler;

	public void setHandler(Handler handler){
	    mHandler = handler;
	}
        @Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){
		Log.d(TAG, "--receive ACTION_PAIRING_REQUEST");
		int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
					      BluetoothDevice.ERROR);
		if (type != BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION)
		    return;

		if(true){
		      //coldwave
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    device.setPairingConfirmation(true);
		}else{
		      //iglass
		    Message m = mHandler.obtainMessage(PAIRING_REQUEST_MESSAGE);
		    m.obj = intent;
		    mHandler.sendMessageDelayed(m,0);
		    mHandler.sendEmptyMessageDelayed(TIMEOUT_MESSAGE, TIMEOUT_DELAY);		    
		}
	    }else if(intent.getAction().equals(ACTION_CONNECT)){
		if(intent.getBooleanExtra("validAddr",false) == false){
		    Message m = mHandler.obtainMessage(DISBOND_REQUEST_MESSAGE);
		    mHandler.sendMessageDelayed(m,0);
		}

	    }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
		Message msg = new Message();
		switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {    
		case BluetoothAdapter.STATE_ON:    
		    if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_ON");    
		    break;    
		case BluetoothAdapter.STATE_OFF:    
		    if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_OFF");    
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
		switch (device.getBondState()) {    
		case BluetoothDevice.BOND_BONDING:    
		    if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bonding");    
			break;    
		case BluetoothDevice.BOND_BONDED:    
		    if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bonded");    
		    break;    
		case BluetoothDevice.BOND_NONE:    
		    if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED--bond none");    
		    break;
		default:    
		    break;    
		}        
	    }
        }//
    }

}
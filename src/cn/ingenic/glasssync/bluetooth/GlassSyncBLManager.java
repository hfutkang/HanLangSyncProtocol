package cn.ingenic.glasssync.blmanager;

import cn.ingenic.glasssync.R;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;

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

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.data.FeatureConfigCmd;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.SystemModule;

import com.ingenic.glass.voicerecognizer.api.VoiceRecognizer;

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
    public static final int BLUETOOTH_DISCOVERABLE_MESSAGE = 4;

    public static final int TIMEOUT_DELAY = 20000;//20s
    private static final String ACTION_CANCEL_BOND = "cn.ingenic.glasssync.CANCEL_BOND";
    private String lastBondAddress = null;

    private VoiceRecognizer mVoiceRecognizer = null;

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
		case BLUETOOTH_DISCOVERABLE_MESSAGE:
		    //Log.d(TAG,"------BLUETOOTH_DISCOVERABLE_MESSAGE");
		    enableBluetoothVisible();
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
	if(DEBUG) Log.d(TAG, "GlassSyncBLManager");
	mContext = context;
	mWindowParams = new WindowManager.LayoutParams();
	mWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE;	    
	mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
	mWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
	
	mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
	
	init_receiver(context);

	boolean enable = SystemProperties.getBoolean("ro.bluetooth.open.enable", false);
	if(enable){
	      //make sure bluetooth is open
	    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	    if (!adapter.isEnabled()) {
		if(DEBUG) Log.d(TAG, "----open bl");
		adapter.enable();
	    }
	}

	mVoiceRecognizer = new VoiceRecognizer(VoiceRecognizer.REC_TYPE_COMMAND, null);
	mVoiceRecognizer.setAppName("GlassSyncBLManager");
    }

    private void init_receiver(Context c){
        nReceiver = new BLManagerReceiver();
	nReceiver.setHandler(mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

	/*listen broadcast from DefaultSyncManager*/
	// filter.addAction(ACTION_CONNECT);
	filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
	filter.addAction(ACTION_CANCEL_BOND);
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
		}// else{
		//       //iglass
		//     Message m = mHandler.obtainMessage(PAIRING_REQUEST_MESSAGE);
		//     m.obj = intent;
		//     mHandler.sendMessageDelayed(m,0);
		//     mHandler.sendEmptyMessageDelayed(TIMEOUT_MESSAGE, TIMEOUT_DELAY);		    
		// }

	    }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
		Message msg = new Message();
		switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {    
		case BluetoothAdapter.STATE_ON:    
		    if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_ON");    
		    if(DefaultSyncManager.getDefault().getLockedAddress().equals(""))
			enableBluetoothVisible();

		    break;    
		case BluetoothAdapter.STATE_OFF:    
		    if(DEBUG) Log.d(TAG, "BluetoothAdapter.STATE_OFF");    
		    break;    
		default:    
		    break;    
		}    
	    }else if(intent.getAction().equals(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE)){
		int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,DefaultSyncManager.IDLE);
		boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true : false;
		if(DEBUG) Log.d(TAG,"----state="+state+"--isConnect="+isConnect);
		if(isConnect){
			if(lastBondAddress == null){
				Log.d(TAG,"bond ok"+lastBondAddress);
			   //Bonded ok
			   lastBondAddress = DefaultSyncManager.getDefault().getLockedAddress();
			   mVoiceRecognizer.playTTS(mContext.getString(R.string.tts_bond_ok));
			}
		    disableBluetoothVisible();
		}else if(DefaultSyncManager.getDefault().getLockedAddress().equals("")){
			Log.d(TAG,"unbond ok"+lastBondAddress);
		    //DisBond ok
		    lastBondAddress = null;
		    enableBluetoothVisible();
		    mVoiceRecognizer.playTTS(mContext.getString(R.string.tts_unbond_ok));
		}
	    }else if (ACTION_CANCEL_BOND.equals(intent.getAction())) {
		if(DEBUG) Log.d(TAG, "cn.ingenic.glasssync.CANCEL_BOND");    
		sendUnbind2Mobile();
		DefaultSyncManager.getDefault().setLockedAddress("");
		enableBluetoothVisible();
	    }
        }//
    }

    private static final int BLUETOOTH_DSCOVERABLE_TIME = 60*600; //60s *600 = 10h
    private void enableBluetoothVisible(){
	if(DEBUG) Log.d(TAG, "enableBluetoothVisible");    
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	boolean scan = adapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
	adapter.setDiscoverableTimeout(BLUETOOTH_DSCOVERABLE_TIME);
	Message m = mHandler.obtainMessage(BLUETOOTH_DISCOVERABLE_MESSAGE);
	mHandler.sendMessageDelayed(m,BLUETOOTH_DSCOVERABLE_TIME*1000);
    }

    private void disableBluetoothVisible(){
	if(DEBUG) Log.d(TAG, "disableBluetoothVisible");    
	mHandler.removeMessages(BLUETOOTH_DISCOVERABLE_MESSAGE);
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	adapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    public static void sendUnbind2Mobile(){
    	DefaultSyncManager manager = DefaultSyncManager.getDefault();
    	Config config = new Config(SystemModule.SYSTEM);
    	Map<String, Boolean> map = new HashMap<String, Boolean>();
    	map.put(DeviceModule.FEATURE_UNBIND, true);
    	Projo projo = new FeatureConfigCmd();
    	projo.put(FeatureConfigCmd.FeatureConfigColumn.feature_map,map);
    	ArrayList<Projo> datas = new ArrayList<Projo>();
    	datas.add(projo);
    	manager.request(config, datas);
    	manager.featureStateChange(DeviceModule.FEATURE_UNBIND, true);
    }
}

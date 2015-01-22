package cn.ingenic.glasssync.bluetooth;

import java.util.Set;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.GestureDetector;
import android.widget.GestureDetector.SimpleOnGestureListener;

import android.widget.LinearLayout;
import android.widget.TextView;
// import android.widget.Toast;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.data.FeatureConfigCmd;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.SystemModule;

public class Bind_Activity extends Activity{
    private static final String TAG = "Bind_Activity";
    private static final boolean DEBUG = true;

    private static final String ACTION_CONNECT = "cn.ingenic.glasssync.bond_changed";
    private DefaultSyncManager mManger;
    private BluetoothDevice mDevice;
    private Context mContext;
    private GestureDetector mGestureDetector;

    private static final int BOND_VIEW = 1;
    private static final int DISBOND_VIEW = 2;

    private int mCurrentView = 1;
    private BluetoothAdapter mBluetoothAdapter ;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	if(DEBUG) Log.e(TAG, "onCreate in");
	
	setContentView(R.layout.activity_bind);
	mManger = DefaultSyncManager.getDefault();
	
	LinearLayout layout_bind = (LinearLayout) findViewById(R.id.layout_bind);
	layout_bind.setOnTouchListener(new myTouchListener());
	
	SysApplication.getInstance().addActivity(this); 
	mContext = this;

	  /*listen broadcast from DefaultSyncManager*/
	IntentFilter filter = new IntentFilter();
	filter.addAction(ACTION_CONNECT);
	registerReceiver(mBindStateReceiver, filter);
	
    }

    @Override
	protected void onStart() {
	super.onStart();
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	String bindAddress = mManger.getLockedAddress();
	mDevice = mBluetoothAdapter.getRemoteDevice(bindAddress);
	String bindName = mDevice.getName();

	int tag = getIntent().getIntExtra("Tag", 1);
	
	TextView tv = (TextView)findViewById(R.id.tv_mobile);
	if(DEBUG) Log.e(TAG, "onStart in tag="+tag +"--bindAddress="+bindAddress+"--bindName="+bindName);
	tv.setText(bindName);

	gestureDetectorWorker();
    } 

    @Override
	protected void onDestroy() {
	super.onDestroy();
	if(mBindStateReceiver == null)
	    return;
	unregisterReceiver(mBindStateReceiver);
    }

    private void disableBond(){
	if(DEBUG) Log.e(TAG, "-----disableBond address="+mManger.getLockedAddress());
	mManger.setLockedAddress("");
	// mManger.disconnect();//must not used


	LinearLayout layout_bind = (LinearLayout) findViewById(R.id.layout_bind);
	layout_bind.setVisibility(View.GONE);

	TextView layout_disbind = (TextView) findViewById(R.id.layout_disbind);
	layout_disbind.setVisibility(View.VISIBLE);
	mCurrentView = DISBOND_VIEW;
	layout_disbind.setOnTouchListener(new myTouchListener());	
	unregisterReceiver(mBindStateReceiver);
	mBindStateReceiver = null;
    }

    private void gestureDetectorWorker(){
	mGestureDetector =  new GestureDetector(this,new SimpleOnGestureListener() {
		 @Override
		    public boolean onDown(boolean fromPhone){		    
			    return true;
		    }
		@Override
		public boolean onSlideDown(boolean fromPhone){		    
		    if(DEBUG) Log.d(TAG,"---onSlideDown");
		    SysApplication.getInstance().exit();
		    return true;
		}

		@Override
		public boolean onTap(boolean fromPhone){
		    if(DEBUG) Log.d(TAG,"---onTap in");
		    if(mCurrentView == DISBOND_VIEW){
			Intent load = new Intent(Bind_Activity.this,Welcome_Activity.class);
			startActivity(load);
			finish();
		    }
		    return true;
		}

		@Override
		public boolean onLongPress(boolean fromPhone){
		    if(DEBUG) Log.d(TAG,"---onLongPress");
		    sendUnbind();
		    disableBond();
		    return true;
		}		    
	    });
    }

    //just be used when mobile disbond actively
    private BroadcastReceiver mBindStateReceiver = new BroadcastReceiver() {
    	    @Override
    		public void onReceive(Context context, Intent intent) {
    		if(intent.getAction().equals(ACTION_CONNECT)){
    		    if(intent.getBooleanExtra("validAddr",false) == true)
    			return;
    		    disableBond();
    		}
    	    }
    	};

    class myTouchListener implements OnTouchListener {
	@Override
	    public boolean onTouch(View v, MotionEvent event) {
	    return mGestureDetector.onTouchEvent(event);
	}	
    }//

    private void sendUnbind(){
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
		Log.d(TAG,"sendUnbind out");
    }

}






















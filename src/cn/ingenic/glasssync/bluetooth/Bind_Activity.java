package cn.ingenic.glasssync.bluetooth;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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

public class Bind_Activity extends Activity{
    private static final String TAG = "Bind_Activity";
    private static final boolean DEBUG = true;

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
	if (tag == 1) {
	    tv.setText(bindName);
	    mManger.glass_connect(bindAddress);
	} else if (tag == 2) {
	    tv.setText(bindName);
	}

	gestureDetectorWorker();
    } 

    private void disableBond(){
	mManger.setLockedAddress("");
	mManger.disconnect();
	mDevice.removeBond();

	LinearLayout layout_bind = (LinearLayout) findViewById(R.id.layout_bind);
	layout_bind.setVisibility(View.GONE);

	TextView layout_disbind = (TextView) findViewById(R.id.layout_disbind);
	layout_disbind.setVisibility(View.VISIBLE);
	mCurrentView = DISBOND_VIEW;
	layout_disbind.setOnTouchListener(new myTouchListener());	
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
		    if(DEBUG) Log.d(TAG,"---onLongPress remote address=");
		    disableBond();
		    return true;
		}		    
	    });
    }
    class myTouchListener implements OnTouchListener {
	@Override
	    public boolean onTouch(View v, MotionEvent event) {
	    return mGestureDetector.onTouchEvent(event);
	}	
    }//

}






















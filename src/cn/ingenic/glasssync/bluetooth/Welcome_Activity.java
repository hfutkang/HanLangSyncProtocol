package cn.ingenic.glasssync.bluetooth;
import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnTouchListener;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Welcome_Activity extends Activity implements OnTouchListener{
    private static final String TAG = "Welcome_Activity";
    private static final boolean DEBUG = true;

    public static BluetoothAdapter sBluetoothAdapter;

    private LinearLayout mlayout_welcome;
    private GestureDetector mGestureDetector;
    private static final int ERROR_VIEW = 1;
    private static final int WELCOME_VIEW = 2;

    private int mCurrentView = 0;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
	if(DEBUG) Log.e(TAG, "onCreate in");
	super.onCreate(savedInstanceState);
	SysApplication.getInstance().addActivity(this); 
	setContentView(R.layout.activity_welcome);
    }
    
    @Override
	protected void onStart() {
	mCurrentView = 0;
	gestureDetectorWorker();
	Enabke();
	
	SharedPreferences tsp = getSharedPreferences("MAC_INFO", MODE_PRIVATE);
	String mAddress=tsp.getString("mAddress", null);
	if(DEBUG) Log.e(TAG, "mAddress:" + mAddress);
	if (mAddress!=null) {			
	    Intent bind = new Intent(Welcome_Activity.this,Bind_Activity.class);
	    // bind.putExtra("mAddress", mAddress);
	    bind.putExtra("Tag", 1);
	      //  bind.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); 
	    startActivity(bind);
	    
	} else {
	    TextView tv = (TextView) findViewById(R.id.pre_info);
	    tv.setVisibility(View.INVISIBLE);
	    
	    mlayout_welcome = (LinearLayout) findViewById(R.id.Layout_welcome);
	    mlayout_welcome.setOnTouchListener(this);
	    mlayout_welcome.setVisibility(View.VISIBLE);			
	    mCurrentView = WELCOME_VIEW;
	}
	super.onStart();
    }

    private void Enabke() {
        sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	if (sBluetoothAdapter == null) {
	    Log.e(TAG, "now bl state:" + sBluetoothAdapter);
	    ShowErrorView();
	    Log.e(TAG, "enabke sBluetoothAdapter is done");
	}else if (!sBluetoothAdapter.isEnabled()) {
	    sBluetoothAdapter.enable();
	}
    } 

    private void ShowErrorView(){
	ViewGroup root  = (ViewGroup) findViewById(R.id.main);
	View layout=getLayoutInflater().inflate(R.layout.error_view,null);
	layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	TextView tv = (TextView) layout.findViewById(R.id.tv_err);
	tv.setText(R.string.err_nofind_bluetooth);
	
	mCurrentView = ERROR_VIEW;
	root.addView(layout);

	((TextView) findViewById(R.id.pre_info)).setVisibility(View.INVISIBLE);
	
	layout.setVisibility(View.VISIBLE);
	layout.setOnTouchListener(this);
    }

    private void gestureDetectorWorker(){
	mGestureDetector =  new GestureDetector(this,new SimpleOnGestureListener() {
		      // Touch down时触发
		    @Override
			public boolean onDown(MotionEvent e) {
			if(DEBUG) Log.d(TAG,"---onDown in");
			return true;
		    }
		    
		    @Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					       float velocityX, float velocityY) {
			if(DEBUG) Log.d(TAG,"---velocityX="+velocityX+"--velocityY="+velocityY);
			if (velocityY > SysApplication.SNAP_VELOCITY 
			    && Math.abs(velocityX) < SysApplication.SNAP_VELOCITY) {
			    Welcome_Activity.this.finish();
			    System.exit(0);
			}
			return true;
		    }
		    
		    @Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
			if(DEBUG) Log.d(TAG,"---onSingleTapConfirmed in");
			if(mCurrentView == WELCOME_VIEW){
			    Intent sacn = new Intent(Welcome_Activity.this,CaptureActivity.class);
			    startActivity(sacn);
			    Welcome_Activity.this.finish();
			}
			return true;
		    }
		    
	    });
    }

    @Override
	public boolean onTouch(View v, MotionEvent event) {
	Log.e(TAG, "onTouch in");
	return mGestureDetector.onTouchEvent(event);
    }	

}

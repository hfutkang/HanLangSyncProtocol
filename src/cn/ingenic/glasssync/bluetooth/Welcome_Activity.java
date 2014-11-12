package cn.ingenic.glasssync.bluetooth;
import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnTouchListener;
import android.widget.GestureDetector;
import android.widget.GestureDetector.SimpleOnGestureListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Welcome_Activity extends Activity implements OnTouchListener{
    private static final String TAG = "Welcome_Activity";
    private static final boolean DEBUG = true;

    public static BluetoothAdapter sBluetoothAdapter;
    public BluetoothDevice pairDeivce;
    private LinearLayout mlayout_welcome,mlayout_pairing;
    private GestureDetector mGestureDetector;
    private static final int ERROR_VIEW = 1;
    private static final int WELCOME_VIEW = 2;
    private static final int PAIRING_VIEW=3;
    private TextView mPairing_info;
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
	    Welcome_Activity.this.finish();
	    
	} else {
		String start=getIntent().getStringExtra("start");
		if(null!=start){
			MediaPlayer player = MediaPlayer.create(Welcome_Activity.this, R.raw.pair);
			player.start();
			mlayout_pairing=(LinearLayout)findViewById(R.id.Layout_pairing);
			mPairing_info=(TextView) findViewById(R.id.device_info);
			mlayout_pairing.setVisibility(View.VISIBLE);
			mlayout_pairing.setOnTouchListener(this);
			pairDeivce=getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			mPairing_info.setText(pairDeivce.getName());
			mCurrentView=PAIRING_VIEW;
		}else{
			TextView tv = (TextView) findViewById(R.id.pre_info);
		    tv.setVisibility(View.INVISIBLE);
		    
		    mlayout_welcome = (LinearLayout) findViewById(R.id.Layout_welcome);
		    mlayout_welcome.setOnTouchListener(this);
		    mlayout_welcome.setVisibility(View.VISIBLE);			
		    mCurrentView = WELCOME_VIEW;
		}
	    
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
		  @Override
		    public boolean onDown(boolean fromPhone){		    
			    return true;
		    }
		    @Override
		    public boolean onSlideDown(boolean fromPhone){		    
			    Welcome_Activity.this.finish();
			    System.exit(0);
			    return true;
		    }
		    
		    @Override
		    public boolean onTap(boolean fromPhone){
			if(DEBUG) Log.d(TAG,"---onTap in");
			if(mCurrentView == WELCOME_VIEW){
			    Intent sacn = new Intent(Welcome_Activity.this,CaptureActivity.class);
			    startActivity(sacn);
			    Welcome_Activity.this.finish();
			}else if(mCurrentView==PAIRING_VIEW){
				pairDeivce.setPairingConfirmation(true);
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

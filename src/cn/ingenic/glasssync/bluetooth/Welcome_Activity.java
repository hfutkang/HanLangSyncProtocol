package cn.ingenic.glasssync.bluetooth;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.DefaultSyncManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Welcome_Activity extends Activity {
    private static final String TAG = "Welcome_Activity";
	private LinearLayout mlayout_welcome,mlayout_welcome1;
	private TextView mTv_mobile;
	private TextView mWelcome_content;
	private String Welcome_content;
	public static BluetoothAdapter sBluetoothAdapter;
	private DefaultSyncManager mManger;
	private String mMobileAddress;
	private String mMobile_name;
	private int mTag = 1;
	private float x1 = 0;
	private float x2 = 0;
	private float y1 = 0;
	private float y2 = 0;
	private Editor editor;
	private SharedPreferences mpreferences;
	private BluetoothDevice mdevice;
	public String mAddress;
    private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    Log.e(TAG, "onCreate in");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		mpreferences = getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		mlayout_welcome1=(LinearLayout) findViewById(R.id.Layout_welcome1);
		mlayout_welcome = (LinearLayout) findViewById(R.id.Layout_welcome);
		mContext = this;
		SysApplication.getInstance().addActivity(this); 
	//	mTv_mobile = (TextView) findViewById(R.id.tv_mobile);
		//mWelcome_content=(TextView) findViewById(R.id.welcome_content);
	//	mManger = DefaultSyncManager.getDefault();
		enabke();
		Log.e(TAG, "enabke end");
		 //mAddress=getIntent().getStringExtra("mAddress");
		// SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		// tsp.getString(mAddress,"");
		   //mAddress=getIntent().getStringExtra("mAddress");
		 SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		  mAddress=tsp.getString("mAddress", null);
		 // Log.e(TAG, "mAddress:" + mAddress);
		  // Editor editor = tsp.edit();
		//	editor.String("mAddress", mdevice.getName());
		Log.e(TAG, "mAddress:" + mAddress);
	
	
		mlayout_welcome.setOnTouchListener(new myTouchListener());
	
	}

	@Override
	protected void onStart() {
		Log.e(TAG, "onstart in ");
		if (mAddress!=null) {
			//Log.e(TAG, "mAddress:" + mAddress);
			
		    Intent bind = new Intent(Welcome_Activity.this,
						Bind_Activity.class);
		    bind.putExtra("mAddress", mAddress);
		    bind.putExtra("Tag", 1);
		  //  bind.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); 
		    startActivity(bind);
			
		} else {
			
			mlayout_welcome.setVisibility(View.VISIBLE);
			
		}
		super.onStart();
	}

    private void enabke() {
    	sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
if (sBluetoothAdapter == null) {
	Log.e(TAG, "now bl state:" + sBluetoothAdapter);
	Toast.makeText(this, "本机没有找到蓝牙硬件或驱动！", Toast.LENGTH_SHORT).show();
	finish();

Log.e(TAG, "enabke sBluetoothAdapter is done");}

else if (!sBluetoothAdapter.isEnabled()) {
	sBluetoothAdapter.enable();
}
} 


    class myTouchListener implements OnTouchListener {
	long start;
	long end;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
	    Log.e(TAG, "onTouch in");
	    // TODO Auto-generated method stub
	    if (event.getAction() == MotionEvent.ACTION_DOWN) {
		start = System.currentTimeMillis();
		Log.d("Tag", "1 start_time" + start);
		x1 = event.getX();
		y1 = event.getY();
		Log.e("Tag","x1"+x1);
		Log.e("Tag","y1"+y1);
		//v.getDisplay().getDisplayId();
	    }
	    if (event.getAction() == MotionEvent.ACTION_UP ){
			x2 = event.getX();
			y2 = event.getY();
			Log.e("Tag","x2"+x2);
			Log.e("Tag","y1"+y2);	    		    	
	    }
	    
		
		if (Math.abs(x1 - x2) < 50 && end - start < 2000 ) {
		    Log.d("Tag", "click");
		    Intent sacn = new Intent(Welcome_Activity.this,CaptureActivity.class);
		    //Intent sacn = new Intent(Welcome_Activity.this,Load_Activity.class);

		    startActivity(sacn);
		} else if (y2 - y1 > 50) {
		    Log.d("Tag", "down");
		    Welcome_Activity.this.finish();
		    System.exit(0);
		}
			

	    return true;}
	
	}
    }

package cn.ingenic.glasssync.bluetooth;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.DefaultSyncManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
	private LinearLayout mlayout_welcome, mLayout_bind;
	private TextView mTv_mobile;
	private String mMobile_name;
	private SharedPreferences mpreferences;
	private BluetoothDevice mdevice;
	private String mAddress;
	public static BluetoothAdapter sBluetoothAdapter;
	private DefaultSyncManager mManger;
	private String mMobileAddress;
	private int mTag = 1;
	private float x1 = 0;
	private float x2 = 0;
	private float y1 = 0;
	private float y2 = 0;
	private Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    Log.e(TAG, "onCreate in");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		mpreferences = getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		mLayout_bind = (LinearLayout) findViewById(R.id.layout_bind);
		mlayout_welcome = (LinearLayout) findViewById(R.id.Layout_welcome);
		mTv_mobile = (TextView) findViewById(R.id.tv_mobile);
		mManger = DefaultSyncManager.getDefault();
		mAddress = mpreferences.getString("mAddress", null);
		Log.e(TAG, "mAddress:" + mAddress);
		visible();
		enabke();
		Log.e(TAG, "enabke end");
		mlayout_welcome.setOnTouchListener(new myTouchListener());
		mLayout_bind.setOnTouchListener(new myTouchListener());
	}

	@Override
	protected void onStart() {
	    Log.e(TAG, "onStart in");
		mAddress = mpreferences.getString("mAddress", null);
		Log.e(TAG, "onStart mAddress:" + mAddress);
		editor= mpreferences.edit();
		mMobile_name = mpreferences.getString("mDevice", null);
		mMobileAddress = getIntent().getStringExtra("mAddress");
		mTag = getIntent().getIntExtra("Tag", 1);
		Log.e(TAG, "mMobile_name:" + mMobile_name + " mMobileAddress:" + mMobileAddress + " mTag:" + mTag);
		if (mAddress != null) {
			mdevice = sBluetoothAdapter.getRemoteDevice(mAddress);
			// Toast.makeText(this, mMobile_name + "-------mMobileAddress---------",
			// 		Toast.LENGTH_LONG).show();

			if (mTag == 1) {
				mMobile_name = mdevice.getName();
				mTv_mobile.setText(mMobile_name);
				if (mAddress != null)
				    mManger.glass_connect(mAddress);
			} else if (mTag == 2) {
			    if (mMobileAddress != null){
				mAddress = mMobileAddress;
				editor.putString("mAddress", mMobileAddress);
				editor.commit();
			    }
			    mLayout_bind.setVisibility(View.VISIBLE);
			    mlayout_welcome.setVisibility(View.GONE); 
				
			    mTv_mobile.setText(mMobile_name);				
			}
		} else if (mAddress == null) {
			if (mTag == 2) {
				mLayout_bind.setVisibility(View.VISIBLE);
				mlayout_welcome.setVisibility(View.GONE);
				mTv_mobile.setText(mMobile_name);	
				editor.putString("mAddress", mMobileAddress);
				editor.commit();
			}
			
			
		}
		super.onStart();
	}

	private void visible() {
		if (mAddress != null) {
			mLayout_bind.setVisibility(View.VISIBLE);
			mlayout_welcome.setVisibility(View.GONE);
		} else {
			mlayout_welcome.setVisibility(View.VISIBLE);
			mLayout_bind.setVisibility(View.GONE);
		}
	}

	private void enabke() {
		sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (sBluetoothAdapter == null) {
			Toast.makeText(this, "本机没有找到蓝牙硬件或驱动！", Toast.LENGTH_SHORT).show();
			finish();
		}
		Log.e(TAG, "enabke sBluetoothAdapter is done");

		// sBluetoothAdapter.disable();
		// Log.e(TAG, "disable bl sleep 8sec");
		// try {
		//     Thread.sleep(8000);
		// } catch (InterruptedException e) {
		// }
		// Log.e(TAG, "now bl state:" + sBluetoothAdapter.getState());

		// sBluetoothAdapter.enable();
		// Log.e(TAG, "enable bl sleep 8sec");
		// try {
		//     Thread.sleep(8000);
		// } catch (InterruptedException e) {
		// }
		// Log.e(TAG, "now bl state:" + sBluetoothAdapter.getState());

		if (!sBluetoothAdapter.isEnabled()) {
			sBluetoothAdapter.enable();
		}
	}

	class myTouchListener implements OnTouchListener {
		long start;
		long end;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				start = System.currentTimeMillis();
				Log.d("Tag", "start_time" + start);
				x1 = event.getX();
				y1 = event.getY();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				x2 = event.getX();
				y2 = event.getY();
				end = System.currentTimeMillis();
				Log.d("Tag", "end_time" + end);
				if (Math.abs(x1 - x2) < 10 && end - start < 2000) {
					Log.d("Tag", "click");
					// Intent sacn = new Intent(Welcome_Activity.this,
					// 		CaptureActivity.class);
					// startActivity(sacn);
					Intent sacn = new Intent(Welcome_Activity.this,
					 		Load_Activity.class);
					startActivity(sacn);
					Welcome_Activity.this.finish();
				} else if (Math.abs(x1 - x2) < 10 && end - start > 1400) {
					Log.d("Tag", "longclick");
					mManger.disconnect();
					Toast.makeText(getApplicationContext(), "您已成功解除绑定",
							Toast.LENGTH_LONG).show();
					mlayout_welcome.setVisibility(View.VISIBLE);
					mLayout_bind.setVisibility(View.GONE);
				} else if (y2 - y1 > 50) {
					Log.d("Tag", "down");
					Welcome_Activity.this.finish();
					System.exit(0);
				}
			}
			return true;
		}
	}
}

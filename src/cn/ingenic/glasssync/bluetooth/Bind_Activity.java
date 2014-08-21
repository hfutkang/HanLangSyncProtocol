package cn.ingenic.glasssync.bluetooth;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.bluetooth.Welcome_Activity.myTouchListener;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Bind_Activity extends Activity{
	 private static final String TAG = "Bind_Activity";
		private LinearLayout mlayout_bind;
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

		Log.e(TAG, "bind_onCreate in");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bind);
		mManger = DefaultSyncManager.getDefault();
		mpreferences = getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		mlayout_bind = (LinearLayout) findViewById(R.id.activity_bind);
		mlayout_bind.setOnTouchListener(new myTouchListener());
		mTv_mobile= (TextView)findViewById(R.id.tv_mobile);
		SysApplication.getInstance().addActivity(this); 
		mContext = this;
	
	}

	 protected void onStart() {
         //mAddress = mpreferences.getString("mAddress", null);
         // Log.e(TAG, "onStart mAddress:" + mAddress);
          //editor= mpreferences.edit();
          mMobile_name = mpreferences.getString("mDevice", null);
          mMobileAddress= mpreferences.getString("mAddress", null);
       //  mMobileAddress = getIntent().getStringExtra("mdevice");
         Log.e(TAG, "mMobileAddress:" + mMobileAddress);
        // mAddress=getIntent().getStringExtra("mAddress");
         SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
		   //mAddress=tsp.getString(mAddress, null);
          mTag = getIntent().getIntExtra("Tag", 1);
          Log.e(TAG, "mMobile_name:" + mMobile_name + " mMobileAddress:" + mMobileAddress + " mTag:" + mTag);
          sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
          Log.e(TAG, "sBluetoothAdapter:" + sBluetoothAdapter);
     
	 //   mdevice = sBluetoothAdapter.getRemoteDevice(mAddress);
	   // Log.e(TAG, "sBluetoothAdapter:" + sBluetoothAdapter);
	   // Log.e(TAG, mdevice + "-------mdevice---------");

	 if (mTag == 1) {
	 	//mMobile_name = mdevice.getName();
	 	Log.e(TAG,"mdevice"+mdevice);
	 	Log.e(TAG,"mMobile_name"+mMobile_name);
	 	mTv_mobile.setText(mMobile_name);
	 	//if (mAddress != null)
	 	    mManger.glass_connect(mMobileAddress);
	 } else if (mTag == 2) {
		 Log.e(TAG,"mTag"+mTag);
		 Log.e(TAG,"mMobileAddress"+mMobileAddress+"tv.gettext"+mTv_mobile.getText().toString());
		mTv_mobile.setText(mMobile_name);
		//mAddress = mMobileAddress;
		//editor.putString("mAddress", mMobileAddress);
	 	//editor.commit();
	 }
	 super.onStart();
	 } 
	

		//mTv_mobile.setText(mMobile_name);	
		//editor.putString("mAddress", mMobileAddress);
		//editor.commit();

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
			    	x2=event.getX();
				y2 = event.getY();
				end = System.currentTimeMillis();
				Log.e("Tag","x2"+x2);
				Log.e("Tag","y2"+y2);
				Log.e("TAG", "1 end_time" + end);
				Log.e("TAG","mMobileAddress"+mMobileAddress);	
			    }
			    if (Math.abs(x1 - x2) < 50 && end - start > 2000) {
				    Log.d("Tag", "long"+ "click");
				    mManger.disconnect();
				    
				    Log.e("TAG","disbind_mAddress"+mMobileAddress);	
				    Toast.makeText(getApplicationContext(), "您已成功解除绑定",
						   Toast.LENGTH_LONG).show();
				    /**/

					SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
					//Log.d(TAG,mAddress_connect);
					Editor editor = tsp.edit();
					editor.putString("mAddress", null);
					
					//editor.commit();
				    editor.putString("mDevice", null);
				   
					editor.commit();
					Log.e(TAG, "mdevice:" + mdevice.getName());
				    
				    Intent disbind = new Intent(Bind_Activity.this,
								Disbind_Activity.class);
				     
				    disbind.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				    startActivity(disbind);
				  
				   // mlayout_welcome.setVisibility(View.VISIBLE);
				    //mLayout_bind.setVisibility(View.GONE);
				}else if (y2 - y1 > 50) {
				    Log.d("Tag", "down");
				   // Bind_Activity.this.finish();
				    SysApplication.getInstance().exit();
				   // System.exit(0);
				}
					
			    return true;
				}
			
			   
		    }

}






















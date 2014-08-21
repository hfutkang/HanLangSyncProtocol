package cn.ingenic.glasssync.bluetooth;


import java.util.Stack;

import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Disbind_Activity extends Activity{
	private LinearLayout mLayout_disbind;
	private float x1 = 0;
	private float x2 = 0;
	private float y1 = 0;
	private float y2 = 0;
	public String mAddress;
    private Context mContext;
	private static Stack<Activity> activityStack;

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_disbind);
		mLayout_disbind=(LinearLayout)findViewById(R.id.Layout_disbind);
		mLayout_disbind.setOnTouchListener(new myTouchListener());
		mContext = this;
		SysApplication.getInstance().addActivity(this);  
		
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
				Log.e("Tag","x1"+x1);
				Log.e("Tag","y1"+y1);
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				x2 = event.getX();
				y2 = event.getY();
				Log.e("Tag","x2"+x2);
				Log.e("Tag","y2"+y2);
				end = System.currentTimeMillis();
				Log.d("Tag", "2end_time" + end);
				if (y2 - y1 > 50) {
					//Activity activity_top=activityStack.lastElement();
					//Log.e("activity_top", activity_top+"");
					Log.d("Tag", "down");
					SysApplication.getInstance().exit();
					//Disbind_Activity.this.finish();
				
				} else if(Math.abs(x1 - x2) < 50 && end - start < 2000 ){
					Log.d("Tag", "click");
					//String str=findViewById(R.string.welcome_content);
					//mWelcome_content.setText("请在手机端确认绑定信息");
				
					//mWelcome_content
					
					SharedPreferences tsp = mContext.getSharedPreferences("MAC_INFO", MODE_PRIVATE);
					
					Editor editor = tsp.edit();
					editor.putString("mAddress", null);
					editor.commit();
					
					//Log.d("disbind_mAddress", mAddress);
				
					Log.d("disbind_tsp", tsp+"");
					
					
					Intent load = new Intent(Disbind_Activity.this,
							Welcome_Activity.class);
					//load.putExtra("mdevice", mdevice.getName());
					//load.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(load);
					
					/*Intent sacn = new Intent(Welcome_Activity.this,
					 		Load_Activity.class);
					startActivity(sacn);
					*/
				//	Welcome_Activity.this.finish();
				}
			}
			return true;
		}
	}}
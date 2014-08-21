package cn.ingenic.glasssync.bluetooth;

import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class Unbind_Activity extends Activity {
    private String TAG = "Unbind_Activity";
	private RelativeLayout mLayout_Unbind;
	private float x1 = 0;
	private float x2 = 0;
	private float y1 = 0;
	private float y2 = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "Unbind_Activity onCreate in");
		setContentView(R.layout.activity_unbind);
		mLayout_Unbind = (RelativeLayout) findViewById(R.id.layout_unbind);
		Toast.makeText(getApplicationContext(), R.string.toast_message,
				Toast.LENGTH_LONG).show();
		mLayout_Unbind.setOnTouchListener(new myTouchListener());
		SysApplication.getInstance().addActivity(this); 
	}

	class myTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if(event.getAction() == MotionEvent.ACTION_DOWN) {  
	            x1 = event.getX();  
	            y1 = event.getY();  
	        }  
		 if(event.getAction() == MotionEvent.ACTION_UP) {   
	            x2 = event.getX();  
	            y2 = event.getY();  
	             if(Math.abs(x1 - x2) < 6) { 
	            	Intent intent=new Intent(Unbind_Activity.this,CaptureActivity.class);
	  			  startActivity(intent);
	  			  finish();
	            	}
	            if(y2 - y1 > 50) { 
	            	SysApplication.getInstance().exit();
	            	finish();
	            } 
	        } 
			return true;
		}
	}
}
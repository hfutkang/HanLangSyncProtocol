package cn.ingenic.glasssync.updater;

import java.io.File;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import cn.ingenic.glasssync.R;
/**
 * a dialog window  to display some notices or result of some operations.
 * @author dfdun
 * */
public class NoticesActivity extends Activity {

	static final String PREFS_NAME= "pre_ota", Key="ota_path";
	SharedPreferences settings;
	SharedPreferences.Editor editor;
    private String path;
    private Button mOk,mLater;
    NotificationManager mNotificationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notices_activity);
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();
        mOk = (Button)findViewById(R.id.ok);
        mLater= (Button)findViewById(R.id.later);
        path=getIntent().getStringExtra("filename");
		if (null == path)
			path = settings.getString(Key, "dd");
		mOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startOta();
			}
		});
		mLater.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				NoticesActivity.this.finish();
			}
		});
    	mNotificationManager.cancel(101);
	}

    protected void onDestroy(){
        super.onDestroy();
        later();
    }
    private void startOta(){
    	mNotificationManager.cancel(101);
        Log.i("OtaUpdater","NoticesActivity] - ,sendBroadcast " +
        		"[android.intent.action.MASTER_CLEAR] to reboot."+path);
        // MasterClearReceiver.java to receive . and reboot
        Intent in= new Intent("android.intent.action.MASTER_CLEAR");
        in.putExtra("ota", "ota");
        in.putExtra("filename", path);
        sendBroadcast(in);
    }
    private void later(){
    	editor.putString(Key,path);
    	editor.commit();
        long when = System.currentTimeMillis();
        CharSequence contentTitle = getText(R.string.app_name);
        CharSequence contentText = getText(R.string.update_later_msg);
        
        Intent intent= new Intent(this,NoticesActivity.class);
        PendingIntent cIntent=PendingIntent.getActivity(this, 0, intent, 0);

		Notification.Builder builder = new Notification.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setTicker(getString(R.string.update_later_msg))
				.setWhen(when + 100).setContentIntent(cIntent)
				.setContentTitle(contentTitle).setContentText(contentText)
				.setAutoCancel(false).setOngoing(true);
		mNotificationManager.notify(101, builder.getNotification());
    }
}

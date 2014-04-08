package cn.ingenic.glasssync.notify;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import cn.ingenic.glasssync.R;

public class GlassSyncNotifyModule extends SyncModule {
    private static final String TAG = "GlassSyncNotifyModule";
    private static final String LETAG = "GSNFMD";

    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TEXT = "android.text";

    public static final int micro_message_id = 0x7F01;

    private Context mContext;
    private static GlassSyncNotifyModule sInstance;

    public GlassSyncNotifyModule(Context context){
	super(LETAG, context);
	mContext = context;
    }
    /*
    public static GlassSyncNotifyModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncNotifyModule(c);
	return sInstance;
    }
    */
    @Override
    protected void onCreate() {
	Log.e(TAG, "onCreate in");
    }

    /*
    public void sendNotify(Notification notify) {
	SyncData data = new SyncData();
	data.putNotification(NOTIFY_SEND_TO_GLASS, notify);
	try {
	    Log.e(TAG, "send notify() " + notify);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
    */

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String title = data.getString(EXTRA_TITLE);
	String text = data.getString(EXTRA_TEXT);

	NotificationManager nManager = (NotificationManager) mContext.getSystemService("notification");
	Notification noti = new Notification.Builder(mContext)
	    .setSmallIcon(R.drawable.mm_small)
	    .setTicker(title)
	    .setContentTitle(title)
	    .setContentText(text)
	    .build();

	nManager.notify(micro_message_id, noti);

	Log.e(TAG, "title:" + title + " text" + text);
    }
}
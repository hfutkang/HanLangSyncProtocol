/**
 * @author dfdun
 *  from Phone/src/.../PhoneApp.java & NotificationMgr.java 
 *  for notify missed call log.
 */

package cn.ingenic.glasssync.calllog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import cn.ingenic.glasssync.R;

public class MissedCallNotify {

    private static final String MISSEDCALL_TAG = "missed_call";
    static final int MISSED_CALL_NOTIFICATION = 1;
    static MissedCallNotify mInstance=null;
    static Context mContext;
    static NotificationManager mNotificationManager;
    private static String mCallName,mNumber;
	
	public static MissedCallNotify getInstance(Context c){
		if(mInstance == null){
			mInstance = new MissedCallNotify(c);
		}
		return mInstance;
	}
	private MissedCallNotify (Context context){
		mContext=context;
		mNotificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	void cancelMissedCallNotification(){
	    mNotificationManager.cancel(MISSEDCALL_TAG,MISSED_CALL_NOTIFICATION);
	}
		
     public static void notifyMissedCall(
	String name, String number, long date) {

        // When the user clicks this notification, we go to the call log.
        final Intent callLogIntent =  new Intent(Intent.ACTION_VIEW, null);
        callLogIntent.setType("vnd.android.cursor.dir/calls");

        // title resource id
        int titleResId;
        // the text in the notification's line 1 and 2.
        String expandedText, callName;

        // increment number of missed calls.
        // get the name for the ticker text
        // i.e. "Missed call from <caller name or number>"
        if (name != null && TextUtils.isGraphic(name)) {
            callName = name;
        } else if (!TextUtils.isEmpty(number)){
            callName = number;
        } else {
            // use "unknown" if the caller is unidentifiable.
            callName = mContext.getString(R.string.unknown);
        }
	mNumber = number;
	mCallName = callName;
        /* display the first line of the notification:
	   1 missed call: call name
	   more than 1 missed call: <number of calls> + "missed calls"*/

        titleResId = R.string.notification_missedCallTitle;
        expandedText = callName;


        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call)
                .setTicker(mContext.getString(R.string.notification_missedCallTicker, mCallName))
                .setWhen(date)
                .setContentText(expandedText)
                .setContentTitle(mContext.getText(titleResId))
	    .setAutoCancel(true)
	    .setContentIntent(callIntent());

        Notification notification = builder.getNotification();
        configureLedNotification(notification);
        mNotificationManager.notify(MISSEDCALL_TAG, MISSED_CALL_NOTIFICATION, notification);
    }
	
    private static void configureLedNotification(Notification note) {
        note.flags |= Notification.FLAG_SHOW_LIGHTS;
        note.defaults |= Notification.DEFAULT_LIGHTS;
    }
    
    private static PendingIntent callIntent(){
	Intent intent = new Intent(mContext,missedCallAction.class);
	intent.putExtra("mUserName", mCallName);
	intent.putExtra("mContact",mNumber);
	intent.putExtra("start", "contact");
	intent.putExtra("info",2);
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	PendingIntent contentIntent = PendingIntent.getActivity(mContext,0,intent,0);
	return contentIntent;
    }
}

/**
 * @author dfdun
 *  from Phone/src/.../PhoneApp.java & NotificationMgr.java 
 *  for notify missed call log.
 */

package cn.ingenic.glasssync.calllog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallerInfo;
import cn.ingenic.glasssync.R;

import java.util.ArrayList;
public class MissedCallNotify {

    private static final String MISSEDCALL_TAG = "missed_call";
	static MissedCallNotify mInstance=null;
    static final int MISSED_CALL_NOTIFICATION = 1;
    static Context mContext;
    static NotificationManager mNotificationManager;
    private static final int MISSEDCALL_TOKEN = -3;//dfdun add
    private QueryHandler mQueryHandler = null;
    private int mNumberMissedCalls = 0;
    private ArrayList<NotificationInfo> mNoPickUp = new ArrayList<NotificationInfo>();
	
	public static MissedCallNotify getInstance(Context c){
		if(mInstance == null){
			mInstance = new MissedCallNotify(c);
		}
		return mInstance;
	}
	private MissedCallNotify (Context context){
		mContext=context;
		mQueryHandler = new QueryHandler(context.getContentResolver());
		mNotificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	void cancelMissedCallNotification(){
		mNumberMissedCalls = 0;
		mNotificationManager.cancel(MISSEDCALL_TAG, MISSED_CALL_NOTIFICATION);
	}
	
    void queryAndNotifyMissedCall(){
    	mNumberMissedCalls = 0;
    	cancelMissedCallNotification();
        mQueryHandler.startQuery(MISSEDCALL_TOKEN, null, Calls.CONTENT_URI, null,  Calls.TYPE + "=3"
                + " and " + Calls.NEW + "=1", null, null);
    }
	
    private void notifyMissedCall(
	String name, String number, String type, Drawable photo, 
	Bitmap photoIcon, long date, int count) {

        // When the user clicks this notification, we go to the call log.
        final Intent callLogIntent =  new Intent(Intent.ACTION_VIEW, null);
        callLogIntent.setType("vnd.android.cursor.dir/calls");

        // title resource id
        int titleResId;
        // the text in the notification's line 1 and 2.
        String expandedText, callName;

        // increment number of missed calls.
        mNumberMissedCalls++;

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

        /* display the first line of the notification:
	   1 missed call: call name
	   more than 1 missed call: <number of calls> + "missed calls"*/

        titleResId = R.string.notification_missedCallTitle;
        expandedText = callName;


        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call)
                .setTicker(mContext.getString(R.string.notification_missedCallTicker, callName))
                .setWhen(date)
                .setContentTitle(mContext.getText(titleResId)+"("+count+")")
                .setContentText(expandedText)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, callLogIntent, 0))
                .setAutoCancel(true);
//                .setDeleteIntent(createClearMissedCallsIntent());


        Notification notification = builder.getNotification();
        configureLedNotification(notification);
        mNotificationManager.notify(MISSEDCALL_TAG, MISSED_CALL_NOTIFICATION, notification);
    }
	
    private static void configureLedNotification(Notification note) {
        note.flags |= Notification.FLAG_SHOW_LIGHTS;
        note.defaults |= Notification.DEFAULT_LIGHTS;
    }
    
//    private PendingIntent createClearMissedCallsIntent() {
//        Intent intent = new Intent(mContext, ClearMissedCallsService.class);
//        intent.setAction(ClearMissedCallsService.ACTION_CLEAR_MISSED_CALLS);
//        return PendingIntent.getService(mContext, 0, intent, 0);
//    }
	
	private class QueryHandler extends AsyncQueryHandler{
				
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }
        
        /**
         * Handles the query results.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case MISSEDCALL_TOKEN:      //dfdun add this case.
                if (cursor != null) {
		    		ArrayList<String> list = new ArrayList<String>();
                    while (cursor.moveToNext()) {
                        NotificationInfo n = getNotificationInfo (cursor);
                        n.name=cursor.getString(cursor.getColumnIndex(Calls.CACHED_NAME));
			//notifyMissedCall(n.name, n.number, n.type, null, null, n.date,);
			if(!list.contains(n.name)){
			    list.add(n.name);
			    n.count = 1;
			    mNoPickUp.add(n);
			}else{
			    ++ mNoPickUp.get(list.indexOf(n.name)).count;
			}		
                    }
		    for(int i = 0 ;i<mNoPickUp.size();i++ ){
			Log.e("qqli","MissedCallNotify --"+mNoPickUp.get(i).name+"--"+mNoPickUp.size());
			notifyMissedCall(mNoPickUp.get(i).name, mNoPickUp.get(i).number, 
					 mNoPickUp.get(i).type, null, null, mNoPickUp.get(i).date,
					 mNoPickUp.get(i).count);
		    }
		    list.clear();
		    mNoPickUp.clear();
                }
                break;
            default:
        }
    }
        private final NotificationInfo getNotificationInfo(Cursor cursor) {
            NotificationInfo n = new NotificationInfo();
            n.name = null;
            n.number = cursor.getString(cursor.getColumnIndexOrThrow(Calls.NUMBER));
            n.type = cursor.getString(cursor.getColumnIndexOrThrow(Calls.TYPE));
            n.date = cursor.getLong(cursor.getColumnIndexOrThrow(Calls.DATE));

            if ( (n.number.equals(CallerInfo.UNKNOWN_NUMBER)) ||
                 (n.number.equals(CallerInfo.PRIVATE_NUMBER)) ||
                 (n.number.equals(CallerInfo.PAYPHONE_NUMBER)) ) {
                n.number = null;
            }
            return n;
        }
	}
		
    public class NotificationInfo {
	public String name;
	public String number;
	public String type;
	public long date;
	public int count;
    }
}

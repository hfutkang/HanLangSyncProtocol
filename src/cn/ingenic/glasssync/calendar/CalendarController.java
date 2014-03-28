package cn.ingenic.glasssync.calendar;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.vcalendar.VCalendarBuilder;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;

import java.util.ArrayList;
import java.util.EnumSet;

import cn.ingenic.glasssync.Config;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;

public class CalendarController {
	
	private static final String TAG = "CalendarController";
	
    private static CalendarController sInstance;
    private static Context mContext;
    private static MyThread mThread;
    static ExecuteHandler mHandler;

	public final static String UPDATE_AGENDA_TO_WATCH_TAG = "update_agenda_to_watch_tag";
	public final static String DELETE_ALL_TO_WATCH_TAG = "delete_all_to_watch_tag";
	public final static String WATCH_DELETE_AGENDA_TAG = "watch_delete_agenda_tag";
	public final static String WATCH_REQUEST_UPDATE_TAG = "watch_request_update_tag";

    public static final String[] PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE,
        Calendars.SYNC_EVENTS};
    
	public static final int INDEX_ID = 0;
	public static final int INDEX_ACCOUNT_NAME = 1;
	public static final int INDEX_ACCOUNT_TYPE = 2;
	public static final int INDEX_SYNC_EVENTS = 3;

    public static final int ACCOUNT_SYNC =1;
    public static final int ACCOUNT_NOT_SYNC =0;
  
    public static final String SELECTION =Calendars.SYNC_EVENTS+ "=?";
    public static String [] mArgs =new String[1];
    // is primary lets us sort the user's main calendar to the top of the list
    public static final String SORT_ORDER = Calendars._ID + " ASC " ;
    
    
    static final String[] ALERT_PROJECTION = new String[] {
        CalendarAlerts._ID,                     // 0
        CalendarAlerts.EVENT_ID,                // 1
        CalendarAlerts.STATE,                   // 2
        CalendarAlerts.TITLE,                   // 3
        CalendarAlerts.EVENT_LOCATION,          // 4
        CalendarAlerts.SELF_ATTENDEE_STATUS,    // 5
        CalendarAlerts.ALL_DAY,                 // 6
        CalendarAlerts.ALARM_TIME,              // 7
        CalendarAlerts.MINUTES,                 // 8
        CalendarAlerts.BEGIN,                   // 9
        CalendarAlerts.END,                     // 10
        CalendarAlerts.DESCRIPTION,             // 11
    };
    
    public final static int MSG_UPDATE_ONE_EVENT=0;
    public final static int MSG_UPDATE_ALL=1;
    public final static int MSG_INSERT_ONE_EVENT=2;
    public final static int MSG_INSERT_ONE_ACCOUNT=3;
    public final static int MSG_INSERT_ALL=4;
    public final static int MSG_DELETE_ONE_EVENT=5;
    public final static int MSG_DELETE_ONE_ACCOUNT=6;
    public final static int MSG_DELETE_ALL=7;
    public final static int MSG_UPDATE_NOTIFICATION =8;
    public final static int MSG_UPDATE_REQUEST = 9;
    ContentResolver mContentResolver ;
    
    
    
    /** Singleton access */
    public static synchronized CalendarController getInstance(Context context) {
         if(sInstance ==null){
        	 sInstance = new CalendarController(context);
        }
        return sInstance;
    }
    
    private CalendarController(Context context) {
		mContext = context.getApplicationContext();
		mContentResolver=mContext.getContentResolver();
		ensureHandlerExists();
	}
    
    private  synchronized void ensureHandlerExists() {
        if (mThread == null) {
        	mThread = new MyThread();
        	mHandler = new ExecuteHandler(mThread.getLooper());
        }
    }
    
    class ExecuteHandler extends Handler{
 	   ExecuteHandler(Looper looper){
 		   super(looper);
 	   }
 	   
 	@Override
 	public void handleMessage(Message msg) {
 		switch(msg.what){
 		case MSG_UPDATE_ONE_EVENT:
 			int eventId = msg.arg1;
 			updateByEventId(eventId);
 			break;
 		case MSG_UPDATE_ALL:
 			doUpdate();
 			break;
 		case MSG_INSERT_ALL:
 			doInsert();
 			break;
 		case MSG_DELETE_ALL:
 			Bundle bundle =msg.getData();
 			doDeleteAll(bundle);
 			break;
 		case MSG_DELETE_ONE_EVENT:
 			doDelete(msg.arg1);
 			break;
// 		case MSG_UPDATE_NOTIFICATION:
// 			doSendNotificationInfo(bundle);
// 			break;
		case MSG_UPDATE_REQUEST:
			doRequestUpdate();
			break;
			default:
				Log.e(TAG, "there is no this msg");
			}
 		
 		super.handleMessage(msg);
 	}
 	
 }
    private void updateByEventId(int eventId){
    	// to do
    }
    
    private void doUpdate(){
		ArrayList<Projo> eventList = null ;

		mArgs[0] = Integer.toString(ACCOUNT_SYNC) ;
		Cursor accountCur = mContentResolver.query(Calendars.CONTENT_URI,
				PROJECTION, SELECTION, mArgs, SORT_ORDER);
		if (accountCur != null ){
          if( accountCur.getCount() > 0) {
			eventList = new ArrayList<Projo>(accountCur.getCount());
			accountCur.moveToFirst();
			do {
				String accountName=accountCur.getString(1);
				String accountType=	accountCur.getString(2);
				long calendarId = accountCur.getLong(0);
				Projo projo = new DefaultProjo(
						EnumSet.allOf(CalendarColumn.class), ProjoType.LIST);
				projo.put(CalendarColumn.accountname,accountName);
				projo.put(CalendarColumn.accounttype,accountType);
				projo.put(CalendarColumn.events, constuctVcalendar(calendarId));
				projo.put(CalendarColumn.tag,
						UPDATE_AGENDA_TO_WATCH_TAG);
				eventList.add(projo);
			} while (accountCur.moveToNext());
			
			sendEvents(eventList);
          } 
          
          accountCur.close();
		}
	}

	private void doInsert() {
		// to do
	}
	
	/*
	 * delete one event
	 */
	private void doDelete(int eventId) {
//		if (bundle == null) {
//			return;
//		}
//		int eventId = bundle.getInt("eventId");
		
		if (eventId == -1) {
			return;
		}
		
       
//		if(!CalendarSyncHelper.isSynced()){
//			return;
//		}
		ArrayList<Projo>  projos  = new ArrayList<Projo>();
		Projo projo = new DefaultProjo(
				EnumSet.allOf(CalendarColumn.class), ProjoType.DATA);
		projo.put(CalendarColumn.event_id,eventId);
		projo.put(CalendarColumn.tag,WATCH_DELETE_AGENDA_TAG);
		projos.add(projo);
		sendEvents(projos);
	}

	private void doRequestUpdate() {
		ArrayList<Projo> projos = new ArrayList<Projo>();
		Projo projo = new DefaultProjo(EnumSet.allOf(CalendarColumn.class),
				ProjoType.DATA);
		projo.put(CalendarColumn.tag, WATCH_REQUEST_UPDATE_TAG);
		projos.add(projo);
		sendEvents(projos);

	}
	
	/*
	 * delete all events
	 */
	private void doDeleteAll(Bundle bundle) {
		// to do
	}
	
    private ArrayList<String> constuctVcalendar(long accountId){
    	 ArrayList<String> eventList =new ArrayList<String>();
    	 String selection ="calendar_id="+accountId+" AND deleted = 0";
    	 String order =Events._ID+" ASC ";
		Cursor eventCursor = mContentResolver.query(Events.CONTENT_URI, 
												new String[]{Events._ID}, 
												selection, null,
												order);
		
		if (eventCursor == null || eventCursor.getCount() == 0) {
			eventList =null;
			Log.d(TAG,"nothing events in accountId:"+accountId);
		}else{
			eventCursor.moveToFirst();
			do {
				int mEventId = eventCursor.getInt(0);
				eventList.add(onBuilder(mEventId));
			} while (eventCursor.moveToNext());
		}
		eventCursor.close();
		return eventList;
		 
    }
   
    
    private String onBuilder(int eventId){
    	final VCalendarBuilder builder = new VCalendarBuilder();
    	builder.appendEvent(eventId,mContentResolver)
 		.appendReminders(eventId,mContentResolver)
 		.appendAttendee(eventId,mContentResolver);
        return builder.toString();
    }
    Handler  mHander = new Handler (){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.arg1){
			case  DefaultSyncManager.SUCCESS:
				Log.i(TAG,"SEND EVENTS SUCCESS");
				break;
			case DefaultSyncManager.NO_CONNECTIVITY:
				Log.i(TAG,"SEND EVENTS NO_CONNECTIVITY");
				break;
			case DefaultSyncManager.FEATURE_DISABLED:
				Log.i(TAG,"SEND EVENTS FEATURE_DISABLED");
			   break;
			case DefaultSyncManager.NO_LOCKED_ADDRESS:
				Log.i(TAG,"SEND EVENTS NO_LOCKED_ADDRESS");
				break;
			   default:
				   Log.e(TAG,"SEND EVENTS FAILED");
			}
		}
	};
    
	private void sendEvents(ArrayList<Projo> eventsList) {
		Log.d(TAG,"send Events on watch");
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		Config config = new Config(CalendarModule.CALENDAR);
		Message msg =  Message.obtain(mHander);
		config.mCallback = msg;
		manager.request(config, eventsList);
	}
    
    
    private static class MyThread implements Runnable {
        /** Lock to ensure proper initialization */
        private final Object mLock = new Object();
        /** The {@link Looper} that handles messages for this thread */
        private Looper mLooper;

        MyThread() {
            new Thread(null, this, "CalendarThread").start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
       

        @Override
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Looper.loop();
        }
        void quit() {
            mLooper.quit();
        }
        Looper getLooper() {
            return mLooper;
        }
    }
    
    static void startSyncAgendar(){
    	
    }
    
	public void deleteEventNotify(int eventId) {
		Message mMessage = new Message();
		mMessage.what = MSG_DELETE_ONE_EVENT;
		mMessage.arg1 = eventId;
		mHandler.sendMessage(mMessage);
	}

	public void requestUpdate() {
		Message mMessage = new Message();
		mMessage.what = MSG_UPDATE_REQUEST;
		mHandler.sendMessage(mMessage);
	}
}

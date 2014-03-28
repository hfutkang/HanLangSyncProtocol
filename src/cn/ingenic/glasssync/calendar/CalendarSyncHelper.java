package cn.ingenic.glasssync.calendar;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

public class CalendarSyncHelper {
	private Context mContext;
	public final static String ACCOUNT_NAME="private";
    public final static String DISPLAY_NAME_PREFIX ="priv";
    private final static String TAG ="CalendarSyncHelper";
    public static boolean isSynced = false;
	public CalendarSyncHelper(Context mContext) {
		this.mContext = mContext;
	}
	
	 public Uri addLocalCalendar(Account account,ContentResolver cr) {
	       
	        final ContentValues cv = buildContentValues(account);

	        Uri calUri = buildCalUri();
	        Uri uri=cr.insert(calUri, cv);
	        return uri;
	    }
	 
    private static ContentValues buildContentValues(Account account) {
    	String intName =  account.name;//appendLocalCalendarName(dispName);
        String dispName = appendLocalCalendarName(intName);  //Calendar.getName() returns a String
       
        final ContentValues cv = new ContentValues();
       // cv.put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_NAME, account.name);
       // cv.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        cv.put(Calendars.ACCOUNT_TYPE,account.type);
        cv.put(Calendars.NAME, intName);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, dispName);
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        cv.put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME);
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1);
        return cv;
    }
    
    private static Uri buildCalUri() {
        return CalendarContract.Calendars.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
    }
    
    public static String appendLocalCalendarName(String accountName){
    	return DISPLAY_NAME_PREFIX+"_"+accountName;
    }

	public static void setSyncState(boolean state) {
		isSynced = state;
	}

	public static boolean isSynced() {
		return isSynced;
	}

}

package cn.ingenic.glasssync.vcalendar;

import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class VCalendarBuilder {
	private static final String LOG_TAG = "VCalendarBuilder";

	public static final String VCALENDER_PROPERTY_BEGIN = "VCALENDAR";
	public static final String VCALENDER_PROPERTY_VEVENT = "VEVENT";
	public static final String VCALENDER_PROPERTY_PRODID = "-//Pekall Corporation//Android//EN";
	public static final String VCALENDER_PROPERTY_VERSION = "1.0";
	public static final String PROPERTY_VERSION = "VERSION";
	private static final String VCALENDAR_DATA_VCALENDAR = "VCALENDAR";
	
	private static final String VCALENDER_PARAM_SEPARATOR = ";";
    private static final String VCALENDER_END_OF_LINE = "\r\n";
    private static final String VCALENDER_DATA_SEPARATOR = ":";
    private static final String VCALENDER_ITEM_SEPARATOR = ";";
    private static final String VCALENDER_ITEM_EQUET = "=";
    private static final String VCALENDER_PARAM_ENCODING_QP = "ENCODING=QUOTED-PRINTABLE";
    
    
    private static final String UTF_8 = "UTF-8";
    private final String mCharsetString;
    private final String mVCalendarCharsetParameter;
    private final String mValendarEncoding;
	 
	
    private static final String[] EVENT_PROJECTION = new String[] {
        Events._ID,                  // 0  
        Events.DESCRIPTION,          // 1
        Events.EVENT_LOCATION,       // 2
        Events.TITLE, 				// 3
        
        
        Events.STATUS,            // 4
        Events.DTSTART,              // 5 
        Events.DTEND,              // 6  
        Events.EVENT_TIMEZONE,       // 7
		Events.ALL_DAY, 			// 8
        Events.AVAILABILITY,         //9
        Events.ACCESS_LEVEL,         //10
		Events.RRULE, // 11
		Events.DURATION		//12
    };
    private static final int EVENT_INDEX_EVENT_ID = 0;
    private static final int EVENT_INDEX_DESCRIPPTION = 1;
    private static final int EVENT_INDEX_LOCATION = 2;
    private static final int EVENT_INDEX_SUMMARY = 3;
    private static final int EVENT_INDEX_STATUS = 4;
    private static final int EVENT_INDEX_DTSTART = 5;
    private static final int EVENT_INDEX_DTEND = 6;
    private static final int EVENT_INDEX_TIMEZONE = 7;
	private static final int EVENT_INDEX_ALLDAY = 8;
	private static final int EVENT_INDEX_AVAILABILITY = 9;
	private static final int EVENT_INDEX_ACCESS_LEVEL = 10;
	private static final int EVENT_INDEX_RRULE = 11;
	private static final int EVENT_INDEX_DURATION = 12;
    
    private static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=%d AND (" +
    		Reminders.METHOD + "=" + Reminders.METHOD_ALERT + " OR " + Reminders.METHOD + "=" +
    		Reminders.METHOD_DEFAULT + ")";
    private static final String ATTENDEE_WHERE = Attendees.EVENT_ID + "=%d";
    private static final String ALERTS_WHERE = CalendarAlerts.EVENT_ID + "=%d";
    private static final String ALERTS_SORT = "CalendarAlerts._id";
    
    private static final String REMINDERS_SORT = Reminders.MINUTES;
    private static final String[] REMINDER_PROJECTION = new String[] {
    	Reminders.MINUTES,
    	Reminders.METHOD,
    };
    
    private static final int REMINDERS_MINUTES = 0;
    private static final int REMINDERS_METHOD = 1;
    
    private static final String[] ATTENDEE_PROJECTION = new String[] {
    	Attendees.ATTENDEE_NAME,
    	Attendees.ATTENDEE_EMAIL,
    	Attendees.ATTENDEE_TYPE
    };
    
    private static final String[] ALERT_PROJECTION = new String[] {
    	CalendarAlerts.BEGIN,                   // 0
    	CalendarAlerts.END,                     // 1
    	CalendarAlerts.ALARM_TIME,              // 2
//    	CalendarAlerts.NOTIFY_TIME,				// 3  //4.0 is not has the column
        CalendarAlerts.STATE,                   // 4
        CalendarAlerts.MINUTES                  // 5
    };
    private static final int ALERT_BEGIN = 0;
    private static final int ALERT_END =1;
    private static final int ALARM_TIME =2;
//    private static final int ALERT_NOTIFY_TIME =3;
    private static final int ALERT_STATE =3;
    private static final int ALERT_MINUTES =4;
    
    private static final int ATTENDEE_NAME = 0;
    private static final int ATTENDEE_EMAIL = 1;
	private StringBuilder mBuilder;
	private boolean mEndAppended;

	private String startTime;
	public VCalendarBuilder() {
		super();
		mCharsetString = UTF_8;
		mValendarEncoding = "ENCODING=QUOTED-PRINTABLE";
		mVCalendarCharsetParameter = "CHARSET=" + UTF_8;
		clear();
	}
	
	public void clear() {
        mBuilder = new StringBuilder();
        mEndAppended = false;
        appendLine(VCalendarConstants.PROPERTY_BEGIN, VCALENDER_PROPERTY_BEGIN);
        appendLine(VCalendarConstants.PROPERTY_PRODID, VCALENDER_PROPERTY_PRODID);
        appendLine(VCalendarConstants.PROPERTY_VERSION, VCALENDER_PROPERTY_VERSION);
        appendLine(VCalendarConstants.PROPERTY_BEGIN_VEVENT, VCALENDER_PROPERTY_VEVENT);
    }
	/*public VCalendarBuilder appendEvent(ContentValues contentValues){
		final String eventTitle = contentValues.getAsString(Events.TITLE);
		return this;
	}*/
	public VCalendarBuilder appendEvent(long mEventId,ContentResolver cr){
		
		Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
		Cursor mEventCursor = cr.query(uri, EVENT_PROJECTION, null, null, null);
		mEventCursor.moveToFirst();
		
		long event_id = mEventCursor.getLong(EVENT_INDEX_EVENT_ID);
		if(event_id<0)  return null;
		Log.i("VCalendarBuilder", "append event _id  =" + event_id);
		mBuilder.append(VCalendarConstants.PROPERTY_EVENT_ID);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(event_id);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		String discription = mEventCursor.getString(EVENT_INDEX_DESCRIPPTION);
		if(discription != null && !"".equals(discription)){
			Log.i("VCalendarBuilder", "in if discription = " + discription);
			String encodeDiscription ; 
			mBuilder.append(VCalendarConstants.PROPERTY_DESCRIPTION);
			appendComment();
			encodeDiscription = encodeQuotedPrintable(discription.trim());
			mBuilder.append(encodeDiscription);
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
			
		String location = mEventCursor.getString(EVENT_INDEX_LOCATION);
		if(location != null &&  !"".equals(location)){
			mBuilder.append(VCalendarConstants.PROPERTY_LOCATION);
			appendComment();
			mBuilder.append(encodeQuotedPrintable(location.trim()));
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
		String summary = mEventCursor.getString(EVENT_INDEX_SUMMARY);
		if(summary != null && !"".equals(summary)){
			mBuilder.append(VCalendarConstants.PROPERTY_SUMMARY);
			appendComment();
			mBuilder.append(encodeQuotedPrintable(summary.trim()));
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
		
		int status = mEventCursor.getInt(EVENT_INDEX_STATUS);
		mBuilder.append(VCalendarConstants.PROPERTY_STATUS);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(status);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		Time time = new Time();
		String tempStartValue = mEventCursor.getString(EVENT_INDEX_DTSTART );
		Long dtStart = Long.valueOf(tempStartValue);
		time.clear(Time.TIMEZONE_UTC);
		time.set(dtStart.longValue());
		String StringTimeStart = time.toString();
		startTime = StringTimeStart.substring(0, 15);
		mBuilder.append(VCalendarConstants.PROPERTY_DTSTART);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(startTime);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		
		String tempEndValue = mEventCursor.getString(EVENT_INDEX_DTEND );
		
		String stringTimeEnd = null;
		if (tempEndValue == null || tempEndValue.equals("")) {
			stringTimeEnd = "";
		} else {
			Long dtEnd = Long.valueOf(tempEndValue);
			time.clear(Time.TIMEZONE_UTC);
			time.set(dtEnd.longValue());
			stringTimeEnd = time.toString().substring(0, 15);
		}
		mBuilder.append(VCalendarConstants.PROPERTY_DTEND);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(stringTimeEnd);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		String timeZone = mEventCursor.getString(EVENT_INDEX_TIMEZONE);
		mBuilder.append(VCalendarConstants.PROPERTY_TIMEZONE);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(timeZone);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		int allDay = mEventCursor.getInt(EVENT_INDEX_ALLDAY);
		mBuilder.append(VCalendarConstants.PROPERTY_ALLDAY);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(allDay);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		int availability = mEventCursor.getInt(EVENT_INDEX_AVAILABILITY);
		mBuilder.append(VCalendarConstants.PROPERTY_AVAILABILITY);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(availability);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		int accessLevel = mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL);
		mBuilder.append(VCalendarConstants.PROPERTY_ACCESSLEVEL);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(accessLevel);
		mBuilder.append(VCALENDER_END_OF_LINE);

		String rrule = mEventCursor.getString(EVENT_INDEX_RRULE);
		String escapeRrule = escapeCharacters(rrule);
		mBuilder.append(VCalendarConstants.PROPERTY_RRULE);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(escapeRrule);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		String duration = mEventCursor.getString(EVENT_INDEX_DURATION);
		mBuilder.append(VCalendarConstants.PROPERTY_DURATION);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
		mBuilder.append(duration);
		mBuilder.append(VCALENDER_END_OF_LINE);
		
		mEventCursor.close();
		
		return this;
	}
	
	public VCalendarBuilder	appendAttendee(long mEventId,ContentResolver cr){
		Log.i("VCalendarBuilder", "in appendAttendee");
		Uri uri = Attendees.CONTENT_URI;
		String where = String.format(ATTENDEE_WHERE, mEventId);
		Cursor mAttendeeCursor = cr.query(uri, ATTENDEE_PROJECTION, where, null, Attendees.ATTENDEE_TYPE);
		while(mAttendeeCursor.moveToNext()){
			mBuilder.append(VCalendarConstants.PROPERTY_ATTENDEES);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			String attendeeEmail = mAttendeeCursor.getString(ATTENDEE_EMAIL);
			mBuilder.append(attendeeEmail);
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
		mAttendeeCursor.close();
		return this;
	}
	public VCalendarBuilder	appendReminders(long mEventId,ContentResolver cr){
		Time time = new Time();
		Uri uri = Reminders.CONTENT_URI;
		String where = String.format(REMINDERS_WHERE, mEventId);
		Cursor mReminderCursor = cr.query(uri, REMINDER_PROJECTION, where, null, REMINDERS_SORT);
		while(mReminderCursor.moveToNext()){
			int minutes = mReminderCursor.getInt(REMINDERS_MINUTES);
			int method = mReminderCursor.getInt(REMINDERS_METHOD);
			Log.i("VCalendarBuilder", "minutes = " + minutes+" method:"+method);
			Long alartMinutes = alartMinutes(minutes);
			time.set(alartMinutes);
			String alarmMinutesString = time.toString();
			mBuilder.append(VCalendarConstants.PROPERTY_AALARM);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(alarmMinutesString.substring(0, 15));
			mBuilder.append(VCALENDER_END_OF_LINE);
			
			mBuilder.append(VCalendarConstants.PROPERTY_REMINDER_METHOD);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(method);
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
		
		mReminderCursor.close();
		return this;
	}
	
	public VCalendarBuilder appendAlerts(long mEventId,ContentResolver cr){  //ccxu add
		Log.d(LOG_TAG,"in appendAlerts");
		Uri uri = CalendarAlerts.CONTENT_URI;
		String where = String.format(ALERTS_WHERE, mEventId);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		Cursor alertsCursor = cr.query(uri, ALERT_PROJECTION, where, null, ALERTS_SORT);
		while(alertsCursor.moveToNext()){
			Time time = new Time();
			String tempBeginValue = alertsCursor.getString(ALERT_BEGIN);
			String stringTimeBegin = null;
			if (tempBeginValue == null || tempBeginValue.equals("")) {
				stringTimeBegin = "";
			} else {
				Long begin = Long.valueOf(tempBeginValue);
				time.clear(Time.TIMEZONE_UTC);
				time.set(begin.longValue());
				stringTimeBegin = time.toString();
				stringTimeBegin = stringTimeBegin.substring(0, 15);
			}
			mBuilder.append(VCalendarConstants.PROPERTY_ALERT_BEGIN);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(stringTimeBegin);
			mBuilder.append(VCALENDER_END_OF_LINE);
			Log.d(LOG_TAG,"begin:"+stringTimeBegin);
			
			String tempEndValue = alertsCursor.getString(ALERT_END);
			String stringTimeEnd = null;
			if (tempEndValue == null || tempEndValue.equals("")) {
				stringTimeEnd = "";
			} else {
				Long end = Long.valueOf(tempEndValue);
				time.clear(Time.TIMEZONE_UTC);
				time.set(end.longValue());
				stringTimeEnd = time.toString().substring(0, 15);
			}
			mBuilder.append(VCalendarConstants.PROPERTY_ALERT_END);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(stringTimeEnd);
			mBuilder.append(VCALENDER_END_OF_LINE);
			
			String tempAlarmValue = alertsCursor.getString(ALARM_TIME);
			String stringAlarmTime = null;
			if (tempAlarmValue == null || tempAlarmValue.equals("")) {
				stringAlarmTime = "";
			} else {
				Long alarm = Long.valueOf(tempAlarmValue);
				time.clear(Time.TIMEZONE_UTC);
				time.set(alarm.longValue());
				stringAlarmTime = time.toString().substring(0, 15);
			}
			mBuilder.append(VCalendarConstants.PROPERTY_ALARM_TIME);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(stringAlarmTime);
			mBuilder.append(VCALENDER_END_OF_LINE);
			
//			String tempNotifyValue = alertsCursor.getString(ALERT_NOTIFY_TIME);
//			String stringNotifyTime = null;
//			if (tempNotifyValue == null || tempNotifyValue.equals("")) {
//				stringNotifyTime = "";
//			} else {
//				Long alarm = Long.valueOf(tempNotifyValue);
//				time.clear(Time.TIMEZONE_UTC);
//				time.set(alarm.longValue());
//				stringNotifyTime = time.toString().substring(0, 15);
//			}
//			mBuilder.append(VCalendarConstants.PROPERTY_NOTIFY_TIME);
//			mBuilder.append(VCALENDER_DATA_SEPARATOR);
//			mBuilder.append(stringNotifyTime);
//			mBuilder.append(VCALENDER_END_OF_LINE);
			
			mBuilder.append(VCalendarConstants.PROPERTY_STATE);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(alertsCursor.getString(ALERT_STATE));
			mBuilder.append(VCALENDER_END_OF_LINE);
			
			mBuilder.append(VCalendarConstants.PROPERTY_MINUTES);
			mBuilder.append(VCALENDER_DATA_SEPARATOR);
			mBuilder.append(alertsCursor.getString(ALERT_MINUTES));
			mBuilder.append(VCALENDER_END_OF_LINE);
		}
		alertsCursor.close();
		return this;
	}
	
	public Long alartMinutes(int minutes){
		Time timeStart = new Time();
		Long alarmMinutes;
		timeStart.parse(startTime);
		long millisStart = timeStart.toMillis(true);
		return millisStart - minutes*60*1000 ;
	}
	
	/**
     * Appends one line with a given property name and value.  
     */
    public void appendLine(final String propertyName, final String rawValue) {
        appendLine(propertyName, rawValue, false, false);
    }

    public void appendLine(final String propertyName, final List<String> rawValueList) {
        appendLine(propertyName, rawValueList, false, false);
    }

    public void appendLine(final String propertyName,
            final String rawValue, final boolean needCharset,
            boolean reallyUseQuotedPrintable) {
        appendLine(propertyName, null, rawValue, needCharset, reallyUseQuotedPrintable);
    }

    public void appendLine(final String propertyName, final List<String> parameterList,
            final String rawValue) {
        appendLine(propertyName, parameterList, rawValue, false, false);
    }

    public void appendLine(final String propertyName, final List<String> parameterList,
            final String rawValue, final boolean needCharset,
            boolean reallyUseQuotedPrintable) {
        mBuilder.append(propertyName);
        if (parameterList != null && parameterList.size() > 0) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            //appendTypeParameters(parameterList);
        }
        if (needCharset) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            mBuilder.append(mVCalendarCharsetParameter);
        }

        final String encodedValue;
        if (reallyUseQuotedPrintable) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            mBuilder.append(VCALENDER_PARAM_ENCODING_QP);
            encodedValue = encodeQuotedPrintable(rawValue);
        } else {
            // TODO: one line may be too huge, which may be invalid in vCard spec, though
            //       several (even well-known) applications do not care this.
            encodedValue = escapeCharacters(rawValue);
        }

        mBuilder.append(VCALENDER_DATA_SEPARATOR);
        mBuilder.append(encodedValue);
        mBuilder.append(VCALENDER_END_OF_LINE);
    }
    
    
    public void appendLine(final String propertyName, final List<String> rawValueList,
            final boolean needCharset, boolean needQuotedPrintable) {
        appendLine(propertyName, null, rawValueList, needCharset, needQuotedPrintable);
    }

    public void appendLine(final String propertyName, final List<String> parameterList,
            final List<String> rawValueList, final boolean needCharset,
            final boolean needQuotedPrintable) {
        mBuilder.append(propertyName);
        if (parameterList != null && parameterList.size() > 0) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            //appendTypeParameters(parameterList);
        }
        if (needCharset) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            mBuilder.append(mVCalendarCharsetParameter);
        }
        if (needQuotedPrintable) {
            mBuilder.append(VCALENDER_PARAM_SEPARATOR);
            mBuilder.append(VCALENDER_PARAM_ENCODING_QP);
        }

        mBuilder.append(VCALENDER_DATA_SEPARATOR);
        boolean first = true;
        for (String rawValue : rawValueList) {
            final String encodedValue;
            if (needQuotedPrintable) {
                encodedValue = encodeQuotedPrintable(rawValue);
            } else {
                // TODO: one line may be too huge, which may be invalid in vCard 3.0
                //        (which says "When generating a content line, lines longer than
                //        75 characters SHOULD be folded"), though several
                //        (even well-known) applications do not care this.
                encodedValue = escapeCharacters(rawValue);
            }

            if (first) {
                first = false;
            } else {
                mBuilder.append(VCALENDER_ITEM_SEPARATOR);
            }
            mBuilder.append(encodedValue);
        }
        mBuilder.append(VCALENDER_END_OF_LINE);
    }
    private String encodeQuotedPrintable(final String str) {
    	//ccxu modify for doshare unreadable code
    	return str.trim();
//        if (TextUtils.isEmpty(str)) {
//            return "";
//        }
//
//        final StringBuilder builder = new StringBuilder();
//        int index = 0;
//        int lineCount = 0;
//        byte[] strArray = null;
//
//        try {
//            strArray = str.getBytes(mCharsetString);
//        } catch (UnsupportedEncodingException e) {
//            Log.e(LOG_TAG, "Charset " + mCharsetString + " cannot be used. "
//                    + "Try default charset");
//            strArray = str.getBytes();
//        }
//        while (index < strArray.length) {
//            builder.append(String.format("=%02X", strArray[index]));
//            index += 1;
//            lineCount += 3;
//
//            if (lineCount >= 1000) {
//                // Specification requires CRLF must be inserted before the
//                // length of the line
//                // becomes more than 76.
//                // Assuming that the next character is a multi-byte character,
//                // it will become
//                // 6 bytes.
//                // 76 - 6 - 3 = 67
//                builder.append("=\r\n");
//                lineCount = 0;
//            }
//        }
//
//        return builder.toString();
    }
    
    
    @SuppressWarnings("fallthrough")
    private String escapeCharacters(final String unescaped) {
        if (TextUtils.isEmpty(unescaped)) {
            return "";
        }

        final StringBuilder tmpBuilder = new StringBuilder();
        final int length = unescaped.length();
        for (int i = 0; i < length; i++) {
            final char ch = unescaped.charAt(i);
            switch (ch) {
                case ';': {
                    tmpBuilder.append('\\');
                    tmpBuilder.append(';');
                    break;
                }
                case '\r': {
                    if (i + 1 < length) {
                        char nextChar = unescaped.charAt(i);
                        if (nextChar == '\n') {
                            break;
                        } else {
                            // fall through
                        }
                    } else {
                        // fall through
                    }
                }
                case '\n': {
                    // In vCard 2.1, there's no specification about this, while
                    // vCard 3.0 explicitly requires this should be encoded to "\n".
                    tmpBuilder.append("\\n");
                    break;
                }
                case '\\': 
                case '<':
                case '>': {
                       tmpBuilder.append('\\');
                       tmpBuilder.append(ch);
                    break;
                }
                case ',': {
                       tmpBuilder.append(ch);
                    break;
                }
                default: {
                    tmpBuilder.append(ch);
                    break;
                }
            }
        }
        return tmpBuilder.toString();
    }
    public void appendComment(){
    	//ccxu modify for doshare unreadable code
//    	mBuilder.append(VCALENDER_PARAM_SEPARATOR);
//		mBuilder.append(mValendarEncoding);
//		mBuilder.append(VCALENDER_PARAM_SEPARATOR);
//		mBuilder.append(mVCalendarCharsetParameter);
		mBuilder.append(VCALENDER_DATA_SEPARATOR);
    }
    
    @Override
    public String toString() {
        if (!mEndAppended) {
        	appendLine(VCalendarConstants.PROPERTY_END_VEVENT, VCALENDER_PROPERTY_VEVENT);
            appendLine(VCalendarConstants.PROPERTY_END, VCALENDAR_DATA_VCALENDAR);
            mEndAppended = true;
        }
        return mBuilder.toString();
    }

}

package cn.ingenic.glasssync.vcalendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class VCalendarEntry {
	
	private static final String LOG_TAG = "VCalendarEntry";
	private EventData mEventData;
	private ReminderData mReminderData;
	private CalendarAlertsData mAlertsData;
	private boolean hasAlarm = false;
	public boolean hasAttendees = false;
	public boolean hadGlasssync = true;
	public boolean hasAlert = false;
	public EventData getmEventData() {
		return mEventData;
	}
	public void setmEventData(EventData mEventData) {
		this.mEventData = mEventData;
	}
	public class ReminderData{
		public String event_id;
		public String minutes;
		public String method;
		public ReminderData() {
			super();
		}
		public ReminderData(String eventId, String minutes,String method) {
			super();
			event_id = eventId;
			this.minutes = minutes;
			this.method = method;
		}
	}
	
	public class CalendarAlertsData{
		public String event_id;
		public String begin;
		public String end;
		public String alarmTime;
		public String state;
		public String minutes;
//		public String notifyTime;
		public CalendarAlertsData(){
			super();
		}
		public CalendarAlertsData(String event_id,String begin,String end,
				String alarmTime,String state,String minutes/*,String notifyTime*/){
			super();
			this.event_id = event_id;
			this.begin = begin;
			this.end = end;
			this.alarmTime = alarmTime;
			this.state = state;
			this.minutes = minutes;
//			this.notifyTime = notifyTime;
		}
	}
	
	
	 public class EventData{
		public String description ;
		public String eventLocation ;
		public String title ;
		
		public String status ;
		public String dtStart ;
		public String dtEnd ;
		public String aAlarm ;
		public String dAlarm ;
		public String timeZone;
		public String allday;
		public String rrule;
		public String duration;
		public String availability;
		public String accessLevel;
		public String attendeeEmail;
		public boolean hasAlarm = false;
		
		public EventData() {
			super();
		}
		public EventData(String description, String eventLocation,
				String title, String status, String dtStart, String dtEnd,
				String aAlarm,String dAlarm,boolean hasAlarm,String timeZone,String availability,String accessLevel,
				String attendeeEmail) {
			super();
			this.description = description;
			this.eventLocation = eventLocation;
			this.title = title;
			this.status = status;
			this.dtStart = dtStart;
			this.dtEnd = dtEnd;
			this.aAlarm = aAlarm;
			this.dAlarm = dAlarm;
			this.hasAlarm = hasAlarm;
			this.timeZone = timeZone;
			this.availability = availability;
			this.accessLevel = accessLevel;
			this.attendeeEmail = attendeeEmail;
		}
		
	}
	static class Property{
		private String mPropertyName;
		private Map<String, Collection<String>> mParameterMap = 
			new HashMap<String,Collection<String>>();
		private String mPropertyValue ;
		private byte[] mPropertyBytes ;
		
		public void setPropertyName(final String propertyName) {
            mPropertyName = propertyName;
        }
		public void setPropertyValue(final String propertyValue) {
			mPropertyValue = propertyValue;
		}
		
		  public void addParameter(final String paramName, final String paramValue) {
	            Collection<String> values;
	            Log.i(LOG_TAG, "paramName = "+ paramName);
	            Log.i(LOG_TAG, "paramValue = "+ paramValue);
	            if (!mParameterMap.containsKey(paramName)) {
	                if (paramName.equals("TYPE")) {
	                    values = new HashSet<String>();
	                } else {
	                    values = new ArrayList<String>();
	                }
	                mParameterMap.put(paramName, values);
	            } else {
	                values = mParameterMap.get(paramName);
	            }
	            values.add(paramValue);
	        }

	        public void setPropertyBytes(final byte[] propertyBytes) {
	            mPropertyBytes = propertyBytes;
	        }

	        public final Collection<String> getParameters(String type) {
	            return mParameterMap.get(type);
	        }

	        public void clear() {
	            mPropertyName = null;
	            mParameterMap.clear();
	            mPropertyValue = null;
	            mPropertyBytes = null;
	        }
	}
	public void addProperty(Property property) {
		final String propName = property.mPropertyName;
		final String propValue = property.mPropertyValue;
		
		Log.i(LOG_TAG, "in addProperty() propName = " + propName);
		Log.i(LOG_TAG, "propValue = " + propValue);
		
		if(mEventData == null){
			mEventData = new EventData();
		}
		if(mReminderData == null){
			mReminderData = new ReminderData();
		}
		if(mAlertsData == null){  //ccxu
			mAlertsData = new CalendarAlertsData();
		}
		
		if(propName.equals(VCalendarConstants.PROPERTY_PRODID)){
			//Ignore this
		}else if(propName.equals(VCalendarConstants.PROPERTY_VERSION)){
			//VCalendar version. Ignore this
		}else if(propName.equals(VCalendarConstants.PROPERTY_EVENT_ID)){
			mReminderData.event_id = propValue;
			mAlertsData.event_id = propValue;  //ccxu
		}else if(propName.equals(VCalendarConstants.PROPERTY_DESCRIPTION)){
			mEventData.description = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_LOCATION)){
			mEventData.eventLocation = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_SUMMARY)){
			mEventData.title = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_STATUS)){
			mEventData.status = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_DTSTART)){
			mEventData.dtStart = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_DTEND)){
			mEventData.dtEnd = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_TIMEZONE)){
			mEventData.timeZone = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ALLDAY)){
			mEventData.allday = propValue;
		}else if (propName.equals(VCalendarConstants.PROPERTY_RRULE)) {
			mEventData.rrule = propValue;
		}else if (propName.equals(VCalendarConstants.PROPERTY_DURATION)) {
			mEventData.duration = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_AVAILABILITY)){
			mEventData.availability = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ACCESSLEVEL)){
			mEventData.accessLevel = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ATTENDEES)){
			mEventData.attendeeEmail = propValue;
			hasAttendees = true;
		}else if(propName.equals(VCalendarConstants.PROPERTY_AALARM)){
			mEventData.aAlarm = propValue.substring(0, 15);
			mEventData.hasAlarm = true;
			hasAlarm = true;
			mReminderData.minutes = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_REMINDER_METHOD)){
			mReminderData.method = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ALERT_BEGIN)){  //ccxu
			mAlertsData.begin = propValue;
			hasAlert = true;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ALERT_END)){
			mAlertsData.end = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_ALARM_TIME)){
			mAlertsData.alarmTime = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_STATE)){
			mAlertsData.state = propValue;
		}else if(propName.equals(VCalendarConstants.PROPERTY_MINUTES)){
			mAlertsData.minutes = propValue;
		}/*else if(propName.equals(VCalendarConstants.PROPERTY_NOTIFY_TIME)){
			mAlertsData.notifyTime = propValue;
		}*/
	}
	public Long alartMinutes(){
		Time timeStart = new Time();
		timeStart.parse(mEventData.dtStart);
		long millisStart= timeStart.toMillis(true);
		Time timeAlartStart = new Time();
		timeAlartStart.parse(mEventData.aAlarm);
		long millisAlartStart = timeAlartStart.toMillis(true);
		return millisStart - millisAlartStart;
	}
	public Uri pushIntoContentResolver(ContentResolver resolver,long calendarId){
		
		ArrayList<ContentProviderOperation> operationList = 
				new ArrayList<ContentProviderOperation>();
		
		Time time = new Time();
		
		if(calendarId==-1){
			Cursor cursorCalendar = resolver.query(Calendars.CONTENT_URI, 
					new String[]{Calendars._ID}, null, null, null);
			if(cursorCalendar != null && cursorCalendar.moveToFirst()){
				calendarId = cursorCalendar.getLong(0);
			}
			cursorCalendar.close();
		}
		
		if(calendarId != -1){
			ContentProviderOperation.Builder builder = 
				ContentProviderOperation.newInsert(Events.CONTENT_URI);
			builder.withValue(Events.CALENDAR_ID, calendarId);
			
			if(mEventData.description != null && !"".equals(mEventData.description)){
				builder.withValue(Events.DESCRIPTION, mEventData.description);
			}
			
			if(mEventData.eventLocation != null && !"".equals(mEventData.eventLocation)){
				builder.withValue(Events.EVENT_LOCATION, mEventData.eventLocation);
			}
			
			if(mEventData.title != null && !"".equals(mEventData.title)){
				builder.withValue(Events.TITLE, mEventData.title);
			}
			builder.withValue(Events.STATUS, mEventData.status);
			
			time.clear(Time.TIMEZONE_UTC);
			time.parse(mEventData.dtStart);
			long dtStart = time.toMillis(false);
			builder.withValue(Events.DTSTART, dtStart);

			// ADD by hhyuan ,we must insert one of the dtEnd\duration
			// and can not insert both also in CalendarProvider2
			if (!TextUtils.isEmpty(mEventData.dtEnd)) {
				time.clear(Time.TIMEZONE_UTC);
				time.parse(mEventData.dtEnd);
				builder.withValue(Events.DTEND, time.toMillis(false));
			} else if (!TextUtils.isEmpty(mEventData.duration)) {
				builder.withValue(Events.DURATION, mEventData.duration);
			} else {
				builder.withValue(Events.DTEND, dtStart);
			}

			builder.withValue(Events.HAS_ALARM, mEventData.hasAlarm);
			
			
			builder.withValue(Events.EVENT_TIMEZONE, mEventData.timeZone);
			if (mEventData.allday != null && !"".equals(mEventData.allday)) {
				builder.withValue(Events.ALL_DAY,
						Integer.parseInt(mEventData.allday));
			}
			if (mEventData.rrule != null && !"".equals(mEventData.rrule)) {
				builder.withValue(Events.RRULE, mEventData.rrule);
			}
			if(mEventData.availability != null && !"".equals(mEventData.availability)){
				builder.withValue(Events.AVAILABILITY, mEventData.availability);
			}
			if(mEventData.accessLevel != null && !"".equals(mEventData.accessLevel)){
				builder.withValue(Events.ACCESS_LEVEL, mEventData.accessLevel);
			}
			
			operationList.add(builder.build());
		}
		Log.i(LOG_TAG, "operationList = " + operationList.toString());
		try {
			ContentProviderResult[] results = resolver.applyBatch(CalendarContract.AUTHORITY, operationList);
			
			String eventId = "";
			int id = 1;
			Cursor cursorEvent = resolver.query(Events.CONTENT_URI, 
					new String[]{Events._ID}, null, null, null);

			if (cursorEvent != null && cursorEvent.getCount() > 0){
				if (cursorEvent.moveToLast()) {
					eventId = cursorEvent.getString(0);
					id = Integer.parseInt(eventId);
				}
			}
			cursorEvent.close();

			if(hasAlarm){
				AddReminder(resolver,id);
			}
			if(hasAttendees){
				AddAttendees(resolver,id);
			}
			if(hadGlasssync){
			   AddGlasssync(resolver,id);
			}
			if(hasAlert){
				Log.d(LOG_TAG,"start add alerts");
				AddCalendarAlerts(resolver,id);  //ccxu
				Log.d(LOG_TAG,"end add alerts");
				Log.d(LOG_TAG,"the last operationList = " + operationList.toString());
			}
			
			return (results == null || results.length == 0 || results[0] == null)
					? null
					: results[0].uri;
		} catch (RemoteException e) {
			 Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
			 return null;
		} catch (OperationApplicationException e) {
			 Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
	            return null;
		}
	}
	private void AddAttendees(ContentResolver resolver,int id){
		ArrayList<ContentProviderOperation> operationList = 
			new ArrayList<ContentProviderOperation>();
//		String eventId = "";
//		Cursor cursorEvent = resolver.query(Events.CONTENT_URI, 
//				new String[] { Calendars._ID }, null, null, null);
//		if (cursorEvent == null)
//			return;
//
//		if(cursorEvent != null && cursorEvent.moveToLast()){
//			eventId = cursorEvent.getString(0);
//		}
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Attendees.CONTENT_URI);
		builder.withValue(Attendees.EVENT_ID, id);
		builder.withValue(Attendees.ATTENDEE_EMAIL, mEventData.attendeeEmail);
		operationList.add(builder.build());
		try {
			ContentProviderResult[] results = resolver.applyBatch(CalendarContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
        }catch(android.database.sqlite.SQLiteException e){
            e.printStackTrace();//fix bug2014031811872
		}finally {
//			if (cursorEvent != null) {
//				cursorEvent.close();
//
//			}
		}
	}
	private void AddReminder(ContentResolver resolver,int id) {
		ArrayList<ContentProviderOperation> operationList = 
			new ArrayList<ContentProviderOperation>();
		
//		String eventId = "";
//		
//		Cursor cursorEvent = resolver.query(Events.CONTENT_URI, 
//				new String[]{Events._ID}, null, null, null);
//
//		if (cursorEvent == null || cursorEvent.getCount() == 0)
//			return;
//
//		if (cursorEvent.moveToLast()) {
//			eventId = cursorEvent.getString(0);
//		}
//
//		if (TextUtils.isEmpty(eventId))
//			return;

		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Reminders.CONTENT_URI);
		builder.withValue(Reminders.EVENT_ID, id);

		Long alartMinutes = alartMinutes();
		builder.withValue(Reminders.MINUTES, alartMinutes/(60*1000));
		builder.withValue(Reminders.METHOD,mReminderData.method);
		operationList.add(builder.build());
		try {
			ContentProviderResult[] results = resolver.applyBatch(CalendarContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
        }catch(android.database.sqlite.SQLiteException e){
            e.printStackTrace();//fix bug2014031811872
		}finally {
//			if (cursorEvent != null) {
//				cursorEvent.close();
//
//			}
		}
	}
	
	// add for indroid sync
	private void AddGlasssync(ContentResolver resolver,int id) {
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

//		long eventId = -1;
//
//		Cursor cursorEvent = resolver.query(Events.CONTENT_URI,
//				new String[] { Events._ID }, null, null, null);
//		if (cursorEvent != null && cursorEvent.moveToLast()) {
//			eventId = cursorEvent.getLong(0);
//		}
		Uri uri = Uri.parse("content://" + CalendarContract.AUTHORITY
				+ "/indroid_sync");
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(uri);
		builder.withValue("event_id", id);
		builder.withValue("indroid_sync_id", mReminderData.event_id);
		operationList.add(builder.build());
		try {
			ContentProviderResult[] results = resolver.applyBatch(
					CalendarContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		} finally {
//			if (cursorEvent != null) {
//				cursorEvent.close();
//
//			}
		}
	}
	//add for calendar alerts
	private void AddCalendarAlerts(ContentResolver resolver,int id){  //ccxu
		ArrayList<ContentProviderOperation> operationList = 
			new ArrayList<ContentProviderOperation>();
		
//		String eventId = "";
//		
//		Cursor cursorEvent = resolver.query(Events.CONTENT_URI, 
//				new String[]{Events._ID}, null, null, null);
//
//		if (cursorEvent == null || cursorEvent.getCount() == 0)
//			return;
//
//		if (cursorEvent.moveToLast()) {
//			eventId = cursorEvent.getString(0);
//		}
//
//		if (TextUtils.isEmpty(eventId))
//			return;
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(CalendarAlerts.CONTENT_URI);
		builder.withValue(CalendarAlerts.EVENT_ID, id);
		Time time = new Time();
		if(!TextUtils.isEmpty(mAlertsData.begin)){
			time.clear(Time.TIMEZONE_UTC);
			time.parse(mAlertsData.begin);
			builder.withValue(CalendarAlerts.BEGIN, time.toMillis(false));
		}
		if(!TextUtils.isEmpty(mAlertsData.end)){
			time.clear(Time.TIMEZONE_UTC);
			time.parse(mAlertsData.end);
			builder.withValue(CalendarAlerts.END, time.toMillis(false));
		}
		if(!TextUtils.isEmpty(mAlertsData.alarmTime)){
			time.clear(Time.TIMEZONE_UTC);
			time.parse(mAlertsData.alarmTime);
			builder.withValue(CalendarAlerts.ALARM_TIME, time.toMillis(false));
		}
//		if(!TextUtils.isEmpty(mAlertsData.notifyTime)){
//			time.clear(Time.TIMEZONE_UTC);
//			time.parse(mAlertsData.notifyTime);
//			builder.withValue(CalendarAlerts.NOTIFY_TIME, time.toMillis(false));
//		}
		builder.withValue(CalendarAlerts.STATE, mAlertsData.state);
		builder.withValue(CalendarAlerts.MINUTES, mAlertsData.minutes);
		operationList.add(builder.build());
		try {
			ContentProviderResult[] results = resolver.applyBatch(CalendarContract.AUTHORITY, operationList);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}finally {
//			if (cursorEvent != null) {
//				cursorEvent.close();
//			}
		}
	}
}

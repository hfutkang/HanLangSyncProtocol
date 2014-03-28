package cn.ingenic.glasssync.calendar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.vcalendar.VCalendarEntryConstructor;
import cn.ingenic.glasssync.vcalendar.VCalendarException;
import cn.ingenic.glasssync.vcalendar.VCalendarParser;
import cn.ingenic.glasssync.data.Projo;

/** @author hhyuan<br>
 * watch receive the data , and make some action depended on the data received 
 * */
public class CalendarTransaction extends Transaction {
	private final static String TAG = "CalendarTransaction";
	private static MyHandler mHandler = null;
	private static HandlerThread thread = null;
	public final static int MSG_UPDATE_ONE_EVENT = 0;
	public final static int MSG_UPDATE_ALL = 1;
	public final static int MSG_INSERT_ONE_EVENT = 2;
	public final static int MSG_INSERT_ONE_ACCOUNT = 3;
	public final static int MSG_INSERT_ALL = 4;
	public final static int MSG_DELETE_ONE_EVENT = 5;
	public final static int MSG_DELETE_ONE_ACCOUNT = 6;
	public final static int MSG_DELETE_ALL = 7;
	public final static int MSG_ALERT_NOTIFICATION = 8;

	private static final String CHARSET = "UTF-8";

	CalendarSyncHelper syncHelper;

	@Override
	public void onStart(ArrayList<Projo> datas) {
		super.onStart(datas);

		if (datas == null || datas.size() == 0) {
			return;
		}
		mHandler = ensureHandlerExists();
		syncHelper = new CalendarSyncHelper(mContext);

		String tag = (String) datas.get(0).get(CalendarColumn.tag);
		Log.i(TAG,"transaction start receiver tag:"+tag);
		Message msg = new Message();
		Bundle bundle = new Bundle();
		if ((CalendarController.UPDATE_AGENDA_TO_WATCH_TAG).equals(tag)) {
			msg.what = MSG_UPDATE_ALL;
		} else if ((CalendarController.WATCH_DELETE_AGENDA_TAG).equals(tag)) {
			msg.what = MSG_DELETE_ONE_EVENT;
		} else if ((CalendarController.DELETE_ALL_TO_WATCH_TAG).equals(tag)) {
			msg.what = MSG_DELETE_ALL;
		}

		ArrayList list = new ArrayList();
		list.add(datas);
		bundle.putParcelableArrayList("list", list);
		msg.setData(bundle);
		mHandler.sendMessage(msg);

	}

	private synchronized MyHandler ensureHandlerExists() {
		if (thread == null || !thread.isAlive()) {
			thread = new HandlerThread("datas_thread");
			thread.start();
		}
		if (mHandler == null) {
			mHandler = new MyHandler(thread.getLooper());
		}
		return mHandler;
	}

	private class MyHandler extends Handler {

		public MyHandler(Looper looper) {

			super(looper);

		}

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "handleMessage msg:" + msg.what);
			Bundle bundle = msg.getData();
			ArrayList<Projo> projos = (ArrayList<Projo>) bundle
					.getParcelableArrayList("list").get(0);
			switch (msg.what) {
			case MSG_UPDATE_ALL:
				startUpdate(projos);
				break;
			case MSG_INSERT_ALL:
				startInsert(projos);
				break;
			case MSG_DELETE_ALL:
				startDeleteAll();
				break;
			case MSG_DELETE_ONE_EVENT:
				startDelete(projos);
				break;
			case MSG_ALERT_NOTIFICATION:
				startAlertNotification(projos);
				break;
			}

			super.handleMessage(msg);
		}

	}

	private void startUpdate(ArrayList<Projo> datas) {
		if (datas == null || datas.size() == 0)
			return;
		ContentResolver mResolver = mContext.getContentResolver();
		new AsyncUpdateTask(mResolver).execute(datas);
	}

	private void startInsert(ArrayList<Projo> datas) {
		// TO DO
	}

	private void startDeleteAll() {

		ContentResolver mResolver = mContext.getContentResolver();
		String selection = Calendars.SYNC_EVENTS + "=1";
		Cursor calendarCur = mResolver
				.query(CalendarContract.Calendars.CONTENT_URI,
						CalendarController.PROJECTION,
						CalendarController.SELECTION, new String[] { String
								.valueOf(CalendarController.ACCOUNT_SYNC) },
						CalendarController.SORT_ORDER);
		if (calendarCur != null && calendarCur.getCount() > 0) {
			calendarCur.moveToFirst();
			do {
				long id = calendarCur.getLong(0);
				Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, id);
				ContentValues values = new ContentValues();
				values.put(Calendars.VISIBLE, 0);
				values.put(Calendars.SYNC_EVENTS, 0);
				mResolver.update(uri, values, null, null);
			} while (calendarCur.moveToNext());
		}

		if (calendarCur != null) {
			calendarCur.close();
		}
	}

	private void startDelete(ArrayList<Projo> datas) {
		if (datas == null)
			return;
		ContentResolver mResolver = mContext.getContentResolver();
		for (Projo projo : datas) {
			int event_id = (Integer) projo.get(CalendarColumn.event_id);
			Log.i(TAG, "delete event_id:" + event_id);
			if (event_id != -1) {
				// if(CalendarSyncHelper.isSynced()){
				Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI,
						event_id);
				mResolver.delete(uri, null, null);
				// }
			}
		}
	}

	private void startAlertNotification(ArrayList<Projo> datas) {
		// TO DO
	}

	private void insertEventList(ContentResolver mResolver, long calendarId,
			ArrayList<String> eventsList) {
		Iterator<String> iterator = eventsList.iterator();
		while (iterator.hasNext()) {
			String event = (String) iterator.next();
			doReadOneVCalendar(mResolver, calendarId, event);
		}
	}

	private void doReadOneVCalendar(ContentResolver mResolver, long calendarId,
			String event) {
		VCalendarParser mVCalendarParser;
		VCalendarEntryConstructor mBuilder;

		mVCalendarParser = new VCalendarParser();
		mBuilder = new VCalendarEntryConstructor(CHARSET, CHARSET, mResolver,
				false, calendarId);
		try {
			mVCalendarParser.parse(new ByteArrayInputStream(event.getBytes()),
					CHARSET, mBuilder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (VCalendarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class AsyncUpdateTask extends AsyncTask<Object, Object, Object> {
		ContentResolver mResolver;
		ArrayList<Projo> datas;

		public AsyncUpdateTask(ContentResolver mResolver) {
			super();
			this.mResolver = mResolver;
		}

		@Override
		protected Object doInBackground(Object... params) {
			datas = (ArrayList<Projo>) params[0];

			// delete all before update
			int count = mResolver.delete(Calendars.CONTENT_URI, null, null);
			return count;
		}

		@Override
		protected void onPostExecute(Object result) {

			for (Projo projo : datas) {
				ArrayList<String> eventsList = (ArrayList<String>) projo
						.get(CalendarColumn.events);
				String accountName = (String) projo
						.get(CalendarColumn.accountname);
				String accountType = (String) projo
						.get(CalendarColumn.accounttype);
				Account account = new Account(accountName, accountType);
				if (eventsList == null || eventsList.size() == 0) {
					continue;
				}

				Uri uri = syncHelper.addLocalCalendar(account, mResolver);
				Log.i(TAG, "inset uri:" + uri.toString());
				if (uri != null) {
					String path = uri.getPath();
					String calendarIdStr = path
							.substring(path.lastIndexOf("/") + 1);
					insertEventList(mResolver, Long.valueOf(calendarIdStr),
							eventsList);
				}
			}

			// set the state that it was synced.
			CalendarSyncHelper.setSyncState(true);
			// int dataSize = datas.size();
			// for (int i=0;i<dataSize;i++) {
			// Projo projo = datas.get(i);
			// ArrayList<String> eventsList = (ArrayList<String>)projo
			// .get(CalendarColumn.events);
			// String accountName = (String)
			// projo.get(CalendarColumn.accountname);
			// String accountType = (String)
			// projo.get(CalendarColumn.accounttype);
			// Account account = new Account(accountName,accountType);
			// if (eventsList == null || eventsList.size() == 0) {
			// return;
			// }
			// Log.i(TAG,"receive data------name:"+accountName+" type:"+accountType);
			// String queryCalendarSeletion = Calendars.ACCOUNT_NAME
			// + " = '" + accountName + "' AND " + Calendars.ACCOUNT_TYPE
			// + " = '" + accountType + "' " /*+ " OR ("
			// + Calendars.NAME + " = '"
			// + CalendarSyncHelper.appendLocalCalendarName(accountName)
			// + "' AND " + Calendars.ACCOUNT_TYPE + " = '"
			// + CalendarContract.ACCOUNT_TYPE_LOCAL + "')"*/;
			//
			// Cursor calendarCur = mResolver.query(Calendars.CONTENT_URI,
			// CALENDAR_QUERY_PROJECTION, queryCalendarSeletion, null,
			// null, null);
			// if (calendarCur != null&&calendarCur.getCount()>0) {
			// calendarCur.moveToFirst();
			// long calendarId = calendarCur.getLong(CALENDAR_ID_INDEX);
			// Log.i(TAG,"delete calendar :"+calendarId);
			// int syncTag = calendarCur
			// .getInt(CALENDAR_SYNC_EVENTS_INDEX);
			// // if (syncTag == SYNC_TAG) {
			// String where = Events.CALENDAR_ID + "=" + calendarId;
			// // once we sync from other device , delete all events on
			// // the local device and insert the new events that is from
			// // the sync
			// // device.
			// mResolver.delete(Events.CONTENT_URI, where, null);
			//
			// insertEventList(mResolver, calendarId, eventsList);
			//
			// // }else{
			// // if the account is not sync ,we should notify the local
			// // device
			// // }
			// } else {
			// // if the account is not exist , we should create a new
			// // account
			// Uri uri = syncHelper.addLocalCalendar(account, mResolver);
			// if (uri != null) {
			// String path = uri.getPath();
			// String calendarIdStr = path.substring(path
			// .lastIndexOf("/") + 1);
			// insertEventList(mResolver,
			// Long.valueOf(calendarIdStr), eventsList);
			// }
			//
			// }
			// }
			super.onPostExecute(result);
		}

	}

}

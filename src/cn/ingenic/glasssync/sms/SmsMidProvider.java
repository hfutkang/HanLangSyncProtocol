package cn.ingenic.glasssync.sms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.CustomDatabaseHelperCallback;
import cn.ingenic.glasssync.services.mid.MidDestContentProvider;
// import cn.ingenic.glasssync.sms.Sms; 
// import cn.ingenic.glasssync.sms.SmsEntry;
import cn.ingenic.glasssync.sms.SmsModule;
//import cn.ingenic.glasssync.sms.ThreadEntry;

public class SmsMidProvider extends MidDestContentProvider {

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final int THREAD_BASE = 0;
	private static final int THREAD_CHECK = 1;
	private static final int THREAD_DRAFT = 2;
	private static final int THREAD_DELETE_ALL=3;

	static {
		URI_MATCHER.addURI("sms-dest", "thread", THREAD_BASE);
		URI_MATCHER.addURI("sms-dest", "thread/check", THREAD_CHECK);
		URI_MATCHER.addURI("sms-dest", "thread/draft", THREAD_DRAFT);
		URI_MATCHER.addURI("sms-dest", "thread/deleteall", THREAD_DELETE_ALL);
	}

	@Override
	public SyncModule getSyncModule() {
		return SmsModule.getInstance(getContext().getApplicationContext());
	}

	@Override
	protected CustomDatabaseHelperCallback getCustomDatabaseHelperCallback() {
		return new SmsDatabaseHelper();
	}

	@Override
	public boolean onCreate() {
		return super.onCreate();
	}

	@Override
	protected int deleteWithCustomUri(Uri uri, String selection,
			String[] selectionArgs) {
		String table = null;
		switch (URI_MATCHER.match(uri)) {
		case THREAD_BASE:
			table = Sms.THREAD_NAME;
			break;
		case THREAD_CHECK:
			// if(selection==null||selection.equals("")){
			// 	checkDeleteThreadData();
			// }else{
			// 	checkDeleteThreadDate(selection);
			// }
			return 0x00;
		case THREAD_DELETE_ALL:
			table = Sms.THREAD_NAME;
			int id = this.getWritableDatabase().delete(table, selection,
					selectionArgs);
			notifyThreadChanged();
			return id;
		}
		int id = this.getWritableDatabase().delete(table, selection,
				selectionArgs);
		// if(!isApplyBatch()){
		// notifyThreadChanged();
		// }
		return id;

	}
	
//	private String getDraftTable(){
//		return "("+Sms.THREAD_NAME+" inner join "+Sms.SMS_NAME+" on "
//	            +"("+Sms.THREAD_NAME+"."+Sms.ThreadColumns.PHONE_THREAD_ID+" = "+Sms.SMS_NAME+"."+Sms.SmsColumns.PHONE_THREAD_ID
//				+" AND "+Sms.SMS_NAME+"."+Sms.SmsColumns.TYPE+" = "+Sms.MESSAGE_TYPE_DRAFT+"))";
//	}
	
	// private void checkDeleteThreadData(){
	// 	Cursor allSmsCursor = query(SmsEntry.SmsUri.SMS_URI,SmsEntry.PROJECTION, null, null,
	// 			null);
	// 	if (Sms.DEBUG)
	// 		Log.i(Sms.TAG,
	// 				"checkThread data sms cursor count is :"
	// 						+ allSmsCursor.getCount());
	// 	if (allSmsCursor.getCount() == 0) {
	// 		allSmsCursor.close();
	// 		this.getWritableDatabase().delete(Sms.THREAD_NAME, null, null);
	// 		return;
	// 	}
	// 	allSmsCursor.moveToFirst();
		
	// 	Map<Long,SmsEntry> smsEntryMap=new HashMap<Long,SmsEntry>();
	// 	String ALL_SMS_PHONE_THREAD_ID = null;
	// 	do {
	// 		SmsEntry se=new SmsEntry(allSmsCursor);
	// 		smsEntryMap.put(se.getMidKey(), se);

	// 		if (ALL_SMS_PHONE_THREAD_ID == null) {
	// 				ALL_SMS_PHONE_THREAD_ID = String.valueOf(se.getPhoneThreadId());
	// 		} else {
	// 				ALL_SMS_PHONE_THREAD_ID = ALL_SMS_PHONE_THREAD_ID + ","
	// 						+ se.getPhoneThreadId();
	// 		}
	// 	} while (allSmsCursor.moveToNext());
	// 	allSmsCursor.close();
	// 	if (Sms.DEBUG)
	// 		Log.i(Sms.TAG, "checkThread data delete selectionArgs is :"
	// 				+ ALL_SMS_PHONE_THREAD_ID);
	// 	this.getWritableDatabase().delete(
	// 			Sms.THREAD_NAME,
	// 			Sms.ThreadColumns.PHONE_THREAD_ID + " NOT IN ("
	// 					+ ALL_SMS_PHONE_THREAD_ID + ")", null);
	// }

	// private void checkDeleteThreadDate(String selection) {
	// 	Cursor allSmsCursor = query(SmsEntry.SmsUri.SMS_URI,SmsEntry.PROJECTION, null, null,
	// 			null);
	// 	if (allSmsCursor.getCount() == 0) {
	// 		allSmsCursor.close();
	// 		return;
	// 	}
	// 	allSmsCursor.moveToFirst();
	// 	Map<String,SmsEntry> smsEntryMap=new HashMap<String,SmsEntry>();
	// 	do {
	// 		SmsEntry se=new SmsEntry(allSmsCursor);
	// 		smsEntryMap.put(String.valueOf(se.getMidKey()), se);
	// 	} while (allSmsCursor.moveToNext());	
	// 	allSmsCursor.close();
		
	// 	String[] midKeyArray=selection.split(",");
	// 	ArrayList<Long> haveUpdateThreadList=new ArrayList<Long>();
	// 	for(String mid_key:midKeyArray){
	// 		SmsEntry se=smsEntryMap.get(mid_key);
	// 		if(se==null){
	// 			Log.e("yangliu","sms table no this delete mid key!");
	// 			continue;
	// 		}
	// 		long phoneThreadId=se.getPhoneThreadId();
	// 		if(haveUpdateThreadList.contains(phoneThreadId))continue;
			
	// 		Cursor cursor=query(SmsEntry.SmsUri.SMS_URI,new String[]{"MAX("+Sms.SmsColumns.DATE+")"},
	// 				Sms.SmsColumns.PHONE_THREAD_ID+"="+phoneThreadId+" AND "
	// 				+Sms.MID_KEY_COLUMN+" NOT IN("+selection+")",null,null);
	// 		if(cursor.getCount()==0){
	// 			cursor.close();
	// 			continue;
	// 		}
	// 		cursor.moveToFirst();
	// 		long date=cursor.getLong(0);
	// 		cursor.close();
	// 		ContentValues cv=new ContentValues();
	// 		cv.put(Sms.ThreadColumns.DATE, date);
	// 		update(ThreadEntry.ThreadUri.THREAD_URI,cv,
	// 				Sms.ThreadColumns.PHONE_THREAD_ID+"="+se.getPhoneThreadId(),null);
	// 		haveUpdateThreadList.add(phoneThreadId);
			
	// 	}
		

	// }
	
//	private String[] removeSameData(String[] old){
//		ArrayList<String> list=new ArrayList<String>();
//		for(String o:old){
//			if(!list.contains(o)){
//				list.add(o);
//			}
//		}
//		return list.toArray(new String[list.size()]);
//	}

	 private void notifyThreadChanged(){
		 getContext().getContentResolver().notifyChange(Sms.getThreadNotifyUri(),
	        null);
	 }

	@Override
	protected Uri insertWithCustomUri(Uri uri, ContentValues values) {
		String table = null;
		switch (URI_MATCHER.match(uri)) {
		case THREAD_BASE:
			table = Sms.THREAD_NAME;
			break;
		}
		long id = this.getWritableDatabase().insert(table, null, values);
		Uri path = Uri.parse("content://" + table + "/" + id);
		// if(!isApplyBatch()){
		// notifyThreadChanged();
		// }
		return path;
	}

	@Override
	protected Cursor queryWithCustomUri(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		String table = null;
		switch (URI_MATCHER.match(uri)) {
		case THREAD_BASE:
			table = Sms.THREAD_NAME;
			break;
		case THREAD_DRAFT:
//			table = Sms.THREAD_NAME;
//			table=getDraftTable();
//			for(String pro:projection){
//				Log.e("yangliu","query projection is :"+pro);
//			}
			break;
		}
		
		
		return this.getReadableDatabase().query(true, table, projection,
				selection, selectionArgs, null, null, sortOrder, null);
	}

	@Override
	protected int updateWithCustomUri(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {
		String table = null;
		switch (URI_MATCHER.match(uri)) {
		case THREAD_BASE:
			table = Sms.THREAD_NAME;
			break;
		}
		int id = this.getWritableDatabase().update(table, values, selection,
				selectionArgs);
		// if(!isApplyBatch()){
		// notifyThreadChanged();
		// }
		return id;
	}

	@Override
	protected Uri getNotifyUri() {
		return Sms.getSmsNotifyUri();
	}

}

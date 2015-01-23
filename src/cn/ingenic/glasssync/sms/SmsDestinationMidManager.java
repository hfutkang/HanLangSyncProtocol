package cn.ingenic.glasssync.sms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.Column;
import cn.ingenic.glasssync.services.mid.DefaultColumn;
import cn.ingenic.glasssync.services.mid.MidException;
import cn.ingenic.glasssync.services.mid.SimpleMidDestManager;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import android.content.Intent;
import android.os.Bundle;
public class SmsDestinationMidManager extends SimpleMidDestManager {

	private Context mContext;
	private List<Column> mColumnList;
	private Map<Long, ThreadEntry> mThreadMap;
	private Map<Long, SmsEntry> mSmsMap;

	public SmsDestinationMidManager(Context ctx, SyncModule module) {
		super(ctx, module);
		this.mContext = ctx;
	}

	@Override
	protected List<Column> getDestColumnList() {
		if (mColumnList == null) {
			mColumnList = new ArrayList<Column>();
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.ADDRESS,
					Column.STRING));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.BODY,
					Column.STRING));
			mColumnList
					.add(new DefaultColumn(Sms.SmsColumns.DATE, Column.LONG));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.ERROR_CODE,
					Column.INTEGER));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.READ,
					Column.INTEGER));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.SEEN,
					Column.INTEGER));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.TYPE,
					Column.INTEGER));
			mColumnList.add(new DefaultColumn(Sms.SmsColumns.PHONE_THREAD_ID,
					Column.LONG));
		}

		return mColumnList;
	}

	@Override
	protected String getMidAuthorityName() {
		return "sms-dest";
	}
	
	

	@Override
	protected void onDatasClear() {
		super.onDatasClear();
		mContext.getContentResolver().delete(ThreadEntry.ThreadUri.THREAD_DELETE_ALL_URI, null, null);
	}

	@Override
	protected ArrayList<ContentProviderOperation> applySyncDatas(
			SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
			SyncData[] appends) throws MidException {

		    if(inserts!=null){
			for(int i = 0;i<=inserts.length-1;i++){
			    int type = inserts[i].getInt("type");
			    int read = inserts[i].getInt("read");
			    String body = inserts[i].getString("body");
			    String address = inserts[i].getString("address");
			    if(type == 1 && read == 0){
				if (Sms.DEBUG){
				    Log.i(Sms.TAG,"new sms come and sendBroadcast:action.new_sms.RECEIVER");
				}
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				intent.setAction("action.new_sms.RECEIVER");  
				bundle.putString("address", address);
				bundle.putString("body",body);
				intent.putExtras(bundle);
				mContext.sendBroadcast(intent);
			    }
			}
			if (Sms.DEBUG){
			Log.i(Sms.TAG,"applySyncData insert size is :"+inserts.length);
			}
		    
		    if(deletes!=null)Log.i(Sms.TAG,"applySyncData deletes size is :"+deletes.length);
		    if(updates!=null)Log.i(Sms.TAG,"applySyncData updates size is :"+updates.length);
		    if(appends!=null)Log.i(Sms.TAG,"applySyncData appends size is :"+appends.length);
		}
		ArrayList<ContentProviderOperation> list=new ArrayList<ContentProviderOperation>();
		if(deletes!=null)checkDeleteThreadDate(list,deletes);
		list.addAll(list.size(), super.applySyncDatas(deletes, updates, inserts, appends));
//		ArrayList<ContentProviderOperation> list = super.applySyncDatas(
//				deletes, updates, inserts, appends);
		// if(deletes!=null)list=executeDelete(deletes,list);
		if (appends != null)
			list = appendThreadOperationList(appends, list);
		if (deletes != null)
			checkDeleteThreadData(list,deletes);
		return list;
	}

	// private ArrayList<ContentProviderOperation> executeDelete(SyncData[]
	// deletes,ArrayList<ContentProviderOperation> list){
	// Log.e("yangliu","executeDelete .... delete size is :"+deletes.length);
	// Map<Long,ThreadEntry> threadMap=getThreadMap();
	// Map<Long,SmsEntry> smsMap=getSmsMap();
	// for(SyncData delete:deletes){
	// long smsId=delete.getLong(Mid.COLUMN_KEY);
	// if(!smsMap.containsKey(smsId)){
	// Log.e("SmsDestinationMidManager","execut e will delete sms find id is :"+smsId+" sms not exist !!!");
	// continue;
	// }
	// SmsEntry se=smsMap.get(smsId);
	// long phoneThreadId=se.getPhoneThreadId();
	// Log.e("yangliu","in executeDelete smsId is :"+smsId+" , and phone Thread id is :"+phoneThreadId);
	// ThreadEntry te=threadMap.get(phoneThreadId);
	// ContentProviderOperation operation;
	// if(te.getMessageCount()>1){
	// operation=ContentProviderOperation.newUpdate(ThreadEntry.THREAD_URI)
	// .withValue(Sms.ThreadColumns.MESSAGE_COUNT, te.getMessageCount()-1)
	// .build();
	// }else{
	// operation=ContentProviderOperation.newDelete(ThreadEntry.THREAD_URI)
	// .withSelection(Sms.ThreadColumns.PHONE_THREAD_ID+"="+String.valueOf(phoneThreadId),
	// null)
	// .build();
	// }
	// te.setMessageCount(te.getMessageCount()-1);
	// list.add(operation);
	// }
	//
	// return list;
	// }
	//
	private ArrayList<ContentProviderOperation> appendThreadOperationList(
			SyncData[] appends, ArrayList<ContentProviderOperation> list) {

		Map<Long, ThreadEntry> idMap = getThreadMap();
		for (SyncData data : appends) {
			long phoneThreadId = data.getLong(Sms.ThreadColumns.ID);
			ContentValues cv = getThreadValue(data);
			ContentProviderOperation operation;

			if (idMap != null && idMap.containsKey(phoneThreadId)) {
				operation = ContentProviderOperation
						.newUpdate(ThreadEntry.ThreadUri.THREAD_URI)
						.withValues(cv)
						.withSelection(
								Sms.ThreadColumns.PHONE_THREAD_ID + "="
										+ String.valueOf(phoneThreadId), null)
						.build();
			} else {
				operation = ContentProviderOperation
						.newInsert(ThreadEntry.ThreadUri.THREAD_URI).withValues(cv)
						.build();
			}
			list.add(operation);
		}
		return list;
	}
	
	private void checkDeleteThreadDate(ArrayList<ContentProviderOperation> list,SyncData[] deletes){
		String selection="";
		for(int i=0;i<deletes.length;i++){
			long mid_key=deletes[i].getLong(Sms.MID_KEY_COLUMN);
			if(i==0){
				selection=String.valueOf(mid_key);
			}else{
				selection=selection+","+mid_key;
			}
		}
		
		list.add(ContentProviderOperation.newDelete(
				ThreadEntry.ThreadUri.THREAD_CHECK_URI)
				.withSelection(selection, null).build());
	}

	private void checkDeleteThreadData(ArrayList<ContentProviderOperation> list,SyncData[] deletes) {
	
		list.add(ContentProviderOperation.newDelete(
				ThreadEntry.ThreadUri.THREAD_CHECK_URI).build());
	}

	private Map<Long, ThreadEntry> getThreadMap() {
		Map<Long, ThreadEntry> mThreadMap = new HashMap<Long, ThreadEntry>();
		Cursor cursor = mContext.getContentResolver().query(
				ThreadEntry.ThreadUri.THREAD_URI, ThreadEntry.PROJECTION, null, null,
				null);
		if (cursor.getCount() == 0) {
			cursor.close();
			if (Sms.DEBUG)
				Log.d(Sms.TAG, "table thread have no datas");
			return null;
		}
		cursor.moveToFirst();
		do {

			ThreadEntry te = new ThreadEntry(cursor);
			mThreadMap.put(te.getPhoneThreadId(), te);
//			if (Sms.DEBUG)
//				Log.i(Sms.TAG, "get local thread addres is :" + te.getAddress()
//						+ " snippet is :" + te.getSnippet());
		} while (cursor.moveToNext());
		cursor.close();
		if (Sms.DEBUG)
			Log.i(Sms.TAG, "local thread size is :" + mThreadMap.size());
		return mThreadMap;
	}

	// private Map<Long,SmsEntry> getSmsMap(){
	// if(mSmsMap==null){
	// mSmsMap=new HashMap<Long,SmsEntry>();
	// Cursor cursor=mContext.getContentResolver().query(SmsEntry.SMS_URI,
	// SmsEntry.PROJECTION, null, null, null);
	// if(cursor.getCount()==0){
	// cursor.close();
	// return null;
	// }
	// cursor.moveToFirst();
	// do{
	//
	// SmsEntry se=new SmsEntry(cursor);
	// mSmsMap.put(se.getMidKey(), se);
	// }while(cursor.moveToNext());
	// cursor.close();
	// }
	// return mSmsMap;
	// }

	private ContentValues getThreadValue(SyncData data) {
		ContentValues cv = new ContentValues();
		cv.put(Sms.ThreadColumns.DATE, data.getLong(Sms.ThreadColumns.DATE));
//		cv.put(Sms.ThreadColumns.ERROR, data.getInt(Sms.ThreadColumns.ERROR));
//		cv.put(Sms.ThreadColumns.MESSAGE_COUNT,
//				data.getInt(Sms.ThreadColumns.MESSAGE_COUNT));
		cv.put(Sms.ThreadColumns.PHONE_THREAD_ID,
				data.getLong(Sms.ThreadColumns.ID));
//		cv.put(Sms.ThreadColumns.READ, data.getInt(Sms.ThreadColumns.READ));
//		String[] adArray = data.getStringArray(Sms.ThreadColumns.RECIPIENT_ADS);
//		String mAllAddress = null;
//		for (int l = 0; l < adArray.length; l++) {
//			if (mAllAddress == null) {
//				mAllAddress = adArray[l];
//			} else {
//				mAllAddress = mAllAddress + "," + adArray[l];
//			}
//		}
//		cv.put(Sms.ThreadColumns.RECIPIENT_ADS, mAllAddress);
//		cv.put(Sms.ThreadColumns.SNIPPET,
//				data.getString(Sms.ThreadColumns.SNIPPET));
//		cv.put(Sms.ThreadColumns.TYPE, data.getInt(Sms.ThreadColumns.TYPE));
		return cv;

	}

}

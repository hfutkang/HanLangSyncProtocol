package cn.ingenic.contactslite.common;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public final class ContactsLiteContract {
	
	protected interface ContentColumn {
		public static final String _ID = "_id";
	}
	
	protected interface ContactsColumn extends ContentColumn {
		public static final String DISPLAY_NAME = "displayName";
		public static final String SORT_KEY = "sortKey";
		public static final String WATCH_SORT_KEY="watch_sort_key";
		public static final String VERSION = "version";
		public static final String LOOKUP_KEY = "lookup";
	}
	
	public static class Tables {
		public static final String CONTACTS = "contacts";
		public static final String DATA = "data";
	}
	
	protected interface DataColumn extends ContentColumn {
		public static final String CONTACT_ID = "contact_id";
		public static final String MIMETYPE = "mimetype";
		public static final String DATA1 = "data1";
		public static final String TYPE = "type";
		public static final String LABEL = "label";
	}
	
	public static class Contacts implements ContactsColumn {
		private Contacts() {};
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "contacts");
		public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(CONTENT_URI, "lookup");
		
		/* TODO 
		public static String lookupNameByPhoneNumber(Context context, String phoneNumber) {
			
		}
		*/
	}
	
	public static interface MimeType{
		public static final String EMAIL_MIMETYPE="vnd.android.cursor.item/email_v2";
		public static final String IM_MIMETYPE="vnd.android.cursor.item/im";
		public static final String NICKNAME_MIMETYPE="vnd.android.cursor.item/nickname";
		public static final String ORG_MIMETYPE="vnd.android.cursor.item/organization";
		public static final String PHONE_MIMETYPE="vnd.android.cursor.item/phone_v2";
		public static final String SIPADDRESS_MIMETYPE="vnd.android.cursor.item/sip_address";
		public static final String STRUCTUREDNAME_MIMETYPE="vnd.android.cursor.item/name";
		public static final String STRUCTUREDPOSTAL_MIMETYPE="vnd.android.cursor.item/postal-address_v2";
	}
	
	public static class MidTable implements ContactsColumn {
		private MidTable() {};
		
		private static final String AUTHORITY = "contactslite.mid-dest";
		public static final Uri CONTENT_URI=Uri.parse("content://"+AUTHORITY);
		
	}
	
	public static class Data implements DataColumn {
		private Data() {};
		
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "data");
	}
	
	public static final String AUTHORITY = "cn.ingenic.contactslite";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	
	public static class SimpleContacts{
		private Context mContext;
		
		private String[] PROJECT_DATA={
				DataColumn.CONTACT_ID,
				DataColumn.DATA1,
				DataColumn.LABEL,
				DataColumn.MIMETYPE,
				DataColumn.TYPE,
		};
		
		public SimpleContacts(Context context){
			mContext=context;
		}
		
		public Map<Long,Contact> getAllContacts(){
			Cursor cursor=mContext.getContentResolver().query(Data.CONTENT_URI, PROJECT_DATA, null, null, null);
			if(cursor == null) {
				return null;
			}
			
			if (cursor.getCount()==0){
				cursor.close();
				return null;
			}
			
			Map<Long,Contact> map=new HashMap<Long,Contact>();
			cursor.moveToFirst();
			do{
				long id=cursor.getLong(cursor.getColumnIndex(DataColumn.CONTACT_ID));
				String data=cursor.getString(cursor.getColumnIndex(DataColumn.DATA1));
//				String label=cursor.getString(cursor.getColumnIndex(DataColumn.LABEL));
				String mimeType=cursor.getString(cursor.getColumnIndex(DataColumn.MIMETYPE));
//				int type=cursor.getInt(cursor.getColumnIndex(DataColumn.TYPE));
				Contact c;
				if(map.containsKey(id)){
					c=map.get(id);
				}else{
					c=new Contact();
					c.mId=id;
				}
				if(mimeType.equals(MimeType.STRUCTUREDNAME_MIMETYPE)){
					c.mName=data;
				}else if(mimeType.equals(MimeType.PHONE_MIMETYPE)){
					c.setAddress(data);
				}
				
				map.put(id, c);
			}while(cursor.moveToNext());
			cursor.close();
			return map;
		}
		
		private Contact getContact(String address){
			Cursor cursor=mContext.getContentResolver().query(Data.CONTENT_URI, PROJECT_DATA, 
					DataColumn.CONTACT_ID+" IN SELECT "+DataColumn.CONTACT_ID+" FROM data WHERE "
					+DataColumn.MIMETYPE+"="+MimeType.PHONE_MIMETYPE+" AND "
					+DataColumn.DATA1+"="+address, null, null);
			if(cursor.getCount()==0){
				cursor.close();
				return null;
			}
			cursor.moveToFirst();
			Contact c=new Contact();
			do{
				long id=cursor.getLong(cursor.getColumnIndex(DataColumn.CONTACT_ID));
				String data=cursor.getString(cursor.getColumnIndex(DataColumn.DATA1));
				String mimeType=cursor.getString(cursor.getColumnIndex(DataColumn.MIMETYPE));
				c.mId=id;
				if(mimeType.equals(MimeType.STRUCTUREDNAME_MIMETYPE)){
					c.mName=data;
				}else if(mimeType.equals(MimeType.PHONE_MIMETYPE)){
					c.setAddress(data);
				}
			}while(cursor.moveToNext());
			cursor.close();
			return c;
			
		}
		
		
	}
	
}

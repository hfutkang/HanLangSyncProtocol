package cn.ingenic.contactslite.common;

import java.util.*;

import cn.ingenic.contactslite.common.ContactsLiteContract.Contacts;
import cn.ingenic.contactslite.common.ContactsLiteContract.Data;
import cn.ingenic.glasssync.services.mid.Mid;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * a class holding all datas of single contact. 
 * TODO: using Builder pattern to build this class.
 */
public class ContactPacket {
	private final String lookupKey;
	transient public long conactId;
	transient public long name_raw_contact_id;
	public String mDisplayName;
	public String mSortKey;
	public int mPhoneId;
//	public String mPhoneUri; 
	private Map<String, String> versions = new HashMap<String, String>();
	
	private List<DataEntity> datas = new ArrayList<DataEntity>(3);
	
	public ContactPacket(String lookupKey) {
		this.lookupKey = lookupKey;
	}
	
	public String getLookupKey() {
		return lookupKey;
	}
	

	public static ContactPacket getContactPacketByContactId(Context context, String contactId) {
		ContactPacket contact = null;
		ContentResolver resolver = context.getContentResolver();
		Cursor cursor = resolver.query(ContactsLiteContract.AUTHORITY_URI, 
				new String[] {Contacts.DISPLAY_NAME, Mid.COLUMN_KEY, Contacts.SORT_KEY},
				Contacts._ID + "=?",
				new String[]{contactId}, null);
		if (isCursorEmpty(cursor)) 
			return contact;
		cursor.moveToNext();
		String lookupKey = cursor.getString(cursor.getColumnIndex(Mid.COLUMN_KEY));
		String displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
		contact = new ContactPacket(lookupKey);
		contact.mDisplayName=displayName;
		
		cursor.close();
		
		Cursor dataCursor = resolver.query(Data.CONTENT_URI, 
				new String[] {Data.DATA1,Data.TYPE, Data.LABEL, Data.MIMETYPE}, 
				Data.CONTACT_ID + "=?", 
				new String[]{contactId}, 
				null);
		if (isCursorEmpty(dataCursor)) 
			return contact;
		while (dataCursor.moveToNext()) {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String data = dataCursor.getString(dataCursor.getColumnIndex(Data.DATA1));
			int type = dataCursor.getInt(dataCursor.getColumnIndex(Data.TYPE));
			String label = dataCursor.getString(dataCursor.getColumnIndex(Data.LABEL));
			DataEntity entity = new DataEntity(mimeType, data, type, label);
			contact.addDataEntity(entity);
		}
		dataCursor.close();
		
		return contact;
	}
	
	private static boolean isCursorEmpty(Cursor cursor) {
		return cursor == null || cursor.getCount() == 0;
	}
	
	public static final class DataEntity {
		private final String mimeType;
		private final String data;
		private final int type;
		private final String label;
		//TODO too much arguments, replace maybe Builder pattern
		public DataEntity(String mimeType, String data, int type, String label) {
			this.mimeType = mimeType;
			this.data = data;
			this.type = type;
			this.label = label;
		}
		
		public String getData() {
			return data;
		}
		
		public String getLabel() {
			return label;
		}
		
		public int getType() {
			return type;
		}
		
		public String getMimeType() {
			return mimeType;
		}
	}
	
	public void appendVersion(String rawContactId, String anotherVersion) {
		if (versions.get(rawContactId) == null) {
			versions.put(rawContactId, anotherVersion);
		}
	}
	
	public String getVersion() {
		String v = "";
		Iterator<String> iterator = versions.values().iterator();
		for (v = iterator.next();iterator.hasNext();) {
			v += "," + iterator.next();
		}
		return v;
	}
	
	public void addDataEntity(DataEntity data) {
		datas.add(data);
	}
	
	public List<DataEntity> getDatas() {
		return datas;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("lookupKey: " + lookupKey);
		sb.append("\n");
		sb.append("displayName: " + mDisplayName);
		sb.append("\n");
		sb.append("sortKey: " + mSortKey);
		sb.append("\n");
		for (DataEntity entity : datas) {
			sb.append(entity.data + " | ");
		}
		return sb.toString();
	}
	
	public ContentValues getContactContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(Contacts.DISPLAY_NAME, mDisplayName);
		cv.put(Mid.COLUMN_KEY, lookupKey);
		cv.put(Contacts.SORT_KEY, mSortKey);
		ContactSortKeyParse p=ContactSortKeyParse.getContactSortKeyParse();
		cv.put(Contacts.WATCH_SORT_KEY, p.parseSortKeyTop(mSortKey));
		return cv;
	}
	
	public List<ContentValues> getDataContentValues() {
		List<ContentValues> cvs = new ArrayList<ContentValues>();
		for (DataEntity entity : datas) {
			String mimeType = entity.mimeType;
			String data = entity.data;
			int type = entity.type;
			String label = entity.label;
			ContentValues cv = new ContentValues();
			cv.put(Data.MIMETYPE, mimeType);
			cv.put(Data.DATA1, data);
			cv.put(Data.TYPE, type);
			cv.put(Data.LABEL, label);
			cvs.add(cv);
		}
		return cvs;
	}
}

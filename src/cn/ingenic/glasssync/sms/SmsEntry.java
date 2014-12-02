package cn.ingenic.glasssync.sms;

import java.util.Map;

import cn.ingenic.contactslite.common.Contact;
// import cn.ingenic.glasssync.sms.SmsApp.ContactSet;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SmsEntry {

	private int id = -1;
	private String address = " ";
	private String body = null;
	private int read = -1;
	private int error = -1;
	private int type = -1;
	private long data = -1;
	private long threadId = -1;
	private long mMidKey = -1;
	
	public interface SmsUri{
		public static Uri SMS_URI = Uri.parse("content://sms-dest");
	}
	
	public static String[] PROJECTION = { Sms.SmsColumns.ID,
			Sms.SmsColumns.ADDRESS, Sms.SmsColumns.BODY, Sms.SmsColumns.DATE,
			Sms.SmsColumns.ERROR_CODE, Sms.SmsColumns.PHONE_THREAD_ID,
			Sms.SmsColumns.READ, Sms.SmsColumns.SEEN, Sms.SmsColumns.TYPE,
			Sms.MID_KEY_COLUMN, };

	public SmsEntry(Cursor cursor) {
		id = cursor.getInt(cursor.getColumnIndex(Sms.SmsColumns.ID));
		address = cursor.getString(cursor
				.getColumnIndex(Sms.SmsColumns.ADDRESS));
		body = cursor.getString(cursor.getColumnIndex(Sms.SmsColumns.BODY));
		read = cursor.getInt(cursor.getColumnIndex(Sms.SmsColumns.READ));
		error = cursor.getInt(cursor.getColumnIndex(Sms.SmsColumns.ERROR_CODE));
		type = cursor.getInt(cursor.getColumnIndex(Sms.SmsColumns.TYPE));
		data = cursor.getLong(cursor.getColumnIndex(Sms.SmsColumns.DATE));
		threadId = cursor.getLong(cursor
				.getColumnIndex(Sms.SmsColumns.PHONE_THREAD_ID));
		mMidKey = cursor.getLong(cursor.getColumnIndex(Sms.MID_KEY_COLUMN));
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setError(int error) {
		this.error = error;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setData(long data) {
		this.data = data;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public int getId() {
		return id;
	}

	public String getAddress() {
		if(address==null){
			return " ";
		}
		return address;
	}

	public String getBody() {
		return body;
	}

	public int getRead() {
		return read;
	}

	public void setRead(int read) {
		this.read = read;
	}

	public int getError() {
		return error;
	}

	public int getType() {
		return type;
	}

	public long getData() {
		return data;
	}

	public long getPhoneThreadId() {
		return threadId;
	}

	public long getMidKey() {
		return mMidKey;
	}
	
	
	
	// public String getContact(ContactSet contactSet){
	// 	Contact contact=contactSet.getOneContact(address);
	// 	if(contact==null){
	// 		return address;
	// 	}
	// 	return contact.mName;
	// }

}

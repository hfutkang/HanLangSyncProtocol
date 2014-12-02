package cn.ingenic.glasssync.sms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.ingenic.contactslite.common.Contact;
// import cn.ingenic.glasssync.sms.SmsApp.ContactSet;
// import cn.ingenic.glasssync.sms.activity.Draft;
import android.R.integer;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ListView;

public class ThreadEntry {

	private long mId;
	private String mSnippet;
	private long mDate;
	private long mPhoneThreadId;
	private String mAddress="";
	private String[] mAddressArray;
	private int mRead;
	private int mError;
	private int mMessagecount;
//	private int type;
	private String mSortKey;
	private String mName; 
	private boolean showErrorIcon=false;
	private String arrayNameFirst = "";
	private String stationFirst = "";
	private int mUnRead;
	private List<Integer> mCount = new ArrayList<Integer>();

	public interface ThreadUri{
		public static Uri THREAD_URI = Uri.parse("content://sms-dest/thread");
		public static Uri THREAD_CHECK_URI = Uri
				.parse("content://sms-dest/thread/check");
		public static Uri THREAD_DELETE_ALL_URI=Uri.parse("content://sms-dest/thread/deleteall");
		
//		public static Uri THREAD_DEAFT=Uri.parse("content://sms-dest/thread/draft");
	}

	

	public static String[] PROJECTION = { 
		Sms.ThreadColumns.ID,
		Sms.ThreadColumns.DATE, 
//		Sms.ThreadColumns.ERROR,
//		Sms.ThreadColumns.MESSAGE_COUNT, 
		Sms.ThreadColumns.PHONE_THREAD_ID,
//		Sms.ThreadColumns.READ, 
//		Sms.ThreadColumns.RECIPIENT_ADS,
//		Sms.ThreadColumns.SNIPPET, 
//		Sms.ThreadColumns.TYPE,
        
	};
	
	public ThreadEntry(){
	}

	public ThreadEntry(Cursor threadCursor) {
		mId = threadCursor.getInt(threadCursor.getColumnIndex(Sms.ThreadColumns.ID));
//		snippet = cursor.getString(cursor
//				.getColumnIndex(Sms.ThreadColumns.SNIPPET));
		mDate = threadCursor.getLong(threadCursor.getColumnIndex(Sms.ThreadColumns.DATE));
		mPhoneThreadId = threadCursor.getInt(threadCursor
				.getColumnIndex(Sms.ThreadColumns.PHONE_THREAD_ID));
//		address = cursor.getString(cursor
//				.getColumnIndex(Sms.ThreadColumns.RECIPIENT_ADS));
//		error = cursor.getInt(cursor.getColumnIndex(Sms.ThreadColumns.ERROR));
//		read = cursor.getInt(cursor.getColumnIndex(Sms.ThreadColumns.READ));
//		message_count = cursor.getInt(cursor
//				.getColumnIndex(Sms.ThreadColumns.MESSAGE_COUNT));
//		type = cursor.getInt(cursor.getColumnIndex(Sms.ThreadColumns.TYPE));
//		addressArray = address.split(",");

	}

	public long getId() {
		return mId;
	}

	public void setId(long id) {
		this.mId = id;
	}

	public void setAddress(String address) {
		this.mAddress = address;
	}

	public void setAddressArray(String[] addressArray) {
		this.mAddressArray = addressArray;
	}

	public void setRead(int read) {
		if (read == Sms.UN_READ) {
			mCount.add(read);
			this.mRead = mCount.size();
		}
	}

	public void setError(int error) {
		this.mError = error;
	}

//	public void setType(int type) {
//		this.type = type;
//	}

	public String getSnippet() {
		return mSnippet;
	}

	public void setSnippet(String snippet) {
		this.mSnippet = snippet;
	}

	public long getData() {
		return mDate;
	}

	public void setData(long data) {
		this.mDate = data;
	}

	public long getPhoneThreadId() {
		return mPhoneThreadId;
	}

	public void setPhoneThreadId(long phone_thread_id) {
		this.mPhoneThreadId = phone_thread_id;
	}

	public String getAddress() {
		if(mAddress==null||mAddress.equals("")){
			//if a conversation has only one draft address is null
			return " ";
		}
		getRemoveHyphenAddress();
		return mAddress;
	}

	public String[] getAddressArray() {
		return mAddressArray;
	}

	public int getRead() {
		return mRead;
	}

	public int getError() {
		return mError;
	}

	public void setMessageCount(int count) {
		this.mMessagecount = count;
	}

	public int getMessageCount() {
		return mMessagecount;
	}

//	public int getType() {
//		return type;
//	}
	
	public void setShowErrorIcon(boolean show){
		this.showErrorIcon=show;
	}
	
	public boolean showErrorIcon(){
		return showErrorIcon;
	}
	
	private void getRemoveHyphenAddress(){
		String[] array=null;
		if(mAddress.contains("-")){
			array=mAddress.split("-");
		}else if(mAddress.contains(" ")){
			array=mAddress.split(" ");
		}else{
			return;
		}
		String address="";
		for(String s:array){
			address+=s;
		}
		mAddress=address;
	}
		
	// public String getContact(ContactSet contactSet){
		
	// 	String address=getAddress();
	// 	String[] addArray=address.split(",");
	// 	if(!contactSet.haveContact())return address;
	// 	String name="";
	// 	for(int l=0;l<addArray.length;l++){
	// 		Contact contact=contactSet.getOneContact(addArray[l]);
	// 		if(contact==null){
				
	// 			if(l==0){
	// 				name=addArray[l];
	// 			}else{
	// 				name=name+","+addArray[l];
	// 			}
	// 			continue;
	// 		}
	// 		if(l==0){
	// 			name=contact.mName;
	// 		}else{
	// 			if (arrayNameFirst.equals("")) {
	// 				arrayNameFirst = name;
	// 			}
	// 			name=name+","+contact.mName;			
	// 		}
	// 	}
	// 	setName(name);
	// 	return name;
	// }
	
	private void setName(String name){
		this.mName=name;
	}
	
	public String getName(){
		return mName;
	}

	public String getmSortKey() {
		return mSortKey;
	}

	public void setmSortKey(String mSortKey) {
		this.mSortKey = mSortKey;
	}

	public String getArrayNameFirst() {
		return arrayNameFirst;
	}

	public void setArrayNameFirst(String arrayNameFirst) {
		this.arrayNameFirst = arrayNameFirst;
	}

	public String getStationFirst() {
		return stationFirst;
	}

	public void setStationFirst(String stationFirst) {
		this.stationFirst = stationFirst;
	}

	public int getmUnRead() {
		return mUnRead;
	}

	public void setmUnRead(int mUnRead) {
		this.mUnRead = mUnRead;
	}
}

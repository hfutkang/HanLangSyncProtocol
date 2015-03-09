package cn.ingenic.glasssync.contact;

import java.util.List;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import cn.ingenic.contactslite.common.ContactsLiteContract;
import cn.ingenic.contactslite.common.ContactsLiteContract.Tables;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.CustomDatabaseHelperCallback;
import cn.ingenic.glasssync.services.mid.MidDestContentProvider;


public class ContactsLiteProvider extends MidDestContentProvider {
	
	private static boolean DEBUG=false;

	@Override
	public SyncModule getSyncModule() {
		return ContactsLiteModule.getInstance(getContext().getApplicationContext());
	}
	
	@Override
	protected CustomDatabaseHelperCallback getCustomDatabaseHelperCallback() {
		return new ContactsLiteDatabaseHelper();
	}

	private static final String TAG = "ContactsLiteProvider";
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int BASE = 0x123;
	private static final int CONTACTS = BASE + 1;
	private static final int CONTACTS_ID = BASE + 2;
	
	private static final int DATA = BASE + 11;
	
	
	static {
		UriMatcher matcher = sURIMatcher;
		matcher.addURI(ContactsLiteContract.AUTHORITY, "contacts", CONTACTS);
		matcher.addURI(ContactsLiteContract.AUTHORITY, "contacts/#", CONTACTS_ID);
		
		matcher.addURI(ContactsLiteContract.AUTHORITY, "data", DATA);
	};
	
	private static int findMatch(Uri uri, String methodName) {
		int match = sURIMatcher.match(uri);
		if (match == UriMatcher.NO_MATCH) {
			throw new IllegalArgumentException("unknown " + uri.toString());
		} else {
			if(DEBUG)Log.d(TAG, methodName + ": " + "Uri is " + uri.toString());
		}
		return match;
	}
	
	private SQLiteDatabaseProxy mDatabase;
	
	private SQLiteDatabaseProxy getDatabase(Context context) {
		if (mDatabase == null) {
			mDatabase = getWritableDatabase();
		}
		return mDatabase;
	}
	
	// 发送数据库变化广播给语音识别广播接收器
	private Intent sendBroadcastToVoice(Context context, int method, String selection, ContentValues values) {
//		Log.d(TAG, "sendBroadcastToVoice method:"+method+" selection:"+selection+" values:"+values);
		Intent intent = new Intent("cn.ingenic.glasssync.contact.DATA_CHANGE");             
		intent.setComponent(new ComponentName("com.ingenic.glass.voicerecognizer", "com.ingenic.glass.voicerecognizer.contactsync.ContactSyncReceiver"));
		intent.addCategory("com.ingenic.glass.voicerecognizer.contactsync.ContactSyncReceiver");
		intent.putExtra("method", method);
		intent.putExtra("selection", selection);
		intent.putExtra("values", values);
		context.sendBroadcast(intent);
		return intent;
	}
	
	@Override
	public int deleteWithCustomUri(Uri uri, String selection, String[] selectionArgs) {
		int match = findMatch(uri, "delete");
		Context context = getContext();
		
		SQLiteDatabaseProxy db = getDatabase(context);
		String table = whichTableUriMatch(match);
		switch(match) {
		case CONTACTS:
			break;
		case CONTACTS_ID:
			String contactId = getSegmentFromUri(uri, 1);
			selection = whereWithId(contactId, selection);
			break;
		case DATA:
			break;
		}
		int result = db.delete(table, selection, selectionArgs);
		Log.d("yangliu", "delete db :" + db + " table:" + table + " id:" + result);
		if (result > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			
			sendBroadcastToVoice(getContext(), 1 /*METHOD_DELETE*/, selection, null);
		}
		return result;
	}

    private String whereWithId(String id, String selection) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(Contacts._ID +"=");
        sb.append(id);
        if (selection != null) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }
    
	private String getSegmentFromUri(Uri uri, int index) {
		int expectedSize = index + 1;
		final List<String> pathSegments = uri.getPathSegments();
		final int segmentCount = pathSegments.size();
        if (segmentCount < expectedSize) {
            throw new IllegalArgumentException(uri.toString() 
            		+ " invalid, expect more path segment.");
        } else {
        	return pathSegments.get(index);
        }
	}
	
	private long getLongFromUri(Uri uri, int index) {
		String value = getSegmentFromUri(uri, index);
		return Long.valueOf(value);
	}
	
	@Override
	public Uri insertWithCustomUri(Uri uri, ContentValues values) {
		int match = findMatch(uri, "insert");
		Context context = getContext();
        ContentResolver resolver = context.getContentResolver();

        SQLiteDatabaseProxy db = getDatabase(context);
        String table = whichTableUriMatch(match);
        long id = db.insert(table, null, values);
        Log.d("yangliu", "insert db :" + db + " table:" + table + " id:" + id);
        
        Uri resultUri = ContentUris.withAppendedId(uri, id);
        resolver.notifyChange(resultUri, null);
        sendBroadcastToVoice(getContext(), 0 /*METHOD_INSER*/, null, values);
        return resultUri;
	}

	@Override
	public boolean onCreate() {
		super.onCreate();
		return true;
	}

	@Override
	public Cursor queryWithCustomUri(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		//Thread.dumpStack();
		SQLiteDatabaseProxy db = getReadableDatabase();
		int match = findMatch(uri, "query");
		String table = whichTableUriMatch(match);
		Cursor c = db.query(false, table, projection, 
				selection, selectionArgs, null, null, sortOrder, null);
		Log.d("yangliu", "query db:" + db + " c:" + c + " c.c:" + c.getCount());
		return c;
	}

	private String whichTableUriMatch(int match) {
		String table = "";
		switch (match) {
		case CONTACTS:
		case CONTACTS_ID:
			table = Tables.CONTACTS;
			break;
		case DATA:
			table = Tables.DATA;
			break;
		}
		return table;
	}
	
	@Override
	public int updateWithCustomUri(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabaseProxy db = getDatabase(getContext());
		int match = findMatch(uri, "update");
		String table = whichTableUriMatch(match);
		int result = db.update(table, values, selection, selectionArgs);
		if (result > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			sendBroadcastToVoice(getContext(), 2 /*METHOD_UPDATE*/, selection, values);
		}
		return result;
	}
}

package cn.ingenic.glasssync.contact;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import cn.ingenic.contactslite.common.ContactsLiteContract.Data;
import cn.ingenic.contactslite.common.ContactsLiteContract.Tables;
import cn.ingenic.glasssync.services.mid.CustomDatabaseHelperCallback;

class ContactsLiteDatabaseHelper implements CustomDatabaseHelperCallback {
	private static final int CONTACTS_LITE_VERSION = 1;
	private static final String TAG = "ContactsLiteDatabaseHelper";
	
	@Override
	public void onCreateCustomTables(SQLiteDatabase db) {
		createDataTable(db);
	}
	
	private void createDataTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + Tables.DATA + "(" +
				Data._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				Data.MIMETYPE + " TEXT," +
				Data.DATA1 + " TEXT," +
				Data.TYPE + " INTEGER," +
				Data.LABEL + " TEXT," +
				Data.CONTACT_ID + " INTEGER NOT NULL" + 
				");");
	}

	@Override
	public void onCustomUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		Log.d(TAG, "upgrade database from " + oldVersion + " to " + newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.DATA + ";");
		createDataTable(db);
	}

	@Override
	public int getCustomVersion() {
		return CONTACTS_LITE_VERSION;
	}

}
package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.services.mid.CustomDatabaseHelperCallback;
import cn.ingenic.glasssync.sms.Sms;

import android.database.sqlite.SQLiteDatabase;

public class SmsDatabaseHelper implements CustomDatabaseHelperCallback {

	private static int version = 1;

	@Override
	public int getCustomVersion() {
		return version;
	}

	@Override
	public void onCreateCustomTables(SQLiteDatabase db) {
		createThreadTable(db);
	}

	private void createThreadTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS  " + Sms.THREAD_NAME + " ("
				+ Sms.ThreadColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Sms.ThreadColumns.DATE + " TEXT," 
//				+ Sms.ThreadColumns.ERROR+ " INTEGER NOT NULL DEFAULT -1,"
//				+ Sms.ThreadColumns.MESSAGE_COUNT+ " INTEGER NOT NULL DEFAULT -1,"
				+ Sms.ThreadColumns.PHONE_THREAD_ID+ " INTEGER NOT NULL DEFAULT -1" + " UNIQUE"
//				+ Sms.ThreadColumns.READ + " INTEGER NOT NULL DEFAULT -1,"
//				+ Sms.ThreadColumns.SNIPPET + " TEXT," 
//				+ Sms.ThreadColumns.TYPE+ " INTEGER NOT NULL DEFAULT -1,"
//				+ Sms.ThreadColumns.RECIPIENT_ADS + " TEXT" 
				+ ");");
	}

	@Override
	public void onCustomUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		db.execSQL("DROP TABLE IF EXISTS " + Sms.THREAD_NAME + ";");
		createThreadTable(db);

	}

}

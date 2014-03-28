package cn.ingenic.glasssync.services.mid;

import android.database.sqlite.SQLiteDatabase;

public interface CustomDatabaseHelperCallback {
	void onCreateCustomTables(SQLiteDatabase db);

	void onCustomUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

	int getCustomVersion();
}

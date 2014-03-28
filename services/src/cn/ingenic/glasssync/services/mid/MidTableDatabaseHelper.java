package cn.ingenic.glasssync.services.mid;

import java.util.HashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class MidTableDatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "MidTableDatabaseHelper";

	private static final String DATABASE_NAME = "midtable.db";
	static final int MID_DATABASE_VERSION = 1;
	private static final int RESERVED_SHIFT = 16;
	static final int RESERVED_DATABASE_VERSION = 1 << RESERVED_SHIFT;

	static final String TABLE_MID_SRC = "mid_src";

	static final String TABLE_MID_DEST = "mid_dest";

	private final MidTableManager mManager;
	private final CustomDatabaseHelperCallback mCallback;

	private static HashMap<String, MidTableDatabaseHelper> sMap = new HashMap<String, MidTableDatabaseHelper>();

	static MidTableDatabaseHelper getInstance(Context context, String module,
			MidTableManager mgr, CustomDatabaseHelperCallback callback) {
		MidTableDatabaseHelper helper = sMap.get(module);
		if (helper == null) {
			if (context == null || TextUtils.isEmpty(module) || mgr == null) {
				throw new IllegalArgumentException("Invalid Args.");
			}
			int version = 0;
			if (callback != null) {
				int customVersion = callback.getCustomVersion();
				if (customVersion >= RESERVED_DATABASE_VERSION
						|| customVersion <= 0) {
					throw new RuntimeException(
							"Invalid custom database version:"
									+ customVersion
									+ ". this version must be less than RESERVED_DATABASE_VERSION:"
									+ RESERVED_DATABASE_VERSION);
				}
				version = MID_DATABASE_VERSION << RESERVED_SHIFT
						| customVersion;
			} else {
				version = MID_DATABASE_VERSION;
			}
			Log.i(TAG, "Database version:" + version);
			helper = new MidTableDatabaseHelper(context, module, mgr, callback,
					version);
			sMap.put(module, helper);
		}

		return helper;
	}

	protected MidTableDatabaseHelper(Context context, String module, MidTableManager mgr,
			CustomDatabaseHelperCallback callback, int version) {
		super(context, module + "_" + DATABASE_NAME, null, version);
		mManager = mgr;
		mCallback = callback;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createMidSrcTable(db);
		createMidDestTable(db);
		if (mCallback != null) {
			mCallback.onCreateCustomTables(db);
		}
	}

	private void createMidDestTable(SQLiteDatabase db) {
		KeyColumn key = mManager.getDestKey();
		if (key == null) {
			Log.i(TAG,
					"do not create MidDestTable, Source role supported only.");
			return;
		}

		StringBuilder sb = new StringBuilder("CREATE TABLE ");
		sb.append(TABLE_MID_DEST).append(" (_id INTEGER PRIMARY KEY,");
		for (Column c : mManager.getDestTableList()) {
			sb.append(c.getName()).append(" ").append(c.getDbType())
					.append(",");
		}
		sb.append(key.getName() + " " + key.getDbType() + " UNIQUE);");
		String sql = sb.toString();

		Log.v(TAG, "createMidDestTable slq:" + sql);
		db.execSQL(sql);
	}

	private void createMidSrcTable(SQLiteDatabase db) {
		KeyColumn key = mManager.getSrcKey();
		if (key == null) {
			Log.i(TAG, "do not create MidSrcTable, Dest role supported only.");
			return;
		}

		StringBuilder sb = new StringBuilder("CREATE TABLE ");
		sb.append(TABLE_MID_SRC).append(" (_id INTEGER PRIMARY KEY,");
		for (Column c : mManager.getSrcTableList()) {
			sb.append(c.getName()).append(" ").append(c.getDbType())
					.append(",");
		}
		sb.append(key.getName() + " " + key.getDbType() + " UNIQUE,");
		SyncColumn syncColumn = new SyncColumn();
		sb.append(syncColumn.getName() + " " + syncColumn.getDbType() + ");");
		String sql = sb.toString();

		Log.v(TAG, "createMidSrcTable slq:" + sql);
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// do mid onUpgrade first

		if (mCallback != null) {
			int oldCustomVersion = oldVersion & RESERVED_DATABASE_VERSION;
			int newCustomVersion = newVersion & RESERVED_DATABASE_VERSION;
			mCallback.onCustomUpgrade(db, oldCustomVersion, newCustomVersion);
		}
	}
}

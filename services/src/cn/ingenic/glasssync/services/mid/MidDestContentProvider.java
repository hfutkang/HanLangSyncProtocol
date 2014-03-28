package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncModule;

public abstract class MidDestContentProvider extends ContentProvider implements
		ObtainSyncModule {

	private static final boolean TIME_CAL = true;
	private static final boolean V = false;

	private final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int URI_BASE = 0;

	private SQLiteOpenHelper mOpenHelper;

	private MidTableManager mManager;
	
	private boolean mIsApplyBatch=false;

	protected static class SQLiteDatabaseProxy {
		private final SQLiteDatabase mmDB;

		SQLiteDatabaseProxy(SQLiteDatabase database) {
			if (database == null) {
				throw new IllegalArgumentException("database can not bu null.");
			}
			mmDB = database;
		}

		public Cursor query(boolean distinct, String table, String[] columns,
				String selection, String[] selectionArgs, String groupBy,
				String having, String orderBy, String limit) {
			return mmDB.query(distinct, table, columns, selection,
					selectionArgs, groupBy, having, orderBy, limit);
		}

		public long insert(String table, String nullColumnHack,
				ContentValues values) {
			if (MidTableDatabaseHelper.TABLE_MID_DEST.equals(table)) {
				throw new RuntimeException(
						"Insert is disabled for table mid_dest");
			}
			return mmDB.insert(table, nullColumnHack, values);
		}

		public int update(String table, ContentValues values,
				String whereClause, String[] whereArgs) {
			if (MidTableDatabaseHelper.TABLE_MID_DEST.equals(table)) {
				throw new RuntimeException(
						"Update is disabled for table mid_dest");
			}
			return mmDB.update(table, values, whereClause, whereArgs);
		}

		public int delete(String table, String whereClause, String[] whereArgs) {
			if (MidTableDatabaseHelper.TABLE_MID_DEST.equals(table)) {
				throw new RuntimeException(
						"Delete is disabled for table mid_dest");
			}
			return mmDB.delete(table, whereClause, whereArgs);
		}
	}

	protected final SQLiteDatabaseProxy getReadableDatabase() {
		return new SQLiteDatabaseProxy(mOpenHelper.getReadableDatabase());
	}

	protected final SQLiteDatabaseProxy getWritableDatabase() {
		return new SQLiteDatabaseProxy(mOpenHelper.getWritableDatabase());
	}

	@Override
	public boolean onCreate() {
		SyncModule module = getSyncModule();
		if (module == null) {
			throw new IllegalArgumentException(
					"can not get null from getSyncModule()");
		}

		mManager = module.getMidTableManager();
		if (mManager == null) {
			throw new IllegalArgumentException(
					"can not get null from module.getMidTableManager()");
		}

		mUriMatcher.addURI(mManager.getDestAuthorityName(), null, URI_BASE);

		mOpenHelper = MidTableDatabaseHelper.getInstance(getContext(), module.getName(),
				mManager, getCustomDatabaseHelperCallback());
		return true;
	}

	protected CustomDatabaseHelperCallback getCustomDatabaseHelperCallback() {
		return null;
	}

	@Override
	public final Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (V) {
			logv("query uri:" + uri);
		}
		switch (mUriMatcher.match(uri)) {
		case URI_BASE:
			return mOpenHelper.getReadableDatabase().query(
					MidTableDatabaseHelper.TABLE_MID_DEST, projection,
					selection, selectionArgs, null, null, sortOrder);
		default:
			return queryWithCustomUri(uri, projection, selection,
					selectionArgs, sortOrder);
		}
	}

	protected Cursor queryWithCustomUri(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public final String getType(Uri uri) {
		return null;
	}

	@Override
	public final Uri insert(Uri uri, ContentValues values) {
		if (V) {
			logv("insert uri:" + uri);
		}
		String table;
		switch (mUriMatcher.match(uri)) {
		case URI_BASE:
			table = MidTableDatabaseHelper.TABLE_MID_DEST;
			break;
		default:
			return insertWithCustomUri(uri, values);
		}
		long id = mOpenHelper.getWritableDatabase().insert(table, null, values);
		return ContentUris.withAppendedId(uri, id);
	}

	protected Uri insertWithCustomUri(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public final int delete(Uri uri, String selection, String[] selectionArgs) {
		if (V) {
			logv("delete uri:" + uri);
		}
		String table;
		switch (mUriMatcher.match(uri)) {
		case URI_BASE:
			table = MidTableDatabaseHelper.TABLE_MID_DEST;
			break;
		// case URI_SINGLE_ID:
		// l
		// table = MidTableDatabaseHelper.TABLE_MID_DEST;
		// break;
		// case URI_SINGLE:
		// if (uri.getPathSegments().size() < 1) {
		// throw new IllegalArgumentException(
		// "can not find mid_key for delete uri:" + uri);
		// }
		// table = MidTableDatabaseHelper.TABLE_MID_DEST;
		// break;
		default:
			return deleteWithCustomUri(uri, selection, selectionArgs);
		}
		return mOpenHelper.getWritableDatabase().delete(table, selection,
				selectionArgs);
	}

	protected int deleteWithCustomUri(Uri uri, String selection,
			String[] selectionArgs) {
		return -1;
	}

	@Override
	public final int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (V) {
			logv("update uri:" + uri);
		}
		String table;
		switch (mUriMatcher.match(uri)) {
		case URI_BASE:
			table = MidTableDatabaseHelper.TABLE_MID_DEST;
			break;
		default:
			return updateWithCustomUri(uri, values, selection, selectionArgs);
		}
		return mOpenHelper.getWritableDatabase().update(table, values,
				selection, selectionArgs);
	}

	protected int updateWithCustomUri(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {
		return -1;
	}

	@Override
	public final ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		ContentProviderResult[] reaults = null;
		try {
			db.beginTransaction();
			mIsApplyBatch=true;
			long t1 = SystemClock.currentThreadTimeMillis();
			reaults = super.applyBatch(operations);
			long t2 = SystemClock.currentThreadTimeMillis();
			if (TIME_CAL && (t2 - t1) > 2000) {
				logv("TIME_CAL totalTime:" + (t2 - t1) + " optCount:"
						+ operations.size() + " aveTime:" + (t2 - t1)
						/ operations.size());
			}
			db.setTransactionSuccessful();
			mIsApplyBatch=false;
		} finally {
			db.endTransaction();
		}

		Uri notifyUri = getNotifyUri();
		if (notifyUri != null) {
			getContext().getContentResolver()
					.notifyChange(getNotifyUri(), null);
		} else {
			logw("get NULL notify uri from getNotifyUri()");
		}
		return reaults;
	}

	protected Uri getNotifyUri() {
		return Uri.parse("content://" + mManager.getDestAuthorityName());
	}
	
	public boolean isApplyBatch(){
		return mIsApplyBatch;
	}

	private static final String PRE = "<DCP>";

	private static void logv(String msg) {
		Log.v(Mid.DEST, PRE + msg);
	}

	// private static void logd(String msg) {
	// Log.d(Mid.DEST, PRE + msg);
	// }
	//
	// private static void logi(String msg) {
	// Log.i(Mid.DEST, PRE + msg);
	// }

	private static void logw(String msg) {
		Log.w(Mid.DEST, PRE + msg);
	}

	// private static void loge(String msg) {
	// Log.e(Mid.DEST, PRE + msg);
	// }
}

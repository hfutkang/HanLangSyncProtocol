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
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncModule;

public abstract class MidSrcContentProvider extends ContentProvider implements
		ObtainSyncModule {
	private static final boolean TIME_CAL = true;
	private static final boolean V = false;

	private final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int URI_BASE = 0;

	private SQLiteOpenHelper mOpenHelper;
	private MidTableManager mManager;

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

		URI_MATCHER.addURI(mManager.getSrcAuthorityName(), null, URI_BASE);

		mOpenHelper = MidTableDatabaseHelper.getInstance(getContext(), module.getName(),
				mManager, null);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (V) {
			logv("query:" + uri);
		}
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (URI_MATCHER.match(uri)) {
		case URI_BASE:
			qb.setTables(MidTableDatabaseHelper.TABLE_MID_SRC);
			break;
		default:
			loge("unsupported uri:" + uri + " in query");
			return null;
		}
		Cursor c = qb.query(mOpenHelper.getReadableDatabase(), projection,
				selection, selectionArgs, null, null, sortOrder);
		return c;
	}

	@Override
	public final String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (V) {
			logv("insert:" + uri);
		}
		switch (URI_MATCHER.match(uri)) {
		case URI_BASE:
			long rowId = mOpenHelper.getWritableDatabase().insert(
					MidTableDatabaseHelper.TABLE_MID_SRC, null, values);
			return ContentUris.withAppendedId(uri, rowId);
		default:
			loge("unsupported uri:" + uri + " in insert");
			return null;
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (V) {
			logd("delete:" + uri);
		}
		switch (URI_MATCHER.match(uri)) {
		case URI_BASE:
			logi("Clear all mid datas");
			return mOpenHelper.getWritableDatabase().delete(
					MidTableDatabaseHelper.TABLE_MID_SRC, selection,
					selectionArgs);
		default:
			loge("unsupported uri:" + uri + " in delete");
			return -1;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (V) {
			logv("update:" + uri);
		}
		switch (URI_MATCHER.match(uri)) {
		case URI_BASE:
			return mOpenHelper.getWritableDatabase().update(
					MidTableDatabaseHelper.TABLE_MID_SRC, values, selection,
					selectionArgs);
		default:
			loge("unsupported uri:" + uri + " in update");
			return -1;
		}
	}

	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		ContentProviderResult[] reaults = null;
		try {
			db.beginTransaction();
			long t1 = SystemClock.currentThreadTimeMillis();
			reaults = super.applyBatch(operations);
			long t2 = SystemClock.currentThreadTimeMillis();
			if (TIME_CAL && (t2 - t1) > 2000) {
				logv("TIME_CAL totalTime:" + (t2 - t1) + " optCount:"
						+ operations.size() + " aveTime:" + (t2 - t1)
						/ operations.size());
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return reaults;
	}

	private static final String PRE = "<SCP>";

	private static void logv(String msg) {
		Log.v(Mid.SRC, PRE + msg);
	}

	private static void logd(String msg) {
		Log.d(Mid.SRC, PRE + msg);
	}

	private static void logi(String msg) {
		Log.i(Mid.SRC, PRE + msg);
	}
	//
	// private static void logw(String msg) {
	// Log.w(Mid.SRC, PRE + msg);
	// }

	private static void loge(String msg) {
		Log.e(Mid.SRC, PRE + msg);
	}

}

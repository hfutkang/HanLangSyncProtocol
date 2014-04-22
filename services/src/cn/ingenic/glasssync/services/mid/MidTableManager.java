package cn.ingenic.glasssync.services.mid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

public abstract class MidTableManager {
	// private static final boolean V = true;
	private static final String TAG = "MidTableManager";

	static final String DATA_KEY_MID_SYNC_FLAG = "mid_sync_flag";
	static final String DATA_KEY_KESYS = "mid_keys";
	static final String DATA_KEY_SYNCS = "mid_syncs";
	static final String DATA_KEY_EXCLUDES = "mid_excludes";
	static final String DATA_KEY_SYNC = "mid_sync";
	static final String DATA_KEY_INSERTS_P = "mid_inserts";
	static final String DATA_KEY_UPDATES_P = "mid_updates";
	static final String DATA_KEY_DELETES_P = "mid_deletes";
	static final String DATA_KEY_APPENDS_P = "mid_appends";
	static final String DATA_KEY_DATAS = "mid_datas";

	static final String DATA_KEY_TRANSACTION = "mid_transaction";
	static final int DATA_VALUE_TRANSACTION_INVALID = -1;
	static final int DATA_VALUE_TRANSACTION_REF = 1;
	static final int DATA_VALUE_TRANSACTION_REQ = 2;
	static final int DATA_VALUE_TRANSACTION_RESP = 3;

	protected final Context mContext;
	protected MidContentObserver mContentObserver = null;

	private KeyColumn mSrcKey;
	private KeyColumn mDestKey;
	private List<Column> mSrcTableList;
	private List<Column> mDestTableList;
	private DataSource mSource;
	private DataDestination mDestination;
	protected final SyncModule mModule;

	private static final int MSG_BASE = 0;
	static final int MSG_PROCESS_REF_ASYNC = MSG_BASE + 2;

	// static final int MSG_

	static class ProcessREFArg<T> {
		final Set<T> deletedKeysToCommit;
		final Set<T> otherKeysToCommit;
		final Set<T> exculdeKeys;

		ProcessREFArg(Set<T> deletes, Set<T> others, Set<T> exculdes) {
			deletedKeysToCommit = deletes;
			otherKeysToCommit = others;
			exculdeKeys = exculdes;
		}
	}

	MidTableManager(Context ctx, SyncModule module) {
		if (ctx == null || module == null) {
			throw new IllegalArgumentException("Args can not be null!");
		}

		mContext = ctx;
		mModule = module;
	}

	public final void onModuleInit() {
		Log.d(TAG, "onModuleInit");
		if (hasSource()) {
			try {
				if (mModule.hasLockedAddress()) {
					startObserve();
				}
			} catch (SyncException e) {
				Log.e(TAG, "onModuleInit:", e);
			}
		}
	}

	public final void onModuleClear(String address) {
		Log.d(TAG, "onModuleClear:" + address);
		if (hasSource()) {
			endObserve();
			mSource.getTransactionManager().processClear(address);
		}

		if (hasDest()) {
			mDestination.getTransactionManager().processCleaar(address);
		}
	}

	public final void onModuleConnectivityChange(boolean connect) {
		Log.d(TAG, "Mid connectivity change:" + connect);
		if (connect) {
			if (hasDest()) {
				try {
					mDestination.getTransactionManager().sendReflect();
				} catch (SyncException e) {
					Log.e(TAG, "onModuleConnectivityChange:", e);
				}
			}
		}
	}

	public final void executeSync() throws SyncException {
		if (mModule.isConnected()) {
			Log.v(TAG, "Already connected, give up sync.");
			return;
		}

		SyncData data = new SyncData();
		data.setConfig(new SyncData.Config(true));
		mModule.send(data);
	}

	void init(DataSource source, DataDestination dest) {
		if (source == null && dest == null) {
			throw new IllegalArgumentException("Args can not be all null!");
		}

		if (source != null) {
			mSrcTableList = source.getSrcColumnList();
			if (mSrcTableList != null) {
				KeyColumn srckey = source.getSrcKeyColumn();
				mSrcKey = (srckey == null) ? new KeyColumn(Mid.COLUMN_ID,
						Column.LONG) : srckey;
				mSource = source;
			} else {
				throw new IllegalArgumentException(
						"can not getSrcKeyColumn null from DataSource.");
			}
		} else {
			Log.w(TAG,
					"Disable source role, because of can not getMappedColumnList from DataSource");
		}

		if (dest != null) {
			mDestTableList = dest.getDestColumnList();
			if (mDestTableList != null) {
				KeyColumn destkey = dest.getDestKeyColumn();
				mDestKey = (destkey == null) ? new KeyColumn(Mid.COLUMN_KEY,
						Column.LONG) : destkey;
				mDestination = dest;
			} else {
				throw new IllegalArgumentException(
						"can not getDestColumnList null from DataDestination.");
			}
		}
	}

	protected final boolean hasSource() {
		return mSource != null;
	}

	protected final boolean hasDest() {
		return mDestination != null;
	}

	MidTableManager(Context ctx, DataSource source, DataDestination dest,
			SyncModule module) {
		this(ctx, module);
		init(source, dest);
	}

	// static Set<String> sDbSupportedTypes = new HashSet<String>();
	static Set<String> sReservedNames = new HashSet<String>();
	static Map<String, Set<Integer>> sTypesMap = new HashMap<String, Set<Integer>>();

	static {
		/*
		 * sDbSupportedTypes.add(Mid.DB_TYPE_INTEGER);
		 * sDbSupportedTypes.add(Mid.DB_TYPE_TEXT);
		 */

		sReservedNames.add(Mid.COLUMN_ID);
		sReservedNames.add(Mid.COLUMN_KEY);
		sReservedNames.add(Mid.COLUMN_SYNC);

		Set<Integer> dbInt = new HashSet<Integer>();
		dbInt.add(Column.INTEGER);
		dbInt.add(Column.LONG);

		Set<Integer> dbText = new HashSet<Integer>();
		dbText.add(Column.STRING);

		sTypesMap.put(Column.DB_TYPE_INTEGER, dbInt);
		sTypesMap.put(Column.DB_TYPE_TEXT, dbText);
	}

	final List<Column> getSrcTableList() {
		return mSrcTableList;
	}

	final List<Column> getDestTableList() {
		return mDestTableList;
	}

	final KeyColumn getSrcKey() {
		return mSrcKey;
	}

	final KeyColumn getDestKey() {
		return mDestKey;
	}

	String getSrcAuthorityName() {
		if (mSource == null) {
			Log.w(TAG,
					"Source role is not supported, return null AuthorityName");
			return null;
		}
		return mSource.getSrcAuthorityName();
	}

	String getDestAuthorityName() {
		if (mDestination == null) {
			Log.w(TAG, "Dest role is not supported, return null AuthorityName");
			return null;
		}
		return mDestination.getDestAuthorityName();
	}

	Uri getSrcUri() {
		if (mSource == null) {
			Log.w(TAG, "Source role is not supported, return null uri");
			return null;
		}
		return mSource.getSrcMidUri();
	}

	Uri getDestUri() {
		if (mDestination == null) {
			Log.w(TAG, "Dest role is not supported, return null uri");
			return null;
		}
		return mDestination.getDestMidUri();
	}

	public final void onRetriveMidSyncData(SyncData data) {
		try {
			switch (data.getInt(DATA_KEY_TRANSACTION,
					DATA_VALUE_TRANSACTION_INVALID)) {
			case DATA_VALUE_TRANSACTION_REQ:
				Log.i(TAG, "Received a transaction request.");
				if (hasDest()) {
					mDestination.getTransactionManager().processRequest(data);
				} else {
					Log.e(TAG,
							"no Dest role process DATA_VALUE_TRANSACTION_REQ.");
				}
				break;
			case DATA_VALUE_TRANSACTION_REF:
				Log.i(TAG, "Received a transaction reflect.");
				if (hasSource()) {
					mSource.getTransactionManager().processReflect(data);
				} else {
					Log.e(TAG,
							"no Source role process DATA_VALUE_TRANSACTION_REF.");
				}
				break;
			case DATA_VALUE_TRANSACTION_RESP:
				Log.i(TAG, "Received a transaction response.");
				if (hasSource()) {
					mSource.getTransactionManager().processResponse(data);
				} else {
					Log.e(TAG,
							"no Source role process DATA_VALUE_TRANSACTION_RESP.");
				}
				break;
			case DATA_VALUE_TRANSACTION_INVALID:
				Log.i(TAG,
						"No transaction code been got, it may just be a executeSync cmd.");
				break;
			default:
				Log.e(TAG,
						"unknow transaction code:"
								+ data.getInt(DATA_KEY_TRANSACTION));
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, "onRetriveMidSyncData:", e);
		}

	}

	public final void startObserve() {
		if (!hasSource()) {
			Log.e(TAG, "can not startObserve() without Source role.");
			return;
		}
		
		if (mContentObserver != null) {
			Log.v(TAG, "MidContentObserver already registed.");
			return;
		}

		Log.d(TAG, "startObserve");
		Uri[] observedUris = mSource.getSrcObservedUris();
		if (observedUris == null || observedUris.length == 0) {
			Log.e(TAG, "can not start() without observedUris");
			return;
		}

		mContentObserver = new MidContentObserver(observedUris);
		for (Uri uri : observedUris) {
			mContext.getContentResolver().registerContentObserver(uri, true,
					mContentObserver);
		}
		mSource.getTransactionManager().applyMidDiff();
	}
	
	final void endObserve() {
		if (!hasSource()) {
			Log.e(TAG, "can not endObserve() without Source role.");
			return;
		}
		
		if (mContentObserver == null) {
			Log.e(TAG, "Can not endObserve without MidContentObserver.");
			return;
		}
		
		Log.d(TAG, "endObserve");
		mContext.getContentResolver().unregisterContentObserver(mContentObserver);
		mContentObserver = null;
	}

	class MidContentObserver extends ContentObserver {

		public MidContentObserver(Uri[] observerdUris) {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		public void onChange(boolean selfChange, Uri uri) {
			Log.d(TAG, "onChange(" + selfChange + ", " + uri + ")");
			// if (uri == null) {
			if (hasSource()) {
				mSource.getTransactionManager().sendDiffSyncRequest();
			}
		}

	};

}
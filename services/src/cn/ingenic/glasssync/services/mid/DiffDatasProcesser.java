package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;

abstract class DiffDatasProcesser<T> {
	protected final Cursor mSourceCursor;
	protected final int mKeySourceIndex;
	protected final Cursor mMidCursor;
	protected final int mKeyMidIndex;

	private final DataSource mSource;
	private final MidTableManager mMidMgr;

	private final Handler mHandler;

	// source cursor position
	protected final Set<MappedArg<T>> mInsertedRecords = new HashSet<MappedArg<T>>();
	protected final Set<MappedArg<T>> mUpdatedRecords = new HashSet<MappedArg<T>>();
	// mid cursor position
	protected final Set<MappedArg<T>> mDeletedRecords = new HashSet<MappedArg<T>>();

	DiffDatasProcesser(Cursor source, Cursor mid, DataSource dataSource,
			MidTableManager mgr, Handler handler) throws MidException {
		if (source == null || mid == null) {
			throw new MidException("Args can not be null, source:" + source
					+ ", mid:" + mid);
		}

		mKeySourceIndex = source
				.getColumnIndex(mgr.getSrcKey().getMappedName());
		mKeyMidIndex = mid.getColumnIndex(mgr.getSrcKey().getName());

		if (mKeySourceIndex < 0 || mKeySourceIndex < 0) {
			throw new MidException("No keys cloumn found in db.");
		}

		mSource = dataSource;
		mSourceCursor = source;
		mMidCursor = mid;
		mMidMgr = mgr;

		mHandler = handler;
	}

	abstract T getKey(Cursor source);

	abstract T getKeyFromMid(Cursor mid);

	abstract void appendKey(SyncData data, T t);

	abstract void putData(Cursor mid, Map<T, Integer> map);

	void dump() {
		logi("insertedRecords.size:" + mInsertedRecords.size());
		logi("updatedRecords.size:" + mUpdatedRecords.size());
		logi("deletedRecords.size:" + mDeletedRecords.size());
	}

	boolean fillDiffDatasPosition() throws MidException {
		if (mSourceCursor.getCount() <= 0) {
			logi("query 0 size datas from Source Cursor.");
			if (mMidCursor.getCount() > 0) {
				if (!mMidCursor.moveToFirst()) {
					throw new MidException(
							"can not moveToFirst of mid cursor size:"
									+ mMidCursor.getCount());
				}

				do {
					mDeletedRecords.add(new MappedArg<T>(mMidCursor
							.getPosition(), getKeyFromMid(mMidCursor),
							Mid.VALUE_SYNC_DELETED));
				} while (mMidCursor.moveToNext());
			} else {
				return false;
			}
			return true;
		}

		if (!mSourceCursor.moveToFirst()) {
			throw new MidException(
					"can not moveToFirst of mmmSourceCursor cursor size:"
							+ mSourceCursor.getCount());
		}

		HashMap<T, Integer> map = new HashMap<T, Integer>();
		if (mMidCursor.getCount() > 0) {
			if (!mMidCursor.moveToFirst()) {
				throw new MidException(
						"can not moveToFirst of mmmMidCursor cursor size:"
								+ mMidCursor.getCount());
			}

			do {
				putData(mMidCursor, map);
			} while (mMidCursor.moveToNext());

			do {
				T key = getKey(mSourceCursor);
				Integer position = map.remove(key);
				if (position == null) {
					mInsertedRecords.add(new MappedArg<T>(mSourceCursor
							.getPosition(), key, Mid.VALUE_SYNC_INSERTED));
					continue;
				}

				if (!mMidCursor.moveToPosition(position)) {
					throw new MidException("mmmMidCursor.move(" + position
							+ ") return false.");
				}
				int sync = mMidCursor.getInt(mMidCursor
						.getColumnIndex(Mid.COLUMN_SYNC));
				if (sync == -1) {
					mUpdatedRecords.add(new MappedArg<T>(mSourceCursor
							.getPosition(), key, Mid.VALUE_SYNC_INSERTED));
					continue;
				}

				if (!mSource.isSrcMatch(mSourceCursor, mMidCursor)) {
					mUpdatedRecords.add(new MappedArg<T>(mSourceCursor
							.getPosition(), key,
							(sync > Mid.VALUE_SYNC_INSERTED) ? sync + 1
									: Mid.VALUE_SYNC_UPDATED));

				}
			} while (mSourceCursor.moveToNext());

			if (!map.isEmpty()) {
				for (Entry<T, Integer> entry : map.entrySet()) {
					mDeletedRecords.add(new MappedArg<T>(entry.getValue(),
							entry.getKey(), Mid.VALUE_SYNC_DELETED));
				}
			}
		} else {
			do {
				mInsertedRecords.add(new MappedArg<T>(mSourceCursor
						.getPosition(), getKey(mSourceCursor),
						Mid.VALUE_SYNC_INSERTED));
			} while (mSourceCursor.moveToNext());
		}

		return (mInsertedRecords.size() + mDeletedRecords.size() + mUpdatedRecords
				.size()) > 0;
	}

	SyncData createSendDatas() throws MidException {
		SyncData totalData = new SyncData();
		List<SyncData> datas = new ArrayList<SyncData>();
		Map<T, Integer> syncMap = new HashMap<T, Integer>();

		for (MappedArg<T> arg : mDeletedRecords) {
			int i = arg.sourcePos;
			if (!mMidCursor.moveToPosition(i)) {
				throw new MidException("midCursor can not moveToPosition:" + i);
			}
			SyncData data = new SyncData();
			MidTools.fillSyncData(data, mMidCursor, mMidMgr.getSrcKey());
			data.putInt(MidTableManager.DATA_KEY_SYNC, arg.sync);
			datas.add(data);

			syncMap.put(arg.key, arg.sync);
		}

		Set<Integer> positons = new HashSet<Integer>();

		for (MappedArg<T> arg : mUpdatedRecords) {
			int i = arg.sourcePos;
			if (!mSourceCursor.moveToPosition(i)) {
				throw new MidException("sourceCursor can not moveToPosition:"
						+ i);
			}
			SyncData data = new SyncData();
			mSource.fillSrcSyncData(data, mSourceCursor);
			appendKey(data, arg.key);
			data.putInt(MidTableManager.DATA_KEY_SYNC, arg.sync);
			datas.add(data);

			syncMap.put(arg.key, arg.sync);
			positons.add(i);
		}

		for (MappedArg<T> arg : mInsertedRecords) {
			int i = arg.sourcePos;
			if (!mSourceCursor.moveToPosition(i)) {
				throw new MidException("sourceCursor can not moveToPosition:"
						+ i);
			}
			SyncData data = new SyncData();
			mSource.fillSrcSyncData(data, mSourceCursor);
			appendKey(data, arg.key);
			data.putInt(MidTableManager.DATA_KEY_SYNC, arg.sync);
			datas.add(data);

			syncMap.put(arg.key, arg.sync);
			positons.add(i);
		}

		// totalData.putLong(MidTableManager.DATA_KEY_TID, mTid);
		int position = mDeletedRecords.size() - 1;
		totalData.putInt(MidTableManager.DATA_KEY_DELETES_P, position);
		position += mUpdatedRecords.size();
		totalData.putInt(MidTableManager.DATA_KEY_UPDATES_P, position);
		position += mInsertedRecords.size();
		totalData.putInt(MidTableManager.DATA_KEY_INSERTS_P, position);

		SyncData[] appendDatas = mSource.appendSrcSyncData(positons,
				mSourceCursor);
		int appendP = 0;
		if (appendDatas != null) {
			for (SyncData data : appendDatas) {
				datas.add(data);
			}
			appendP = appendDatas.length;
		}
		totalData
				.putInt(MidTableManager.DATA_KEY_APPENDS_P, position + appendP);

		SyncData[] array = new SyncData[datas.size()];
		totalData.putDataArray(MidTableManager.DATA_KEY_DATAS,
				(SyncData[]) (datas.toArray(array)));

		SyncData.Config config = new SyncData.Config(true);
		config.mmCallback = mHandler.obtainMessage();
		config.mmCallback.obj = syncMap;
		totalData.setConfig(config);
		totalData.putInt(MidTableManager.DATA_KEY_TRANSACTION,
				MidTableManager.DATA_VALUE_TRANSACTION_REQ);
		return totalData;
	}

	void sendDiffDatas() throws MidException, SyncException {
		if (mMidMgr.mModule.isConnected()  && mMidMgr.mModule.getSyncEnable()) {
			mMidMgr.mModule.send(createSendDatas());
		} else {
			logi("Do not send diff datas without connectivity or syncEable is false, executeSync only.");
			mMidMgr.executeSync();
		}
	}

	void applyChanges() throws MidException, RemoteException,
			OperationApplicationException {
		if ((mInsertedRecords.size() + mUpdatedRecords.size() + mDeletedRecords
				.size()) > 0) {
			// apply changes
			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
			for (MappedArg<T> arg : mDeletedRecords) {
				int p = arg.sourcePos;
				if (!mMidCursor.moveToPosition(p)) {
					throw new MidException("move to position:" + p + " failed.");
				}

				operations.add(ContentProviderOperation
						.newUpdate(mMidMgr.getSrcUri())
						.withValue(Mid.COLUMN_SYNC, Mid.VALUE_SYNC_DELETED)
						.withSelection(
								mMidMgr.getSrcKey().getName()
										+ "='"
										+ MidTools.getValue(mMidCursor,
												mMidMgr.getSrcKey()) + "'",
								null).build());
			}

			for (MappedArg<T> arg : mUpdatedRecords) {
				int p = arg.sourcePos;
				if (!mSourceCursor.moveToPosition(p)) {
					throw new MidException("move to position:" + p + " failed.");
				}

				operations.add(ContentProviderOperation
						.newUpdate(mMidMgr.getSrcUri())
						.withValues(
								MidTools.getValues(mSourceCursor,
										mMidMgr.getSrcTableList()))
						.withValue(Mid.COLUMN_SYNC, arg.sync)
						.withSelection(
								mMidMgr.getSrcKey().getName()
										+ "='"
										+ MidTools.getKeyValue(mSourceCursor,
												mMidMgr.getSrcKey(), false)
										+ "'", null).build());
			}

			for (MappedArg<T> arg : mInsertedRecords) {
				int p = arg.sourcePos;
				if (!mSourceCursor.moveToPosition(p)) {
					throw new MidException("move to position:" + p + " failed.");
				}
				operations.add(ContentProviderOperation
						.newInsert(mMidMgr.getSrcUri())
						.withValues(
								MidTools.getValues(mSourceCursor,
										mMidMgr.getSrcTableList()))
						.withValue(
								mMidMgr.getSrcKey().getName(),
								MidTools.getKeyValue(mSourceCursor,
										mMidMgr.getSrcKey(), false))
						.withValue(Mid.COLUMN_SYNC, Mid.VALUE_SYNC_INSERTED)
						.build());
			}

			mMidMgr.mContext.getContentResolver().applyBatch(
					mMidMgr.getSrcAuthorityName(), operations);
			logd("apply patch over");
		}
	}

	private static final String PRE = "<SDIFF>";

	// private static void logv(String msg) {
	// Log.v(Mid.SRC, PRE + msg);
	// }

	private static void logd(String msg) {
		Log.d(Mid.SRC, PRE + msg);
	}

	private static void logi(String msg) {
		Log.i(Mid.SRC, PRE + msg);
	}

	// private static void logw(String msg) {
	// Log.w(Mid.SRC, PRE + msg);
	// }
	//
	// private static void loge(String msg) {
	// Log.e(Mid.SRC, PRE + msg);
	// }
}

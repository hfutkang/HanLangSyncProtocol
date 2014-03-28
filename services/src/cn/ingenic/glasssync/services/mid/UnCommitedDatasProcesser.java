package cn.ingenic.glasssync.services.mid;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

abstract class UnCommitedDatasProcesser<T> extends DiffDatasProcesser<T> {
	private final Map<T, Integer> mExclude;

	UnCommitedDatasProcesser(Cursor source, Cursor mid, DataSource dataSource,
			MidTableManager mgr, Handler hanlder, Map<T, Integer> exclude)
			throws MidException {
		super(source, mid, dataSource, mgr, hanlder);
		mExclude = exclude;
	}

	@Override
	boolean fillDiffDatasPosition() throws MidException {
		if (mMidCursor.getCount() <= 0) {
			loge("No datas found in MidCursor, give up Mid sync.");
			return false;
		}

		/*
		 * if (mSourceCursor.getCount() <= 0) { Log.w(TAG,
		 * "query 0 size datas from Source Cursor, wait for next sync action.");
		 * return false; }
		 */

		if (!mMidCursor.moveToFirst()) {
			throw new MidException("can not moveToFirst of Cursor");
		}

		HashMap<T, Integer> map = new HashMap<T, Integer>();
		do {
			putData(mMidCursor, map);
		} while (mMidCursor.moveToNext());

		if (mSourceCursor.getCount() > 0 && mSourceCursor.moveToFirst()) {
			do {
				T key = getKey(mSourceCursor);

				Integer position = map.remove(key);
				if (position == null) {
					continue;
				}

				if (!mMidCursor.moveToPosition(position)) {
					throw new MidException("MidCursor.move(" + position
							+ ") return false.");
				}
				int sync = mMidCursor.getInt(mMidCursor
						.getColumnIndex(Mid.COLUMN_SYNC));

				if (mExclude.containsKey(key) && sync == mExclude.get(key)) {
					logv("Sent datas hits with key:" + key
							+ " which is processing at dest.");
					continue;
				}

				if (sync >= Mid.VALUE_SYNC_UPDATED) {
					mUpdatedRecords.add(new MappedArg<T>(mSourceCursor
							.getPosition(), key, sync));
				} else if (sync == Mid.VALUE_SYNC_INSERTED) {
					mInsertedRecords.add(new MappedArg<T>(mSourceCursor
							.getPosition(), key, sync));
				} else {
					logw("Invalid sync:" + sync + " found here.");
				}
			} while (mSourceCursor.moveToNext());
		} else {
			logw(
					"No source datas found, just do mid sync depending on datas in Mid.");
		}

		if (!map.isEmpty()) {
			for (Entry<T, Integer> entry : map.entrySet()) {
				int position = entry.getValue();
				if (!mMidCursor.moveToPosition(position)) {
					throw new MidException("MidCursor.move(" + position
							+ ") return false.");
				}
				int sync = mMidCursor.getInt(mMidCursor
						.getColumnIndex(Mid.COLUMN_SYNC));
				if (sync == Mid.VALUE_SYNC_DELETED) {
					mDeletedRecords.add(new MappedArg<T>(entry.getValue(),
							entry.getKey(), Mid.VALUE_SYNC_DELETED));
				} else {
					logw(
							"missing a row of mid data key:" + entry.getKey()
									+ " sync:" + sync + ", wait for next sync.");
				}
			}
		}

		return (mInsertedRecords.size() + mDeletedRecords.size() + mUpdatedRecords
				.size()) > 0;
	}

	@Override
	void applyChanges() throws MidException, RemoteException,
			OperationApplicationException {
	}

	@Override
	SyncData createSendDatas() throws MidException {
		SyncData totalData = super.createSendDatas();
		totalData.putBoolean(MidTableManager.DATA_KEY_MID_SYNC_FLAG, true);
		return totalData;
	}
	
	private static final String PRE = "<SUCDIFF>";
	private static void logv(String msg) {
		Log.v(Mid.SRC, PRE + msg);
	}
	
	private static void logw(String msg) {
		Log.w(Mid.SRC, PRE + msg);
	}
	
	private static void loge(String msg) {
		Log.e(Mid.SRC, PRE + msg);
	}
}

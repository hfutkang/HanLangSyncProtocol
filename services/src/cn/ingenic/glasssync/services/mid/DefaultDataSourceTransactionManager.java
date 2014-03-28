package cn.ingenic.glasssync.services.mid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.DataSource.SrcTransactionManager;

public class DefaultDataSourceTransactionManager<T> implements
		SrcTransactionManager {
	private final MidTableManager mMidMgr;
	private final DataSource mSource;
	private final Handler mAsyncHandler;
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			processRequestSent(msg.arg1 == SyncModule.SUCCESS, msg.obj);
		}

	};

	private class MyHandler extends Handler {

		public MyHandler(Looper looper) {
			super(looper);
		}
	}

	DefaultDataSourceTransactionManager(MidTableManager mgr, DataSource source) {
		mMidMgr = mgr;
		mSource = source;
		// mBaseId = System.currentTimeMillis();
		// mLastId = mBaseId;
		// mPool = new TransactionPool<T>();
		HandlerThread ht = new HandlerThread("DefaultDataSourceTransactionManager");
		ht.setPriority(Thread.MAX_PRIORITY - 1);
		ht.start();
		mAsyncHandler = new MyHandler(ht.getLooper());
	}

	@Override
	public void processResponse(SyncData data) {
		logd("processResponse");
		final Map<T, Integer> syncs = new HashMap<T, Integer>();
		fillSyncMapAndExclude(data, syncs, null);
		if (!syncs.isEmpty()) {
			mAsyncHandler.post(new Runnable() {

				@Override
				public void run() {
					doCommitAsync(syncs, new HashSet<T>());
				}

			});
		} else {
			logw("received empty sync values from Response");
		}
	}

	// param excludes should never be null.
	private void doCommitAsync(Map<T, Integer> syncMap, Set<T> excludes) {
		if (syncMap != null && !syncMap.isEmpty()) {
			Set<T> all = syncMap.keySet();
			Cursor midC = null;
			try {
				String selection = getSelection(all);
				midC = mMidMgr.mContext.getContentResolver().query(
						mMidMgr.getSrcUri(),
						new String[] { Mid.COLUMN_KEY, Mid.COLUMN_SYNC },
						selection, null, null);
				if (midC != null && midC.getCount() > 0) {
					if (midC.moveToFirst()) {
						Set<T> deletes = new HashSet<T>();
						Set<T> updates = new HashSet<T>();
						do {
							int sync = midC.getInt(midC
									.getColumnIndex(Mid.COLUMN_SYNC));
							T key = (T) MidTools.getKeyValue(midC,
									mMidMgr.getSrcKey(), true);
							if (excludes.contains(key)) {
								logd("Data:"
										+ key
										+ " is processing at dest, do not commit its sync.");
								continue;
							}
							int receivedSync = syncMap.get(key);
							if (sync == receivedSync) {
								if (sync == Mid.VALUE_SYNC_DELETED) {
									deletes.add(key);
								} else {
									updates.add(key);
								}
							} else {
								if (receivedSync == Mid.VALUE_SYNC_DELETED) {
									logw("Received sync value(DELETED) with local sync value:"
											+ sync);
								} else if (receivedSync > sync) {
									logw("Received sync value is newer than local sync value:"
											+ sync);
								}
							}
						} while (midC.moveToNext());

						if (!deletes.isEmpty()) {
							mMidMgr.mContext.getContentResolver().delete(
									mMidMgr.getSrcUri(), getSelection(deletes),
									null);
						}

						if (!updates.isEmpty()) {
							ContentValues cv = new ContentValues();
							cv.put(Mid.COLUMN_SYNC, Mid.VALUE_SYNC_SUCCESS);
							mMidMgr.mContext.getContentResolver().update(
									mMidMgr.getSrcUri(), cv,
									getSelection(updates), null);
						}
					} else {
						loge("can not moveToFirst");
					}
				}
			} catch (Exception e) {
				loge("Exception:", e);
			} finally {
				if (midC != null) {
					midC.close();
				}
			}
		} else {
			logi("nothing to Commit.");
		}
	}

	@Override
	public void processRequestSent(boolean success, Object obj) {
		logi("Request sent:" + (success ? "succeed" : "failed"));
		// Map<T, Integer> syncMap = (Map<T, Integer>) obj;
		// dumpMapData(syncMap);
	}

	// private void dumpMapData(Map<T, Integer> syncMap) {
	// if (syncMap == null || syncMap.isEmpty()) {
	// Log.w(TAG, "Empty syncMap to dump.");
	// return;
	// }
	//
	// StringBuilder sb = new StringBuilder();
	// for (T t : syncMap.keySet()) {
	// sb.append("key:").append(t).append(" sync:").append(syncMap.get(t))
	// .append("\n");
	// }
	// Log.v(TAG, sb.toString());
	// }

	void fillSyncMapAndExclude(final SyncData data, Map<T, Integer> syncMap,
			Set<T> exclude) {
		if (syncMap == null) {
			logw("can not fill into null map");
			return;
		}
		try {
			int[] syncs = data.getIntArray(MidTableManager.DATA_KEY_SYNCS);
			if (syncs != null && syncs.length > 0) {
				switch (mMidMgr.getSrcKey().getType()) {
				case Column.INTEGER: {
					int[] keys = data
							.getIntArray(MidTableManager.DATA_KEY_KESYS);
					if (keys == null || keys.length != syncs.length) {
						throw new MidException(
								"Datas in keys and sync not match");
					}

					for (int i = 0; i < keys.length; i++) {
						syncMap.put((T) Integer.valueOf(keys[i]), syncs[i]);
					}

					if (exclude != null) {
						int[] excludes = data
								.getIntArray(MidTableManager.DATA_KEY_EXCLUDES);
						if (excludes != null && excludes.length > 0) {
							for (int i = 0; i < excludes.length; i++) {
								exclude.add((T) Integer.valueOf(excludes[i]));
							}
						}
					}
					break;
				}
				case Column.LONG: {
					long[] keys = data
							.getLongArray(MidTableManager.DATA_KEY_KESYS);
					if (keys == null || keys.length != syncs.length) {
						throw new MidException(
								"Datas in keys and sync not match");
					}

					for (int i = 0; i < keys.length; i++) {
						syncMap.put((T) Long.valueOf(keys[i]), syncs[i]);
					}

					if (exclude != null) {
						long[] excludes = data
								.getLongArray(MidTableManager.DATA_KEY_EXCLUDES);
						if (excludes != null && excludes.length > 0) {
							for (int i = 0; i < excludes.length; i++) {
								exclude.add((T) Long.valueOf(excludes[i]));
							}
						}
					}
					break;
				}
				case Column.STRING: {
					String[] keys = data
							.getStringArray(MidTableManager.DATA_KEY_KESYS);
					if (keys == null || keys.length != syncs.length) {
						throw new MidException(
								"Datas in keys and sync not match");
					}

					for (int i = 0; i < keys.length; i++) {
						syncMap.put((T) keys[i], syncs[i]);
					}

					if (exclude != null) {
						String[] excludes = data
								.getStringArray(MidTableManager.DATA_KEY_EXCLUDES);
						if (excludes != null && excludes.length > 0) {
							for (int i = 0; i < excludes.length; i++) {
								exclude.add((T) excludes[i]);
							}
						}
					}
					break;
				}
				default:
					throw new MidException("Invalid key type:"
							+ mMidMgr.getSrcKey().getType());
				}
			}
		} catch (MidException e) {
			loge("Exception:", e);
		}
	}

	@Override
	public void processReflect(SyncData data) {
		logd("processReflect");
		final Map<T, Integer> syncs = new HashMap<T, Integer>();
		final Set<T> excludes = new HashSet<T>();
		fillSyncMapAndExclude(data, syncs, excludes);
		mAsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				doCommitAsync(syncs, excludes);
				doMidSyncAsync(syncs, excludes);
			}

		});
	}

	private void doMidSyncAsync(Map<T, Integer> syncs, Set<T> excludes) {
		Cursor midC = null;
		Cursor c = null;
		try {
			StringBuilder sb = new StringBuilder(Mid.COLUMN_SYNC + "!="
					+ Mid.VALUE_SYNC_SUCCESS);
			// if (syncs != null && !syncs.isEmpty()) {
			// sb.append(" OR ");
			// sb.append(getSelection(syncs.keySet()));
			// }
			logv("Selection for query old uncommited datas:" + sb.toString());
			midC = mMidMgr.mContext.getContentResolver().query(
					mMidMgr.getSrcUri(), null, sb.toString(), null, null);
			if (midC == null || midC.getCount() <= 0) {
				logi("No old uncommited datas found.");
				return;
			}

			if (!midC.moveToFirst()) {
				throw new MidException("can not move to First");
			}

			int keyMidIndex = midC
					.getColumnIndex(mMidMgr.getSrcKey().getName());
			UnCommitedDatasProcesser<?> processer;
			HashMap<T, Integer> excludeMap = new HashMap<T, Integer>();
			for (T i : excludes) {
				excludeMap.put(i, syncs.get(i));
			}
			switch (mMidMgr.getSrcKey().getType()) {
			case Column.INTEGER: {
				Set<Integer> keys = new HashSet<Integer>();
				do {
					keys.add(midC.getInt(keyMidIndex));
				} while (midC.moveToNext());

				c = mSource.getSrcDataCursor(keys);
				processer = new UnCommitedDiffDataForInteger(c, midC,
						(Map<Integer, Integer>) excludeMap);
				break;
			}
			case Column.LONG: {
				Set<Long> keys = new HashSet<Long>();
				do {
					keys.add(midC.getLong(keyMidIndex));
				} while (midC.moveToNext());

				c = mSource.getSrcDataCursor(keys);
				processer = new UnCommitedDiffDataForLong(c, midC,
						(Map<Long, Integer>) excludeMap);
				break;
			}
			case Column.STRING: {
				Set<String> keys = new HashSet<String>();
				do {
					keys.add(midC.getString(keyMidIndex));
				} while (midC.moveToNext());

				c = mSource.getSrcDataCursor(keys);
				processer = new UnCommitedDiffDataForString(c, midC,
						(Map<String, Integer>) excludeMap);
				break;
			}
			default:
				throw new MidException("not supported KeyColumn type:"
						+ mMidMgr.getSrcKey().getType());
			}

			if (processer.fillDiffDatasPosition()) {
				processer.dump();
				// We only find uncommited datas to send, so no datas to apply
				// processer.applyChanges();
				processer.sendDiffDatas();
			}
		} catch (Exception e) {
			loge("Exception:", e);
		} finally {
			if (midC != null) {
				midC.close();
			}

			if (c != null) {
				c.close();
			}
		}
	}

	private String getSelection(Set<T> keys) {
		StringBuilder sb = new StringBuilder(mMidMgr.getSrcKey().getName()
				+ " IN (");
		boolean first = true;
		for (Object o : keys) {
			if (first) {
				sb.append("'").append(o).append("'");
				first = false;
			} else {
				sb.append(",").append("'").append(o).append("'");
			}
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public void applyMidDiff() {
		logd("applyMidDiff");
		mAsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				doDiffSyncAsync(false);
			}
			
		});
	}

	@Override
	public void sendDiffSyncRequest() {
		logd("sendDiffSyncRequest");
		mAsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				doDiffSyncAsync(true);
			}

		});

	}
	
	@Override
	public void processClear(String address) {
		logd("processClear");
		mAsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				mMidMgr.mContext.getContentResolver().delete(mSource.getSrcMidUri(), null, null);
			}
			
		});
	}

	private Cursor getMidDataCursor() {
		return mMidMgr.mContext.getContentResolver().query(mMidMgr.getSrcUri(),
				null, null, null, null);
	}

	private void doDiffSyncAsync(boolean send) {
		Cursor c = mSource.getSrcDataCursor(null);
		Cursor midC = getMidDataCursor();
		try {
			DiffDatasProcesser<?> processer;
			switch (mMidMgr.getSrcKey().getType()) {
			case Column.INTEGER:
				processer = new DiffDataForInteger(c, midC);
				break;
			case Column.LONG:
				processer = new DiffDataForLong(c, midC);
				break;
			case Column.STRING:
				processer = new DiffDataForString(c, midC);
				break;
			default:
				throw new MidException("not supported KeyColumn type:"
						+ mMidMgr.getSrcKey().getType());
			}

			if (processer.fillDiffDatasPosition()) {
				processer.dump();
				processer.applyChanges();
				if (send) {
					processer.sendDiffDatas();
				}
			}
		} catch (Exception e) {
			loge("Exception:", e);
		} finally {
			if (c != null) {
				c.close();
			}

			if (midC != null) {
				midC.close();
			}
		}
	}

	private class DiffDataForInteger extends DiffDatasProcesser<Integer> {

		DiffDataForInteger(Cursor source, Cursor mid) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler);
		}

		@Override
		Integer getKey(Cursor source) {
			return source.getInt(mKeySourceIndex);
		}

		@Override
		Integer getKeyFromMid(Cursor mid) {
			return mid.getInt(mKeyMidIndex);
		}

		@Override
		void appendKey(SyncData data, Integer t) {
			data.putInt(Mid.COLUMN_KEY, t);
		}

		@Override
		void putData(Cursor mid, Map<Integer, Integer> map) {
			map.put(mid.getInt(mKeyMidIndex), mid.getPosition());
		}

	}

	private class DiffDataForLong extends DiffDatasProcesser<Long> {

		DiffDataForLong(Cursor source, Cursor mid) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler);
		}

		@Override
		void putData(Cursor mid, Map<Long, Integer> map) {
			map.put(mid.getLong(mKeyMidIndex), mid.getPosition());
		}

		@Override
		Long getKey(Cursor source) {
			return source.getLong(mKeySourceIndex);
		}

		@Override
		void appendKey(SyncData data, Long t) {
			data.putLong(Mid.COLUMN_KEY, t);
		}

		@Override
		Long getKeyFromMid(Cursor mid) {
			return mid.getLong(mKeyMidIndex);
		}

	}

	private class DiffDataForString extends DiffDatasProcesser<String> {

		DiffDataForString(Cursor source, Cursor mid) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler);
		}

		@Override
		void putData(Cursor mid, Map<String, Integer> map) {
			map.put(mid.getString(mKeyMidIndex), mid.getPosition());
		}

		@Override
		String getKey(Cursor source) {
			return source.getString(mKeySourceIndex);
		}

		@Override
		void appendKey(SyncData data, String t) {
			data.putString(Mid.COLUMN_KEY, t);
		}

		@Override
		String getKeyFromMid(Cursor mid) {
			return mid.getString(mKeyMidIndex);
		}

	}

	private class UnCommitedDiffDataForInteger extends
			UnCommitedDatasProcesser<Integer> {

		UnCommitedDiffDataForInteger(Cursor source, Cursor mid,
				Map<Integer, Integer> exclude) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler, exclude);
		}

		@Override
		Integer getKey(Cursor source) {
			return source.getInt(mKeySourceIndex);
		}

		@Override
		Integer getKeyFromMid(Cursor mid) {
			return mid.getInt(mKeyMidIndex);
		}

		@Override
		void appendKey(SyncData data, Integer t) {
			data.putInt(Mid.COLUMN_KEY, t);
		}

		@Override
		void putData(Cursor mid, Map<Integer, Integer> map) {
			map.put(mid.getInt(mKeyMidIndex), mid.getPosition());
		}

	}

	private class UnCommitedDiffDataForLong extends
			UnCommitedDatasProcesser<Long> {

		UnCommitedDiffDataForLong(Cursor source, Cursor mid,
				Map<Long, Integer> exclude) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler, exclude);
		}

		@Override
		void putData(Cursor mid, Map<Long, Integer> map) {
			map.put(mid.getLong(mKeyMidIndex), mid.getPosition());
		}

		@Override
		Long getKey(Cursor source) {
			return source.getLong(mKeySourceIndex);
		}

		@Override
		void appendKey(SyncData data, Long t) {
			data.putLong(Mid.COLUMN_KEY, t);
		}

		@Override
		Long getKeyFromMid(Cursor mid) {
			return mid.getLong(mKeyMidIndex);
		}
	}

	private class UnCommitedDiffDataForString extends
			UnCommitedDatasProcesser<String> {

		UnCommitedDiffDataForString(Cursor source, Cursor mid,
				Map<String, Integer> exclude) throws MidException {
			super(source, mid, mSource, mMidMgr, mHandler, exclude);
		}

		@Override
		void putData(Cursor mid, Map<String, Integer> map) {
			map.put(mid.getString(mKeyMidIndex), mid.getPosition());
		}

		@Override
		String getKey(Cursor source) {
			return source.getString(mKeySourceIndex);
		}

		@Override
		void appendKey(SyncData data, String t) {
			data.putString(Mid.COLUMN_KEY, t);
		}

		@Override
		String getKeyFromMid(Cursor mid) {
			return mid.getString(mKeyMidIndex);
		}

	}

	private static final String PRE = "<STM>";

	private static void logv(String msg) {
		Log.v(Mid.SRC, PRE + msg);
	}

	private static void logd(String msg) {
		Log.d(Mid.SRC, PRE + msg);
	}

	private static void logi(String msg) {
		Log.i(Mid.SRC, PRE + msg);
	}

	private static void logw(String msg) {
		Log.w(Mid.SRC, PRE + msg);
	}

	private static void loge(String msg) {
		Log.e(Mid.SRC, PRE + msg);
	}

	private static void loge(String msg, Throwable t) {
		Log.e(Mid.SRC, PRE + msg, t);
	}
}
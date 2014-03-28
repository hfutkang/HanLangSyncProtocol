package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.DataDestination.DestTransactionManager;

public class DefaultDataDestTransactionManager implements
		DestTransactionManager {
	private static final boolean V = false;

	private final Handler mAsyncHandler;
	private final MidTableManager mMidMgr;
	private final DataDestination mDest;
	private final SyncStatePool mPool = new SyncStatePool();
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			processRespSent(msg.arg1 == SyncModule.SUCCESS, msg.obj);
		}

	};

	private static class HandlerArg {
		final boolean isREF;
		final Map<Object, Integer> syncMap;

		HandlerArg(boolean isREF, Map<Object, Integer> syncMap) {
			this.isREF = isREF;
			this.syncMap = syncMap;
		}
	}

	private static class SyncState {
		static final int PROCESSING = 0;
		static final int COMPLETE = 1;
		final int sync;
		int status = PROCESSING;

		SyncState(int sync) {
			this.sync = sync;
		}
	}

	private static class SyncStatePool {
		private final Map<Object, SyncState> mmSyncStatePool = new HashMap<Object, SyncState>();

		boolean isEmpty() {
			return mmSyncStatePool.isEmpty();
		}

		void fillCompleteMapAndProcessingSet(Map<Object, Integer> syncMap,
				Set<Object> processing) {
			if (syncMap == null || processing == null) {
				loge("null map or set, so nothing be filled.");
				return;
			}
			for (Object o : mmSyncStatePool.keySet()) {
				SyncState syncInPool = mmSyncStatePool.get(o);
				syncMap.put(o, syncInPool.sync);
				if (syncInPool.status == SyncState.PROCESSING) {
					processing.add(o);
				} else {
					loge("unknow sync status:" + syncInPool.status);
				}
			}
		}

		void putAll(Map<Object, Integer> syncMap) {
			for (Object o : syncMap.keySet()) {
				int sync = syncMap.get(o);
				if (mmSyncStatePool.containsKey(o)
						&& mmSyncStatePool.get(o).sync == sync) {
					logw("I think this should't happen, or some strategy of prevention is need.");
				} else {
					mmSyncStatePool.put(o, new SyncState(sync));
				}
			}
		}

		void remove(Map<Object, Integer> syncMap) {
			if (syncMap == null || syncMap.isEmpty()) {
				logw("nothing to remove");
				return;
			}
			Set<Object> rm = new HashSet<Object>();
			for (Object o : syncMap.keySet()) {
				if (mmSyncStatePool.containsKey(o)) {
					int sync = syncMap.get(o);
					SyncState syncInPool = mmSyncStatePool.get(o);
					if (sync == syncInPool.sync) {
						rm.add(o);
					}
				} else {
					logw("SyncStatePool missing the data:" + o + " in remove");
				}
			}

			for (Object o : rm) {
				mmSyncStatePool.remove(o);
			}
		}

		void signComplete(Map<Object, Integer> syncMap) {
			if (syncMap == null || syncMap.isEmpty()) {
				logw("nothing to sign");
				return;
			}
			for (Object o : syncMap.keySet()) {
				if (mmSyncStatePool.containsKey(o)) {
					int sync = syncMap.get(o);
					SyncState syncInPool = mmSyncStatePool.get(o);
					if (sync == syncInPool.sync) {
						syncInPool.status = SyncState.COMPLETE;
					}
				} else {
					logw("SyncStatePool missing the data:" + o
							+ " in signComplete");
				}
			}
		}
	}

	DefaultDataDestTransactionManager(MidTableManager mgr, DataDestination dest) {
		mMidMgr = mgr;
		mDest = dest;
		HandlerThread ht = new HandlerThread(
				"DefaultDataDestTransactionManager");
		ht.setPriority(Thread.MAX_PRIORITY - 1);
		ht.start();
		mAsyncHandler = new Handler(ht.getLooper());

	}

	@Override
	public void processCleaar(String address) {
		mAsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				mDest.onDatasClear();
			}
			
		});
	}

	@Override
	public void sendReflect() throws SyncException {
		SyncData data = new SyncData();
		data.putInt(MidTableManager.DATA_KEY_TRANSACTION,
				MidTableManager.DATA_VALUE_TRANSACTION_REF);

		Map<Object, Integer> syncMap = null;
		Set<Object> processing = null;
		try {
			if (!mPool.isEmpty()) {
				syncMap = new HashMap<Object, Integer>();
				processing = new HashSet<Object>();
				mPool.fillCompleteMapAndProcessingSet(syncMap, processing);
				int[] syncs = new int[syncMap.size()];
				switch (mMidMgr.getDestKey().getType()) {
				case Column.INTEGER: {
					int[] keys = new int[syncMap.size()];
					int index = 0;
					for (Object o : syncMap.keySet()) {
						keys[index] = (Integer) o;
						syncs[index] = syncMap.get(o);
						index++;
					}
					data.putIntArray(MidTableManager.DATA_KEY_KESYS, keys);
					int[] excludes = new int[processing.size()];
					index = 0;
					for (Object o : processing) {
						excludes[index++] = (Integer) o;
					}
					data.putIntArray(MidTableManager.DATA_KEY_EXCLUDES,
							excludes);
					break;
				}
				case Column.LONG: {
					long[] keys = new long[syncMap.size()];
					int index = 0;
					for (Object o : syncMap.keySet()) {
						keys[index] = (Long) o;
						syncs[index] = syncMap.get(o);
						index++;
					}
					data.putLongArray(MidTableManager.DATA_KEY_KESYS, keys);
					long[] excludes = new long[processing.size()];
					index = 0;
					for (Object o : processing) {
						excludes[index++] = (Long) o;
					}
					data.putLongArray(MidTableManager.DATA_KEY_EXCLUDES,
							excludes);
					break;
				}
				case Column.STRING: {
					String[] keys = new String[syncMap.size()];
					int index = 0;
					for (Object o : syncMap.keySet()) {
						keys[index] = (String) o;
						syncs[index] = syncMap.get(o);
						index++;
					}
					data.putStringArray(MidTableManager.DATA_KEY_KESYS, keys);
					String[] excludes = new String[processing.size()];
					index = 0;
					for (Object o : processing) {
						excludes[index++] = (String) o;
					}
					data.putStringArray(MidTableManager.DATA_KEY_EXCLUDES,
							excludes);
					break;
				}
				default:
					throw new MidException("Invalid dest key type:"
							+ mMidMgr.getDestKey().getType());
				}
				data.putIntArray(MidTableManager.DATA_KEY_SYNCS, syncs);
			}
		} catch (MidException e) {
			loge("Exception:", e);
		}

		SyncData.Config config = new SyncData.Config(true);
		config.mmCallback = mHandler.obtainMessage();
		config.mmCallback.obj = new HandlerArg(true, syncMap);
		data.setConfig(config);
		mMidMgr.mModule.send(data);
	}

	@Override
	public void processRequest(SyncData data) {
		final SyncData[] datas = data
				.getDataArray(MidTableManager.DATA_KEY_DATAS);
		if (datas == null || datas.length == 0) {
			loge("can not find datas from RetriveMidSyncData.");
			return;
		}
		logd("datas received size:" + datas.length);

		final int dPos = data.getInt(MidTableManager.DATA_KEY_DELETES_P);
		final int uPos = data.getInt(MidTableManager.DATA_KEY_UPDATES_P);
		final int iPos = data.getInt(MidTableManager.DATA_KEY_INSERTS_P);
		final int aPos = data.getInt(MidTableManager.DATA_KEY_APPENDS_P);

		SyncData[] deletes = null;
		SyncData[] updates = null;
		SyncData[] insterts = null;
		SyncData[] appends = null;
		int newP = -1, l = dPos - newP;
		logd("deletes size:" + l);
		if (l > 0) {
			deletes = new SyncData[l];
			System.arraycopy(datas, newP + 1, deletes, 0, l);
			newP += l;
		}

		l = uPos - newP;
		logd("updates size:" + l);
		if (l > 0) {
			updates = new SyncData[l];
			System.arraycopy(datas, newP + 1, updates, 0, l);
			newP += l;
		}

		l = iPos - newP;
		logd("inserts size:" + l);
		if (l > 0) {
			insterts = new SyncData[l];
			System.arraycopy(datas, newP + 1, insterts, 0, l);
			newP += l;
		}

		l = aPos - newP;
		logd("appends size:" + l);
		if (l > 0) {
			appends = new SyncData[l];
			System.arraycopy(datas, newP + 1, appends, 0, l);
			newP += l;
		}

		final SyncData[] ds = deletes;
		final SyncData[] us = updates;
		final SyncData[] is = insterts;
		final SyncData[] as = appends;

		int dsl = ds == null ? 0 : ds.length;
		int usl = us == null ? 0 : us.length;
		int isl = is == null ? 0 : is.length;
		final SyncData[] datasWithoutAppend = new SyncData[dsl + usl + isl];
		if (dsl != 0) {
			System.arraycopy(ds, 0, datasWithoutAppend, 0, dsl);
		}

		if (usl != 0) {
			System.arraycopy(us, 0, datasWithoutAppend, dsl, usl);
		}

		if (isl != 0) {
			System.arraycopy(is, 0, datasWithoutAppend, dsl + usl, isl);
		}
		final Map<Object, Integer> syncMap = new HashMap<Object, Integer>();
		Column column = new SyncColumn();
		try {
			for (SyncData sd : datasWithoutAppend) {
				syncMap.put(
						MidTools.getKeyValue(sd, mMidMgr.getDestKey(), false),
						(Integer) MidTools.getValue(sd, column));
			}

			mPool.putAll(syncMap);
			final boolean isMidSync = data.getBoolean(
					MidTableManager.DATA_KEY_MID_SYNC_FLAG, false);

			if (datasWithoutAppend.length == 0 || syncMap.isEmpty()) {
				loge("Received empty request.");
				return;
			}
			mAsyncHandler.post(new Runnable() {

				@Override
				public void run() {
					try {
						SyncData[] isChecked, usChecked;
						if (isMidSync && ((is != null) || (us != null))) {
							ArrayList<SyncData> isList = new ArrayList<SyncData>();
							ArrayList<SyncData> usList = new ArrayList<SyncData>();
							logi("Request is mid sync, so some datas check is needed.");
							Set<Object> insertAndUpdates = new HashSet<Object>();
							if (is != null) {
								for (SyncData insert : is) {
									insertAndUpdates.add(MidTools.getKeyValue(
											insert, mMidMgr.getDestKey(), false));
								}
							}

							if (us != null) {
								for (SyncData update : us) {
									insertAndUpdates.add(MidTools.getKeyValue(
											update, mMidMgr.getDestKey(), false));
								}
							}
							StringBuilder sb = new StringBuilder(Mid.COLUMN_KEY
									+ " IN (");
							boolean isFirst = true;
							for (Object o : insertAndUpdates) {
								if (isFirst) {
									sb.append("'").append(o).append("'");
									isFirst = false;
								} else {
									sb.append(",").append("'").append(o)
											.append("'");
								}
							}
							sb.append(")");
							logv("selection:" + sb.toString());
							Cursor c = null;
							try {
								c = mMidMgr.mContext
										.getContentResolver()
										.query(mMidMgr.getDestUri(),
												new String[] { Mid.COLUMN_KEY },
												sb.toString(), null, null);
								if (c == null) {
									throw new MidException("get Null cursor.");
								}

								if (c.getCount() > 0) {
									if (!c.moveToFirst()) {
										throw new MidException(
												"can not moveToFirst from cursor");
									}

									Set<Object> set = new HashSet<Object>();
									do {
										set.add(MidTools.getKeyValue(c,
												mMidMgr.getDestKey(), true));
									} while (c.moveToNext());

									if (is != null) {
										for (SyncData data : is) {
											if (set.contains(MidTools
													.getKeyValue(data, mMidMgr
															.getDestKey(),
															false))) {
												usList.add(data);
											} else {
												isList.add(data);
											}
										}
									}

									if (us != null) {
										for (SyncData data : us) {
											if (set.contains(MidTools
													.getKeyValue(data, mMidMgr
															.getDestKey(),
															false))) {
												usList.add(data);
											} else {
												isList.add(data);
											}
										}
									}
								} else {
									if (us != null) {
										for (SyncData data : us) {
											isList.add(data);
										}
									}

									if (is != null) {
										for (SyncData data : is) {
											isList.add(data);
										}
									}
								}

								usChecked = usList.toArray(new SyncData[] {});
								isChecked = isList.toArray(new SyncData[] {});
							} finally {
								if (c != null) {
									c.close();
								}
							}
						} else {
							usChecked = us;
							isChecked = is;
						}
						ArrayList<ContentProviderOperation> operations = mDest
								.applySyncDatas(ds, usChecked, isChecked, as);
						if (V) {
							for (ContentProviderOperation opt : operations) {
								logv(opt.toString());
							}
						}
						mMidMgr.mContext.getContentResolver().applyBatch(
								mMidMgr.getDestAuthorityName(), operations);
						logi("Dest apply over");

						SyncData resp = new SyncData();
						SyncData.Config config = new SyncData.Config(true);
						config.mmCallback = mHandler.obtainMessage();
						config.mmCallback.obj = new HandlerArg(false, syncMap);
						resp.setConfig(config);

						resp.putInt(MidTableManager.DATA_KEY_TRANSACTION,
								MidTableManager.DATA_VALUE_TRANSACTION_RESP);

						int[] syncs = new int[syncMap.size()];
						switch (mMidMgr.getDestKey().getType()) {
						case Column.INTEGER: {
							int[] keys = new int[syncMap.size()];
							int index = 0;
							for (Object o : syncMap.keySet()) {
								keys[index] = (Integer) o;
								syncs[index] = syncMap.get(o);
								index++;
							}
							resp.putIntArray(MidTableManager.DATA_KEY_KESYS,
									keys);
							break;
						}
						case Column.LONG: {
							long[] keys = new long[syncMap.size()];
							int index = 0;
							for (Object o : syncMap.keySet()) {
								keys[index] = (Long) o;
								syncs[index] = syncMap.get(o);
								index++;
							}
							resp.putLongArray(MidTableManager.DATA_KEY_KESYS,
									keys);
							break;
						}
						case Column.STRING: {
							String[] keys = new String[syncMap.size()];
							int index = 0;
							for (Object o : syncMap.keySet()) {
								keys[index] = (String) o;
								syncs[index] = syncMap.get(o);
								index++;
							}
							resp.putStringArray(MidTableManager.DATA_KEY_KESYS,
									keys);
							break;
						}
						default:
							throw new MidException("Invalid dest key type:"
									+ mMidMgr.getDestKey().getType());
						}
						resp.putIntArray(MidTableManager.DATA_KEY_SYNCS, syncs);
						mMidMgr.mModule.send(resp);
					} catch (Exception e) {
						loge("Clear sync value pool, and give up the resp for this req.", e);
						mPool.remove(syncMap);
					}
				}

			});

		} catch (MidException e) {
			loge("Exception:", e);
		}

	}

	@Override
	public void processRespSent(boolean success, Object obj) {
		HandlerArg arg = (HandlerArg) obj;
		logi((arg.isREF ? "Reflect request" : "Response") + " sent:"
				+ (success ? "succeed" : "failed"));
		if (success) {
			if (arg.syncMap != null) {
				mPool.remove(arg.syncMap);
			}
		} else if (!arg.isREF) {
			mPool.signComplete(arg.syncMap);
		}
	}

	private static final String PRE = "<DTM>";

	private static void logv(String msg) {
		Log.v(Mid.DEST, PRE + msg);
	}

	private static void logd(String msg) {
		Log.d(Mid.DEST, PRE + msg);
	}

	private static void logi(String msg) {
		Log.i(Mid.DEST, PRE + msg);
	}

	private static void logw(String msg) {
		Log.w(Mid.DEST, PRE + msg);
	}

	private static void loge(String msg) {
		Log.e(Mid.DEST, PRE + msg);
	}

	private static void loge(String msg, Throwable t) {
		Log.e(Mid.DEST, PRE + msg, t);
	}

}

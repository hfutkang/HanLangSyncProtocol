package cn.ingenic.glasssync.services.mid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.ingenic.glasssync.services.mid.Transaction.OnRemoveListener;

import android.util.Log;

//not used 
class TransactionPool<T> {
	private static final String TAG = "TransactionStack";

	static final int NOT_FOUND = Integer.MIN_VALUE;

	private long mMaxTranId = -1;
	private HashMap<Long, Transaction<T>> mTotalTransMap = new HashMap<Long, Transaction<T>>();
	private HashMap<T, Integer> mTotalDatasMaxStMap = new HashMap<T, Integer>();
	private HashMap<T, Set<Long>> mTotalDatasTranIdsMap = new HashMap<T, Set<Long>>();

	synchronized Set<T> getDatasAfter(long tId) {
		Set<T> results = new HashSet<T>();
		for (Transaction<T> tran : mTotalTransMap.values()) {
			if (tran.getId() > tId) {
				results.addAll(tran.getDatas().keySet());
			}
		}

		return results;
	}

	synchronized void push(Transaction<T> newTran) {
		long newTranId = newTran.getId();
		if (newTranId <= mMaxTranId) {
			Log.e(TAG, "Pushing an older Transaction is impossible.");
			return;
		}
		mMaxTranId = newTranId;
		Map<T, Integer> map = newTran.getDatas();

		for (T t : map.keySet()) {
			Set<Long> tranIdsSet = null;
			if (mTotalDatasMaxStMap.containsKey(t)) {
				if (map.get(t) >= mTotalDatasMaxStMap.get(t)) {
					removeEntryFromOlderTrans(t);
				} else {
					Log.e(TAG,
							"Datas's ST from new Tran is lower than older Trans, this is impossible.");
					return;
				}

				tranIdsSet = mTotalDatasTranIdsMap.get(t);
			}

			if (tranIdsSet == null) {
				tranIdsSet = new HashSet<Long>();
				mTotalDatasTranIdsMap.put(t, tranIdsSet);
			}

			mTotalDatasMaxStMap.put(t, map.get(t));
			tranIdsSet.add(newTran.getId());
		}

		Set<Long> rm = new HashSet<Long>();
		for (long tId : mTotalTransMap.keySet()) {
			Transaction<T> tran = mTotalTransMap.get(tId);
			if (tran.getDatas().isEmpty()) {
				rm.add(tId);
			}
		}

		for (long tId : rm) {
			removeTran(tId, false);
		}

		mTotalTransMap.put(newTranId, newTran);
	}

	private Transaction<T> removeTran(long tId, boolean pop) {
		Transaction<T> tran = mTotalTransMap.remove(tId);
		if (tran == null) {
			Log.e(TAG, "Transaction:" + tId + " had already been removed!");
			return null;
		}

		OnRemoveListener<T> listener = tran.getOnRemoveListener();
		if (listener != null) {
			listener.onRemove(tran, pop);
		}

		return tran;
	}

	private void removeEntryFromOlderTrans(T t) {
		Set<Long> set = mTotalDatasTranIdsMap.get(t);
		if (set.isEmpty()) {
			Log.e(TAG, "mTotalDatasTrans must have the value of " + t);
			return;
		}

		Set<Long> rm = new HashSet<Long>();
		for (long tId : set) {
			Transaction<T> tran = mTotalTransMap.get(tId);
			Map<T, Integer> datas = tran.getDatas();
			if (datas.containsKey(t)) {
				datas.remove(t);
				rm.add(tId);
			}
		}
		set.removeAll(rm);
		if (set.isEmpty()) {
			mTotalDatasTranIdsMap.remove(t);
		}
	}

	private void removeTranIdsMapValue(long tId) {
		Set<T> rm = new HashSet<T>();
		for (T t : mTotalDatasTranIdsMap.keySet()) {
			Set<Long> set = mTotalDatasTranIdsMap.get(t);
			if (set.remove(tId)) {
				if (set.isEmpty()) {
					rm.add(t);
				}
			}
		}

		for (T t : rm) {
			mTotalDatasTranIdsMap.remove(t);
		}
	}

	private void removeMaxStMapValue(Transaction<T> tran) {
		Map<T, Integer> stMap = tran.getDatas();
		for (T t : stMap.keySet()) {
			int maxSt = mTotalDatasMaxStMap.get(t);
			if (stMap.get(t) == maxSt) {
				mTotalDatasMaxStMap.remove(t);
			} else {
				Log.e(TAG, "MaxSt:" + maxSt
						+ "in mTotalDatasMaxStMap should be the same as entry("
						+ stMap.get(t)
						+ ") in the datas of Tran which to be removed");
			}
		}
	}

	synchronized Transaction<T> pop(long tId) {
		Transaction<T> tran = removeTran(tId, true);
		if (tran == null) {
			Log.i(TAG, "Transaction had already been removed.");
			return null;
		}

		removeTranIdsMapValue(tId);
		removeMaxStMapValue(tran);
		return tran;
	}
}

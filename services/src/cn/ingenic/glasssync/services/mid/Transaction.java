package cn.ingenic.glasssync.services.mid;

import java.util.Map;

// not used 
class Transaction<T> {
	private final long mId;
	private final Map<T, Integer> mDatas;
	private OnRemoveListener<T> mRemoveListener;

	Transaction(long tId, Map<T, Integer> datas) {
		if (tId == 0l || datas == null || datas.isEmpty()) {
			throw new IllegalArgumentException(
					"Invalid args for construct Transaction");
		}
		mId = tId;
		mDatas = datas;
	}

//	final void setListener(OnRemoveListener<T> listener) {
//		mRemoveListener = listener;
//	}

	final long getId() {
		return mId;
	}

	final Map<T, Integer> getDatas() {
		return mDatas;
	}

	OnRemoveListener<T> getOnRemoveListener() {
		return mRemoveListener;
	}

	interface OnRemoveListener<T> {
		void onRemove(Transaction<T> tran, boolean pop);
	}
}

package cn.ingenic.glasssync.services;

class TooLargeSyncDataBuilder {
	private final SyncData mParent;
	private final int mTotalLen;
	private int mReaded = 0;
	private final byte[] mSerialDatas;
	TooLargeSyncDataBuilder(int totalLen, SyncData parent) {
		mTotalLen = totalLen;
		mParent = parent;
		mSerialDatas = new byte[totalLen];
		add(parent);
	}
	
	boolean add(SyncData d) {
		byte[] added = d.getSerialDatas();
		if ((mReaded + added.length) > mTotalLen) {
			return false;
		}
		System.arraycopy(added, 0, mSerialDatas, mReaded, added.length);
		mReaded += added.length;
		return true;
	}
	
	boolean isFinish() {
		return mReaded == mTotalLen;
	}
	
	SyncData build() {
		mParent.setSerialDatas(mSerialDatas);
		return mParent;
	}
}

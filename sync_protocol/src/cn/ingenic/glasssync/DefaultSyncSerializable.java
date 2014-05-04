package cn.ingenic.glasssync;

public class DefaultSyncSerializable implements SyncSerializable {
	
	private final SyncDescriptor mDes;
	private final byte[] mDatas;

	public DefaultSyncSerializable(SyncDescriptor des, byte[] datas) {
		if (des == null || datas == null) {
			throw new IllegalArgumentException("Args can not be null");
		}
		mDes = des;
		mDatas = datas;
	}
	
	@Override
	public SyncDescriptor getDescriptor() {
		return mDes;
	}

	@Override
	public byte[] getDatas(int pos, int len) {
		byte[] buffer;
		int totalLen = getLength();
		if ((pos + len) > totalLen) {
			len = totalLen - pos;
		}
		buffer = new byte[len];
		System.arraycopy(mDatas, pos, buffer, 0, len);
		return buffer;
	}

	@Override
	public int getLength() {
		return mDatas.length;
	}
	
}
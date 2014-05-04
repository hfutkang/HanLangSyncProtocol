package cn.ingenic.glasssync;

import android.util.Log;
import cn.ingenic.glasssync.transport.ext.ProLogTag;
import cn.ingenic.glasssync.transport.ext.TransportManagerExtConstants;

public class FileOutputSyncSerializable implements SyncSerializable {

	private final SyncDescriptor mDes;
	private final String mOriName;
	private final String mDestName;
	private final int mLen;

	public FileOutputSyncSerializable(SyncDescriptor des, String oriName, String destName, int len) {
		if (des == null || oriName == null || destName == null) {
			throw new IllegalArgumentException("Invalid args.");
		}
		des.mPri = TransportManagerExtConstants.FILE;
		mDes = des;
		mOriName = oriName;
		mDestName = destName;
		mLen = len;
	}

	@Override
	public SyncDescriptor getDescriptor() {
		return mDes;
	}

	@Override
	public byte[] getDatas(int pos, int len) {
		Log.e(ProLogTag.TAG,
				"FileOutputSyncSerializable do not support getDatas(int pos, int len)");
		return null;
	}

	@Override
	public int getLength() {
		return mLen;
	}

	public String getOriName() {
		return mOriName;
	}
	
	public String getDestName() {
		return mDestName;
	}

}

package cn.ingenic.glasssync;

import java.io.InputStream;

import android.util.Log;

import cn.ingenic.glasssync.transport.ext.ProLogTag;
import cn.ingenic.glasssync.transport.ext.TransportManagerExtConstants;

public class FileInputSyncSerializable implements SyncSerializable {

	private final SyncDescriptor mDes;
	private final String mName;
	private final int mLen;
	private final InputStream mIn;

	public FileInputSyncSerializable(SyncDescriptor des, String name, int len,
			InputStream in) {
		if (des == null || name == null || in == null || len < 0) {
			throw new IllegalArgumentException("Invalid args.");
		}
		des.mPri = TransportManagerExtConstants.FILE;
		mDes = des;
		mName = name;
		mLen = len;
		mIn = in;
	}

	@Override
	public SyncDescriptor getDescriptor() {
		return mDes;
	}

	@Override
	public byte[] getDatas(int pos, int len) {
		Log.e(ProLogTag.TAG,
				"FileInputSyncSerializable do not support getDatas(int pos, int len)");
		return null;
	}

	@Override
	public int getLength() {
		return mLen;
	}

	public InputStream getInputStream() {
		return mIn;
	}

	public String getName() {
		return mName;
	}

}

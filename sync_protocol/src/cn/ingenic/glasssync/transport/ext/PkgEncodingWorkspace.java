package cn.ingenic.glasssync.transport.ext;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import cn.ingenic.glasssync.FileInputSyncSerializable;
import cn.ingenic.glasssync.SyncDescriptor;
import cn.ingenic.glasssync.SyncSerializable;

class PkgEncodingWorkspace {
	private int mCurType = TransportManagerExtConstants.FILE;
	private final List<Integer> mTypeList = new ArrayList<Integer>();
	private final SparseArray<Queue<SyncSerialIterator>> mReqMap = new SparseArray<Queue<SyncSerialIterator>>();
	private volatile boolean mIsLocking = false;
	private volatile boolean mIsQuit = false;
	private final int mFlagSuccess;
	private final int mFlagNoConnectivity;
	private final int mFlagUnknow;

	private static class PkgBuilder {
		static PkgBuilder newBuilder(byte[] datas) {
			return new PkgBuilder(datas);
		}
		
		static PkgBuilder newBuilder(byte[] datas, int len) {
			return new PkgBuilder(datas, len);
		}

		private final byte[] mmDatas;

		private PkgBuilder(byte[] datas) {
			this(datas, datas.length);
		}
		
		private PkgBuilder(byte[] datas, int len) {
			// mmDatas = new byte[1 + datas.length];
			// System.arraycopy(datas, 0, mmDatas, 1, datas.length);
			mmDatas = new byte[len];
			System.arraycopy(datas, 0, mmDatas, 0, len);
		}

		// PkgBuilder setPriority(int pri) {
		// mmDatas[0] = (byte) (mmDatas[0] | (pri << 6));
		// return this;
		// }

		Pkg build() {
			return new Pkg(mmDatas);
		}
	}

	private static class CfgBuilder {
		static CfgBuilder newBuilder() {
			return new CfgBuilder();
		}

		private final byte[] mmDatas = new byte[Cfg.MAX_LEN];
		private String mmModule;

		CfgBuilder setPriority(int pri) {
			mmDatas[0] = (byte) (mmDatas[0] | (pri << 6));
			return this;
		}

		CfgBuilder setServiceFlag(boolean isService) {
			return setFlag(isService, Cfg.FLAG_SERVICE);
		}

		private CfgBuilder setFlag(boolean set, int flag) {
			if (set) {
				mmDatas[0] = (byte) (mmDatas[0] | flag);
			} else {
				mmDatas[0] = (byte) (mmDatas[0] & ~flag);
			}
			return this;
		}

		CfgBuilder setReplyFlag(boolean isReply) {
			return setFlag(isReply, Cfg.FLAG_REPLY);
		}

		CfgBuilder setMidFlag(boolean isMid) {
			return setFlag(isMid, Cfg.FLAG_MID);
		}

		CfgBuilder setProjoFlag(boolean isProjo) {
			return setFlag(isProjo, Cfg.FLAG_PROJO);
		}

		CfgBuilder setModule(String module) throws ProtocolException,
				UnsupportedEncodingException {
			mmModule = module;
			byte[] bytes = module.getBytes(Pkg.UTF_8);
			if (bytes.length > Cfg.MODULE_LEN) {
				throw new ProtocolException("module name overflows.");
			}

			System.arraycopy(bytes, 0, mmDatas, 1, bytes.length);
			return this;
		}

		CfgBuilder setDatasCount(int count) throws ProtocolException {
			if (count <= 0) {
				throw new ProtocolException("invalid pkg count.");
			}

			mmDatas[16] = (byte) (count >> 24);
			mmDatas[17] = (byte) (count >> 16);
			mmDatas[18] = (byte) (count >> 8);
			mmDatas[19] = (byte) (count);
			return this;
		}

		CfgBuilder setUUID(UUID uuid) {
			if (uuid != null) {
				writeLong(Cfg.POS_MOST_SIG_BITS, uuid.getMostSignificantBits());
				writeLong(Cfg.POS_LEAST_SIG_BITS,
						uuid.getLeastSignificantBits());
			}
			return this;
		}

		private void writeLong(int pos, long l) {
			mmDatas[pos++] = (byte) (l >> 56);
			mmDatas[pos++] = (byte) (l >> 48);
			mmDatas[pos++] = (byte) (l >> 40);
			mmDatas[pos++] = (byte) (l >> 32);
			mmDatas[pos++] = (byte) (l >> 24);
			mmDatas[pos++] = (byte) (l >> 16);
			mmDatas[pos++] = (byte) (l >> 8);
			mmDatas[pos] = (byte) (l);
		}

		Cfg build() {
			Cfg cfg = new Cfg(mmDatas);
			logv("Sending Cfg:" + mmModule + " DataCount:"
					+ cfg.getDatasCount());
			return new Cfg(mmDatas);
		}
	}

	private class SyncSerialIterator {
		protected static final int CONFIG_POS = -1;
		protected final SyncSerializable mmSerial;
		protected int mmCurPos = CONFIG_POS;
		protected final int mmTotalLen;

		SyncSerialIterator(SyncSerializable serial) {
			mmSerial = serial;
			mmTotalLen = serial.getLength();
			logv("Sending Serial Des:" + serial.getDescriptor());
		}

		Pkg next() throws Exception {
			SyncDescriptor des = mmSerial.getDescriptor();
			if (mmCurPos == CONFIG_POS) {
				CfgBuilder builder = CfgBuilder.newBuilder()
						.setPriority(des.mPri).setServiceFlag(des.isService)
						.setReplyFlag(des.isReply).setMidFlag(des.isMid)
						.setProjoFlag(des.isProjo).setModule(des.mModule)
						.setDatasCount(mmTotalLen).setUUID(des.mUUID);
				mmCurPos = 0;
				return builder.build();
			} else if (mmCurPos >= mmTotalLen) {
				return null;
			}

			byte[] datas = mmSerial.getDatas(mmCurPos, Pkg.MAX_LEN);
			if (datas == null) {
				return null;
			}
			PkgBuilder builder = PkgBuilder.newBuilder(datas);
			// .setPriority(des.mPri);
			mmCurPos += datas.length;
			return builder.build();
		}

		void notifySendComplete(boolean success) {
			SyncDescriptor des = mmSerial.getDescriptor();
			Message msg = des.mCallback;
			if (msg != null) {
				logv("Module:" + des.mModule + " notifySendComplete "
						+ (success ? " SUCCEED" : "FAILED"));
				msg.arg1 = success ? mFlagSuccess : mFlagNoConnectivity;
				msg.sendToTarget();
			}
		}
	}

	private class FileSyncSerialIterator extends SyncSerialIterator {
		private final InputStream mmIn;
		private byte[] mmPre;
		private final String mmName;
	        private final String mmPath;

		FileSyncSerialIterator(FileInputSyncSerializable serial) {
			super(serial);
			mmIn = serial.getInputStream();
			mmName = serial.getName();
			mmPath = serial.getPath();
		}

		Pkg next() throws Exception {
			byte[] datas = new byte[Pkg.MAX_LEN];
			int offset = 0;
			int shouldReadCount;
			SyncDescriptor des = mmSerial.getDescriptor();
			if (mmCurPos == CONFIG_POS) {
				byte[] name = mmName.getBytes(Pkg.UTF_8);
				int nameLen = name.length;
				int pathlen = 0;
				if (mmPath != null){
				    pathlen = mmPath.length();
				    loge("pathlen:" + pathlen);
				}else{
				    loge("pathlen is zero");
				}
				mmPre = new byte[4 + nameLen + 4 + pathlen];
				mmPre[0] = (byte) ((nameLen >> 24) & 0xff);
				mmPre[1] = (byte) ((nameLen >> 16) & 0xff);
				mmPre[2] = (byte) ((nameLen >> 8) & 0xff);
				mmPre[3] = (byte) nameLen;
				System.arraycopy(name, 0, mmPre, 4, nameLen);

				mmPre[0+4+nameLen] = (byte) ((pathlen >> 24) & 0xff);
				mmPre[1+4+nameLen] = (byte) ((pathlen >> 16) & 0xff);
				mmPre[2+4+nameLen] = (byte) ((pathlen >> 8) & 0xff);
				mmPre[3+4+nameLen] = (byte) ((pathlen) & 0xff);
				if (pathlen > 0){
				    byte[] path = mmPath.getBytes(Pkg.UTF_8);
				    System.arraycopy(path, 0, mmPre, 8+nameLen, pathlen);
				}

				int toatalLen = (4 + nameLen + mmTotalLen + 4 + pathlen);
				loge("totalLen:" + toatalLen);
//				int pkgCount = ((toatalLen % Pkg.MAX_LEN) == 0) ? (toatalLen / Pkg.MAX_LEN)
//						: ((mmSerial.getLength() / Pkg.MAX_LEN) + 1);
				CfgBuilder builder = CfgBuilder.newBuilder()
						.setPriority(des.mPri).setServiceFlag(des.isService)
						.setReplyFlag(des.isReply).setMidFlag(des.isMid)
						.setModule(des.mModule).setDatasCount(toatalLen)
						.setUUID(des.mUUID);
				mmCurPos = 0;
				return builder.build();
			} else if (mmCurPos == 0) {
				System.arraycopy(mmPre, 0, datas, 0, mmPre.length);
				offset = mmPre.length;
				shouldReadCount = datas.length - offset;
				if (shouldReadCount < 0) {
					// TODO: resolve the larger File name bytes
					throw new ProtocolException(
							"File name bytes is larger than Pkg.LEN.");
				}

				if (shouldReadCount > mmTotalLen) {
					shouldReadCount = mmTotalLen;
				}
			} else if (mmCurPos >= mmTotalLen) {
				mmIn.close();
				return null;
			} else {
				shouldReadCount = datas.length;
				if (shouldReadCount > (mmTotalLen - mmCurPos)) {
					shouldReadCount = mmTotalLen - mmCurPos;
				}
			}

			int readLen = 0;
			int readCount = shouldReadCount;
			int start = offset;
			while (readLen < shouldReadCount) {
				readLen += mmIn.read(datas, offset, readCount);
				offset += readLen;
				readCount -= readLen;
			}
			PkgBuilder builder = PkgBuilder.newBuilder(datas, start + readLen);
			// .setPriority(des.mPri);
			mmCurPos += shouldReadCount;
			return builder.build();
		}

	}

	PkgEncodingWorkspace(int flagSuccess, int flagNoConnectivity, int flagUnknow) {
		mFlagSuccess = flagSuccess;
		mFlagNoConnectivity = flagNoConnectivity;
		mFlagUnknow = flagUnknow;
		mTypeList.add(TransportManagerExtConstants.CMD);
		mTypeList.add(TransportManagerExtConstants.SPEICAL);
		mTypeList.add(TransportManagerExtConstants.DATA);
		mTypeList.add(TransportManagerExtConstants.FILE);
		mReqMap.put(TransportManagerExtConstants.CMD,
				new LinkedList<SyncSerialIterator>());
		mReqMap.put(TransportManagerExtConstants.SPEICAL,
				new LinkedList<SyncSerialIterator>());
		mReqMap.put(TransportManagerExtConstants.DATA,
				new LinkedList<SyncSerialIterator>());
		mReqMap.put(TransportManagerExtConstants.FILE,
				new LinkedList<SyncSerialIterator>());
	}

	void push(SyncSerializable syncReq) {
		synchronized (mReqMap) {
			int type = syncReq.getDescriptor().mPri;
			SyncSerialIterator iterator;
			if (type == TransportManagerExtConstants.FILE) {
				if (!(syncReq instanceof FileInputSyncSerializable)) {
					loge("Type FILE only supports FileInputtSyncSerializable");
					Message msg = syncReq.getDescriptor().mCallback;
					if (msg != null) {
						msg.arg1 = mFlagUnknow;
					}
					msg.sendToTarget();
					return;
				}
				iterator = new FileSyncSerialIterator(
						(FileInputSyncSerializable) syncReq);
			} else {
				iterator = new SyncSerialIterator(syncReq);
			}
			mReqMap.get(type).add(iterator);
			if (type < mCurType) {
				logv("Priority of type:" + type + " higer than curType:"
						+ mCurType);
				mCurType = type;
			}

			if (mIsLocking) {
				mReqMap.notifyAll();
				mIsLocking = false;
			}
		}
	}

	Pkg poll() throws Exception {
		if (mIsQuit) {
			logd("Quit now.");
			mIsQuit = false;
			return null;
		}

		synchronized (mReqMap) {
			Queue<SyncSerialIterator> curQ = mReqMap.get(mCurType);
			if (curQ.isEmpty()) {
				if (!fall()) {
					try {
						logd("waiting for new Push or Clear.");
						mIsLocking = true;
						ConnectionTimeoutManager.getInstance().peEmpty();
						mReqMap.wait();
					} catch (InterruptedException e) {
						loge("InterruptedException in poll", e);
					}
					// return null;
				}
				return poll();
			}

			Pkg pkg = curQ.peek().next();
			if (pkg == null) {
				SyncSerialIterator iterator = curQ.poll();
				iterator.notifySendComplete(true);
				return poll();
			}

			return pkg;
		}
	}
	
	void start() {
		mIsQuit = false;
	}

        int size(int type){
	    return mReqMap.get(type).size();
	}

	void clear() {
		synchronized (mReqMap) {
			for (int type : mTypeList) {
				Queue<SyncSerialIterator> q = mReqMap.valueAt(type);
				SyncSerialIterator iter = q.poll();
				while (iter != null) {
					iter.notifySendComplete(false);
					iter = q.poll();
				}
			}

			mIsQuit = true;
			if (mIsLocking) {
				mReqMap.notifyAll();
				mIsLocking = false;
			}
			logd("PkgWorkspace is cleared.");
		}
	}

	boolean isEmpty() {
		for (int type : mTypeList) {
			if (!mReqMap.get(type).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private boolean fall() {
		if (mCurType == TransportManagerExtConstants.FILE) {
			logi("all Pkgs clear, with curT:" + mCurType);
			return false;
		}

		for (int pos = mCurType + 1; mReqMap.get(pos).isEmpty(); pos++) {
			if (pos == TransportManagerExtConstants.FILE) {
				logi("all Pkgs clear, with curT:" + mCurType);
				return false;
			}
		}

		mCurType += 1;
		return true;
	}

	private static final String PRE = "<PEWP>";
	private static final boolean DBG = false;
	private static final void logv(String msg) {
	    if(DBG)Log.v(ProLogTag.TAG, PRE + msg);
	}

	private static final void logd(String msg) {
		if(DBG)Log.d(ProLogTag.TAG, PRE + msg);
	}

	private static final void logi(String msg) {
		if(DBG)Log.i(ProLogTag.TAG, PRE + msg);
	}

	private static final void loge(String msg) {
		Log.e(ProLogTag.TAG, PRE + msg);
	}

	private static final void loge(String msg, Throwable t) {
		Log.e(ProLogTag.TAG, PRE + msg, t);
	}

}

package cn.ingenic.glasssync.transport.ext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncSerializable;
import cn.ingenic.glasssync.FileOutputSyncSerializable;
import cn.ingenic.glasssync.SyncDescriptor;
import cn.ingenic.glasssync.SyncSerializable;

public class PkgDecodingWorkspace {

	private final Context mContext;
	private final Handler mNotifyRetriveHandler;
	private int mCurPri = TransportManagerExtConstants.FILE;
	private volatile boolean mIsDecoding = false;
	private ArrayList<Queue<SyncSerialBuilder>> mRetMap = new ArrayList<Queue<SyncSerialBuilder>>(4);

	PkgDecodingWorkspace(Context context, Handler notifyRetrive) {
		mContext = context;
		mNotifyRetriveHandler = notifyRetrive;
		mRetMap.add(//TransportManagerExtConstants.CMD,
				new LinkedList<SyncSerialBuilder>());
		mRetMap.add(//TransportManagerExtConstants.SPEICAL,
				new LinkedList<SyncSerialBuilder>());
		mRetMap.add(//TransportManagerExtConstants.DATA,
				new LinkedList<SyncSerialBuilder>());
		mRetMap.add(//TransportManagerExtConstants.FILE,
				new LinkedList<SyncSerialBuilder>());
	}

	private class SyncSerialBuilder {
		protected final int mmDataCount;
		protected final SyncDescriptor mmDes;
		protected byte[] mmBuffer;
		protected int mmPos = 0;

		SyncSerialBuilder(Cfg cfg) throws UnsupportedEncodingException {
			mmDes = new SyncDescriptor(cfg.getType(), cfg.isService(),
					cfg.isReply(), cfg.isMid(), cfg.isProjo(), cfg.getModule(), cfg.getUUID());
			mmDataCount = cfg.getDatasCount();
			logv("Received Cfg:" + cfg.getModule() + " DataCount:" + mmDataCount + " Des:" + mmDes);
			createBufferAndPos();
		}

		void createBufferAndPos() {
			mmBuffer = new byte[mmDataCount];
		}

		void push(Pkg pkg) throws Exception {
			byte[] datas = pkg.getData();
			System.arraycopy(datas, 0, mmBuffer, mmPos, datas.length);
			mmPos += datas.length;
			pkg=null;//gc will release
		}

		boolean isFinish() {
			return mmPos >= mmDataCount;
		}

		SyncSerializable build() {
			byte[] datas = new byte[mmPos];
			System.arraycopy(mmBuffer, 0, datas, 0, mmPos);
			return new DefaultSyncSerializable(mmDes, datas);
		}
	}
	
        private static String getDestDirString(String in){
	    loge("MultiMedia getDestDirString " + in);
	    if (in == null){
		loge("getDestDirString" + "null");
		return "data";
	    }

	    if (in.endsWith("jpg") || in.endsWith("png")){
		return "Pictures";
	    }

	    if (in.endsWith("mp4") || in.endsWith("3gp")){
		return "Video";
	    }

	    if (in.endsWith("wma") || in.endsWith("mp3")){
		return "Music";
	    }

	    return "data";
	}
	private static File getUniqueDestination(String packageName, String name, String extension) throws IOException {
	        String dds = getDestDirString(extension);
		File f = new File(Environment
				.getExternalStorageDirectory() + "/" + "IGlass/" + dds);
		if (!f.exists()) {
			logd("File:" + f + " does not exist, call mkdirs");
			if (!f.mkdirs()) {
				throw new IOException("File:" + f + " mkdirs failed");
			}
		}
		String base = f + "/" + name;
		boolean noExt = TextUtils.isEmpty(extension);
		String fileName = noExt ? base : base + "." + extension;
		File file = new File(fileName);

		for (int i = 2; file.exists(); i++) {
			file = new File(base + "_" + i + (noExt ? "" : "." + extension));
		}
		
		return file;
	}

	private class FileSyncSerialBuilder extends SyncSerialBuilder {
		private String mmOriName;
		private String mmDestName;
		private FileOutputStream mmFos;

		FileSyncSerialBuilder(Cfg cfg) throws UnsupportedEncodingException {
			super(cfg);
		}

		@Override
		void createBufferAndPos() {
			mmBuffer = new byte[Pkg.MAX_LEN];
		}

		@Override
		void push(Pkg pkg) throws Exception {
			byte[] datas = pkg.getData();
			int offset = 0;
			int byteCount = datas.length;
			if (mmPos == 0) {
				int nameLen = ((datas[0] & 0xff) << 24);
				nameLen |= ((datas[1] & 0xff) << 16);
				nameLen |= ((datas[2] & 0xff) << 8);
				nameLen |= (datas[3] & 0xff);
				if (datas.length >= (4 + nameLen)) {
					byte[] nameBytes = new byte[nameLen];
					System.arraycopy(datas, 4, nameBytes, 0, nameLen);
					mmOriName = new String(nameBytes, Pkg.UTF_8);
					String extension = "";
					String fileName = mmOriName;
					int index;
					if ((index = mmOriName.lastIndexOf('.')) != -1) {
						extension = mmOriName.substring(index + 1,
								mmOriName.length());
						fileName = mmOriName.substring(0, index);
					}
					File dest = getUniqueDestination(
							mContext.getPackageName(), fileName, extension);
					mmDestName = dest.getAbsolutePath();
					mmFos = new FileOutputStream(mmDestName);
					offset = (4 + nameLen);
					byteCount = datas.length - offset;
				} else {
					//TODO: resolve the larger File name bytes
					throw new ProtocolException("File name bytes is larger than Pkg.LEN.");
				}
			}
			mmFos.write(datas, offset, byteCount);
			mmPos += (offset + byteCount);
			pkg=null;//gc will release
		}

		@Override                                                                
		SyncSerializable build() {
			if (mmFos != null) {
				try {
					mmFos.close();
				} catch (IOException e) {
					loge("IOException:", e);
				}
			}
			return new FileOutputSyncSerializable(mmDes, mmOriName, mmDestName,
					mmPos + 1);
		}

//		void notfiyRetriveFailed() {
//			String module = mmDes.mModule;
//			DefaultSyncManager manager = DefaultSyncManager.getDefault();
//			Module m = manager.getModule(module);
//			if (m != null) {
//				OnFileChannelCallBack cb = m.getFileChannelCallBack();
//				if (cb != null) {
//					cb.onRetriveComplete(mmOriName, false);
//				} else {
//					loge("Can not find OnFileChannelCallBack from module:"
//							+ module
//							+ " in FileSyncSerialBuilder notifying process.");
//				}
//			} else {
//				loge("Can not find Moudle:" + module
//						+ " in FileSyncSerialBuilder notifying process.");
//			}
//		}
	}

	void push(Pkg pkg) throws Exception {
		int type = pkg.getType();
		if (type == Pkg.PKG) {
			Queue<SyncSerialBuilder> q = mRetMap.get(mCurPri);
			SyncSerialBuilder builder = q.element();
			builder.push(pkg);
			if (builder.isFinish()) {
				mIsDecoding = false;
				Message msg = mNotifyRetriveHandler.obtainMessage();
				msg.obj = builder.build();
				msg.sendToTarget();
				q.remove();
				if (q.isEmpty()) {
					while ((mCurPri++) < TransportManagerExtConstants.FILE
							&& mRetMap.get(mCurPri).isEmpty()) {
						if (mCurPri == TransportManagerExtConstants.FILE) {
							ConnectionTimeoutManager.getInstance().pdEmpty();
						}
					}
				}
			}else{
				/*now is receving data.
				 *  we should not close connect, although isEmpty()==true; */
				if(!mIsDecoding)
					mIsDecoding = true;
			}
		} else if (type == Pkg.CFG) {
			Cfg cfg = (Cfg) pkg;
			SyncSerialBuilder builder;
			int pri = cfg.getPriority();
			if (pri == Pkg.CHANNEL_FILE) {
				builder = new FileSyncSerialBuilder(cfg);
			} else {
				builder = new SyncSerialBuilder(cfg);
			}
			mRetMap.get(pri).add(builder);
			if (pri < mCurPri) {
				logd("CurPri changed to:" + pri + " from:" + mCurPri);
				mCurPri = pri;
			} else if (pri > mCurPri) {
				loge("Received a lower pri Cfg, something wrong meight happen in Send role.");
			}
		} else {
			loge("unsupoort Pkg type:" + type);
		}
	}

	void clear() {
		//Queue<SyncSerialBuilder> fileQueue = mRetMap.get(TransportManagerExtConstants.FILE);
//		while (!fileQueue.isEmpty()) {
//			FileSyncSerialBuilder sb = (FileSyncSerialBuilder) fileQueue.remove();
//			FileSyncSerialBuilder fsb = (FileSyncSerialBuilder) sb;
//			if (!fsb.isFinish()) {
//				fsb.notfiyRetriveFailed();
//			} else {
//				logw("FileSyncSerialBuilder is already finished when clear request comes.");
//			}
//		}
		for (int i = TransportManagerExtConstants.CHANNEL_MIN; i < mRetMap.size(); i++) {
			Queue<SyncSerialBuilder> q = mRetMap.get(i);
			q.clear();
		}
		logd("PkgDecodingWorkspace cleared.");
	}
	
	boolean isDecoding(){
		return mIsDecoding;
	}
	
	boolean isEmpty() {
		for (int i = TransportManagerExtConstants.CHANNEL_MIN; i < mRetMap.size(); i++) {
			Queue<SyncSerialBuilder> q = mRetMap.get(i);
			if (q.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	private static final String PRE = "<PDWP>";

	 private static final void logv(String msg) {
		 Log.v(ProLogTag.TAG, PRE + msg);
	 }
	
	private static final void logd(String msg) {
		Log.d(ProLogTag.TAG, PRE + msg);
	}
	
//	private static final void logw(String msg) {
//		Log.i(ProLogTag.TAG, PRE + msg);
//	}
	
	private static final void loge(String msg, Throwable t) {
		Log.e(ProLogTag.TAG, PRE + msg, t);
	}

	private static final void loge(String msg) {
		Log.e(ProLogTag.TAG, PRE + msg);
	}

}

package cn.ingenic.glasssync.transport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnFileChannelCallBack;
import cn.ingenic.glasssync.data.CmdProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.SystemCmds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("HandlerLeak")
public class FileChannelManager {
	private static final String TAG = "FileChannelManager";

	static final UUID FILE_UUID = UUID
			.fromString("683b3a33-d0db-42f2-81d7-74e4c9b090c1");
	static final String FILE_NAME = "FileChannelManager";
	
	private FileChannel mChannel;
	private Queue<Request> mSendingQueue = new LinkedList<Request>();
	private Queue<Retrive> mReceivingQueue = new LinkedList<Retrive>();
	private final Context mContext;
	private final MyHandler mHandler;
	private final Handler mTransportHandler;
	private final SendHandler mSendHandler;
	private final RetriveHandler mRetriveHandler;

	private static final int IDLE = 0;
	private static final int PREPARING = 1;
	private static final int CONNECTED = 2;
	private static final int DISCONNECTING = 3;

	private volatile int mState = IDLE;

	FileChannelManager(Context context, Handler transportHandler) {
		mTransportHandler = transportHandler;
		mContext = context;
		mHandler = new MyHandler();

		HandlerThread sendThread = new HandlerThread("WorkDrivenHandler");
		sendThread.start();
		mSendHandler = new SendHandler(sendThread.getLooper());

		HandlerThread retriveThread = new HandlerThread("RetriveHandler");
		retriveThread.start();
		mRetriveHandler = new RetriveHandler(retriveThread.getLooper());

	}

	class RetriveHandler extends Handler {
		private static final int BASE = 100;
		static final int RETRIVE_MSG = BASE + 1;

		RetriveHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RETRIVE_MSG:
				Retrive ret = (Retrive) msg.obj;
				mChannel.retrive(ret);
				break;
			}
		}
	}

	class SendHandler extends Handler {
		private static final int BASE = 100;
		static final int SEND_MSG = BASE + 1;

		public SendHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SEND_MSG:
				Request req = (Request) msg.obj;
				mChannel.send(req);
				break;
			}
		}

	}

	class MyHandler extends Handler {
		private static final int BASE = 0;
		static final int CONNECTED_MSG = BASE + 1;
		static final int SEND_OVER_MSG = BASE + 2;
		static final int RETRIVE_OVER_MSG = BASE + 3;
		static final int EXCEPTION_MSG = BASE + 4;
//		static final int LISTENED_TIMEOUT_MSG = BASE + 5;
		static final int DELAYED_CLOSE_MSG = BASE + 6;
		
		static final long TIMEOUT = 10 * 1000;
		
		private void closeDelayed() {
			mState = DISCONNECTING;
			sendEmptyMessageDelayed(DELAYED_CLOSE_MSG, TIMEOUT);
		}
		
		void removeCloseMsg() {
			if (mHandler.hasMessages(MyHandler.DELAYED_CLOSE_MSG)) {
				mHandler.removeMessages(MyHandler.DELAYED_CLOSE_MSG);
			}
		}
		
		private void work() {
			boolean sendingEmpty = mSendingQueue.isEmpty();
			boolean receivingEmpty = mReceivingQueue.isEmpty();
			if (!sendingEmpty) {
				Request req = mSendingQueue.peek();
				Message message = mSendHandler.obtainMessage(
						SendHandler.SEND_MSG, req);
				message.sendToTarget();
			} else {
				Log.d(TAG,
						"CONNECTED_MSG coming in FileChannelManager, but no request founded in SendingQueue!");
			}

			if (!receivingEmpty) {
				Retrive ret = mReceivingQueue.peek();
				Message message = mRetriveHandler.obtainMessage(
						RetriveHandler.RETRIVE_MSG, ret);
				message.sendToTarget();
			} else {
				Log.d(TAG,
						"CONNECTED_MSG coming in FileChannelManager, but no retrive founded in ReceivingQueue!");
			}

			if (sendingEmpty && receivingEmpty) {
				Log.w(TAG,
						"There is not any request or retrive founded in CONNECTED_MSG, it is confused!");
				close();
			}
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECTED_MSG:
				Log.d(TAG, "CONNECTED_MSG comes");
				mState = CONNECTED;
//				if (hasMessages(LISTENED_TIMEOUT_MSG)) {
//					Log.d(TAG, "remove LISTENED_TIMEOUT_MSG");
//					removeMessages(LISTENED_TIMEOUT_MSG);
//				}
				work();
				break;

			case SEND_OVER_MSG:
				Request request = mSendingQueue.remove();
				notifyComplete(request.module, request.name, true, true);

				if (!mSendingQueue.isEmpty()) {
					Request req = mSendingQueue.peek();
					Message message = mSendHandler.obtainMessage(
							SendHandler.SEND_MSG, req);
					message.sendToTarget();
				} else if (mReceivingQueue.isEmpty()) {
					Log.i(TAG,
							"There is not any request or retrive founded in SEND_OVER_MSG, close the FileChannel delayed.");
					closeDelayed();
				}
				break;

			case RETRIVE_OVER_MSG:
				Retrive retrive = mReceivingQueue.remove();
				notifyComplete(retrive.module, retrive.name, true, false);

				if (!mReceivingQueue.isEmpty()) {
					Retrive ret = mReceivingQueue.peek();
					Message message = mRetriveHandler.obtainMessage(
							RetriveHandler.RETRIVE_MSG, ret);
					message.sendToTarget();
				} else if (mSendingQueue.isEmpty()) {
					Log.i(TAG,
							"There is not any request or retrive founded in RETRIVE_OVER_MSG, close the FileChannel delayed.");
					closeDelayed();
				}
				break;

			case EXCEPTION_MSG:
				Log.d(TAG, "EXCEPTION_MSG comes, SendingQueue.size:"
						+ mSendingQueue.size() + " ReceivingQueue.size:"
						+ mReceivingQueue.size());
				close();
				break;
				
//			case LISTENED_TIMEOUT_MSG:
//				Log.w(TAG, "LISTENED_TIMEOUT_MSG comes, SendingQueue.size:"
//						+ mSendingQueue.size() + " ReceivingQueue.size:"
//						+ mReceivingQueue.size());
//				close();
//				break;
				
			case DELAYED_CLOSE_MSG:
				Log.d(TAG, "DELAYED_CLOSE_MSG comes, close FileChannel");
				close();
				break;
			}
			
		}

	}

	static File getUniqueDestination(String packageName, String name, String extension) throws IOException {
	    Log.e(TAG, " @@@" + Environment
				.getExternalStorageDirectory() + "/" + packageName);
		File f = new File(Environment
				.getExternalStorageDirectory() + "/" + packageName);
		if (!f.exists()) {
			Log.d(TAG, "File:" + f + " does not exist, call mkdirs");
			if (!f.mkdirs()) {
				throw new IOException("File:" + f + " mkdirs failed");
			}
		}
		String base = f + "/" + name;
		boolean noExt = TextUtils.isEmpty(extension);
		String fileName = noExt ? base : base + "." + extension;
		Log.e(TAG, "fineName:" + fileName);
		File file = new File(fileName);

		for (int i = 2; file.exists(); i++) {
			file = new File(base + "_" + i + (noExt ? "" : "." + extension));
		}
		
		return file;
	}
	
	private void clean() {
		mState = IDLE;
		while (!mSendingQueue.isEmpty()) {
			Request req = mSendingQueue.poll();
			notifyComplete(req.module, req.name, false, true);
		}
		
		while (!mReceivingQueue.isEmpty()) {
			Retrive ret = mReceivingQueue.poll();
			notifyComplete(ret.module, ret.name, false, false);
		}
	}

	void close() {
		close(true);
	}
	
	void close(boolean notify) {
		if (mChannel != null) {
			mChannel.close();
			mChannel = null;
			if (notify) {
				Log.d(TAG, "notfiy MSG_FILE_CHANNEL_CLOSE");
				mTransportHandler.sendEmptyMessage(TransportManager.MSG_FILE_CHANNEL_CLOSE);
			}
		}
		clean();
	}

	static class Request {
		final String module;
		final String name;
		final int length;
		final InputStream in;

		Request(String module, String name, int length, InputStream in) {
			this.module = module;
			this.name = name;
			this.length = length;
			this.in = in;
		}
	}

	static class Retrive {
		final String module;
		String name;
		final int length;

		Retrive(String module, String name, int length) {
			this.module = module;
			this.name = name;
			this.length = length;
		}
	}

//	private static final long TIMEOUT = 30 * 1000;
	public void sendFile(String module, String name, int length, InputStream in) {
		Request req = new Request(module, name, length, in);
		switch (mState) {
		case IDLE:
			if (mChannel == null) {
				try {
					mChannel = new FileServerChannel(mHandler, mContext);
					mState = PREPARING;
					mSendingQueue.clear();
//					mHandler.sendEmptyMessageDelayed(MyHandler.LISTENED_TIMEOUT_MSG, TIMEOUT);
				} catch (IOException e) {
					Log.e(TAG, "FileServerChannel listened error with :" + e.getMessage());
					notifyComplete(module, name, false, true);
					close();
					return;
				}
			} else {
				Log.w(TAG,
						"FileChannel should be null when state is IDLE in sendFile");
			}

		case PREPARING:
		case CONNECTED:
			mSendingQueue.offer(req);
			break;
			
		case DISCONNECTING:
			mSendingQueue.offer(req);
			continueWork();
			break;
		default:
			Log.e(TAG, "not support state:" + mState + " when sendFile");
		}
	}
	
	private boolean checkExternalStorageState() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}
	
	private void notifyComplete(String module, String name, boolean success, boolean isSend) {
		OnFileChannelCallBack cb = DefaultSyncManager.getDefault().getModule(module).getFileChannelCallBack();
		if (cb != null) {
			if (isSend) {
				cb.onSendComplete(name, success);
			} else {
				cb.onRetriveComplete(name, success);
			}
		} else {
			Log.w(TAG, "There is no OnFileChannelCallback found for module:"
					+ module + " and the file:" + name);
		}
	}
	
	private void sendFileChannelCloseCmd() {
		Log.d(TAG, "send FileChannelCloseCmd");
		Projo projo = new CmdProjo(SystemCmds.FILE_CHANNEL_CLOSE);
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		manager.request(new Config(TransportManager.getSystemMoudleName()), projo);
	}

	public void retriveFile(String module, String name, int length, String address) {
		if (!checkExternalStorageState()) {
			Log.w(TAG,
					"ExternalStorageState is not mounted, droping retriving file "
							+ name + " for moudle " + module);
			notifyComplete(module, name, false, false);
			sendFileChannelCloseCmd();
			return;
		} 
		
		Retrive ret = new Retrive(module, name, length);
		switch (mState) {
		case IDLE:
			if (mChannel == null) {
				try {
					mState = PREPARING;
					mChannel = new FileClientChannel(mHandler, address, mContext);
					mState = CONNECTED;
					mReceivingQueue.clear();
					mHandler.obtainMessage(MyHandler.CONNECTED_MSG)
							.sendToTarget();
				} catch (IOException e) {
					Log.e(TAG,
							"FileClientChannel connect error in retriveFile, disable FileChannelManager. e:"
									+ e.getMessage());
					notifyComplete(module, name, false, false);
					sendFileChannelCloseCmd();
					close();
					return;
				}
			} else {
				Log.w(TAG,
						"FileChannel should be null when state is IDLE in retriveFile");
			}

		case PREPARING:
		case CONNECTED:
			mReceivingQueue.offer(ret);
			break;
			
		case DISCONNECTING:
			mReceivingQueue.offer(ret);
			continueWork();
			break;
		}
	}
	
	private void continueWork() {
		mHandler.obtainMessage(MyHandler.CONNECTED_MSG).sendToTarget();
	}

}
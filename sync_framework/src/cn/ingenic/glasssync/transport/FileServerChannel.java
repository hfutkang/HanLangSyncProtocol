package cn.ingenic.glasssync.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.transport.FileChannelManager.Request;
import cn.ingenic.glasssync.transport.FileChannelManager.Retrive;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

class FileServerChannel implements FileChannel, Runnable {

	private static final String TAG = "FileServerChannel";

	private BluetoothServerSocket mServerSocket;
	private BluetoothSocket mClient;
	private Thread mServerThread;
	private boolean mClosed = false;
	private final Handler mHandler;
	private final Context mContext;

	FileServerChannel(Handler handler, Context context) throws IOException {
		mContext = context;
		mHandler = handler;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		mServerSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
				FileChannelManager.FILE_NAME, FileChannelManager.FILE_UUID);

		mServerThread = new Thread(this);
		mServerThread.start();
	}

	@Override
	public void send(Request req) {
		try {
			Log.i(TAG, "send file:" + req.name + " length:" + req.length);
			OutputStream out = mClient.getOutputStream();
			byte[] buffer = new byte[4096];
			int length = 0;
			int total = req.length;
			while (total > 0) {
				length = req.in.read(buffer);
				total -= length;
				if (total < 0) {
					Log.w(TAG,
							"File actually size is bigger than Request size for file:"
									+ req.name + " with " + -total);
					out.write(buffer, 0, -total);
				} else {
					out.write(buffer, 0, length);
				}
			}
			out.flush();

			Log.i(TAG, "file:" + req.name + " send over");
			req.in.close();
			mHandler.obtainMessage(FileChannelManager.MyHandler.SEND_OVER_MSG,
					req).sendToTarget();
		} catch (IOException e) {
			Log.e(TAG, "Exception occur in FileServerChannel.send. e:" + e.getMessage());
			LogTag.printExp(TAG, e);
			mHandler.obtainMessage(FileChannelManager.MyHandler.EXCEPTION_MSG).sendToTarget();
		}
	}

	@Override
	public void retrive(Retrive retrive) {
		try {
			InputStream in = mClient.getInputStream();
//			String dir = Environment.getDataDirectory() + "/" + retrive.module
//					+ "/";
			String fileName = retrive.name;
			String extension = "";
			int index;
			if ((index = fileName.lastIndexOf('.')) != -1) {
				extension = fileName.substring(index + 1, fileName.length());
				fileName = fileName.substring(0, index);
			}
			File dest = FileChannelManager.getUniqueDestination(
					mContext.getPackageName(), fileName, extension);
			FileOutputStream fos = new FileOutputStream(dest);
			int total = retrive.length;
			int length = 0;
			byte[] buffer = new byte[4096];
			while (total > 0) {
				if (total > 4096) {
					length = in.read(buffer);
				} else {
					length = in.read(buffer, 0, total);
				}
				fos.write(buffer, 0, length);
				total -= length;
			}
			fos.close();
			Log.i(TAG, "retrive file:" + dest + " over");
			
			mHandler.obtainMessage(FileChannelManager.MyHandler.RETRIVE_OVER_MSG,
					retrive).sendToTarget();
		} catch (IOException e) {
			Log.e(TAG, "Exception occur in FileServerChannel.retrive. e:" + e.getMessage());
			LogTag.printExp(TAG, e);
			mHandler.obtainMessage(FileChannelManager.MyHandler.EXCEPTION_MSG).sendToTarget();
		}
	}

	@Override
	public void run() {
		BluetoothSocket clnt = null;
		while (!mServerThread.isInterrupted() && !mClosed) {
			try {
				clnt = mServerSocket.accept();
				mClient = clnt;
			} catch (IOException e) {
				Log.e(TAG, "FileServerChannel accept error:" + e.getMessage());
				mHandler.obtainMessage(FileChannelManager.MyHandler.EXCEPTION_MSG,
						this).sendToTarget();
				return;
			}

			mHandler.obtainMessage(FileChannelManager.MyHandler.CONNECTED_MSG,
					this).sendToTarget();

		}
		Log.i(TAG, "FileServerChannle run end.");
	}

	@Override
	public void close() {
		mClosed = true;
		mServerThread.interrupt();
		
		try {
			if (mClient != null) {
				mClient.close();
			}
			mServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

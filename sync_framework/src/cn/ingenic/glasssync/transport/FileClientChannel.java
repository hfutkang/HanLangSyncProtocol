package cn.ingenic.glasssync.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.transport.FileChannelManager.Request;
import cn.ingenic.glasssync.transport.FileChannelManager.Retrive;

class FileClientChannel implements FileChannel {
	private static final String TAG = "FileClientChannel";
	
	private final Handler mHandler;
	private BluetoothSocket mClient;
	private final Context mContext;
	
	FileClientChannel(Handler handler, String address, Context context) throws IOException {
		mContext = context;
		mHandler = handler;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice device = adapter.getRemoteDevice(address);
		adapter.cancelDiscovery();
		mClient = device.createInsecureRfcommSocketToServiceRecord(FileChannelManager.FILE_UUID);
		
		mClient.connect();
	}
	
	@Override
	public void send(Request req) {
		try {
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
			Log.e(TAG, "Exception occur in FileClientChannel.send. e:" + e.getMessage());
			LogTag.printExp(TAG, e);
			mHandler.obtainMessage(FileChannelManager.MyHandler.EXCEPTION_MSG).sendToTarget();
		}
	}

	@Override
	public void retrive(Retrive retrive) {
		try {
			InputStream in = mClient.getInputStream();
//			String dir = "/data/data/cn.ingenic.glasssync/";
//			String dir = Environment.getDownloadCacheDirectory() + "/" + retrive.module
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
			retrive.name = dest.getAbsolutePath();
			FileOutputStream fos = new FileOutputStream(dest.getAbsolutePath());
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
			Log.e(TAG, "Exception occur in FileClientChannel.retrive. e:" + e.getMessage());
			LogTag.printExp(TAG, e);
			mHandler.obtainMessage(FileChannelManager.MyHandler.EXCEPTION_MSG).sendToTarget();
		}
	}

	@Override
	public void close() {
		try {
			if (mClient != null) {
				mClient.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

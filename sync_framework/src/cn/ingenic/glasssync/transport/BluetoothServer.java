package cn.ingenic.glasssync.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.ILocalBinder;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.LogTag.Server;
import cn.ingenic.glasssync.data.ControlProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.data.ServiceProjo;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.data.ServiceProjo.ServiceColumn;
import cn.ingenic.glasssync.utils.internal.Memory;

class BluetoothServer implements Runnable, BluetoothChannel {
	protected Handler mHandler;
	private BluetoothSocket mClient;
	protected boolean mClosed = false;
	protected boolean mAvailiable = false;
	private Thread mCurThread;
	protected final UUID mUUID;
	private final String mName;
	private final BluetoothServerSocket mServerSocket;
	private InputStream mInput;
	private OutputStream mOutput;
	
	static Object sReadLock = new Object();

	public BluetoothServer(Handler handler, String name, UUID uuid) throws IOException {
		mName = name;
		mUUID = uuid;
		mHandler = handler;
		
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		mServerSocket = adapter
				.listenUsingRfcommWithServiceRecord(mName, mUUID);
		Server.i(mName + ":listen over.");
	}

	public void send(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		Message msg = projoList.getCallbackMsg();
		projoList.reset();
		try {
			Server.d(mName + ":write start... for " + projoList.getModule());
			BluetoothClient.write(projoList, mOutput);
			Server.d(mName + ":write over...  for " + projoList.getModule());
			if (msg != null) {
				msg.arg1 = DefaultSyncManager.SUCCESS;
			}
		} catch (Exception e) {
			Server.e(mName + ":write client error:" + e.getMessage());
			LogTag.printExp(LogTag.SERVER, e);
			if (msg != null) {
				msg.arg1 = DefaultSyncManager.NO_CONNECTIVITY;
			}
			sendClientCloseMsg();
		}
		if (msg != null) {
			msg.sendToTarget();
		}
	}

	protected void sendClientCloseMsg() {
		if (!mClosed) {
			close();
			mHandler.obtainMessage(
					BluetoothChannelManager.CLIENT_CLOSE_MSG, this)
					.sendToTarget();
		}
	}

	@Override
	public void run() {
		Server.i(mName + ":server started.");
		mCurThread = Thread.currentThread();
		BluetoothSocket clnt = null;
		try {
			Server.d(mName + ":accepting...");
			clnt = mServerSocket.accept();
			mClient = clnt;
			mInput = mClient.getInputStream();
			mOutput = mClient.getOutputStream();
			Server.d(mName + ":accept one");
			
			mAvailiable = true;
			mHandler.obtainMessage(
					BluetoothChannelManager.NEW_CLIENT_MSG, this)
					.sendToTarget();
			mServerSocket.close();
			
//			synchronized (sReadLock) {
//				sReadLock.wait(3000);
//			}
			
			ProjoList projoList = null;
			while (!mCurThread.isInterrupted() && !mClosed) {
				projoList = (ProjoList) read(mInput);
				onRetrive(projoList);
				Server.d(mName + ":read projoList over for " + projoList.getModule());
			}
			Server.d(mName + ":current client quit.");
		} catch (IOException e) {
			Server.e(mName + ":Exception occurs:" + e.getMessage());
			LogTag.printExp(LogTag.SERVER, e);
			sendClientCloseMsg();
		} 
//		catch (InterruptedException e) {
//			Server.e("ReadLock timeout");
//			sendClientCloseMsg();
//		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
			
		Server.i(mName + ":server end.");
	}
	
	static Object read(InputStream in) throws IOException, ClassNotFoundException {
		byte[] s = read(4, in);
		int size = Memory.peekInt(s, 0, ByteOrder.BIG_ENDIAN);
		Log.d("read", "read size over:" + size);
		byte[] b = read(size, in);
		Log.d("read", "read byteArray over:" + b.length);
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object obj = ois.readObject();
		Log.d("read", "read obj over.");
		return obj;
	}
	
	private static byte[] read(int total, InputStream in) throws IOException {
		byte[] temp = new byte[total];
		int len = 0;
		int lastLog = 0;
		while (len < total) {
			int l = in.read(temp, len, total - len);
			if (l < 0) {
				Log.e("read", "l <= 0 l:" + l);
				throw new RuntimeException(l + " return from read()");
			} else if (l == 0) {
				Log.e("read", "read 0 size byte from BluetoothSocket!");
			} else {
				len += l;
			}
			
			if ((len - lastLog) > 8 * 1024) {
				Log.v("read", "read l:" + l + " len:" + len);
				lastLog = len;
			}
		}
		
		return temp;
	}

	@Override
	public void onRetrive(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		ControlProjo controlProjo = (ControlProjo) projoList.get(ProjoListColumn.control);
		@SuppressWarnings("unchecked")
		ArrayList<Projo> datas = (ArrayList<Projo>) projoList.get(ProjoListColumn.datas);
		Config config = Config.getConfig(controlProjo);
		if (config.mIsService && datas.size() == 1) {
			ServiceProjo serviceProjo = (ServiceProjo) datas.get(0);
			String descriptor = (String) serviceProjo.get(ServiceColumn.descriptor);
			int code = (Integer)serviceProjo.get(ServiceColumn.code);
			RemoteParcel parcel = (RemoteParcel) serviceProjo.get(ServiceColumn.parcel);
			DefaultSyncManager manager = DefaultSyncManager.getDefault();
			Module module = manager.getModule(config.mModule);
			if (config.mIsReply) {
				IRemoteBinder binder = module.getRemoteService(descriptor);
				binder.onReply(code, parcel);
			} else {
				ILocalBinder binder = module.getService(descriptor);
				RemoteParcel reply = binder.onTransact(code, parcel);

				ServiceProjo replyProjo = new ServiceProjo();
				replyProjo.put(ServiceColumn.code, code);
				replyProjo.put(ServiceColumn.descriptor, descriptor);
				replyProjo.put(ServiceColumn.parcel, reply);
				reply(config.mModule, replyProjo);
			}
		} else {
			Message msg = mHandler
					.obtainMessage(BluetoothChannelManager.RETRIVE_MSG);
			msg.obj = projoList;
			msg.sendToTarget();
		}
	}
	
	private void reply(String moduel, ServiceProjo projo) {
		ProjoList projoList = new ProjoList();
		
		Config config = new Config(moduel);
		config.mIsService = true;
		config.mIsReply = true;
		
		ArrayList<Projo> datas = new ArrayList<Projo>();
		datas.add(projo);
		
		projoList.put(ProjoListColumn.control, config.getControl());
		projoList.put(ProjoListColumn.datas, datas);
		
		send(projoList);
	}

	@Override
	public void close() {
		if (mClosed) {
			return;
		}

		Server.d(mName + ":shutdown the server");
		mClosed = true;
		mAvailiable = false;

		if (mCurThread != null) {
			mCurThread.interrupt();
		}
		try {
			if (mClient != null) {
				mClient.close();
				mClient = null;
			}
			mServerSocket.close();
		} catch (IOException e) {
			Server.d(mName + ":server close error:" + e.getMessage());
			LogTag.printExp(LogTag.CLIENT, e);
		}
	}

	@Override
	public UUID getUUID() {
		return mUUID;
	}

	@Override
	public boolean isAvailiable() {
		return mAvailiable;
	}
}

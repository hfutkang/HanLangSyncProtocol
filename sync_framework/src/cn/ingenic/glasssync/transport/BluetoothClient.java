package cn.ingenic.glasssync.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
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
import cn.ingenic.glasssync.LogTag.Client;
import cn.ingenic.glasssync.data.ControlProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.data.ServiceProjo;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.data.ServiceProjo.ServiceColumn;
import cn.ingenic.glasssync.utils.internal.Memory;

class BluetoothClient implements BluetoothChannel {

	private BluetoothSocket mSocket;
	private Handler mHandler;
	private Thread mReadThread;
	private final UUID mUUID;
	protected boolean mClosed = false;
	protected boolean mAvailiable = false;
	
	private OutputStream mOutput;
	private InputStream mInput;

	BluetoothClient(BluetoothDevice device, UUID uuid, Handler handler)
			throws IOException {
		mHandler = handler;
		mUUID = uuid;
		mSocket = device.createRfcommSocketToServiceRecord(uuid);
		mSocket.connect();
		mOutput = mSocket.getOutputStream();
		mReadThread = new Thread(mReadRunnable);
		mReadThread.start();
		
		mAvailiable = true;
	}
	
	private Runnable mReadRunnable = new Runnable() {

		@Override
		public void run() {
			Client.d("client read thread running.");

			ProjoList projoList = null;
			if (mInput == null) {
				try {
					mInput = mSocket.getInputStream();
				} catch (IOException e) {
					LogTag.printExp(LogTag.CLIENT, e);
					sendClientCloseMsg();
				}
			}
			while (!mReadThread.isInterrupted() && !mClosed) {
				try {
					projoList = (ProjoList) BluetoothServer.read(mInput);
					onRetrive(projoList);
					Client.d("read projoList over... for " + projoList.getModule());
				} catch (Throwable e) {
					Client.e("Client read error:" + e.getMessage());
					LogTag.printExp(LogTag.CLIENT, e);

					sendClientCloseMsg();
				}
			}
			Client.d("read thread quit.");
		}

	};
	
	private void sendClientCloseMsg() {
		if (!mClosed) {
			close();
			mHandler.obtainMessage(
					BluetoothChannelManager.CLIENT_CLOSE_MSG,
					BluetoothClient.this).sendToTarget();
		}
	}
	
	static void write(Object s, OutputStream d) throws IOException {
		synchronized(d) {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bao);
			oos.writeObject(s);
			byte[] b = bao.toByteArray();
			Log.d("write", "write size:" + b.length);
			byte[] dst = new byte[4];
			Memory.pokeInt(dst, 0, b.length, ByteOrder.BIG_ENDIAN);
			d.write(dst);
			Log.d("write", "write size over");
			for (int offset = 0;offset < b.length;) {
				int count = 8 * 1024;
				if ((count + offset) > b.length) {
					count = b.length - offset;
				}
				d.write(b, offset, count);
				offset += count;
				Log.v("write", "write count:" + count + " curLen:" + (offset + count));
			}
			Log.d("write", "write obj over");
			d.flush();
		}
	}
	
	public void send(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		Message msg = projoList.getCallbackMsg();
		projoList.reset();
		try {
			Client.v("write start..." + projoList.getModule());
//			write(projoList.get(ProjoList.ProjoListColumn.control), mOutput);
			write(projoList, mOutput);
			Client.v("write over.." + projoList.getModule());
			if (msg != null) {
				msg.arg1 = DefaultSyncManager.SUCCESS;
			}
		} catch (Exception e) {
			Client.e("Client send error:" + e.getMessage());
			LogTag.printExp(LogTag.CLIENT, e);
			if (msg != null) {
				msg.arg1 = DefaultSyncManager.NO_CONNECTIVITY;
			}
			sendClientCloseMsg();
		}
		if (msg != null) {
			msg.sendToTarget();
		}

	}

	public void close() {
		if (!mClosed) {
			try {
				mAvailiable = false;
				mClosed = true;
				mSocket.close();
				
				mReadThread.interrupt();
			} catch (IOException e) {
				Client.e("client close error!");
				LogTag.printExp(LogTag.CLIENT, e);
			}
		}
	}

	@Override
	public void onRetrive(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		ControlProjo control = (ControlProjo) projoList.get(ProjoListColumn.control);
		Config config = Config.getConfig(control);
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		Module module = manager.getModule(config.mModule);
		@SuppressWarnings("unchecked")
		ArrayList<Projo> datas = (ArrayList<Projo>) projoList.get(ProjoListColumn.datas);
		if (config.mIsService && datas.size() == 1) {
			ServiceProjo serviceProjo = (ServiceProjo) datas.get(0); 
			int code = (Integer) serviceProjo.get(ServiceColumn.code);
			String descriptor = (String) serviceProjo.get(ServiceColumn.descriptor);
			RemoteParcel parcel = (RemoteParcel) serviceProjo.get(ServiceColumn.parcel);
			if (!config.mIsReply) {
				ILocalBinder binder = module.getService(descriptor);
				RemoteParcel reply = binder.onTransact(code, parcel);

				ServiceProjo replyProjo = new ServiceProjo();
				replyProjo.put(ServiceColumn.code, code);
				replyProjo.put(ServiceColumn.descriptor, descriptor);
				replyProjo.put(ServiceColumn.parcel, reply);
				reply(config.mModule, replyProjo);
			} else {
				IRemoteBinder binder = module.getRemoteService(descriptor);
				binder.onReply(code, parcel);
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
	public UUID getUUID() {
		return mUUID;
	}

	@Override
	public boolean isAvailiable() {
		return mAvailiable;
	}
}
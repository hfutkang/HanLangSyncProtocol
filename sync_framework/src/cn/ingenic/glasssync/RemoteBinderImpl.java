package cn.ingenic.glasssync;

import java.util.ArrayList;

import android.util.Log;

import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.data.ServiceProjo;
import cn.ingenic.glasssync.data.ServiceProjo.ServiceColumn;

public class RemoteBinderImpl implements IRemoteBinder {

	private Object mObject = new Object();
	private final String mModule;
	private final String mDescriptor;
	private boolean mIsWaiting = false;
	
	private RemoteParcel mReply;
	
	public RemoteBinderImpl(String module, String descriptor) {
		mModule = module;
		mDescriptor = descriptor;
	}
	
	@Override
	public RemoteParcel transact(int code, RemoteParcel request) throws RemoteBinderException {
		Config config = new Config(mModule);
		config.mIsService = true;
		
		Projo projo = new ServiceProjo();
		projo.put(ServiceColumn.descriptor, mDescriptor);
		projo.put(ServiceColumn.code, code);
		projo.put(ServiceColumn.parcel, request);
		ArrayList<Projo> datas = new ArrayList<Projo>();
		datas.add(projo);
		
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		synchronized (mObject) {
			manager.request(config, datas);
		
			try {
				mIsWaiting = true;
				mObject.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return mReply;
	}

	@Override
	public void onReply(int code, RemoteParcel reply) {
		mReply = reply;
		synchronized (mObject) {
			Log.d(LogTag.APP, "Received reply, notify wating obj in transact().");
			mObject.notifyAll();
			mIsWaiting = false;
		}
	}
	
	void onConnectivityChange(boolean connect) {
		synchronized (mObject) {
			if (mIsWaiting && !connect) {
				Log.d(LogTag.APP, "Disconnected came, notify wating obj in transact().");
				mObject.notifyAll();
				mIsWaiting = false;
			}
		}
	}
}

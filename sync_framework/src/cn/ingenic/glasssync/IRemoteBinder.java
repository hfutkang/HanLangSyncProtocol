package cn.ingenic.glasssync;

import cn.ingenic.glasssync.data.RemoteParcel;

public interface IRemoteBinder {
	RemoteParcel transact(int code, RemoteParcel request) throws RemoteBinderException;
	
	void onReply(int code, RemoteParcel reply);
}

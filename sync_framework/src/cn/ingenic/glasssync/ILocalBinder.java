package cn.ingenic.glasssync;

import cn.ingenic.glasssync.data.RemoteParcel;

public interface ILocalBinder {
	RemoteParcel onTransact(int code, RemoteParcel request);
}

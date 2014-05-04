package cn.ingenic.glasssync;

import java.util.UUID;

import cn.ingenic.glasssync.transport.ext.TransportManagerExtConstants;

import android.os.Message;

public class SyncDescriptor {
	public int mPri;
	public boolean isService;
	public boolean isReply;
	public boolean isMid;
	public boolean isProjo;
	public String mModule;
	public UUID mUUID;

	public Message mCallback;

	public SyncDescriptor(String module) {
		mPri = TransportManagerExtConstants.DATA;
		mModule = module;
	}

	public SyncDescriptor(int pri, boolean service, boolean reply, boolean mid,
			boolean isProjo, String module) {
		this(pri, service, reply, mid, isProjo, module, null, null);
	}

	public SyncDescriptor(int pri, boolean service, boolean reply, boolean mid,
			boolean isProjo, String module, UUID uuid) {
		this(pri, service, reply, mid, isProjo, module, uuid, null);
	}

	public SyncDescriptor(int pri, boolean service, boolean reply, boolean mid,
			boolean projo, String module, UUID uuid, Message callback) {
		mPri = pri;
		isService = service;
		isReply = reply;
		isMid = mid;
		isProjo = projo;
		mModule = module;
		mUUID = uuid;
		mCallback = callback;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pri:").append(mPri);
		sb.append(" Mid:").append(isMid);
		sb.append(" Service:").append(isService);
		sb.append(" Reply:").append(isReply);
		sb.append(" Module:").append(mModule);
		sb.append(" Projo:").append(isProjo);
		sb.append(" Uuid:").append(mUUID);
		sb.append(" Callback:").append(mCallback != null);
		return sb.toString();
	}
}

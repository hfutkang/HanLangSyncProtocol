package cn.ingenic.glasssync.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncSerializable;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.SyncDescriptor;
import cn.ingenic.glasssync.SyncSerializable;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.transport.ext.TransportManagerExt;

public class SyncSerializableTools {
	public static SyncData serial2Data(SyncSerializable serial) {
		return serial2Data(serial, false);
	}
	
	public static SyncData serial2Data(SyncSerializable serial, boolean raw) {
		if (serial == null) {
			logw("Null serial in serial2Data.");
			return null;
		}
		
		if (raw) {
			SyncData data = new SyncData();
			data.setSerialDatas(serial.getDatas(0, serial.getLength()));
			return data;
		} else {
			return SyncDataTools.bytes2Data(serial.getDatas(0, serial.getLength()));
		}
	}

	public static SyncSerializable data2Serial(String module, SyncData data) {
		return data2Serial(module, data, null);
	}
	
	public static SyncSerializable data2Serial(String module, SyncData data, UUID uuid) {
		if (module == null || data == null) {
			logw("Null module or data in data2Serial.");
			return null;
		}
		SyncData.Config c = data.getConfig();
		SyncDescriptor des = new SyncDescriptor(TransportManagerExt.DATA,
				false, false, c == null ? false : c.mmIsMid, false, module,
				uuid, c == null ? null : c.mmCallback);
		return new DefaultSyncSerializable(des, data.getSerialDatas());
	}

	public static SyncSerializable cmd2Serial(String module, SyncData data) {
		if (module == null || data == null) {
			logw("Null module or data in data2Serial.");
			return null;
		}
		SyncData.Config c = data.getConfig();
		SyncDescriptor des = new SyncDescriptor(TransportManagerExt.CMD,
				false, false, c == null ? false : c.mmIsMid, false, module,
				null, c == null ? null : c.mmCallback);
		return new DefaultSyncSerializable(des, data.getSerialDatas());
	}

	@Deprecated
	public static SyncSerializable projoList2Serial(ProjoList p) {
		if (p == null) {
			logw("Null projoList in projoList2Serial");
			return null;
		}
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bao);
			oos.writeObject(p.get(ProjoListColumn.datas));
			final byte[] b = bao.toByteArray();
			return new DefaultSyncSerializable(config2Des(p.getConfig(), true), b);
		} catch (IOException e) {
			loge("", e);
		}

		return null;
	}

	@Deprecated
	public static ProjoList serial2ProjoList(SyncSerializable serial) {
		if (serial == null) {
			logw("Null serial in serial2ProjoList.");
			return null;
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(serial.getDatas(0, serial.getLength()));
		Object obj;
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);
			obj = ois.readObject();
			ProjoList projoList = new ProjoList();
			projoList.put(ProjoListColumn.control, des2Config(serial.getDescriptor()).getControl());
			projoList.put(ProjoListColumn.datas, (ArrayList<Projo>) obj);
			return projoList;
		} catch (Exception e) {
			loge("Exception:", e);
			return null;
		} 
	}

	@Deprecated
	public static SyncDescriptor config2Des(Config config, boolean isProjo) {
		if (config == null) {
			logw("Null config in config2Des.");
			return null;
		}
		return new SyncDescriptor(TransportManagerExt.DATA, config.mIsService,
				config.mIsReply, config.mIsMid, isProjo, config.mModule, null,
				config.mCallback);
	}

	@Deprecated
	public static Config des2Config(SyncDescriptor des) {
		if (des == null) {
			logw("Null des in des2Config.");
			return null;
		}
		Config c = new Config(des.mModule);
		c.mIsService = des.isService;
		c.mIsReply = des.isReply;
		c.mIsMid = des.isMid;
		return c;
	}
	
	public static void notifyCallback(SyncSerializable serial, int status) {
		Message msg = serial.getDescriptor().mCallback;
		if (msg != null) {
			msg.arg1 = status;
		}
	}

	private static final String PRE = "<SST>";

	private static final void loge(String msg, Throwable t) {
		Log.e(LogTag.APP, PRE + msg, t);
	}
	
	private static final void logw(String msg) {
		Log.w(LogTag.APP, PRE + msg);
	}
}

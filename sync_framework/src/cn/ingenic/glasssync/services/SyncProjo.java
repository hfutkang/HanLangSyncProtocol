package cn.ingenic.glasssync.services;

import cn.ingenic.glasssync.data.DefaultProjo;

@Deprecated
public class SyncProjo extends DefaultProjo {
	private static final long serialVersionUID = 4054301560316681502L;
	
	private static final String KEY_SERIAL = "key_serial";
	public SyncProjo(SyncData data) {
		byte[] serialDatas = data.getSerialDatas();
		put(KEY_SERIAL, serialDatas);
	}
	
	SyncData getData() {
		SyncData data = new SyncData();
		data.setSerialDatas((byte[]) get(KEY_SERIAL)); 
		return data;
	}
	
}

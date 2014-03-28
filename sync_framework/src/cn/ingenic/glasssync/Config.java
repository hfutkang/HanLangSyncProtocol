package cn.ingenic.glasssync;

import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.data.ControlProjo;
import cn.ingenic.glasssync.data.ControlProjo.ControlColumn;

public class Config {
	public final String mModule;
	
	public String mFeature;
	public boolean mIsService = false;
	public boolean mIsReply = false;
	
	public boolean mIsMid = false;
//	public int tId;
	public Message mCallback;
	
	public Config(String module) {
		this(module, module);
	}
	
	public Config(String module, boolean isMid) {
		this(module, module, isMid);
	}
	
	public Config(String module, String feature) {
		this(module, feature, false);
	}
	
	public Config(String module, String feature, boolean isMid) {
		mModule = module;
		mFeature = feature;
		mIsMid = isMid;
	}
	
	public ControlProjo getControl() {
		ControlProjo projo = new ControlProjo(mCallback);
//		projo.put(ControlColumn.tId, tId);
		projo.put(ControlColumn.module, mModule);
		projo.put(ControlColumn.feature, mFeature);
		projo.put(ControlColumn.service, mIsService);
		projo.put(ControlColumn.reply, mIsReply);
		projo.put(ControlColumn.mid, mIsMid);
		return projo;
	}
	
	public void dump(String tag) {
		StringBuilder sb = new StringBuilder("Config:\n");
		sb.append(" Module:" + mModule).append("\n");
		sb.append(" Mid:" + mIsMid).append("\n");
		sb.append(" Service:" + mIsService).append("\n");
		sb.append(" Reply:" + mIsReply).append("\n");
		Log.v(tag, sb.toString());
	}
	
	public static Config getConfig(ControlProjo projo) {
		Config config = new Config((String) projo.get(ControlColumn.module),
				(String) projo.get(ControlColumn.feature));
		config.mIsService = (Boolean) projo.get(ControlColumn.service);
		config.mIsReply = (Boolean) projo.get(ControlColumn.reply);
		config.mIsMid = (Boolean) projo.get(ControlColumn.mid);
		config.mCallback = projo.getCallbackMsg();
		// config.tId = (Integer) projo.get(ControlColumn.tId);
		return config;
	}
	
}

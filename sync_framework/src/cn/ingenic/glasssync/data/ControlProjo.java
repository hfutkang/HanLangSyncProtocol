package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import android.os.Message;

import cn.ingenic.glasssync.Column;

public class ControlProjo extends DefaultProjo {

	private static final long serialVersionUID = 848961152859103038L;
	
	private Message mCallback;

	// public ControlProjo() {
	// super(EnumSet.allOf(ControlColumn.class), ProjoType.CONTROL);
	// }

	public ControlProjo(Message callback) {
		super(EnumSet.allOf(ControlColumn.class), ProjoType.CONTROL);
		mCallback = callback;
	}

	public Message getCallbackMsg() {
		return mCallback;
	}

	void reset() {
		mCallback = null;
	}

	public String getModule() {
		return (String) get(ControlColumn.module);
	}

	public enum ControlColumn implements Column {
		// tId(Integer.class),
		module(String.class), feature(String.class), service(Boolean.class), reply(
				Boolean.class), mid(Boolean.class);

		private Class<?> mType;

		ControlColumn(Class<?> clazz) {
			mType = clazz;
		}

		@Override
		public Class<?> type() {
			return mType;
		}

		@Override
		public String key() {
			return name();
		}

	}

}

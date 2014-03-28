package cn.ingenic.glasssync.data;

import java.util.ArrayList;
import java.util.EnumSet;

import android.os.Message;

import cn.ingenic.glasssync.Column;
import cn.ingenic.glasssync.Config;

public class ProjoList extends DefaultProjo {
	private static final long serialVersionUID = -7826060579968729834L;
	public ProjoList() {
		super(EnumSet.allOf(ProjoListColumn.class), ProjoType.LIST);
	}
	
	public String getModule() {
		return ((ControlProjo) get(ProjoListColumn.control)).getModule();
	}
	
	public Message getCallbackMsg() {
		return ((ControlProjo) get(ProjoListColumn.control)).getCallbackMsg();
	}
	
	public void reset() {
		((ControlProjo) get(ProjoListColumn.control)).reset();
	}
	
	public Config getConfig() {
		ControlProjo controlProjo = ((ControlProjo) get(ProjoListColumn.control));
		return Config.getConfig(controlProjo);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Projo> getDatas() {
		return (ArrayList<Projo>) get(ProjoListColumn.datas);
	}
	
	public enum ProjoListColumn implements Column {
		control(ControlProjo.class), datas(ArrayList.class);

		private Class<?> mType;
		ProjoListColumn(Class<?> clazz) {
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

package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import cn.ingenic.glasssync.Column;

public class ModeSendCmd extends CmdProjo {

	private static final long serialVersionUID = 1484295033565299473L;
	public ModeSendCmd() {
		super(EnumSet.allOf(ModeSendColumn.class), SystemCmds.MODE_SEND);
	}
	
	public static enum ModeSendColumn implements Column {
		mode(Integer.class);
		private Class<?> mType;
		ModeSendColumn(Class<?> clazz) {
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

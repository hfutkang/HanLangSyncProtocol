package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import cn.ingenic.glasssync.Column;

public class AddressSendCmd extends CmdProjo {

	private static final long serialVersionUID = 5964898067235474829L;
	public AddressSendCmd() {
		super(EnumSet.allOf(AddressSendColumn.class), SystemCmds.ADDRESS_SEND);
	}
	
	public static enum AddressSendColumn implements Column {
		address(String.class);
		
		private Class<?> mType;
		AddressSendColumn(Class<?> clazz) {
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

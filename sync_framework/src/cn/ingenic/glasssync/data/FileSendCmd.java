package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import cn.ingenic.glasssync.Column;

public class FileSendCmd extends CmdProjo {

	private static final long serialVersionUID = -7729990878654070532L;
	public FileSendCmd() {
		super(EnumSet.allOf(FileSendCmdColumn.class), SystemCmds.FILE_SEND);
	}
	
	public static enum FileSendCmdColumn implements Column {
		module(String.class), name(String.class), length(Integer.class), address(String.class);

		private Class<?> mType;
		FileSendCmdColumn(Class<?> clazz) {
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

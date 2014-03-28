package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import cn.ingenic.glasssync.Column;

public class CmdProjo extends DefaultProjo {
	
	private static final long serialVersionUID = -2790957346234019489L;
	protected final byte mCode;

	public CmdProjo(EnumSet<? extends Column> c, byte code) {
		super(c, ProjoType.CMD);
		mCode = code;
	}
	
	public CmdProjo(byte code) {
		this(null, code);
	}
	
	public byte getCode() {
		return mCode;
	}

}

package cn.ingenic.glasssync.data;

public class SystemCmds {
	public static final byte FILE_SEND = 0x01;
	public static final byte FEATURE_CONFIG = 0x02;
	public static final byte ADDRESS_SEND = 0x03;
	public static final byte MODE_SEND = 0x04;
	public static final byte FILE_CHANNEL_CLOSE = 0x05;
	
	public static final String COL_ADDRESS = "col_address";
	public static final String COL_BOND_ADDR = "col_bond_addr";
	public static final byte ADDRESS_REQUEST = 0x06;
	
	public static final String COL_PASS_ST = "col_pass";
	public static final int ST_PASS = 0;
	public static final int ST_PASS_WITH_INIT = 1;
	public static final byte ADDRESS_RESPONSE = 0x07;
}

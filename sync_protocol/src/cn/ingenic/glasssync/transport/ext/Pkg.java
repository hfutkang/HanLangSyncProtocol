package cn.ingenic.glasssync.transport.ext;

class Pkg {
	static final String UTF_8 = "UTF-8";
	
	static final int PKG = 0x00;
	static final int CFG = 0x01;
	static final int NEG = 0x02;
	
	static final int CHANNEL_CMD = 0x00;
	static final int CHANNEL_SPEICAL = 0x01;
	static final int CHANNEL_DATA = 0x2;
	static final int CHANNEL_FILE = 0x3;
	
	static final int MAX_LEN = (1 << 14) - 1;
        static final int BIG_LEN = MAX_LEN - 10;
    	
	protected final byte[] mDatas;
	private final int mType;
	
	Pkg(int type, byte[] datas) {
		mType = type;
		mDatas = datas;
	}
	
	Pkg(byte[] datas) {
		this(PKG, datas);
	}
	
//	Pkg(byte[] datas, int pos, int len) {
//		mType = PKG;
//		mDatas = new byte[len];
//		System.arraycopy(datas, pos, mDatas, 0, len);
//	}
	
	int getType() {
		return mType;
	}
	
	byte[] getData() {
		return mDatas;
	}
}

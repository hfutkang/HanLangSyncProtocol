package cn.ingenic.glasssync.transport.ext;

class Neg extends Pkg {
	static final int MAX_VER = (1 << 7) - 1;
	static final int MAX_REASON = (1 << 7) - 1;
	static final int FAIL_ADDRESS_MISMATCH = 1;
	static final int FAIL_VERSION_MISMATCH = 2;

	static Neg fromRequest(int version, boolean isInit) {
		if (version > MAX_VER || version <= 0) {
			throw new IllegalArgumentException("Invalid version number:"
					+ version);
		}
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (isInit ? 1 << 7 : 0);
		bytes[1] = (byte) version;
		
		return new Neg(bytes);
	}

	static Neg fromResponse(boolean pass, int reason) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) 1 << 6;
		if (pass) {
			bytes[1] = (byte) (1 << 7);
		} else {
			if (reason > MAX_REASON) {
				throw new ProtocolRuntimeException("Invalid reason:" + reason);
			}
			bytes[1] = (byte) reason;
		}
		return new Neg(bytes);
	}

	Neg(byte[] datas) {
		super(NEG, datas);
	}

	boolean isACK1() {
		return (mDatas[0] & 0x40) == 0;
	}

	boolean isACK2() {
		return !isACK1();
	}

	// all from ACK1 below------------------------
	int getVersion() {
		return isACK1() ? (0xff & mDatas[1]) : -1;
	}

	// non serializable
	private String mAddr;

	String getAddr() {
		return mAddr;
	}

	void setAddr(String addr) {
		mAddr = addr;
	}
	
	boolean isInit() {
		return isACK1() && ((mDatas[0] & 0x80) != 0);
	}

	// all from ACK2 below-----------------------
	boolean isPass() {
		return isACK2() && (((mDatas[1] & 0xff) >> 7) == 1);
	}

	int getReason() {
		return isACK2() ? (mDatas[1] & 0x7f) : -1;
	}
}

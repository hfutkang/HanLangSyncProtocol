package cn.ingenic.glasssync.transport.ext;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

class Cfg extends Pkg {
	static final int MAX_LEN = 36;
	static final int MODULE_LEN = 15;

	static final int FLAG_SERVICE = 1 << 5;
	static final int FLAG_REPLY = 1 << 4;
	static final int FLAG_MID = 1 << 3;
	static final int FLAG_PROJO = 1 << 2;

	static final int POS_MOST_SIG_BITS = 20;
	static final int POS_LEAST_SIG_BITS = 28;

	Cfg(byte[] datas) {
		super(CFG, datas);
	}

	int getPriority() {
		return (mDatas[0] & 0xff) >>> 6;
	}

	boolean isService() {
		return (mDatas[0] & FLAG_SERVICE) > 0;
	}

	boolean isReply() {
		return (mDatas[0] & FLAG_REPLY) > 0;
	}
	
	boolean isProjo() {
		return (mDatas[0] & FLAG_PROJO) > 0;
	}

	boolean isMid() {
		return (mDatas[0] & FLAG_MID) > 0;
	}

	private long readLong(int pos) {
		long temp = 0;
		long res = 0;
		for (int i = 0; i < 8; i++) {
			res <<= 8;
			temp = mDatas[pos++] & 0xff;
			res |= temp;
		}
		return res;
//		long l = 0;
//		l |= (long) (((mDatas[pos++] & 0xff) << 56) & 0xff00000000000000l);
//		l |= (long) (((mDatas[pos++] & 0xff) << 48) & 0x00ff000000000000l);
//		l |= (long) (((mDatas[pos++] & 0xff) << 40) & 0x0000ff0000000000l);
//		l |= (long) (((mDatas[pos++] & 0xff) << 32) & 0x000000ff00000000l);
//		l |= (long) (((mDatas[pos++] & 0xff) << 24) & 0x00000000ff000000l);
//		l |= (long) ((mDatas[pos++] & 0xff) << 16);
//		l |= (long) ((mDatas[pos++] & 0xff) << 8);
//		l |= (long) ((mDatas[pos++] & 0xff));
//		return l;
	}

	UUID getUUID() {
		long most = readLong(POS_MOST_SIG_BITS);
		long least = readLong(POS_LEAST_SIG_BITS);
		if (most == 0 && least == 0) {
			return null;
		} else {
			return new UUID(most, least);
		}
	}

	String getModule() throws UnsupportedEncodingException {
		byte[] bytes = new byte[MODULE_LEN];
		System.arraycopy(mDatas, 1, bytes, 0, MODULE_LEN);
		int lastIndex = -1;
		for (int i = bytes.length - 1;i > 0;i--) {
			if (bytes[i] != 0) {
				lastIndex = i;
				break;
			}
		}
		return new String(bytes, 0, lastIndex + 1, UTF_8);
	}

	int getDatasCount() {
		byte[] bytes = new byte[4];
		System.arraycopy(mDatas, 16, bytes, 0, 4);
		int i = 0;
		i |= (bytes[0] & 0xff) << 24;
		i |= (bytes[1] & 0xff) << 16;
		i |= (bytes[2] & 0xff) << 8;
		i |= (bytes[3] & 0xff);
		return i;
	}
}

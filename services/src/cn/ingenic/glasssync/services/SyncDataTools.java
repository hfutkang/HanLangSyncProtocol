package cn.ingenic.glasssync.services;

import java.io.UnsupportedEncodingException;

import android.util.Log;

public final class SyncDataTools {
	private static String CHINESE_CODE = "UTF-8";

	static final byte NULL = -1;
	static final byte BOOLEAN = 0x0;
	static final byte BYTE = 0x1;
	static final byte SHORT = 0x2;
	static final byte INT = 0x3;
	static final byte LONG = 0x4;
	static final byte FLOAT = 0x5;
	static final byte DOUBLE = 0x6;
//	static final byte CHAR = 0x7;
	static final byte STRING = 0x8;
	static final byte BOOLEAN_ARY = 0x9;
	static final byte BYTE_ARY = 0xa;
	static final byte SHORT_ARY = 0xb;
	static final byte INT_ARY = 0xc;
	static final byte LONG_ARY = 0xd;
	static final byte FLOAT_ARY = 0xe;
	static final byte DOUBLE_ARY = 0xf;
	static final byte CHAR_ARY = 0x10;
	static final byte STRING_ARY = 0x11;
	static final byte SYNCDATA_ARY = 0x12;

	static final int INT_LENGTH = 4;
	// static final int CHAR_LENGTH=2;
	static final int BOOLEAN_LENGTH = 1;
	static final int LONG_LENGTH = 8;
	static final int SHORT_LENGTH = 2;
	static final int FLOAT_LENGTH = 4;
	static final int DOUBLE_LENGTH = 8;
	static final int BYTE_LENGTH = 1;

	static class SyncDataEncoderException extends Exception {

		private static final long serialVersionUID = 2915351385159757644L;

		SyncDataEncoderException(String msg) {
			super(msg);
		}
	}

	static class SyncDataEncoder {
		private final SyncData mData;
		private byte[] mBytes;
		private int mPos = 0;
		private int mCap = 4 * 1024;

		SyncDataEncoder(SyncData data) {
			mData = data;
			mBytes = new byte[mCap];
		}

		byte[] encode() {
			if (mData == null) {
				logw("Null SyncData in SyncDataEncoder, do nothing.");
				return null;
			}
			try {
				writeInt(mData.keySet().size());
				for (String key : mData.keySet()) {
					Object val = mData.get(key);
					//fillBytes(key);
					writeString(key);
					writeObj(val);
				}
			} catch (Exception e) {
				loge("Exception:", e);
			}
			byte[] result = new byte[mPos];
			System.arraycopy(mBytes, 0, result, 0, mPos);
			return result;
		}

		private void writeObj(Object val) throws SyncDataEncoderException,
				UnsupportedEncodingException {
			if (val == null) {
				logw("fillBytes(null) occurs");
				writeByte(NULL);
			} else if (val instanceof Byte) {
				writeByte(BYTE);
				writeByte((Byte) val);
			} else if (val instanceof Integer) {
				writeByte(INT);
				writeInt((Integer) val);
			} else if (val instanceof String) {
				writeByte(STRING);
				writeString((String) val);
			} else if (val instanceof Boolean) {
				writeByte(BOOLEAN);
				writeBoolean((Boolean) val);
			} else if (val instanceof SyncData[]) {
				writeByte(SYNCDATA_ARY);
				writeSyncDataArray((SyncData[]) val);
			} else if (val instanceof byte[]) {
				writeByte(BYTE_ARY);
				writeInt(((byte[]) val).length);
				writeByteArray((byte[]) val);
			}  else if (val instanceof Long) {
				writeByte(LONG);
				writeLong((Long) val);
			} else if (val instanceof Short) {
				writeByte(SHORT);
				writeShort((Short) val);
			} else if (val instanceof Float) {
				writeByte(FLOAT);
				writeFloat((Float) val);
			} else if (val instanceof Double) {
				writeByte(DOUBLE);
				writeDouble((Double) val);
			} else if (val instanceof int[]) {
				writeByte(INT_ARY);
				writeInt(((int[]) val).length);
				writeIntArray((int[]) val);
			} else if (val instanceof String[]) {
				writeByte(STRING_ARY);
				writeInt(((String[]) val).length);
				writeStringArray((String[]) val);
			} else if (val instanceof boolean[]) {
				writeByte(BOOLEAN_ARY);
				writeInt(((boolean[]) val).length);
				writeBooleanArray((boolean[]) val);
			} else if (val instanceof long[]) {
				writeByte(LONG_ARY);
				writeInt(((long[]) val).length);
				writeLongArray((long[]) val);
			} else if (val instanceof short[]) {
				writeByte(SHORT_ARY);
				writeInt(((short[]) val).length);
				writeShortArray((short[]) val);
			} else if (val instanceof float[]) {
				writeByte(FLOAT_ARY);
				writeInt(((float[]) val).length);
				writeFloatArray((float[]) val);
			} else if (val instanceof double[]) {
				writeByte(DOUBLE_ARY);
				writeDoubleArray((double[]) val);
			}
//			else if (val instanceof Character) {
//				writeByte(CHAR);
//				writeChar((Character) val);
//			} 
			
//			else if (val instanceof char[]) {
//				writeByte(CHAR_ARY);
//				writeCharArray((char[]) val);
//			}
		}

		private void writeSyncDataArray(SyncData[] dataArray)
				throws SyncDataEncoderException {
			writeInt(dataArray.length);
			for (SyncData data : dataArray) {
				byte[] oneBytes = SyncDataTools.data2Bytes(data);
				if (oneBytes == null) {
					loge("one Sync data is null");
					continue;
				}
				writeInt(oneBytes.length);
				writeOneSyncData(oneBytes);

			}
		}

		private void writeOneSyncData(byte[] oneBytes)
				throws SyncDataEncoderException {

			growDataPossiblly(oneBytes.length);
			System.arraycopy(oneBytes, 0, mBytes, mPos, oneBytes.length);
			mPos = mPos + oneBytes.length;
		}

		private void writeStringArray(String[] sArray)
				throws SyncDataEncoderException, UnsupportedEncodingException {
			int length = sArray.length;
			byte[] tArray = null;
			for (int i = 0; i < length; i++) {
				String s = sArray[i];
				byte[] sbArray = s.getBytes(CHINESE_CODE);
				writeInt(sbArray.length);
				System.arraycopy(sbArray, 0, mBytes, mPos, sbArray.length);
				mPos += sbArray.length;

			}
			if (tArray == null) {
				loge("writeStringArray totle byte array is null !!!!!");
				return;
			}

		}

//		private void writeCharArray(char[] cArray)
//				throws SyncDataEncoderException {
//
//			byte[] charArray = getBytes(cArray);
//			writeInt(charArray.length);
//			growDataPossiblly(charArray.length);
//			System.arraycopy(charArray, 0, mBytes, mPos, charArray.length);
//
//			mPos += charArray.length;
//		}

		private void writeDoubleArray(double[] dArray)
				throws SyncDataEncoderException {
			int length = dArray.length;
			writeInt(length);
			long[] newLArray = new long[length];
			for (int i = 0; i < length; i++) {
				newLArray[i] = Double.doubleToLongBits(dArray[i]);
			}
			writeLongArray(newLArray);
		}

		private void writeFloatArray(float[] fArray)
				throws SyncDataEncoderException {
			int[] newIArray = new int[fArray.length];
			for (int i = 0; i < fArray.length; i++) {
				newIArray[i] = Float.floatToIntBits(fArray[i]);
			}
			writeIntArray(newIArray);
		}

		private void writeLongArray(long[] lArray)
				throws SyncDataEncoderException {
			byte[] newLArray = new byte[8 * lArray.length];
			for (int i = 0; i < lArray.length; i++) {
				long l = lArray[i];
				newLArray[8 * i] = (byte) (l >> 56);
				newLArray[8 * i + 1] = (byte) (l >> 48);
				newLArray[8 * i + 2] = (byte) (l >> 40);
				newLArray[8 * i + 3] = (byte) (l >> 32);
				newLArray[8 * i + 4] = (byte) (l >> 24);
				newLArray[8 * i + 5] = (byte) (l >> 16);
				newLArray[8 * i + 6] = (byte) (l >> 8);
				newLArray[8 * i + 7] = (byte) l;
			}
			growDataPossiblly(newLArray.length);

			System.arraycopy(newLArray, 0, mBytes, mPos, newLArray.length);
			mPos += newLArray.length;
		}

		private void writeIntArray(int[] iArray)
				throws SyncDataEncoderException {
			byte[] newIArray = new byte[iArray.length * 4];
			for (int c = 0; c < iArray.length; c++) {
				int i = iArray[c];
				newIArray[4 * c] = (byte) (i >> 24);
				newIArray[4 * c + 1] = (byte) (i >> 16);
				newIArray[4 * c + 2] = (byte) (i >> 8);
				newIArray[4 * c + 3] = (byte) i;
			}
			growDataPossiblly(newIArray.length);
			System.arraycopy(newIArray, 0, mBytes, mPos, newIArray.length);
			mPos = mPos + newIArray.length;
		}

		private void writeShortArray(short[] sArray)
				throws SyncDataEncoderException {
			int length = sArray.length;
			byte[] newSArray = new byte[2 * length];
			for (int i = 0; i < length; i++) {
				short s = sArray[i];
				newSArray[i * 2] = (byte) (s >> 8);
				newSArray[i * 2 + 1] = (byte) s;
			}
			growDataPossiblly(newSArray.length);
			System.arraycopy(newSArray, 0, mBytes, mPos, newSArray.length);
			mPos = mPos + newSArray.length;
		}

		private void writeBooleanArray(boolean[] bArray)
				throws SyncDataEncoderException {
			int length = bArray.length;
			byte[] newBArray = new byte[length];
			for (int i = 0; i < length; i++) {
				if (bArray[i]) {
					newBArray[i] = 1;
				} else {
					newBArray[i] = 0;
				}
			}
			growDataPossiblly(length);
			System.arraycopy(newBArray, 0, mBytes, mPos, length);
			mPos = mPos + length;
		}

		private void writeByteArray(byte[] bArray)
				throws SyncDataEncoderException {
			int length = bArray.length;
			growDataPossiblly(length);
			System.arraycopy(bArray, 0, mBytes, mPos, length);
			mPos = mPos + length;
		}

//		private void writeChar(char c) throws SyncDataEncoderException {
//
//			byte[] charArray = getBytes(c);
//			writeInt(charArray.length);
//			growDataPossiblly(charArray.length);
//
//			System.arraycopy(charArray, 0, mBytes, mPos, charArray.length);
//			mPos += charArray.length;
//		}

		private void writeString(String s) throws SyncDataEncoderException,
				UnsupportedEncodingException {
			byte[] sArray = null;
			sArray = s.getBytes(CHINESE_CODE);
			writeInt(sArray.length);
			growDataPossiblly(sArray.length);
			System.arraycopy(sArray, 0, mBytes, mPos, sArray.length);
			mPos += sArray.length;
		}

		private void writeDouble(double d) throws SyncDataEncoderException {

			writeLong(Double.doubleToLongBits(d));
		}

		private void writeByte(byte b) throws SyncDataEncoderException {
			growDataPossiblly(1);
			mBytes[mPos++] = b;
		}

		private void writeInt(int i) throws SyncDataEncoderException {
			growDataPossiblly(4);
			mBytes[mPos++] = (byte) (i >> 24);
			mBytes[mPos++] = (byte) (i >> 16);
			mBytes[mPos++] = (byte) (i >> 8);
			mBytes[mPos++] = (byte) i;
		}

		private void writeFloat(float f) throws SyncDataEncoderException {
			writeInt(Float.floatToIntBits(f));
		}

		private void writeBoolean(boolean b) throws SyncDataEncoderException {
			growDataPossiblly(1);
			if (b) {
				mBytes[mPos++] = (byte) 1;
			} else {
				mBytes[mPos++] = (byte) 0;
			}
		}

		private void writeShort(short s) throws SyncDataEncoderException {
			growDataPossiblly(2);
			mBytes[mPos++] = (byte) (s >> 8);
			mBytes[mPos++] = (byte) s;
		}

		private void writeLong(long l) throws SyncDataEncoderException {
			growDataPossiblly(8);
			mBytes[mPos++] = (byte) (l >> 56);
			mBytes[mPos++] = (byte) (l >> 48);
			mBytes[mPos++] = (byte) (l >> 40);
			mBytes[mPos++] = (byte) (l >> 32);
			mBytes[mPos++] = (byte) (l >> 24);
			mBytes[mPos++] = (byte) (l >> 16);
			mBytes[mPos++] = (byte) (l >> 8);
			mBytes[mPos++] = (byte) l;
		}

//		private byte[] getBytes(char[] chars) {
//			Charset cs = Charset.forName(CHINESE_CODE);
//			CharBuffer cb = CharBuffer.allocate(chars.length);
//			cb.put(chars);
//			cb.flip();
//			ByteBuffer bb = cs.encode(cb);
//
//			return bb.array();
//		}
//
//		private byte[] getBytes(char c) {
//			Charset cs = Charset.forName(CHINESE_CODE);
//			CharBuffer cb = CharBuffer.allocate(1);
//			cb.put(c);
//			cb.flip();
//			ByteBuffer bb = cs.encode(cb);
//
//			return bb.array();
//		}

		private void growDataPossiblly(int len) throws SyncDataEncoderException {
			if ((mPos + len) > mCap) {
				int newSize = ((mPos + len) * 3) / 2;
				if (newSize <= mPos) {
					throw new SyncDataEncoderException(
							"Can not growData with mPos:" + mPos
									+ " desire_len:" + len);
				}
				mCap = newSize;
				byte[] newByte = new byte[newSize];
				System.arraycopy(mBytes, 0, newByte, 0, mBytes.length);
				mBytes = newByte;
			}
		}

	}

	static class SyncDataBytesDecoder {

		private final static int TRUE = 1;
		private byte[] mOneBytes;
		private int mPos = 0;

		public SyncDataBytesDecoder(byte[] oneSyncDataBytes) {
			mOneBytes = oneSyncDataBytes;

		}

		public SyncData decode() {
			logi("parse oneBytes length is :" + mOneBytes.length);
			SyncData data = new SyncData();
			try {
				int keyCount = readInt();
				for (int i = 0; i < keyCount; i++) {
//					byte key_type = readByte();
					String key = readString();
					Object value = readObj();
					data.put(key, value);
					//getSyncData(data, key, value);
				}
			} catch (UnsupportedEncodingException e) {
				loge("UnsupportedEncodingException", e);
			}
			return data;
		}

		/*private SyncData getSyncData(SyncData data, String key, Object val) {
			if (val == null) {
			} else if (val instanceof String) {
				data.putString(key, (String) val);
			} else if (val instanceof Integer) {
				data.put(key, (Integer) val);
			} else if (val instanceof Double[]) {
				data.putDoubleArray(key, (double[]) val);
			} else if (val instanceof Short) {
				data.put(key, (Short) val);
			} else if (val instanceof Long) {
				data.putLong(key, (Long) val);
			} else if (val instanceof Float) {
				data.put(key, (Float) val);
			} else if (val instanceof Double) {
				data.put(key, (Double) val);
			} else if (val instanceof Boolean) {
				data.putBoolean(key, (Boolean) val);
			} else if (val instanceof Character) {
				data.put(key, (Character) val);
			} else if (val instanceof boolean[]) {
				data.putBooleanArray(key, (boolean[]) val);
			} else if (val instanceof byte[]) {
				data.putByteArray(key, (byte[]) val);
			} else if (val instanceof String[]) {
				data.putStringArray(key, (String[]) val);
			} else if (val instanceof CharSequence[]) {
				data.putCharArray(key, (char[]) val);
			} else if (val instanceof int[]) {
				data.put(key, (int[]) val);
			} else if (val instanceof long[]) {
				data.putLongArray(key, (long[]) val);
			} else if (val instanceof Byte) {
				data.putByte(key, (Byte) val);
			} else if (val instanceof short[]) {
				data.putShortArray(key, (short[]) val);
			} else if (val instanceof float[]) {
				data.put(key, (float[]) val);
			} else if (val instanceof SyncData[]) {
				data.putDataArray(key, (SyncData[]) val);
			}
			return data;
		}*/
		
		private Object readObj() throws UnsupportedEncodingException {
			byte type = readByte();
			if (type == NULL) {
				return null;
			} else if (type == BYTE) {
				return readByte();
			} else if (type == INT) {
				return readInt();
			} else if (type == STRING) {
				return readString();
			} else if (type == BOOLEAN) {
				return readBoolean();
			} else if (type == SYNCDATA_ARY) {
				return readSyncDataArray();
			} else if (type == BYTE_ARY) {
				return readByteArray();
			} else if (type == LONG) {
				return readLong();
			} else if (type == SHORT) {
				return readShort();
			} else if (type == FLOAT) {
				return readFloat();
			} 
			/*else if (type == CHAR) {
				int l = (Integer) parseTypeValue(INT);
				byte[] b = getChildBytes(mPos, mPos + l - 1);
				char c = readChar(b);
				mPos += l;
				return c;
			} */
			else if (type == DOUBLE) {
				return readDouble();
			} else if (type == INT_ARY) {
				return readIntArray();
			} else if (type == STRING_ARY) {
				return readStringArray();
			} else if (type == BOOLEAN_ARY) {
				return readBooleanArray();
			} 
//			else if (type == CHAR_ARY) {
//				return readCharArray();
//			} 
			else if (type == LONG_ARY) {
				return readLongArray();
			} else if (type == SHORT_ARY) {
				return readShortArray();
			} else if (type == FLOAT_ARY) {
				return readFloatArray();
			} else if (type == DOUBLE_ARY) {
				return readDoubleArray();
			} 
			loge("unknow type:" + type);
			return null;
		}

		private byte[] getChildBytes(int start, int end) {
			byte[] des = new byte[end - start];
			System.arraycopy(mOneBytes, start, des, 0, des.length);
			return des;
		}

		private boolean readBoolean() {
			if (mOneBytes[mPos++] == TRUE) {
				return true;
			}
			return false;
		}

		private int readInt() {
			byte[] b = getChildBytes(mPos, mPos + INT_LENGTH);
			int i = (b[0] << 24) & 0xff000000;
			i |= (b[1] << 16) & 0x00ff0000;
			i |= (b[2] << 8) & 0x0000ff00;
			i |= b[3] & 0xff;
			mPos += INT_LENGTH;
			return i;
		}

		private byte readByte() {
			return mOneBytes[mPos++];
		}

		private short readShort() {
			return (short) ((mOneBytes[mPos++] & 0xff << 8) | (mOneBytes[mPos++] & 0xff));
		}

		private long readLong() {
			long temp = 0;
			long res = 0;
			for (int i = 0; i < LONG_LENGTH; i++) {
				res <<= 8;
				temp = mOneBytes[mPos++] & 0xff;
				res |= temp;
			}
			return res;
		}

		private float readFloat() {
			return Float.intBitsToFloat(readInt());
		}

		private double readDouble() {
			return Double.longBitsToDouble(readLong());
		}

//		private char readChar(byte[] b) {
//			return getChars(b)[0];
//		}

		private String readString() throws UnsupportedEncodingException {
			int length = readInt();
			byte[] stringArray = getChildBytes(mPos, mPos + length);
			String s = new String(stringArray, CHINESE_CODE);
			mPos += length;
			return s;
		}

		private boolean[] readBooleanArray()
				throws UnsupportedEncodingException {
			int l = readInt();
			boolean[] booleanArray = new boolean[l];
			byte[] booleanBytes = getChildBytes(mPos, mPos + l);
			mPos += l;
			for (int i = 0; i < l; i++) {
				if (booleanBytes[i] == TRUE) {
					booleanArray[i] = true;
				} else {
					booleanArray[i] = false;
				}
			}
			return booleanArray;
		}

		private short[] readShortArray() throws UnsupportedEncodingException {
			int count = readInt();
			short[] shortArray = new short[count];
			for (int i = 0; i < count; i++) {
				shortArray[i] = readShort();
			}
			return shortArray;
		}

		private byte[] readByteArray() throws UnsupportedEncodingException {
			int count = readInt();
			byte[] byteArray = new byte[count];
			System.arraycopy(mOneBytes, mPos, byteArray, 0, count);
			mPos += count;
			return byteArray;
		}

		private int[] readIntArray() throws UnsupportedEncodingException {
			int count = readInt();
			int[] intArray = new int[count];
			for (int i = 0; i < count; i++) {
				intArray[i] = readInt();
			}
			return intArray;
		}

		// private int readCount(int count_start){
		// int count=readInt(getChildBytes(count_start));
		// System.out.println("readcount count is :"+count);
		// return count;
		// }

//		private char[] getChars(byte[] bytes) {
//			Charset cs = Charset.forName(CHINESE_CODE);
//			ByteBuffer bb = ByteBuffer.allocate(bytes.length);
//			bb.put(bytes);
//			bb.flip();
//			CharBuffer cb = cs.decode(bb);
//			return cb.array();
//		}

		private long[] readLongArray() throws UnsupportedEncodingException {
			int count = readInt();
			long[] longArray = new long[count];
			for (int i = 0; i < count; i++) {
				longArray[i] = readLong();
			}
			return longArray;
		}

		private float[] readFloatArray() throws UnsupportedEncodingException {
			int count = readInt();
			float[] fArray = new float[count];
			for (int i = 0; i < count; i++) {
				fArray[i] = readFloat();
			}
			return fArray;
		}

		private double[] readDoubleArray() throws UnsupportedEncodingException {
			int count = readInt();
			double[] doubleArray = new double[count];
			for (int i = 0; i < count; i++) {
				doubleArray[i] = readDouble();
			}
			return doubleArray;
		}

//		private char[] readCharArray() throws UnsupportedEncodingException {
//			int l = (Integer) parseTypeValue(INT);
//			System.out.println("readCharArray count is :" + l);
//			byte[] b = getChildBytes(mPos, mPos + l - 1);
//			char[] c = getChars(b);
//			mPos += l;
//			for (char cc : c) {
//				System.out.println("readCharArray cc is :" + cc);
//			}
//
//			return c;
//		}

		private String[] readStringArray() throws UnsupportedEncodingException {
			int count = readInt();
			String[] stringArray = new String[count];
			for (int i = 0; i < count; i++) {
				stringArray[i] = readString();
			}
			return stringArray;
		}

		private SyncData[] readSyncDataArray()
				throws UnsupportedEncodingException {
			int count = readInt();
			SyncData[] dataArray = new SyncData[count];
			for (int i = 0; i < count; i++) {
				int l = readInt();
				byte[] oneDataBytes = getChildBytes(mPos, mPos + l);
				mPos += l;
				dataArray[i] = SyncDataTools.bytes2Data(oneDataBytes);
			}
			return dataArray;
		}
	}

	static byte[] data2Bytes(SyncData data) {
		return new SyncDataEncoder(data).encode();
	}

	static SyncData bytes2Data(byte[] dataBytes) {
		return new SyncDataBytesDecoder(dataBytes).decode();
	}

	private static final String PRE = "<SDT>";

	private static void logw(String msg) {
		Log.w(Constants.TAG, PRE + msg);
	}
	
	private static void logi(String msg) {
		Log.i(Constants.TAG, PRE + msg);
	}

	private static void loge(String msg) {
		Log.e(Constants.TAG, PRE + msg);
	}
	
	private static void loge(String msg, Throwable t) {
		Log.e(Constants.TAG, PRE + msg, t);
	}
}
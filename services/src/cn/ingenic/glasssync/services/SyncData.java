package cn.ingenic.glasssync.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class SyncData implements Parcelable {
	private static final String TAG = "SyncData";
	
	public static final long INIT_SORT = 0l;
	public static final long INVALID_SORT = -1l; 
	
	static final String KEY_TOTAL_LEN = "key_total_len";
	//static final String KEY_POS = "key_pos";
	static final int MAX_LEN_PER_DATA = 996;
	
	private Config mConfig;
	private Map<String, Object> mValues = new HashMap<String, Object>();
	private boolean mFlowDirrect = true;
	private byte[] mSerialDatas = null;
	
	void setFlowDirrect(boolean dirrect) {
		mFlowDirrect = dirrect;
	}
	
	void setSerialDatas(byte[] datas) {
		mSerialDatas = datas;
	}
	
	byte[] getSerialDatas() {
		if (mSerialDatas == null) {
			mSerialDatas = SyncDataTools.data2Bytes(this);
		}
		return mSerialDatas;
	}
	
	void retain() {
		if (!mFlowDirrect && mSerialDatas != null) {
			SyncData data = SyncDataTools.bytes2Data(mSerialDatas);
			mValues = data.mValues;
		}
	}
	
	public static class Config {
		
		//parcelable
		long mmSort = INVALID_SORT;
		
		public final boolean mmIsMid;
		
		//non parcelable
		public Message mmCallback;
		
		public Config() {
			this(false);
		}
		
		public Config(boolean isMid) {
			mmIsMid = isMid;
		}
		
		//method
		public long getSort() {
			return mmSort;
		}
	}
	
	private static Config readConfigFromParcel(Parcel in) {
		long sort = in.readLong();
		if (sort == INVALID_SORT) {
			boolean isMid = (in.readInt() == 1);
			return isMid ? new Config(isMid) : null;
		}
		
		Config config = new Config(in.readInt() == 1);
		config.mmSort = sort;
		
		return config;
	}
	
	private void writeConfigToParcel(Parcel dest, Config config) {
		if (config == null) {
			dest.writeLong(INVALID_SORT);
			// isMid
			dest.writeInt(0);
			return;
		}
		
		dest.writeLong(config.mmSort);
		dest.writeInt(config.mmIsMid ? 1 : 0);
	}

	public static final Parcelable.Creator<SyncData> CREATOR = new Parcelable.Creator<SyncData>() {
		public SyncData createFromParcel(Parcel in) {
			Config config = readConfigFromParcel(in);
			boolean flowDirrect = in.readInt() == 1;
			int len = in.readInt();
			byte[] datas = null;
			if (len != -1) {
				datas = new byte[len];
			} else {
				loge("No serial data found when createFrom Parcel.");
				return null;
			}
			in.readByteArray(datas);
			SyncData data;
			if (flowDirrect) {
				data = new SyncData();
				data.setSerialDatas(datas);
			} else {
				data = SyncDataTools.bytes2Data(datas);
			}
			data.mConfig = config;
			return data;
		}

		public SyncData[] newArray(int size) {
			return new SyncData[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		writeConfigToParcel(dest, mConfig);
		dest.writeInt(mFlowDirrect ? 1 : 0);
		byte[] datas = getSerialDatas();
		
		if (datas != null) {
			dest.writeInt(datas.length);
			dest.writeByteArray(datas);
		} else {
			loge("No serial data found when writeToParcel.");
			dest.writeInt(-1);
		}
	}
	
	public void setConfig(Config config) {
		mConfig = config;
	}
	
	public Config getConfig() {
		return mConfig;
	}
	
	public void putBoolean(String key, boolean b) {
		mValues.put(key, b);
	}
	
	public void putBooleanArray(String key, boolean[] array) {
		mValues.put(key, array);
	}
	
	public void putByte(String key, byte value) {
		mValues.put(key, value);
    }
	
	public void putByteArray(String key, byte[] value) {
		mValues.put(key, value);
    }
	
//	public void putChar(String key, char value) {
//		mValues.put(key, value);
//    }
//	
//	public void putCharArray(String key, char[] value) {
//		mValues.put(key, value);
//    }
	
	public void putDouble(String key, double value) {
		mValues.put(key, value);
    }
	
	public void putDoubleArray(String key, double[] value) {
		mValues.put(key, value);
    }
	
	public void putFloat(String key, float value) {
		mValues.put(key, value);
    }
	
	public void putFloatArray(String key, float[] value) {
		mValues.put(key, value);
    }
	
	public void putInt(String key, int value) {
		mValues.put(key, value);
    }
	
	public void putIntArray(String key, int[] value) {
		mValues.put(key, value);
    }
	
	public void putLong(String key, long value) {
		mValues.put(key, value);
    }
	
	public void putLongArray(String key, long[] value) {
		mValues.put(key, value);
    }
	
	public void putShort(String key, short value) {
		mValues.put(key, value);
    }
	
	public void putShortArray(String key, short[] value) {
		mValues.put(key, value);
    }
	
	public void putString(String key, String value) {
		mValues.put(key, value);
    }
	
	public void putStringArray(String key, String[] value) {
		mValues.put(key, value);
    }
	
	public void putDataArray(String key, SyncData[] value) {
		if (value != null) {
			for (SyncData data : value) {
				if (data != null) {
					data.mConfig = null;
					
				}
			}
		}
//		mExcludeKey = key;
//		mDatas = value;
		mValues.put(key, value);
	}
	
	private void typeWarning(String key, Object value, String className,
	        ClassCastException e) {
	        typeWarning(key, value, className, "<null>", e);
	}
	
	private void typeWarning(String key, Object value, String className,
	        Object defaultValue, ClassCastException e) {
	        StringBuilder sb = new StringBuilder();
	        sb.append("Key ");
	        sb.append(key);
	        sb.append(" expected ");
	        sb.append(className);
	        sb.append(" but value was a ");
	        sb.append(value.getClass().getName());
	        sb.append(".  The default value ");
	        sb.append(defaultValue);
	        sb.append(" was returned.");
	        Log.w(TAG, sb.toString());
	        Log.w(TAG, "Attempt to cast generated internal exception:", e);
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		Object o = mValues.get(key);
		if (o == null) {
			return defaultValue;
		}
		
		try {
			return (Boolean) o;
		} catch (ClassCastException e) {
			typeWarning(key, o, "Boolean", defaultValue, e);
			return defaultValue;
		}
	}
	
	public boolean[] getBooleanArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (boolean[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "byte[]", e);
            return null;
        }
    }
	
	public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public Byte getByte(String key, byte defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Byte) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Byte", defaultValue, e);
            return defaultValue;
        }
    }
    
    public byte[] getByteArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (byte[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "byte[]", e);
            return null;
        }
    }
    
//    public char getChar(String key) {
//        return getChar(key, (char) 0);
//    }
//
//    public char getChar(String key, char defaultValue) {
//        Object o = mValues.get(key);
//        if (o == null) {
//            return defaultValue;
//        }
//        try {
//            return (Character) o;
//        } catch (ClassCastException e) {
//            typeWarning(key, o, "Character", defaultValue, e);
//            return defaultValue;
//        }
//    }
//    
//    public char[] getCharArray(String key) {
//        Object o = mValues.get(key);
//        if (o == null) {
//            return null;
//        }
//        try {
//            return (char[]) o;
//        } catch (ClassCastException e) {
//            typeWarning(key, o, "char[]", e);
//            return null;
//        }
//    }
    
    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Double) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Double", defaultValue, e);
            return defaultValue;
        }
    }
    
    public double[] getDoubleArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (double[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "double[]", e);
            return null;
        }
    }
    
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    public float getFloat(String key, float defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Float) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Float", defaultValue, e);
            return defaultValue;
        }
    }
    
    public float[] getFloatArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (float[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "float[]", e);
            return null;
        }
    }
    
    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Integer) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Integer", defaultValue, e);
            return defaultValue;
        }
    }
    
    public int[] getIntArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (int[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "int[]", e);
            return null;
        }
    }
    
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Long) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Long", defaultValue, e);
            return defaultValue;
        }
    }
    
    public long[] getLongArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (long[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "long[]", e);
            return null;
        }
    }
    
    public String[] getStringArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String[]", e);
            return null;
        }
    }
    
    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    public short getShort(String key, short defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (Short) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Short", defaultValue, e);
            return defaultValue;
        }
    }
    
    public short[] getShortArray(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (short[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "short[]", e);
            return null;
        }
    }
    
	public SyncData[] getDataArray(String key) {
		Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (SyncData[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "short[]", e);
            return null;
        }
	}
    
    public String getString(String key) {
        Object o = mValues.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", e);
            return null;
        }
    }

    public String getString(String key, String defaultValue) {
        Object o = mValues.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", e);
            return defaultValue;
        }
    }
	
    /**
     * Do not use this method.
     * @param key
     * @param obj
     */
	void put(String key, Object obj) {
		mValues.put(key, obj);
	}
	
	/**
	 * Do not use this method.
	 * @param key
	 * @return
	 */
	Object get(String key) {
		return mValues.get(key);
	}
	
	public Set<String> keySet() {
		return mValues.keySet();
	}
	
	private static final String PRE = "<SD>";
	private static final void loge(String msg) {
		Log.e(Constants.TAG, PRE + msg);
	}
}

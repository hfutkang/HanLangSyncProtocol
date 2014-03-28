package cn.ingenic.glasssync.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RemoteParcel implements Serializable {
	
	private static final long serialVersionUID = -3861321761065905549L;
	private List<Serializable> mDatas = new ArrayList<Serializable>();
	private int mPtr = 0;
	
	public void writeInt(int i) {
		mDatas.add(Integer.valueOf(i));
	}
	
	public void writeString(String s) {
		mDatas.add(s);
	}
	
	public void writeBoolean(boolean b) {
		mDatas.add(b);
	}
	
	public void wtrieObject(Serializable s) {
		mDatas.add(s);
	}
	
	public int readInt() {
		Integer i = (Integer) readObject();
		return i;
	}
	
	public String readString() {
		String s = (String) readObject();
		return s;
	}
	
	public boolean readBoolean() {
		boolean b = (Boolean) readObject();
		return b;
	}
	
	public Serializable readObject() {
		Serializable s = null;
		try {
			s = (Serializable) mDatas.get(mPtr++);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}
}

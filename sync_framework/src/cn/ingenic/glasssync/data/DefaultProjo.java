package cn.ingenic.glasssync.data;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import cn.ingenic.glasssync.Column;

public class DefaultProjo implements Projo {
	
	private static final long serialVersionUID = -6860325096097721134L;
	private HashMap<String, Object> mValues;
	private final EnumSet<? extends Column> mColumn;
	private final ProjoType mType;

	public DefaultProjo(EnumSet<? extends Column> c, ProjoType type) {
		mColumn = c;
		mType = type;
		mValues = new HashMap<String, Object>();
	}
	
	public DefaultProjo() {
		mColumn = null;
		mType = ProjoType.DATA;
		mValues = new HashMap<String, Object>();
	}
	
	@Override
	public void put(Column c, Object obj) {
		if (obj != null) {
			if (c.type().equals(obj.getClass())) {
				mValues.put(c.key(), obj);
			} else {
				throw new RuntimeException("Data Type not matching!");
			}
		}
	}

	@Override
	public Object get(Column c) {
		Object obj = mValues.get(c.key());
		
		if (obj == null) {
			return null;
		}
		
		if (obj.getClass().equals(c.type())) {
			return obj;
		}
		
		throw new RuntimeException("Data Type not matching!");
	}

	@Override
	public EnumSet<? extends Column> getColumn() {
		return mColumn;
	}

	@Override
	public ProjoType getType() {
		return mType;
	}

	@Override
	public Object get(String key) {
		return mValues.get(key);
	}

	@Override
	public void put(String key, Object obj) {
		mValues.put(key, obj);
	}

	@Override
	public Set<String> keySet() {
		return mValues.keySet();
	}

}

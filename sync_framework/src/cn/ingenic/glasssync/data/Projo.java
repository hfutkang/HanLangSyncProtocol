package cn.ingenic.glasssync.data;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import cn.ingenic.glasssync.Column;

public interface Projo extends Serializable {
	void put(Column c, Object obj);
	
	Object get(Column c);
	
	EnumSet<? extends Column> getColumn();
	
	ProjoType getType();
	
	Object get(String key);
	
	void put(String key, Object obj);
	
	Set<String> keySet();
}

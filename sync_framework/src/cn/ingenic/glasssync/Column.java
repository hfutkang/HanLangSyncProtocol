package cn.ingenic.glasssync;

import java.io.Serializable;

public interface Column extends Serializable {
	@SuppressWarnings("rawtypes")
	Class type();
	
	String key();
}

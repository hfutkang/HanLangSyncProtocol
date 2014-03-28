package cn.ingenic.glasssync.calendar;

import java.util.ArrayList;

import cn.ingenic.glasssync.Column;

public enum CalendarColumn implements Column {

	accounttype(String.class), accountname(String.class), event_id(Integer.class),events(ArrayList.class),tag(String.class);
    
	private final Class<?> mClass;
	CalendarColumn(Class<?> c) {
		mClass = c;
	}
	
	@Override
	public Class<?> type() {
		return mClass;
	}

	@Override
	public String key() {
		return name();
	}

}

package cn.ingenic.glasssync.data;

import java.util.EnumSet;

import cn.ingenic.glasssync.Column;

public class ServiceProjo extends DefaultProjo {

	private static final long serialVersionUID = -2965244050772990822L;

	public ServiceProjo() {
		super(EnumSet.allOf(ServiceColumn.class), ProjoType.SERVICE);
	}
	
	public enum ServiceColumn implements Column {
		descriptor(String.class), code(Integer.class), parcel(RemoteParcel.class);

		private Class<?> mType;
		ServiceColumn(Class<?> clazz) {
			mType = clazz;
		}
		@Override
		public Class<?> type() {
			return mType;
		}

		@Override
		public String key() {
			return name();
		}
		
	}
	
}

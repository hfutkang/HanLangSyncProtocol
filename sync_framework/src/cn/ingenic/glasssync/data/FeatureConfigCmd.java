package cn.ingenic.glasssync.data;

import java.util.EnumSet;
import java.util.HashMap;

import cn.ingenic.glasssync.Column;

public class FeatureConfigCmd extends CmdProjo {

	private static final long serialVersionUID = 2944317291657757723L;
	public FeatureConfigCmd() {
		super(EnumSet.allOf(FeatureConfigColumn.class), SystemCmds.FEATURE_CONFIG);
	}
	
	public static enum FeatureConfigColumn implements Column {
		feature_map(HashMap.class);
		
		private Class<?> mType;
		FeatureConfigColumn(Class<?> clazz) {
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

package cn.ingenic.glasssync;

import cn.ingenic.glasssync.services.SyncData;

public interface SyncManager {
	public static final int SUCCESS = 0;
	public static final int NO_CONNECTIVITY = SUCCESS - 1;
	public static final int FEATURE_DISABLED = SUCCESS - 2;
	public static final int NO_LOCKED_ADDRESS = SUCCESS - 3;
	
	public static final int SAVING_POWER_MODE = 0;
	public static final int RIGHT_NOW_MODE = 1;
	
	public static final int IDLE = 10;
		public static final int NON_REASON = 0;
		public static final int CONNECT_FAILED = NON_REASON - 1;
	public static final int CONNECTING = IDLE + 1;
	public static final int CONNECTED = IDLE + 2;
	public static final int DISCONNECTING = IDLE + 3;
	public static final int REQUESTING = IDLE + 4;
	public static final int RESPONDING = IDLE + 5;

	int getState();
	
	boolean hasLockedAddress();
	
	boolean isConnect();
	
	int send(String module, SyncData data);
}

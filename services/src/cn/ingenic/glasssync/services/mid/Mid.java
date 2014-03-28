package cn.ingenic.glasssync.services.mid;

public class Mid {
	static final String SRC = "Source";
	static final String DEST = "Destination";
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_KEY = "mid_key";
	static final String COLUMN_SYNC = "mid_sync";
	static final int VALUE_SYNC_DELETED = -1;
	static final int VALUE_SYNC_INSERTED = 1;
	static final int VALUE_SYNC_UPDATED = 2;
	static final int VALUE_SYNC_SUCCESS = 0;
}

package cn.ingenic.glasssync.appmanager;

import java.util.HashMap;

import cn.ingenic.glasssync.appmanager.ApplicationManager.AppEntry;


public class EntryMap<String, AppEntry> extends HashMap<String,AppEntry> {
	public static final int STATUS_NOT_OK = 0;
	public static final int STATUS_OK = 1;
	public int mStatus = STATUS_NOT_OK;

}

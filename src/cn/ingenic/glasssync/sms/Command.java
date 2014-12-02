package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.services.SyncData;

public class Command {
	public static final int NEW_SMS_COM = 0;
	public static final int REQUEST_SYNC = 1;

	private static Command mCommand = null;

	public static Command getCommandInstands() {
		if (mCommand == null) {
			mCommand = new Command();
		}
		return mCommand;
	}

	public int parse(SyncData data) {
		String command = data.getString("command");
		if ("new_sms".equals(command)) {
			return NEW_SMS_COM;
		}else if ("sync_req".equals(command)) {
			return REQUEST_SYNC;
		}
		return -1;
	}

}

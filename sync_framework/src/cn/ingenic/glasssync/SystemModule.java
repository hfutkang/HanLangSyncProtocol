package cn.ingenic.glasssync;

import cn.ingenic.glasssync.RemoteChannelManagerImpl;
import cn.ingenic.glasssync.RemoteChannelManagerService;
import android.content.Context;
import android.util.Log;

public class SystemModule extends Module {

	public static final String SYSTEM = "SYSTEM";

	private static final String TAG = "M-SYS";
	private static final boolean V = true;

	SystemModule() {
		super(SYSTEM);
	}

	@Override
	protected void onCreate(Context context) {
		if (V) {
			Log.d(TAG, "SystemModule created.");
		}

		if (DefaultSyncManager.isWatch()) {
			registService(RemoteChannelManagerService.DESPRITOR,
					new RemoteChannelManagerImpl());
		} else {
			registRemoteService(RemoteChannelManagerService.DESPRITOR,
					new RemoteBinderImpl(SYSTEM,
							RemoteChannelManagerService.DESPRITOR));
		}
	}

	@Override
	protected Transaction createTransaction() {
		return new SystemTransaction();
	}
	
}

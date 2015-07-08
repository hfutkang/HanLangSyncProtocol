package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

public class SmsModule extends SyncModule {

	private static SmsModule mInstance = null;
	private static String SMS_NAME = "sms_module";
	private Context mContext;
	private String mAddress;

        private static final String SYNC_REQUEST = "sync_request"; //The length of not more than 15
	private static SmsDestinationMidManager mSmsDestinationMidManager;

	public static SmsModule getInstance(Context context) {
		if (mInstance == null) {
			Context appCtx = context.getApplicationContext();
			mInstance = new SmsModule(SMS_NAME, appCtx);
			mSmsDestinationMidManager = new SmsDestinationMidManager(appCtx,
					mInstance);
		}
		return mInstance;
	}

	public SmsModule(String name, Context context) {
		super(name, context);
		this.mContext = context;
		setSyncEnable(false);
	}

	@Override
	protected void onCreate() {

	}

	@Override
	protected void onRetrive(SyncData data) {
		super.onRetrive(data);
		if (Sms.DEBUG)
			Log.i(Sms.TAG, "SmsModule onRetrive .");

		int ret = Command.getCommandInstands().parse(data);
		if(ret == Command.REQUEST_SYNC){
		    boolean enabled = data.getBoolean(SYNC_REQUEST,false);
		    Log.d("SmsModule", "---onRetrive enabled="+enabled);
		    setSyncEnable(enabled);
		    if(enabled) getMidTableManager().onModuleConnectivityChange(true);	
		}
	}

	@Override
	public MidTableManager getMidTableManager() {
		return mSmsDestinationMidManager;
	}

	public String getmAddress() {
		return mAddress;
	}

	public void setmAddress(String mAddress) {
		this.mAddress = mAddress;
	}
}

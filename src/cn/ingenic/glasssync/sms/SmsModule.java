package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidTableManager;
// import cn.ingenic.glasssync.sms.nitification.NewSmsDialog;
// import cn.ingenic.glasssync.sms.nitification.SmsNotification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
		}else if(ret == Command.NEW_SMS_COM){
		    Intent intent = new Intent();  
		    intent.setAction("action.new_sms.RECEIVER");  
		    // Bundle b = new Bundle();
		    // b.putParcelable("syncdata", data);
		    // intent.putExtra("bundle", b);
		    intent.putExtra("syncdata", data);
		    mContext.sendBroadcast(intent);  
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

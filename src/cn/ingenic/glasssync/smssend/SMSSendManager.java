package cn.ingenic.glasssync.smssend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.IntentFilter;

public class SMSSendManager {
    private static final String TAG = "SMSSendManager";
    private static final String ISMSIDTF = "ismsidtf";
    private static final String ISMSPHNUM = "ismsphnum";
    private static final String ISMSCONT = "ismscont";
    
    Context mContext = null;
    private static SMSSendManager sInstance = null;
    private SentActionReceiver nReceiver;

    private SMSSendManager(Context context){
	mContext = context;

	Log.e(TAG, "SMSSendManager");

	SMSSendModule ssm = SMSSendModule.getInstance(context);

	nReceiver = new SentActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("cn.ingenic.glasssync.smssend.SENDMESSAGE");
	mContext.registerReceiver(nReceiver, filter);

    }

    public static SMSSendManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new SMSSendManager(c);
	return sInstance;
    }

    class SentActionReceiver extends BroadcastReceiver{
        @Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals("cn.ingenic.glasssync.smssend.SENDMESSAGE")){
		Log.e(TAG, "onReceive " + intent.getAction());
		long idtf = intent.getLongExtra(ISMSIDTF, 0l);
		String phnum = intent.getStringExtra(ISMSPHNUM);
		String content = intent.getStringExtra(ISMSCONT);
		Log.e(TAG, "rcv message idtf:" + idtf + " phnum:" + phnum + " content:" + content);

		SMSSendModule ssm = SMSSendModule.getInstance(mContext);
		ssm.sendMessage(idtf, phnum, content);
	    }	    
        }
    }

}
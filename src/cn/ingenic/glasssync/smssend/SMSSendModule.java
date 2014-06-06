package cn.ingenic.glasssync.smssend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class SMSSendModule extends SyncModule {
    private static final String TAG = "SMSSendModule";
    private static final String LETAG = "GSSSM";

    private static final String GSSMD_CMD = "gssmd_cmd";
    private static final String GSSMD_SND = "gssmd_snd";
    private static final String GSSMD_RST = "gssmd_rst";

    private static final String GSSMD_IDTF = "gssmd_idtf";
    private static final String GSSMD_TEXT = "gssmd_text";
    private static final String GSSMD_PHNUM = "gssmd_phnum";

    private static final String GSSMD_RTVAL = "gssmd_rtval";

    Context mContext = null;
    private static SMSSendModule sInstance = null;

    private SMSSendModule(Context context){
	super(LETAG, context);
	mContext = context;
	Log.e(TAG, "SMSSendModule");
    }

    public static SMSSendModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new SMSSendModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String cmd = data.getString(GSSMD_CMD);

	if (cmd.equals(GSSMD_RST)){
	    Log.e(TAG, "GSSMD_RST");
	    long idtf = data.getLong(GSSMD_IDTF);
	    int rst = data.getInt(GSSMD_RTVAL);
	    Log.e(TAG, "idtf:" + idtf + " rst:" + rst);
	}
    }

    public void sendMessage(long idtf, String phnum, String content){
	if (idtf <= 0 || phnum == null || content == null)
	    return;

	SyncData data = new SyncData();

	data.putString(GSSMD_CMD, GSSMD_SND);
	data.putLong(GSSMD_IDTF, idtf);
	data.putString(GSSMD_TEXT, content);
	data.putString(GSSMD_PHNUM, phnum);

	try {
	    Log.e(TAG, "sendMessage:" + phnum + " " + idtf);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
}
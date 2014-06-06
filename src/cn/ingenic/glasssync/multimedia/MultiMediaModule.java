package cn.ingenic.glasssync.multimedia;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import android.content.SharedPreferences;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.multimedia.MultiMediaObserver;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Intent;

public class MultiMediaModule extends SyncModule {
    private static final String TAG = "MultiMediaModule";
    private static final Boolean DEBUG = true;
    private static final String LETAG = "GSMMM";

    private static final String GSMMD_CMD = "gsmmd_cmd";
    private static final String GSMMD_ASK = "gsmmd_ask";
    private static final String GSMMD_rqst = "gsmmd_rqst";
    private static final String GSMMD_FINISH = "gsmmd_finish";
    private static final String GSMMD_DELETE = "gsmmd_delete";
    private static final String GSMMD_DELFNS = "gsmmd_delfns";
    private static final String GSMMD_FFAIL = "gsmmd_ffail";
    private static final String GSMMD_FFACK = "gsmmd_ffack";
    private static final String GSMMD_AUTOSYNC = "gsmmd_autosync";

    private static final String GSMMD_NAME = "gsmmd_name";

    private static final String GSMMD_TYPE = "gsmmd_type";
    public static int GSMMD_PIC = 0x1;
    public static int GSMMD_VIDEO = 0x2;
    public static int GSMMD_SINGLE_FILE = 0x3;
    public static int GSMMD_ALL = 0x10;

    private static final String GSMMD_ACT = "gsmmd_act";
    public static int GSMMD_EXIST = 0x1;
    public static int GSMMD_NOEXIST = 0x2;

    private static final String GSMMD_TSP = "gsmmd_tsp";
    private int ASK_TSP = 0;

    private static final String GSMMD_SRST = "gsmmd_srst";

    private static final String GSMMD_STASM = "gsmmd_stasm";
    private static final int STASM_TRUE  = 0x75;
    private static final int STASM_FALSE = 0x74;

    private static final String GSMMD_SINGLE_FILE_NAME = "gsmmd_single_file_name";
    private static final String GSMMD_SINGLE_FILE_TYPE = "gsmmd_single_file_type";
    public static int SINGLE_FILE_TYPE_PIC = 0x1;
    public static int SINGLE_FILE_TYPE_VIDEO = 0x2;

    private boolean gnAutoSync = false;

    private Context mContext;
    private static MultiMediaModule sInstance;
    Calendar mCld = Calendar.getInstance();

    private MultiMediaModule(Context context){
	super(LETAG, context);
	mContext = context;
	Log.e(TAG, "MultiMediaModule");
    }

    public static MultiMediaModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new MultiMediaModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    public void ask_sync(String name, int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_ASK);
	data.putInt(GSMMD_TYPE, type);
	data.putString(GSMMD_NAME, name);
	ASK_TSP = mCld.get(Calendar.SECOND);
	data.putInt(GSMMD_TSP, ASK_TSP);

	try {
	    Log.e(TAG, "send ask sync " + name);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void sync_file(String name, int type){
	String pathname = null;
	if (type == GSMMD_PIC)
	    pathname = MultiMediaObserver.getPubPath() + "Pictures/" + name;
	else if (type == GSMMD_VIDEO)
	    pathname = MultiMediaObserver.getPubPath() + "Video/" + name;

	File f = new File(pathname);
	Log.e(TAG, "sync_file length:" + f.length());
	Date date = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	Log.e(TAG, "tttime:" + sdf.format(date));

	try{
	    //sendFile(f, name, (int)f.length());
	    if (type == GSMMD_PIC)
		sendFileByPath(f, name, (int)f.length(), "IGlass/Pictures");
	    else if (type == GSMMD_VIDEO)
		sendFileByPath(f, name, (int)f.length(), "IGlass/Video");
	}catch (SyncException e){
	    Log.e(TAG, "" + e);
	}catch (FileNotFoundException e){
	    Log.e(TAG, "" + e);
	}
    }

    public void delete_finish(String name, int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_DELFNS);
	data.putInt(GSMMD_TYPE, type);
	data.putString(GSMMD_NAME, name);

	try {
	    Log.e(TAG, "send del fns " + name);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void fileFail(String name, int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_FFAIL);
	data.putString(GSMMD_NAME, name);
	data.putInt(GSMMD_TYPE, type);

	try {
	    Log.e(TAG, "send file fail " + name);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public boolean getAutoSync(){
	SharedPreferences sp = mContext.getSharedPreferences(LETAG, Context.MODE_PRIVATE);

	boolean async = sp.getBoolean("autosync", false);
	Log.e(TAG, "getAutoSync:" + async);
	return async;
    }

    public void setAutoSync(boolean value){
	SharedPreferences sp = mContext.getSharedPreferences(LETAG, Context.MODE_PRIVATE);
	SharedPreferences.Editor editor = sp.edit();
	Log.e(TAG, "setAutoSync:" + value);
	editor.putBoolean("autosync", value);
	editor.commit();

	gnAutoSync = value;

	if (gnAutoSync == false){
	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.clearWaitList();
	}else{
	    MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
	    m.sync_pic();
	}
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String cmd = data.getString(GSMMD_CMD);

	if (cmd.equals(GSMMD_rqst)){
	    int type = data.getInt(GSMMD_TYPE);
	    if (type == GSMMD_SINGLE_FILE){
		if(DEBUG) Log.e(TAG, "--GSMMD_SINGLE_FILE");
		int file_type = data.getInt(GSMMD_SINGLE_FILE_TYPE);
		String file_name = data.getString(GSMMD_SINGLE_FILE_NAME);
		MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
		m.sync_single_file(file_name,file_type);
	    }else if (type == GSMMD_ALL){
		Log.e(TAG, "GSMMD_ALL");
		// Intent i = new Intent("cn.ingenic.glasssync.smssend.SENDMESSAGE");
		// i.putExtra("ismsidtf", 1234321l);
		// i.putExtra("ismsphnum", "15901336736");
		// i.putExtra("ismscont", "This is test message from bl");
		// Log.e("SMSSend", "SENDMESSAGE");
		// mContext.sendBroadcast(i);
		MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
		m.sync_pic();
	    }else if (type == GSMMD_PIC || type == GSMMD_VIDEO){
		Log.e(TAG, "req " + type);
		int tsp = data.getInt(GSMMD_TSP);
		if (tsp != ASK_TSP)
		    return;
		MultiMediaManager m = MultiMediaManager.getInstance(mContext);
		m.replyAsk(data.getString(GSMMD_NAME), type, data.getInt(GSMMD_ACT));
	    }
	}else if (cmd.equals(GSMMD_FINISH)){
	    Log.e(TAG, "GSMMD_FINISH");
	    boolean succ = data.getBoolean(GSMMD_SRST, false);
	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.replyFinish(succ);
	}else if (cmd.equals(GSMMD_DELETE)){
	    Log.e(TAG, "GSMMD_DELETE");
	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.deleteFile(data.getString(GSMMD_NAME), data.getInt(GSMMD_TYPE));
	}else if (cmd.equals(GSMMD_FFACK)){
	    Log.e(TAG, "GSMMD_FFACK");
	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.reply_ffail(data.getString(GSMMD_NAME), data.getInt(GSMMD_TYPE));
	}else if (cmd.equals(GSMMD_AUTOSYNC)){
	    
	    int async = data.getInt(GSMMD_STASM);
	    Log.e(TAG, "GSMMD_AUTOSYNC:" + async);
	    if (async == STASM_TRUE)
		setAutoSync(true);
	    else
		setAutoSync(false);
	}
    }

    @Override
    public void onFileSendComplete(String fileName, boolean success){
	Log.e(TAG, "onFileSendComplete " + fileName + " " + success);
	Date date = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	Log.e(TAG, "tttime:" + sdf.format(date));

	if (success == false){
	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.fileFail(fileName);
	}
    }

}
package cn.ingenic.glasssync.multimedia;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import android.net.Uri;
import android.provider.MediaStore;
import cn.ingenic.glasssync.multimedia.MultiMediaModule;
import java.util.ArrayList;

public class MultiMediaManager {
    private static final String TAG = "MultiMediaManager";

    private Context mContext;
    private static MultiMediaManager sInstance;
    private Handler mReqHandle;
    private Runnable mReqRunnable;
    public MultiMediaObserver MMObsv;
    static int CNT = 0;

    private ArrayList<String> mPicList = new ArrayList<String>();
    private ArrayList<String> mPicWaitList = new ArrayList<String>();

    private ArrayList<String> mVideoList = new ArrayList<String>();
    private ArrayList<String> mVideoWaitList = new ArrayList<String>();

    private ArrayList<String> mPicFailList = new ArrayList<String>();
    private ArrayList<String> mVideoFailList = new ArrayList<String>();

    private String askName;
    private int syncType;

    private static int mReqState;
    private static final int REQ_IDEL = 0;
    private static final int REQ_ASK = 1;
    private static final int REQ_SYNC = 2;

    private MultiMediaManager(Context context){
	Log.e(TAG, "MultiMediaManager");
	mContext = context;

	createMMObserver(mContext);
	RequestHandler(context);
	mReqState = REQ_IDEL;
	mReqHandle.postDelayed(mReqRunnable, 1000);
    }

    public static MultiMediaManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new MultiMediaManager(c);
	return sInstance;
    }

    private void createMMObserver(Context context){
	Log.e(TAG, "createMMObserver");
	MMObsv = MultiMediaObserver.getInstance(context);

	context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, MMObsv);
	context.getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false, MMObsv);
    }

    public void addWaitList(String name, int type){
	if (type == MultiMediaModule.GSMMD_PIC){
	    if (mPicList.contains(name)){
		Log.e(TAG, "addWaitList exist " + name);
		return;
	    }

	    if (!mPicWaitList.contains(name)){
		Log.e(TAG, "Add to wait list " + name);
		mPicWaitList.add(name);
	    }
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    if (mVideoList.contains(name)){
		Log.e(TAG, "addWaitList exist " + name);
		return;
	    }

	    if (!mVideoWaitList.contains(name)){
		Log.e(TAG, "Add to wait list " + name);
		mVideoWaitList.add(name);
	    }
	}
    }

    public void replyAsk(String name, int type, int act){
	Log.e(TAG, "replyAsk " + name + " " + type + " " + act);
	if (!askName.equals(name)){
	    Log.e(TAG, "ask " + askName + " reply" + name);
	    mReqState = REQ_IDEL;
	    return;
	}

	if (mReqState != REQ_ASK){
	    Log.e(TAG, "Re ask");
	    return;
	}

	if (type == MultiMediaModule.GSMMD_PIC){
	    Log.e(TAG, "replyAsk PIC");
	    if (act == MultiMediaModule.GSMMD_EXIST){
		Log.e(TAG, "replyAsk EXIST");
		if (!mPicList.contains(name)){
		    mPicList.add(name);
		}
		mPicWaitList.remove(name);
		mReqState = REQ_IDEL;
	    }else{
		Log.e(TAG, "replyAsk NOEXIST");
		mReqState = REQ_SYNC;
		syncType = type;
		MultiMediaModule m = MultiMediaModule.getInstance(mContext);
		m.sync_file(name, type);
	    }
	}else if (type == MultiMediaModule.GSMMD_VIDEO) {
	    Log.e(TAG, "replyAsk VIDEO");
	    if (act == MultiMediaModule.GSMMD_EXIST){
		Log.e(TAG, "replyAsk EXIST");
		if (!mVideoList.contains(name)){
		    mVideoList.add(name);
		}
		mVideoWaitList.remove(name);
		mReqState = REQ_IDEL;
	    }else{
		Log.e(TAG, "replyAsk NOEXIST");
		mReqState = REQ_SYNC;
		syncType = type;
		MultiMediaModule m = MultiMediaModule.getInstance(mContext);
		m.sync_file(name, type);
	    }
	}else{
	    Log.e(TAG, "Invalid type " + mReqState);
	}
    }

    public void replyFinish(boolean success){
	Log.e(TAG, "replyFinish " + success);

	if (success == true){
	    if (syncType == MultiMediaModule.GSMMD_PIC){
		if (askName != null)
		    mPicWaitList.remove(askName);
	
		if (!mPicList.contains(askName)){
		    mPicList.add(askName);
		}
	    }else if (syncType == MultiMediaModule.GSMMD_VIDEO){
		if (askName != null)
		    mVideoWaitList.remove(askName);
	
		if (!mVideoList.contains(askName)){
		    mVideoList.add(askName);
		}	    
	    }
	}

	mReqState = REQ_IDEL;
    }

    public void deleteFile(String fileName, int type){
	Log.e(TAG, "deleteFile:" + fileName + " " + type);
	if (type == MultiMediaModule.GSMMD_PIC){
	    if (mPicList.contains(fileName)){
		MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
		m.deleteFile(fileName, type);
		mPicList.remove(fileName);
	    }
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    if (mVideoList.contains(fileName)){
		MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
		m.deleteFile(fileName, type);
		mVideoList.remove(fileName);
	    }
	}
	// to do delete end
	MultiMediaModule m = MultiMediaModule.getInstance(mContext);
	m.delete_finish(fileName, type);
    }

    public void fileFail(String fileName){

	if (syncType == MultiMediaModule.GSMMD_PIC){
	    mPicFailList.add(fileName);
	    mPicWaitList.remove(fileName);
	}else if (syncType == MultiMediaModule.GSMMD_VIDEO){
	    mVideoFailList.add(fileName);
	    mVideoWaitList.remove(fileName);
	}
	mReqState = REQ_IDEL;
    }

    public void reply_ffail(String fileName, int type){
	if (type == MultiMediaModule.GSMMD_PIC){
	    mPicFailList.remove(askName);
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    mVideoFailList.remove(fileName);
	}
    }

    private void RequestHandler(Context c){
	mReqHandle = new Handler();
	mReqRunnable = new Runnable(){
		@Override
		public void run() {
		    if (mReqState != REQ_IDEL){
			// need cal time out
			if (CNT++ > 10 && mReqState == REQ_ASK){
			    Log.e(TAG, "ASK time out");
			    CNT = 0;
			    mReqState = REQ_IDEL;
			}
			mReqHandle.postDelayed(this, 1000);
			return;
		    }

		    if (mPicWaitList.isEmpty() && mVideoWaitList.isEmpty()){
			if (!mPicFailList.isEmpty()){
			    String name = mPicFailList.get(0);
			    MultiMediaModule m = MultiMediaModule.getInstance(mContext);
			    m.fileFail(name, MultiMediaModule.GSMMD_PIC);
			    mReqHandle.postDelayed(this, 3000);
			    return;
			}

			if (!mVideoFailList.isEmpty()){
			    String name = mVideoFailList.get(0);
			    MultiMediaModule m = MultiMediaModule.getInstance(mContext);
			    m.fileFail(name, MultiMediaModule.GSMMD_VIDEO);
			    mReqHandle.postDelayed(this, 3000);
			    return;
			}

			mReqHandle.postDelayed(this, 10000);
			return;
		    }

		    if (!mPicWaitList.isEmpty()){
			String name = mPicWaitList.get(0);
			if (name != null){
			    mReqState = REQ_ASK;
			    askName = name;
			    MultiMediaModule m = MultiMediaModule.getInstance(mContext);
			    CNT = 0;
			    m.ask_sync(name, MultiMediaModule.GSMMD_PIC);
			    mReqHandle.postDelayed(this, 1000);
			    return;
			}
		    }

		    String name = mVideoWaitList.get(0);
		    if (name != null){
			mReqState = REQ_ASK;
			askName = name;
			MultiMediaModule m = MultiMediaModule.getInstance(mContext);
			CNT = 0;
			m.ask_sync(name, MultiMediaModule.GSMMD_VIDEO);
			mReqHandle.postDelayed(this, 1000);
			return;
		    }

		    mReqHandle.postDelayed(this, 1000);
		}
	    };
    }
}
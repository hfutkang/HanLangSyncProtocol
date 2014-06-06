package cn.ingenic.glasssync.multimedia;

import android.content.Context;
import android.database.Cursor;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import java.util.List;
import android.os.Environment;
import java.io.File;
import cn.ingenic.glasssync.multimedia.MultiMediaModule;
import cn.ingenic.glasssync.multimedia.MultiMediaManager;

public class MultiMediaObserver extends ContentObserver {
    private Context mContext;
    private static final String TAG = "MultiMediaObserver";
    private static final Boolean DEBUG = true;

    private static MultiMediaObserver sInstance;
    private static String mPubPath;

    String[] picColu = new String[] {
	MediaStore.Images.Media.DATA
    };

    String[] videoColu = new String[] {
	MediaStore.Video.Media.DATA
    };

    private MultiMediaObserver(Context context, Handler handler){
	super(handler);
	mContext = context;
	Log.e(TAG, "MultiMediaObserver");
	MultiMediaModule m = MultiMediaModule.getInstance(mContext);
    }

    public static MultiMediaObserver getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new MultiMediaObserver(c, mHandler);
	return sInstance;
    }

    public static String getPubPath(){
	return mPubPath;
    }

    @Override  
	public void onChange(boolean selfChange, Uri uri) {  
	Log.e(TAG, "onChange " + uri);

	MultiMediaModule mmm = MultiMediaModule.getInstance(mContext);
	if (mmm.getAutoSync())
	    sync_pic();
    }
    public void sync_single_file(String name,int type){
	if(DEBUG) Log.e(TAG,"----------sync_single_file in type="+type+" name:" + name);
	if(type != MultiMediaModule.GSMMD_PIC) 
	    return;

	StringBuilder where = new StringBuilder();
	where.append(MediaStore.Images.Media.DATA + " like ?");
	String whereVal[] = {"%" + "IGlass/Pictures" + "%" + name};

	Cursor cs = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
							picColu, where.toString(), whereVal, null);

	if(DEBUG) Log.e(TAG, "ex getColumnCount " + cs.getColumnCount() + " getCount:" + cs.getCount());
	if  (cs == null) return;
	cs.moveToFirst();
	try{
	    int pathidx = cs.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

	    String picPath = cs.getString(pathidx);
	    String picName = picPath.substring(picPath.lastIndexOf('/')+1, picPath.length());
	    if (mPubPath == null)
		mPubPath = picPath.substring(1, picPath.lastIndexOf("IGlass")+7);
	    if(DEBUG) Log.e(TAG, "picPath " + picPath + " picName:" + picName + " mPubPath:" + mPubPath);

	    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
	    m.addWaitList(picName, MultiMediaModule.GSMMD_PIC);
	    
	}catch(IllegalArgumentException e){
	}
    }

    public void sync_pic(){
	Log.e(TAG,"----------sync_pic in");
	StringBuilder where = new StringBuilder();
	where.append(MediaStore.Images.Media.DATA + " like ?");
	String whereVal[] = {"%" + "IGlass/Pictures" + "%"};
	Log.e(TAG,"----------sync_pic in2");
	Cursor cs = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, picColu, where.toString(), whereVal, null);

	Log.e(TAG, "ex getColumnCount " + cs.getColumnCount() + " getCount:" + cs.getCount());
	if  (cs.moveToFirst()){
	    try{
		int pathidx = cs.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		do{
		    String picPath = cs.getString(pathidx);
		    String picName = picPath.substring(picPath.lastIndexOf('/')+1, picPath.length());
		    if (mPubPath == null)
			mPubPath = picPath.substring(1, picPath.lastIndexOf("IGlass")+7);
		    Log.e(TAG, "picPath " + picPath + " picName:" + picName + " mPubPath:" + mPubPath);
		    Log.e(TAG, "lastIndexOf:" + picPath.lastIndexOf("IGlass/Pictures") + " length:" + picPath.length() + " lastIndexOf:" + picPath.lastIndexOf('/'));
		    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
		    m.addWaitList(picName, MultiMediaModule.GSMMD_PIC);
		}while (cs.moveToNext());
	    }catch(IllegalArgumentException e){
	    }
	}

	where = new StringBuilder();
	where.append(MediaStore.Video.Media.DATA + " like ?");
	String whereVal1[] = {"%" + "IGlass/Video" + "%"};

	cs = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoColu, where.toString(), whereVal1, null);
	if  (cs.moveToFirst()){
	    try{
		int pathidx = cs.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

		do{
		    String picPath = cs.getString(pathidx);
		    String picName = picPath.substring(picPath.lastIndexOf('/')+1, picPath.length());
		    if (mPubPath == null)
			mPubPath = picPath.substring(1, picPath.lastIndexOf("IGlass")+7);
		    Log.e(TAG, "picPath " + picPath + " picName:" + picName + " mPubPath:" + mPubPath);
		    //Log.e(TAG, "lastIndexOf:" + picPath.lastIndexOf("IGlass/Pictures") + " length:" + picPath.length() + " lastIndexOf:" + picPath.lastIndexOf('/'));
		    MultiMediaManager m = MultiMediaManager.getInstance(mContext);
		    m.addWaitList(picName, MultiMediaModule.GSMMD_VIDEO);
		}while (cs.moveToNext());
	    }catch(IllegalArgumentException e){
	    }
	}
    }

    public void deleteFile(String fileName, int type){
	String dirpath;
	if (type == MultiMediaModule.GSMMD_PIC){
	    dirpath = "/IGlass/Pictures/";
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    dirpath = "/IGlass/Video/";
	}else{
	    dirpath = "/IGlass/data/";
	}

	File f = new File(Environment.getExternalStorageDirectory() + dirpath + fileName);
	f.delete();
    }

    private static Handler mHandler = new Handler() {  
	    public void handleMessage(Message msg) {  
		switch (msg.what) {  
		default:  
		    break;  
		}  
	    }  
	}; 

}
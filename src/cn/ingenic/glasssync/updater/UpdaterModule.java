package cn.ingenic.glasssync.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.RemoteBinderImpl;
import cn.ingenic.glasssync.DefaultSyncManager.OnFileChannelCallBack;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
/**
 * for update sync, to get watch's name & version info
 * @author dfdun*/
public class UpdaterModule extends Module {

    public static final String TAG = "M-UPDATER";
    public static final boolean V = true;

    static final String UPDATER = "UPDATER";
    private Context mContext;
    
    public UpdaterModule() {
        super(UPDATER);
    }

    protected void onCreate(Context context) {
        mContext = context;
        if (V) {
            Log.d(TAG, "UpdaterModule created.");
        }

        //if (!DefaultSyncManager.isWatch()) {
//            registRemoteService(IUpdaterRemoteService.DESPRITOR,
//                    new RemoteBinderImpl(UPDATER,
//                            IUpdaterRemoteService.DESPRITOR));
//        } else {
            registService(IUpdaterRemoteService.DESPRITOR,
                    new UpdaterRemoteServiceImpl(context));
//        }
    }
    
    private OnFileChannelCallBack mFileChannelCallBack = new OnFileChannelCallBack() {

        @Override
        public void onRetriveComplete(String name, boolean success) {
            Log.i(TAG, "onRetriveComplete] file " + name + ", " + success);
            Toast.makeText(mContext, name + ", " + success, Toast.LENGTH_SHORT)
                    .show();
            String newfile=copyFile(name);

            Intent intent = new Intent(mContext, NoticesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("update", "yes");
            intent.putExtra("filename", newfile);
            if (success){
                mContext.startActivity(intent);
            }
        }

        @Override
        public void onSendComplete(String name, boolean success) {
            Log.i(TAG, "onSendComplete] file "+name +", "+success);
        }
        
    };

    @Override
    public OnFileChannelCallBack getFileChannelCallBack() {
        return mFileChannelCallBack;
    }

    private static String copyFile(String name) { // /sdcard0   to /data/data
    	final String arm= "/data/data/cn.ingenic.glasssync/update.zip";
        try {
            File oldfile = new File(name/*"/flash/update.zip"*/);
            File newfile = new File(arm);
            if (newfile.exists())
                newfile.delete();
            Log.d("OtaUpdater", arm+" [create  result="+newfile.createNewFile());
            FileInputStream in = new FileInputStream(oldfile);
            FileOutputStream out = new FileOutputStream(newfile);
            FileChannel inc=in.getChannel();
            FileChannel outc = out.getChannel();
            int len =2097152;
            ByteBuffer b=null;
            while (true) {
                if (inc.position() == inc.size()) {
                    in.close();
                    out.close();
                    return arm;
                }
                if(inc.size()-inc.position() < len){
                    len = (int)(inc.size()-inc.position());
                }else{
                    len =2097152;
                }
                b = ByteBuffer.allocateDirect(len);
                inc.read(b); b.flip();
                outc.write(b);
                outc.force(false);
            }
        } catch (IOException e) {
            Log.e("OtaUpdater", "copy from /sdcard0 to /data failed. "+e.toString());
           return name ;
        }
    }
}

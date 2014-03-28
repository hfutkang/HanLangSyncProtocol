package cn.ingenic.glasssync.devicemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;

public class ConnectionManager {
	private static ConnectionManager sInstance;
	public Context mContext;
	
    private static Handler mHandleCallback = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Context con=ConnectionManager.getInstance().mContext;
            switch(msg.arg2){
            case Commands.CMD_RING_AND_VIBRAT:
                klilog.i("HandleCallback, result="+msg.arg1+", cmd="+Commands.CMD_RING_AND_VIBRAT);
                if (msg.arg1 != DefaultSyncManager.SUCCESS) {
                    device2Apps(con, Commands.CMD_RING_AND_VIBRAT, "ring_failed");
                } else {
                    device2Apps(con, Commands.CMD_RING_AND_VIBRAT, "ring_success");
                }
                break;
            case Commands.CMD_SYNC_CALL_LOG_REQUEST:
                if(msg.arg1 != DefaultSyncManager.SUCCESS){
                    Toast.makeText(con, R.string.call_log_sync_fail, 0).show();
                } else {
                    Toast.makeText(con, R.string.call_log_sync_success, 0).show();
                }
                break;
            }
        }

    };
	
	private ConnectionManager(Context context){
		mContext = context;
	}
	
	public static ConnectionManager getInstance(Context context){
		if(sInstance == null){
			sInstance = new ConnectionManager(context);
		}
		return sInstance;
	}
	
	public static ConnectionManager getInstance(){
		return sInstance;
	}
	
	public static void device2Device(int cmd, String data){
		klilog.i("device 2 device; cmd:"+cmd+", data:"+data);
		Projo projo = new DefaultProjo(EnumSet.allOf(DeviceColumn.class),
				ProjoType.DATA);
		projo.put(DeviceColumn.command, cmd);
		projo.put(DeviceColumn.data, data);
		Config config = new Config(DeviceModule.MODULE, Commands.getCmdFeature(cmd));
		if(cmd==Commands.CMD_RING_AND_VIBRAT||cmd==Commands.CMD_SYNC_CALL_LOG_REQUEST){
		    config.mCallback = mHandleCallback.obtainMessage(1);
	        config.mCallback.arg2 = cmd;
		}
		ArrayList<Projo> datas = new ArrayList<Projo>(1);
		datas.add(projo);
		DefaultSyncManager.getDefault().request(config, datas);
	}
	
	public void apps2Device(int cmd, String data) {
		// request
		klilog.i("request: cmd: " + cmd + "; data: " + data);
		device2Device(cmd, data);
	}
	
	/*
	public void sendFile(File sendFile){
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
        FileInputStream fis;
        try {
            fis = new FileInputStream(sendFile);
            manager.sendFile(DeviceModule.MODULE, sendFile.getName(), (int)sendFile.length(), fis);
        } catch (FileNotFoundException e) {
        	klilog.e("file send error");
        }
	}
	
	public void sendFile(InputStream is, String name){
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
        try {
        	int length = is.available();
            manager.sendFile(DeviceModule.MODULE, name, length, is);
        } catch (FileNotFoundException e) {
			e.printStackTrace();
        	klilog.e("file send error");
        } catch (IOException e) {
			e.printStackTrace();
        	klilog.e("file send error");
		}
	}*/
	
	public static void device2Apps(Context context, int cmd, String data){
		Intent intent = new Intent();
		if(cmd==Commands.CMD_GET_TIME){
			int index = data.indexOf(",");
			if (Math.abs(Long.valueOf(data.substring(0, index))- System.currentTimeMillis()) < 30000
					&& data.substring(index + 1).equals(java.util.TimeZone.getDefault().getID()))
				// if  time lag is < 30s && timezone is same. we do not sync time
				return;
		}
		
		intent.setAction(Commands.getCmdAction(cmd));
		intent.putExtra("cmd", cmd);
		intent.putExtra("data", data);
		context.sendBroadcast(intent);
		klilog.i("broadcast send. cmd:"+cmd+", data:"+data);
	}
}

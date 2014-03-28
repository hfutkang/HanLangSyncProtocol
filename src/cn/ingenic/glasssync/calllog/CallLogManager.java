package cn.ingenic.glasssync.calllog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog.Calls;
import cn.ingenic.glasssync.devicemanager.Commands;
import cn.ingenic.glasssync.devicemanager.ConnectionManager;
import cn.ingenic.glasssync.devicemanager.klilog;

public class CallLogManager {
	

	public final static boolean DEBUG = true;
	
	public final static int RES_SUCCESS = 1;
	public final static int RES_FAILED = 2;
	public final static int RES_FAILED_WITH_DATA_CHAOS = 3;
	   
	
	private final static int MSG_SYNC_START = 1;
	private final static int MSG_SYNC_FINISH = 2;
	private final static int MSG_LOAD_FILE = 3;
	private final static int MSG_LOAD_FINISH = 4;
	private final static int MSG_CLEAR_FINISH = 5;
	private final static int MSG_CLEAR_DATA = 6;
	private final static int MSG_ON_CLEARED = 7;
	

	private static CallLogManager sInstance;
	private Context mContext;
	private LoadDataTask mLoadDataTask;
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case MSG_CLEAR_DATA:
				startClearData((String)msg.obj);
				break;
			case MSG_LOAD_FILE:
				String data = (String)msg.obj;
				startLoadData(data);
				break;
			case MSG_LOAD_FINISH:
				finishLoadData(String.valueOf(msg.arg1));
				break;
			case MSG_CLEAR_FINISH:
				ConnectionManager.device2Device(Commands.CMD_CLEAR_CALL_LOG, String.valueOf(msg.arg1));
				break;
			case MSG_ON_CLEARED:
				ConnectionManager.device2Device(Commands.CMD_SYNC_WATCH_ON_CLEAR, String.valueOf(msg.arg1));
				break;
			}
		}
		
	};
	
	private void startClearData(String cmd){
		if(mLoadDataTask != null){
			mLoadDataTask.cancel(true);
			mLoadDataTask = null;
		}
		new ClearDataTask().execute(cmd);
	}
	
	private void startLoadData(String data){
		mLoadDataTask = new LoadDataTask();
		mLoadDataTask.execute(data);
	}
	
	private void finishLoadData(String res){
		if(mLoadDataTask != null){
			mLoadDataTask = null;
			ConnectionManager.device2Device(Commands.CMD_SYNC_CALL_LOG, res);
		}
	}
	
	private CallLogManager(Context context){
		mContext = context;
	}
	
	public static CallLogManager getInstance(Context context){
		if(sInstance == null){
			sInstance = new CallLogManager(context);
		}
		return sInstance;
	}
	
	public void dataReceived(String data){
		if(sInstance != null){
			Message msg = mHandler.obtainMessage(MSG_LOAD_FILE);
			msg.obj = data;
			msg.sendToTarget();
		}
	}
	
	public void clearReceived(String data){
		if(sInstance != null){
			Message msg = mHandler.obtainMessage(MSG_CLEAR_DATA);
			msg.obj = data;
			msg.sendToTarget();
		}
	}
	
	private class ClearDataTask extends AsyncTask<String, Integer, Integer>{
		String cmd;
		@Override
		protected Integer doInBackground(String... arg0) {
			ContentResolver resolver = mContext.getContentResolver();
			try {
				cmd = arg0[0];
				klilog.i("delete all call logs. data:"+cmd);
				resolver.delete(Calls.CONTENT_URI, null, null);
				return RES_SUCCESS;

			} catch (Exception e) {
				klilog.e(""+e);
				e.printStackTrace();
				return RES_FAILED;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if("all".equals(cmd)){
				Message msg = mHandler.obtainMessage(MSG_CLEAR_FINISH);
				msg.arg1 = result;
				msg.sendToTarget();
			}else if("onclear".equals(cmd)){
				Message msg = mHandler.obtainMessage(MSG_ON_CLEARED);
				msg.arg1 = result;
				msg.sendToTarget();
			}
		}
	}
	
	private class LoadDataTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... arg0) {
			JSONArray delArray = null;
			JSONArray addArray = null;
			JSONArray new2readArray =null;
			JSONArray namesArray = null;
			long start = System.currentTimeMillis();
			ContentResolver resolver = mContext.getContentResolver();
			try {
				String dataString = arg0[0];
				JSONObject dataObj = new JSONObject(dataString);
				delArray = dataObj.getJSONArray("del");
				addArray = dataObj.getJSONArray("add");
				new2readArray=dataObj.getJSONArray("new2read");
				namesArray = dataObj.getJSONArray("update_name");
			} catch (Exception e) {
				e.printStackTrace();
				return RES_FAILED;
			}

			try {
				final int dels =delArray.length(),adds=addArray.length(),
						news=new2readArray.length(),names=namesArray.length();
				klilog.i("load delete,size = " + dels);
				for (int i = 0; i < dels; i++) {
					if (this.isCancelled()) {
						return RES_FAILED_WITH_DATA_CHAOS;
					}
					resolver.delete(Calls.CONTENT_URI, Calls._ID + "="
							+ delArray.getInt(i), null);
				}
				klilog.i("load add, size = " + adds);
				for (int i = 0; i < adds; i++) {
					if (this.isCancelled()) {
						return RES_FAILED_WITH_DATA_CHAOS;
					}
					JSONObject jobj = addArray.getJSONObject(i);
					resolver.insert(Calls.CONTENT_URI,
							CallLogUtils.json2ContentValues(jobj));
				}
				klilog.i("load new2read, size =" + news);
				for (int i = 0; i < news; i++) {
					ContentValues cv = new ContentValues();
					cv.put(Calls.NEW, 0);
					resolver.update(Calls.CONTENT_URI, cv, Calls._ID + "= "
							+ new2readArray.getInt(i), null);
				}
				klilog.i("load update_name, size ="+names);
				for (int i = 0; i < names; i++) {
					JSONObject one = namesArray.getJSONObject(i);
					ContentValues cv = new ContentValues();
					int id = one.getInt(Calls._ID);
					String name=null;
					try{
						name = one.getString(Calls.CACHED_NAME);
						cv.put(Calls.CACHED_NAME, name);
					}catch(JSONException e){
						klilog.i("name get error. so it is null..putNull() for id ="+id);
						cv.putNull(Calls.CACHED_NAME);
					}
					resolver.update(Calls.CONTENT_URI, cv,
							Calls._ID + "=" + id, null);
				}
				klilog.i("Load data spend:"+String.valueOf(System.currentTimeMillis() - start));
			} catch (JSONException e) {
				e.printStackTrace();
				return RES_FAILED_WITH_DATA_CHAOS;
			}
			return RES_SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Message msg = mHandler.obtainMessage(MSG_LOAD_FINISH);
			msg.arg1 = result;
			msg.sendToTarget();
			MissedCallNotify.getInstance(mContext).queryAndNotifyMissedCall();
		}
	}
}

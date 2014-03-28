package cn.ingenic.glasssync;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;

import cn.ingenic.glasssync.LogTag.Tran;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.services.SyncData;

public class Transaction {
	
	private Config mConfig;
	protected Context mContext;
	@SuppressWarnings("unused")
	private ArrayList<Projo> mDatas;
	private Handler mHandler;
	
	public Config getConfig() {
		return mConfig;
	}

	public void onCreate(Config config, Context context) {
		if (LogTag.V) {
			Tran.d("Transaction onCreated.");
		}
		mConfig = config;
		mContext = context;
	}

	public void setHandler(Handler handler) {
		mHandler = handler;
	}

	public void onStart(ArrayList<Projo> datas) {
		if (LogTag.V) {
			Tran.d("Transaction onStarted.");
		}
		
		mDatas = datas;
	}
	
	/*public void onStart(SyncData data) {
		if (LogTag.V) {
			Tran.d("Transaction onStarted(SyncData).");
		}
	}*/

	public Handler getHandler() {
		return mHandler;
	}
}

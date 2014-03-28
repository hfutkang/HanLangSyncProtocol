package cn.ingenic.glasssync.syncdemo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public class MusicModule extends SyncModule {
	private static final String TAG = "music";
	private boolean mCreated = false;
	
	private static final String CMD = "cmd";
	private static final int START = 1;
	private static final int PAUSE = 2;

	public MusicModule(Context context) {
		super(TAG, context);
	}

	@Override
	protected void onCreate() {
		Log.d(TAG, "MusicModule onCreate");
		mCreated = true;
	}

	boolean isCreated() {
		return mCreated;
	}

	@Override
	protected void onRetrive(SyncData data) {
		int cmd = data.getInt(CMD);
		Log.d(TAG, "retrive data cmd:" + cmd);
		Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
		KeyEvent keyEvent;

		switch (cmd) {
		case START:
			keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
			keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
			mContext.sendOrderedBroadcast(keyIntent, null);
			
			keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY);
			keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
			mContext.sendOrderedBroadcast(keyIntent, null);
			break;
		case PAUSE:
			keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
			keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
			mContext.sendOrderedBroadcast(keyIntent, null);
			
			keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE);
			keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
			mContext.sendOrderedBroadcast(keyIntent, null);

			break;
		default:
			Log.e(TAG, "unknow cmd:" + cmd);
		}
	}

}

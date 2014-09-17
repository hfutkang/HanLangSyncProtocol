package cn.ingenic.glasssync.bluetooth;

import java.io.IOException;

import cn.ingenic.glasssync.bluetooth.camera.CameraManager;
import cn.ingenic.glasssync.bluetooth.decoding.CaptureActivityHandler;
import cn.ingenic.glasssync.bluetooth.decoding.InactivityTimer;
import cn.ingenic.glasssync.bluetooth.view.ViewfinderView;

import com.google.zxing.Result;

//import cn.ingenic.glasssync.*;
import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnTouchListener;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class CaptureActivity extends Activity implements Callback ,OnTouchListener{
    private static final String TAG = "CaptureActivity";
    private static final boolean DEBUG = true;
    private CaptureActivityHandler mHandler;
    private ViewfinderView mViewfinderView;
    private boolean mHasSurface;
    private String mCharacterSet;
      //private TextView txtResult;
    private InactivityTimer mInactivityTimer;
    private MediaPlayer mMediaPlayer;
    private boolean mPlayBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean mVibrate;
    // public  BluetoothAdapter sBluetoothAdapter=Welcome_Activity.sBluetoothAdapter;
    private GestureDetector mGestureDetector;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		CameraManager.init(getApplication());
		mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		FrameLayout fl=(FrameLayout) findViewById(R.id.layout_scan);
		fl.setOnTouchListener(this);
		mHasSurface = false;
		mInactivityTimer = new InactivityTimer(this);
		gestureDetectorWorker();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (mHasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		mCharacterSet = null;

		mPlayBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			mPlayBeep = false;
		}
		initBeepSound();
		mVibrate = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mHandler != null) {
			mHandler.quitSynchronously();
			mHandler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		mInactivityTimer.shutdown();
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (mHandler == null) {
			mHandler = new CaptureActivityHandler(this, null,
					mCharacterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!mHasSurface) {
			mHasSurface = true;
			initCamera(holder);
		}

	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return mViewfinderView;
	}

	public Handler getHandler() {
		return mHandler;
	}

	public void drawViewfinder() {
		mViewfinderView.drawViewfinder();

	}

	public void handleDecode(Result obj, Bitmap barcode) {
	    if(DEBUG) Log.e(TAG, "handleDecode:" + obj.getText());
		  mInactivityTimer.onActivity();
		  mViewfinderView.drawResultBitmap(barcode);
		  playBeepSoundAndVibrate();	  
		  Intent intent=new Intent(CaptureActivity.this,Load_Activity.class);
		  intent.putExtra("result", obj.getText());
		  startActivity(intent);
		  CaptureActivity.this.finish();
	}

	private void initBeepSound() {
		if (mPlayBeep && mMediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mMediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mMediaPlayer.prepare();
			} catch (IOException e) {
				mMediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (mPlayBeep && mMediaPlayer != null) {
			mMediaPlayer.start();
		}
		if (mVibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mMediaPlayer.seekTo(0);
		}
	};

    private void gestureDetectorWorker(){
	mGestureDetector =  new GestureDetector(this,new SimpleOnGestureListener() {
		      // Touch down时触发
		    @Override
			public boolean onDown(MotionEvent e) {
			if(DEBUG) Log.d(TAG,"---onDown in");
			return true;
		    }
		    
		    @Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					       float velocityX, float velocityY) {
			if(DEBUG) Log.d(TAG,"---velocityX="+velocityX+"--velocityY="+velocityY);
			if (velocityY > SysApplication.SNAP_VELOCITY 
			    && Math.abs(velocityX) < SysApplication.SNAP_VELOCITY) {
			    CaptureActivity.this.finish();
			}
			return true;
		    }
		    		    
	    });
    }

    @Override
	public boolean onTouch(View v, MotionEvent event) {
	return mGestureDetector.onTouchEvent(event);
    }	

}
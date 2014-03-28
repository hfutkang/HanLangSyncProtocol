package cn.ingenic.glasssync.camera;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.data.ProjoType;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.EnumSet;

public class CameraClient extends CameraBase implements OnChannelCallBack{

    private static final String TAG = "CameraClient";

    /*Camera client states*/
    private static final int STATE_DISCONNECT = 0;
    private static final int STATE_OPENNING_CAMERA = 1;
    private static final int STATE_CAMERA_IDLE = 2;
    private static final int STATE_TAKING_PICTURE = 3;
    private static final int STATE_BEFORE_CAPTURE_SUCCESS = 4;
    private static final int STATE_TAKE_PICTURE_SUCCESS = 5;
    private static final int STATE_BROWSE_PICTURE = 6;

	/*capture failed reasons*/
    private static final int FAILED_DEFAULT = 0; //from phone
    private static final int FAILED_SEND_REQUEST = 1;
    private static final int FAILED_DISCONNECT = 2;

    private static final String mPicPath = "/data/data/cn.ingenic.glasssync/pic.jpg";
    private static final String tmpPath = mPicPath + ".tmp";

    private volatile int mState;

    private int screenWidth;
    private int screenHeight;

    private ProgressDialog mProgressDialog;
    private AlertDialog mBrowseDialog;
    private SurfaceView mPreviewFrame;
    private ImageView mPreviewPic;
    private SurfaceHolder mHolder;

    private LinearLayout mControlLayout;
    private ImageView mControlButton;
    private ImageView mOperateButton;

    private RelativeLayout mTakingLayout;
    private ImageView mPhoneCamera;
    private ImageView mPhoneLandscape;
    private ImageView mPhoneCard;

    private Animation mTakingAnimation;
    private Animation mTakingRotateLong;
    private Animation mTakingRotateShort;
    private Animation mTakingAlpha;

    private byte[] mPictureData;
    
    /*Messages*/
    private static final int HIDE_PHONE_LANDSCAPE = 0;
    private static final int SHOW_BROSWE_PICTURE_DIALOG = 1;
    private static final int HIDE_TAKING_ANIMATION = 2;
    private static final int SEND_REQUEST_RESULT = 3;

    private boolean hasDisconnected = true;

    public void logi(String s) { Log.i(TAG, s); }
    public void loge(String s) { Log.e(TAG, s); }

    private Handler mHandler = new Handler(){
        public void  handleMessage (Message msg){
            switch (msg.what) {
			case HIDE_PHONE_LANDSCAPE:
				if (!inCapture()) return;
				mPhoneLandscape.setVisibility(View.GONE);
				mTakingRotateShort = AnimationUtils.loadAnimation(CameraClient.this, R.anim.taking_scale_anim);
				mPhoneCard.startAnimation(mTakingRotateShort);
				sendEmptyMessageDelayed(SHOW_BROSWE_PICTURE_DIALOG, 1000);
				break;

			case SHOW_BROSWE_PICTURE_DIALOG:
				if (!inCapture()) return;
				setState(STATE_TAKE_PICTURE_SUCCESS);
				break;
				
			case HIDE_TAKING_ANIMATION:
				if (mState == STATE_DISCONNECT) return;
				setState(STATE_CAMERA_IDLE);
				mOperateButton.startAnimation(mTakingAlpha);
				break;
	    
			case SEND_REQUEST_RESULT:
				switch(msg.arg1){
				case DefaultSyncManager.NO_CONNECTIVITY:
				case DefaultSyncManager.FEATURE_DISABLED:
				case DefaultSyncManager.NO_LOCKED_ADDRESS:
					sendRequestFailed();
					break;
				default:
					loge("invalid message!");
				}
				break;

			default :
				logi("handleMessage-------default");
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
							 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.camera_client);
		initView();
		setState(STATE_DISCONNECT);
		CameraTransaction.setCameraClient(this);
		WindowManager mWinManager = (WindowManager) getSystemService("window");
		screenWidth = mWinManager.getDefaultDisplay().getWidth();
		screenHeight= mWinManager.getDefaultDisplay().getHeight();
    }

    private void initView () {
		mPreviewFrame = (SurfaceView)findViewById(R.id.preview_frame);
		mHolder = mPreviewFrame.getHolder();

		mPreviewPic = (ImageView)findViewById(R.id.preview_pic);

		mControlLayout = (LinearLayout)findViewById(R.id.control_panel);
		mControlButton = (ImageView)findViewById(R.id.control_button);
		mOperateButton = (ImageView)findViewById(R.id.operate_button);

		mTakingLayout = (RelativeLayout)findViewById(R.id.taking_picture_layout);
		mPhoneCard = (ImageView)findViewById(R.id.save_card);
		mPhoneCamera = (ImageView)findViewById(R.id.phone_camera);
		mPhoneLandscape = (ImageView)findViewById(R.id.phone_landscape);
    }

    private void setState(final int state) {
        mState = state;
		logi("setState--------------------------------->"+state);
        switch (state) {
		case STATE_OPENNING_CAMERA:
			getProgressDialog().show();
			enableControls(false);
			break;

		case STATE_DISCONNECT:
			if (getProgressDialog().isShowing())
				getProgressDialog().hide();

			if (mBrowseDialog != null && mBrowseDialog.isShowing()){
				mBrowseDialog.hide();
			}

			mPreviewFrame.setVisibility(View.GONE);
			mPreviewPic.setVisibility(View.GONE);
			mTakingLayout.setVisibility(View.GONE);

			enableControls(true);
			break;

		case STATE_CAMERA_IDLE:
			mOperateButton.setImageResource(R.drawable.capture_button);
			mTakingLayout.setVisibility(View.GONE);
			mPreviewPic.setVisibility(View.GONE);
			mPreviewFrame.setVisibility(View.VISIBLE);
			break;

		case STATE_TAKING_PICTURE:
			mPreviewFrame.setVisibility(View.GONE);
			mPreviewPic.setVisibility(View.GONE);
			mTakingLayout.setVisibility(View.VISIBLE);
	
			mTakingRotateLong = AnimationUtils.loadAnimation(CameraClient.this, R.anim.taking_rotate_long);
			mPhoneCamera.startAnimation(mTakingRotateLong);
			break;

		case STATE_BEFORE_CAPTURE_SUCCESS:
			mPhoneLandscape.setVisibility(View.VISIBLE);
			mTakingAnimation = AnimationUtils.loadAnimation(this, R.anim.taking_anim);
			mPhoneLandscape.startAnimation(mTakingAnimation);
			break;

		case STATE_TAKE_PICTURE_SUCCESS:
			showBrowseDialog();
			break;

		case STATE_BROWSE_PICTURE:
			mPreviewFrame.setVisibility(View.GONE);
			mPreviewPic.setVisibility(View.VISIBLE);
			mOperateButton.setImageResource(R.drawable.go_back_from_picture);
			mTakingLayout.setVisibility(View.GONE);
			break;
		}

		if (disConnectState() || mState == STATE_OPENNING_CAMERA){
			mOperateButton.setVisibility(View.GONE);

			mControlLayout.setVisibility(View.VISIBLE);
			if (mTakingAlpha != null)
				mControlLayout.startAnimation(mTakingAlpha);

		} else if (inCapture() || mState == STATE_TAKE_PICTURE_SUCCESS){
			mControlLayout.setVisibility(View.GONE);
			mOperateButton.setVisibility(View.GONE);
		} else {
			mControlLayout.setVisibility(View.GONE);
			mOperateButton.setVisibility(View.VISIBLE);
		}
    }
	
    private void enableControls(boolean show){
		if (mControlLayout != null) mControlLayout.setEnabled(show);
    }

    public void controlCamera(View v){
		switch (mState) {
		case STATE_DISCONNECT:
			CameraModule.setChannelCallBack(this);

			/*send message to phone to open camera.*/
			sendRequestToServer(CameraTransaction.OPEN_CAMERA_REQUEST);
			setState(STATE_OPENNING_CAMERA);
			break;

		default:
			logi("controlCamera-------default");
		}
    }

    public void operateRemoteCamera(View v){
		switch (mState) {
		case STATE_CAMERA_IDLE:
		case STATE_TAKE_PICTURE_SUCCESS:
			logi("send message to remote phone to capture");
			sendRequestToServer(CameraTransaction.TAKE_PICTURE_REQUEST);
			
			setState(STATE_TAKING_PICTURE);
			break;
		case STATE_BROWSE_PICTURE:
			exitPicture();
			break;
		default:
			logi("controlCamera-------default");
		}
	
    }

    private void exitPicture(){
		mOperateButton.setImageResource(R.drawable.capture_button);
		if (hasDisconnected) {
			setState(STATE_DISCONNECT);
		} else {
			setState(STATE_CAMERA_IDLE);
		}
    }

    @Override
    public void onBackPressed() {
        if (mState == STATE_BROWSE_PICTURE) {
			exitPicture();
		} else if (mState != STATE_OPENNING_CAMERA && !inCapture()){
			super.onBackPressed();
        }
    }

    public void showOpenedResult(int mode){
		boolean state = (mode == CameraTransaction.OPEN_RESULT_SUCCESS);
		logi("open camera " + (state ? "success" : "failed") + "---------------" + mode);

		if (mTakingLayout != null) mTakingLayout.setVisibility(View.GONE);

		getProgressDialog().hide();
		if (state){
			hasDisconnected = false;
			mTakingAlpha = AnimationUtils.loadAnimation(this, R.anim.taking_alpha_anim);
			mOperateButton.startAnimation(mTakingAlpha);
			setState(STATE_CAMERA_IDLE);
			showToast(R.string.open_camera_success);
		} else {
			setState(STATE_DISCONNECT);

			switch(mode){
			//from watch
			case CameraTransaction.OPEN_RESULT_FAILED_DISCONN:
				showToast(R.string.open_camera_fail_bt);
				break;

			case CameraTransaction.OPEN_RESULT_FAILED_TIMEOUT:
				showToast(R.string.open_camera_fail_timeout);
				break;

			//from phone.
			case CameraTransaction.OPEN_RESULT_FAILED_POWER: 
				showToast(R.string.open_camera_fail_power);
				break;

			case CameraTransaction.OPEN_RESULT_FAILED_SENSOR:
				showToast(R.string.open_camera_fail_sensor);
				break;

			case CameraTransaction.OPEN_RESULT_FAILED_CHANNEL:
				showToast(R.string.open_camera_fail_channel);
				break;
			}
		}
    }

    public void takePictureResult(boolean result){
		takePictureResult(result, FAILED_DEFAULT);
    }

    public void takePictureResult(boolean result, int reason){
		logi("take picture " + (result ? "success" : "failed") + "---------------" + reason);

		if (disConnectState()) return;

		mPhoneCamera.clearAnimation();
		if (result) {
			setState(STATE_BEFORE_CAPTURE_SUCCESS);
			mHandler.sendEmptyMessageDelayed(HIDE_PHONE_LANDSCAPE, 1500);
			showToast(R.string.take_picture_success);
	    
		} else {
			switch(reason){
			case FAILED_DEFAULT:
				showToast(R.string.take_picture_fail);
				setState(STATE_CAMERA_IDLE);
				break;

			case FAILED_SEND_REQUEST:
				showToast(R.string.take_picture_fail_bt);
				setState(STATE_DISCONNECT);
				break;

			case FAILED_DISCONNECT:
				showToast(R.string.take_picture_fail_timeout);
				setState(STATE_DISCONNECT);
			}
		}
    }

    private void showBrowseDialog(){
		if (mBrowseDialog == null){
			mBrowseDialog = new AlertDialog.Builder(CameraClient.this)
                .setTitle(R.string.browse_picture)
				.setCancelable(false)
                .setOnKeyListener(new OnKeyListener(){
						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_BACK) {
								if (hasDisconnected) {
									setState(STATE_DISCONNECT);
								} else {
									mHandler.sendEmptyMessage(HIDE_TAKING_ANIMATION);
									mBrowseDialog.hide();
								}
								return true;
							}
							return false;
						}
					})
                .setPositiveButton(R.string.camera_alert_ok,
								   new DialogInterface.OnClickListener() {
									   public void onClick(DialogInterface dialog, int whichButton) {
										   setState(STATE_BROWSE_PICTURE);
										   
										   if (mPicPath != null){
											   Bitmap bitmap = BitmapFactory.decodeFile(mPicPath);
											   bitmap = CameraUtil.rotateBitmap(bitmap, getDefaultAngle());
											   mPreviewPic.setImageBitmap(bitmap);
										   }
									   }
                            })
				.setNegativeButton(R.string.camera_alert_cancel,
								   new DialogInterface.OnClickListener() {
									   public void onClick(DialogInterface dialog, int whichButton) {
										   if (hasDisconnected) {
											   setState(STATE_DISCONNECT);
										   } else {
											   mHandler.sendEmptyMessage(HIDE_TAKING_ANIMATION);
										   }
									   }
								   })
				.create();
		}
		mBrowseDialog.show();
    }

    private void dismissBrowseDialog(){
		if (mBrowseDialog != null){
			mBrowseDialog.dismiss();
			mBrowseDialog = null;
		}
    }

    private Dialog getProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(CameraClient.this,  R.style.CustomProgressDialog);
            mProgressDialog.setMessage(getString(R.string.wait_open_camera_message));

            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        return mProgressDialog;
    }

    @Override
    protected void onDestroy() {
		// dismiss the dialog.
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
		dismissBrowseDialog();
		CameraModule.setChannelCallBack(null);
		super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

		if (!disConnectState()){
			//send message to close remote camera
			sendRequestToServer(CameraTransaction.EXIT_CAMERA_REQUEST);
			disconnectCameraFromPhone();
		}
    }

    public void setFrameToDisplay(byte[] data){
		if (mState == STATE_BROWSE_PICTURE || mState == STATE_TAKING_PICTURE) return;
		//Log.d(TAG,Log.getStackTraceString(new Throwable("stack dump")));
		Rect dst = new Rect(mPreviewFrame.getLeft(), 
							mPreviewFrame.getTop(), 
							mPreviewFrame.getRight(),
							mPreviewFrame.getBottom());

		setFrameToDisplay(data, mHolder, dst);
    }

    private void sendRequestToServer(int request){
		logi("CameraClient--sendRequestToServer--------------------------------->"+request);
		ArrayList<Projo> datas = new ArrayList<Projo>(1);
        Projo projo = new DefaultProjo(EnumSet.of(CameraColumn.watchRequest), ProjoType.DATA);
        projo.put(CameraColumn.watchRequest, request);
        datas.add(projo);

		if (request == CameraTransaction.OPEN_CAMERA_REQUEST){
			int maxBound = (screenWidth > screenHeight) ? screenWidth : screenHeight;
			Projo maxProjo = new DefaultProjo(EnumSet.of(CameraColumn.maxScreen), ProjoType.DATA);
			maxProjo.put(CameraColumn.maxScreen, maxBound);
			datas.add(maxProjo);
		}

		Message msg = mHandler.obtainMessage(SEND_REQUEST_RESULT);
		Config config = new Config(CameraModule.CAMERA);
		config.mCallback = msg;
        DefaultSyncManager.getDefault().request(config, datas);
    }

    public void disconnectCameraFromBT(){
		logi("disconnectCameraFromBT");
		if (mState == STATE_DISCONNECT) 
			return;
		else if (mState == STATE_OPENNING_CAMERA){
			showOpenedResult(CameraTransaction.OPEN_RESULT_FAILED_TIMEOUT);
		} else if (mState == STATE_TAKING_PICTURE){
			takePictureResult(false/*failed*/, FAILED_DISCONNECT);
		} else if (mState == STATE_CAMERA_IDLE) {
			setState(STATE_DISCONNECT);
		}
		hasDisconnected = true;
	    showToast(R.string.no_connection);
    }

    public void disconnectCameraFromPhone(){
		logi("disconnectCameraFromPhone");
		if (mState == STATE_DISCONNECT) return;
		setState(STATE_DISCONNECT);
		showToast(R.string.close_camera_message);
    }

    @Override
    public void onCreateComplete(boolean success, boolean local){
		logi("watch------->onCreateComplete:"+success);
    }

    @Override
    public void onRetrive(ProjoList projoList){
		ArrayList<Projo> datas = (ArrayList<Projo>) projoList.get(ProjoListColumn.datas);
		Projo data = datas.get(0);
		setFrameToDisplay((byte[]) data.get(CameraColumn.previewData));
    }

    private boolean inPreview(){
		return (mState == STATE_CAMERA_IDLE);
    }

    private boolean inCapture(){
		return mState == STATE_TAKING_PICTURE ||
			mState == STATE_BEFORE_CAPTURE_SUCCESS;
    }

    public boolean disConnectState(){
		return (mState == STATE_DISCONNECT);
    }

    @Override
    public void onDestory(){
		logi("watch-----OnChannelCallBack--->onDestory()");
    }

    public void setPictureData(byte[] picData){
		mPictureData = picData;

		FileOutputStream out = null;
		try {
            out = new FileOutputStream(tmpPath);
            out.write(picData);
            out.close();
            new File(tmpPath).renameTo(new File(mPicPath));
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.switch_camera_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.switch_camera:
			sendRequestToServer(CameraTransaction.SWITCH_CAMERA_REQUEST);
			break;
		case R.id.rotate_preview:
			setDefaultAngle((getDefaultAngle() + 90) % 360);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.switch_camera).setEnabled(inPreview());
		menu.findItem(R.id.rotate_preview).setEnabled(inPreview());
		return true;
    }

    private void sendRequestFailed() {
		switch (mState) {
		case STATE_OPENNING_CAMERA:
			showOpenedResult(CameraTransaction.OPEN_RESULT_FAILED_DISCONN);
			break;

		case STATE_TAKING_PICTURE:
			takePictureResult(false/*failed*/, FAILED_SEND_REQUEST);
			break;

		default:
			
		}
    }
}

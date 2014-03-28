package cn.ingenic.glasssync.phone;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;
import cn.ingenic.glasssync.devicemanager.Commands;

import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.GlowPadView.OnTriggerListener;

/**
 * when receive ring state, display this activity to operator the Phone 
 * Just as package/apps/Phone's InCallScreen
 * */
public class InCall extends Activity implements OnTriggerListener{

    public static final String TAG="SyncPhone";
    /*package*/ static int mState=0, mOldState=0;
    /*package*/ static String mName0,mPhoneNumber0;
    /*package*/ static final int ACCEPT_ID = 22 , REJECT_ID =21;
    private final static int NOTIFICATION_ID = 314;
    private static final boolean ENABLE_PING_AUTO_REPEAT = true;
    private static final int ACCEPTING=3;
    private static final int DISCONNECTING=4;
    
    private final int TIME_OVER = 0x123;
    private final int TICKING_UP = 0X234;
    private final int INCOMING_WIDGE_PING = 0x345;
    private final int WAKE_SCREEN = 0x456;
    private final int HOLD_CONNECT=0X002;
    
    private final int RingDelay = 65000;       //65s
    private final int DisconnectingDelay = 5000;  //5 s
    private final int AcceptingDelay = 10000; //10s
    private TextView mName, mPhoneNumber, mElapsedTime, mCallStateLabel;
    private Button mEndCall/*, mAnswerButton*/;
    private GlowPadView mIncomingCallWidget;  // UI used for an incoming call
    private boolean mIncomingCallWidgetShouldBeReset = true;
    private Ringer mRinger;
    private RespondViaSmsManager mRespondViaSmsManager;
    
    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock mWakeLock;
    private static Timer mTimer = new Timer();
    private static int mTimeLeft = 0;

    private Handler mHandler = new Handler(){
        int m=0,s=0;
        public void handleMessage(Message msg){
            switch (msg.what) {
            case TIME_OVER:
                log("set state = 0; because of time over");
                mState = 0;
                if (mRinger.isRinging())
                    mRinger.stopRing();
                stopTimer();
                finish();
                break;
            case TICKING_UP:
                mTimeLeft++;
                m = mTimeLeft/60;
                s = mTimeLeft%60;
                mElapsedTime.setVisibility(View.VISIBLE);
                mElapsedTime.setText((m < 10 ? "0" + m : m) + ":"
                        + (s < 10 ? "0" + s : s));
                break;
            case INCOMING_WIDGE_PING:
                mIncomingCallWidget.ping();
                if(ENABLE_PING_AUTO_REPEAT){
                    sendEmptyMessageDelayed(INCOMING_WIDGE_PING, 500);
                }
                break;
            case WAKE_SCREEN:
                if (mWakeLock != null)
                    mWakeLock.acquire();
                break;
            case HOLD_CONNECT:
                DefaultSyncManager.getDefault().holdOnConnTemporary(PhoneModule.PHONE);
                sendEmptyMessageDelayed(HOLD_CONNECT, DefaultSyncManager.TIMEOUT-2000);
                break;
            }
        } //end void-handleMessage
    };
    
    private MyBR mBr;
    
    class MyBR extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state != BluetoothAdapter.STATE_ON) { // bt off
                    log("set state = 0; because of bluetooth off");
                    mState = 0;
                    if(mRinger.isRinging())
                        mRinger.stopRing();
                    stopTimer();
                    finish();
                }
            }else if (action.equals(Commands.ACTION_BLUETOOTH_STATUS)) {
                boolean con=intent.getBooleanExtra("data", false);
                if (!con) {
                    log("set state = 0; because of bluetooth connection ="+con);
                    mState = 0;
                    if(mRinger.isRinging())
                        mRinger.stopRing();
                    stopTimer();
                    finish();
                }
            }
        }
        
    } 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mContext= this ;
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_in_call);
        mName =(TextView)findViewById(R.id.name);
        mPhoneNumber=(TextView)findViewById(R.id.phoneNumber);
        mElapsedTime = (TextView)findViewById(R.id.elapsedTime);
        mCallStateLabel=(TextView)findViewById(R.id.callStateLabel);
        mIncomingCallWidget=(GlowPadView)findViewById(R.id.incomingCallWidget);
        mIncomingCallWidget.setOnTriggerListener(this);
        mEndCall = (Button)findViewById(R.id.endButton);
        mEndCall.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0) {
                operatorCall(REJECT_ID);
            }
        });
//        mAnswerButton=(Button)findViewById(R.id.answerButton);
//        mAnswerButton.setOnClickListener(new OnClickListener(){
//            @Override
//            public void onClick(View arg0) {
//                ;//operatorCall(ACCEPT_ID);
//            }
//        });
//        mAnswerButton.setVisibility(View.GONE);
        mRinger=Ringer.init(this);
        mRespondViaSmsManager=new RespondViaSmsManager();
        mRespondViaSmsManager.setInCallScreenInstance(this);
        
        PowerManager pm = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "glasssync");
        if (!pm.isScreenOn()) {
            mHandler.sendEmptyMessageAtTime(WAKE_SCREEN, 500);
        }
        
        mNotificationManager= (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        mBr=new MyBR();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Commands.ACTION_BLUETOOTH_STATUS);//sync connected?
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);// bt on/off
        this.registerReceiver(mBr, filter);
        mHandler.sendEmptyMessageDelayed(HOLD_CONNECT, 5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNotificationManager.cancel(NOTIFICATION_ID);
        Intent i = getIntent();
        mOldState = mState;
        mState = i.getIntExtra("state", 100);
        if (!(mOldState == 1 && mState == 2)) {
            //if accept a call, do not get number,because the number is null
            mName0 = i.getStringExtra("name");
            mPhoneNumber0 = i.getStringExtra("phoneNumber");
        }
        if( mState ==100){
            Log.w(TAG,"state is 100, return");
            this.finish(); //return;
        }
        if (mState != mOldState)
            notifyCallState(mState,mPhoneNumber0);
        refreshDisplay(mState,mName0,mPhoneNumber0);
        if (LogTag.V) {
            log("onResume . state : "+mOldState+" --> "+mState+
                    " , name:"+mName0+", "+mPhoneNumber0);
        }
    }

    @Override
    protected void onDestroy(){
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        this.unregisterReceiver(mBr);
        mHandler.removeMessages(HOLD_CONNECT);
        super.onDestroy();
    }
    
    @Override
    protected void onStop(){
        super.onStop();
        if (LogTag.V) {
            log("onStop . state : " + mState + " , name:" + mName0 + ", "
                    + mPhoneNumber0);
        }
        if (mState == 0){
            stopTimer();
            notifyCallState(0,""); //notify  framework layer  state changed
            return;
        }
        Intent ii=new Intent(this,InCall.class);
        ii.putExtra("state", mState);
        ii.putExtra("name", mName0);
        ii.putExtra("phoneNumber", mPhoneNumber0);
        final Notification.Builder builder = new Notification.Builder(this);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setSmallIcon(R.drawable.ic_lockscreen_answer_activated);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, ii
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0));
        builder.setWhen(System.currentTimeMillis()+10);
        builder.setAutoCancel(false).setOngoing(true);;
        if (mState == 1){
            builder.setContentTitle(getString(R.string.ring));
        }else{
            builder.setContentTitle(getString(R.string.offhook));
        }
        Notification notification = builder.getNotification();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_in_call, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //if(item.getItemId()==R.menu.activity_in_call)
        return super.onOptionsItemSelected(item);
    }

    private void startTimer(int howLong){
        if(mTimer != null){
            stopTimer();
        }
        mTimer = new Timer();
        mTimeLeft = howLong;
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(TICKING_UP);
            }
        }, 0, 1000);// send every 1 s ,for time ++
    }
    
    private void stopTimer(){
        if(mTimer!=null){
            mTimer.cancel();
            mTimer=null;
        }
    }
    /** notify  other apps the call state */
    private void notifyCallState(int state,String number){
        if (state > -1 && state < 3) {
            Log.d(InCall.TAG, "notify [Framework] call state =" + state);
            if(state == 0) stopTimer();
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.setCallState(state, number);
        }
    }
    /*package*/ void refreshDisplay(int state,String name ,String phoneNumber){

        mPhoneNumber.setText(phoneNumber);
        if (TextUtils.isEmpty(name)) {
            mName.setVisibility(View.GONE);
        } else
            mName.setText(name);
        if(state == TelephonyManager.CALL_STATE_IDLE){// 0
        	if(mRinger.isRinging())
        		mRinger.stopRing();
            stopTimer();
            finish();
        }else if(state == ACCEPTING){               // 3
            mCallStateLabel.setText(R.string.accepting);
            mCallStateLabel.setVisibility(View.VISIBLE);
//            mAnswerButton.setVisibility(View.GONE);
            if(mRinger.isRinging()){
                mRinger.stopRing();
                mHandler.removeMessages(TIME_OVER);
            }
            mHandler.sendEmptyMessageDelayed(TIME_OVER, AcceptingDelay);
        }else if (state == DISCONNECTING){          // 4
            mCallStateLabel.setText(R.string.disconnecting);
            mCallStateLabel.setVisibility(View.VISIBLE);
//            mAnswerButton.setVisibility(View.GONE);
            if(mRinger.isRinging()){
                mRinger.stopRing();
                mHandler.removeMessages(TIME_OVER);
            }
            mHandler.sendEmptyMessageDelayed(TIME_OVER, DisconnectingDelay);
        }else if (state == TelephonyManager.CALL_STATE_RINGING){    // 1
            mCallStateLabel.setText(R.string.ring);
            mCallStateLabel.setVisibility(View.VISIBLE);
            mElapsedTime.setVisibility(View.GONE);
            if(!mRinger.isRinging())
                mRinger.ring();
            showIncomingCallWidget();
//            mAnswerButton.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessageDelayed(TIME_OVER, RingDelay);
        }else if (state == TelephonyManager.CALL_STATE_OFFHOOK){    // 2
            log("refresh. "+mOldState+"->"+mState);
            if (mOldState != 2)
                startTimer(0);
            else
                startTimer(mTimeLeft);
            mCallStateLabel.setVisibility(View.GONE);
            if(mRinger.isRinging()){
            	mRinger.stopRing();
            	mHandler.removeMessages(TIME_OVER);
            }
            hideIncomingCallWidget();
//            mAnswerButton.setVisibility(View.GONE);
        }
    }

    /*package*/ void operatorCall(int operatorCode){
        if (LogTag.V) {
            log( "send  to Phone Client ,"+operatorCode);
        }
        if (operatorCode == REJECT_ID) {
            mState = DISCONNECTING;
        } else if (operatorCode == ACCEPT_ID) {
            mState = ACCEPTING;
        } else
            return;
        refreshDisplay(mState,mName0,mPhoneNumber0);
        Projo projo = new DefaultProjo(EnumSet.allOf(PhoneColumn.class), ProjoType.DATA);
        projo.put(PhoneColumn.state, operatorCode);
        Config config = new Config(PhoneModule.PHONE);
        ArrayList<Projo> datas = new ArrayList<Projo>(1);
        datas.add(projo);
        DefaultSyncManager.getDefault().request(config, datas);
        //Toast.makeText(this, "Send  to Phone : "+operatorCode, Toast.LENGTH_LONG).show();
    }
    
    
    // below is for lockScreen about accept or decline RingCall --from InCallTouchUi------
    private void showIncomingCallWidget(){
        log("showIncomingCallwidget....");
        ViewPropertyAnimator animator = mIncomingCallWidget.animate();
        if (animator != null) {
            animator.cancel();
        }
        mIncomingCallWidget.setAlpha(1.0f);

        final int targetResourceId = R.array.incoming_call_widget_2way_targets;
        
        if (mIncomingCallWidget.getTargetResourceId()!=targetResourceId){
            mIncomingCallWidget.setTargetResources(targetResourceId);
            mIncomingCallWidget.setTargetDescriptionsResourceId(
                    R.array.incoming_call_widget_2way_target_descriptions);
            mIncomingCallWidget.setDirectionDescriptionsResourceId(
                    R.array.incoming_call_widget_2way_direction_descriptions);
            
            mIncomingCallWidgetShouldBeReset = true;
        }
        
        if(mIncomingCallWidgetShouldBeReset){
            mIncomingCallWidget.reset(false);
            mIncomingCallWidgetShouldBeReset=false;
        }
        
        mIncomingCallWidget.setVisibility(View.VISIBLE);
        mHandler.removeMessages(INCOMING_WIDGE_PING);
        mHandler.sendEmptyMessageDelayed(INCOMING_WIDGE_PING, 250/**/);
    }
    
    @Override
    public void onGrabbed(View v, int handle){};
    @Override
    public void onReleased(View v, int handle){}
    @Override
    public void onTrigger(View v, int target){
        
        //mShowInCallControlsDuringHidingAnimation= false;
        log("onTrigger target = "+target);
        switch(target){
        case 0:    //Right, accept ; dfdun modify it to decline via SMS
            //operatorCall(ACCEPT_ID);
            mRespondViaSmsManager.showRespondViaSmsPopup(mPhoneNumber0);
            break;
        case 1:   // Up , response via SMS
            break;
        case 2:     //Left , hungup
            operatorCall(REJECT_ID);
            break;
        }
        hideIncomingCallWidget();
    }
    
    public void onGrabbedStateChange(View v, int handle){}
    public void onFinishFinalAnimation(){}
    
    private void hideIncomingCallWidget(){
        log("hide IncamingCallWidget----hide");
        if(mIncomingCallWidget.getVisibility()!=View.VISIBLE)
            return ;
        ViewPropertyAnimator animator = mIncomingCallWidget.animate();
        animator.cancel();
        animator.setDuration(250);
        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //if (mShowInCallControlsDuringHidingAnimation) {
                    //updateInCallControls(mApp.mCM);
                    //mInCallControls.setVisibility(View.VISIBLE);
                //}
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mIncomingCallWidget.setAlpha(1);
                mIncomingCallWidget.setVisibility(View.GONE);
                mIncomingCallWidget.animate().setListener(null);
                mIncomingCallWidgetShouldBeReset = true;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                mIncomingCallWidget.animate().setListener(null);
                mIncomingCallWidgetShouldBeReset = true;
            }
        });
        animator.alpha(0f);
    }
    
    private void log(String msg){
        Log.i(TAG, "InCall]"+msg);
    }
}

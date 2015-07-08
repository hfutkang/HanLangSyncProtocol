package cn.ingenic.glasssync.calllog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ingenic.glasssync.R;
import com.ingenic.glass.voicerecognizer.api.VoiceRecognizerActivity;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterPagedView;
import android.widget.SimpleAdapter;
import android.widget.AdapterPagedView.OnDownSlidingBackListener;
import android.widget.AdapterPagedView.OnItemClickListener;
import android.content.ComponentName;

public class missedCallAction extends VoiceRecognizerActivity implements OnItemClickListener,OnDownSlidingBackListener{
    public static final String TAG = "missedCallAction";
    private final String[] VOICE_CMDS = { "回拨", "回短信" };
    private AdapterPagedView mActionPagedView;
    private String mUserName;
    private String mContact;
	@Override
    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.missed_call_menu);
	    mActionPagedView = (AdapterPagedView) findViewById(R.id.menu);
	    SimpleAdapter mAdapter = new SimpleAdapter(this, getData(), R.layout.missed_call_menu_list, new String[] {"title", "img" }, new int[] { R.id.tv_menu,R.id.iv_menuIcon });
	    mActionPagedView.setAdapter(mAdapter);
	    mUserName = getIntent().getStringExtra("mUserName");
	    mContact = getIntent().getStringExtra("mContact");
	    mActionPagedView.setOnDownSlidingBackListener(this);
	    mActionPagedView.setOnItemClickListener(this);

	    setAppName(getResources().getString(R.string.app_name));
	    for (int i = 0; i < VOICE_CMDS.length; i++) {
		addRecognizeCommand(VOICE_CMDS[i]);
	    }
	    setAppIcon(R.drawable.luncher);
	}
    private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("title",getApplication().getResources().getString(R.string.call_back));
		map.put("img", R.drawable.call_back);
		list.add(map);
		map = new HashMap<String, Object>();
		map.put("title", getApplication().getResources().getString(R.string.send_message));
		map.put("img", R.drawable.send_message);
		list.add(map);
		return list;
	}
	@Override
	public void onDownSlidingBack(AdapterPagedView pagedView) {
		// TODO Auto-generated method stub
	    finish();
	}
	@Override
	public void onItemClick(AdapterPagedView pagedView, View view, int position) {
		switch (position) {
		case 0:
		    dialBack();
		    break;
		case 1:
		    replySms();
		    break;
		default:
		    break;
		
	}
	}

        private void dialBack() {
	    Intent intentCall = new Intent("com.ingenic.phone");
	    intentCall.putExtra("mUserName", mUserName);
	    intentCall.putExtra("contact", mContact);
	    intentCall.putExtra("start", "contact");
	    intentCall.putExtra("info",2);
	    startActivity(intentCall);
        }

        private void replySms() {
	    ComponentName componenName=new ComponentName("cn.ingenic.glasssync.sms","cn.ingenic.glasssync.sms.activity.SmsSendMessage");
	    Intent intentSms =new Intent();
	    Bundle bundle =new Bundle();
	    bundle.putString("mContact", mContact);
	    bundle.putString("mUserName", mUserName);
	    intentSms.setComponent(componenName);
	    intentSms.putExtras(bundle);
	    startActivity(intentSms);
        }

        @Override
    	protected boolean onCommandResult(String result, float score) {
	    Log.d(TAG, "onCommandResult " + result + " " + score);
	    if (score > -11.0f) {
		if (VOICE_CMDS[0].equals(result)) {//回拨
		    dialBack();
		}else if (VOICE_CMDS[1].equals(result)) {//回短信
		    replySms();
		}

	    }
	    return false;
	}
    
        @Override
	protected boolean onExit(float score) {
	    Log.d(TAG, "onExit " + score);
	    if (score > -11.0f) {
		finish();
		return true;
	    }
	    return false;
        }

}
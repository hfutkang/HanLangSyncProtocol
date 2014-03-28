package cn.ingenic.glasssync.phone;

import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;

/** @author dfdun<br>
 * watch: receive the data , and make some action depended on the data received 
 * */
public class PhoneTransaction extends Transaction {

    static int mState = 0 , mOldState = 0;
    static String mName,mIncomingNumber;
    @Override
    public void onStart(ArrayList<Projo> datas) {
        super.onStart(datas);
        if("true".equals(android.os.SystemProperties.get("ro.device.headsetmode"))){
            Log.d(InCall.TAG,"PhoneTransaction] ro.device.headsetmode=true");
        return;
        }
        int size = datas.size();
        if (LogTag.V) {
            //Log.d(InCall.TAG, "PhoneTransaction] datas size = "+size);
        }
        if (1 == size) {
            Projo data = datas.get(0);
            int state = (Integer) data.get(PhoneColumn.state);

            String name = "", phoneNumber = "";
            if (state >= 0 && state < 3) {
                if (mState != state) { // received new state
                    name = (String) data.get(PhoneColumn.name);
                    phoneNumber = (String) data.get(PhoneColumn.phoneNumber);
                    mOldState = mState;
                    mState = state;
                    mName = name;
                    mIncomingNumber = phoneNumber;
                    StartInCall(state, name, phoneNumber);
                }
            } else if (state == 30) { // update shared_pref for resopnse via sms
                name = (String) data.get(PhoneColumn.name);
                phoneNumber = (String) data.get(PhoneColumn.phoneNumber);
                updatePref(name, phoneNumber);
            }
            if (LogTag.V) {
                Log.d(InCall.TAG, "PhoneTransaction] received state: " + state
                        + ", name: " + name + ",number: " + phoneNumber
                        + " , STATE: " + mOldState + "->" + mState);
            }
        }else {
            for (Projo data : datas) {
                int state = (Integer) data.get(PhoneColumn.state);
                if (state == 30) {
                    String name = (String) data.get(PhoneColumn.name);
                    String phoneNumber = (String) data.get(PhoneColumn.phoneNumber);
                    updatePref(name, phoneNumber);
                }
            }
        }
    }

    private void updatePref(String key , String value){
        Log.d(InCall.TAG, "PhoneTransaction] updatePreference: "+key+" , "+value);
        SharedPreferences prefs = mContext.getSharedPreferences(RespondViaSmsManager.SHARED_PREFERENCES_NAME, 0);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(key, value);
        e.commit();
    }

    private void StartInCall(int state,String name,String phoneNumber) {
        Intent intent ;
        //if (android.os.Build.VERSION.SDK_INT > 15) { // 4.1 is 16
        intent= new Intent(mContext,cn.ingenic.glasssync.phone.InCall.class);
        //} 
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        intent.putExtra("phoneNumber", phoneNumber);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(InCall.TAG, "**+1+start Activity failed: " + e);
            Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
        }catch(java.lang.NoClassDefFoundError e){
            Log.e(InCall.TAG, "**+2+start Activity failed: " + e);
            Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
        }
        //return intent;
    }

}

package cn.ingenic.glasssync.phone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;

/**
 *  merage from Phone/src/...
 * Helper class to manage the "Respond via SMS" feature for incoming calls.
 * @see InCallScreen.internalRespondViaSms()
 * @author dfdun
 */
public class RespondViaSmsManager {
    private static final String TAG = "RespondViaSmsManager";
    private static final boolean DBG =true;
    private static final boolean VDBG = false;
    private Dialog mPopup;
    /** The array of "canned responses"; see loadCannedResponses(). */
    private String[] mCannedResponses;
    /** SharedPreferences file name for our persistent settings. */
    public static final String SHARED_PREFERENCES_NAME = "respond_via_sms_prefs";

    // Preference keys for the 4 "canned responses"; see RespondViaSmsManager$Settings.
    // Since (for now at least) the number of messages is fixed at 4, and since
    // SharedPreferences can't deal with arrays anyway, just store the messages
    // as 4 separate strings.
    private static final int NUM_CANNED_RESPONSES = 4;
    private static final String KEY_CANNED_RESPONSE_PREF_1 = "canned_response_pref_1";
    private static final String KEY_CANNED_RESPONSE_PREF_2 = "canned_response_pref_2";
    private static final String KEY_CANNED_RESPONSE_PREF_3 = "canned_response_pref_3";
    private static final String KEY_CANNED_RESPONSE_PREF_4 = "canned_response_pref_4";

    private InCall mInCall;
    public static final int SYNC_SMS_CODE= 30;// opeator code
    
    public RespondViaSmsManager() {
    }
    public void setInCallScreenInstance(InCall inCallScreen) {
        mInCall = inCallScreen;
        if (mInCall != null) {
            mInCall.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
    }
    /**
     * Brings up the "Respond via SMS" popup for an incoming call.
     *
     * @param ringingCall the current incoming call
     */
    public void showRespondViaSmsPopup(String  number) {
        // Very quick succession of clicks can cause this to run twice.
        // Stop here to avoid creating more than one popup.
        if(isShowingPopup()){
            return;
        }

        ListView lv = new ListView(mInCall);

        // Refresh the array of "canned responses".
        mCannedResponses = loadCannedResponses();

        // Build the list: start with the canned responses, but manually add
        // "Custom message..." as the last choice.
//        int numPopupItems = mCannedResponses.length + 1;
//        String[] popupItems = Arrays.copyOf(mCannedResponses, numPopupItems);
//        popupItems[numPopupItems - 1] = mInCall.getResources()
//                .getString(R.string.respond_via_sms_custom_message);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(mInCall,
                                         android.R.layout.simple_list_item_1,
                                         android.R.id.text1,
                                         mCannedResponses);
        lv.setAdapter(adapter);

        // Create a RespondViaSmsItemClickListener instance to handle item
        // clicks from the popup.
        // (Note we create a fresh instance for each incoming call, and
        // stash away the call's phone number, since we can't necessarily
        // assume this call will still be ringing when the user finally
        // chooses a response.)

        lv.setOnItemClickListener(new RespondViaSmsItemClickListener(number));

        AlertDialog.Builder builder = new AlertDialog.Builder(mInCall)
                .setCancelable(true)
                .setOnCancelListener(new RespondViaSmsCancelListener())
                .setView(lv);
        mPopup = builder.create();
        mPopup.show();
    }

    /**
     * Dismiss the "Respond via SMS" popup if it's visible.
     *
     * This is safe to call even if the popup is already dismissed, and
     * even if you never called showRespondViaSmsPopup() in the first
     * place.
     */
    public void dismissPopup() {
        if (mPopup != null) {
            mPopup.dismiss();  // safe even if already dismissed
            mPopup = null;
        }
    }

    public boolean isShowingPopup() {
        return mPopup != null && mPopup.isShowing();
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {
        // Phone number to send the SMS to.
        private String mPhoneNumber;

        public RespondViaSmsItemClickListener(String phoneNumber) {
            mPhoneNumber = phoneNumber;
        }

        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                                View view,  // The TextView that was clicked
                                int position,
                                long id) {
            String message = (String) parent.getItemAtPosition(position);

            // The "Custom" choice is a special case.
            // (For now, it's guaranteed to be the last item.)
            if (position == (parent.getCount() - 1)) {
                // Take the user to the standard SMS compose UI.
                //launchSmsCompose(mPhoneNumber); TODO
                sendText(mPhoneNumber, message);
            } else {
                // Send the selected message immediately with no user interaction.
                sendText(mPhoneNumber, message);

                // ...and show a brief confirmation to the user (since
                // otherwise it's hard to be sure that anything actually
                // happened.)
//                final Resources res = mInCall.getResources();
//                String formatString = res.getString(R.string.respond_via_sms_confirmation_format);
//                String confirmationMsg = String.format(formatString, mPhoneNumber);
//                Toast.makeText(mInCall,
//                               confirmationMsg,
//                               Toast.LENGTH_LONG).show();
            }

            // At this point the user is done dealing with the incoming call, so
            // there's no reason to keep it around.  (It's also confusing for
            // the "incoming call" icon in the status bar to still be visible.)
            // So reject the call now.
            mInCall.operatorCall(InCall.REJECT_ID);
            dismissPopup();
            mInCall.refreshDisplay(InCall.mState,InCall.mName0,InCall.mPhoneNumber0);
        }
    }

    /**
     * OnCancelListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsCancelListener implements DialogInterface.OnCancelListener {
        public RespondViaSmsCancelListener() {
        }

        /**
         * Handles the user canceling the popup, either by touching
         * outside the popup or by pressing Back.
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            //:TODO
            dismissPopup();
            mInCall.refreshDisplay(InCall.mState,InCall.mName0,InCall.mPhoneNumber0);
        }
    }

    /**
     * Sends a text message without any interaction from the user.
     */
    private void sendText(String phoneNumber, String message) {
        Projo projo = new DefaultProjo(EnumSet.allOf(PhoneColumn.class), ProjoType.DATA);
        projo.put(PhoneColumn.state, 31);
        projo.put(PhoneColumn.name, message);
        projo.put(PhoneColumn.phoneNumber, phoneNumber);
        Config config = new Config(PhoneModule.PHONE);
        ArrayList<Projo> datas = new ArrayList<Projo>(1);
        datas.add(projo);
        DefaultSyncManager.getDefault().request(config, datas);
        Toast.makeText(mInCall, phoneNumber+" : "+message, Toast.LENGTH_SHORT).show();
    }


    /**
     * Read the (customizable) canned responses from SharedPreferences,
     * or from defaults if the user has never actually brought up
     * the Settings UI.
     *
     * This method does disk I/O (reading the SharedPreferences file)
     * so don't call it from the main thread.
     *
     * @see RespondViaSmsManager.Settings
     */
    private String[] loadCannedResponses() {

        SharedPreferences prefs =
            mInCall.getSharedPreferences(SHARED_PREFERENCES_NAME,
                                                   Context.MODE_PRIVATE);
        final Resources res = mInCall.getResources();

        String[] responses = new String[NUM_CANNED_RESPONSES];

        // Note the default values here must agree with the corresponding
        // android:defaultValue attributes in respond_via_sms_settings.xml.

        responses[0] = prefs.getString(KEY_CANNED_RESPONSE_PREF_1,
                                       res.getString(R.string.respond_via_sms_canned_response_1));
        responses[1] = prefs.getString(KEY_CANNED_RESPONSE_PREF_2,
                                       res.getString(R.string.respond_via_sms_canned_response_2));
        responses[2] = prefs.getString(KEY_CANNED_RESPONSE_PREF_3,
                                       res.getString(R.string.respond_via_sms_canned_response_3));
        responses[3] = prefs.getString(KEY_CANNED_RESPONSE_PREF_4,
                                       res.getString(R.string.respond_via_sms_canned_response_4));
        return responses;
    }

}

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ingenic.glasssync.bluetoothPan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import cn.ingenic.glasssync.DefaultSyncManager;
import android.content.SharedPreferences;
import java.util.List;

/**
 * PanProfile handles Bluetooth PAN profile (NAP and PANU).
 */
public class PanProfile {
    private static final String TAG = "PanProfile";
    public static final String PAN_CONNECT = "cn.ingenic.glasssync.panConnet";
    public static final String PAN_DISCONNECT = "cn.ingenic.glasssync.panDisConnet";
    private static boolean V = true;

    private BluetoothPan mService;
    private boolean mIsProfileReady;
    public static PanProfile sInstance;
    // Tethering direction for each device
    static final String NAME = "PAN";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 4;
    private Context mContext;
    private BluetoothDevice mDevice; 
    
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
	    final String statu = intent.getAction();
            Log.e(TAG,"PanProfile STATUS = "+statu);
	    if(statu.equals(PAN_CONNECT)){
		connect(getConnectedDevices());
	    }
	    if(statu.equals(PAN_DISCONNECT)){
		disconnect(mDevice);
	    }
        }
    };
    
    // These callbacks run on the main thread.
    private final class PanServiceListener implements BluetoothProfile.ServiceListener {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (V) Log.d(TAG,"Bluetooth service connected profile="+profile);
            mService = (BluetoothPan) proxy;
            mIsProfileReady=true;
        }

        public void onServiceDisconnected(int profile) {
            if (V) Log.d(TAG,"Bluetooth service disconnected profile="+profile);
            mIsProfileReady=false;
        }
    }

    public boolean isProfileReady() {
        return mIsProfileReady;
    }

    public static PanProfile getInstance(Context c) {
	if (null == sInstance){
	    sInstance = new PanProfile(c);
	}
	return sInstance;
    }

    PanProfile(Context context) {
	mContext = context;
	mIntentFilter = new IntentFilter();
	mIntentFilter.addAction(PAN_CONNECT);
	mIntentFilter.addAction(PAN_DISCONNECT);
	mIntentFilter.addAction("receiver.action.STATE_CHANGE");
	context.registerReceiver(mReceiver, mIntentFilter);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(context, new PanServiceListener(),
                BluetoothProfile.PAN);
    }

    private void connect(BluetoothDevice device) {
        if (mService == null) return ;
        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                mService.disconnect(sink);
            }
        }
	if (V) Log.d(TAG,"--connect");
        mService.connect(device);
    }

    private void disconnect(BluetoothDevice device) {
        if (mService == null) return;
	List<BluetoothDevice> sinks = mService.getConnectedDevices();
        mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mService.getConnectionState(device);
    }
    
    public BluetoothDevice getConnectedDevices(){
	// SharedPreferences sp = mContext.getSharedPreferences("settings", Context.MODE_PRIVATE);
	// Log.e("dsfa","getLockedAddress()" + sp.getString("unique_address", ""));	
       String address = DefaultSyncManager.getDefault().getLockedAddress();
       Log.e(TAG,"---address = "+address);	
       if(address.equals(""))
	   return null;
       mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
       return mDevice;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    protected void finalize() {
        if (V) Log.d(TAG, "finalize()");
        if (mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.PAN, mService);
                mService = null;
            }catch (Throwable t) {
                Log.w(TAG, "Error cleaning up PAN proxy", t);
            }
        }
    }
}

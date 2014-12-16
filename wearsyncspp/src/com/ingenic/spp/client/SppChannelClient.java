/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.ingenic.spp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class SppChannelClient extends SppChannel{
    // Debugging
    private static final String TAG = "SppChannelClient";
    private static final boolean D = true;
    // Message types sent from the  Handler.
    private   static final int MESSAGE_BASE           = 100;
    private   static final int MESSAGE_CONNECT        = MESSAGE_BASE+1;
    private   static final int MESSAGE_DISCONNECT     = MESSAGE_BASE+2;
    private   static final int MESSAGE_CONN_FAILED    = MESSAGE_BASE+3;
    private   static final int MESSAGE_CONN_LOST      = MESSAGE_BASE+4;
    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;

    private UUID mSdpUuid;
    private boolean mIsSecure;
    private BluetoothDevice mBindingDevice;
    private static SppChannelClient sChannel;

    private void addClientMessages() {
        addChannelMessage(MESSAGE_CONNECT, "MESSAGE_CONNECT");
        addChannelMessage(MESSAGE_DISCONNECT, "MESSAGE_DISCONNECT");
        addChannelMessage(MESSAGE_CONN_FAILED, "MESSAGE_CONN_FAILED");
        addChannelMessage(MESSAGE_CONN_LOST, "MESSAGE_CONN_LOST");
    }

    private SppChannelClient(OnChannelListener listener) {
        super(TAG, listener);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        HandlerThread ht = new HandlerThread("SppChannelClient");
        ht.start();
        mHandler = new ClientHandler(ht.getLooper());
        addClientMessages();
    }

    public static SppChannelClient getDefault(OnChannelListener listener) {
        if (sChannel == null) {
            sChannel = new SppChannelClient(listener);
        }
        return sChannel;
    }

    private synchronized void internalInit() {
        if (D) Log.d(TAG, "internalInit");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(null, STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */

    public void connect(String addr, UUID uuid, boolean secure) {
        Log.v(TAG, "connect addr: "+addr+" length: "+addr.length());

        if (addr == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(KEY_DEVICE_ADDR, addr);
        bundle.putString(KEY_SDP_UUID, uuid.toString());
        bundle.putBoolean(KEY_SECURE, secure);
        mHandler.obtainMessage(MESSAGE_CONNECT, bundle).sendToTarget();
    }
    private synchronized void internalConnect(BluetoothDevice device, UUID uuid, boolean secure) {
        if (D) Log.d(TAG, "internalconnect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, uuid, secure);
        mConnectThread.start();
        setState(null, STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected device addr: "+device.getAddress()
                   +" name: "+device.getName()+" Socket Type:" + socketType);
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        setState(device.getAddress(), STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public void disconnect() {
        Log.v(TAG, "disconnect");
        mHandler.sendEmptyMessage(MESSAGE_DISCONNECT);
    }

    private synchronized void internalStop() {
        if (D) Log.d(TAG, "internalStop");
         
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(null, STATE_NONE);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONN_FAILED);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TOAST_PROMPT, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    @Override
    protected void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONN_LOST);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TOAST_PROMPT, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID mmUuid;
        private String mSocketType;

        ConnectThread(BluetoothDevice device, UUID uuid, boolean secure) {
            mmDevice = device;
            mmUuid = uuid;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(uuid);
                } else {
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.v(TAG, "start connect addr: "+mmDevice.getAddress()+" uuid: "+mmUuid);
                mmSocket.connect();
                Log.v(TAG, "finish connect");
            } catch (Exception e) {
                // Close the socket
                try {
                    Log.v(TAG, "close mmSocket e: "+e); 
                    mmSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SppChannelClient.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        void cancel() {
            try {
                Log.v(TAG, "close mmSocket"); 
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ClientHandler extends SppChannel.ChannelHandler {

        public ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    int state = msg.arg1;
                    mListener.onStateChanged(state, (String)msg.obj);
                    break;
                case MESSAGE_CONN_FAILED:
                case MESSAGE_CONN_LOST:
                    SppChannelClient.this.internalInit();
                    if (mBindingDevice == null) {
                        break;
                    }
                    removeMessages(MESSAGE_CONNECT);
                    Bundle bdl = new Bundle();
                    bdl.putString(KEY_DEVICE_ADDR, mBindingDevice.getAddress());
                    bdl.putString(KEY_SDP_UUID, mSdpUuid.toString());
                    bdl.putBoolean(KEY_SECURE, mIsSecure);
                    Message message = obtainMessage(MESSAGE_CONNECT, bdl);
                    sendMessageDelayed(message, 4000);
                    break;
                case MESSAGE_CONNECT:
                    Bundle bundle = (Bundle)msg.obj;
                    String addr = bundle.getString(KEY_DEVICE_ADDR);
                    UUID uuid = UUID.fromString(bundle.getString(KEY_SDP_UUID));
                    boolean secure = bundle.getBoolean(KEY_SECURE);
                    /*
                    if (mBindingDevice != null &&
                            addr.equals(mBindingDevice.getAddress()) 
                            && mState != STATE_NONE) {
                        return;
                    }
                    */
                    mBindingDevice = mAdapter.getRemoteDevice(addr);
                    mSdpUuid = uuid;
                    mIsSecure = secure;
                    internalConnect(mBindingDevice, uuid, secure);
                    break;
                case MESSAGE_DISCONNECT:
                     removeMessages(MESSAGE_CONNECT); 
                     internalStop();
                     mBindingDevice = null;
                     mSdpUuid = null;
                    break;
                case MESSAGE_QUIT:
                    removeMessages(MESSAGE_CONNECT); 
                    if (hasMessages(MESSAGE_WRITE) || hasMessages(MESSAGE_READ)) {
                        removeMessages(MESSAGE_QUIT);
                        sendEmptyMessage(MESSAGE_QUIT);
                        break;
                    }
                    getLooper().quit();
                    mHandler = null;
                    break;
            }//end switch.
        }//end handleMessage.
    };//end mHandler.

    public void stop() {
        //do nothing.
    }


}




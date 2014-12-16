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
class SppChannelServer extends SppChannel{
    // Debugging
    private static final String TAG = "SppChannelServer";
    private static final boolean D = true;
   
    // Message types sent from the  Handler.
    private static final int MESSAGE_BASE           = 200;
    private static final int MESSAGE_LISTEN         = MESSAGE_BASE+1;
    private static final int MESSAGE_STOP           = MESSAGE_BASE+2;
    private static final int MESSAGE_CONN_LOST      = MESSAGE_BASE+3;

    // Member fields
    private UUID mSdpUuid;
    private String mServiceName;
    private boolean mIsSecure;
    private final BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private static SppChannelServer sSppChannelServer;

    private void addServerMessages() {
        addChannelMessage(MESSAGE_LISTEN, "MESSAGE_LISTEN");
        addChannelMessage(MESSAGE_STOP, "MESSAGE_STOP");
        addChannelMessage(MESSAGE_CONN_LOST, "MESSAGE_CONN_LOST");
    }
    /**
     * Constructor. Prepares a new JzBtChannMgr session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    private SppChannelServer(OnChannelListener listener) {
        super(TAG, listener);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        HandlerThread ht = new HandlerThread("SppChannelServer");
        ht.start();
        mHandler = new ServerHandler(ht.getLooper());
        addServerMessages();
    }

    public static SppChannelServer getDefault(OnChannelListener listener) {
        if (sSppChannelServer == null) {
            sSppChannelServer = new SppChannelServer(listener);
        }
        return sSppChannelServer;
    }
    
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void listen(String name, UUID uuid, boolean secure) {
        if (D) Log.d(TAG, "listen");
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SERVICE_NAME, name);
        bundle.putString(KEY_SDP_UUID, uuid.toString());
        bundle.putBoolean(KEY_SECURE, secure);
        Message msg = mHandler.obtainMessage(MESSAGE_LISTEN, bundle);
        mHandler.sendMessage(msg);
    }

    private synchronized void internalInit() {
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) { mAcceptThread.cancel(); mAcceptThread = null;}
        setState(null, STATE_NONE);
    }

    private synchronized void internalListen(String name, UUID uuid, boolean secure) {
        if (D) Log.d(TAG, "internalListen");
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel(); 
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(null, STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread(name, uuid, secure);
            mAcceptThread.start();
        }
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

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        setState(device.getAddress(), STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public void stop() {
        if (D) Log.d(TAG, "stop");
        mHandler.sendEmptyMessage(MESSAGE_STOP);
    }

    private synchronized void internalStop() {
        if (D) Log.d(TAG, "internalStop");
         
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        
        setState(null, STATE_NONE);
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

        // Start the service over to restart listening mode
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        AcceptThread(String name, UUID uuid, boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                Log.v(TAG, "start listen uuid: "+uuid+" secure: "+secure);
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
                } else {
                    //tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
                }
            } catch (IOException e) {
                Log.e(TAG, "Listen IOException socket: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    " BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            if (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.v(TAG, "start accept");
                    socket = mmServerSocket.accept();
                    Log.v(TAG, "finish accept");
                } catch (IOException e) {
                    Log.e(TAG, "Accept exception Type: " + mSocketType + "accept() failed", e);
                } catch (Exception e) {
                    Log.e(TAG, "Accept exception socket: "+socket);
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (SppChannelServer.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
                            break;
                        default:    
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                if (D) Log.i(TAG, "AcceptThread close socket "+this);
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                } else {
                    connectionLost();
                }
            }

        }

        void cancel() {
            if (D) Log.d(TAG, "close mmListenSocket");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed "+e);
            } catch (Exception e) {
                Log.e(TAG, "accept mmServerSocket: "+mmServerSocket);
            }
        }
    }

    private class ServerHandler extends ChannelHandler {

        public ServerHandler(Looper looper) {
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
                case MESSAGE_LISTEN:
                    Bundle bundle = (Bundle)msg.obj;
                    String name = bundle.getString(KEY_SERVICE_NAME);
                    UUID uuid = UUID.fromString(bundle.getString(KEY_SDP_UUID));
                    boolean secure = bundle.getBoolean(KEY_SECURE);
                    /*
                    if (mState != STATE_NONE && uuid.equals(mSdpUuid)) {
                        break;
                    }
                    */
                    mSdpUuid = uuid;
                    mServiceName = name;
                    mIsSecure = secure;
                    SppChannelServer.this.internalListen(name, uuid, secure);
                    break;
                case MESSAGE_CONN_LOST:
                    SppChannelServer.this.internalInit();
                    if (mSdpUuid == null) {
                        break;
                    }
                    Bundle bdl = new Bundle();
                    bdl.putString(KEY_SERVICE_NAME, mServiceName);
                    bdl.putString(KEY_SDP_UUID, mSdpUuid.toString());
                    bdl.putBoolean(KEY_SECURE, mIsSecure);
                    removeMessages(MESSAGE_LISTEN);
                    Message message = obtainMessage(MESSAGE_LISTEN, bdl);
                    sendMessageDelayed(message, 1000);
                    break;
                case MESSAGE_STOP:
                    removeMessages(MESSAGE_LISTEN);
                    removeMessages(MESSAGE_CONN_LOST);
                    mSdpUuid = null;
                    mServiceName = null;
                    internalStop();
                    break;
                case MESSAGE_QUIT:
                    removeMessages(MESSAGE_LISTEN);
                    removeMessages(MESSAGE_CONN_LOST);
                    if (hasMessages(MESSAGE_WRITE) || hasMessages(MESSAGE_READ)) {
                        removeMessages(MESSAGE_QUIT);
                        sendEmptyMessage(MESSAGE_QUIT);
                        break;
                    }
                    getLooper().quit();
                    mHandler = null;
                    sSppChannelServer = null;
                    break;
            }//end switch.
        }//end handleMessage.
    };//end mHandler.

    public void connect(String add) {
        //Do nothing.
    }

}




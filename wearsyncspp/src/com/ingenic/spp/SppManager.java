package com.ingenic.spp;

import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import android.util.Log;

public class SppManager {
    private static final boolean DBG = true;
    private static  String TAG = "SppManager";

    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a62");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a64");
    private static final String STRING_SERVICE_NAME = "custom_sync_data";
    
    private static final int MESSAGE_LISTEN        = 0;
    private static final int MESSAGE_CONNECT       = 1;
    private static final int MESSAGE_DISCONNECT    = 2;
    private static final int MESSAGE_WRITE         = 3;
    private static final int MESSAGE_FEEDBACK      = 4;
    private static final int MESSAGE_STOP          = 5;
    private static final int MESSAGE_QUIT          = 6;

    private Handler mConnHandler;
    private Handler mWriteHandler;
    private Handler mListenerHandler;
    private ArrayList<CellData> mReadList = new ArrayList<CellData>();
    private SppChannel mChannel = null;
    private OnChannelListener mListener;
    private volatile int mConnState;
    private volatile int mWriteLength;

    private Object mWriteMonitor = new Object();

    public SppManager(String tag, OnChannelListener listener, boolean isServer) {
        TAG = tag;
        mConnState = OnChannelListener.STATE_NONE;
        mListener = listener;
        if (isServer) {
            mChannel = SppChannelServer.getDefault(mSppListener);
        } else {
            mChannel = SppChannelClient.getDefault(mSppListener);
        }
        HandlerThread ht = new HandlerThread("spp_channel");
        ht.start();
        mConnHandler = new SppConnHandler(ht.getLooper());
        
        HandlerThread ll = new HandlerThread("spp_listener");
        ll.start();
        mListenerHandler = new SppListenerHandler(ht.getLooper());
        
        HandlerThread sw = new HandlerThread("spp_write", Process.THREAD_PRIORITY_AUDIO+6);
       // sw.setPriority(Thread.MAX_PRIORITY);
        sw.start();
        //sw.setPriority(Thread.MAX_PRIORITY);
        mWriteHandler = new SppWriteHandler(sw.getLooper());
    }

    private class SppConnHandler extends Handler {
        public SppConnHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG) Log.v(TAG, "SppConnHandler handleMessage msg: "+msg.what);
            switch (msg.what) {
                case MESSAGE_LISTEN:
                    Bundle bundle = (Bundle)msg.obj;
                    String  name = bundle.getString(SppChannel.KEY_SERVICE_NAME);
                    UUID    uuid = UUID.fromString(bundle.getString(SppChannel.KEY_SDP_UUID));
                    boolean secure = bundle.getBoolean(SppChannel.KEY_SECURE);
                    mChannel.listen(name, uuid, secure);
                    break;
                case MESSAGE_CONNECT:
                    Bundle bdl = (Bundle)msg.obj;
                    String addr = bdl.getString(SppChannel.KEY_DEVICE_ADDR);
                    UUID id = UUID.fromString(bdl.getString(SppChannel.KEY_SDP_UUID));
                    boolean sec = bdl.getBoolean(SppChannel.KEY_SECURE);
                    mChannel.connect(addr, id, sec);
                    break;
                case MESSAGE_DISCONNECT:
                    mChannel.disconnect();
                    break;
                case MESSAGE_STOP:
                    mChannel.stop();
                    break;
                case MESSAGE_QUIT:
                    getLooper().quit();
                    break;
                default:
                    break;
            }
        }//end handleMessage.
    }

    private class SppListenerHandler extends Handler {
        public SppListenerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG) Log.v(TAG, "SppListenerHandler handleMessage msg: "+msg.what);
            switch (msg.what) {
                case MESSAGE_FEEDBACK:
                    CellData data = null;
                    synchronized (mReadList) {
                        if (mReadList.size() == 0) { 
                            break;
                        }
                        data = mReadList.remove(0);
                    }
                    mListener.onRead(data.buf, data.err);
                    sendEmptyMessage(MESSAGE_FEEDBACK);
                    break;
            }//end switch.
        }//end handleMessage.
    }//end SppListenerHandler.


    private class SppWriteHandler extends Handler {
        public SppWriteHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG) Log.v(TAG, "1 SppWriteHandler handleMessage msg: "+msg.what+" priority: "+
                    Thread.currentThread().getPriority());
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] buf = (byte[])msg.obj;
                    int offset = msg.arg1;
                    int len = msg.arg2;
                    mWriteLength = mChannel.write(buf, offset, len);
                    synchronized (mWriteMonitor) {
                        mWriteMonitor.notify();
                    }
                    break;
            }//end switch.
        }//end handleMessage.
    }//end SppListenerHandler.


    private class CellData {
        public byte[] buf;
        public int err;
        public CellData(byte[] data, int error) {
            buf = data;
            err = error;
        }
    }
    //////////////////////////////////////////////////
    public void start() {
        start(STRING_SERVICE_NAME, MY_UUID_SECURE, true);
    }

    public void start(String name, UUID uuid, boolean secure) {
        Bundle bundle = new Bundle();
        bundle.putString(SppChannel.KEY_SERVICE_NAME, name);
        bundle.putString(SppChannel.KEY_SDP_UUID, uuid.toString());
        bundle.putBoolean(SppChannel.KEY_SECURE, secure);
        mConnHandler.obtainMessage(MESSAGE_LISTEN, bundle).sendToTarget();
    }

    public void connect(String addr) {
        connect(addr, MY_UUID_SECURE, true);
    }

    public void connect(String addr, UUID uuid, boolean secure) {
        Bundle bundle = new Bundle();
        bundle.putString(SppChannel.KEY_DEVICE_ADDR, addr);
        bundle.putString(SppChannel.KEY_SDP_UUID, uuid.toString());
        bundle.putBoolean(SppChannel.KEY_SECURE, secure);
        mConnHandler.obtainMessage(MESSAGE_CONNECT, bundle).sendToTarget();
    }

    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    synchronized public int write(byte[] data, int offset, int len) {
        if (mConnState != OnChannelListener.STATE_CONNECTED) {
            Log.w(TAG, "write error: there is no connection now");
            return -1;
        }
        if (DBG) Log.v(TAG, "request write data len: "+len);
        synchronized (mWriteMonitor) {
            mWriteHandler.obtainMessage(MESSAGE_WRITE, offset, len, data).sendToTarget();
            try {
                mWriteMonitor.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "mWriteMonitor. error: "+e);
                return -1;
            }
        }
        return mWriteLength;
    }

    public void disconnect() {

        mConnHandler.obtainMessage(MESSAGE_DISCONNECT).sendToTarget();
    }

    public void stop() {
        mConnHandler.obtainMessage(MESSAGE_STOP).sendToTarget();
    }

    private OnChannelListener mSppListener = new OnChannelListener() {
        public void onStateChanged(int state, String addr) {
            mConnState = state;
            mListener.onStateChanged(state, addr);
        }
   
        public void onWrite(byte[] buf, int len, int err) {
            if(DBG) Log.v(TAG, "onWrite buf err: "+err);
            mListener.onWrite(buf, len, err);
        }

        public void onRead(byte[] buf, int err) {
            synchronized (mReadList) {
                mReadList.add(new CellData(buf, err));
            }
            mListenerHandler.obtainMessage(MESSAGE_FEEDBACK).sendToTarget();
        }
    };

}



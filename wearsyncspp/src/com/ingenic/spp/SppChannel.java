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

import java.util.UUID;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
abstract class SppChannel {
    // Debugging
    private static  String TAG = "SppChannel";
    private static final boolean DBG = true;
    private static final boolean mReadSync = false;  
    private static final boolean mSendAck = false;

    protected static final String KEY_DEVICE_NAME   = "key_device_name";
    protected static final String KEY_DEVICE_ADDR   = "key_device_addr";
    protected static final String KEY_SERVICE_NAME  = "key_service_name";
    protected static final String KEY_SECURE        = "key_secure";
    protected static final String KEY_SDP_UUID      = "key_sdp_uuid";
    protected static final String KEY_TOAST_PROMPT  = "key_toast_prompt";

    private   static final int MESSAGE_BASE           = 0;
    protected static final int MESSAGE_WRITE          = MESSAGE_BASE+1;
    protected static final int MESSAGE_READ           = MESSAGE_BASE+2;
    protected static final int MESSAGE_QUIT           = MESSAGE_BASE+3;
    protected static final int MESSAGE_STATE_CHANGE   = MESSAGE_BASE+4;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    // Member fields
    protected volatile int mState;
    protected ConnectedThread mConnectedThread;
    protected Handler mHandler;
    protected OnChannelListener mListener;

    private final int HEADER_IND_SIZE = 4;
    private int mClientSerialNum;
    private int mServerSerialNum;
    private HashMap<Integer, String> mMsgList = new HashMap<Integer, String>();

    public void addChannelMessage(Integer key, String msgStr) {
        mMsgList.put(key, msgStr);
    }

    public SppChannel(String tag, OnChannelListener listener) {
        TAG = tag;
        mState = STATE_NONE;
        mListener = listener;
        
        addChannelMessage(MESSAGE_STATE_CHANGE, "MESSAGE_STATE_CHANGE");
        addChannelMessage(MESSAGE_WRITE, "MESSAGE_WRITE");
        addChannelMessage(MESSAGE_READ, "MESSAGE_READ");
        addChannelMessage(MESSAGE_QUIT, "MESSAGE_QUIT");
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    public synchronized void setState(String addr, int state) {
        if (DBG) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1, addr).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public int write(byte[] out, int offset, int len) {
        if (out.length < offset+len) {
            len = out.length-offset;
            Log.w(TAG, "writting data len is changed into "+len);
        }
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                Log.w(TAG, "There is no connection");
                return -1;
            }
            r = mConnectedThread;
        }
        mClientSerialNum += 1;
        // Perform the write unsynchronized
        return r.write(out, offset, len, DataHeader.TYPE_USER_DATA, mClientSerialNum);
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, String socketType) {
            if (DBG) Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = new BufferedInputStream(tmpIn);
            mmOutStream = new BufferedOutputStream(tmpOut);
            //mmInStream  = tmpIn;
            //mmOutStream = tmpOut;
        }

        public void run() {
            if (DBG) Log.i(TAG, "BEGIN mConnectedThread");
            int readLen;
            mClientSerialNum = 0;
            mServerSerialNum = 0;
            byte[] headerIndbuf = new byte[HEADER_IND_SIZE];
            int error = OnChannelListener.ERROR_READ;
            byte[] databuf = null;
            DataHeader dataHeader = null;
            //setPriority(Thread.MAX_PRIORITY);
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    int priority = Thread.currentThread().getPriority();
                    if (DBG) Log.v(TAG, "-------------- start read ------------ priority: "+priority);
                    //read header size.
                    error = OnChannelListener.ERROR_READ;
                    dataHeader = null;
                    databuf = new byte[0];
                    long startUptime =  SystemClock.uptimeMillis();
                    long startTime = SystemClock.currentThreadTimeMillis();
                    long startElapsedTime = SystemClock.elapsedRealtime();
                    readLen = 0;
                    while (readLen < HEADER_IND_SIZE) {
                        readLen += mmInStream.read(headerIndbuf, readLen, (HEADER_IND_SIZE-readLen));
                        if (DBG) Log.v(TAG, "---- headIndLen: "+readLen);
                    }
                    int headerDataLen = readInt(headerIndbuf, 0);
                    if (DBG) Log.v(TAG, "headerDataLen: "+headerDataLen);
                    byte[] headerbuf = new byte[headerDataLen];
                    readLen = 0;
                    while (readLen < headerDataLen) {
                        readLen += mmInStream.read(headerbuf, readLen, (headerDataLen-readLen));
                        if (DBG) Log.v(TAG, "---- readLen: "+readLen);
                    }
                    dataHeader = DataHeader.fromBytes(headerbuf);
                    int dataLen = dataHeader.mmDataLen;
                    if (DBG) Log.v(TAG, "data type: "+dataHeader.mmDataType+" length: "+dataLen);
                    databuf = new byte[dataLen];
                    readLen = 0;
                    // Read from the InputStream
                    while (readLen < dataLen) {
                        readLen += mmInStream.read(databuf, readLen, dataLen-readLen);
                        if (DBG) Log.v(TAG, "data readLen: "+readLen+" remainder: "+(dataLen-readLen));
                    }
                    long endTime = SystemClock.currentThreadTimeMillis();
                    long endUptime =  SystemClock.uptimeMillis();
                    long endElapsedTime = SystemClock.elapsedRealtime();

                    long interval_threadTime = endTime-startTime;
                    long interval_uptime = endUptime-startUptime;
                    long interval_elapsedTime = endElapsedTime-startElapsedTime;
                    if (interval_threadTime > 1000 || interval_uptime > 10000 || interval_elapsedTime > 1000) {
                        if (DBG) Log.v(TAG, "Interval time is exceeded");
                    }
                    if (DBG) Log.v(TAG, "---------------- finish read length: "+dataLen);
                    if (DBG) Log.v(TAG, "read interval_threadTime: "+interval_threadTime+" interval_uptime: "+
                            interval_uptime+" interval_elapsedTime: "+interval_elapsedTime);
                    if (DBG)Log.v(TAG, "----read serial number: "+dataHeader.mmSerialNum);
                    error = OnChannelListener.ERROR_NONE;
                    // Send the obtained bytes to the UI Activity
                } catch (IOException e) {
                    Log.e(TAG, "read IOException: "+e);
                    connectionLost();
                    break;
                } catch (NullPointerException e) {
                    Log.e(TAG, "NullPoniterException: "+ e);
                    connectionLost();
                    break;
                } finally {
                    if (error != OnChannelListener.ERROR_NONE) {
                        mHandler.obtainMessage(MESSAGE_READ, error, -1, databuf).sendToTarget();
                        return;
                    }
                    if (dataHeader.mmDataType == DataHeader.TYPE_USER_DATA) {
                        mHandler.obtainMessage(MESSAGE_READ, error, -1, databuf).sendToTarget();
                        
                        if (mSendAck && (dataHeader.mmSerialNum+1)%21 == 0) {
                            byte[] numbuf = new byte[4];
                            writeInt(numbuf, 0, dataHeader.mmSerialNum);
                            mServerSerialNum += 1;
                            write(numbuf, 0, numbuf.length, DataHeader.TYPE_ACK, mServerSerialNum);
                        }
                    }
                }
            }//end while.
        }

        synchronized int write(byte[] data, int offset, int length, int type, int serialNum) {
            int error = OnChannelListener.ERROR_WRITE;
            int write_len = -1;
            try {
                byte[] dataHeader = DataHeader.toBytes(new DataHeader(type, serialNum, length));
                byte[] headerbuf = new byte[HEADER_IND_SIZE+dataHeader.length+length];
                //transmit header size.
                if (DBG) Log.v(TAG, "prepare dataHeader length: "+headerbuf.length);
                writeInt(headerbuf, 0, dataHeader.length);
                //transmit header data.
                System.arraycopy(dataHeader, 0, headerbuf, HEADER_IND_SIZE, dataHeader.length);
                //transmit real data.
                System.arraycopy(data, offset, headerbuf, (HEADER_IND_SIZE+dataHeader.length), length);
                int priority = Thread.currentThread().getPriority();
                if (DBG) Log.v(TAG, "=============== start write data"+" priority: "+priority);
                    long startUptime =  SystemClock.uptimeMillis();
                    long startTime = SystemClock.currentThreadTimeMillis();
                    long startElapsedTime = SystemClock.elapsedRealtime();
                mmOutStream.write(headerbuf);
                if (DBG) Log.v(TAG, "=============== start   flush =========");
                mmOutStream.flush();
                    long endTime = SystemClock.currentThreadTimeMillis();
                    long endUptime =  SystemClock.uptimeMillis();
                    long endElapsedTime = SystemClock.elapsedRealtime();

                    long interval_threadTime = endTime-startTime;
                    long interval_uptime = endUptime-startUptime;
                    long interval_elapsedTime = endElapsedTime-startElapsedTime;
                    if (interval_threadTime > 1000 || interval_uptime > 10000 || interval_elapsedTime > 1000) {
                        if (DBG) Log.v(TAG, "Interval time is exceeded");
                    }
                    if (DBG) Log.v(TAG, "write interval_threadTime: "+interval_threadTime+"interval_uptime: "+
                            interval_uptime+" interval_elapsedTime: "+interval_elapsedTime);
                if (DBG) Log.v(TAG, "=============== end   flush data len: "+length+" SN: "+serialNum);
                error = OnChannelListener.ERROR_NONE;
                write_len = length;
                // Share the sent message back to the UI Activity
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            } finally {
                if (type == DataHeader.TYPE_USER_DATA) {
                    mHandler.obtainMessage(MESSAGE_WRITE, length, error, data).sendToTarget();
                }
            }
            return write_len;
        }

        void cancel() {
            try {
                if (DBG) Log.v(TAG, "close mmSocket");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }//end ConnectedThread.

    protected class ChannelHandler extends Handler {

        public ChannelHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "----handleMessage msg: "+mMsgList.get(msg.what));
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    int writtenLen = msg.arg1;
                    int writtenError = msg.arg2;
                    mListener.onWrite(writeBuf, writtenLen, writtenError);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    int readError = msg.arg1;
                    mListener.onRead(readBuf, readError);
                    break;
            }//end switch.
        }//end handleMessage.
    }

    private static int readInt(byte[] buf, int offset) {
        int val = ((buf[offset]&0xff)
        		| ((buf[offset+1]&0xff) << 8)
        		| ((buf[offset+2]&0xff) << 16)
        		| ((buf[offset+3]&0xff) << 24));
        return val;
    }
    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    private static void writeInt(byte[] buf, int offset, int value) {
            buf[offset] = (byte) (value);
            buf[offset+1] = (byte) (value >> 8);
            buf[offset+2] = (byte) (value >> 16);
            buf[offset+3] = (byte) (value >> 24);
    }

    private static class DataHeader implements Serializable {
        public static final int TYPE_USER_DATA  = 0;
        public static final int TYPE_ACK = 1;
        public static final int MEMBER_NUM = 3;
        public static final int BYTES_PER_MEMBER = 4;
        public int mmDataType;
        public int mmSerialNum;
        public int mmDataLen;
        public DataHeader (int type, int num, int dataLen) {
            mmDataType = type;
            mmSerialNum = num;
            mmDataLen = dataLen;
        }

        public static byte[] toBytes(DataHeader header) {
             byte[] bytes = new byte[BYTES_PER_MEMBER*MEMBER_NUM];
             writeInt(bytes, BYTES_PER_MEMBER*0, header.mmDataType);
             writeInt(bytes, BYTES_PER_MEMBER*1, header.mmSerialNum);
             writeInt(bytes, BYTES_PER_MEMBER*2, header.mmDataLen);
             return bytes;
        }
        
        public static DataHeader fromBytes(byte[] bytes) {
            if (bytes == null) return null;
            int[] mem = new int[MEMBER_NUM];
            for (int i = 0; i < mem.length; i++) {
                mem[i] = readInt(bytes, BYTES_PER_MEMBER*i);
            }
            DataHeader header = new DataHeader(mem[0], mem[1], mem[2]);
            return header;
        }
    }

    protected abstract void connectionLost();
    protected abstract void stop();
    protected void listen() {}
    protected void listen(String name, UUID uuid, boolean secure) {}
    protected void connect(String addr) {}
    protected void connect(String addr, UUID uuid, boolean secure) {}
    protected void disconnect() {}

}







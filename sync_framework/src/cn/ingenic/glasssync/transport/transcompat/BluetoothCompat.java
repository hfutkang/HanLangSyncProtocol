package cn.ingenic.glasssync.transport.transcompat;

import java.util.ArrayList;
import com.ingenic.spp.SppClientManager;
import com.ingenic.spp.SppServerManager;
import com.ingenic.spp.SppManager;
import com.ingenic.spp.OnChannelListener;
import java.io.IOException;
import android.util.Log;

public class BluetoothCompat {
    private static String TAG = "BluetoothCompat";
    private static final boolean DBG = true;

    private static final int MESSAGE_READ_ERROR = 0;
    private static BluetoothCompat sClientCompat;
    private static BluetoothCompat sServerCompat;

    private SppManager mSppMgr;
    private volatile CellData mCellData;
    private volatile int mConnState;
    private final ArrayList<CellData> mReadList = new ArrayList<CellData>();

    private String mDevAddr;

    private OnChannelListener mCallback;
    private OnChannelListener mListener = new OnChannelListener() {
        public void onStateChanged(int state, String addr) {
            synchronized(mReadList) {
                mConnState = state;
                if (state == OnChannelListener.STATE_CONNECTED) {
                    mDevAddr = addr;
                    mReadList.clear();
                    mCellData = null;
                } else if (state == OnChannelListener.STATE_NONE) {
                    mReadList.notify();
                }
            }
            mCallback.onStateChanged(state, addr);
        }
        public void onWrite(byte[] buf, int len, int err) {
            mCallback.onWrite(buf, len, err);
        }
        public void onRead(byte[] buf, int err) {
            synchronized(mReadList) {
                mReadList.add(new CellData(buf, 0, err));
                mReadList.notify();
            }
            mCallback.onRead(buf, err);
        }
    };
    
    public BluetoothCompat(String tag, boolean isServer, OnChannelListener listener) {
        mCallback = listener;
        if (isServer) {
            mSppMgr = SppServerManager.getDefault(mListener);
        } else {
            mSppMgr = SppClientManager.getDefault(mListener);
        }
        TAG = tag;
        mConnState = OnChannelListener.STATE_NONE;
    }

    public static BluetoothCompat getClientCompat(OnChannelListener listener) {
        if (sClientCompat == null) {
            sClientCompat = new BluetoothClientCompat(listener);
        }
        return sClientCompat;
    }

    public static BluetoothCompat getServerCompat(OnChannelListener listener) {
        if (sServerCompat == null) {
            sServerCompat = new BluetoothServerCompat(listener);
        }
        return sServerCompat;
    }

    private class CellData {
        public byte[] buf;
        public int offset;
        public int err;
        public CellData(byte[] data, int offset, int error) {
            buf = data;
            err = error;
            this.offset = offset;
        }
    }

    //////////////////////////////////////////
    public String getAddr() {
        return mDevAddr;
    }

    public void connect(String addr) {
        mSppMgr.connect(addr);
    }

    public void disconnect() {
        mSppMgr.disconnect();
    }

    public void start() {
        mSppMgr.start();
    }

    public void stop() {
        mSppMgr.stop();
    }

    public int write(byte[] data) throws IOException {
        return write(data, 0, data.length);
    }

    public int write(byte[] data, int offset, int len) throws IOException{
        if (DBG) Log.v(TAG, "write data offset: "+offset+" len: "+len);
        int writeLen = mSppMgr.write(data, offset, len);
        if (writeLen == -1) {
            throw new IOException("write error");
        }
        return writeLen;
    }

    public byte read() throws IOException{
        byte[] data = new byte[1];
        read(data, 0, 1);
        return data[0];
    }

    synchronized public int read(byte[] data, int pos, int len) throws IOException{
        if (DBG) Log.v(TAG, "read data pos: "+pos+" len: "+len);

        synchronized (mReadList) {
            if (mCellData == null) {
                if (mReadList.size() > 0) {
                    mCellData = mReadList.remove(0);
                } else {
                    if (mConnState != OnChannelListener.STATE_CONNECTED) {
                            throw new IOException("There is no more data and conn was closed");
                    }
                    try {
                        mReadList.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "mReadList.wait error: "+e);
                        throw new IOException("InterruptedException wait error");
                    }
                    if (mReadList.size() > 0) {
                        mCellData = mReadList.remove(0);
                    } else {
                        throw new IOException("There is no more data 1");
                    }
                }
            }

            if (mCellData == null) {
                throw new IOException("There is no more data 2");
            }

            if (mCellData.err != OnChannelListener.ERROR_NONE) {
                Log.w(TAG, "read error: there is no valid data");
                throw new IOException("read error");
            }
            int validLen = mCellData.buf.length-mCellData.offset;
            if (validLen < len) {
                Log.e(TAG, "read error req len: "
                                +len+" but data len: "+mCellData.buf.length+" offset: "+mCellData.offset);
                throw new RuntimeException("There is no more data len: "+len);
            }
            System.arraycopy(mCellData.buf, mCellData.offset, data, pos, len);
            mCellData.offset += len;
            if (mCellData.offset == mCellData.buf.length) {
                mCellData = null;
            }
            if (DBG) Log.v(TAG, "finish read data");
            return len;
        }
    }


}



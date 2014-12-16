package com.ingenic.spp;

public interface OnChannelListener {
    
    public static final int ERROR_NONE       = 0;
    public static final int ERROR_WRITE      = 1;
    public static final int ERROR_READ       = 2;

    public static final int STATE_NONE       = 0;
    public static final int STATE_LISTEN     = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED  = 3; 

    void onStateChanged(int state, String addr);
    void onWrite(byte[] buf, int len, int err);
    void onRead(byte[] buf, int err);
}



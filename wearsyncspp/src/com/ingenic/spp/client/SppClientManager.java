package com.ingenic.spp;

public class SppClientManager extends SppManager{
    // Debugging
    private static final String TAG = "SppClientManager";
    private static final boolean D = true;
    private static SppClientManager sMgr;

    private SppClientManager(OnChannelListener listener) {
        super(TAG, listener, false);
    }

    public static SppClientManager getDefault(OnChannelListener listener) {
        if (sMgr == null) {
            sMgr = new SppClientManager(listener);
        }
        return sMgr;
    }

    @Override
    public void connect(String addr) {
        super.connect(addr);
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    @Override
    public int write(byte[] data, int offset, int len) {
        return super.write(data, offset, len);
    }

}



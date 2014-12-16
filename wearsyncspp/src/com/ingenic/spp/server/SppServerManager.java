package com.ingenic.spp;

import java.util.UUID;

public class SppServerManager extends SppManager {
    // Debugging
    private static final String TAG = "SppServerManager";
    private static final boolean D = true;
    private static SppServerManager sMgr;

    private SppServerManager(OnChannelListener listener) {
        super(TAG, listener, true);
    }

    public static  SppServerManager getDefault(OnChannelListener listener) {
        if (sMgr == null) {
            sMgr = new SppServerManager(listener);
        }
        return sMgr;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void start(String name, UUID uuid, boolean secure) {
        super.start(name, uuid, secure);
    }

    @Override
    public void stop() {
        super.stop();
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



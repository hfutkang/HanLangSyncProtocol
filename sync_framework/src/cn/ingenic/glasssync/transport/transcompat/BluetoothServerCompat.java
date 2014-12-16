package cn.ingenic.glasssync.transport.transcompat;

import com.ingenic.spp.SppClientManager;
import com.ingenic.spp.OnChannelListener;
import java.io.IOException;

class BluetoothServerCompat extends BluetoothCompat {
    private static final String TAG = "BluetoothServerCompat";
    private static final boolean DBG = true;

    public BluetoothServerCompat(OnChannelListener listener) {
        super(TAG, true, listener);
    }

    //////////////////////////////////////////
    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    @Override
    public int write(byte[] data) throws IOException {
        return super.write(data, 0, data.length);
    }

    public int write(byte[] data, int offset, int len) throws IOException {
        return super.write(data, offset, len);
    }

    public int read(byte[] data, int pos, int len) throws IOException {
        return super.read(data, pos, len);
    }

}



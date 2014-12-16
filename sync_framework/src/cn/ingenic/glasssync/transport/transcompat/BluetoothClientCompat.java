package cn.ingenic.glasssync.transport.transcompat;

import com.ingenic.spp.SppClientManager;
import com.ingenic.spp.OnChannelListener;
import java.io.IOException;

class BluetoothClientCompat extends BluetoothCompat {
    private static final String TAG = "BluetoothClientCompat";
    private static final boolean DBG = true;

    public BluetoothClientCompat(OnChannelListener listener) {
        super(TAG, false, listener);
    }
    
    //////////////////////////////////////////
    public void connect(String addr) {
        super.connect(addr);
    }

    public void disconnect() {
        super.disconnect();
    }

    @Override
    public int write(byte[] data) throws IOException {
        return super.write(data);
    }

    @Override
    public int write(byte[] data, int offset, int len) throws IOException{
        return super.write(data, offset, len);
    }

    @Override
    public int read(byte[] data, int pos, int len) throws IOException {
        return super.read(data, pos, len);
    }

}



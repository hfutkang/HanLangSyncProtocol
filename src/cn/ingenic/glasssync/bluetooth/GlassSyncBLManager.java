package cn.ingenic.glasssync.blmanager;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.bluetooth.BluetoothDevice;

public class GlassSyncBLManager {
    private static final String TAG = "GlassSyncBLManager";

    private Context mContext;
    private static GlassSyncBLManager sInstance;
    private BLManagerReceiver nReceiver;

    private GlassSyncBLManager(Context context){
	Log.e(TAG, "GlassSyncBLManager");
	mContext = context;

	init_receiver(context);
    }

    public static GlassSyncBLManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncBLManager(c);
	return sInstance;
    }

    private void init_receiver(Context c){
        nReceiver = new BLManagerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        c.registerReceiver(nReceiver,filter);
    }

    class BLManagerReceiver extends BroadcastReceiver{
	private String TAG = "BLManagerReceiver";
	
        @Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){
		Log.e(TAG, "rcv msg BluetoothDevice.ACTION_PAIRING_REQUEST");
		int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
					      BluetoothDevice.ERROR);
		if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION){
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    Log.e(TAG, "setPairingConfirmation true");
		    device.setPairingConfirmation(true);
		}
	    }
        }
    }

}
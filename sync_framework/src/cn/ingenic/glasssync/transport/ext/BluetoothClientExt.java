package cn.ingenic.glasssync.transport.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.LogTag.Client;
import cn.ingenic.glasssync.transport.BluetoothChannel;
import cn.ingenic.glasssync.transport.transcompat.BluetoothCompat;
import com.ingenic.spp.OnChannelListener;

class BluetoothClientExt implements BluetoothChannelExt {
	private BluetoothSocket mSocket = null;
	private final TransportStateMachineExt mStateMachine;
	private volatile boolean mClosed = true;

	private final Handler mRetrive;
	private final Context mContext;
	private Object mSocketLock = new Object();
    private final BluetoothCompat mClientCompat;
    private final OnChannelListener mConnListener;

    private class ConnListener implements OnChannelListener {
        public void onStateChanged(int state, String addr) {
            if (state == OnChannelListener.STATE_CONNECTED) {
		Client.e("OnChannelListener.STATE_CONNECTED");
                notifyStateChange(TransportStateMachineExt.C_CONNECTED, mStateMachine);
                doStartRead();
                mClosed = false;
            } else if (state == OnChannelListener.STATE_NONE) {
		Client.e("OnChannelListener.STATE_NONE");
            	sendClientCloseMsg();
            }
        }

        public void onWrite(byte[] buf, int len, int err) {
            //Do nothing.
        }

        public void onRead(byte[] buf, int err) {
        }

    }

	BluetoothClientExt(TransportStateMachineExt stateMachine,
			final Handler retrive,Context c) {
		mStateMachine = stateMachine;
		mRetrive = retrive;
		mContext = c;
        mConnListener = new ConnListener();
        mClientCompat = BluetoothCompat.getClientCompat(mConnListener);
	}
	
	static void notifyStateChange(int state,
			TransportStateMachineExt stateMachine) {
		Message msg = stateMachine
				.obtainMessage(TransportStateMachineExt.MSG_STATE_CHANGE);
		msg.arg1 = state;
		msg.sendToTarget();
	}

	private void sendClientCloseMsg() {
		if (!mClosed) {
			notifyStateChange(
					TransportStateMachineExt.C_IDLE, mStateMachine);
		}
	}

       /* private final boolean is_ping(byte[] b){
	    if (b[0] == 'p' && b[1] == 'i' && b[2] == 'n' && b[3] == 'g'
		&& b[4] == 'g' && b[5] == 'n' && b[6] == 'i' && b[7] == 'p'){
		//Client.e("is_ping");
		return true;
	    }

	    return false;
	}*/

	void start(final String address) {
		if (!mClosed) {
			Client.e("Client already running.");
			return;
		}
        mClientCompat.connect(address);
    }
    private void doStartRead() {
		
		Thread thread = new Thread() {

			@Override
			public void run() {
			  /*  Client.e("client run in");
				mClosed = false;
				Client.e("client----mClosed"+mClosed);
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				Client.d("client thread running, connect the address:" + address);
				BluetoothDevice device = adapter.getRemoteDevice(address);
				Client.d("client thread running, connect the device:" + device);
				Enviroment env = Enviroment.getDefault();
				
				InputStream input = null;
				int i = 0;
				while (true) {
					try {
						mTmp = device.createRfcommSocketToServiceRecord(env
								.getUUID(BluetoothChannel.CUSTOM, true));
						Client.e("getUUID:" + env
								.getUUID(BluetoothChannel.CUSTOM, true)+System.currentTimeMillis());
						mSocket=mTmp;
						Client.e("connect start");
						mSocket.connect();
						Client.e("connect end");
						mOutput = mSocket.getOutputStream();
						input = mSocket.getInputStream();
						Client.e("connect input");
						notifyStateChange(
										TransportStateMachineExt.C_CONNECTED,
										mStateMachine);
						break;
					} catch (IOException e) {
						if (i >= 3) {
							Client.e("Client connect failed finally:" + e.getMessage());
							close();
							notifyStateChange(
									TransportStateMachineExt.C_IDLE, mStateMachine);
							return;
						}
						Client.e("Client connect failed " + (i + 1) + " times:" + e.getMessage());
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							Client.e("Sleep 1s error:" + e1.getMessage());
						}
					}
					i++;
				}*/
				try {
					while (!mClosed) {
						Pkg pkg = BluetoothChannelExtTools.retrivePkg(mClientCompat, mRetrive);

						if(pkg == null)
							continue;
						  /*  }else if (pkg.getData().length > Pkg.BIG_LEN){
							Client.e("set mLastRetriveSec length:%d" + pkg.getData().length);
							mLastRetriveSec = System.currentTimeMillis() / 1000l;
						    }
						}*/
						Message msg = mRetrive.obtainMessage();
						msg.obj = pkg;
						msg.sendToTarget();
					}
				} catch (Exception e) {
					Client.e("client exception:" + e.getMessage());
					//sendClientCloseMsg();
				}
				Client.d("read thread quit.");
			}

		};
		thread.start();
/*
		Thread thread1 = new Thread() {
			@Override
			public void run() {
			    try {
				Thread.sleep(2000);
			    } catch (InterruptedException e) {
			    }

			    while (!mClosed){
				if (System.currentTimeMillis() / 1000l - 10 < mLastRetriveSec){
				    byte[] p = new byte[8];
				    p[0]='p';p[1]='i';p[2]='n';p[3]='g';
				    p[4]='g';p[5]='n';p[6]='i';p[7]='p';
				    Pkg pkg = new Pkg(p);
				    try{
					Client.d("send ping");
					send(pkg);
				    }catch (ProtocolException e){
				    }
				}
				
				try {
				    Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			    }
			    Client.d("ping thread1 quit.");
			}
		};
		thread1.start();
*/
	}

	@Override
	public void send(Pkg pkg) throws ProtocolException {
		try {
			BluetoothChannelExtTools.send(pkg, mClientCompat);
		} catch (Exception e) {
			Client.e("send error:" + e.getMessage());
			mRetrive.removeMessages(BluetoothChannelExtTools.MsgSendAnyData);
			
		}

	}

	public void close() {
		if (!mClosed) {
			mClientCompat.disconnect();
            try {
				synchronized (mSocketLock) {
		Client.e("--close");
					mClosed = true;
					if (mSocket != null) {
						mSocket.close();
					}
					mSocket = null;
				}
			} catch (IOException e) {
				Client.e("client close error!");
				LogTag.printExp(LogTag.CLIENT, e);
			}
		}
	}
	
	
}

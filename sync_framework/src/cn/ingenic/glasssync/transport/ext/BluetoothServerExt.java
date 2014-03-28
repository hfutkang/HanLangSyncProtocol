package cn.ingenic.glasssync.transport.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.LogTag.Server;
import cn.ingenic.glasssync.transport.BluetoothChannel;

class BluetoothServerExt implements BluetoothChannelExt {
	private BluetoothSocket mClient;
	private Object mClientLock = new Object();
	protected boolean mClosed = true;
	private BluetoothServerSocket mServerSocket;
	private OutputStream mOutput;
	private final TransportStateMachineExt mStateMachine;
	private final Handler mRetrive;

	public BluetoothServerExt(TransportStateMachineExt stateMachine,
			final Handler retrive) {
		mStateMachine = stateMachine;
		mRetrive = retrive;
	}

	void start() {
		if (!mClosed) {
			Server.e("Start BluetoothServerExt with not closing st.");
			close();
		}
		
		Thread thread = new Thread() {
			@Override
			public void run() {
				Server.i("server started.");
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				Enviroment env = Enviroment.getDefault();
				try {
					mServerSocket = adapter.listenUsingRfcommWithServiceRecord(
							"BluetoothServerExt",
							env.getUUID(BluetoothChannel.CUSTOM, false));
					Server.i("listen over, accepting...");
					synchronized (mClientLock) {
						if (mServerSocket != null) {
							mClient = mServerSocket.accept();
						} else {
							Server.d("Server is closed.");
							return;
						}
						InputStream input = mClient.getInputStream();
						mOutput = mClient.getOutputStream();
						Server.d("accept one");
						mClosed = false;
						
						BluetoothClientExt
						.notifyStateChange(
								TransportStateMachineExt.S_CONNECTED,
								mStateMachine);
						
						mServerSocket.close();
						
						while (!mClosed) {
							Pkg pkg = BluetoothChannelExtTools.retrivePkg(input);
							if (pkg instanceof Neg) {
								Neg neg = (Neg) pkg;
								neg.setAddr(mClient.getRemoteDevice().getAddress());
							}
							
							Message msg = mRetrive.obtainMessage();
							msg.obj = pkg;
							msg.sendToTarget();
						}
					}
					Server.d("current client quit.");
				} catch (IOException e) {
					Server.e("Exception occurs:" + e.getMessage());
					LogTag.printExp(LogTag.SERVER, e);
					sendClientCloseMsg();
				} catch (ProtocolException ep) {
					Server.e("protocol exception:" + ep.getMessage());
				}
				Server.i("server end.");
			}
		};
		thread.start();
	}

	public void send(Pkg pkg) throws ProtocolException {
		try {
			BluetoothChannelExtTools.send(pkg, mOutput);
		} catch (IOException e) {
			Server.e("send error:" + e.getMessage());
			sendClientCloseMsg();
		}
	}

	protected void sendClientCloseMsg() {
		if (!mClosed) {
			close();
			BluetoothClientExt.notifyStateChange(
					TransportStateMachineExt.S_IDLE, mStateMachine);
		}
	}

	public void close() {
		if (mClosed) {
			return;
		}

		Server.d("shutdown the server");
		mClosed = true;

		synchronized (mClientLock) {
			if (mClient != null) {
				try {
					mClient.close();
					mClient = null;
					mOutput = null;
				} catch (IOException e) {
					Server.d("client close error:" + e.getMessage());
					LogTag.printExp(LogTag.CLIENT, e);
				}
			}
		}

		try {
			mServerSocket.close();
			mServerSocket = null;
		} catch (IOException e) {
			Server.d("server close error:" + e.getMessage());
			LogTag.printExp(LogTag.CLIENT, e);
		}

	}
}
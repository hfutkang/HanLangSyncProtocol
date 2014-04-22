package cn.ingenic.glasssync.transport.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.LogTag.Client;
import cn.ingenic.glasssync.transport.BluetoothChannel;

class BluetoothClientExt implements BluetoothChannelExt {
	private BluetoothSocket mSocket;
	private final TransportStateMachineExt mStateMachine;
	private volatile boolean mClosed = true;

	private OutputStream mOutput;
	private final Handler mRetrive;

	BluetoothClientExt(TransportStateMachineExt stateMachine,
			final Handler retrive) {
		mStateMachine = stateMachine;
		mRetrive = retrive;
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
			close();
			notifyStateChange(
					TransportStateMachineExt.C_IDLE, mStateMachine);
		}
	}

	void start(final String address) {
		if (!mClosed) {
			Client.e("Client already running.");
			//close();
			return;
		}
		
		Thread thread = new Thread() {

			@Override
			public void run() {
				mClosed = false;
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				BluetoothDevice device = adapter.getRemoteDevice(address);
				Client.d("client thread running, connect the device:" + device);
				Enviroment env = Enviroment.getDefault();
				
				InputStream input = null;
				int i = 0;
				while (true) {
					try {
						mSocket = device.createRfcommSocketToServiceRecord(env
								.getUUID(BluetoothChannel.CUSTOM, true));
						mSocket.connect();
						mOutput = mSocket.getOutputStream();
						input = mSocket.getInputStream();
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
				}
				try {
					while (!mClosed) {
						Pkg pkg = BluetoothChannelExtTools.retrivePkg(input);

						Message msg = mRetrive.obtainMessage();
						msg.obj = pkg;
						msg.sendToTarget();
					}
				} catch (Exception e) {
					Client.e("client exception:" + e.getMessage());
					sendClientCloseMsg();
				}
				Client.d("read thread quit.");
			}

		};
		thread.start();
	}

	@Override
	public void send(Pkg pkg) throws ProtocolException {
		//BluetoothChannelExtTools.send(pkg, mOutput);
		try {
			BluetoothChannelExtTools.send(pkg, mOutput);
		} catch (IOException e) {
			Client.e("send error:" + e.getMessage());
			sendClientCloseMsg();
		}

	}

	public void close() {
		if (!mClosed) {
			try {
				mClosed = true;
				if (mSocket != null) {
					mSocket.close();
				}
				mSocket = null;
				mOutput = null;
			} catch (IOException e) {
				Client.e("client close error!");
				LogTag.printExp(LogTag.CLIENT, e);
			}
		}
	}
	
	
}
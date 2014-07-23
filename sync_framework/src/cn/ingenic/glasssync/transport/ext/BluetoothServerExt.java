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
	private Object mClientCloseLock=new Object();
	private volatile boolean mClosed = true;
	private BluetoothServerSocket mServerSocket;
	private OutputStream mOutput;
	private final TransportStateMachineExt mStateMachine;
	private final Handler mRetrive;
        private long mLastRetriveSec;
	private Object mSendLock = new Object();

	public BluetoothServerExt(TransportStateMachineExt stateMachine,
			final Handler retrive) {
		mStateMachine = stateMachine;
		mRetrive = retrive;
	}

        private final boolean is_ping(byte[] b){
	    if (b[0] == 'p' && b[1] == 'i' && b[2] == 'n' && b[3] == 'g'
		&& b[4] == 'g' && b[5] == 'n' && b[6] == 'i' && b[7] == 'p'){
		Server.i("is_ping");
		return true;
	    }

	    return false;
	}

	void start() {
		if (!mClosed) {
			Server.d("Server already running.");
//			close();
			return;
		}
		
		Thread thread = new Thread() {
			@Override
			public void run() {
				Server.i("server started.");
				mClosed = false;
				mLastRetriveSec = 0;
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				Enviroment env = Enviroment.getDefault();
				try {
					mServerSocket = adapter.listenUsingRfcommWithServiceRecord(
							"BluetoothServerExt",
							env.getUUID(BluetoothChannel.CUSTOM, false));
					Server.i("listen over, accepting..."+env.getUUID(BluetoothChannel.CUSTOM, false)+System.currentTimeMillis());
					synchronized (mClientLock) {
						Server.i("listen over, accepting..."+mServerSocket);
						if (mServerSocket != null) {
							mClient = mServerSocket.accept();
							Server.i("listen over--mServerSocket.accept()");
							Server.i("listen over, ...mClient"+mClient);
						} else {
							Server.d("Server is closed.");
							return;
						}
						
						InputStream input = mClient.getInputStream();
						Server.i("listen over-------input"+input);
						mOutput = mClient.getOutputStream();
						Server.i("listen over----mOutput"+mOutput);
						Server.i("accept one");
						
						BluetoothClientExt
						.notifyStateChange(
								TransportStateMachineExt.S_CONNECTED,
								mStateMachine);
						
						mServerSocket.close();

						while (!mClosed) {
							Pkg pkg = BluetoothChannelExtTools.retrivePkg(input);
						    
							if (pkg.getType() == Pkg.PKG){
							    byte[] b = pkg.getData();
							    if (is_ping(b)){
								//Server.i("retrive length:" + b.length);
								continue;
							    }else if (pkg.getData().length > Pkg.BIG_LEN){
								Server.e("set mLastRetriveSec length:%d" + pkg.getData().length);
								mLastRetriveSec = System.currentTimeMillis() / 1000l;
							    }
							}
					
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
					Server.d("send ping");
					send(pkg);
				    }catch (ProtocolException e){
				    }
				}
				
				try {
				    Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			    }
			    Server.d("ping thread1 quit.");
			}
		};
		thread1.start();
	}

	public void send(Pkg pkg) throws ProtocolException {
		try {
		    synchronized (mSendLock){
		    	if(!mClosed){
		    		BluetoothChannelExtTools.send(pkg, mOutput);
		    	}		
		    }
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

		/*
		 * if use mClientLock, then this happens:
		 *    mServerSocket.accept(); is block always, then mClientLock is hold always.
		 *    then close() is never return.  Many error will occur.
		 * */
		synchronized (mClientCloseLock) {
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

		if (mServerSocket != null) {
			try {
				mServerSocket.close();
				mServerSocket = null;
			} catch (IOException e) {
				Server.d("server close error:" + e.getMessage());
				LogTag.printExp(LogTag.CLIENT, e);
			}
		}

	}
}
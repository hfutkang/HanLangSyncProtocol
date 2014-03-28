package cn.ingenic.glasssync.transport.ext;

import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.SyncSerializable;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.services.SyncSerializableTools;
import cn.ingenic.glasssync.transport.TransportManager;

public class TransportManagerExt extends TransportManager {
	static final int PRO_VER = 1;

	public static final int CMD = Pkg.CHANNEL_CMD;
	public static final int SPEICAL = Pkg.CHANNEL_SPEICAL;
	public static final int DATA = Pkg.CHANNEL_DATA;
	public static final int FILE = Pkg.CHANNEL_FILE;
	public static final int CHANNEL_MIN = CMD;

	private TransportStateMachineExt mStateMachine;
	private Handler mRetriveHandler;
	private PkgEncodingWorkspace mPkgEncode;
	private PkgDecodingWorkspace mPkgDecode;
	private Thread mWorkThread;
	private OnRetriveCallback mCallback;

	public interface OnRetriveCallback {
		void onRetrive(SyncSerializable serial);
	}

	public TransportManagerExt(Context context, String sysetmModuleName,
			Handler mgrHandler) {
		super(context, sysetmModuleName, mgrHandler);

		Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				mCallback.onRetrive((SyncSerializable) msg.obj);
			}

		};
		mPkgEncode = new PkgEncodingWorkspace(DefaultSyncManager.SUCCESS,
				DefaultSyncManager.NO_CONNECTIVITY, DefaultSyncManager.UNKNOW);
		mPkgDecode = new PkgDecodingWorkspace(context, handler);
		ConnectionTimeoutManager.init(
				context,
				mPkgEncode,
				mPkgDecode,
				mgrHandler.obtainMessage(
						DefaultSyncManager.MSG_TIME_OUT),
				DefaultSyncManager.TIMEOUT);
	}

	public void send(SyncSerializable serializable) {
		mPkgEncode.push(serializable);
//		if (!mIsRunning) {
//			if (mWorkThread == null) {
//				d("restart the work thread.");
//				mWorkThread = new Thread(mSendWork);
//				mWorkThread.start();
//			} else {
//				v("waiting the work thread working.");
//			}
//		}
	}

	public void setRetriveCallback(OnRetriveCallback callback) {
		mCallback = callback;
	}

	private Runnable mSendWork = new Runnable() {

		@Override
		public void run() {
			d("SendWork thread start working...");
			try {
				while (true) {
					Pkg pkg = mPkgEncode.poll();
					if (pkg == null) {
						d("No pkg waiting to ben sent, quit sending thread");
						return;
					}

					mStateMachine.sendRequest(pkg);
				}
			} catch (Exception e) {
				e("Exception occurs, quit send thread.", e);
				mPkgEncode.clear();
			}
		}

	};

	@Override
	protected void init() {
		HandlerThread ht = new HandlerThread("retrive");
		ht.start();

		mRetriveHandler = new Handler(ht.getLooper()) {

			@Override
			public void handleMessage(Message msg) {
				Pkg pkg = (Pkg) msg.obj;
				int type = pkg.getType();
				if (type == Pkg.PKG || type == Pkg.CFG) {
					try {
						mPkgDecode.push(pkg);
					} catch (Exception e) {
						e("", e);
					}
				} else if (type == Pkg.NEG) {
					Neg neg = (Neg) pkg;
					int reason = 0;
					if (neg.isACK1()) {
						DefaultSyncManager mgr = DefaultSyncManager.getDefault();
						String bondingAddr = mgr.getLockedAddress();
						String remoteAddr = neg.getAddr();
						boolean isInit = neg.isInit();
						boolean hasBondAddr = mgr.hasLockedAddress();
						if (!BluetoothAdapter.checkBluetoothAddress(remoteAddr)) {
							e("Can not get BT addr from remote requesting dev:" + remoteAddr);
							reason = Neg.FAIL_ADDRESS_MISMATCH;
						}
						
						boolean reBond = false;
						if (reason == 0) {
							if (isInit) {
								if (hasBondAddr) {
									if (bondingAddr.equalsIgnoreCase(remoteAddr)) {
										reBond = true;
									} else {
										w("Address mismatch with Initilization.");
										reason = Neg.FAIL_ADDRESS_MISMATCH;
									}
								}
							} else {
								if (hasBondAddr) {
									if (!bondingAddr.equalsIgnoreCase(remoteAddr)) {
										w("Address mismatch without Initilization.");
										reason = Neg.FAIL_ADDRESS_MISMATCH;
									}
								} else {
									w("This device had unbond remote device.");
									reason = Neg.FAIL_ADDRESS_MISMATCH;
								}
							}
						}

						if (reason == 0 && !(PRO_VER == neg.getVersion())) {
							w("protocol version mismatch");
							reason = Neg.FAIL_VERSION_MISMATCH;
						}

						mStateMachine.sendMessage(
								TransportStateMachineExt.MSG_S_CONTINUE,
								Neg.fromResponse(reason == 0, reason));
						
						if (reason == 0) {
							Message m = mMgrHandler.obtainMessage(DefaultSyncManager.MSG_SET_LOCKED_ADDRESS);
							m.arg1 = reBond ? 1 : 0;
							m.obj = remoteAddr;
							m.sendToTarget();
						}
					} else if (neg.isACK2()) {
						if (neg.isPass()) {
							mStateMachine
									.sendMessage(TransportStateMachineExt.MSG_C_CONTINUE);
						} else {
							mStateMachine
									.sendMessage(TransportStateMachineExt.MSG_DISCONNECT);
							int r = neg.getReason();
							i("Fail reason:" + r);
							int res;
							if (r == Neg.FAIL_ADDRESS_MISMATCH) {
								res = R.string.fail_addr_mismatch;
								mMgrHandler.sendEmptyMessage(DefaultSyncManager.MSG_CLEAR_ADDRESS);
							} else if (r == Neg.FAIL_VERSION_MISMATCH) {
								res = R.string.fail_ver_mismatch;
							} else {
								res = R.string.fail_unknow;
							}
							Toast.makeText(mContext, res, Toast.LENGTH_SHORT).show();
						}
					} else {
						e("wrong neg.");
					}
				} else {
					e("unknow Pkg TYPE:" + type);
					return;
				}
			}

		};
		mStateMachine = new TransportStateMachineExt(mContext, this,
				mRetriveHandler);
		mStateMachine.start();
	}

	@Override
	public void prepare(String address) {
		if (BluetoothAdapter.checkBluetoothAddress(address)) {
			Message msg = mStateMachine.obtainMessage(TransportStateMachineExt.MSG_CONNECT);
			msg.obj = address;
			msg.sendToTarget();
		} else {
			Message msg = mStateMachine.obtainMessage(TransportStateMachineExt.MSG_DISCONNECT);
			msg.sendToTarget();
		}
	}

	@Override
	public void notifyMgrState(boolean success, int arg2) {
		if (success) {
			mPkgEncode.start();
			if (mWorkThread == null) {
				mWorkThread = new Thread(mSendWork, "PollingThread");
				mWorkThread.start();
			} else {
				e("WorkThread should be null while notifying ConnectSuccessState.");
			}
		} else {
			mWorkThread = null;
			mPkgEncode.clear();
			mPkgDecode.clear();
			releaseWakeLock();
		}
		
		int state = success ? DefaultSyncManager.CONNECTED : DefaultSyncManager.IDLE;
		Message msg = mMgrHandler.obtainMessage(DefaultSyncManager.MSG_STATE_CHANGE);
		msg.arg1 = state;
		msg.arg2 = arg2;
		msg.sendToTarget();
	}

	@Override
	public void sendBondResponse(boolean pass) {
		w("unimplemention sendBondResponse");
	}

	@Override
	public void request(ProjoList projoList) {
		send(SyncSerializableTools.projoList2Serial(projoList));
	}

	@Override
	public void requestSync(ProjoList projoList) {
		request(projoList);
	}

	@Override
	public void requestUUID(UUID uuid, ProjoList projoList) {
		SyncSerializable serial = SyncSerializableTools
				.projoList2Serial(projoList);
		serial.getDescriptor().mUUID = uuid;
		serial.getDescriptor().mPri = SPEICAL;
		send(serial);
	}

	@Override
	public void sendFile(String module, String name, int length, InputStream in) {
		e("sendFile unimplement.");
	}

	@Override
	public void retriveFile(String module, String name, int length,
			String address) {
		e("retriveFile unimplement.");
	}

	@Override
	public void createChannel(UUID uuid, OnChannelCallBack callback) {
		e("createChannel unimplement.");
	}

	@Override
	public boolean listenChannel(UUID uuid, OnChannelCallBack callback) {
		e("listenChannel unimplement.");
		return true;
	}

	@Override
	public void destoryChannel(UUID uuid) {
		e("destoryChannel unimplement.");
	}

	@Override
	public void closeFileChannel() {
		e("closeFileChannel unimplement.");
	}

	private static final String TAG = "<TRAN>";

	private static void i(String msg) {
		Log.i(LogTag.APP, TAG + msg);
	}

	private static void d(String msg) {
		Log.d(LogTag.APP, TAG + msg);
	}
//	
//	private static void v(String msg) {
//		Log.v(LogTag.APP, TAG + msg);
//	}

	private static void w(String msg) {
		Log.w(LogTag.APP, TAG + msg);
	}

	private static void e(String msg) {
		Log.e(LogTag.APP, TAG + msg);
	}

	private static void e(String msg, Throwable t) {
		Log.e(LogTag.APP, msg, t);
	}

}

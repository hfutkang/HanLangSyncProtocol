package cn.ingenic.glasssync.transport.ext;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import cn.ingenic.glasssync.Enviroment;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.transport.TransportManager;
import cn.ingenic.glasssync.utils.internal.State;
import cn.ingenic.glasssync.utils.internal.StateMachine;

class TransportStateMachineExt extends StateMachine {

	private static final int MSG_BASE = 0;
	static final int MSG_CONNECT = MSG_BASE + 1;
	static final int MSG_DISCONNECT = MSG_BASE + 2;
	static final int MSG_STATE_CHANGE = MSG_BASE + 3;
	static final int MSG_C_CONTINUE = MSG_BASE + 4;
	static final int MSG_S_CONTINUE = MSG_BASE + 5;

	// private static final int MSG_SEND = MSG_BASE + 5;
	private static final int MSG_BT_ON = MSG_BASE + 6;
	private static final int MSG_BT_OFF = MSG_BASE + 7;

	private static final int STATE_BASE = 0;
	static final int C_IDLE = STATE_BASE + 1;
	static final int C_CONNECTED = STATE_BASE + 2;
	static final int S_IDLE = STATE_BASE + 3;
	static final int S_CONNECTED = STATE_BASE + 4;

	private State mChannelDefaultState;

	static String convert(Message msg) {
		switch (msg.what) {
		case MSG_CONNECT:
			return "MSG_CONNECT";
		case MSG_DISCONNECT:
			return "MSG_DISCONNECT";
		case MSG_STATE_CHANGE:
			return "MSG_STATE_CHANGE:" + getState(msg.arg1);
			/*
			 * case MSG_C_RETRIVE: return "MSG_C_RETRIVE"; case MSG_S_RETRIVE:
			 * return "MSG_S_RETRIVE";
			 */
		case MSG_BT_ON:
			return "MSG_BT_ON";
		case MSG_BT_OFF:
			return "MSG_BT_OFF";
			/*
			 * case MSG_SEND_BOND_RESP: return "MSG_SEND_BOND_RESP";
			 */
		case MSG_C_CONTINUE:
			return "MSG_C_CONTINUE";
		case MSG_S_CONTINUE:
			return "MSG_S_CONTINUE";

		default:
			return "UNKNOW:" + msg.what;
		}
	}

	static String getState(int state) {
		switch (state) {
		case C_IDLE:
			return "C_IDLE";
		case C_CONNECTED:
			return "C_CONNECTED";
		case S_IDLE:
			return "S_IDLE";
		case S_CONNECTED:
			return "S_CONNECTED";

		default:
			return "unknow state:" + state;
		}
	}

	// private final Context mContext;
	private BluetoothClientExt mClient;
	private BluetoothServerExt mServer;
	private final TransportManager mTransportManager;

	// private final Handler mRetrive;

	TransportStateMachineExt(Context context,
			TransportManager transportManager, Handler retrive) {
		super("TransprotStateMachine");
		// mContext = context;
		mTransportManager = transportManager;
		// mRetrive = retrive;

		addState(mDefaultState);
		addState(mClientState, mDefaultState);
		addState(mClientIdleState, mClientState);
		addState(mClientReqState, mClientState);
		addState(mClientConnectedState, mClientState);
		addState(mServerState, mDefaultState);
		addState(mServerIdleState, mServerState);
		addState(mServerRespState, mServerState);
		addState(mServerConnectedState, mServerState);

		

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(mBluetoothReceiver, filter);
		
		if (Enviroment.getDefault().isWatch()) {
			mChannelDefaultState = mServerState;
			mServer = new BluetoothServerExt(this, retrive);
		} else {
			mChannelDefaultState = mClientState;
			mClient = new BluetoothClientExt(this, retrive, context);

		}

		setInitialState(mChannelDefaultState);

		
		
	}

	boolean sendRequest(Pkg pkg) throws ProtocolException {
		BluetoothChannelExt channel = getAvaliableChannel();
		if (channel != null) {
			channel.send(pkg);
			return true;
		} else {
			w("no avaliable channel found for sendRequest.");
			return false;
		}
	}

	private BluetoothChannelExt getAvaliableChannel() {
	    Log.e("SDF", "mIsClient:" + mIsClient);
		return mIsClient ? mClient : mServer;
	}

	private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR);
				i("bluetooth state:"
						+ ((state == BluetoothAdapter.STATE_ON) ? "on"
								: (state == BluetoothAdapter.STATE_OFF) ? "off"
										: state));
				if (BluetoothAdapter.STATE_ON == state) {
					sendMessage(MSG_BT_ON);
				} else if (BluetoothAdapter.STATE_OFF == state) {
					sendMessage(MSG_BT_OFF);
				}
			}
		}

	};

	private final DefaultState mDefaultState = new DefaultState();

	private class DefaultState extends State {

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_CONNECT:
				w("no one process MSG_CONNECT, ingore it and notifyMgrState(false)");
				mTransportManager.notifyMgrState(false);
				return HANDLED;
			default:
				return NOT_HANDLED;
			}
		}

	}

	private volatile boolean mIsClient = false;

	private final ClientState mClientState = new ClientState();

	private class ClientState extends State {

		private int mmIdleReason = DefaultSyncManager.NON_REASON;
		
		private boolean mmIngoreIdleTransition = false;

		void ingoreIdleTransition() {
			mmIngoreIdleTransition = true;
		}


		@Override
		public void enter() {
			enterLog(this);
			mIsClient = true;
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (mmIngoreIdleTransition) {
				mmIngoreIdleTransition = false;
				return;
			}
			if (BluetoothAdapter.STATE_ON == adapter.getState()) {
//				mClientIdleState.setConnectingAddress(DefaultSyncManager.getDefault().getLockedAddress());
				transitionTo(mClientIdleState);
			}

		}

		@Override
		public void exit() {
			exitLog(this);
//			mTransportManager.notifyMgrState(false, mmIdleReason);
//			mIsClient = false;
//			mClient.close();
//			mmIdleReason = DefaultSyncManager.NON_REASON;
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_CONNECT:
				//mClientIdleState.setConnectingAddress((String)msg.obj);
				//transitionTo(mClientIdleState);
//				DefaultSyncManager.getDefault().forceDisconnect();
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
//					mmIdleReason = msg.arg2;
//					transitionTo(mClientState);
					return HANDLED;
				case S_CONNECTED:
					if (DefaultSyncManager.isWatch()) {
						//yangliu ???
//						mClient.close();
//						mServerState.ingoreIdleTransition();
//						transitionTo(mServerRespState);
					} else {
						//mServer.close();
					}
					return HANDLED;
					// case S_IDLE:
					// mServerChannelManager.prepare(ServerBTChannelManager.SETUP);
					// return HANDLED;
					
				}
			case MSG_BT_ON:
				transitionTo(mClientIdleState);
				sendMessage(MSG_CONNECT);
				return HANDLED;
			case MSG_BT_OFF:
				d("in ClientState receiver BT_OFF do nothing !");
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ServerState mServerState = new ServerState();

	private class ServerState extends State {

		private boolean mmIngoreIdleTransition = false;
		
		void ingoreIdleTransition() {
			mmIngoreIdleTransition = true;
		}
		
		@Override
		public void enter() {
			enterLog(this);
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (mmIngoreIdleTransition) {
				mmIngoreIdleTransition = false;
				return;
			}
			if (BluetoothAdapter.STATE_ON == adapter.getState()) {
				transitionTo(mServerIdleState);
			}
		}

		@Override
		public void exit() {
			exitLog(this);
			// mChannelManager.prepare("");
			// mIsClient = true;
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_BT_ON:
				transitionTo(mServerIdleState);
				return HANDLED;
			case MSG_BT_OFF:
				mServer.close();
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ClientRequestingState mClientReqState = new ClientRequestingState();

	private class ClientRequestingState extends State {

		@Override
		public void enter() {
			enterLog(this);
			try {
				DefaultSyncManager mgr = DefaultSyncManager.getDefault();
				d("Client"+mgr.hasLockedAddress());
				mClient.send(Neg.fromRequest(TransportManagerExt.PRO_VER,
						!mgr.hasLockedAddress()));
			} catch (ProtocolException e) {
				e("Protocol Error:", e);
			}
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_C_CONTINUE:
				transitionTo(mClientConnectedState);
				return HANDLED;
				/*
				 * case MSG_C_RETRIVE: processResponse((ProjoList) msg.obj);
				 * return HANDLED;
				 */

				// case MSG_S_RETRIVE:
				// w("receive a S msg at Client role, this would happed at an extremely low chance "
				// +
				// "(connect each other at the same time), this msg must be address validating msg");
				// mServerRespState.processRequest((ProjoList) msg.obj, false);
				// return HANDLED;

			case MSG_DISCONNECT:
				mClient.close();
				transitionTo(mClientState);
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
				    
					transitionTo(mClientState);
					return HANDLED;
				}
				return NOT_HANDLED;

			case MSG_CONNECT:
				v("more connect req comes, ingore it at ClientRequestingState.");

				return HANDLED;
			case MSG_BT_OFF:
				mClient.close();
				transitionTo(mClientState);
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ClientIdleState mClientIdleState = new ClientIdleState();

	private class ClientIdleState extends State {

		private String mmConnectingAddress;

		void setConnectingAddress(String address) {
			mmConnectingAddress = address;
		}

		@Override
		public void enter() {
			enterLog(this);
//			if (TextUtils.isEmpty(mmConnectingAddress)) {
//				DefaultSyncManager mgr = DefaultSyncManager.getDefault();
//				mmConnectingAddress = mgr.getLockedAddress();
//			}
//
//			v("Connect address:" + mmConnectingAddress);
//			if (!BluetoothAdapter.checkBluetoothAddress(mmConnectingAddress)) {
//				throw new IllegalArgumentException("wrong address to connect:"
//						+ mmConnectingAddress);
//			}
//
//			mClient.start(mmConnectingAddress);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				// case C_IDLE:
				// transitionTo(mServerState);
				// return HANDLED;
				case C_CONNECTED:
					transitionTo(mClientReqState);
					return HANDLED;
				}
				return NOT_HANDLED;

			case MSG_CONNECT:
			mmConnectingAddress=(String)msg.obj;
				if (TextUtils.isEmpty(mmConnectingAddress)) {
					DefaultSyncManager mgr = DefaultSyncManager.getDefault();
					mmConnectingAddress = mgr.getLockedAddress();
				}
				v("Connect address:" + mmConnectingAddress);
                if (mmConnectingAddress.equals("")){
                    v("return address is: null");
                    return HANDLED;
                }
				if (!BluetoothAdapter.checkBluetoothAddress(mmConnectingAddress)) {
					throw new IllegalArgumentException("wrong address to connect:"
							+ mmConnectingAddress);
				}
	
				mClient.start(mmConnectingAddress);
//				v("more connect req comes, ingore it at ClientIdleState.");
				return HANDLED;
			case MSG_DISCONNECT:
				mClient.close();
				d("in ClientState force disconnect !");
				return HANDLED;
			case MSG_BT_OFF:
				mClient.close();
				transitionTo(mClientState);
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}

		}

	}

	private final ClientConnectedState mClientConnectedState = new ClientConnectedState();

	private class ClientConnectedState extends State {

		@Override
		public void enter() {
			enterLog(this);
			mTransportManager.notifyMgrState(true);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			// case MSG_S_RETRIVE:
			// w("receive S msg with Client role.");
			/*
			 * case MSG_C_RETRIVE: mTransportManager.retrive((ProjoList)
			 * msg.obj); return HANDLED;
			 */

			case MSG_CONNECT:
				d("already connected, ingore connect req at ClientConnectedState.");
				return HANDLED;

			case MSG_DISCONNECT:
				mClient.close();
				mTransportManager.notifyMgrState(false, DefaultSyncManager.NON_REASON);
				transitionTo(mClientState);
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
//					mClient.close();
					mTransportManager.notifyMgrState(false, DefaultSyncManager.NON_REASON);
					transitionTo(mClientState);
					
					return HANDLED;
				}

				// case MSG_SEND:
				// sendRequestInternal((ProjoList) msg.obj);
				// return HANDLED;
			case MSG_BT_OFF:
				mClient.close();
				transitionTo(mClientState);
				mTransportManager.notifyMgrState(false, DefaultSyncManager.NON_REASON);
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ServerResponseState mServerRespState = new ServerResponseState();

	private class ServerResponseState extends State {

		@Override
		public void enter() {
			enterLog(this);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			// Enviroment env = Enviroment.getDefault();
			switch (msg.what) {
			case MSG_S_CONTINUE:
				Neg neg = (Neg) msg.obj;
				try {
					mServer.send(neg);
				} catch (ProtocolException e) {
					e("ProtocolException:", e);
				}
				if (neg.isPass()) {
					transitionTo(mServerConnectedState);
				}
				return HANDLED;

			case MSG_CONNECT:
				d("just ingore the connect req, waiting for the result of address validating.");
				return HANDLED;

			case MSG_DISCONNECT:
				// env.processBondResponse(false);
				mServer.close();
				transitionTo(mServerState);
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case S_IDLE:
					// env.processBondResponse(false);
					transitionTo(mServerState);
					return HANDLED;
				}
				return NOT_HANDLED;
			case MSG_BT_OFF:
				mServer.close();
				transitionTo(mServerState);
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ServerIdleState mServerIdleState = new ServerIdleState();

	private class ServerIdleState extends State {
		// private String mmConnectingAddress;

		@Override
		public void enter() {
			enterLog(this);
			mServer.start();
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_CONNECT:
//				String address = (String) msg.obj;
//				mClientIdleState.setConnectingAddress(address);
//				transitionTo(mClientIdleState);
				i("server ignore connect common !!!!~~~~~~ ");
				return HANDLED;

			case MSG_BT_OFF:
				mServer.close();
				transitionTo(mServerState);
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case S_CONNECTED:
					transitionTo(mServerRespState);
					return HANDLED;

				case S_IDLE:
					transitionTo(mServerState);
					return HANDLED;
				}
				return NOT_HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private final ServerConnectedState mServerConnectedState = new ServerConnectedState();

	private class ServerConnectedState extends State {

		@Override
		public void enter() {
			enterLog(this);
			mTransportManager.notifyMgrState(true);
		}

		@Override
		public void exit() {
			exitLog(this);
			mTransportManager.notifyMgrState(false);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_CONNECT:
				d("just ingore the connect req, waiting for the result of address validating.");
				return HANDLED;

			case MSG_DISCONNECT:
				mServer.close();
				mTransportManager.notifyMgrState(false, DefaultSyncManager.NON_REASON);
				transitionTo(mServerState);
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case S_IDLE:
				    mTransportManager.notifyMgrState(false, DefaultSyncManager.NON_REASON);
					transitionTo(mServerState);
					return HANDLED;
				}
				return NOT_HANDLED;

				// case MSG_SEND:
				// sendRequestInternal((ProjoList) msg.obj);
				// return HANDLED;
			case MSG_BT_OFF:
				mServer.close();
				transitionTo(mServerState);
				return HANDLED;

			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}

	}

	private static void enterLog(State state) {
		v("enter " + state.getName());
	}

	private static void exitLog(State state) {
		v("exit " + state.getName());
	}

	private static void dumpMsg(Message msg, State state) {
		v(convert(msg) + " comes at " + state.getName());
	}

	private static void dumpIngore(Message msg, State state) {
		v("ignore " + convert(msg) + " at " + state.getName());
	}

	private static final String TAG = "<TSME>";

	private static void i(String msg) {
		Log.i(LogTag.APP, TAG + msg);
	}

	private static void d(String msg) {
		Log.d(LogTag.APP, TAG + msg);
	}

	private static void v(String msg) {
		Log.v(LogTag.APP, TAG + msg);
	}

	private static void w(String msg) {
		Log.w(LogTag.APP, TAG + msg);
	}

	/*
	 * private static void e(String msg) { Log.e(LogTag.APP, TAG + msg); }
	 */

	private static void e(String msg, Throwable t) {
		Log.e(LogTag.APP, msg, t);
	}
}

package cn.ingenic.glasssync.transport;

import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.data.CmdProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.ProjoType;
import cn.ingenic.glasssync.data.SystemCmds;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.utils.internal.IState;
import cn.ingenic.glasssync.utils.internal.State;
import cn.ingenic.glasssync.utils.internal.StateMachine;

class TransportStateMachine extends StateMachine {
	
	private static final int MSG_BASE = 0;
	static final int MSG_CONNECT = MSG_BASE + 1;
	static final int MSG_DISCONNECT = MSG_BASE + 2;
	
	static final int MSG_STATE_CHANGE = MSG_BASE + 3;
	static final int MSG_C_RETRIVE = MSG_BASE + 4;
	static final int MSG_S_RETRIVE = MSG_BASE + 5;
	
//	private static final int MSG_SEND = MSG_BASE + 5;
	private static final int MSG_BT_ON = MSG_BASE + 6;
	private static final int MSG_BT_OFF = MSG_BASE + 7;
	private static final int MSG_SEND_BOND_RESP = MSG_BASE + 8;
	
	private static final int STATE_BASE = 0;
	static final int C_IDLE = STATE_BASE + 1; 
	static final int C_CONNECTED = STATE_BASE + 2;
	static final int S_IDLE = STATE_BASE + 3; 
	static final int S_CONNECTED = STATE_BASE + 4;
	
	static String convert(Message msg) {
		switch (msg.what) {
		case MSG_CONNECT:
			return "MSG_CONNECT";
		case MSG_DISCONNECT:
			return "MSG_DISCONNECT";
		case MSG_STATE_CHANGE:
			return "MSG_STATE_CHANGE:" + getState(msg.arg1);
		case MSG_C_RETRIVE:
			return "MSG_C_RETRIVE";
		case MSG_S_RETRIVE:
			return "MSG_S_RETRIVE";
		case MSG_BT_ON:
			return "MSG_BT_ON";
		case MSG_BT_OFF:
			return "MSG_BT_OFF";
		case MSG_SEND_BOND_RESP:
			return "MSG_SEND_BOND_RESP";
		
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
	
	private final BluetoothChannelManager mClientChannelManager;
	private final BluetoothChannelManager mServerChannelManager;
	private final Context mContext;
	private String mRemoteDevAddr;
	private String mRemoteBondAddr;
	//private final String mMyBTAddr = BluetoothAdapter.getDefaultAdapter().getAddress();
	private String mLockedAddr;
	
	private final TransportManager mTransportManager;

	TransportStateMachine(Context context, TransportManager transportManager) {
		super("TransprotStateMachine");
		mContext = context;
		mTransportManager = transportManager;
		
		addState(mDefaultState);
			addState(mClientState, mDefaultState);
				addState(mClientIdleState, mClientState);
				addState(mClientReqState, mClientState);
				addState(mClientConnectedState, mClientState);
			addState(mServerState, mDefaultState);
				addState(mServerIdleState, mServerState);
				addState(mServerRespState, mServerState);
				addState(mServerConnectedState, mServerState);
			
		setInitialState(mServerState);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(mBluetoothReceiver, filter);
		
		mClientChannelManager = new SingleBTChannelManagerClient(getHandler(),
				mContext);
		mServerChannelManager = new SingleBTChannelManagerServer(getHandler(),
				TransportManager.THREAD_POOL_EXECUTOR, mContext);
	}
	
	void sendBondResponse(final boolean pass) {
		Message msg = getHandler().obtainMessage(MSG_SEND_BOND_RESP);
		msg.obj = pass;
		msg.sendToTarget();
	}
	
	private void sendBondRespInternal(boolean pass, boolean transit) {
		IState state = getCurrentState();
		if (state == mServerRespState) {
			Config config = new Config(TransportManager.getSystemMoudleName());
			Projo projo = new CmdProjo(SystemCmds.ADDRESS_RESPONSE);
			int passSt = -1;
			if (!BluetoothAdapter.checkBluetoothAddress(mRemoteDevAddr)) {
				e("Invalid remoteDeviceAddr:" + mRemoteDevAddr + " while sendBondRespInternal");
			} else {
				if (pass) {
					String myBTAddr = BluetoothAdapter.getDefaultAdapter().getAddress();
					if (mLockedAddr.equalsIgnoreCase(mRemoteDevAddr) && myBTAddr.equalsIgnoreCase(mRemoteBondAddr)) {
						passSt = SystemCmds.ST_PASS;
					} else {
						d("RA:" + mRemoteDevAddr + " ? MB:" + mLockedAddr
								+ "  MY:" + myBTAddr + " ? RB:" + mRemoteBondAddr);
						passSt = SystemCmds.ST_PASS_WITH_INIT;
					}
				} 
			}
			projo.put(SystemCmds.COL_PASS_ST, passSt);
			
			i("sendBondResponse pass:" + passSt);
			
			ProjoList projoList = new ProjoList();
			projoList.put(ProjoListColumn.control, config.getControl());
			ArrayList<Projo> datas = new ArrayList<Projo>();
			datas.add(projo);
			projoList.put(ProjoListColumn.datas, datas);
			
			mServerChannelManager.send(projoList, false);
			
			if (transit && pass) {
				if (passSt == SystemCmds.ST_PASS_WITH_INIT) {
					mServerConnectedState.setConnectedArg(DefaultSyncManager.CONNECTED_WITH_INIT);;
				}
				transitionTo(mServerConnectedState);
			} 
		} else {
			e("sendBondResponse:" + pass + " at state:" + getCurrentState().getName());
		}
		
		mRemoteDevAddr = null;
		mLockedAddr = null;
	}
	
	void sendRequest(final ProjoList projoList) {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				sendRequestInternal(projoList);
			}
			
		};
		
		getHandler().post(runnable);
//		Message msg = obtainMessage(MSG_SEND);
//		msg.obj = projoList;
//		msg.sendToTarget();
	}
	
	private BluetoothChannelManager getAvaliableChannelManager() {
		return mIsClient ? mClientChannelManager : mServerChannelManager;
	}
	
	private void sendRequestInternal(ProjoList projoList) {
		getAvaliableChannelManager().send(projoList, false);
	}
	
	void sendRequestSync(ProjoList projoList) {
		getAvaliableChannelManager().send(projoList, true);
	}
	
	void sendRequestByUUID(UUID uuid, ProjoList projoList) {
		BluetoothChannel channel = getAvaliableChannelManager().getChannel(uuid);
		if (channel != null) {
			channel.send(projoList);
		} else {
			w("UUID:" + uuid + " channel does not exist");
		}
	}
	
	void createChannel(UUID uuid, OnChannelCallBack callback) {
		getAvaliableChannelManager().createChannel(uuid, callback);
	}
	
	boolean listenChannel(UUID uuid, OnChannelCallBack callback) {
		return getAvaliableChannelManager().listenChannel(uuid, callback);
	}
	
	void destoryChannel(UUID uuid) {
		getAvaliableChannelManager().destoryChannle(uuid);
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
				
			case MSG_SEND_BOND_RESP:
				w("no one process MSG_SEND_BOND_RESP");
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

		@Override
		public void enter() {
			enterLog(this);
			mIsClient = true;
		}

		@Override
		public void exit() {
			exitLog(this);
			mTransportManager.notifyMgrState(false, mmIdleReason);
			mIsClient = false;
			mmIdleReason = DefaultSyncManager.NON_REASON;
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_CONNECT:
				String address = (String) msg.obj;
				mClientIdleState.setConnectingAddress(address);
				transitionTo(mClientIdleState);
				return HANDLED;
				
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
					mmIdleReason = msg.arg2;
					transitionTo(mServerState);
					return HANDLED;
				case S_CONNECTED:
					if (DefaultSyncManager.isWatch()) {
						mClientChannelManager.prepare("");
					} else {
						mServerChannelManager.prepare("");
					}
					return HANDLED;
//				case S_IDLE:
//					mServerChannelManager.prepare(ServerBTChannelManager.SETUP);
//					return HANDLED;
				}
				
			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
		}
		
	}
	
	private final ServerState mServerState = new ServerState();
	private class ServerState extends State {

		@Override
		public void enter() {
			enterLog(this);
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (BluetoothAdapter.STATE_ON == adapter.getState()) {
				transitionTo(mServerIdleState);
			}
		}

		@Override
		public void exit() {
			exitLog(this);
//			mChannelManager.prepare("");
//			mIsClient = true;
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_BT_ON:
				transitionTo(mServerIdleState);
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
			sendBondRequest();
		}
		
		private void sendBondRequest() {
			Config config = new Config(TransportManager.getSystemMoudleName());
			Projo projo = new CmdProjo(SystemCmds.ADDRESS_REQUEST);
			String address = BluetoothAdapter.getDefaultAdapter().getAddress();
			d("sendBondConfirm my address:" + address);
			projo.put(SystemCmds.COL_ADDRESS, address);
			projo.put(SystemCmds.COL_BOND_ADDR, DefaultSyncManager.getDefault().getLockedAddress());
			ProjoList projoList = new ProjoList();
		    projoList.put(ProjoListColumn.control, config.getControl());
		    ArrayList<Projo> datas = new ArrayList<Projo>();
		    datas.add(projo);
		    projoList.put(ProjoListColumn.datas, datas);
		    
		    mClientChannelManager.send(projoList, false);
		}
		
		void processResponse(ProjoList projoList) {
			if (!projoList.getModule().equals(TransportManager.getSystemMoudleName())) {
				throw new IllegalArgumentException("non System request arrived at RequestState");
			}
			
			ArrayList<Projo> datas = projoList.getDatas();
			if (datas.size() != 1) {
				throw new IllegalArgumentException("wrong data size received at RequestState");
			}
			
			Projo projo = datas.get(0);
			if (ProjoType.CMD != projo.getType()) {
				throw new IllegalArgumentException("non CmdProjo received at RequestState");
			}
			
			CmdProjo cmdProjo = (CmdProjo) projo;
			if (cmdProjo.getCode() == SystemCmds.ADDRESS_RESPONSE) {
				int pass = (Integer) cmdProjo.get(SystemCmds.COL_PASS_ST);
				i("response:" + pass);
				if (pass < SystemCmds.ST_PASS) {
					mClientChannelManager.prepare("");
					mTransportManager.sendClearLockedAddressMsg();
				} else {
					if (pass == SystemCmds.ST_PASS_WITH_INIT) {
						mClientConnectedState
								.setConnectedArg(DefaultSyncManager.CONNECTED_WITH_INIT);
					}
					transitionTo(mClientConnectedState);
				}
			} else if (cmdProjo.getCode() == SystemCmds.ADDRESS_REQUEST) {
				w("receive a ADDRESS_REQUEST msg at Client role, this would happed at an extremely low chance " 
						+ "(connect each other at the same time)");
				mServerRespState.processAddrReq(cmdProjo, false);
			} else {
				throw new IllegalArgumentException("Neither ADDRESS_RESPONSE nor ADDRESS_REQUEST received at RequestState.");
			}
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_C_RETRIVE:
				processResponse((ProjoList) msg.obj);
				return HANDLED;
				
			case MSG_SEND_BOND_RESP:
				sendBondRespInternal((Boolean) msg.obj, false);
				return HANDLED;
				
//			case MSG_S_RETRIVE:
//				w("receive a S msg at Client role, this would happed at an extremely low chance " 
//						+ "(connect each other at the same time), this msg must be address validating msg");
//				mServerRespState.processRequest((ProjoList) msg.obj, false);
//				return HANDLED;
				
			case MSG_DISCONNECT:
				mClientChannelManager.prepare("");
				return HANDLED;
			
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
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
	
	private final ClientIdleState mClientIdleState = new ClientIdleState();
	private class ClientIdleState extends State {
		
		private String mmConnectingAddress;
		
		void setConnectingAddress(String address) {
			mmConnectingAddress = address;
		}

		@Override
		public void enter() {
			enterLog(this);
			if (TextUtils.isEmpty(mmConnectingAddress)) {
				DefaultSyncManager mgr = DefaultSyncManager.getDefault();
				mmConnectingAddress = mgr.getLockedAddress();
			}

			v("Connect address:" + mmConnectingAddress);
			if (!BluetoothAdapter.checkBluetoothAddress(mmConnectingAddress)) {
				throw new IllegalArgumentException("wrong address to connect:" + mmConnectingAddress);
			}
			
			mClientChannelManager.prepare(mmConnectingAddress);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
//				case C_IDLE:
//					transitionTo(mServerState);
//					return HANDLED;
				case C_CONNECTED:
					transitionTo(mClientReqState);
					return HANDLED;
				}
				return NOT_HANDLED;
				
			case MSG_CONNECT:
				v("more connect req comes, ingore it at ClientIdleState.");
				return HANDLED;
				
			default:
				dumpIngore(msg, this);
				return NOT_HANDLED;
			}
			
		}
		
	}
	
	private final ClientConnectedState mClientConnectedState = new ClientConnectedState();
	private class ClientConnectedState extends State {
		private int mmStateArg = DefaultSyncManager.NON_REASON;

		@Override
		public void enter() {
			enterLog(this);
			mTransportManager.notifyMgrState(true, mmStateArg);
			mmStateArg = DefaultSyncManager.NON_REASON;
		}
		
		void setConnectedArg(int arg2) {
			mmStateArg = arg2;
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
//			case MSG_S_RETRIVE:
//				w("receive S msg with Client role.");
			case MSG_C_RETRIVE:
				mTransportManager.retrive((ProjoList) msg.obj);
				return HANDLED;
				
			case MSG_CONNECT:
				d("already connected, ingore connect req at ClientConnectedState.");
				return HANDLED;
				
			case MSG_DISCONNECT:
				mClientChannelManager.prepare("");
				return HANDLED;
				
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case C_IDLE:
					transitionTo(mServerState);
					return HANDLED;
				}
				
//			case MSG_SEND:
//				sendRequestInternal((ProjoList) msg.obj);
//				return HANDLED;
			
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
		
		void processRequest(ProjoList projoList) {
			if (!projoList.getModule().equals(TransportManager.getSystemMoudleName())) {
				throw new IllegalArgumentException("non System request arrived at ResponseState");
			}
			
			ArrayList<Projo> datas = projoList.getDatas();
			if (datas.size() != 1) {
				throw new IllegalArgumentException("wrong data size received at ResponseState");
			}
			
			Projo projo = datas.get(0);
			if (ProjoType.CMD != projo.getType()) {
				throw new IllegalArgumentException("non CmdProjo received at ResponseState");
			}
			
			CmdProjo cmdProjo = (CmdProjo) projo;
			if (cmdProjo.getCode() != SystemCmds.ADDRESS_REQUEST) {
				throw new IllegalArgumentException("non ADDRESS_REQUEST received at ResponseState");
			} 
			processAddrReq(cmdProjo, true);
		}
		
		void processAddrReq(CmdProjo cmdProjo, boolean asServer) {
			mRemoteDevAddr = (String) cmdProjo.get(SystemCmds.COL_ADDRESS);
			mRemoteBondAddr = (String) cmdProjo.get(SystemCmds.COL_BOND_ADDR);
			d("requesting device address:" + mRemoteDevAddr + " and its bonding addr:" + mRemoteBondAddr);
			DefaultSyncManager mgr = DefaultSyncManager.getDefault();
			mLockedAddr = mgr.getLockedAddress();
			if (!BluetoothAdapter.checkBluetoothAddress(mLockedAddr) || !mLockedAddr.equals(mRemoteDevAddr)) {
				i("new Address:" + mRemoteDevAddr + " comes");
				Enviroment env = Enviroment.getDefault();
				env.processBondRequest(mRemoteDevAddr);
			} else {
//				env.processBondResponse(true);
				sendBondRespInternal(true, asServer);
			}
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			Enviroment env = Enviroment.getDefault();
			switch (msg.what) {
			case MSG_S_RETRIVE:
				processRequest((ProjoList) msg.obj);
				return HANDLED;
				
			case MSG_SEND_BOND_RESP:
				sendBondRespInternal((Boolean) msg.obj, true);
				return HANDLED;
				
			case MSG_CONNECT:
				d("just ingore the connect req, waiting for the result of address validating.");
				return HANDLED;
				
			case MSG_DISCONNECT:
				env.processBondResponse(false);
				mServerChannelManager.prepare("");
				transitionTo(mServerState);
				return HANDLED;
			
			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case S_IDLE:
					env.processBondResponse(false);
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
	
	private final ServerIdleState mServerIdleState = new ServerIdleState();
	private class ServerIdleState extends State {
//		private String mmConnectingAddress;

		@Override
		public void enter() {
			enterLog(this);
			mServerChannelManager.prepare(ServerBTChannelManager.SETUP);
		}

		@Override
		public boolean processMessage(Message msg) {
			dumpMsg(msg, this);
			switch (msg.what) {
			// FIXME:retrive msg on ServerIdleState? (This would happen when
			// retriving a large projo which time of reading it from socket or
			// parsing it from byte[] is too long and the connection is
			// disconnected during the time.)
			case MSG_S_RETRIVE:
				mTransportManager.retrive((ProjoList) msg.obj);
				return HANDLED;
			
			case MSG_CONNECT:
				transitionTo(mClientState);
				deferMessage(msg);
				return HANDLED;
				
			case MSG_BT_OFF:
				mServerChannelManager.prepare("");
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
		
		private int mmConnectedArg = DefaultSyncManager.NON_REASON;
		
		void setConnectedArg(int connectedArg) {
			mmConnectedArg = connectedArg;
		}
		@Override
		public void enter() {
			enterLog(this);
			mTransportManager.notifyMgrState(true, mmConnectedArg);
			mmConnectedArg = DefaultSyncManager.NON_REASON;
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
			case MSG_S_RETRIVE:
				mTransportManager.retrive((ProjoList) msg.obj);
				return HANDLED;
				
			case MSG_CONNECT:
				d("just ingore the connect req, waiting for the result of address validating.");
				return HANDLED;	
				
			case MSG_DISCONNECT:
				mServerChannelManager.prepare("");
				return HANDLED;

			case MSG_STATE_CHANGE:
				int state = msg.arg1;
				switch (state) {
				case S_IDLE:
					transitionTo(mServerState);
					return HANDLED;
				}
				return NOT_HANDLED;
				
//			case MSG_SEND:
//				sendRequestInternal((ProjoList) msg.obj);
//				return HANDLED;
			
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
	
	private static final String TAG = "<TSM>";
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
	
	private static void e(String msg) {
		Log.e(LogTag.APP, TAG + msg);
	}
}

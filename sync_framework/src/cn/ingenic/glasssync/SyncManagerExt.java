package cn.ingenic.glasssync;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.LogTag.Mgr;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.data.ServiceProjo;
import cn.ingenic.glasssync.data.ServiceProjo.ServiceColumn;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncSerializableTools;
import cn.ingenic.glasssync.services.SyncService.ThirdPartyModule;
import cn.ingenic.glasssync.transport.TransportManager;
import cn.ingenic.glasssync.transport.ext.TransportManagerExt;

public class SyncManagerExt extends DefaultSyncManager implements
		TransportManagerExt.OnRetriveCallback {

	private TransportManagerExt mTransportManagerExt;
	public static final String UTF_8 = "UTF-8";
	private static final int MODULE_NAME_MAX_LEN = 15;

	protected SyncManagerExt(Context context) {
		super(context);
		TransportManager tranMgr = TransportManager.getDefault();
		if (tranMgr != null && (tranMgr instanceof TransportManagerExt)) {
			mTransportManagerExt = (TransportManagerExt) tranMgr;
			mTransportManagerExt.setRetriveCallback(this);
		} else {
			throw new RuntimeException(
					"SyncManagerExt only support TransportManagerExt");
		}
	}

	@Override
	int request(Config config, ArrayList<Projo> datas, boolean sync) {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (BluetoothAdapter.STATE_ON != adapter.getState()) {
			Mgr.w("can not request without Bluetooth");
			sendCallbackMsg(config.mCallback, NO_LOCKED_ADDRESS);
			return NO_CONNECTIVITY;
		}
		
		if (!BluetoothAdapter.checkBluetoothAddress(getLockedAddress())) {
			Mgr.w("can not request without locked address;");
			sendCallbackMsg(config.mCallback, NO_LOCKED_ADDRESS);
			return NO_LOCKED_ADDRESS;
		}
		
		if (!isFeatureEnabled(config.mFeature)) {
			Mgr.w("Feature:" + config.mFeature + " in module:" + config.mModule + " is disabled in request().");
			sendCallbackMsg(config.mCallback, FEATURE_DISABLED);
			return FEATURE_DISABLED;
		}
		
		ProjoList projoList = new ProjoList();
		projoList.put(ProjoListColumn.control, config.getControl());
		projoList.put(ProjoListColumn.datas, datas);
		
		SyncSerializable serial = SyncSerializableTools.projoList2Serial(projoList);
		return sendSyncSerializable(serial);
	}

	@Override
	public boolean registModule(Module m) {
		try {
			if (m.getName().getBytes(UTF_8).length > MODULE_NAME_MAX_LEN) {
				logw("Module:" + m.getName() + " registed failed. The Name_Len of Module to be registed must be less than "
						+ MODULE_NAME_MAX_LEN + " when using " + UTF_8);
				return false;
			}
		} catch (UnsupportedEncodingException e) {
			loge("", e);
		}
		return super.registModule(m);
	}

	public int sendSyncSerializable(final SyncSerializable serial) {
		if (serial == null) {
			loge("send null serial");
			return UNKNOW;
		}
		final Message callback = serial.getDescriptor().mCallback;
		String module = serial.getDescriptor().mModule;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (BluetoothAdapter.STATE_ON != adapter.getState()) {
			logw("can not request without Bluetooth");
			sendCallbackMsg(callback, NO_LOCKED_ADDRESS);
			return NO_CONNECTIVITY;
		}

		if (!BluetoothAdapter.checkBluetoothAddress(getLockedAddress())) {
			logw("can not request without locked address;");
			sendCallbackMsg(callback, NO_LOCKED_ADDRESS);
			return NO_LOCKED_ADDRESS;
		}

		if (!isFeatureEnabled(module)) {
			logw("Feature:" + module + " in module:" + module
					+ " is disabled in request().");
			sendCallbackMsg(callback, FEATURE_DISABLED);
			return FEATURE_DISABLED;
		}

		if (isConnect()) {
			mTransportManagerExt.send(serial);
			return SUCCESS;
		} else {
			logd("requesting without connecvitity.");
			// connect();
			// push(new DelayedTask() {

			// 	@Override
			// 	public void execute(boolean connected) {
			// 		if (connected) {
			// 			mTransportManagerExt.send(serial);
			// 		} else {
			// 			sendCallbackMsg(callback, NO_CONNECTIVITY);
			// 		}
			// 	}

			// });
			return DELAYED;
		}
	}

	public int send(String module, SyncData data) {
		SyncSerializable serial = SyncSerializableTools.data2Serial(module,
				data);
		return sendSyncSerializable(serial);
	}

	@Override
	public int send(String module, SyncData data, UUID uuid) {
		SyncSerializable serial = SyncSerializableTools.data2Serial(module,
				data, uuid);
		return sendSyncSerializable(serial);
	}

	@Override
	public void holdOnConnTemporary(String module) {
		logw("holdOnConnTemporary unimplement.");
	}

	@Override
	public void destoryChannel(String module, UUID uuid) {
		logw("destoryChannel unimplement.");
	}

	@Override
	public void onRetrive(SyncSerializable serial) {
		String module = serial.getDescriptor().mModule;
		if (!isFeatureEnabled(module)) {
			logw("Feature:" + module + " in module:" + module
					+ " is disabled in response().");
			return;
		}

		Module m = getModule(module);
		if (m == null) {
			loge("There is not any Module be registed with:" + module);
			return;
		}
		
		if (serial instanceof FileOutputSyncSerializable) {
			OnFileChannelCallBack cb = m.getFileChannelCallBack();
			if (cb != null) {
				FileOutputSyncSerializable fs = (FileOutputSyncSerializable) serial;
				cb.onRetriveComplete(fs.getDestName(), true);
			} else {
				loge("Can not find OnFileChannelCallback from module:" + module);
			}
			return;
		}

		if (m instanceof ThirdPartyModule) {
			ThirdPartyModule th = (ThirdPartyModule) m;
			th.onRetrive(serial);
			return;
		}

		SyncDescriptor des = serial.getDescriptor();
		if (des.isProjo) {
			ProjoList pl = SyncSerializableTools.serial2ProjoList(serial);
			if (des.isService) {
				ArrayList<Projo> datas = (ArrayList<Projo>) pl
						.get(ProjoListColumn.datas);
				if (datas.size() == 1) {
					ServiceProjo serviceProjo = (ServiceProjo) datas.get(0);
					int code = (Integer) serviceProjo.get(ServiceColumn.code);
					String descriptor = (String) serviceProjo
							.get(ServiceColumn.descriptor);
					RemoteParcel parcel = (RemoteParcel) serviceProjo
							.get(ServiceColumn.parcel);
					if (!des.isReply) {
						ILocalBinder binder = m.getService(descriptor);
						RemoteParcel reply = binder.onTransact(code, parcel);

						ServiceProjo replyProjo = new ServiceProjo();
						replyProjo.put(ServiceColumn.code, code);
						replyProjo.put(ServiceColumn.descriptor, descriptor);
						replyProjo.put(ServiceColumn.parcel, reply);

						Config config = new Config(m.getName());
						config.mIsService = true;
						config.mIsReply = true;

						ArrayList<Projo> replyDatas = new ArrayList<Projo>();
						replyDatas.add(replyProjo);

						request(config, replyDatas);
					} else {
						IRemoteBinder binder = m.getRemoteService(descriptor);
						binder.onReply(code, parcel);
					}
				} else {
					loge("Service flag Projo, but datas size not 1");
				}
				return;
			}

			if (des.mUUID != null) {
				OnChannelCallBack cb = m.getChannelCallBack(des.mUUID);
				if (cb == null) {
					loge("Can not find OnChannelCallBack for Module:"
							+ m.getName());
					return;
				}
				cb.onRetrive(pl);
				return;
			}
			Transaction tran = m.createTransaction();
			if (tran != null) {
				tran.onCreate(pl.getConfig(), mContext);
				tran.onStart(pl.getDatas());
			} else {
				loge("can not create Transaction in response!");
			}
		} else {
			loge("Unexcepted happened:Received SyncData, but not belong to ThridPartyModule.");
			/*
			 * tran.onCreate(SyncSerializableTools.des2Config(serial.getDescriptor
			 * ()), mContext);
			 * tran.onStart(SyncSerializableTools.serial2Data(serial));
			 */
		}
	}

	@Override
	public boolean sendFile(final String module, final String name, int length,
			InputStream in) {
		SyncDescriptor des = new SyncDescriptor(module);
		des.mModule = module;
		des.mCallback = Message.obtain(this, MSG_RUNNABLE_WITH_ARGS, new RunnableWithArgs() {
			@Override
			public void run() {
				Module m = getModule(module);
				if (m != null) {
					OnFileChannelCallBack cb = m.getFileChannelCallBack();
					if (cb != null) {
						cb.onSendComplete(name, arg1 == DefaultSyncManager.SUCCESS);
					} else {
						loge("Can not find OnFileChannelCallBack from module:"
								+ module + " in callback of sending file");
					}
				} else {
					loge("Can not find Moudle:" + module
							+ " in callback of sending file.");
				}
			}
		});
		SyncSerializable serial = new FileInputSyncSerializable(des, name, length, in);
		int status = sendSyncSerializable(serial);
		return status == SUCCESS || status == DELAYED;
	}

    public boolean sendCMD(String module, SyncData data){
	SyncSerializable serial = SyncSerializableTools.cmd2Serial(module,data);
	
	    int status = sendSyncSerializable(serial);
	    return status == SUCCESS || status == DELAYED;
    }

    public int getWaitingListSize(int type){
	return mTransportManagerExt.getPkgEncodeSize(type);
    }

	@Override
	public boolean sendFileByPath(final String module, final String name, int length,InputStream in, String path) {
	    SyncDescriptor des = new SyncDescriptor(module);
	    des.mModule = module;
	    des.mCallback = Message.obtain(this, MSG_RUNNABLE_WITH_ARGS, new RunnableWithArgs() {
		    @Override
		    public void run() {
			Module m = getModule(module);
			if (m != null) {
			    OnFileChannelCallBack cb = m.getFileChannelCallBack();
			    if (cb != null) {
				cb.onSendComplete(name, arg1 == DefaultSyncManager.SUCCESS);
			    } else {
				loge("Can not find OnFileChannelCallBack from module:"
				     + module + " in callback of sending file");
			    }
			} else {
			    loge("Can not find Moudle:" + module
				 + " in callback of sending file.");
			}
		    }
		});
	    SyncSerializable serial = new FileInputSyncSerializable(des, name, length, in, path);
	    int status = sendSyncSerializable(serial);
	    return status == SUCCESS || status == DELAYED;
	}

	@Override
	public void createChannel(final String module, final UUID uuid) {
		if (isConnect()) {
			triggerChannelCallback(true, module, uuid);
		} else {
			push(new DelayedTask() {

				@Override
				public void execute(boolean connected) {
					triggerChannelCallback(connected, module, uuid);
				}

			});
			connect();
		}
	}

	private void triggerChannelCallback(boolean success, String module,
			UUID uuid) {
		Module requestModule = getModule(module);
		OnChannelCallBack callback = null;
		if (requestModule != null) {
			callback = requestModule.getChannelCallBack(uuid);
			if (callback != null) {
				callback.onCreateComplete(success, true);
			} else {
				loge("Module:" + module + " Can not found OnChannelCallback when trigger Channel onCreate.");
			}
		} else {
			loge("module:" + module + " not found in createChannel");
			return;
		}
	}

	private static final String PRE = "<SME>";

	private static final void logd(String msg) {
		Log.d(LogTag.APP, PRE + msg);
	}

	private static final void loge(String msg) {
		Log.e(LogTag.APP, PRE + msg);
	}

	private static final void loge(String msg, Throwable t) {
		Log.e(LogTag.APP, PRE + msg, t);
	}

	private static final void logw(String msg) {
		Log.w(LogTag.APP, PRE + msg);
	}
}

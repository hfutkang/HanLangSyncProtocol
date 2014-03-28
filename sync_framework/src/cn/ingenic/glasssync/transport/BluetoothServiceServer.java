package cn.ingenic.glasssync.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.ILocalBinder;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.data.ControlProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoList;
import cn.ingenic.glasssync.data.RemoteParcel;
import cn.ingenic.glasssync.data.ServiceProjo;
import cn.ingenic.glasssync.data.ProjoList.ProjoListColumn;
import cn.ingenic.glasssync.data.ServiceProjo.ServiceColumn;

import android.os.Handler;

class BluetoothServiceServer extends BluetoothServer {

	public BluetoothServiceServer(Handler handler, String name, UUID uuid)
			throws IOException {
		super(handler, name, uuid);
	}

	@Override
	public void onRetrive(ProjoList projoList) {
		TransportManager.sendTimeoutMsg();
		ControlProjo controlProjo = (ControlProjo) projoList.get(ProjoListColumn.control);
		@SuppressWarnings("unchecked")
		ArrayList<Projo> datas = (ArrayList<Projo>) projoList.get(ProjoListColumn.datas);
		if (datas.size() == 1) {
			Config config = Config.getConfig(controlProjo);
			if (config.mIsService) {
				ServiceProjo serviceProjo = (ServiceProjo) datas.get(0);
				String descriptor = (String) serviceProjo.get(ServiceColumn.descriptor);
				int code = (Integer)serviceProjo.get(ServiceColumn.code);
				RemoteParcel parcel = (RemoteParcel) serviceProjo.get(ServiceColumn.parcel);
				DefaultSyncManager manager = DefaultSyncManager.getDefault();
				Module module = manager.getModule(config.mModule);
				if (config.mIsReply) {
					IRemoteBinder binder = module.getRemoteService(descriptor);
					binder.onReply(code, parcel);
				} else {
					ILocalBinder binder = module.getService(descriptor);
					RemoteParcel reply = binder.onTransact(code, parcel);

					ServiceProjo replyProjo = new ServiceProjo();
					replyProjo.put(ServiceColumn.code, code);
					replyProjo.put(ServiceColumn.descriptor, descriptor);
					replyProjo.put(ServiceColumn.parcel, reply);
					reply(config.mModule, replyProjo);
				}
			}
			
		}
		
	}
	
	private void reply(String moduel, ServiceProjo projo) {
		ProjoList projoList = new ProjoList();
		
		Config config = new Config(moduel);
		config.mIsService = true;
		config.mIsReply = true;
		
		ArrayList<Projo> datas = new ArrayList<Projo>();
		datas.add(projo);
		
		projoList.put(ProjoListColumn.control, config.getControl());
		projoList.put(ProjoListColumn.datas, datas);
		
		send(projoList);
	}

}

package cn.ingenic.glasssync.devicemanager;

import java.util.ArrayList;

import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;

/** @author dfdun<br>
 * watch receive the data , and make some action depended on the data received 
 * */
public class DeviceTransaction extends Transaction {
	
	@Override
	public void onStart(ArrayList<Projo> datas) {
		super.onStart(datas);
		for(Projo data : datas){
			handleCommandOnWatch(data);
		}
	}
	
	private void handleCommandOnWatch(Projo projo){
		int cmd = (Integer)projo.get(DeviceColumn.command);
		String data = (String)projo.get(DeviceColumn.data);
		klilog.i("watch - handle command: "+cmd+", data:"+data);
		switch(cmd){
		case Commands.CMD_INTERNET_REQUEST:	// 1
		case Commands.CMD_LOCK_SCREEN:		// 2
		case Commands.CMD_RING_AND_VIBRAT:	// 14
		case Commands.CMD_GET_BATTERY:		// 7
		case Commands.CMD_GET_TIME:			// 6
			ConnectionManager.device2Apps(mContext, cmd, data);
			break;
		case Commands.CMD_CLEAR_CALL_LOG:	// 3
			DeviceModule.getInstance().getCallLogManager().clearReceived(data);
			break;
		case Commands.CMD_SYNC_CALL_LOG:	// 5
			DeviceModule.getInstance().getCallLogManager().dataReceived(data);
			break;
		case Commands.CMD_SYNC_HAS_NEW_SYNC:// 10
			ConnectionManager.device2Apps(mContext, cmd, data);
			break;
		}
	}
}

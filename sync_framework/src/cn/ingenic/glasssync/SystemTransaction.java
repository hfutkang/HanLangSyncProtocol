package cn.ingenic.glasssync;

import java.util.ArrayList;
import java.util.Map;

import android.util.Log;

import cn.ingenic.glasssync.data.AddressSendCmd.AddressSendColumn;
import cn.ingenic.glasssync.data.FeatureConfigCmd.FeatureConfigColumn;
import cn.ingenic.glasssync.data.ModeSendCmd.ModeSendColumn;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.CmdProjo;
import cn.ingenic.glasssync.data.ProjoType;
import cn.ingenic.glasssync.data.SystemCmds;
import cn.ingenic.glasssync.data.FileSendCmd.FileSendCmdColumn;
import cn.ingenic.glasssync.transport.TransportManager;

public class SystemTransaction extends Transaction {
	
	private static final String TAG = "SystemTransaction";

	@Override
	public void onStart(ArrayList<Projo> datas) {
		super.onStart(datas);
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		TransportManager tM = TransportManager.getDefault();
		
		for (Projo projo : datas) {
			if (ProjoType.CMD == projo.getType()) {
				CmdProjo cmdProjo = (CmdProjo) projo;
				switch (cmdProjo.getCode()) {
				case SystemCmds.FILE_SEND:
//					FileSendCmd fileSendCmd = (FileSendCmd) cmdProjo;
					String module = (String) cmdProjo.get(FileSendCmdColumn.module);
					String name = (String) cmdProjo.get(FileSendCmdColumn.name);
					int length = (Integer) cmdProjo.get(FileSendCmdColumn.length);
					String address =  (String) cmdProjo.get(FileSendCmdColumn.address);
					manager.retriveFile(module, name, length, address);
					break;
					
				case SystemCmds.FEATURE_CONFIG:
					// FeatureConfigCmd featureConfigCmd = (FeatureConfigCmd)
					// cmdProjo;
					@SuppressWarnings("unchecked")
					Map<String, Boolean> featureConfig = (Map<String, Boolean>) cmdProjo
							.get(FeatureConfigColumn.feature_map);
					manager.applyFeatures(featureConfig);
					break;

				case SystemCmds.ADDRESS_SEND:
					// AddressSendCmd addressSendCmd = (AddressSendCmd)
					// cmdProjo;
					String a = (String) cmdProjo
							.get(AddressSendColumn.address);
					String oldAddress = manager.getLockedAddress();
					Log.i(TAG, "Address change a:" + a+", oldAddress:"+oldAddress);
					if (!oldAddress.equals(a)) {
						Log.i(TAG, "Address change to:" + a);
						manager.setLockedAddress(a);
					}
					break;

				case SystemCmds.MODE_SEND:
					int mode = (Integer) cmdProjo
							.get(ModeSendColumn.mode);
					int oldMode = manager.getCurrentMode();
					if (mode != oldMode) {
						manager.applyMode(mode);
						Log.i(TAG, "Mode change to:" + mode);
						manager.notifyModeChanged(mode);
					}
					break;
					
				case SystemCmds.FILE_CHANNEL_CLOSE:
					tM.closeFileChannel();
					break;
					
//				case SystemCmds.ADDRESS_REQUEST:
//					String deviceAddress = (String) cmdProjo.get(SystemCmds.COL_ADDRESS);
//					Log.d(TAG, "ADDRESS_REQUEST:" + deviceAddress);
//					String bondAddress = manager.getLockedAddress();
//					if (!BluetoothAdapter.checkBluetoothAddress(bondAddress) || !bondAddress.equals(deviceAddress)) {
//						Log.d(TAG, "new Address:" + deviceAddress + " comes");
//						Enviroment env = Enviroment.getDefault();
//						env.processBondRequest(deviceAddress);
//					} else {
//						tM.sendBondResponse(true);
//					}
//					break;
//					
//				case SystemCmds.ADDRESS_RESPONSE:
//					boolean pass = (Boolean) cmdProjo.get(SystemCmds.COL_PASS);
//					Log.d(TAG, "ADDRESS_RESPONSE:" + pass);
//					tM.notifyMgrState(pass);
//					break;
					
				default:
					Log.w(TAG, "not support code:" + cmdProjo.getCode());
					break;
				}
			} else {
				Log.w(TAG, "received projo is not cmd ProjoType in SystemTransaction::onStart()");
			}
		}
	}

}

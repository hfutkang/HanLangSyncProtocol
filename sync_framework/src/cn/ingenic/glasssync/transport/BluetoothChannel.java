package cn.ingenic.glasssync.transport;

import java.util.UUID;

import cn.ingenic.glasssync.data.ProjoList;

public interface BluetoothChannel {
	int CUSTOM = 1;
	int SERVICE = 2;
	
	String CUSTOM_NAME = "BluetoothCustomChannel";
	String SERVICE_NAME = "BluetoothServiceChannel";
	
	UUID CUSTOM_UUID = UUID.fromString("15a6cde4-c997-4a36-916c-5fa58538ccda");
	UUID SERVICE_UUID = UUID.fromString("4219ffae-0c05-4c5f-8f30-bc43a58f7fd1");
	
	UUID W_CUSTOM_UUID = UUID.fromString("5273fb47-b77a-4527-998c-b5c715883469");
	UUID W_SERVICE_UUID = UUID.fromString("3385a711-ced3-470d-a1df-1e88ac06c29f");
	
	UUID getUUID();
	
	boolean isAvailiable();
	
	void send(ProjoList projoList);
	
	void onRetrive(ProjoList projoList);
	
	void close();
}

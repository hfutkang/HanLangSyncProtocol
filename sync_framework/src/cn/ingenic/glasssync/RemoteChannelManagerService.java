package cn.ingenic.glasssync;

import java.util.UUID;

public interface RemoteChannelManagerService {
	String DESPRITOR = "RemoteChannelManagerService";
	
	boolean listenChannel(String module, UUID uuid);
	
	void sendClearMessage();
}

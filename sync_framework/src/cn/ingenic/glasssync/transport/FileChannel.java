package cn.ingenic.glasssync.transport;

import cn.ingenic.glasssync.transport.FileChannelManager.Retrive;
import cn.ingenic.glasssync.transport.FileChannelManager.Request;

public interface FileChannel {
	void send(Request req);
	
	void retrive(Retrive req);
	
	void close();
}

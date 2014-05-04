package cn.ingenic.glasssync;

public interface SyncSerializable {
	SyncDescriptor getDescriptor();
	
	byte[] getDatas(int pos, int len);
	
	int getLength();
}
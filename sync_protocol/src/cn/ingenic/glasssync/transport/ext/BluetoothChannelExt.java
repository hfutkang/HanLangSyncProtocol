package cn.ingenic.glasssync.transport.ext;

interface BluetoothChannelExt {
	void send(Pkg pkg) throws ProtocolException;
	void close();
}

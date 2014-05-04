package cn.ingenic.glasssync.transport.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

final class BluetoothChannelExtTools {
	static void send(Pkg pkg, OutputStream output) throws ProtocolException,
			IOException {
		byte[] datas = pkg.getData();
		int length = datas.length;
		if (length > Pkg.MAX_LEN) {
			throw new ProtocolException("Overflow PKG len：" + length);
		}

		int prefix = pkg.getType();

		int i = (prefix << 14) | length;
		byte a = (byte) (i >> 8);
		byte b = (byte) i;
		byte[] buffer = new byte[2 + length];
		buffer[0] = a;
		buffer[1] = b;
		System.arraycopy(datas, 0, buffer, 2, length);
		logv("start send PKG:" + prefix + " len:" + length);
		output.write(buffer);
		logv("end send PKG" + " a:" + a + " b:" + b);
		try {
			Thread.sleep(190);
		}catch (InterruptedException e){
		}
		pkg=null;//make it to null, gc will release 
	}

	static Pkg retrivePkg(InputStream input) throws IOException, ProtocolException {
		byte a = (byte) input.read();
		byte b = (byte) input.read();
		int len = ((a & 0x3f) << 8) | (b & 0xff);
		logv("start read PKG" + " a:" + a + " b:" + b + " len:" + len);
		byte[] buffer = new byte[len];
		int readed = 0;
		while ((readed += input.read(buffer, readed, len - readed)) < len) {
			//logv("reading:" + readed);
		}
		logv("readed:" + readed);
		if (readed > len) {
			throw new IOException("Try to read " + len + ", but readed " + readed);
		}
		Pkg pkg;
		int prefix = (a & (0xc0)) >> 6;
		if (prefix == Pkg.PKG) {
			pkg = new Pkg(buffer);
		} else if (prefix == Pkg.CFG) {
			pkg = new Cfg(buffer);
		} else if (prefix == Pkg.NEG) {
			pkg = new Neg(buffer);
		} else {
			throw new ProtocolException("unkonw pkg prefix:" + prefix);
		}
		logv("end read PKG:" + prefix + " len:" + len);

		return pkg;
	}
	
	private static final String TAG = "<BCET>";
	
	private static final void logv(String msg) {
		Log.v(ProLogTag.TAG, TAG + msg);
	}
}

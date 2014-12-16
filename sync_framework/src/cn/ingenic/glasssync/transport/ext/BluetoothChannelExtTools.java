package cn.ingenic.glasssync.transport.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.os.Handler;
import cn.ingenic.glasssync.transport.transcompat.BluetoothCompat;

import android.util.Log;

final class BluetoothChannelExtTools {
	/** send data to resume bluetooth socket, because socket maybe block on read().
	 *  we can call write() to try to resume the socket.*/
	public static final int MsgSendAnyData = 100;
	static int send_pkg_index=1,retrive_pkg_index=1;
	private static TmpTask mSendThreadTmp=null;
	private static class TmpTask extends AsyncTask<OutputStream, Void, Void>{
		@Override
		protected void onPostExecute(Void result) {
			mSendThreadTmp = null;
		}

		@Override
		protected Void doInBackground(OutputStream... out) {
			byte[] bu=new byte[2];
			bu[0]=0;bu[1]=0;
			OutputStream os = out[0];
			while (true) {
				try {
					Thread.sleep(5000);
					os.write(bu);
				} catch (Exception e) {
					logv("TmpTask over, reason: "+e.toString());
					break;
				}
			}
			return null;
		}}
	static void send(Pkg pkg, BluetoothCompat output) throws ProtocolException, IOException {
		if(pkg == null){
			byte[] bu=new byte[2];
			bu[0]=0;
			bu[1]=0;
			logv("send null pkg,  try to resume socket.");
			output.write(bu);
			/*
            if (mSendThreadTmp != null) {
				logv("SendThreadTmp is running. do nothing.");
				return;
			}
			if (retrive_pkg_index <= 5) {
				logv("this-should-not-start-TmpTask-to-send-data..retrive_pkd_index-is="+ retrive_pkg_index);
				return;
			}
			mSendThreadTmp=new TmpTask();
			mSendThreadTmp.execute(output,null,null);
			*/
            return;
		}
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
		logv("start send PKG:" + prefix + " len:" + length+";send pkg index ="+send_pkg_index);
		output.write(buffer);
		logv("end send PKG" + " a:" + a + " b:" + b);
		if (b == -1) {
			send_pkg_index++;
		} else {
			send_pkg_index = 1;
		}
		pkg=null;//make it to null, gc will release 
	}

	static Pkg retrivePkg(BluetoothCompat input, Handler handler) throws IOException, ProtocolException {
//		logv("a1 read input_orig.class: "+input_orig.getClass().getName());
		//BufferedInputStream input = new BufferedInputStream(input_orig);
		byte a = (byte) input.read();
		logv("b read");
		byte b = (byte) input.read();
		int len = ((a & 0x3f) << 8) | (b & 0xff);
		logv("start read PKG" + " a:" + a + " b:" + b + " len:" + len+"; retrive pkg index="+retrive_pkg_index);
		if (len < 1){
			logv("len =="+len+"; so return null. this is for trying to resume socket.");
			return null;
		}
		byte[] buffer = new byte[len];
		int readed = 0;
		do {
           if(readed !=0 && readed < len ){
               handler.removeMessages(MsgSendAnyData);
               handler.sendEmptyMessageDelayed(MsgSendAnyData, 100);
               logv("+++++++++++++++++++++++MsgSendAnyData:readed: " + readed  + " len: "+ len);
           }
		   readed += input.read(buffer,readed,len-readed);
		} while (readed < len);
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

		if (b == -1) {
			retrive_pkg_index++;
		} else {
			retrive_pkg_index = 1;
		}
		return pkg;
	}
	
	private static final String TAG = "<BCET>";
	
	private static final void logv(String msg) {
		Log.w(ProLogTag.TAG, TAG + msg);
	}
}

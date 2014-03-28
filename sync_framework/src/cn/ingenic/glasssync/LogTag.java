package cn.ingenic.glasssync;

import android.util.Log;

public class LogTag {
	public static final String APP = "Sync";
	
	public static final String MGR = "<Manager>";
	public static final String TRAN = "<Transaction>";
	public static final String TRANSPORT = "<TransportLayer>";
	public static final String CACHE = "<Cache>";
	public static final String CLIENT = "<Client>";
	public static final String SERVER = "<Server>";
	
	private static final String Sync_EXP = "SyncException";
	
	public static final boolean V = true;
	
	public static void printExp(String tag, Throwable t) {
		if (Log.isLoggable(Sync_EXP, Log.VERBOSE)) {
			Log.v(tag, Log.getStackTraceString(t));
		}
	}
	
	public static class Mgr {
		public static void d(String msg) {
			Log.d(APP, MGR + msg);
		}
		
		public static void i(String msg) {
			Log.i(APP, MGR + msg);
		}
		
		public static void w(String msg) {
			Log.w(APP, MGR + msg);
		}
		
		public static void e(String msg) {
			Log.e(APP, MGR + msg);
		}
	}
	
	public static class Tran {
		public static void d(String msg) {
			Log.d(APP, TRAN + msg);
		}
	}
	
	public static class Transport {
		public static void i(String msg) {
			Log.i(APP, TRANSPORT + msg);
		}
		
		public static void d(String msg) {
			Log.d(APP, TRANSPORT + msg);
		}
		
		public static void w(String msg) {
			Log.w(APP, TRANSPORT + msg);
		}
		
		public static void v(String msg) {
			Log.v(APP, TRANSPORT + msg);
		}
		
		public static void e(String msg) {
			Log.e(APP, TRANSPORT + msg);
		}
	}
	
	public static class Cache {
		public static void d(String msg) {
			Log.d(APP, CACHE + msg);
		}
		
		public static void w(String msg) {
			Log.w(APP, CACHE + msg);
		}
		
		public static void e(String msg) {
			Log.e(APP, CACHE + msg);
		}
		
		public static void i(String msg) {
			Log.i(APP, CACHE + msg);
		}
	}
	
	public static class Client {
		public static void v(String msg) {
			Log.v(APP, CLIENT + msg);
		}
		
		public static void d(String msg) {
			Log.d(APP, CLIENT + msg);
		}
		
		public static void w(String msg) {
			Log.w(APP, CLIENT + msg);
		}
		
		public static void e(String msg) {
			Log.e(APP, CLIENT + msg);
		}
	}
	
	public static class Server {
		public static void d(String msg) {
			Log.d(APP, SERVER + msg);
		}
		
		public static void w(String msg) {
			Log.w(APP, SERVER + msg);
		}
		
		public static void i(String msg) {
			Log.i(APP, SERVER + msg);
		}
		
		public static void e(String msg) {
			Log.e(APP, SERVER + msg);
		}
	}
	
}

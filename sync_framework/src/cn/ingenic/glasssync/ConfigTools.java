package cn.ingenic.glasssync;

import java.util.concurrent.atomic.AtomicInteger;

public class ConfigTools {
	private static AtomicInteger sTid = new AtomicInteger(0);
	public static int generateId() {
		return sTid.incrementAndGet();
	}
}

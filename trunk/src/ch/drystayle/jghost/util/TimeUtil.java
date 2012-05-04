package ch.drystayle.jghost.util;

import java.util.Date;

public class TimeUtil {
	
	static private long startTime;
	
	static {
		startTime = new Date().getTime();
	}
	
	public static int getTicks () {
		return (int) (new Date().getTime() - startTime);
	}
	
	public static long getTime () {
		return new Date().getTime();
	}
}

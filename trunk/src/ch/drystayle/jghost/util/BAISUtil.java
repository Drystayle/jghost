package ch.drystayle.jghost.util;

import java.io.ByteArrayInputStream;

public class BAISUtil {
	
	public static int readInt (ByteArrayInputStream bais) {
		int returnValue = 0;
		returnValue = returnValue | (bais.read());
		//System.out.println(returnValue);
		returnValue = returnValue | (bais.read() << 8);
		//System.out.println(returnValue);
		returnValue = returnValue | (bais.read() << 16);
		//System.out.println(returnValue);
		returnValue = returnValue | (bais.read() << 24);
		//System.out.println(returnValue);
		return returnValue;
	}
	
	public static String readString (ByteArrayInputStream bais, char end) {
		StringBuilder sb = new StringBuilder();
		byte[] tmp = new byte[1]; //garbage value
		bais.read(tmp, 0, 1);
		while (new Character((char) tmp[0]) != '\0') {
			sb.append((char) tmp[0]);
			bais.read(tmp, 0, 1);
		}
		//System.out.println(sb.toString());
		return sb.toString();
	}
}

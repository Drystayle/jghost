package ch.drystayle.jghost.util;

public class StringUtils {

	//---- Static

	public static int findFirstNotOf (String s, String values) {
		for (int i = 0; i < s.length(); i++) {
			CharSequence c = "" + s.charAt(i);
			if (!values.contains(c)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static int toInt32 (String s) {
		return Integer.valueOf(s);
	}
	
	public static String reverse (String source) {
		 int len = source.length();
		 StringBuffer dest = new StringBuffer(len);
		 
		 for (int i = (len - 1); i >= 0; i--) {
			 dest.append(source.charAt(i));
		 }
		 
		 return dest.toString();
	}

	public static String enumNameToPropertyName(String name) {
		StringBuilder enumName = new StringBuilder();
		boolean nextCharUpperCase = false;
		for (int i = 0; i < name.length(); i++) {
			char tmpChar = name.charAt(i);
			if (tmpChar == '_') {
				nextCharUpperCase = true;
			} else {
				if (nextCharUpperCase) {
					enumName.append(Character.toUpperCase(tmpChar));
				} else {
					enumName.append(Character.toLowerCase(tmpChar));
				}
				nextCharUpperCase = false;
			}
		}
		return enumName.toString();
	}
	
}

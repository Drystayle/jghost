package ch.drystayle.jghost.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessagesUtil {

	//---- Static
	
	private static final String MESSAGES_BUNDLE = "messages";
	
	private static String replacePlaceholders (String message, Object... objects) {
		for (int i = 0; i < objects.length; i++) {
			message = message.replace("{" + i + "}", objects[i].toString());
		}
		return message;
	}
	
	public static String getMessage (String propertyName, Object... objects) {
		return getMessage(propertyName, Message.DEFAULT_LOCALE, objects);
	}
	
	public static String getMessage (String propertyName, Locale locale, Object... objects) {
		String message = ResourceBundle.getBundle(MESSAGES_BUNDLE, locale).getString(propertyName);
		if (message.isEmpty()) {
			message = ResourceBundle.getBundle(MESSAGES_BUNDLE).getString(propertyName);
		}
		if (objects.length != 0) {
			message = replacePlaceholders(message, objects);
		}
		return message;
	}
	
}

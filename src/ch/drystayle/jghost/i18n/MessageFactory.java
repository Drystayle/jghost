package ch.drystayle.jghost.i18n;

public class MessageFactory {
	
	//---- Static
	
	public static Message create (String message) {
		return new MessageStaticImpl(message);
	}
	
	public static Message create (Messages message, Object... objects) {
		return new MessageGenericImpl(message, objects);
	}
	
}

package ch.drystayle.jghost.i18n;

import java.util.Locale;

public class MessageStaticImpl implements Message {
	
	//---- State

	private String message;
	
	//---- Constructors
	
	public MessageStaticImpl (String message) {
		this.message = message;
	}
	
	//---- Methods
	
	public Locale getLocale () {
		return null;
	}
	
	public void setLocale (Locale locale) {
		//nop
	}
	
	public String toString () {
		return message;
	}
	
	public String toString (Locale locale) {
		return message;
	}
	
}

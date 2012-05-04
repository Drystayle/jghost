package ch.drystayle.jghost.i18n;

import java.util.Locale;

public class MessageGenericImpl implements Message {

	//---- State

	private Messages m;
	private Object[] objects;
	private Locale locale;
	
	//---- Constructors
	
	public MessageGenericImpl (Messages m, Object[] objects) {
		this(m, null, objects);
	}
	
	public MessageGenericImpl (Messages m, Locale locale, Object[] objects) {
		this.m = m;
		this.locale = locale;
		this.objects = objects;
	}
	
	//---- Methods

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}
	
	public String toString () {
		if (locale != null) {
			return toString(this.locale);
		} else {
			return this.m.createMessageFromArray(objects);
		}
	}
	
	public String toString (Locale locale) {
		return this.m.createMessageFromArray(locale, objects);
	}
	
}

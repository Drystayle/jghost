package ch.drystayle.jghost.i18n;

import java.util.Locale;

public interface Message {
	
	//---- Static
	
	public Locale DEFAULT_LOCALE = Locale.ENGLISH;
	
	//---- Methods
	
	public Locale getLocale ();
	
	public void setLocale (Locale locale);
	
	public String toString ();
	
	public String toString (Locale locale);
	
}

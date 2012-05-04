package ch.drystayle.jghost.i18n;

import java.util.Locale;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.game.Game;

public class IpToCountryData {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(IpToCountryData.class);
	
	//---- State
	
	private String ip1string;
	private String ip2string;
	private int ip1;
	private int ip2;
	private String country2;
	private String country3;
	private String countryFullName;
	
	//---- Constructors
	
	public IpToCountryData (String data) {
		String[] splittedLine = data.split(",");
		for (int i = 0; i < splittedLine.length; i++) {
			if (splittedLine[i].length() > 2) {
				splittedLine[i] = splittedLine[i].substring(1, splittedLine[i].length() - 1);
			} else {
				splittedLine[i] = "";
			}
		}
		this.ip1string = splittedLine[0]; //IP1
		this.ip2string = splittedLine[1]; //IP2
		this.country2 = splittedLine[2]; //Country: 2 letter
		this.country3 = splittedLine[3]; //Country: 3 letter
		this.countryFullName = splittedLine[4]; //Country: Full Name
		try {
			this.ip1 = Integer.valueOf(this.ip1string);
			this.ip2 = Integer.valueOf(this.ip2string);
		} catch (Exception e) {
			LOG.debug("Could not convert string to int", e);
		}
		
	}
	
	//---- Methods
	
	public int getIP1 () {
		return this.ip1;
	}
	
	public int getIP2 () {
		return this.ip2;
	}

	public Locale getLocale() {
		//TODO optimize
		Locale[] loc= Locale.getAvailableLocales();
		for (Locale l : loc) {
			if (l.getCountry().equals(this.country2)) {
				return l;
			}
			
		}
		return null;
	}
	
}

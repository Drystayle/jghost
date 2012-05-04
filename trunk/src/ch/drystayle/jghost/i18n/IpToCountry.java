package ch.drystayle.jghost.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.drystayle.jghost.common.Bytearray;

public class IpToCountry {
	
	//---- State
	
	private List<IpToCountryData> ipToCountryDataList;
	
	//---- Constructor
	
	public IpToCountry () {
		this.ipToCountryDataList = new ArrayList<IpToCountryData>();
	}
	
	//---- Methods
	
	public void add (IpToCountryData data) {
		this.ipToCountryDataList.add(data);
	}
	
	public Locale getLocale (Bytearray ip) {
		if (ip == null || ip.size() == 0) {
			return null;
		}
		int ipInt = ip.toInt();
		for (IpToCountryData ipct : this.ipToCountryDataList) {
			if (ipct.getIP1() <= ipInt && ipct.getIP2() >= ipInt) {
				return ipct.getLocale();
			}
		}
		return null;
	}
	
}

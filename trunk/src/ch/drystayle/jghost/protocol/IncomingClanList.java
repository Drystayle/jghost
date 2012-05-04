package ch.drystayle.jghost.protocol;

public class IncomingClanList {

	//---- State
	
	private String m_Name;
	private char m_Rank;
	private char m_Status;

	//---- Constructors
	
	public IncomingClanList(String nName, char nRank, char nStatus) {
		this.m_Name = nName;
		this.m_Rank = nRank;
		this.m_Status = nStatus;
	}

	//---- Methods
	
	public String getName ()			{ return m_Name; }
	
	public String getRank () {
		switch( m_Rank )
		{
			case 0: return "Recruit";
			case 1: return "Peon";
			case 2: return "Grunt";
			case 3: return "Shaman";
			case 4: return "Chieftain";
		}

		return "Rank Unknown";
	}
	
	public String getStatus () {
		if( m_Status == 0 )
			return "Offline";
		else
			return "Online";
	}
	
	public String getDescription () {
		String description = getName( ) + "\n";
		description += getStatus( ) + "\n";
		description += getRank( ) + "\n\n";
		return description;
	}
}

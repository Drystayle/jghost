package ch.drystayle.jghost.protocol;

public class IncomingFriendList {
	
	//---- State
	
	private String m_Account;
	private char m_Status;
	private char m_Area;
	private String m_Location;

	//---- Constructors
	
	public IncomingFriendList (String nAccount, char nStatus, char nArea, String nLocation){
		this.m_Account = nAccount;
		this.m_Status = nStatus;
		this.m_Area = nArea;
		this.m_Location = nLocation;
	}
	
	//---- Methods
		
	public String GetAccount () { return m_Account; }
	
	public char GetStatus () { return m_Status; }
	
	public char GetArea () { return m_Area; }
	
	public String GetLocation () { return m_Location; }
	
	public String GetDescription () {
		String description = GetAccount( ) + "\n";
		description += ExtractStatus( GetStatus( ) ) + "\n";
		description += ExtractArea( GetArea( ) ) + "\n";
		description += ExtractLocation( GetLocation( ) ) + "\n\n";
		return description;
	}

	private String ExtractStatus (char status) {
		String result = "";

		if(status == 1) {
			result += "<Mutual>";
		}

		if(status == 2) {
			result += "<DND>";
		}

		if(status == 4) {
			result += "<Away>";
		}

		if(result.isEmpty()) {
			result = "<None>";
		}

		return result;
	}
	
	private String ExtractArea (char area) {
		switch(area)
		{
			case 0: return "<Offline>";
			case 1: return "<No Channel>";
			case 2: return "<In Channel>";
			case 3: return "<Public Game>";
			case 4: return "<Private Game>";
			case 5: return "<Private Game>";
		}

		return "<Unknown>";
	}
	
	private String ExtractLocation (String location) {
		String Result = "";

		if( location.substring( 0, 4 ) == "PX3W" ) {
			Result = location.substring( 4 );
		}

		if(Result.isEmpty()) {
			Result = ".";
		}

		return Result;
	}
	
}

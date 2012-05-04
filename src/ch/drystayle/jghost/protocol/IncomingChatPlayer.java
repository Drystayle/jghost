package ch.drystayle.jghost.protocol;

import ch.drystayle.jghost.common.Bytearray;

public class IncomingChatPlayer {
	
	//---- State
	
	private ChatToHostType m_Type;
	private char m_FromPID;
	private Bytearray m_ToPIDs;
	private char m_Flag;
	private String m_Message;
	private char m_Byte;
	private Bytearray m_ExtraFlags;

	//---- Constructors
	
	public IncomingChatPlayer (char nFromPID, Bytearray nToPIDs, char nFlag, String nMessage) {
		m_Type = ChatToHostType.CTH_MESSAGE;
		m_FromPID = nFromPID;
		m_ToPIDs = nToPIDs;
		m_Flag = nFlag;
		m_Message = nMessage;
		m_ExtraFlags = new Bytearray();
	}
	
	public IncomingChatPlayer (char nFromPID, Bytearray nToPIDs, char nFlag, String nMessage, Bytearray nExtraFlags) {
		m_Type = ChatToHostType.CTH_MESSAGEEXTRA;
		m_FromPID = nFromPID;
		m_ToPIDs = nToPIDs;
		m_Flag = nFlag;
		m_Message = nMessage;
		m_ExtraFlags = nExtraFlags;
	}
	
	public IncomingChatPlayer (char nFromPID, Bytearray nToPIDs, char nFlag, char nByte) {
		if( nFlag == 17 )
			m_Type = ChatToHostType.CTH_TEAMCHANGE;
		else if( nFlag == 18 )
			m_Type = ChatToHostType.CTH_COLOURCHANGE;
		else if( nFlag == 19 )
			m_Type = ChatToHostType.CTH_RACECHANGE;
		else if( nFlag == 20 )
			m_Type = ChatToHostType.CTH_HANDICAPCHANGE;

		m_FromPID = nFromPID;
		m_ToPIDs = nToPIDs;
		m_Flag = nFlag;
		m_Byte = nByte;
		m_ExtraFlags = new Bytearray();
	}

	//---- Methods
	
	public ChatToHostType GetType( )	{ return m_Type; }
	public char GetFromPID( )			{ return m_FromPID; }
	public Bytearray GetToPIDs( )		{ return m_ToPIDs; }
	public char GetFlag( )				{ return m_Flag; }
	public String GetMessage( )			{ return m_Message; }
	public char GetByte( )				{ return m_Byte; }
	public Bytearray GetExtraFlags( )	{ return m_ExtraFlags; }
		
	//---- Inner classes & Enums
		
	public enum ChatToHostType {
		
		//---- Static
		
		CTH_MESSAGE((char) 0),			// a chat message
		CTH_MESSAGEEXTRA((char) 1),		// a chat message with extra flags
		CTH_TEAMCHANGE((char) 2),		// a team change request
		CTH_COLOURCHANGE((char) 3),		// a colour change request
		CTH_RACECHANGE((char) 4),		// a race change request
		CTH_HANDICAPCHANGE((char) 5),	// a handicap change request
		;
		
		//---- State
		
		private char val;
		
		//----- Constructors
		
		private ChatToHostType (char val) {
			this.val = val;
		}
		
		//---- Methods
		
		public char toVal () {
			return this.val;
		}
		
	};
}

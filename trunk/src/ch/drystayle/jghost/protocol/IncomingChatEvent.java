package ch.drystayle.jghost.protocol;


//FIX: m_Ping was uint32_t now int

public class IncomingChatEvent {
	
	//---- State
	
	private BnetProtocol.IncomingChatEventEnum m_ChatEvent;
	private int m_Ping;
	private String m_User;
	private String m_Message;

	//---- Constructors
	
	public IncomingChatEvent(
		BnetProtocol.IncomingChatEventEnum nChatEvent, 
		int nPing, 
		String nUser, 
		String nMessage
	) {
		m_ChatEvent = nChatEvent;
		m_Ping = nPing;
		m_User = nUser;
		m_Message = nMessage;
	}

	public BnetProtocol.IncomingChatEventEnum GetChatEvent( )	{ return m_ChatEvent; }
	
	public int GetPing( )									{ return m_Ping; }
	
	public String GetUser( )									{ return m_User; }
	
	public String GetMessage( )								{ return m_Message; }
}

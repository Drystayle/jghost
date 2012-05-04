package ch.drystayle.jghost.protocol;

import ch.drystayle.jghost.common.Bytearray;

//FIX: m_port was uint16_t now int
public class IncomingGameHost {
	
	//---- State
	
	private Bytearray m_IP;
	private int m_Port;
	private String m_GameName;
	private Bytearray m_HostCounter;

	//---- Constructors
	
	public IncomingGameHost (Bytearray nIP, int nPort, String nGameName, Bytearray nHostCounter) {
		m_IP = nIP;
		m_Port = nPort;
		m_GameName = nGameName;
		m_HostCounter = nHostCounter;
	}
	
	//---- Methods

	public Bytearray GetIP() { return m_IP; }
	
	public String GetIPString() {
		String Result = "";

		if( m_IP.size() >= 4 )
		{
			for(int i = 0; i < 4; i++)
			{
				//FIX: was UTIL_ToString( (unsigned int)m_IP[i] );
				Result += m_IP.getChar(i);

				if( i < 3 ) {
					Result += ".";
				}
			}
		}

		return Result;
	}
	
	public int GetPort() { return m_Port; }
	
	public String GetGameName() { return m_GameName; }
	
	public Bytearray GetHostCounter() { return m_HostCounter; }
}

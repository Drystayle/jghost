package ch.drystayle.jghost.protocol;

import ch.drystayle.jghost.common.Bytearray;

public class IncomingJoinPlayer {
	
	//---- State
	
	private String m_Name;
	private Bytearray m_InternalIP;

	//---- Constructors
	
	public IncomingJoinPlayer (String nName, Bytearray nInternalIP) {
		m_Name = nName;
		m_InternalIP = nInternalIP;
	}
	
	//---- Methods

	public String GetName( )			{ return m_Name; }
	public Bytearray GetInternalIP( )	{ return m_InternalIP; }
	
}

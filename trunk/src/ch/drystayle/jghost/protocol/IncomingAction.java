package ch.drystayle.jghost.protocol;

import ch.drystayle.jghost.common.Bytearray;

public class IncomingAction {
	
	//---- State
	
	private char m_PID;
	private Bytearray m_CRC;
	private Bytearray m_Action;

	//---- Constructors
	
	public IncomingAction(char nPID, Bytearray nCRC, Bytearray nAction ) {
		m_PID = nPID;
		m_CRC = nCRC;
		m_Action = nAction;
	}

	//---- Methods

	public char GetPID( )			{ return m_PID; }
	public Bytearray GetCRC( )		{ return m_CRC; }
	public Bytearray GetAction( )	{ return m_Action; }
	public int GetLength()			{ return m_Action.size( ) + 3; }
}

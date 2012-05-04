package ch.drystayle.jghost.bnet;

import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.protocol.BnetProtocol;

public class CommandPacket {
	
	//---- State
	
	private char m_PacketType;
	private char m_ID;
	private Bytearray m_Data;

	//---- Constructors
	
	public CommandPacket (char nPacketType, char nID, Bytearray nData) {
		m_PacketType = nPacketType;
		m_ID = nID;
		m_Data = nData;
	}
	
	//---- Methods

	public char GetPacketType ()				{ return m_PacketType; }
	public char GetID ()						{ return m_ID; }
	public BnetProtocol.Protocol GetProtocol ()	{ return BnetProtocol.Protocol.getEnum(m_ID); }
	public Bytearray GetData ()					{ return m_Data; }
}

package ch.drystayle.jghost.game;

import java.util.LinkedList;
import java.util.Queue;

import ch.drystayle.jghost.bnet.CommandPacket;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.net.GhostTcpSocket;
import ch.drystayle.jghost.protocol.GameProtocol;
import ch.drystayle.jghost.protocol.GameProtocolEnum;
import ch.drystayle.jghost.protocol.IncomingJoinPlayer;

public class PotentialPlayer {
	
	//---- State
	
	protected GameProtocol m_Protocol;
	protected BaseGame m_Game;

	// note: we permit m_Socket to be NULL in this class to allow for the virtual host player which doesn't really exist
	// it also allows us to convert CPotentialPlayers to CGamePlayers without the CPotentialPlayer's destructor closing the socket

	protected GhostTcpSocket m_Socket;
	protected Queue<CommandPacket> m_Packets;
	protected boolean m_DeleteMe;
	protected boolean m_Error;
	protected String m_ErrorString;
	protected IncomingJoinPlayer m_IncomingJoinPlayer;

	//---- Constructors
	
	public PotentialPlayer (GameProtocol nProtocol, BaseGame nGame, GhostTcpSocket nSocket) {
		setProtocol(nProtocol);
		setGame(nGame);
		m_Socket = nSocket;
		m_DeleteMe = false;
		m_Error = false;
		m_IncomingJoinPlayer = null;
		this.m_Packets = new LinkedList<CommandPacket>();
	}
	

	//---- Methods
		
	public GhostTcpSocket GetSocket () { 
		return m_Socket; 
	}
	
	public Bytearray GetExternalIP() {
		char[] Zeros = { 0, 0, 0, 0 };

		if (m_Socket != null) {
			return m_Socket.GetIP();
		}

		return new Bytearray(Zeros);
	}
	
	public String GetExternalIPString() {
		if (m_Socket != null) {
			return m_Socket.GetIPString();
		}

		return "";
	}
	
	public Queue<CommandPacket> GetPackets() { 
		return m_Packets; 
	}
	
	public boolean GetDeleteMe() { 
		return m_DeleteMe; 
	}
	
	public boolean GetError() { 
		return m_Error; 
	}
	
	public String GetErrorString() { 
		return m_ErrorString; 
	}
	
	public IncomingJoinPlayer GetJoinPlayer() { 
		return m_IncomingJoinPlayer; 
	}

	public void SetSocket (GhostTcpSocket nSocket) { 
		m_Socket = nSocket; 
	}
	
	public void SetDeleteMe (boolean nDeleteMe) { 
		m_DeleteMe = nDeleteMe; 
	}

	// processing functions

	public boolean Update (Object fd) {
		if (m_DeleteMe) {
			return true;
		}

		if (m_Socket == null) {
			return false;
		}

		m_Socket.DoRecv(fd);
		ExtractPackets();
		ProcessPackets();

		// don't call DoSend here because some other players may not have updated yet and may generate a packet for this player
		// also m_Socket may have been set to NULL during ProcessPackets but we're banking on the fact that m_DeleteMe has been set to true as well so it'll short circuit before dereferencing

		return m_DeleteMe || m_Error || m_Socket.HasError() || !m_Socket.GetConnected();
	}
	
	public void ExtractPackets () {
		if (m_Socket == null) {
			return;
		}

		// extract as many packets as possible from the socket's receive buffer and put them in the m_Packets queue

		Bytearray Bytes = m_Socket.GetBytes();

		// a packet is at least 4 bytes so loop as long as the buffer contains 4 bytes

		while( Bytes.size( ) >= 4 )
		{
			// byte 0 is always 247

			if( Bytes.getChar(0) == Constants.W3GS_HEADER_CONSTANT )
			{
				// bytes 2 and 3 contain the length of the packet

				short Length = Bytes.extract(2, 2).toShort();

				if (Length >= 4) {
					if (Bytes.size() >= Length) {
						m_Packets.add(new CommandPacket(Constants.W3GS_HEADER_CONSTANT, Bytes.getChar(1), Bytes.extract(0, Length)));
						Bytes = Bytes.extract(Length, Bytearray.END);
					} else {
						return;
					}
				} else {
					m_Error = true;
					m_ErrorString = "received invalid packet from player (bad length)";
					return;
				}
			} else {
				m_Error = true;
				m_ErrorString = "received invalid packet from player (bad header constant)";
				return;
			}
		}
	}
	
	public void ProcessPackets () {
		if (m_Socket == null) {
			return;
		}

		// process all the received packets in the m_Packets queue

		while( !m_Packets.isEmpty( ) ) {
			CommandPacket Packet = m_Packets.poll();

			if (Packet.GetPacketType( ) == Constants.W3GS_HEADER_CONSTANT ) {
				// the only packet we care about as a potential player is W3GS_REQJOIN, ignore everything else

				if (Packet.GetID() == GameProtocolEnum.W3GS_REQJOIN.toVal()) {
					m_IncomingJoinPlayer = getProtocol().RECEIVE_W3GS_REQJOIN( Packet.GetData( ) );

					if (m_IncomingJoinPlayer != null) {
						getGame().EventPlayerJoined( this, m_IncomingJoinPlayer );
					}

					// don't continue looping because there may be more packets waiting and this parent class doesn't handle them
					// EventPlayerJoined creates the new player, NULLs the socket, and sets the delete flag on this object so it'll be deleted shortly
					// any unprocessed packets will be copied to the new CGamePlayer in the constructor or discarded if we get deleted because the game is full

					return;
				}
			}
		}
	}


	public void setProtocol(GameProtocol m_Protocol) {
		this.m_Protocol = m_Protocol;
	}


	public GameProtocol getProtocol() {
		return m_Protocol;
	}


	public void setGame(BaseGame m_Game) {
		this.m_Game = m_Game;
	}


	public BaseGame getGame() {
		return m_Game;
	}


	public void delete() {
		if (this.m_Socket != null) {
			this.m_Socket.Disconnect();
		}
	}
}

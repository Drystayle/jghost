package ch.drystayle.jghost.bnls;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.bnet.CommandPacket;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.net.GhostTcpSocket;
import ch.drystayle.jghost.util.TimeUtil;

public class BnlsClient {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BnlsClient.class);
	
	//---- State
	
	private GhostTcpSocket socket;
	private BnlsProtocol protocol;			// battle.net protocol
	private String server;
	private short port;
	private int wardenCookie;				// the warden cookie
	private int totalWardenIn;
	private int totalWardenOut;
	private long lastNullTime;
	private boolean wasConnected;
	
	private Queue<CommandPacket> packets;			// queue of incoming packets
	private Queue<Bytearray> outPackets;			// queue of outgoing packets to be sent
	private Queue<Bytearray> wardenResponses;		// the warden responses to be sent to battle.net
	
	//---- Constructors
	
	public BnlsClient(String server, short port, int wardenCookie) {
		this.server = server;
		this.port = port;
		this.wardenCookie = wardenCookie;
		this.socket = new GhostTcpSocket();
		this.protocol = new BnlsProtocol();
		this.totalWardenIn = 0;
		this.totalWardenOut = 0;
		this.lastNullTime = 0;
		this.wasConnected = false;
		this.packets = new LinkedList<CommandPacket>();
		this.outPackets = new LinkedList<Bytearray>();
		this.wardenResponses = new LinkedList<Bytearray>();
	}
	
	//---- Methods


	public Bytearray GetWardenResponse() {
		Bytearray WardenResponse = new Bytearray();

		if (!wardenResponses.isEmpty()) {
			WardenResponse = wardenResponses.poll();
			totalWardenOut++;
		}

		return WardenResponse;
	}
	
	public int GetTotalWardenIn () { 
		return totalWardenIn; 
	}
	
	public int GetTotalWardenOut() { 
		return totalWardenOut; 
	}
	
	// processing functions
	
	public boolean Update (Object fd) {
		if (socket.HasError()) {
			LOG.warn("[" + server + ":" + port + ":C" + wardenCookie + "] disconnected from BNLS server due to socket error" );
			return true;
		}

		if( !socket.GetConnecting() && !socket.GetConnected() && wasConnected)
		{
			LOG.warn("[" + server + ":" + port + ":C" + wardenCookie + "] disconnected from BNLS server due to socket not connected" );
			return true;
		}

		if (socket.GetConnected()) {
			socket.DoRecv(fd);
			ExtractPackets( );
			ProcessPackets( );

			if (TimeUtil.getTime() >= lastNullTime + 50000) {
				socket.PutBytes(protocol.SEND_BNLS_NULL());
				lastNullTime = TimeUtil.getTime();
			}

			while( !outPackets.isEmpty()) {
				socket.PutBytes(outPackets.poll());
			}

			socket.doSend();
			return false;
		}

		if (socket.GetConnecting( ) && socket.CheckConnect()) {
			LOG.info("[" + server + ":" + port + ":C" + wardenCookie + "] connected" );
			wasConnected = true;
			lastNullTime = TimeUtil.getTime();
			this.socket.setConnecting(false);
			return false;
		}

		if( !socket.GetConnecting() && !socket.GetConnected() && !wasConnected) {
			LOG.info("[" + server + ":" + port + ":C" + wardenCookie + "] connecting to server [" + server + "] on port " + port);
			socket.Connect("", server, port);
			return false;
		}

		return false;
	}
	
	public void ExtractPackets () {
		Bytearray Bytes = socket.GetBytes();
		
		while (Bytes.size() >= 3) {
			short Length = Bytes.toShort();

			if( Length >= 3 ) {
				if( Bytes.size( ) >= Length ) {
					packets.add(new CommandPacket((char) 0, Bytes.getChar(2), Bytes.extract(0, Length)));
					Bytes = Bytes.extract(Length, Bytearray.END);
				}
				else
					return;
			} else {
				LOG.warn( "[" + server + ":" + port + ":C" + wardenCookie + "] error - received invalid packet from BNLS server (bad length), disconnecting" );
				socket.Disconnect();
				return;
			}
		}
	}
	
	public void ProcessPackets () {
		while(!packets.isEmpty()) {
			CommandPacket Packet = packets.poll();

			if (Packet.GetID() == BnlsProtocolEnum.BNLS_WARDEN.toVal()) {
				Bytearray WardenResponse = protocol.RECEIVE_BNLS_WARDEN(Packet.GetData());

				if (!WardenResponse.isEmpty()) {
					wardenResponses.add(WardenResponse);
				}
			}
		}
	}
	
	// other functions
	
	public void QueueWardenSeed(int seed) {
		outPackets.add(protocol.SEND_BNLS_WARDEN_SEED(wardenCookie, seed));
	}
	
	public void QueueWardenRaw(Bytearray wardenRaw) {
		outPackets.add(protocol.SEND_BNLS_WARDEN_RAW(wardenCookie, wardenRaw));
		totalWardenIn++;
	}
}

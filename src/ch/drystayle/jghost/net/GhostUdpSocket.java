package ch.drystayle.jghost.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.common.Bytearray;

public class GhostUdpSocket {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(GhostUdpSocket.class);
	
	//---- State
	
	private DatagramSocket serverSocket;
	
	//---- Constructors
	
	public GhostUdpSocket () {
		try {
			this.serverSocket = new DatagramSocket();
		} catch (SocketException e) {
			LOG.error("Could not create UDP Socket", e);
		}
	}
	
	
	//---- Methods
	
	public void Broadcast(short port , Bytearray message) {
		byte[] tmpBuffer = new byte[message.size()];
		for (int i = 0; i <message.size(); i++) {
			tmpBuffer[i] = (byte) (char) message.getChar(i);
		}
		try {
			DatagramPacket msg = new DatagramPacket(
				tmpBuffer, 
				tmpBuffer.length, 
				new InetSocketAddress(InetAddress.getLocalHost(), port)
			);
			this.serverSocket.send(msg);
		} catch (SocketException e) {
			LOG.warn("Could not broadcast message", e);
		} catch (IOException e) {
			LOG.warn("Could not broadcast message", e);
		}
	}
	
}

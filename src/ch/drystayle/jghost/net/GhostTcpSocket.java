package ch.drystayle.jghost.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.util.TimeUtil;

public class GhostTcpSocket {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(GhostTcpSocket.class);
	
	//---- State
	
	protected Socket socket;
	protected boolean hasErrors;
	protected Bytearray outputBuffer;
	protected Bytearray inputBuffer;
	protected InputStream is;
	protected OutputStream os;
	protected boolean connecting;
	protected boolean connected;
	protected int port;
	protected String address;
	protected Thread connectingThread;
	private long lastReceived;
	
	//---- Constructors 
	
	public GhostTcpSocket () {
		this(new Socket());
	}
	
	public GhostTcpSocket (Socket socket) {
		this.socket = socket;
		this.hasErrors = false;
		this.inputBuffer = new Bytearray();
		this.outputBuffer = new Bytearray();
		this.connecting = false;
		if (this.socket.isConnected()) {
			this.connected = true;
			this.port = this.socket.getPort();
			try {
				is = socket.getInputStream();
				os = socket.getOutputStream();
			} catch (IOException e) {
				LOG.error("Could not create In/Out Stream for socket", e);
			}
			this.lastReceived = TimeUtil.getTime();
		} else {
			this.connected = false;
			this.port = -1;
		}
	}
	
	//---- Methods
	
	public Bytearray GetIP () {
		byte[] address = this.socket.getInetAddress().getAddress();
		Bytearray ip = new Bytearray();
		for (byte b : address) {
			ip.addChar((char) (0x00FF & b));
		}
		return ip;
	}

	public String GetIPString() {
		return this.socket.getInetAddress().getHostAddress();
	}

	public boolean HasError() {
		return this.hasErrors;
	}

	public boolean GetConnected() {
		return this.connected && !this.connecting;
	}

	public void DoRecv(Object fd) {
		this.inputBuffer = new Bytearray();
		
		try {
			int available = this.is.available();
			if (available != 0) {
				byte[] tmpBuffer = new byte[available];
				this.is.read(tmpBuffer);
				for (byte b : tmpBuffer) {
					this.inputBuffer.addChar((char) (b & 0x00FF));
				}
				this.lastReceived = TimeUtil.getTime();
			}
		} catch (IOException e) {
			LOG.error("Error while reading data from socket stream", e);
			this.hasErrors = true;
		}
	}

	public Bytearray GetBytes() {
		return this.inputBuffer;
	}

	public long GetLastRecv() {
		return lastReceived;
	}
	
	public void SetFD(Object fd, int nfds) {
		// TODO Auto-generated method stub
		
	}

	public void doSend() {
		if (this.os != null) {
			byte[] tmpBuffer = new byte[this.outputBuffer.size()];
			for (int i = 0; i < this.outputBuffer.size(); i++) {
				tmpBuffer[i] = (byte) (char) this.outputBuffer.getChar(i);
			}
			try {
				this.os.write(tmpBuffer);
			} catch (IOException e) {
				LOG.error("Could not write data to socket output stream", e);
				this.hasErrors = true;
			}
			this.outputBuffer = new Bytearray();
		}
	}

	public void PutBytes(Bytearray data) {
		this.outputBuffer.addBytearray(data);
	}

	public Object getErrorString() {
		// TODO Auto-generated method stub
		return "";
	}

	public Bytearray GetPort() {
		int port = this.socket.getPort();
		Bytearray p = new Bytearray();
		p.addChar((char) (0x00FF & port));
		p.addChar((char) (0x00FF & (port >> 8)));
		return p;
	}

	public void Disconnect() {
		try {
			this.socket.close();
		} catch (IOException e) {
			LOG.error("Could not close socket", e);
		}

		this.connected = false;
		this.is = null;
		this.os = null;
	}

	public void Reset() {
		this.connectingThread.interrupt();
		this.connecting = false;
		this.connected = false;
		this.socket = new Socket();
		this.is = null;
		this.os = null;
		this.hasErrors = false;
	}

	public boolean GetConnecting() {
		return this.connecting;
	}

	public void Connect(final String localaddress, final String addr, final int p) {
		this.address = addr;
		this.port = p;
		
		this.connectingThread = new Thread () {
			public void run () {
				try {
					connecting = true;
					socket.connect(new InetSocketAddress(address, port), 15000);
					is = socket.getInputStream();
					os = socket.getOutputStream();
					connected = true;
				} catch (IOException e) {
					LOG.error("Could not connect to " + address + ":" + port, e);
				}
			}
		};
		this.connectingThread.start();
		
	}

	public boolean CheckConnect() {
		return this.connected;
	}

	public void setConnecting(boolean b) {
		this.connecting = b;
	}
}

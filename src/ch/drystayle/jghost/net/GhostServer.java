package ch.drystayle.jghost.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class GhostServer {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(GhostServer.class);
	
	private static final int DEFAULT_BACKLOG = 20;
	private static final int DEFAULT_TIMEOUT = 1;
	
	//---- State
	
	protected ServerSocket serverSocket;
	protected boolean hasErrors;
	private int hostPort;
	private String bindAddress;
	
	//---- Constructors
	
	public GhostServer () {
		//nop
	}
	
	//---- Methods
	
	public boolean Listen(String bindAddress, int hostPort) {
		this.hostPort = hostPort;
		this.bindAddress = bindAddress;
		try {
			if (bindAddress.isEmpty()) {
				this.serverSocket = new ServerSocket(
					hostPort, 
					DEFAULT_BACKLOG,
					InetAddress.getLocalHost()
				);
			} else {
				this.serverSocket = new ServerSocket(
					hostPort, 
					DEFAULT_BACKLOG, 
					InetAddress.getByName(bindAddress)
				);
			}
			this.serverSocket.setSoTimeout(DEFAULT_TIMEOUT);
			return true;
		} catch (UnknownHostException e) {
			LOG.error("Could not bind to " + bindAddress, e);
		} catch (IOException e) {
			LOG.error("Could not start server socket on port " + hostPort, e);
		}
		
		this.serverSocket = null;
		this.hasErrors = true;
		return false;
	}

	public boolean HasError() {
		return this.hasErrors;
	}
	
	public GhostTcpSocket Accept(Object fd) {
		try {
			Socket socket = this.serverSocket.accept();
			return new GhostTcpSocket(socket);
		} catch (SocketTimeoutException e) { 
			//no new sockets
		} catch (IOException e) {
			LOG.error("Errow while accepting new socket", e);
		}
		return null;
	}

	public void SetFD(Object fd, int nfds) {
		
	}
}

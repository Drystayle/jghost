package ch.drystayle.jghost.bnls;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.common.Bytearray;

public class BnlsProtocol {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BnlsProtocol.class);
	
	//---- Constructors
	
	public BnlsProtocol () {
		
	}
	
	//---- Methods
	
	//---- Receive methods

	public Bytearray RECEIVE_BNLS_WARDEN (Bytearray data) {
		// 2 bytes					-> Length
		// 1 byte					-> ID
		// (BYTE)					-> Usage
		// (DWORD)					-> Cookie
		// (BYTE)					-> Result
		// (WORD)					-> Length of data
		// (VOID)					-> Data

		if ( ValidateLength(data) && data.size( ) >= 11 ) {
			char Usage = data.getChar(3);
			int Cookie = data.extract(4, 4).toInt();
			char Result = data.getChar(8);
			short Length = data.extract(9, 2).toShort();

			if( Result == 0x00 ) {
				if (Length == 0) {
					return new Bytearray();
				} else { 
					return data.extract(11, Bytearray.END);
				}
			} else {
				LOG.warn( "Received error code " + (int) data.getChar(8));
			}
		} else {
			LOG.warn("Received invalid package: " + data);
		}

		return new Bytearray();
	}

	//---- Send methods

	public Bytearray SEND_BNLS_NULL () {
		Bytearray packet = new Bytearray();
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar(BnlsProtocolEnum.BNLS_NULL.toVal());	// BNLS_NULL
		AssignLength( packet );
		return packet;
	}
	
	public Bytearray SEND_BNLS_WARDEN_SEED (int cookie, int seed) {
		char[] Client = {  80,  88,  51,  87 };	// "W3XP"

		Bytearray packet = new Bytearray();
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar(BnlsProtocolEnum.BNLS_WARDEN.toVal());	// BNLS_WARDEN
		packet.addChar((char) 0);								// BNLS_WARDEN_SEED
		packet.addInt(cookie);									// cookie
		packet.addCharArray(Client);							// Client
		packet.addShort((short) 4);								// length of seed
		packet.addInt(seed);									// seed
		packet.addChar((char) 0);								// username is blank
		packet.addShort((short) 0);								// password length
																// password
		AssignLength( packet );
		return packet;
	}
	
	public Bytearray SEND_BNLS_WARDEN_RAW (int cookie, Bytearray raw) {
		Bytearray packet = new Bytearray();
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar(BnlsProtocolEnum.BNLS_WARDEN.toVal());	// BNLS_WARDEN
		packet.addChar((char) 1);								// BNLS_WARDEN_RAW
		packet.addInt(cookie);									// cookie
		packet.addShort((short) raw.size());					// raw length
		packet.addBytearray(raw);								// raw
		
		AssignLength( packet );
		return packet;
	}
	
	public Bytearray SEND_BNLS_WARDEN_RUNMODULE (int cookie){ 
		return new Bytearray();
	}
	

	//---- Other methods
		
	private boolean AssignLength (Bytearray content) {
		// insert the actual length of the content array into bytes 1 and 2 (indices 0 and 1)

		Bytearray LengthBytes = new Bytearray();

		if( content.size( ) >= 2 && content.size( ) <= 65535 ) {
			LengthBytes = new Bytearray((short) content.size());
			content.set(0, LengthBytes.getChar(0));
			content.set(1, LengthBytes.getChar(1));
			return true;
		}

		return false;
	}
	
	private boolean ValidateLength (Bytearray content) { 
		// verify that bytes 1 and 2 (indices 0 and 1) of the content array describe the length

		short Length = 0;
		Bytearray LengthBytes = new Bytearray();

		if( content.size( ) >= 2 && content.size( ) <= 65535 ) {
			LengthBytes.addChar(content.getChar(0));
			LengthBytes.addChar(content.getChar(1));
			Length = LengthBytes.toShort();

			if( Length == content.size()) {
				return true;
			}
		}

		return false;
	}
	
}

package ch.drystayle.jghost.protocol;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.util.StringUtils;

public class BnetProtocol {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BnetProtocol.class);

	//---- State
	
	private Bytearray m_ClientToken;			// set in constructor
	private Bytearray m_LogonType;				// set in RECEIVE_SID_AUTH_INFO
	private Bytearray m_ServerToken;			// set in RECEIVE_SID_AUTH_INFO
	private Bytearray m_MPQFileTime;			// set in RECEIVE_SID_AUTH_INFO
	private Bytearray m_IX86VerFileName;		// set in RECEIVE_SID_AUTH_INFO
	private Bytearray m_ValueStringFormula;		// set in RECEIVE_SID_AUTH_INFO
	private Bytearray m_KeyState;				// set in RECEIVE_SID_AUTH_CHECK
	private Bytearray m_KeyStateDescription;	// set in RECEIVE_SID_AUTH_CHECK
	private Bytearray m_Salt;					// set in RECEIVE_SID_AUTH_ACCOUNTLOGON
	private Bytearray m_ServerPublicKey;		// set in RECEIVE_SID_AUTH_ACCOUNTLOGON
	private Bytearray m_UniqueName;				// set in RECEIVE_SID_ENTERCHAT 
	
	//---- Constructors
	
	public BnetProtocol () {
		char[] clientToken = {(char) 220, (char) 1, (char) 203, (char) 7};
		m_ClientToken = new Bytearray(clientToken);
		m_UniqueName = new Bytearray();
	}
	
	//---- Methods
	
	public Bytearray GetClientToken() { return m_ClientToken; }
	
	public Bytearray GetLogonType() { return m_LogonType; }
	
	public Bytearray GetServerToken() { return m_ServerToken; }
	
	public Bytearray GetMPQFileTime() { return m_MPQFileTime; }
	
	public Bytearray GetIX86VerFileName() { return m_IX86VerFileName; }
	
	public String GetIX86VerFileNameString() { return new String(m_IX86VerFileName.asArray()); }
	
	public Bytearray GetValueStringFormula() { return m_ValueStringFormula; }
	
	public String GetValueStringFormulaString() { return new String(m_ValueStringFormula.asArray()); }
	
	public Bytearray GetKeyState() { return m_KeyState; }
	
	public String GetKeyStateDescription() { return new String(m_KeyStateDescription.asArray()); }
	
	public Bytearray GetSalt() { return m_Salt; }
	
	public Bytearray GetServerPublicKey() { return m_ServerPublicKey; }
	
	public Bytearray GetUniqueName() { return m_UniqueName; }
	
	// receive functions

	public boolean RECEIVE_SID_NULL (Bytearray data) {
		LOG.debug("RECEIVED SID_NULL: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length

		return ValidateLength(data);
	}
	
	public IncomingGameHost RECEIVE_SID_GETADVLISTEX (Bytearray data) {
		LOG.debug("RECEIVED SID_GETADVLISTEX: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> GamesFound
		// if( GamesFound > 0 )
		//		10 bytes			-> ???
		//		2 bytes				-> Port
		//		4 bytes				-> IP
		//		null term string	-> GameName
		//		2 bytes				-> ???
		//		8 bytes				-> HostCounter

		if( ValidateLength( data ) && data.size( ) >= 8 )
		{
			Bytearray GamesFound = new Bytearray(data, 4, 8);

			if (GamesFound.toInt() > 0 && data.size() >= 25) {
				Bytearray Port = new Bytearray(data, 18, 20);
				Bytearray IP = new Bytearray(data, 20, 24);
				Bytearray GameName = data.extractCString(24);

				if( data.size( ) >= GameName.size( ) + 35 ) {
					Bytearray HostCounter = new Bytearray();
					HostCounter.addChar(data.extractHex(27, true));
					HostCounter.addChar(data.extractHex(29, true));
					HostCounter.addChar(data.extractHex(31, true));
					HostCounter.addChar(data.extractHex(33, true));
					return new IncomingGameHost(IP, Port.toShort(), GameName.toCharString(), HostCounter);
				}
			}
		}

		return null;
	}
	
	public boolean RECEIVE_SID_ENTERCHAT (Bytearray data) {
		LOG.debug("RECEIVED SID_ENTERCHAT: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// null terminated string	-> UniqueName

		if( ValidateLength(data) && data.size( ) >= 5 ) {
			m_UniqueName = data.extractCString(4);
			return true;
		}

		return false;
	}
	
	public IncomingChatEvent RECEIVE_SID_CHATEVENT (Bytearray data) {
		LOG.debug("RECEIVED SID_CHATEVENT: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> EventID
		// 4 bytes					-> ???
		// 4 bytes					-> Ping
		// 12 bytes					-> ???
		// null terminated string	-> User
		// null terminated string	-> Message

		if( ValidateLength( data ) && data.size( ) >= 29 ) {
			Bytearray EventID = data.extract(4, 4);
			Bytearray Ping = data.extract(12, 4);
			Bytearray User = data.extractCString(28);
			Bytearray Message = data.extractCString(29 + User.size());

			IncomingChatEventEnum eventIdEnum = IncomingChatEventEnum.getEnum((char) EventID.toInt());
			switch (eventIdEnum) {
			case EID_SHOWUSER:
			case EID_JOIN:
			case EID_LEAVE:
			case EID_WHISPER:
			case EID_TALK:
			case EID_BROADCAST:
			case EID_CHANNEL:
			case EID_USERFLAGS:
			case EID_WHISPERSENT:
			case EID_CHANNELFULL:
			case EID_CHANNELDOESNOTEXIST:
			case EID_CHANNELRESTRICTED:
			case EID_INFO:
			case EID_ERROR:
			case EID_EMOTE:
				return new IncomingChatEvent(
					eventIdEnum,
					Ping.toInt(),
					User.toCharString(),
					Message.toCharString() 
				);
			}
		}

		return null;
	}
	
	public boolean RECEIVE_SID_CHECKAD (Bytearray data) {
		LOG.debug("RECEIVED SID_CHECKAD: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length

		return ValidateLength( data );
	}
	
	public boolean RECEIVE_SID_STARTADVEX3 (Bytearray data) {
		LOG.debug("RECEIVED SID_STARTADVEX3: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Status

		if( ValidateLength( data ) && data.size( ) >= 8 ) {
			Bytearray Status = data.extract(4, 4);

			if (Status.toInt() == 0) {
				return true;
			}
		}

		return false;
	}
	
	public Bytearray RECEIVE_SID_PING (Bytearray data) {
		LOG.debug("RECEIVED SID_PING: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Ping

		if (ValidateLength(data) && data.size( ) >= 8 ) {
			return data.extract(4, 4);
		}

		return new Bytearray();
	}
	
	public boolean RECEIVE_SID_LOGONRESPONSE (Bytearray data) {
		LOG.debug("RECEIVED SID_LOGONRESPONSE: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Status

		if( ValidateLength( data ) && data.size( ) >= 8 ) {
			Bytearray Status = data.extract(4, 4);

			if (Status.toInt() == 1) {
				return true;
			}
		}

		return false;
	}
	
	public boolean RECEIVE_SID_AUTH_INFO (Bytearray data) {
		LOG.debug("RECEIVED SID_AUTH_INFO: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> LogonType
		// 4 bytes					-> ServerToken
		// 4 bytes					-> ???
		// 8 bytes					-> MPQFileTime
		// null terminated string	-> IX86VerFileName
		// null terminated string	-> ValueStringFormula

		if( ValidateLength( data ) && data.size( ) >= 25 ) {
			m_LogonType = data.extract(4, 4);
			m_ServerToken = data.extract(8, 4);
			m_MPQFileTime = data.extract(16, 8);
			m_IX86VerFileName = data.extractCString(24);
			m_ValueStringFormula = data.extractCString(25 + m_IX86VerFileName.size());
			return true;
		}

		return false;
	}
	
	public boolean RECEIVE_SID_AUTH_CHECK (Bytearray data) {
		LOG.debug("RECEIVED SID_AUTH_CHECK: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> KeyState
		// null terminated string	-> KeyStateDescription

		if( ValidateLength( data ) && data.size( ) >= 9 )
		{
			m_KeyState = data.extract(4, 4);
			m_KeyStateDescription = data.extractCString(8);

			if (m_KeyState.toInt() == KeyResult.KR_GOOD.toVal()) {
				return true;
			}
		}

		return false;
	}
	
	public boolean RECEIVE_SID_AUTH_ACCOUNTLOGON (Bytearray data) {
		LOG.debug("RECEIVED SID_AUTH_ACCOUNTLOGON: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Status
		// if( Status == 0 )
		//		32 bytes			-> Salt
		//		32 bytes			-> ServerPublicKey

		if( ValidateLength( data ) && data.size( ) >= 8 )
		{
			Bytearray status = data.extract(4, 4);

			if(status.toInt() == 0 && data.size( ) >= 72) {
				m_Salt = data.extract(8, 32);
				m_ServerPublicKey = data.extract(40, 32);
				return true;
			}
		}

		return false;
	}
	
	public boolean RECEIVE_SID_AUTH_ACCOUNTLOGONPROOF (Bytearray data) {
		LOG.debug("RECEIVED SID_AUTH_ACCOUNTLOGONPROOF: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Status

		if( ValidateLength( data ) && data.size( ) >= 8 )
		{
			Bytearray Status = data.extract(4, 4);

			if (Status.toInt() == 0 ) {
				return true;
			}
		}

		return false;
	}
	
	public Bytearray RECEIVE_SID_WARDEN( Bytearray data ) {
		LOG.debug("RECEIVED SID_WARDEN: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// n bytes					-> Data

		if (ValidateLength(data) && data.size() >= 4) {
			return data.extract(4, data.size() - 4);
		}

		return new Bytearray();
	}
	
	public List<IncomingFriendList> RECEIVE_SID_FRIENDSLIST (Bytearray data) {
		LOG.debug("RECEIVED SID_FRIENDSLIST: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 1 byte					-> Total
		// for( 1 .. Total )
		//		null term string	-> Account
		//		1 byte				-> Status
		//		1 byte				-> Area
		//		4 bytes				-> ???
		//		null term string	-> Location

		List<IncomingFriendList> Friends = new ArrayList<IncomingFriendList>();

		if( ValidateLength( data ) && data.size( ) >= 5 ) {
			int i = 5;
			char Total = data.getChar(4);

			while( Total > 0 )
			{
				Total--;

				if( data.size( ) < i + 1 )
					break;

				Bytearray Account = data.extractCString(i);
				i += Account.size() + 1;

				if( data.size( ) < i + 7 )
					break;

				char Status = data.getChar(i);
				char Area = data.getChar(i + 1);
				i += 6;
				Bytearray Location = data.extractCString(i);
				i += Location.size() + 1;
				Friends.add(new IncomingFriendList(	
					Account.toCharString(),
					Status,
					Area,
					Location.toCharString()
				));
			}
		}

		return Friends;
	}
	
	public List<IncomingClanList> RECEIVE_SID_CLANMEMBERLIST (Bytearray data) {
		LOG.debug("RECEIVED SID_CLANMEMBERLIST: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> ???
		// 1 byte					-> Total
		// for( 1 .. Total )
		//		null term string	-> Name
		//		1 byte				-> Rank
		//		1 byte				-> Status
		//		null term string	-> Location

		List<IncomingClanList> ClanList = new ArrayList<IncomingClanList>();

		if( ValidateLength( data ) && data.size( ) >= 9 )
		{
			int i = 9;
			char Total = data.getChar(8);

			while( Total > 0 )
			{
				Total--;

				if( data.size( ) < i + 1 )
					break;

				Bytearray Name = data.extractCString(i);
				i += Name.size( ) + 1;

				if( data.size( ) < i + 3 )
					break;

				char Rank = data.getChar(i);
				char Status = data.getChar(i + 1);
				i += 2;

				// in the original VB source the location string is read but discarded, so that's what I do here

				Bytearray Location = data.extractCString(i);
				i += Location.size() + 1;
				ClanList.add(new IncomingClanList(
					Name.toCharString(),
					Rank,
					Status 
				));
			}
		}

		return ClanList;
	}
	
	public IncomingClanList RECEIVE_SID_CLANMEMBERSTATUSCHANGE (Bytearray data) {
		LOG.debug("RECEIVED SID_CLANMEMBERSTATUSCHANGE: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// null terminated string	-> Name
		// 1 byte					-> Rank
		// 1 byte					-> Status
		// null terminated string	-> Location

		if( ValidateLength( data ) && data.size( ) >= 5 )
		{
			Bytearray Name = data.extractCString(4);

			if( data.size( ) >= Name.size( ) + 7 )
			{
				char Rank = data.getChar(Name.size( ) + 5);
				char Status = data.getChar(Name.size( ) + 6);

				// in the original VB source the location string is read but discarded, so that's what I do here

				Bytearray Location = data.extractCString(Name.size( ) + 7 );
				return new IncomingClanList(Name.toCharString(), Rank, Status);
			}
		}

		return null;
	}

	// send functions

	public Bytearray SEND_PROTOCOL_INITIALIZE_SELECTOR () {
		Bytearray packet = new Bytearray();
		packet.addChar((char) 1);
		
		LOG.debug("SENT PROTOCOL_INITIALIZE_SELECTOR: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_NULL () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_NULL.toVal());		// SID_NULL
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		AssignLength(packet);
		
		LOG.debug("SENT SID_NULL: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_STOPADV () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_STOPADV.toVal());	// SID_STOPADV
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		AssignLength( packet );
		
		LOG.debug("SENT SID_STOPADV: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_GETADVLISTEX (String gameName) {
		char[] MapFilter1	= { 255, 3, 0, 0 };
		char[] MapFilter2	= { 255, 3, 0, 0 };
		char[] MapFilter3	= {   0, 0, 0, 0 };
		char[] NumGames		= {   1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_GETADVLISTEX.toVal());	// SID_GETADVLISTEX
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addCharArray(MapFilter1);					// Map Filter
		packet.addCharArray(MapFilter2);					// Map Filter
		packet.addCharArray(MapFilter3);					// Map Filter
		packet.addCharArray(NumGames);						// maximum number of games to list
		packet.addString(gameName);							// Game Name
		packet.addChar((char) 0);							// Game Password is NULL
		packet.addChar((char) 0);							// Game Stats is NULL
		AssignLength( packet );
		
		LOG.debug("SENT SID_GETADVLISTEX: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_ENTERCHAT () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_ENTERCHAT.toVal());	// SID_ENTERCHAT
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// Account Name is NULL on Warcraft III/The Frozen Throne
		packet.addChar((char) 0);						// Stat String is NULL on CDKEY'd products
		AssignLength( packet );
		
		LOG.debug("SENT SID_ENTERCHAT: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_JOINCHANNEL (String channel) {
		char[] NoCreateJoin	= { 2, 0, 0, 0 };
		char[] FirstJoin	= { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_JOINCHANNEL.toVal());	// SID_JOINCHANNEL
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later

		if ( channel.length( ) > 0 ) {
			packet.addCharArray(NoCreateJoin);				// flags for no create join
		} else {
			packet.addCharArray(FirstJoin);					// flags for first join
		}
		
		packet.addString(channel);
		AssignLength( packet );
		
		LOG.debug("SENT SID_JOINCHANNEL: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_CHATCOMMAND (String command) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_CHATCOMMAND.toVal());	// SID_CHATCOMMAND
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addString(command);							// Message
		AssignLength( packet );
		
		LOG.debug("SENT SID_CHATCOMMAND: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_CHECKAD () {
		char[] Zeros = { 0, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_CHECKAD.toVal());	// SID_CHECKAD
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addCharArray(Zeros);						// ???
		packet.addCharArray(Zeros);						// ???
		packet.addCharArray(Zeros);						// ???
		packet.addCharArray(Zeros);						// ???
		AssignLength( packet );
		
		LOG.debug("SENT SID_CHECKAD: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_STARTADVEX3 (char state, Bytearray mapGameType, Bytearray mapFlags, Bytearray mapWidth, Bytearray mapHeight, String gameName, String hostName, int upTime, String mapPath, Bytearray mapCRC, Bytearray mapSHA1, int hostCounter ) {
		//TODO: sort out how GameType works, the documentation is horrendous

		/*

		Game type tag: (read W3GS_GAMEINFO for this field)
		 0x00000001 - Custom
		 0x00000009 - Blizzard/Ladder
		Map author: (mask 0x00006000) can be combined
		*0x00002000 - Blizzard
		 0x00004000 - Custom
		Battle type: (mask 0x00018000) cant be combined
		 0x00000000 - Battle
		*0x00010000 - Scenario
		Map size: (mask 0x000E0000) can be combined with 2 nearest values
		 0x00020000 - Small
		 0x00040000 - Medium
		*0x00080000 - Huge
		Observers: (mask 0x00700000) cant be combined
		 0x00100000 - Allowed observers
		 0x00200000 - Observers on defeat
		*0x00400000 - No observers
		Flags:
		 0x00000800 - Private game flag (not used in game list)

		*/

		char[] Unknown		= { 255,  3,  0,  0 };
		char[] CustomGame	= {   0,  0,  0,  0 };

		String HostCounterString = String.valueOf(hostCounter);
		while (HostCounterString.length() < 8) {
			HostCounterString = "0" + HostCounterString;
		}
		HostCounterString = StringUtils.reverse(HostCounterString);
		
		Bytearray packet = new Bytearray();

		// make the stat string

		Bytearray StatString = new Bytearray();
		StatString.addBytearray(mapFlags);
		StatString.addChar((char) 0);
		StatString.addBytearray(mapWidth);
		StatString.addBytearray(mapHeight);
		StatString.addBytearray(mapCRC);
		StatString.addString(mapPath);
		StatString.addString(hostName);
		StatString.addChar((char) 0);
		StatString.addBytearray(mapSHA1);
		StatString = StatString.encode();

		if( mapGameType.size( ) == 4 && mapFlags.size( ) == 4 && mapWidth.size( ) == 2 && mapHeight.size( ) == 2 && !gameName.isEmpty( ) && !hostName.isEmpty( ) && !mapPath.isEmpty( ) && mapCRC.size( ) == 4 && mapSHA1.size() == 20 && StatString.size( ) < 128 && HostCounterString.length( ) == 8 )
		{
			// make the rest of the packet

			packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
			packet.addChar(Protocol.SID_STARTADVEX3.toVal());	// SID_STARTADVEX3
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addChar(state);								// State (16 = public, 17 = private, 18 = close)
			packet.addChar((char) 0);							// State continued...
			packet.addChar((char) 0);							// State continued...
			packet.addChar((char) 0);							// State continued...
			packet.addInt(upTime);								// time since creation
			packet.addBytearray(mapGameType);					// Game Type, Parameter
			packet.addCharArray(Unknown);						// ???
			packet.addCharArray(CustomGame);					// Custom Game
			packet.addString(gameName);							// Game Name
			packet.addChar((char) 0);							// Game Password is NULL
			packet.addChar((char) 98);							// Slots Free (ascii 98 = char 'b' = 11 slots free) - note: do not reduce this as this is the # of PID's Warcraft III will allocate
			packet.addString(HostCounterString, false);			// Host Counter
			packet.addBytearray(StatString);					// Stat String
			packet.addChar((char) 0);							// Stat String null terminator (the stat string is encoded to remove all even numbers i.e. zeros)
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_SID_STARTADVEX3");
		}
		
		LOG.debug("SENT SID_STARTADVEX3: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_NOTIFYJOIN (String gameName) {
		char[] ProductID		= {  0, 0, 0, 0 };
		char[] ProductVersion	= { 14, 0, 0, 0 };	// Warcraft III is 14

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_NOTIFYJOIN.toVal());	// SID_NOTIFYJOIN
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addCharArray(ProductID);						// Product ID
		packet.addCharArray(ProductVersion);				// Product Version
		packet.addString(gameName);							// Game Name
		packet.addChar((char) 0 );							// Game Password is NULL
		AssignLength( packet );
		
		LOG.debug("SENT SID_NOTIFYJOIN: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_PING (Bytearray pingValue) {
		Bytearray packet = new Bytearray();

		if( pingValue.size( ) == 4 ) {
			packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
			packet.addChar(Protocol.SID_PING.toVal());		// SID_PING
			packet.addChar((char) 0);						// packet length will be assigned later
			packet.addChar((char) 0);						// packet length will be assigned later
			packet.addBytearray(pingValue);					// Ping Value
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_SID_PING");
		}
			
		LOG.debug("SENT SID_PING: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_LOGONRESPONSE (Bytearray clientToken, Bytearray serverToken, Bytearray passwordHash, String accountName) {
		// todotodo: check that the passed BYTEARRAY sizes are correct (don't know what they should be right now so I can't do this today)

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);			// BNET header constant
		packet.addChar(Protocol.SID_LOGONRESPONSE.toVal());		// SID_LOGONRESPONSE
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addBytearray(clientToken);						// Client Token
		packet.addBytearray(serverToken);						// Server Token
		packet.addBytearray(passwordHash);						// Password Hash
		packet.addString(accountName);							// Account Name
		AssignLength( packet );
		
		LOG.debug("SENT SID_LOGONRESPONSE: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_NETGAMEPORT (short serverPort) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_NETGAMEPORT.toVal());	// SID_NETGAMEPORT
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addShort(serverPort);						// local game server port
		AssignLength( packet );
		
		LOG.debug("SENT SID_NETGAMEPORT: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_AUTH_INFO (char ver, String countryAbbrev, String country) {
		char[] ProtocolID	= {   0,   0,   0,   0 };
		char[] PlatformID	= {  54,  56,  88,  73 };	// "IX86"
		char[] ProductID	= {  80,  88,  51,  87 };	// "W3XP"
		char[] Version		= { ver,   0,   0,   0 };
		char[] Language		= {  83,  85, 110, 101 };	// "enUS"
		char[] LocalIP		= { 127,   0,   0,   1 };
		char[] TimeZoneBias	= {  44,   1,   0,   0 };	// 300 minutes (GMT -0500)
		char[] LocaleID		= {   9,   4,   0,   0 };	// 0x0409 English (United States)
		char[] LanguageID	= {   9,   4,   0,   0 };	// 0x0409 English (United States)

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_AUTH_INFO.toVal());	// SID_AUTH_INFO
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addCharArray(ProtocolID);				// Protocol ID
		packet.addCharArray(PlatformID);				// Platform ID
		packet.addCharArray(ProductID);					// Product ID
		packet.addCharArray(Version);					// Version
		packet.addCharArray(Language);					// Language
		packet.addCharArray(LocalIP);					// Local IP for NAT compatibility
		packet.addCharArray(TimeZoneBias);				// Time Zone Bias
		packet.addCharArray(LocaleID);					// Locale ID
		packet.addCharArray(LanguageID);				// Language ID
		packet.addString(countryAbbrev);				// Country Abbreviation
		packet.addString(country);						// Country
		AssignLength( packet );
		
		LOG.debug("SENT SID_AUTH_INFO: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_AUTH_CHECK (Bytearray clientToken, Bytearray exeVersion, Bytearray exeVersionHash, Bytearray keyInfoROC, Bytearray keyInfoTFT, String exeInfo, String keyOwnerName) {
		char[] NumKeys		= { 2, 0, 0, 0 };	// 2
		char[] UsingSpawn	= { 0, 0, 0, 0 };	// false

		Bytearray packet = new Bytearray();

		if( clientToken.size( ) == 4 && exeVersion.size( ) == 4 && exeVersionHash.size( ) == 4 && keyInfoROC.size( ) == 36 && keyInfoTFT.size( ) == 36 ) {
			packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
			packet.addChar(Protocol.SID_AUTH_CHECK.toVal());	// SID_AUTH_CHECK
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addBytearray(clientToken);					// Client Token
			packet.addBytearray(exeVersion);					// EXE Version
			packet.addBytearray(exeVersionHash);				// EXE Version Hash
			packet.addCharArray(NumKeys);						// number of keys in this packet
			packet.addCharArray(UsingSpawn);					// boolean Using Spawn (32 bit)
			packet.addBytearray(keyInfoROC);					// ROC Key Info
			packet.addBytearray(keyInfoTFT);					// TFT Key Info
			packet.addString(exeInfo);							// EXE Info
			packet.addString(keyOwnerName);						// CD Key Owner Name
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_SID_AUTH_CHECK" );
		}
		
		LOG.debug("SENT SID_AUTH_CHECK: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_AUTH_ACCOUNTLOGON (Bytearray clientPublicKey, String accountName) {
		Bytearray packet = new Bytearray();

		if( clientPublicKey.size( ) == 32 ) {
			packet.addChar(Constants.BNET_HEADER_CONSTANT);			// BNET header constant
			packet.addChar(Protocol.SID_AUTH_ACCOUNTLOGON.toVal());	// SID_AUTH_ACCOUNTLOGON
			packet.addChar((char) 0 );								// packet length will be assigned later
			packet.addChar((char) 0 );								// packet length will be assigned later
			packet.addBytearray(clientPublicKey);					// Client Key
			packet.addString(accountName);							// Account Name
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_SID_AUTH_ACCOUNTLOGON");
		}
		
		LOG.debug("SENT SID_AUTH_ACCOUNTLOGON: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_AUTH_ACCOUNTLOGONPROOF (Bytearray clientPasswordProof) {
		Bytearray packet = new Bytearray();

		if( clientPasswordProof.size( ) == 20 )
		{
			packet.addChar(Constants.BNET_HEADER_CONSTANT);				// BNET header constant
			packet.addChar(Protocol.SID_AUTH_ACCOUNTLOGONPROOF.toVal());// SID_AUTH_ACCOUNTLOGONPROOF
			packet.addChar((char) 0 );									// packet length will be assigned later
			packet.addChar((char) 0 );									// packet length will be assigned later
			packet.addBytearray(clientPasswordProof);					// Client Password Proof
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_SID_AUTH_ACCOUNTLOGON" );
		}
		
		LOG.debug("SENT SID_AUTH_ACCOUNTLOGONPROOF: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_WARDEN (Bytearray wardenResponse) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);	// BNET header constant
		packet.addChar(Protocol.SID_WARDEN.toVal());	// SID_WARDEN
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addChar((char) 0);						// packet length will be assigned later
		packet.addBytearray(wardenResponse);			// warden response
		AssignLength( packet );
		
		LOG.debug("SENT SID_WARDEN: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_FRIENDSLIST () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_FRIENDSLIST.toVal());	// SID_FRIENDSLIST
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		AssignLength( packet );
		
		LOG.debug("SENT SID_FRIENDSLIST: " + packet);
		return packet;
	}
	
	public Bytearray SEND_SID_CLANMEMBERLIST () {
		char[] Cookie = { 0, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.BNET_HEADER_CONSTANT);		// BNET header constant
		packet.addChar(Protocol.SID_CLANMEMBERLIST.toVal());// SID_CLANMEMBERLIST
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addChar((char) 0);							// packet length will be assigned later
		packet.addCharArray(Cookie);			// cookie
		AssignLength(packet);
		
		LOG.debug("SENT SID_CLANMEMBERLIST: " + packet);
		return packet;
	}
	
	// other functions

	private boolean AssignLength (Bytearray content) {
		// insert the actual length of the content array into bytes 3 and 4 (indices 2 and 3)

		Bytearray LengthBytes = null;

		if( content.size( ) >= 4 && content.size( ) <= 65535 ) {
			LengthBytes = new Bytearray((short) content.size());
			content.set(2, LengthBytes.getChar(0));
			content.set(3, LengthBytes.getChar(1));
			return true;
		}

		return false;
	}
	
	private boolean ValidateLength (Bytearray content) {
		// verify that bytes 3 and 4 (indices 2 and 3) of the content array describe the length

		short Length;
		Bytearray LengthBytes = new Bytearray();

		if( content.size( ) >= 4 && content.size( ) <= 65535 ) {
			LengthBytes.addChar(content.getChar(2));
			LengthBytes.addChar(content.getChar(3));
			Length = LengthBytes.toShort();

			if (Length == content.size()) {
				return true;
			}
		}

		return false;
	}
		
	//---- Inner classes
	
	public enum Protocol {
		SID_NULL((char) 0x0),
		SID_STOPADV((char) 0x2),
		SID_GETADVLISTEX((char) 0x9),
		SID_ENTERCHAT((char) 0xA),
		SID_JOINCHANNEL((char) 0xC),
		SID_CHATCOMMAND((char) 0xE),
		SID_CHATEVENT((char) 0xF),
		SID_CHECKAD((char) 0x15), 
		SID_STARTADVEX3((char) 0x1C), 
		SID_DISPLAYAD((char) 0x21),
		SID_NOTIFYJOIN((char) 0x22),
		SID_PING((char) 0x25),
		SID_LOGONRESPONSE((char) 0x29),
		SID_NETGAMEPORT((char) 0x45),
		SID_AUTH_INFO((char) 0x50), 
		SID_AUTH_CHECK((char) 0x51),
		SID_AUTH_ACCOUNTLOGON((char) 0x53),
		SID_AUTH_ACCOUNTLOGONPROOF((char) 0x54),
		SID_WARDEN((char) 0x5E),
		SID_FRIENDSLIST((char) 0x65),
		SID_FRIENDSUPDATE((char) 0x66), 
		SID_CLANMEMBERLIST((char) 0x7D),
		SID_CLANMEMBERSTATUSCHANGE((char) 0x7F);
		
		public static Protocol getEnum (char val) {
			Protocol[] values = Protocol.values();
			for (Protocol value : values) {
				if (value.toVal() == val) {
					return value;
				}
			}
			
			return null;
		}
		
		//---- State
		
		private char val;
		
		//---- Constructors
		
		private Protocol (char val) {
			this.val = val;
		}
		
		public char toVal() { 
			return this.val; 
		}
	}
	
	public enum KeyResult {
		KR_GOOD((char) 0),
		KR_OLD_GAME_VERSION((char) 256),
		KR_INVALID_VERSION((char) 257),
		KR_ROC_KEY_IN_USE((char) 513),
		KR_TFT_KEY_IN_USE((char) 529);
		
		public static KeyResult getEnum (char val) {
			KeyResult[] values = KeyResult.values();
			for (KeyResult value : values) {
				if (value.toVal() == val) {
					return value;
				}
			}
			
			return null;
		}
		
		//---- State
		
		private char val;
		
		//---- Constructors
		
		private KeyResult (char val) {
			this.val = val;
		}

		//---- Methods
		
		public char toVal () {
			return this.val;
		}
		
	}
	
	public enum IncomingChatEventEnum {
		EID_SHOWUSER((char) 1),	// received when you join a channel (includes users in the channel and their information)
		EID_JOIN((char) 2),	// received when someone joins the channel you're currently in
		EID_LEAVE((char) 3),	// received when someone leaves the channel you're currently in
		EID_WHISPER((char) 4),	// received a whisper message
		EID_TALK((char) 5),	// received when someone talks in the channel you're currently in
		EID_BROADCAST((char) 6),	// server broadcast
		EID_CHANNEL((char) 7),	// received when you join a channel (includes the channel's name, flags)
		EID_USERFLAGS((char) 9),	// user flags updates
		EID_WHISPERSENT((char) 10),	// sent a whisper message
		EID_CHANNELFULL((char) 13),	// channel is full
		EID_CHANNELDOESNOTEXIST((char) 14),	// channel does not exist
		EID_CHANNELRESTRICTED((char) 15),	// channel is restricted
		EID_INFO((char) 18),	// broadcast/information message
		EID_ERROR((char) 19),	// error message
		EID_EMOTE((char) 23);	// emote

		public static IncomingChatEventEnum getEnum (char val) {
			IncomingChatEventEnum[] values = IncomingChatEventEnum.values();
			for (IncomingChatEventEnum value : values) {
				if (value.toVal() == val) {
					return value;
				}
			}
			
			return null;
		}
		
		//---- State
		
		private char val;
		
		//---- Constructors
		
		private IncomingChatEventEnum (char val) {
			this.val = val;
		}
		
		//---- Methods
		
		public char toVal () {
			return this.val;
		}
		
	}
}

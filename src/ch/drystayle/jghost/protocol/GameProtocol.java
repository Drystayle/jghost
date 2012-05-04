package ch.drystayle.jghost.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.game.GamePlayer;
import ch.drystayle.jghost.game.GameSlot;
import ch.drystayle.jghost.util.TimeUtil;

public class GameProtocol {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(GameProtocol.class);

	//---- State
	
	private JGhost jGhost;
	
	//---- Constructors
	
	public GameProtocol(JGhost jGhost) {
		this.jGhost = jGhost;
	}
	
	//---- Methods
	
	// receive functions

	public IncomingJoinPlayer RECEIVE_W3GS_REQJOIN( Bytearray data ) {
		LOG.debug("RECEIVED W3GS_REQJOIN: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 15 bytes					-> ???
		// null terminated string	-> Name
		// 4 bytes					-> ???
		// 2 bytes					-> InternalPort (???)
		// 4 bytes					-> InternalIP

		if( ValidateLength( data ) && data.size( ) >= 20 ) {
			Bytearray Name =  data.extractCString(19);
			if(!Name.isEmpty( ) && data.size( ) >= Name.size( ) + 30 ) {
				Bytearray InternalIP = data.extract(Name.size() + 26, 4);
				return new IncomingJoinPlayer(Name.toCharString(), InternalIP );
			}
		}

		return null;
	}
	
	public boolean RECEIVE_W3GS_LEAVEGAME (Bytearray data) {
		LOG.debug("RECEIVED W3GS_LEAVEGAME: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length

		if( ValidateLength( data ) )
			return true;

		return false;
	}
	
	
	public boolean RECEIVE_W3GS_GAMELOADED_SELF (Bytearray data) {
		LOG.debug("RECEIVED W3GS_GAMELOADED_SELF: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length

		if( ValidateLength( data ) )
			return true;

		return false;
	}
	
	public IncomingAction RECEIVE_W3GS_OUTGOING_ACTION (Bytearray data, char PID) {
		LOG.debug("RECEIVED W3GS_OUTGOING_ACTION: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> CRC
		// remainder of packet		-> Action

		if( PID != 255 && ValidateLength( data ) && data.size( ) >= 8 )
		{
			Bytearray CRC = data.extract(4, 4);
			Bytearray Action = data.extract(8, Bytearray.END);
			return new IncomingAction( PID, CRC, Action );
		}

		return null;
	}
	
	public int RECEIVE_W3GS_OUTGOING_KEEPALIVE (Bytearray data) {
		LOG.debug("RECEIVED W3GS_OUTGOING_KEEPALIVE: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 1 byte					-> ???
		// 4 bytes					-> CheckSum??? (used in replays)

		if (ValidateLength(data) && data.size() == 9 ) {
			return data.toInt(5);
		}

		return 0;
	}
	
	public IncomingChatPlayer RECEIVE_W3GS_CHAT_TO_HOST (Bytearray data) {
		LOG.debug("RECEIVED W3GS_CHAT_TO_HOST: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 1 byte					-> Total
		// for( 1 .. Total )
		//		1 byte				-> ToPID
		// 1 byte					-> FromPID
		// 1 byte					-> Flag
		// if( Flag == 16 )
		//		null term string	-> Message
		// elseif( Flag == 17 )
		//		1 byte				-> Team
		// elseif( Flag == 18 )
		//		1 byte				-> Colour
		// elseif( Flag == 19 )
		//		1 byte				-> Race
		// elseif( Flag == 20 )
		//		1 byte				-> Handicap
		// elseif( Flag == 32 )
		//		4 bytes				-> ExtraFlags
		//		null term string	-> Message

		if( ValidateLength( data ) )
		{
			int i = 5;
			char Total = data.getChar(4);

			if( Total > 0 && data.size( ) >= i + Total )
			{
				Bytearray ToPIDs = data.extract(i, Total);
				i += Total;
				char FromPID = data.getChar(i);
				char Flag = data.getChar(i + 1);
				i += 2;

				if( Flag == 16 && data.size( ) >= i + 1 )
				{
					// chat message

					Bytearray Message = data.extractCString(i);
					return new IncomingChatPlayer( FromPID, ToPIDs, Flag, Message.toCharString());
				}
				else if( ( Flag >= 17 && Flag <= 20 ) && data.size( ) >= i + 1 )
				{
					// team/colour/race/handicap change request

					char Byte = data.getChar(i);
					return new IncomingChatPlayer( FromPID, ToPIDs, Flag, Byte );
				}
				else if( Flag == 32 && data.size( ) >= i + 5 )
				{
					// chat message with extra flags

					Bytearray ExtraFlags = data.extract(i, 4);
					Bytearray Message = data.extractCString(i + 4);
					return new IncomingChatPlayer( FromPID, ToPIDs, Flag, Message.toCharString(), ExtraFlags );
				}
			}
		}

		return null;
	}
	
	public boolean RECEIVE_W3GS_SEARCHGAME(Bytearray data) {
		int ProductID = 1462982736;	// "W3XP"
		int Version = 23;			// 1.23

		LOG.debug("RECEIVED W3GS_SEARCHGAME: " + data);

		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> ProductID
		// 4 bytes					-> Version
		// 4 bytes					-> ??? (Zero)

		if (ValidateLength(data) && data.size( ) >= 16 ) {
			if (data.toInt(4) == ProductID ) {
				if (data.toInt(8) == Version ) {
					if (data.toInt(12) == 0 ) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	public IncomingMapSize RECEIVE_W3GS_MAPSIZE (Bytearray data, Bytearray mapSize) {
		LOG.debug("RECEIVED W3GS_MAPSIZE: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> ???
		// 1 byte					-> SizeFlag (1 = have map, 3 = continue download)
		// 4 bytes					-> MapSize

		if( ValidateLength( data ) && data.size( ) >= 13 ) {
			return new IncomingMapSize( data.getChar(8), data.toInt(9));
		}

		return null;
	}
	
	public int RECEIVE_W3GS_MAPPARTOK (Bytearray data) {
		LOG.debug("RECEIVED W3GS_MAPPARTOK: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 1 byte					-> SenderPID
		// 1 byte					-> ReceiverPID
		// 4 bytes					-> ???
		// 4 bytes					-> MapSize

		if( ValidateLength( data ) && data.size( ) >= 14 ) {
			return data.toInt(10);
		}

		return 0;
	}
	
	public int RECEIVE_W3GS_PONG_TO_HOST (Bytearray data) {
		LOG.debug("RECEIVED W3GS_PONG_TO_HOST: " + data);
		
		// 2 bytes					-> Header
		// 2 bytes					-> Length
		// 4 bytes					-> Pong

		// the pong value is just a copy of whatever was sent in SEND_W3GS_PING_FROM_HOST which was GetTicks( ) at the time of sending
		// so as long as we trust that the client isn't trying to fake us out and mess with the pong value we can find the round trip time by simple subtraction
		// (the subtraction is done elsewhere because the very first pong value seems to be 1 and we want to discard that one)

		if( ValidateLength( data ) && data.size( ) >= 8 ) {
			return data.toInt(4);
		}

		return 1;
	}

	// send functions
	
	public Bytearray SEND_W3GS_PING_FROM_HOST () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT );				// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_PING_FROM_HOST.toVal());	// W3GS_PING_FROM_HOST
		packet.addChar((char) 0);										// packet length will be assigned later
		packet.addChar((char) 0);										// packet length will be assigned later
		packet.addInt(TimeUtil.getTicks());
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_PING_FROM_HOST: " + packet);
		
		return packet;
	}
	
	public Bytearray SEND_W3GS_SLOTINFOJOIN (char PID, Bytearray port, Bytearray externalIP, List<GameSlot> slots, int randomSeed, char gameType, char playerSlots) {
		char[] Zeros = { 0, 0, 0, 0 };

		Bytearray SlotInfo = EncodeSlotInfo( slots, randomSeed, gameType, playerSlots );
		Bytearray packet = new Bytearray();

		if( port.size( ) == 2 && externalIP.size( ) == 4 ) {
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);		// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_SLOTINFOJOIN.toVal());	// W3GS_SLOTINFOJOIN
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addShort((short) SlotInfo.size());			// SlotInfo length
			packet.addBytearray(SlotInfo);						// SlotInfo
			packet.addChar(PID);								// PID
			packet.addChar((char) 2);							// AF_INET
			packet.addChar((char) 0);							// AF_INET continued...
			packet.addBytearray(port);							// port
			packet.addBytearray(externalIP);					// external IP
			packet.addCharArray(Zeros);							// ???
			packet.addCharArray(Zeros);							// ???
			AssignLength(packet);
		} else {
			LOG.error("Invalid parameters passed to SEND_W3GS_SLOTINFOJOIN" );
		}
			
		LOG.debug("SENT W3GS_SLOTINFOJOIN: " + packet);
		
		return packet;
	}
	
	public Bytearray SEND_W3GS_REJECTJOIN (int reason) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);		// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_REJECTJOIN.toVal());	// W3GS_REJECTJOIN
		packet.addChar((char) 0 );							// packet length will be assigned later
		packet.addChar((char) 0 );							// packet length will be assigned later
		packet.addInt(reason);								// reason
		AssignLength(packet);
		
		LOG.debug("SENT W3GS_REJECTJOIN: " + packet);
		
		return packet;
	}
	
	public Bytearray SEND_W3GS_PLAYERINFO (char PID, String name, Bytearray externalIP, Bytearray internalIP) {
		char[] PlayerJoinCounter	= { 2, 0, 0, 0 };
		char[] Zeros				= { 0, 0, 0, 0 };

		Bytearray packet = new Bytearray();

		if( !name.isEmpty( ) && name.length( ) <= 15 && externalIP.size( ) == 4 && internalIP.size( ) == 4 ) {
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);		// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_PLAYERINFO.toVal());	// W3GS_PLAYERINFO
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addChar((char) 0);							// packet length will be assigned later
			packet.addCharArray(PlayerJoinCounter);				// player join counter
			packet.addChar((char) PID);							// PID
			packet.addString(name);								// player name
			packet.addChar((char) 1);							// ???
			packet.addChar((char) 0);							// ???
			packet.addChar((char) 2);							// AF_INET
			packet.addChar((char) 0);							// AF_INET continued...
			packet.addChar((char) 0);							// port
			packet.addChar((char) 0);							// port continued...
			packet.addBytearray(externalIP);					// external IP
			packet.addCharArray(Zeros);							// ???
			packet.addCharArray(Zeros);							// ???
			packet.addChar((char) 2);							// AF_INET
			packet.addChar((char) 0);							// AF_INET continued...
			packet.addChar((char) 0);							// port
			packet.addChar((char) 0);							// port continued...
			packet.addBytearray(internalIP);					// internal IP
			packet.addCharArray(Zeros);							// ???
			packet.addCharArray(Zeros);							// ???
			AssignLength( packet );
		} else {
			LOG.error("Invalid parameters passed to SEND_W3GS_PLAYERINFO");
		}
		
		LOG.debug("SENT W3GS_PLAYERINFO: " + packet);
		
		return packet;
	}
	
	
	public Bytearray SEND_W3GS_PLAYERLEAVE_OTHERS (char PID, int leftCode) {
		Bytearray packet = new Bytearray();

		if( PID != 255 )
		{
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_PLAYERLEAVE_OTHERS.toVal());	// W3GS_PLAYERLEAVE_OTHERS
			packet.addChar((char) 0 );									// packet length will be assigned later
			packet.addChar((char) 0 );									// packet length will be assigned later
			packet.addChar( PID );										// PID
			packet.addInt(leftCode); 									// left code (see PLAYERLEAVE_ constants in gameprotocol.h)
			AssignLength( packet );
		} else {
			LOG.error("Invalid parameters passed to SEND_W3GS_PLAYERLEAVE_OTHERS" );
		}
			
		LOG.debug("SENT W3GS_PLAYERLEAVE_OTHERS: " + packet);
		return packet;
	}
	
	
	public Bytearray SEND_W3GS_GAMELOADED_OTHERS (char PID) {
		Bytearray packet = new Bytearray();

		if( PID != 255 )
		{
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_GAMELOADED_OTHERS.toVal());// W3GS_GAMELOADED_OTHERS
			packet.addChar((char) 0 );								// packet length will be assigned later
			packet.addChar((char) 0 );								// packet length will be assigned later
			packet.addChar( PID );									// PID
			AssignLength( packet );
		} else {
			LOG.error("Invalid parameters passed to SEND_W3GS_GAMELOADED_OTHERS" );
		}
			
		LOG.debug("SENT W3GS_GAMELOADED_OTHERS: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_SLOTINFO (List<GameSlot> slots, int randomSeed, char gameType, char playerSlots) {
		Bytearray SlotInfo = EncodeSlotInfo( slots, randomSeed, gameType, playerSlots );
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT );	// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_SLOTINFO.toVal() );	// W3GS_SLOTINFO
		packet.addChar((char) 0 );							// packet length will be assigned later
		packet.addChar((char) 0 );							// packet length will be assigned later
		packet.addShort((short) SlotInfo.size());			// SlotInfo length
		packet.addBytearray(SlotInfo);						// SlotInfo
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_SLOTINFO: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_COUNTDOWN_START () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_COUNTDOWN_START.toVal());	// W3GS_COUNTDOWN_START
		packet.addChar((char) 0 );								// packet length will be assigned later
		packet.addChar((char) 0 );								// packet length will be assigned later
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_COUNTDOWN_START: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_COUNTDOWN_END () {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT );		// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_COUNTDOWN_END.toVal());	// W3GS_COUNTDOWN_END
		packet.addChar((char) 0 );								// packet length will be assigned later
		packet.addChar((char) 0 );								// packet length will be assigned later
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_COUNTDOWN_END: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_INCOMING_ACTION( Queue<IncomingAction> actions, short sendInterval ) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_INCOMING_ACTION.toVal());	// W3GS_INCOMING_ACTION
		packet.addChar((char) 0 );								// packet length will be assigned later
		packet.addChar((char) 0 );								// packet length will be assigned later
		packet.addShort(sendInterval);							// send interval

		// create subpacket

		if(!actions.isEmpty())
		{
			Bytearray subpacket = new Bytearray();

			while( !actions.isEmpty( ) )
			{
				IncomingAction Action = actions.poll();
				Bytearray ActionData = Action.GetAction();
				subpacket.addChar(Action.GetPID());
				subpacket.addShort((short) ActionData.size());
				subpacket.addBytearray(ActionData);
			}

			// calculate crc (we only care about the first 2 bytes though)

			//TODO check for correct Bytearray -> byte[] conversion
			Bytearray crc32 = new Bytearray(this.jGhost.getCrc32().FullCRC(subpacket.asArray()));
			crc32.resize(2);

			// finish subpacket

			packet.addBytearray(crc32);					// crc
			packet.addBytearray(subpacket);				// subpacket
		}
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_INCOMING_ACTION: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_CHAT_FROM_HOST (char fromPID, Bytearray toPIDs, char flag, Bytearray flagExtra, String message) {
		Bytearray packet = new Bytearray();

		if( !toPIDs.isEmpty( ) && !message.isEmpty( ) && message.length() < 255 )
		{
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_CHAT_FROM_HOST.toVal());	// W3GS_CHAT_FROM_HOST
			packet.addChar((char) 0);								// packet length will be assigned later
			packet.addChar((char) 0);								// packet length will be assigned later
			packet.addChar((char) toPIDs.size());					// number of receivers
			packet.addBytearray(toPIDs);							// receivers
			packet.addChar( fromPID );								// sender
			packet.addChar( flag );									// flag
			packet.addBytearray(flagExtra);							// extra flag
			packet.addString(message);								// message
			AssignLength( packet );
		} else {
			LOG.error("Invalid parameters passed to SEND_W3GS_CHAT_FROM_HOST" );
		}
			
		LOG.debug("SENT W3GS_CHAT_FROM_HOST: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_START_LAG (List<GamePlayer> players) {
		Bytearray packet = new Bytearray();

		char NumLaggers = 0;

		for (GamePlayer player : players) {
			if (player.GetLagging()) {
				NumLaggers++;
			}
		}
		
		if (NumLaggers > 0) {
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_START_LAG.toVal());	// W3GS_START_LAG
			packet.addChar((char) 0);									// packet length will be assigned later
			packet.addChar((char) 0);									// packet length will be assigned later
			packet.addChar(NumLaggers);

			for (GamePlayer player : players) {
				if (player.GetLagging()) {
					packet.addChar(player.GetPID());
					packet.addInt(TimeUtil.getTicks() - player.GetStartedLaggingTicks());
				}
			}
			
			AssignLength( packet );
		} else {
			LOG.warn("No laggers passed to SEND_W3GS_START_LAG");
		}
		
		LOG.debug("SENT W3GS_START_LAG: " + packet);
		return packet;
	}
	
	
	public Bytearray SEND_W3GS_STOP_LAG (GamePlayer player) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_STOP_LAG.toVal());	// W3GS_STOP_LAG
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar(player.GetPID());
		packet.addInt(TimeUtil.getTicks() - player.GetStartedLaggingTicks());
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_STOP_LAG: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_SEARCHGAME () {
		char[] ProductID	= { 80, 88, 51, 87 };	// "W3XP"
		char[] Version		= { 23,  0,  0,  0 };	// 1.23
		char[] Unknown		= {  0,  0,  0,  0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_SEARCHGAME.toVal());	// W3GS_SEARCHGAME
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addCharArray(ProductID);								// Product ID
		packet.addCharArray(Version);								// Version
		packet.addCharArray(Unknown);								// ???
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_SEARCHGAME: " + packet);
		return packet;
	}
	
	
	public Bytearray SEND_W3GS_GAMEINFO (Bytearray mapGameType, Bytearray mapFlags, Bytearray mapWidth, Bytearray mapHeight, String gameName, String hostName, int upTime, String mapPath, Bytearray mapCRC, int slotsTotal, int slotsOpen, short port, int hostCounter) {
		char[] ProductID	= { 80, 88, 51, 87 };	// "W3XP"
		char[] Version		= { 23,  0,  0,  0 };	// 1.23
		char[] Unknown1		= {  1,  2,  3,  4 };
		char[] Unknown2		= {  1,  0,  0,  0 };

		Bytearray packet = new Bytearray();

		if( mapGameType.size( ) == 4 && mapFlags.size( ) == 4 && mapWidth.size( ) == 2 && mapHeight.size( ) == 2 && !gameName.isEmpty( ) && !hostName.isEmpty( ) && !mapPath.isEmpty( ) && mapCRC.size( ) == 4 ) {
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
			StatString = StatString.encode();

			// make the rest of the packet

			packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_GAMEINFO.toVal());		// W3GS_GAMEINFO
			packet.addChar((char) 0);									// packet length will be assigned later
			packet.addChar((char) 0);									// packet length will be assigned later
			packet.addCharArray(ProductID);								// Product ID
			packet.addCharArray(Version);								// Version
			packet.addInt(hostCounter);									// Host Counter
			packet.addCharArray(Unknown1);								// ??? (this varies wildly even between two identical games created one after another)
			packet.addString(gameName);									// Game Name
			packet.addChar((char) 0 );									// ??? (maybe game password)
			packet.addBytearray(StatString);							// Stat String
			packet.addChar((char) 0 );									// Stat String null terminator (the stat string is encoded to remove all even numbers i.e. zeros)
			packet.addInt(slotsTotal);									// Slots Total
			packet.addBytearray(mapGameType);							// Game Type
			packet.addCharArray(Unknown2);								// ???
			packet.addInt(slotsOpen);									// Slots Open
			packet.addInt(upTime);										// time since creation
			packet.addShort(port);										// port
			AssignLength(packet);
		} else {
			LOG.warn("Invalid parameters passed to SEND_W3GS_GAMEINFO" );
		}
		LOG.debug("SENT W3GS_GAMEINFO: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_CREATEGAME () {
		char[] ProductID	= { 80, 88, 51, 87 };	// "W3XP"
		char[] Version		= { 23,  0,  0,  0 };	// 1.23
		char[] HostCounter	= {  1,  0,  0,  0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_CREATEGAME.toVal());	// W3GS_CREATEGAME
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addCharArray(ProductID);								// Product ID
		packet.addCharArray(Version);								// Version
		packet.addCharArray(HostCounter);							// Host Counter
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_CREATEGAME: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_REFRESHGAME (int players, int playerSlots) {
		char[] HostCounter	= { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT );			// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_REFRESHGAME.toVal());	// W3GS_REFRESHGAME
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addCharArray(HostCounter);							// Host Counter
		packet.addInt(players);										// Players
		packet.addInt(playerSlots);									// Player Slots
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_REFRESHGAME: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_DECREATEGAME () {
		char[] HostCounter	= { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);				// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_DECREATEGAME.toVal());	// W3GS_DECREATEGAME
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addChar((char) 0);									// packet length will be assigned later
		packet.addCharArray(HostCounter);							// Host Counter
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_DECREATEGAME: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_MAPCHECK (String mapPath, Bytearray mapSize, Bytearray mapInfo, Bytearray mapCRC, Bytearray mapSHA1) {
		char[] Unknown = { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();

		if( !mapPath.isEmpty( ) && mapSize.size( ) == 4 && mapInfo.size( ) == 4 && mapCRC.size( ) == 4 && mapSHA1.size( ) == 20 )
		{
			packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
			packet.addChar(GameProtocolEnum.W3GS_MAPCHECK.toVal());	// W3GS_MAPCHECK
			packet.addChar((char) 0);								// packet length will be assigned later
			packet.addChar((char) 0);								// packet length will be assigned later
			packet.addCharArray(Unknown);							// ???
			packet.addString(mapPath);								// map path
			packet.addBytearray(mapSize);							// map size
			packet.addBytearray(mapInfo);							// map info
			packet.addBytearray(mapCRC);							// map crc
			packet.addBytearray(mapSHA1);							// map sha1
			AssignLength( packet );
		} else {
			LOG.warn("Invalid parameters passed to SEND_W3GS_MAPCHECK" );
		}
			
		LOG.debug("SENT W3GS_MAPCHECK: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_STARTDOWNLOAD (char fromPID ) {
		char[] Unknown = { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);					// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_STARTDOWNLOAD.toVal());	// W3GS_STARTDOWNLOAD
		packet.addChar((char) 0);										// packet length will be assigned later
		packet.addChar((char) 0);										// packet length will be assigned later
		packet.addCharArray(Unknown);									// ???
		packet.addChar( fromPID );										// from PID
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_STARTDOWNLOAD: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_MAPPART (char fromPID, char toPID, int start, byte[] mapData) {
		char[] Unknown = { 1, 0, 0, 0 };

		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);			// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_MAPPART.toVal());	// W3GS_MAPPART
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar((char) 0);								// packet length will be assigned later
		packet.addChar( toPID );								// to PID
		packet.addChar( fromPID );								// from PID
		packet.addCharArray(Unknown);							// ???
		packet.addInt(start);									// start position

		// calculate end position (don't send more than 1442 map bytes in one packet)

		int End = start + 1442;

		if (End > mapData.length) {
			End = mapData.length;
		}

		// calculate crc
		byte[] mapDataPart = Arrays.copyOfRange(mapData, start, End);
		Bytearray crc32 = new Bytearray(jGhost.getCrc32().FullCRC(mapDataPart));
		packet.addBytearray(crc32);

		// map data
		Bytearray Data = new Bytearray(mapDataPart);
		packet.addBytearray(Data);
		AssignLength( packet );
		
		LOG.debug("SENT W3GS_MAPPART: " + packet);
		return packet;
	}
	
	public Bytearray SEND_W3GS_INCOMING_ACTION2 (Queue<IncomingAction> actions) {
		Bytearray packet = new Bytearray();
		packet.addChar(Constants.W3GS_HEADER_CONSTANT);					// W3GS header constant
		packet.addChar(GameProtocolEnum.W3GS_INCOMING_ACTION2.toVal());	// W3GS_INCOMING_ACTION2
		packet.addChar((char) 0);										// packet length will be assigned later
		packet.addChar((char) 0 );										// packet length will be assigned later
		packet.addChar((char) 0 );										// ??? (send interval?)
		packet.addChar((char) 0 );										// ??? (send interval?)

		// create subpacket

		if (!actions.isEmpty()) {
			Bytearray subpacket = new Bytearray();

			while (!actions.isEmpty()) {
				IncomingAction Action = actions.poll();
				Bytearray ActionData = Action.GetAction( );
				subpacket.addChar(Action.GetPID());
				subpacket.addShort((short) ActionData.size());
				subpacket.addBytearray(ActionData);
			}

			// calculate crc (we only care about the first 2 bytes though)
			Bytearray crc32 = new Bytearray(this.jGhost.getCrc32().FullCRC(subpacket.asArray()));
			crc32.resize( 2 );

			// finish subpacket

			packet.addBytearray(crc32);			// crc
			packet.addBytearray(subpacket);		// subpacket
		}

		AssignLength( packet );
		
		LOG.debug("SENT W3GS_INCOMING_ACTION2: " + packet);
		return packet;
	}

	// other functions

	private boolean AssignLength (Bytearray content) {
		// insert the actual length of the content array into bytes 3 and 4 (indices 2 and 3)

		Bytearray LengthBytes;

		if( content.size( ) >= 4 && content.size( ) <= 65535 )
		{
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
	
	private Bytearray EncodeSlotInfo( List<GameSlot> slots, int randomSeed, char gameType, char playerSlots) {
		Bytearray SlotInfo = new Bytearray();
		SlotInfo.addChar((char)slots.size()); // number of slots

		for (int i = 0; i < slots.size( ); i++ ) {
			SlotInfo.addBytearray(slots.get(i).GetByteArray());
		}

		SlotInfo.addInt(randomSeed);	// random seed
		SlotInfo.addChar(gameType);		// GameType (seems to be 0 for regular game, 3 for custom game)
		SlotInfo.addChar(playerSlots);	// number of player slots (non observer)
		return SlotInfo;
	}
}

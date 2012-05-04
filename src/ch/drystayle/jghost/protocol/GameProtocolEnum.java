package ch.drystayle.jghost.protocol;

public enum GameProtocolEnum {
	
	//---- Static
	
	W3GS_PING_FROM_HOST((char) 1), 		// 0x01
	W3GS_SLOTINFOJOIN((char) 4),		// 0x04
	W3GS_REJECTJOIN((char) 5),			// 0x05
	W3GS_PLAYERINFO((char) 6),			// 0x06
	W3GS_PLAYERLEAVE_OTHERS((char) 7),	// 0x07
	W3GS_GAMELOADED_OTHERS((char) 8),	// 0x08
	W3GS_SLOTINFO((char) 9),			// 0x09
	W3GS_COUNTDOWN_START((char) 10),	// 0x0A
	W3GS_COUNTDOWN_END((char) 11),		// 0x0B
	W3GS_INCOMING_ACTION((char) 12),	// 0x0C
	W3GS_CHAT_FROM_HOST((char) 15),		// 0x0F
	W3GS_START_LAG((char) 16),			// 0x10
	W3GS_STOP_LAG((char) 17),			// 0x11
	W3GS_HOST_KICK_PLAYER((char) 28),	// 0x1C
	W3GS_REQJOIN((char) 30),			// 0x1E
	W3GS_LEAVEGAME((char) 33),			// 0x21
	W3GS_GAMELOADED_SELF((char) 35),	// 0x23
	W3GS_OUTGOING_ACTION((char) 38),	// 0x26
	W3GS_OUTGOING_KEEPALIVE((char) 39),	// 0x27
	W3GS_CHAT_TO_HOST((char) 40),		// 0x28
	W3GS_DROPREQ((char) 41),			// 0x29
	W3GS_SEARCHGAME((char) 47),			// 0x2F (UDP/LAN)
	W3GS_GAMEINFO((char) 48),			// 0x30 (UDP/LAN)
	W3GS_CREATEGAME((char) 49),			// 0x31 (UDP/LAN)
	W3GS_REFRESHGAME((char) 50),		// 0x32 (UDP/LAN)
	W3GS_DECREATEGAME((char) 51),		// 0x33 (UDP/LAN)
	W3GS_CHAT_OTHERS((char) 52),		// 0x34
	W3GS_PING_FROM_OTHERS((char) 53),	// 0x35
	W3GS_PONG_TO_OTHERS((char) 54),		// 0x36
	W3GS_MAPCHECK((char) 61),			// 0x3D
	W3GS_STARTDOWNLOAD((char) 63),		// 0x3F
	W3GS_MAPSIZE((char) 66),			// 0x42
	W3GS_MAPPART((char) 67),			// 0x43
	W3GS_MAPPARTOK((char) 68),			// 0x44
	W3GS_MAPPARTNOTOK((char) 69),		// 0x45 - just a guess, received this packet after forgetting to send a crc in W3GS_MAPPART (f7 45 0a 00 01 02 01 00 00 00)
	W3GS_PONG_TO_HOST((char) 70),		// 0x46
	W3GS_INCOMING_ACTION2((char) 72),	// 0x48 - received this packet when there are too many actions to fit in W3GS_INCOMING_ACTION
	;
	
	public static GameProtocolEnum forVal (char val) {
		GameProtocolEnum[] values = GameProtocolEnum.values();
		for (GameProtocolEnum value : values) {
			if (value.toVal() == val) {
				return value;
			}
		}
		
		return null;
	}
	
	//---- State
	
	private char val;
	
	//---- Constructors
	
	private GameProtocolEnum (char val) {
		this.val = val;
	}
	
	public char toVal () {
		return this.val;
	}
	
};

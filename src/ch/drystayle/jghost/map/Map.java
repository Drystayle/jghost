package ch.drystayle.jghost.map;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.game.GameSlot;
import ch.drystayle.jghost.util.BAISUtil;
import ch.drystayle.jghost.util.FileUtil;
import ch.drystayle.jghost.util.JniUtil;

public class Map {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(Map.class);
	
	//---- State
	
	private JGhost jGhost;
	private boolean m_Valid;
	private String m_CFGFile;
	private String m_MapPath; // config value: map path
	private Bytearray m_MapSize; // config value: map size (4 bytes)
	private Bytearray m_MapInfo; // config value: map info (4 bytes)
	private Bytearray m_MapCRC; // config value: map crc (4 bytes)
	private Bytearray m_MapSHA1; // config value: map sha1 (20 bytes)
	private char m_MapSpeed;
	private char m_MapVisibility;
	private char m_MapObservers;
	private char m_MapFlags;
	private char m_MapGameType;
	private Bytearray m_MapWidth; // config value: map width (2 bytes)
	private Bytearray m_MapHeight; // config value: map height (2 bytes)
	private String m_MapType; // config value: map type (for stats class)
	private String m_MapMatchMakingCategory; // config value: map matchmaking category (for matchmaking)
	private String m_MapStatsW3MMDCategory; // config value: map stats w3mmd category (for saving w3mmd stats)
	private String m_MapLocalPath; // config value: map local path
	private String m_MapData; // the map data itself, for sending the map to players
	private byte[] m_byteMapData;
	private int m_MapNumPlayers;
	private int m_MapNumTeams;
	private List<GameSlot> m_Slots;

	//---- Constructors
	
	public Map( JGhost jGhost ) {
		this.jGhost = jGhost;
		m_Slots = new ArrayList<GameSlot>();
		m_Valid = true;
		m_MapPath = "Maps\\FrozenThrone\\(12)EmeraldGardens.w3x";
		m_MapSize = Bytearray.fromStringNumbers("174 221 4 0");
		m_MapInfo = Bytearray.fromStringNumbers("251 57 68 98");
		m_MapCRC = Bytearray.fromStringNumbers("112 185 65 97");
		m_MapSHA1 = Bytearray.fromStringNumbers("187 28 143 4 97 223 210 52 218 28 95 52 217 203 121 202 24 120 59 213");
		m_MapSpeed = Constants.MAPSPEED_FAST;
		m_MapVisibility = Constants.MAPVIS_DEFAULT;
		m_MapObservers = Constants.MAPOBS_NONE;
		m_MapFlags = Constants.MAPFLAG_TEAMSTOGETHER | Constants.MAPFLAG_FIXEDTEAMS;
		m_MapGameType = 9;
		m_MapWidth = Bytearray.fromStringNumbers("172 0");
		m_MapHeight = Bytearray.fromStringNumbers("172 0");
		m_MapNumPlayers = 12;
		m_MapNumTeams = 12;
		m_MapType = "";
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 0,(char) 0,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 1,(char) 1,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 2,(char) 2,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 3,(char) 3,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 4,(char) 4,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 5,(char) 5,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 6,(char) 6,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 7,(char) 7,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 8,(char) 8,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 9,(char) 9,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 10,(char) 10,(char) Constants.SLOTRACE_RANDOM));
		m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 11,(char) 11,(char) Constants.SLOTRACE_RANDOM));
	}

	public Map( JGhost jGhost, Properties cfg, String nCFGFile) {
		this.jGhost = jGhost;
		Load(cfg, nCFGFile);
	}
	
	public boolean GetValid() { return m_Valid; }
	
	public String GetCFGFile() { return m_CFGFile; }
	
	public String GetMapPath() { return m_MapPath; }
	
	public Bytearray GetMapSize() { return m_MapSize; }
	
	public Bytearray GetMapInfo() { return m_MapInfo; }
	
	public Bytearray GetMapCRC() { return m_MapCRC; }
	
	public Bytearray GetMapSHA1() { return m_MapSHA1; }
	
	public char GetMapSpeed() { return m_MapSpeed; }
	
	public char GetMapVisibility() { return m_MapVisibility; }
	
	public char GetMapObservers() { return m_MapObservers; }
	
	public char GetMapFlags() { return m_MapFlags; }
	
	public Bytearray GetMapGameFlags() {
		/*

		Speed: (mask 0x00000003) cannot be combined
			0x00000000 - Slow game speed
			0x00000001 - Normal game speed
			0x00000002 - Fast game speed
		Visibility: (mask 0x00000F00) cannot be combined
			0x00000100 - Hide terrain
			0x00000200 - Map explored
			0x00000400 - Always visible (no fog of war)
			0x00000800 - Default
		Observers/Referees: (mask 0x40003000) cannot be combined
			0x00000000 - No Observers
			0x00002000 - Observers on Defeat
			0x00003000 - Additional players as observer allowed
			0x40000000 - Referees
		Teams/Units/Hero/Race: (mask 0x07064000) can be combined
			0x00004000 - Teams Together (team members are placed at neighbored starting locations)
			0x00060000 - Fixed teams
			0x01000000 - Unit share
			0x02000000 - Random hero
			0x04000000 - Random races

		*/
		
		int GameFlags = 0;

		// speed

		if( m_MapSpeed == Constants.MAPSPEED_SLOW )
			GameFlags = 0x00000000;
		else if( m_MapSpeed == Constants.MAPSPEED_NORMAL )
			GameFlags = 0x00000001;
		else
			GameFlags = 0x00000002;

		// visibility

		if( m_MapVisibility == Constants.MAPVIS_HIDETERRAIN )
			GameFlags |= 0x00000100;
		else if( m_MapVisibility == Constants.MAPVIS_EXPLORED )
			GameFlags |= 0x00000200;
		else if( m_MapVisibility == Constants.MAPVIS_ALWAYSVISIBLE )
			GameFlags |= 0x00000400;
		else
			GameFlags |= 0x00000800;

		// observers

		if( m_MapObservers == Constants.MAPOBS_ONDEFEAT )
			GameFlags |= 0x00002000;
		else if( m_MapObservers == Constants.MAPOBS_ALLOWED )
			GameFlags |= 0x00003000;
		else if( m_MapObservers == Constants.MAPOBS_REFEREES )
			GameFlags |= 0x40000000;

		// teams/units/hero/race

		if((m_MapFlags & Constants.MAPFLAG_TEAMSTOGETHER) != 0)
			GameFlags |= 0x00004000;
		if((m_MapFlags & Constants.MAPFLAG_FIXEDTEAMS) != 0)
			GameFlags |= 0x00060000;
		if((m_MapFlags & Constants.MAPFLAG_UNITSHARE) != 0)
			GameFlags |= 0x01000000;
		if((m_MapFlags & Constants.MAPFLAG_RANDOMHERO) != 0)
			GameFlags |= 0x02000000;
		if((m_MapFlags & Constants.MAPFLAG_RANDOMRACES) != 0)
			GameFlags |= 0x04000000;

		return new Bytearray(GameFlags);
	}
	
	public char GetMapGameType() { return m_MapGameType; }
	
	public Bytearray GetMapWidth() { return m_MapWidth; }
	
	public Bytearray GetMapHeight() { return m_MapHeight; }
	
	public String GetMapType() { return m_MapType; }
	
	public String GetMapMatchMakingCategory() { return m_MapMatchMakingCategory; }
		
	public String GetMapStatsW3MMDCategory() { return m_MapStatsW3MMDCategory; }
		
	public String GetMapLocalPath() { return m_MapLocalPath; }
		
	public String GetMapData() { return m_MapData; } //TODO returned string pointer to value
		
	public int GetMapNumPlayers() { return m_MapNumPlayers; }
		
	public int GetMapNumTeams() { return m_MapNumTeams; }
		
	public List<GameSlot> GetSlots() { return m_Slots; }
	
	public void Load (Properties cfg, String nCFGFile) {
		m_Valid = true;
		m_CFGFile = nCFGFile;

		// load the map data

		m_MapLocalPath = cfg.getProperty("map_localpath", "");
		m_MapData = "";

		if( !m_MapLocalPath.isEmpty( ) ) {
			String fileName = this.jGhost.getMapPath() + m_MapLocalPath;
			try {
				setByteMapData(FileUtil.readFile(fileName));
				//TODO data in byte[] change when casted to string?
				m_MapData = new String(getByteMapData());
			} catch (IOException e) {
				LOG.warn("Unable to load " + fileName);
			}
		}
			

		// load the map MPQ

		String MapMPQFileName = this.jGhost.getMapPath() + m_MapLocalPath;
		boolean MapMPQReady = false;
		long MapMPQPointer = 0L;
		try {
			MapMPQPointer = JniUtil.archive_open(MapMPQFileName);
			LOG.info("Loading MPQ file [" + MapMPQFileName + "]");
			MapMPQReady = true;
		} catch (IOException e) {
			LOG.error("Unable to load MPQ file [" + MapMPQFileName + "]");
		}
		
		Bytearray MapSize = new Bytearray();
		Bytearray MapInfo = new Bytearray();
		Bytearray MapCRC = new Bytearray();
		Bytearray MapSHA1 = new Bytearray();
		
		if (!m_MapData.isEmpty()) {
			this.jGhost.getSha().reset();
			
			// calculate map_size
			
			MapSize = new Bytearray(getByteMapData().length);
			LOG.info("Calculated map_size = " + MapSize.toFormattedString());
			
			// calculate map_info (this is actually the CRC)

			MapInfo = new Bytearray(this.jGhost.getCrc32().FullCRC(getByteMapData()));
			LOG.info("Calculated map_info = " + MapInfo.toFormattedString() );

			// calculate map_crc (this is not the CRC) and map_sha1
			// a big thank you to Strilanc for figuring the map_crc algorithm out
			byte[] CommonJ = null;
			String commonJFileName = this.jGhost.getMapCfgPath() + "common.j";
			try {
				CommonJ = FileUtil.readFile(commonJFileName );
			} catch (IOException e) {
				LOG.error("Unable to calculate map_crc/sha1 - unable to read file [" + commonJFileName + "]" );
			} 
			if (CommonJ == null || CommonJ.length == 0) {
				LOG.error("Unable to calculate map_crc/sha1 - file [" + this.jGhost.getMapCfgPath() + "common.j] is empty" );
			} else {
				byte[] BlizzardJ = null;
				try {
					BlizzardJ = FileUtil.readFile(this.jGhost.getMapCfgPath() + "blizzard.j" );
				} catch (IOException e) {
					LOG.error("Unable to calculate map_crc/sha1 - unable to read file [" + this.jGhost.getMapCfgPath() + "blizzard.j]" );
				} 
				if (BlizzardJ == null || BlizzardJ.length == 0) {
					LOG.error("Unable to calculate map_crc/sha1 - file [" + this.jGhost.getMapCfgPath() + "blizzard.j] is empty" );
				} else {
					int Val = 0;

					// update: it's possible for maps to include their own copies of common.j and/or blizzard.j
					// this code now overrides the default copies if required

					boolean OverrodeCommonJ = false;
					boolean OverrodeBlizzardJ = false;
					
					if (MapMPQReady) {
						
						// override common.j
						try {
							byte[] overridenCommonJ = JniUtil.archive_readFile(MapMPQPointer, "Scripts\\common.j");
							LOG.info("Overriding default common.j with map copy while calculating map_crc/sha1");
							OverrodeCommonJ = true;
							Val = JniUtil.crc_valXORRotateLeft(Val, overridenCommonJ);
							this.jGhost.getSha().update(overridenCommonJ);
						} catch (IOException e) {
							//nop
						}
					}
					
					if (!OverrodeCommonJ) {
						Val = JniUtil.crc_valXORRotateLeft(Val, CommonJ);
						this.jGhost.getSha().update(CommonJ);
					}
					
					if (MapMPQReady) {
						
						// override blizzard.j
						try {
							byte[] overridenBlizzardJ = JniUtil.archive_readFile(MapMPQPointer, "Scripts\\blizzard.j");
							LOG.info("Overriding default blizzard.j with map copy while calculating map_crc/sha1");
							OverrodeBlizzardJ = true;
							Val = JniUtil.crc_valXORRotateLeft(Val, overridenBlizzardJ);
							this.jGhost.getSha().update(overridenBlizzardJ);
						} catch (IOException e) {
							//nop
						}
					}
					
					if(!OverrodeBlizzardJ) {
						Val = JniUtil.crc_valXORRotateLeft(Val, BlizzardJ);
						this.jGhost.getSha().update(BlizzardJ);
					}
					
					Val = JniUtil.crc_rotl(Val, 3);
					Val = JniUtil.crc_rotl(Val ^ 0x03F1379E, 3);
					this.jGhost.getSha().update(new byte[] {(byte) 0x9E, 0x37, (byte) 0xF1, 0x03});
					
					if( MapMPQReady )
					{
						List<String> FileList = new ArrayList<String>();
						FileList.add( "war3map.j" );
						FileList.add( "scripts\\war3map.j" );
						FileList.add( "war3map.w3e" );
						FileList.add( "war3map.wpm" );
						FileList.add( "war3map.doo" );
						FileList.add( "war3map.w3u" );
						FileList.add( "war3map.w3b" );
						FileList.add( "war3map.w3d" );
						FileList.add( "war3map.w3a" );
						FileList.add( "war3map.w3q" );
						boolean FoundScript = false;

						for (String i : FileList) {
							// don't use scripts\war3map.j if we've already used war3map.j (yes, some maps have both but only war3map.j is used)

							if( FoundScript && i.equals("scripts\\war3map.j")) {
								continue;
							}
							
							try {
								byte[] fileData = JniUtil.archive_readFile(MapMPQPointer, i);
								if( "war3map.j".equals(i) || "scripts\\war3map.j".equals(i) ) {
									FoundScript = true;
								}
								Val = JniUtil.crc_rotl(JniUtil.crc_valXORRotateLeft(Val, fileData), 3);
								this.jGhost.getSha().update(fileData);
							} catch (IOException e) {
								//nop
							}
						}

						if(!FoundScript)
							LOG.equals("Couldn't find war3map.j or scripts\\war3map.j in MPQ file, calculated map_crc/sha1 is probably wrong" );

						MapCRC = new Bytearray(Val);
						LOG.info("Calculated map_crc = " + MapCRC.toFormattedString());

						this.jGhost.getSha().finalizeHash();
						MapSHA1 = new Bytearray(this.jGhost.getSha().getHash());
						LOG.info("Calculated map_sha1 = " + MapSHA1.toFormattedString());
					} else {
						LOG.error("Unable to calculate map_crc/sha1 - map MPQ file not loaded" );
					}
					
				} 
			}
			
		}

		// try to calculate map_width, map_height, map_slot<x>, map_numplayers, map_numteams
		
		Bytearray MapWidth = new Bytearray();
		Bytearray MapHeight = new Bytearray();
		int MapNumPlayers = 0;
		int MapNumTeams = 0;
		List<GameSlot> slots = new ArrayList<GameSlot>();
		
		if (getByteMapData().length != 0) {
			if (MapMPQReady) {
				try {
					byte[] fileData = JniUtil.archive_readFile(MapMPQPointer, "war3map.w3i");
					ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
					
					// war3map.w3i format found at http://www.wc3campaigns.net/tools/specs/index.html by Zepir/PitzerMike

					int FileFormat = 0;
					int RawMapWidth = 0;
					int RawMapHeight = 0;
					int RawMapFlags = 0;
					int RawMapNumPlayers = 0;
					int RawMapNumTeams = 0;
					
					FileFormat = BAISUtil.readInt(bais); // file format (18 = ROC, 25 = TFT)
					if( FileFormat == 18 || FileFormat == 25 ) {
						bais.skip(4); // number of saves 
						bais.skip(4); // editor version
						BAISUtil.readString(bais, '\0'); // map name
						BAISUtil.readString(bais, '\0'); // map author
						BAISUtil.readString(bais, '\0'); // map description
						BAISUtil.readString(bais, '\0'); // players recommended
						bais.skip(32); // camera bounds
						bais.skip(16); // camera bounds complements
						RawMapWidth = BAISUtil.readInt(bais); // map width
						RawMapHeight = BAISUtil.readInt(bais); // map height
						RawMapFlags = BAISUtil.readInt(bais); // flags
						bais.skip(1); // map main ground type
						
						if( FileFormat == 18 ) {
							bais.skip(4); // campaign background number
						} else if( FileFormat == 25 ) {
							bais.skip(4); // loading screen background number
							BAISUtil.readString(bais, '\0'); // path of custom loading screen model
						}
						
						BAISUtil.readString(bais, '\0' ); // map loading screen text
						BAISUtil.readString(bais, '\0' ); // map loading screen title
						BAISUtil.readString(bais, '\0' ); // map loading screen subtitle

						if ( FileFormat == 18 ) {
							bais.skip(4); // map loading screen number
						} else if( FileFormat == 25 ) {
							bais.skip(4); // used game data set
							BAISUtil.readString(bais, '\0' ); // prologue screen path
						}
						
						BAISUtil.readString(bais, '\0' ); // prologue screen text
						BAISUtil.readString(bais, '\0' ); // prologue screen title
						BAISUtil.readString(bais, '\0' ); // prologue screen subtitle
						
						if ( FileFormat == 25) {
							bais.skip(4); // uses terrain fog
							bais.skip(4); // fog start z height
							bais.skip(4); // fog end z height
							bais.skip(4); // fog density
							bais.skip(1); // fog red value
							bais.skip(1); // fog green value
							bais.skip(1); // fog blue value
							bais.skip(1); // fog alpha value
							bais.skip(4); // global weather id
							BAISUtil.readString(bais, '\0' ); // custom sound environment
							bais.skip(1); // tileset id of the used custom light environment
							bais.skip(1); // custom water tinting red value
							bais.skip(1); // custom water tinting green value
							bais.skip(1); // custom water tinting blue value
							bais.skip(1); // custom water tinting alpha value
						}
						
						RawMapNumPlayers = BAISUtil.readInt(bais); // number of players
						
						for (int i = 0; i < 10; i++) {
							GameSlot slot = new GameSlot((char) 0,(char)  255,(char)  Constants.SLOTSTATUS_OPEN,(char)  0,(char)  0,(char)  1,(char)  Constants.SLOTRACE_RANDOM);
							int colour;
							int status;
							int race;
							
							colour = BAISUtil.readInt(bais); // colour
							slot.SetColour((char) colour);
							
							status = BAISUtil.readInt(bais); // status
							if (status == 1) {
								slot.SetSlotStatus((char) Constants.SLOTSTATUS_OPEN);
							} else if (status == 2) {
								slot.SetSlotStatus((char) Constants.SLOTSTATUS_OCCUPIED);
								slot.SetComputer((char) 1);
								slot.SetComputerType((char) Constants.SLOTCOMP_NORMAL);
							} else {
								slot.SetSlotStatus((char) Constants.SLOTSTATUS_CLOSED);
							}
							
							race = BAISUtil.readInt(bais); // race
							if( race == 1 ) {
								slot.SetRace((char) Constants.SLOTRACE_HUMAN);
							} else if( race == 2 ) {
								slot.SetRace((char) Constants.SLOTRACE_ORC);
							} else if( race == 3 ) {
								slot.SetRace((char) Constants.SLOTRACE_UNDEAD);
							} else if( race == 4 ) {
								slot.SetRace((char) Constants.SLOTRACE_NIGHTELF);
							} else {
								slot.SetRace((char) Constants.SLOTRACE_RANDOM);
							}
							
							bais.skip(4); // fixed start position
							BAISUtil.readString(bais, '\0'); // player name
							bais.skip(4); // start position x
							bais.skip(4); // start position y
							bais.skip(4); // ally low priorities
							bais.skip(4); // ally high prioritiess
							
							slots.add(slot);
						}
						
						RawMapNumTeams = BAISUtil.readInt(bais);
						
						for (int i = 0; i < RawMapNumTeams; i ++) {
							int Flags;
							int PlayerMask;

							Flags = BAISUtil.readInt(bais); // flags
							PlayerMask = BAISUtil.readInt(bais); // player mask

							for (int j =  0; j <  12; j++){
								if ((PlayerMask & 1) != 0) {
									for (GameSlot s : slots) {
										if (s.GetColour() == j) {
											s.SetTeam((char) i);
										}
									}
								}

								PlayerMask >>= 1;
							}

							BAISUtil.readString(bais, '\0'); // team name
						}
					}
					
					MapWidth = new Bytearray((short) RawMapWidth);
					LOG.info("Calculated map_width = " + MapWidth.toFormattedString());
					MapHeight = new Bytearray((short) RawMapHeight);
					LOG.info("Calculated map_height = " + MapHeight.toFormattedString());
					MapNumPlayers = RawMapNumPlayers;
					LOG.info("Calculated map_numplayers = " + MapNumPlayers);
					MapNumTeams = RawMapNumTeams;
					LOG.info("Calculated map_numteams = " + MapNumTeams);
					LOG.info("Found " + slots.size() + " slots" );
					
					// if it's a melee map...

					if((RawMapFlags & 4) != 0)
					{
						LOG.info("Found melee map, initializing slots and setting map_numteams = map_numplayers");

						// give each slot a different team and set the race to random

						int Team = 0;

						for (GameSlot s : slots) {
							s.SetTeam((char) Team++);
							s.SetRace((char) Constants.SLOTRACE_RANDOM);
						}

						// and set numteams = numplayers because numteams doesn't seem to be a meaningful value in melee maps

						MapNumTeams = MapNumPlayers;
					}
				} catch (FileNotFoundException e) {
					LOG.error("Unable to calculate map_width, map_height, map_slot<x>, map_numplayers, map_numteams - couldn't find war3map.w3i in MPQ file" );
				} catch (IOException e) {
					LOG.error("Unable to calculate map_width, map_height, map_slot<x>, map_numplayers, map_numteams - unable to extract war3map.w3i from MPQ file" );
				}
			} else {
				LOG.error("Unable to calculate map_width, map_height, map_slot<x>, map_numplayers, map_numteams - map MPQ file not loaded" );
			}
		} else {
			LOG.error("No map data available, using config file for map_width, map_height, map_slot<x>, map_numplayers, map_numteams" );
		}
		
		// close the map MPQ
		if( MapMPQReady ) {
			JniUtil.archive_close(MapMPQPointer);
		}

		m_MapPath = cfg.getProperty("map_path", "");
		
		if (MapSize.isEmpty()) {
			MapSize = Bytearray.fromStringNumbers(cfg.getProperty("map_size", "0"));
		} else if (cfg.containsKey("map_size")) {
			MapSize = Bytearray.fromStringNumbers(cfg.getProperty("map_size", "0"));
			LOG.info("Overriding calculated map_size with config value map_size = " + MapSize.toFormattedString());
		}
		m_MapSize = MapSize;

		if (MapInfo.isEmpty()) {
			MapInfo = Bytearray.fromStringNumbers(cfg.getProperty("map_info", "0"));
		} else if (cfg.containsKey("map_info")) {
			MapInfo = Bytearray.fromStringNumbers(cfg.getProperty("map_info", "0"));
			LOG.info("Overriding calculated map_info with config value map_info = " + MapInfo.toFormattedString());
		}
		m_MapInfo = MapInfo;

		if (MapCRC.isEmpty()) {
			MapCRC = Bytearray.fromStringNumbers(cfg.getProperty("map_crc", "0"));
		} else if (cfg.containsKey("map_crc")) {
			MapCRC = Bytearray.fromStringNumbers(cfg.getProperty("map_crc", "0"));
			LOG.info("Overriding calculated map_crc with config value map_crc = " + MapCRC.toFormattedString() );
		}
		m_MapCRC = MapCRC;

		if (MapSHA1.isEmpty()) {
			MapSHA1 = Bytearray.fromStringNumbers(cfg.getProperty("map_sha1", "0"));
		} else if (cfg.containsKey("map_sha1")) {
			MapSHA1 = Bytearray.fromStringNumbers(cfg.getProperty("map_sha1", "0"));
			LOG.info("Overriding calculated map_sha1 with config value map_sha1 = " + MapSHA1.toFormattedString());
		}
		m_MapSHA1 = MapSHA1;
		
		m_MapSpeed = (char) (int) new Integer(cfg.getProperty("map_speed", new Integer(Constants.MAPSPEED_FAST).toString()));
		m_MapVisibility = (char) (int) new Integer(cfg.getProperty("map_visibility", new Integer(Constants.MAPSPEED_FAST).toString()));
		m_MapObservers = (char) (int) new Integer(cfg.getProperty("map_observers", new Integer(Constants.MAPSPEED_FAST).toString()));
		m_MapFlags = (char) (int) new Integer(cfg.getProperty("map_flags", new Integer(Constants.MAPSPEED_FAST).toString()));
		m_MapGameType = (char) (int) new Integer(cfg.getProperty("map_gametype", new Integer(Constants.MAPSPEED_FAST).toString()));
		
		if(MapWidth.isEmpty()) {
			MapWidth = Bytearray.fromStringNumbers(cfg.getProperty("map_width", "0"));
		} else if (cfg.containsKey("map_width")) {
			MapWidth = Bytearray.fromStringNumbers(cfg.getProperty("map_width", "0"));
			LOG.info("Overriding calculated map_width with config value map_width = " + MapWidth.toFormattedString());
		}
		m_MapWidth = MapWidth;

		if (MapHeight.isEmpty()) {
			MapHeight = Bytearray.fromStringNumbers(cfg.getProperty("map_height", "0"));
		} else if (cfg.containsKey("map_height")) {
			MapHeight = Bytearray.fromStringNumbers(cfg.getProperty("map_height", "0"));
			LOG.info("Overriding calculated map_height with config value map_height = " + MapHeight.toFormattedString());
		}
		m_MapHeight = MapHeight;
		
		m_MapType = cfg.getProperty("map_type", "");
		m_MapMatchMakingCategory = cfg.getProperty("map_matchmakingcategory", "");
		m_MapStatsW3MMDCategory = cfg.getProperty("map_statsw3mmdcategory", "");

		if (MapNumPlayers == 0) {
			MapNumPlayers = new Integer(cfg.getProperty("map_numplayers"));
		} else if (cfg.containsKey("map_numplayers")) {
			MapNumPlayers = new Integer(cfg.getProperty("map_numplayers"));
			LOG.info("Overriding calculated map_numplayers with config value map_numplayers = " + MapNumPlayers);
		}
		m_MapNumPlayers = MapNumPlayers;

		if (MapNumTeams == 0) {
			MapNumTeams = new Integer(cfg.getProperty("map_numteams"));
		} else if (cfg.containsKey("map_numteams")) {
			MapNumTeams = new Integer(cfg.getProperty("map_numteams"));
			LOG.info("Overriding calculated map_numteams with config value map_numteams = " + MapNumTeams);
		}
		m_MapNumTeams = MapNumTeams;

		if (slots.isEmpty()) {
			for (int Slot = 1; Slot <= 12; Slot++ ) {
				String SlotString = cfg.getProperty("map_slot" + Slot, "");

				if( SlotString.isEmpty()) {
					break;
				}
					
				slots.add(new GameSlot(Bytearray.fromStringNumbers(SlotString)));
			}
		} else if (cfg.containsKey("map_slot1")) {
			LOG.info("Overriding slots");
			slots.clear();

			for (int Slot = 1; Slot <= 12; Slot++ ) {
				String SlotString = cfg.getProperty("map_slot" + Slot, "");

				if( SlotString.isEmpty()) {
					break;
				}
					
				slots.add(new GameSlot(Bytearray.fromStringNumbers(SlotString)));
			}
		}

		m_Slots = slots;
		
		// if random races is set force every slot's race to random + fixed

		if ((m_MapFlags & Constants.MAPFLAG_RANDOMRACES) != 0) {
			LOG.info("Forcing races to random");

			for (GameSlot s : m_Slots) {
				s.SetRace((char) (Constants.SLOTRACE_RANDOM | Constants.SLOTRACE_FIXED));
			}
		}

		// add observer slots

		if( m_MapObservers == Constants.MAPOBS_ALLOWED || m_MapObservers == Constants.MAPOBS_REFEREES )
		{
			LOG.info("Adding " + (12 - m_Slots.size()) + " observer slots");

			while (m_Slots.size() < 12) {
				m_Slots.add(new GameSlot((char) 0,(char) 255,(char) Constants.SLOTSTATUS_OPEN,(char) 0,(char) 12,(char) 12,(char) Constants.SLOTRACE_RANDOM ));
			}
		}

		CheckValid();
	}
		
	public void CheckValid () {
		if( m_MapPath == null || m_MapPath.isEmpty( ) )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_path detected" );
		} else if( m_MapPath.charAt(0) == '\\' ) {
			LOG.error( "[MAP] warning - map_path starts with '\\', any replays saved by GHost++ will not be playable in Warcraft III" );
		}
			
		if( m_MapSize == null || m_MapSize.size() != 4 ) {
			m_Valid = false;
			LOG.error( "[MAP] invalid map_size detected" );
		}

		if( m_MapInfo == null || m_MapInfo.size() != 4 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_info detected" );
		}

		if( m_MapCRC == null || m_MapCRC.size() != 4 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_crc detected" );
		}

		if( m_MapSHA1 == null || m_MapSHA1.size() != 20 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_sha1 detected" );
		}

		if( m_MapSpeed != Constants.MAPSPEED_SLOW && m_MapSpeed != Constants.MAPSPEED_NORMAL && m_MapSpeed != Constants.MAPSPEED_FAST )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_speed detected" );
		}

		if( m_MapVisibility != Constants.MAPVIS_HIDETERRAIN && m_MapVisibility != Constants.MAPVIS_EXPLORED && m_MapVisibility != Constants.MAPVIS_ALWAYSVISIBLE && m_MapVisibility != Constants.MAPVIS_DEFAULT )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_visibility detected" );
		}

		if( m_MapObservers != Constants.MAPOBS_NONE && m_MapObservers != Constants.MAPOBS_ONDEFEAT && m_MapObservers != Constants.MAPOBS_ALLOWED && m_MapObservers != Constants.MAPOBS_REFEREES )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_observers detected" );
		}

		// todotodo: m_MapFlags

		if( m_MapGameType != 1 && m_MapGameType != 2 && m_MapGameType != 9 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_gametype detected" );
		}

		if( m_MapWidth == null || m_MapWidth.size( ) != 2 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_width detected" );
		}

		if( m_MapHeight == null || m_MapHeight.size( ) != 2 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_height detected" );
		}

		if( m_MapNumPlayers == 0 || m_MapNumPlayers > 12 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_numplayers detected" );
		}

		if( m_MapNumTeams == 0 || m_MapNumTeams > 12 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_numteams detected" );
		}

		if( m_Slots == null || m_Slots.isEmpty() || m_Slots.size( ) > 12 )
		{
			m_Valid = false;
			LOG.error( "[MAP] invalid map_slot<x> detected" );
		}
	}

	public void setByteMapData(byte[] m_byteMapData) {
		this.m_byteMapData = m_byteMapData;
	}

	public byte[] getByteMapData() {
		return m_byteMapData;
	}
	
}

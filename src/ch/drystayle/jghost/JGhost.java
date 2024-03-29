package ch.drystayle.jghost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.bnet.BnetConnection;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.db.DBUtil;
import ch.drystayle.jghost.game.AdminGame;
import ch.drystayle.jghost.game.BaseGame;
import ch.drystayle.jghost.game.Game;
import ch.drystayle.jghost.i18n.IpToCountry;
import ch.drystayle.jghost.i18n.IpToCountryData;
import ch.drystayle.jghost.i18n.MessageFactory;
import ch.drystayle.jghost.i18n.Messages;
import ch.drystayle.jghost.map.Map;
import ch.drystayle.jghost.net.GhostUdpSocket;
import ch.drystayle.jghost.util.CRC32;
import ch.drystayle.jghost.util.JniUtil;
import ch.drystayle.jghost.util.SHA1;

public class JGhost {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(JGhost.class);
	
	private static final String PROPERTIES_FILE_NAME = "ghost.properties";

	private static final String BNCSUTIL_NATIVE_LIB_NAME = "BNCSutil";
	private static final String STORM_NATIVE_LIB_NAME = "storm";
	private static final String JGHOSTUTIL_NATIVE_LIB_NAME = "jghost_util";
	private static final String ZLIB_NATIVE_LIB_NAME = "zlib1";
	
	private static final String WAR_3_PATCH_FILE_NAME = "War3Patch.mpq";
	
	private static final String BLIZZARD_J_PATH = "Scripts\\blizzard.j";
	private static final String BLIZZARD_J_FILE_NAME = "blizzard.j";
	private static final String COMMON_J_PATH = "Scripts\\common.j";
	private static final String COMMON_J_FILE_NAME = "common.j";
	
	private static final String IP_TO_COUNTRY_FILE_NAME = "ip-to-country.csv";
	
	public static void main(String[] args) {
		Properties ghostProperties = new Properties();
		
		LOG.info(Messages.LOAD_NATIVE_LIB);
		
		System.loadLibrary(ZLIB_NATIVE_LIB_NAME);
		System.loadLibrary(BNCSUTIL_NATIVE_LIB_NAME);
		System.loadLibrary(STORM_NATIVE_LIB_NAME);
		System.loadLibrary(JGHOSTUTIL_NATIVE_LIB_NAME);
		
		LOG.info(Messages.LOAD_CONFIG);
		
		try {
			ghostProperties.load(new FileInputStream(PROPERTIES_FILE_NAME));
		} catch (FileNotFoundException e) {
			LOG.fatal(Messages.FILE_NOT_FOUND.createMessage(PROPERTIES_FILE_NAME), e);
			return;
		} catch (IOException e) {
			LOG.fatal(Messages.FILE_NOT_OPENED.createMessage(PROPERTIES_FILE_NAME), e);
			return;
		}
		
		LOG.info(Messages.INIT_GHOST);
		
		JGhost ghost = new JGhost(ghostProperties);
		
		//Initialize Database
		DBUtil.getSessionFactory();
		
		while (true) {
			// block for 50ms on all sockets - if you intend to perform any timed actions more frequently you should change this
			// that said it's likely we'll loop more often than this due to there being data waiting on one of the sockets but there aren't any guarantees

			if (ghost.update(50000)) {
				break;
			}
		}
		
		// shutdown ghost
		
		LOG.info("Shutting down ghost");
	}
	
	//---- State

	private Properties ghostProperties;
	private String warcraft3Path;
	private String bindAddress;
	private short hostPort;
	private String mapPath;
	private String mapCfgPath;
	private String defaultMap;
	private boolean spoofChecks;
	private boolean pingDuringDownloads;
	private String virtualHostName;
	private int hostCounter;
	private int latency;
	private int synclimit;
	private boolean refreshMessages;
	private boolean autoSave;
	private boolean autoLock;
	private boolean checkMultipleIPUsage;
	private boolean hideIPAddresses;
	private char commandTrigger;
	private int allowDownloads;
	private boolean lcPings;
	private int autoKickPing;
	private int maxDownloaders;
	private int maxDownloadSpeed;
	private int lobbyTimeLimit;
	
	private boolean adminGameCreate;
	private short adminGamePort;
	private String adminGamePassword;
	private Map adminMap;
	private AdminGame adminGame;
	
	private BaseGame currentGame;
	private List<BaseGame> games;
	
	private CRC32 crc32;
	private SHA1 sha;
	private Map map;
	private IpToCountry ipToCountry;
	
	private GhostUdpSocket udpSocket;
	
	private List<BnetConnection> bnetConnections;
	
	private boolean exiting;
	
	//---- Constructors
	
	public JGhost (Properties ghostProperties) {
		
		this.setBnetConnections(new ArrayList<BnetConnection>());
		this.setGames(new ArrayList<BaseGame>());
		this.setHostCounter(1);
		this.setExiting(false);
		this.setUdpSocket(new GhostUdpSocket());
		
		this.ghostProperties = ghostProperties;
		this.setWarcraft3Path(this.ghostProperties.getProperty("bot_war3path", "C:\\Program Files\\Warcraft III\\"));
		this.setBindAddress(this.ghostProperties.getProperty("bot_bindaddress"));
		this.setHostPort(Short.parseShort(this.ghostProperties.getProperty("bot_hostport", "6112")));
		this.setMapPath(this.ghostProperties.getProperty("bot_mappath"));
		this.setMapCfgPath(this.ghostProperties.getProperty("bot_mapcfgpath"));
		this.setDefaultMap(this.ghostProperties.getProperty("bot_defaultmap"));
		this.setSpoofChecks(this.ghostProperties.getProperty("bot_spoofchecks", "0").equals("0") ? false : true);
		this.setPingDuringDownloads(this.ghostProperties.getProperty("bot_pingduringdownloads","0").equals("0") ? false : true);
		this.setVirtualHostName(this.ghostProperties.getProperty("bot_virtualhostname","|cFF4080C0GHost"));
		this.setLatency(Integer.parseInt(this.ghostProperties.getProperty("bot_latency","100")));
		this.setSynclimit(Integer.parseInt(this.ghostProperties.getProperty("bot_synclimit","50")));
		this.setRefreshMessages(this.ghostProperties.getProperty("bot_refreshmessages","0").equals("0") ? false : true);
		this.setAutoSave(this.ghostProperties.getProperty("bot_autosave","0").equals("0") ? false : true);
		this.setAutoLock(this.ghostProperties.getProperty("bot_autolock","0").equals("0") ? false : true);
		this.setCheckMultipleIPUsage(this.ghostProperties.getProperty("bot_checkmultipleipusage","0").equals("0") ? false : true);
		this.setHideIPAddresses(this.ghostProperties.getProperty("bot_hideipaddresses","0").equals("0") ? false : true);
		this.setAllowDownloads(new Integer(this.ghostProperties.getProperty("bot_allowdownloads","0")));
		this.setLcPings(this.ghostProperties.getProperty("bot_lcpings","1").equals("0") ? false : true);
		this.setAutoKickPing(Integer.parseInt(this.ghostProperties.getProperty("bot_autokickping","400")));
		this.setMaxDownloaders(Integer.parseInt(this.ghostProperties.getProperty("bot_maxdownloaders", "3")));
		this.setMaxDownloadSpeed(Integer.parseInt(this.ghostProperties.getProperty("bot_maxdownloadspeed", "100")));
		this.setLobbyTimeLimit(Integer.parseInt(this.ghostProperties.getProperty("bot_lobbytimelimit", "10")));
		
		String botCommandTrigger = this.ghostProperties.getProperty("bot_commandtrigger","!");
		if (botCommandTrigger.isEmpty()) {
			this.setCommandTrigger('!');
		} else {
			this.setCommandTrigger(botCommandTrigger.charAt(0));
		}
		
		this.setAdminGameCreate((this.ghostProperties.getProperty("admingame_create", "0")).equals("0") ? false : true);
		this.setAdminGamePort(Short.parseShort(this.ghostProperties.getProperty("admingame_port","6113")));
		this.setAdminGamePassword(this.ghostProperties.getProperty("admingame_password",""));
		
		this.setSha(new SHA1());
		this.setCrc32(new CRC32());
		
		//load the battle.net connections
		String prefix;
		
		for (int i = 0; i < 20; i++) {
			prefix = "bnet_" + i + "_";
			
			String server = this.ghostProperties.getProperty(prefix + "server");
			String cdKeyRoc = this.ghostProperties.getProperty(prefix + "cdkeyroc");
			String cdKeyTft = this.ghostProperties.getProperty(prefix + "cdkeytft");
			String countryAbbrev = this.ghostProperties.getProperty(prefix + "countryabbrev", "USA");
			String country = this.ghostProperties.getProperty(prefix + "country", "United States");
			String userName = this.ghostProperties.getProperty(prefix + "username");
			String password = this.ghostProperties.getProperty(prefix + "password");
			String firstChannel = this.ghostProperties.getProperty(prefix + "firstchannel");
			String rootAdmin =  this.ghostProperties.getProperty(prefix + "rootadmin", "");
			boolean holdFriends = !this.ghostProperties.getProperty(prefix + "holdfriends", "1").equals("0");
			boolean holdClan = !this.ghostProperties.getProperty(prefix + "holdclan", "1").equals("0");
			String bnlsWardenServer = this.ghostProperties.getProperty(prefix + "bnlsserver", "");
			short bnlsWardenPort = (short) Integer.parseInt(this.ghostProperties.getProperty(prefix + "bnlsport", "9367"));
			int bnlsWardenCookie = Integer.parseInt(this.ghostProperties.getProperty(prefix + "bnlswardencookie", "0"));
			char war3Version = (char) Integer.parseInt(this.ghostProperties.getProperty(prefix + "custom_war3version", "23"));
			Bytearray exeVersion = Bytearray.fromStringNumbers(this.ghostProperties.getProperty(prefix + "custom_exeversion", ""));
			Bytearray exeVersionHash = Bytearray.fromStringNumbers(this.ghostProperties.getProperty(prefix + "custom_exeversionhash", ""));
			String passwordHashType = this.ghostProperties.getProperty(prefix + "custom_passwordhashtype", "");
			int maxMessageLength = Integer.parseInt(this.ghostProperties.getProperty(prefix + "custom_maxmessagelength", "200"));
			
			if (server == null || server.isEmpty()) {
				break;
			}
			
			if (cdKeyRoc == null || cdKeyRoc.isEmpty()) {
				LOG.error(Messages.ROC_CD_KEY_EMPTY.createMessage(prefix));
				continue;
			}
			
			if (cdKeyTft == null || cdKeyTft.isEmpty()) {
				LOG.error(Messages.TFT_CD_KEY_EMPTY.createMessage(prefix));
				continue;
			}
			
			if (userName == null || userName.isEmpty()) {
				LOG.error(Messages.USER_NAME_EMTPY.createMessage(prefix));
				continue;
			}
			
			if (password == null || password.isEmpty()) {
				LOG.error(Messages.PASSWORD_EMTPY.createMessage(prefix));
				continue;
			}
			
			LOG.info(Messages.FOUND_BNET_CONNECTION.createMessage(i, server));
			this.getBnetConnections().add(new BnetConnection(
				this, 
				server, 
				bnlsWardenServer,
				bnlsWardenPort,
				bnlsWardenCookie,
				cdKeyRoc,
				cdKeyTft,
				countryAbbrev,
				country,
				userName,
				password,
				firstChannel,
				rootAdmin,
				commandTrigger,
				holdFriends,
				holdClan,
				war3Version,
				exeVersion,
				exeVersionHash,
				passwordHashType,
				maxMessageLength
			));
		}
		
		if (this.getBnetConnections().isEmpty()) {
			LOG.warn("No battle.net connections found in config file");
		}
		
		// extract common.j and blizzard.j from War3Patch.mpq if we can
		// these two files are necessary for calculating "map_crc" when loading maps so we make sure to do it before loading the default map
		// see Map.Load() for more information

		extractScripts();
		
		//load the default map
		
		LOG.info("Load default map");
		
		Properties defaultMapProperties = new Properties();
		String defaultMapFileName = this.mapCfgPath + this.defaultMap + ".properties";
		try {
			defaultMapProperties.load(new FileInputStream(defaultMapFileName));
		} catch (FileNotFoundException e) {
			LOG.error(Messages.FILE_NOT_FOUND.createMessage(defaultMapFileName), e);
			return;
		} catch (IOException e) {
			LOG.error(Messages.FILE_NOT_OPENED.createMessage(defaultMapFileName), e);
			return;
		}
		
		this.map = new Map(this, defaultMapProperties, defaultMapFileName);
		this.adminMap = new Map(this);
		
		// load the iptocountry data
		
		loadIPToCountryData();
		
		// create the admin game
		
		if (this.adminGameCreate) {
			LOG.info("Creating admin game");
			this.adminGame = new AdminGame(this, adminMap, null, adminGamePort,(char) 0, "GHost++ Admin Game", adminGamePassword);
			
			if (adminGamePort == hostPort) {
				LOG.warn( "admingame_port and bot_hostport are set to the same value, you won't be able to host any games" );
			}
		} else {
			this.adminGame = null;
		}
		
		if (bnetConnections.isEmpty() && adminGame == null) {
			LOG.warn("No battle.net connections found and no admin game created");
		}
	}
	
	//---- Methods
	
	public void CreateGame (char gameState, boolean saveGame, String gameName, String ownerName, String creatorName, String creatorServer, boolean whisper )
	{
		/*if( !m_Enabled ) {
			for( vector<CBNET *> :: iterator i = m_BNETs.begin( ); i != m_BNETs.end( ); i++ )
			{
				if( (*i)->GetServer( ) == creatorServer )
					(*i)->QueueChatCommand( m_Language->UnableToCreateGameDisabled( gameName ), creatorName, whisper );
			}

			if( m_AdminGame )
				m_AdminGame->SendAllChat( m_Language->UnableToCreateGameDisabled( gameName ) );

			return;
		}*/

		if( gameName.length( ) > 31 ) {
			for (BnetConnection bConn : this.bnetConnections) {
				if (bConn.GetServer().equals(creatorServer)) {
					bConn.QueueChatCommand(Messages.UNABLE_TO_CREATE_GAME_NAME_TOO_LONG.createMessage(gameName), creatorName, whisper);
				}
			}
			
			if (adminGame != null) {
				adminGame.SendAllChat(MessageFactory.create(Messages.UNABLE_TO_CREATE_GAME_NAME_TOO_LONG, gameName));
			}
				
			return;
		}

		if (!map.GetValid()) {
			for (BnetConnection bConn : this.bnetConnections) {
				if (bConn.GetServer().equals(creatorServer)) {
					bConn.QueueChatCommand(Messages.UNABLE_TO_CREATE_GAME_INVALID_MAP.createMessage(gameName), creatorName, whisper);
				}
			}
			
			if (adminGame != null) {
				adminGame.SendAllChat(MessageFactory.create(Messages.UNABLE_TO_CREATE_GAME_INVALID_MAP, gameName));
			}
			return;
		}

		//TODO save game
		/*if( saveGame )
		{
			if( !m_SaveGame->GetValid( ) )
			{
				for( vector<CBNET *> :: iterator i = m_BNETs.begin( ); i != m_BNETs.end( ); i++ )
				{
					if( (*i)->GetServer( ) == creatorServer )
						(*i)->QueueChatCommand( m_Language->UnableToCreateGameInvalidSaveGame( gameName ), creatorName, whisper );
				}

				if( m_AdminGame )
					m_AdminGame->SendAllChat( m_Language->UnableToCreateGameInvalidSaveGame( gameName ) );

				return;
			}

			string MapPath1 = m_SaveGame->GetMapPath( );
			string MapPath2 = m_Map->GetMapPath( );
			transform( MapPath1.begin( ), MapPath1.end( ), MapPath1.begin( ), (int(*)(int))tolower );
			transform( MapPath2.begin( ), MapPath2.end( ), MapPath2.begin( ), (int(*)(int))tolower );

			if( MapPath1 != MapPath2 )
			{
				for( vector<CBNET *> :: iterator i = m_BNETs.begin( ); i != m_BNETs.end( ); i++ )
				{
					if( (*i)->GetServer( ) == creatorServer )
						(*i)->QueueChatCommand( m_Language->UnableToCreateGameSaveGameMapMismatch( gameName ), creatorName, whisper );
				}

				if( m_AdminGame )
					m_AdminGame->SendAllChat( m_Language->UnableToCreateGameSaveGameMapMismatch( gameName ) );

				return;
			}
		}*/

		if(currentGame != null) {
			for (BnetConnection bConn : this.bnetConnections) {
				if (bConn.GetServer().equals(creatorServer)) {
					bConn.QueueChatCommand(Messages.UNABLE_TO_CREATE_GAME_ANOTHER_GAME_IN_LOBBY.createMessage(gameName, currentGame.GetDescription()), creatorName, whisper);
				}
			}
			
			
			if (adminGame != null) {
				adminGame.SendAllChat(MessageFactory.create(Messages.UNABLE_TO_CREATE_GAME_ANOTHER_GAME_IN_LOBBY, gameName, currentGame.GetDescription()));
			}
			return;
		}

		//TODO max games
		/*if( m_Games.size( ) >= m_MaxGames )
		{
			for( vector<CBNET *> :: iterator i = m_BNETs.begin( ); i != m_BNETs.end( ); i++ )
			{
				if( (*i)->GetServer( ) == creatorServer )
					(*i)->QueueChatCommand( m_Language->UnableToCreateGameMaxGamesReached( gameName, UTIL_ToString( m_MaxGames ) ), creatorName, whisper );
			}

			if( m_AdminGame )
				m_AdminGame->SendAllChat( m_Language->UnableToCreateGameMaxGamesReached( gameName, UTIL_ToString( m_MaxGames ) ) );

			return;
		}*/

		LOG.info("[GHOST] creating game [" + gameName + "]");

		//TODO save game
		/*if( saveGame )
			m_CurrentGame = new CGame( this, m_Map, m_SaveGame, m_HostPort, gameState, gameName, ownerName, creatorName, creatorServer );
		else*/
			currentGame = new Game(this, map, null, hostPort, gameState, gameName, ownerName, creatorName, creatorServer );

		// todotodo: check if listening failed and report the error to the user

		for (BnetConnection bConn : this.bnetConnections) {
			if( whisper && bConn.GetServer().equals(creatorServer)) {
				// note that we send this whisper only on the creator server

				if (gameState == Constants.GAME_PRIVATE) {
					bConn.QueueChatCommand(Messages.CREATING_PRIVATE_GAME.createMessage(gameName, ownerName), creatorName, whisper );
				} else if (gameState == Constants.GAME_PUBLIC) {
					bConn.QueueChatCommand(Messages.CREATING_PUBLIC_GAME.createMessage(gameName, ownerName), creatorName, whisper );
				}
			} else {
				// note that we send this chat message on all other bnet servers

				if (gameState == Constants.GAME_PRIVATE) {
					bConn.QueueChatCommand(Messages.CREATING_PRIVATE_GAME.createMessage(gameName, ownerName));
				} else if ( gameState == Constants.GAME_PUBLIC) {
					bConn.QueueChatCommand(Messages.CREATING_PUBLIC_GAME.createMessage(gameName, ownerName));
				}
			}

			//TODO saveGame
			/*if( saveGame )
				(*i)->QueueGameCreate( gameState, gameName, string( ), m_Map, m_SaveGame, m_CurrentGame->GetHostCounter( ) );
			else*/
				bConn.QueueGameCreate( gameState, gameName, "", map, null, currentGame.GetHostCounter());
		}

		if (adminGame != null) {
			if( gameState == Constants.GAME_PRIVATE ) {
				adminGame.SendAllChat(MessageFactory.create(Messages.CREATING_PRIVATE_GAME, gameName, ownerName));
			} else if( gameState == Constants.GAME_PUBLIC ) {
				adminGame.SendAllChat(MessageFactory.create(Messages.CREATING_PUBLIC_GAME, gameName, ownerName));
			}
		}

		// if we're creating a private game we don't need to send any game refresh messages so we can rejoin the chat immediately
		// unfortunately this doesn't work on PVPGN servers because they consider an enterchat message to be a gameuncreate message when in a game
		// so don't rejoin the chat if we're using PVPGN

		if(gameState == Constants.GAME_PRIVATE) {
			for(BnetConnection bConn : this.bnetConnections) {
				if(bConn.GetPasswordHashType( ) != "pvpgn" ) {
					bConn.QueueEnterChat();
				}
			}
		}

		// hold friends and/or clan members

		for(BnetConnection bConn : this.bnetConnections)
		{
			if (bConn.GetHoldFriends()) {
				bConn.HoldFriends(currentGame );
			}
				
			if (bConn.GetHoldClan()) {
				bConn.HoldClan(currentGame );
			}
		}
	}
	
	public boolean update (long i) {
		//TODO: check for db errors
		
		//TODO: update callabals 
		
		//TODO: check if any sockets have data otherwise sleep i seconds
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			LOG.error("Interrupted while updating...", e);
		}
		
		
		boolean AdminExit = false;
		boolean BNETExit = false;
		
		Object fd = new Object();
		
		// update current game

		if (currentGame != null) {
			if(currentGame.Update(fd)) {
				LOG.info("deleting current game [" + currentGame.GetGameName() + "]" );
				currentGame = null;

				for (BnetConnection bConn : bnetConnections) {
					bConn.QueueEnterChat();
				}
			} else if(currentGame != null) {
				currentGame.UpdatePost( );
			}
		}

		// update admin game

		if (adminGame != null) {
			if(adminGame.Update(fd)) {
				LOG.info("[GHOST] deleting admin game");
				adminGame = null;
				AdminExit = true;
			} else if (adminGame != null) {
				adminGame.UpdatePost();
			}
		}

		// update running games

		for (BaseGame game : this.games) {
			if(game.Update(fd)) {
				LOG.info("[GHOST] deleting game [" + game.GetGameName() + "]");
				EventGameDeleted(game);
				games.remove(game);
			} else {
				game.UpdatePost();
			}
		}

		// update battle.net connections

		for (BnetConnection bConn : this.bnetConnections) {
			if (bConn.Update(fd)) {
				BNETExit = true;
			}
		}
		// autohost
		//TODO autohost
		/*if( !m_AutoHostGameName.empty( ) && !m_AutoHostMapCFG.empty( ) && m_AutoHostMaximumGames != 0 && m_AutoHostAutoStartPlayers != 0 && GetTime( ) >= m_LastAutoHostTime + 30 )
		{
			string GameName = m_AutoHostGameName + " #" + UTIL_ToString( m_HostCounter );

			// copy all the checks from CGHost :: CreateGame here because we don't want to spam the chat when there's an error
			// instead we fail silently and try again soon

			if( m_Enabled && GameName.size( ) <= 31 && !m_CurrentGame && m_Games.size( ) < m_MaxGames && m_Games.size( ) < m_AutoHostMaximumGames )
			{
				// load the autohost map config

				CConfig MapCFG;
				MapCFG.Read( m_AutoHostMapCFG );
				m_Map->Load( &MapCFG, m_AutoHostMapCFG );

				if( m_Map->GetValid( ) )
				{
					CreateGame( GAME_PUBLIC, false, GameName, string( ), m_AutoHostOwner, m_AutoHostServer, false );

					if( m_CurrentGame )
					{
						m_CurrentGame->SetAutoStartPlayers( m_AutoHostAutoStartPlayers );

						if( m_AutoHostMatchMaking )
						{
							if( !m_Map->GetMapMatchMakingCategory( ).empty( ) )
							{
								if( m_Map->GetMapGameType( ) != GAMETYPE_CUSTOM )
									CONSOLE_Print( "[GHOST] autohostmm - map_matchmakingcategory [" + m_Map->GetMapMatchMakingCategory( ) + "] found but matchmaking can only be used with custom maps, matchmaking disabled" );
								else if( m_BNETs.size( ) != 1 )
									CONSOLE_Print( "[GHOST] autohostmm - map_matchmakingcategory [" + m_Map->GetMapMatchMakingCategory( ) + "] found but matchmaking can only be used with one battle.net connection, matchmaking disabled" );
								else
								{
									CONSOLE_Print( "[GHOST] autohostmm - map_matchmakingcategory [" + m_Map->GetMapMatchMakingCategory( ) + "] found, matchmaking enabled" );

									m_CurrentGame->SetMatchMaking( true );
									m_CurrentGame->SetMinimumScore( m_AutoHostMinimumScore );
									m_CurrentGame->SetMaximumScore( m_AutoHostMaximumScore );
								}
							}
							else
								CONSOLE_Print( "[GHOST] autohostmm - map_matchmakingcategory not found, matchmaking disabled" );
						}
					}
				}
				else
				{
					CONSOLE_Print( "[GHOST] stopped auto hosting, map config file [" + m_AutoHostMapCFG + "] is invalid" );
					m_AutoHostGameName.clear( );
					m_AutoHostMapCFG.clear( );
					m_AutoHostOwner.clear( );
					m_AutoHostServer.clear( );
					m_AutoHostMaximumGames = 0;
					m_AutoHostAutoStartPlayers = 0;
					m_AutoHostMatchMaking = false;
					m_AutoHostMinimumScore = 0.0;
					m_AutoHostMaximumScore = 0.0;
				}
			}

			m_LastAutoHostTime = GetTime( );
		}*/

		return isExiting() || AdminExit || BNETExit;
	}
	
	private void loadIPToCountryData() {
		BufferedReader br = null;
		this.setIpToCountry(new IpToCountry());
		
		try {
			File ipFile = new File(IP_TO_COUNTRY_FILE_NAME);
			br = new BufferedReader(new FileReader(ipFile));
			long fileSize = ipFile.length();
			LOG.info("Started loading [" + IP_TO_COUNTRY_FILE_NAME + "]");
			int percent = 0;
			int bytesRead = 0;
			
			while (br.ready()) {
				String nextLine = br.readLine();
				bytesRead += nextLine.length() + 1; //+1 for '\n' or '\r' 
				getIpToCountry().add(new IpToCountryData(nextLine));
				
				int newPercent = (int) (((float) bytesRead / fileSize)* 100);
				if (newPercent != percent && newPercent % 10 == 0 ) {
					percent = newPercent;
					LOG.info("Iptocountry data: " + percent + "% loaded");
				}
			}
			
			LOG.info("Finished loading [" + IP_TO_COUNTRY_FILE_NAME + "]" );
		} catch (FileNotFoundException e) {
			LOG.warn("Unable to read file [" + IP_TO_COUNTRY_FILE_NAME + "], iptocountry data not loaded");
		} catch (IOException e) {
			LOG.warn("Unable to read file [" + IP_TO_COUNTRY_FILE_NAME + "], iptocountry data not loaded");
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// nop
				}
			}
		}
	}

	private void extractScripts() {
		String PatchMPQFileName = this.warcraft3Path + WAR_3_PATCH_FILE_NAME;

		LOG.info("Loading MPQ file [" + PatchMPQFileName + "]");
		
		long archivePointer;
		try {
			archivePointer = JniUtil.archive_open(PatchMPQFileName);
		} catch (IOException e) {
			LOG.error("Unable to load MPQ file [" + PatchMPQFileName + "]");
			return;
		}
		
		//extract common.j
		extractScript(archivePointer, COMMON_J_PATH, COMMON_J_FILE_NAME);
		
		//extract blizzard.j
		extractScript(archivePointer, BLIZZARD_J_PATH, BLIZZARD_J_FILE_NAME);
		
		JniUtil.archive_close(archivePointer);
	}
	
	private void extractScript (long archivePointer, String scriptPath, String extractedFileName) {
		byte[] fileContent = null;;
		try {
			fileContent = JniUtil.archive_readFile(archivePointer, scriptPath);
		} catch (FileNotFoundException e) {
			LOG.error("Couldn't find [" + scriptPath + "] in MPQ file");
		} catch (IOException e) {
			LOG.error("Unable to extract [" + scriptPath + "] from MPQ file");
		}
		
		File extractedScript = new File(this.mapCfgPath + extractedFileName);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(extractedScript);
			fos.write(fileContent);
			fos.close();
		} catch (IOException e) {
			LOG.error("Couldn't write extracted content to [" + extractedScript.getAbsolutePath() + "]", e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					//nop
				}
			}
		}
		
		LOG.info("Extracted [" + scriptPath + "] from MPQ file to [" + extractedScript.getAbsolutePath() + "]");
	}
	
	//---- Events

	public void EventBNETConnecting (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.CONNECTING_TO_BNET, bnet.GetServer()));
		}
	}
	
	public void EventBNETConnected (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.CONNECTED_TO_BNET, bnet.GetServer()));
		}
	}
	
	public void EventBNETDisconnected (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.DISCONNECTED_FROM_BNET, bnet.GetServer()));
		}
	}
	
	public void EventBNETLoggedIn (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.LOGGED_IN_TO_BNET, bnet.GetServer()));
		}
	}
	
	public void EventBNETGameRefreshed (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.BNET_GAME_HOSTING_SUCCEEDED, bnet.GetServer()));
		}
	}
	
	public void EventBNETGameRefreshFailed (BnetConnection bnet) {
		if (this.currentGame != null) {
			for (BnetConnection bConn : bnetConnections) {
				bConn.QueueChatCommand(Messages.UNABLE_TO_CREATE_GAME_TRY_ANOTHER_NAME.createMessage(currentGame.GetGameName(), bnet.GetServer()));

				if (bConn.GetServer( ) == currentGame.GetCreatorServer()) {
					bConn.QueueChatCommand(Messages.UNABLE_TO_CREATE_GAME_TRY_ANOTHER_NAME.createMessage(currentGame.GetGameName(), bnet.GetServer()), currentGame.GetCreatorName( ), true);
				}
			}
			
			if (adminGame != null) {
				adminGame.SendAllChat(MessageFactory.create(Messages.BNET_GAME_HOSTING_FAILED, bnet.GetServer()));
			}
				
			currentGame.SendAllChat(MessageFactory.create(Messages.UNABLE_TO_CREATE_GAME_TRY_ANOTHER_NAME, currentGame.GetGameName(), bnet.GetServer()));

			// we take the easy route and simply close the lobby if a refresh fails
			// it's possible at least one refresh succeeded and therefore the game is still joinable on at least one battle.net (plus on the local network) but we don't keep track of that
			// we only close the game if it has no players since we support game rehosting (via !priv and !pub in the lobby)

			if (currentGame.GetNumPlayers() == 0 ) {
				currentGame.SetExiting( true );
			}

			currentGame.SetRefreshError( true );
		}
	}
	
	public void EventBNETConnectTimedOut (BnetConnection bnet) {
		if (adminGame != null) {
			adminGame.SendAllChat(MessageFactory.create(Messages.CONNECTING_TO_BNET_TIMED_OUT, bnet.GetServer()));
		}
	}
	
	public void EventGameDeleted (BaseGame game) {
		for (BnetConnection bConn : bnetConnections) {
			bConn.QueueChatCommand(Messages.GAME_IS_OVER.createMessage(game.GetDescription()));

			if (bConn.GetServer( ) == game.GetCreatorServer()) {
				bConn.QueueChatCommand(Messages.GAME_IS_OVER.createMessage(game.GetDescription()), game.GetCreatorName(), true );
			}
		}
	}
	
	//---- Getter & Setter
	
	public void setGhostProperties(Properties ghostProperties) {
		this.ghostProperties = ghostProperties;
	}

	public Properties getGhostProperties() {
		return ghostProperties;
	}

	public void setWarcraft3Path(String warcraft3Path) {
		this.warcraft3Path = warcraft3Path;
	}

	public String getWarcraft3Path() {
		return warcraft3Path;
	}

	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public void setHostPort(short hostPort) {
		this.hostPort = hostPort;
	}

	public short getHostPort() {
		return hostPort;
	}

	public void setMapPath(String mapPath) {
		this.mapPath = mapPath;
	}

	public String getMapPath() {
		return mapPath;
	}

	public void setMapCfgPath(String mapCfgPath) {
		this.mapCfgPath = mapCfgPath;
	}

	public String getMapCfgPath() {
		return mapCfgPath;
	}

	public void setDefaultMap(String defaultMap) {
		this.defaultMap = defaultMap;
	}

	public String getDefaultMap() {
		return defaultMap;
	}

	public void setMap(Map map) {
		this.map = map;
	}

	public Map getMap() {
		return map;
	}

	public void setSha(SHA1 sha) {
		this.sha = sha;
	}

	public SHA1 getSha() {
		return sha;
	}

	public void setCrc32(CRC32 crc32) {
		this.crc32 = crc32;
	}

	public CRC32 getCrc32() {
		return crc32;
	}

	public void setAdminGameCreate(boolean adminGameCreate) {
		this.adminGameCreate = adminGameCreate;
	}

	public boolean isAdminGameCreate() {
		return adminGameCreate;
	}

	public void setAdminGamePassword(String adminGamePassword) {
		this.adminGamePassword = adminGamePassword;
	}

	public String getAdminGamePassword() {
		return adminGamePassword;
	}

	public void setAdminGamePort(short adminGamePort) {
		this.adminGamePort = adminGamePort;
	}

	public short getAdminGamePort() {
		return adminGamePort;
	}

	public void setSpoofChecks(boolean spoofChecks) {
		this.spoofChecks = spoofChecks;
	}

	public boolean isSpoofChecks() {
		return spoofChecks;
	}

	public void setPingDuringDownloads(boolean pingDuringDownloads) {
		this.pingDuringDownloads = pingDuringDownloads;
	}

	public boolean isPingDuringDownloads() {
		return pingDuringDownloads;
	}

	public void setBnetConnections(List<BnetConnection> bnetConnections) {
		this.bnetConnections = bnetConnections;
	}

	public List<BnetConnection> getBnetConnections() {
		return bnetConnections;
	}

	public void setVirtualHostName(String virtualHostName) {
		this.virtualHostName = virtualHostName;
	}

	public String getVirtualHostName() {
		return virtualHostName;
	}

	public void setHostCounter(int hostCounter) {
		this.hostCounter = hostCounter;
	}

	public int getHostCounter() {
		return hostCounter;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public int getLatency() {
		return latency;
	}

	public void setSynclimit(int synclimit) {
		this.synclimit = synclimit;
	}

	public int getSynclimit() {
		return synclimit;
	}

	public void setRefreshMessages(boolean refreshMessages) {
		this.refreshMessages = refreshMessages;
	}

	public boolean isRefreshMessages() {
		return refreshMessages;
	}

	public void setAutoSave(boolean autoSave) {
		this.autoSave = autoSave;
	}

	public boolean isAutoSave() {
		return autoSave;
	}

	public void setAutoLock(boolean autoLock) {
		this.autoLock = autoLock;
	}

	public boolean isAutoLock() {
		return autoLock;
	}

	public void setCheckMultipleIPUsage(boolean checkMultipleIPUsage) {
		this.checkMultipleIPUsage = checkMultipleIPUsage;
	}

	public boolean isCheckMultipleIPUsage() {
		return checkMultipleIPUsage;
	}

	public void setHideIPAddresses(boolean hideIPAddresses) {
		this.hideIPAddresses = hideIPAddresses;
	}

	public boolean isHideIPAddresses() {
		return hideIPAddresses;
	}

	public void setCommandTrigger(char commandTrigger) {
		this.commandTrigger = commandTrigger;
	}

	public char getCommandTrigger() {
		return commandTrigger;
	}

	public void setAllowDownloads(int allowDownloads) {
		this.allowDownloads = allowDownloads;
	}

	public int getAllowDownloads() {
		return allowDownloads;
	}

	public void setLcPings(boolean lcPings) {
		this.lcPings = lcPings;
	}

	public boolean isLcPings() {
		return lcPings;
	}

	public void setAutoKickPing(int autoKickPing) {
		this.autoKickPing = autoKickPing;
	}

	public int getAutoKickPing() {
		return autoKickPing;
	}

	public void setCurrentGame(BaseGame currentGame) {
		this.currentGame = currentGame;
	}

	public BaseGame getCurrentGame() {
		return currentGame;
	}

	public void setGames(List<BaseGame> games) {
		this.games = games;
	}

	public List<BaseGame> getGames() {
		return games;
	}

	public void setUdpSocket(GhostUdpSocket udpSocket) {
		this.udpSocket = udpSocket;
	}

	public GhostUdpSocket getUdpSocket() {
		return udpSocket;
	}

	public void setMaxDownloaders(int maxDownloaders) {
		this.maxDownloaders = maxDownloaders;
	}

	public int getMaxDownloaders() {
		return maxDownloaders;
	}

	public void setMaxDownloadSpeed(int maxDownloadSpeed) {
		this.maxDownloadSpeed = maxDownloadSpeed;
	}

	public int getMaxDownloadSpeed() {
		return maxDownloadSpeed;
	}

	public void setLobbyTimeLimit(int lobbyTimeLimit) {
		this.lobbyTimeLimit = lobbyTimeLimit;
	}

	public int getLobbyTimeLimit() {
		return lobbyTimeLimit;
	}

	public void setExiting(boolean exiting) {
		this.exiting = exiting;
	}

	public boolean isExiting() {
		return exiting;
	}

	public void setIpToCountry(IpToCountry ipToCountry) {
		this.ipToCountry = ipToCountry;
	}

	public IpToCountry getIpToCountry() {
		return ipToCountry;
	}

}

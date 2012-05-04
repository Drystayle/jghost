package ch.drystayle.jghost.game;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.i18n.MessageFactory;
import ch.drystayle.jghost.i18n.Messages;
import ch.drystayle.jghost.map.Map;
import ch.drystayle.jghost.protocol.IncomingJoinPlayer;
import ch.drystayle.jghost.util.TimeUtil;

public class AdminGame extends Game {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BaseGame.class);
	
	//---- State
	
	private String password;
	
	//---- Constructors
	
	public AdminGame(JGhost host, Map map, SaveGame saveGame, short hostPort,
			char gameState, String gameName, String password) {
		super(host, map, saveGame, hostPort, gameState, gameName, "", "", "");
		m_VirtualHostName = "|cFFC04040Admin";
		m_MuteLobby = true;
		this.password = password;
	}

	//---- Methods
	
	public boolean Update (Object fd) {
		m_LastReservedSeen = TimeUtil.getTime();
		return super.Update(fd);
	}
	
	public void SendWelcomeMessage(GamePlayer player) {
		SendChat( player, MessageFactory.create("GHost++ Admin Game                    http://forum.codelain.com/"));
		SendChat( player, MessageFactory.create("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
		SendChat( player, MessageFactory.create("Commands: addadmin, autohost, checkadmin, countadmins, deladmin"));
		SendChat( player, MessageFactory.create("Commands: disable, enable, end, exit, getgame, getgames"));
		SendChat( player, MessageFactory.create("Commands: hostsg, load, loadsg, map, password, priv, privby"));
		SendChat( player, MessageFactory.create("Commands: pub, pubby, quit, saygame, saygames, unhost"));
	}
	
	public void EventPlayerJoined(PotentialPlayer potential, IncomingJoinPlayer joinPlayer) {
		//TODO implement temp ban
		
		super.EventPlayerJoined( potential, joinPlayer );
	}
	
	public void EventPlayerBotCommand(GamePlayer player, String command, String payload) {
		
		//CBaseGame :: EventPlayerBotCommand( player, command, payload );

		//TODO implement commands
	
		String User = player.GetName();
	
		if( player.GetLoggedIn( ) || this.password.isEmpty()) {
			LOG.info( "[ADMINGAME] admin [" + User + "] sent command [" + command + "] with payload [" + payload + "]" );
	
			//
			//ADMIN COMMANDS *
			//
	
			/*//
			// !ADDADMIN
			//
	
			if( Command == "addadmin" && !Payload.empty( ) )
			{
				// extract the name and the server
				// e.g. "Varlock useast.battle.net" -> name: "Varlock", server: "useast.battle.net"
	
				string Name;
				string Server;
				stringstream SS;
				SS << Payload;
				SS >> Name;
	
				if( SS.eof( ) )
				{
					if( m_GHost->m_BNETs.size( ) == 1 )
						Server = m_GHost->m_BNETs[0]->GetServer( );
					else
						CONSOLE_Print( "[ADMINGAME] missing input #2 to addadmin command" );
				}
				else
					SS >> Server;
	
				if( !Server.empty( ) )
				{
					if( m_GHost->m_DB->AdminCheck( Server, Name ) )
						SendChat( player, m_GHost->m_Language->UserIsAlreadyAnAdmin( Server, Name ) );
					else
					{
						if( m_GHost->m_DB->AdminAdd( Server, Name ) )
							SendChat( player, m_GHost->m_Language->AddedUserToAdminDatabase( Server, Name ) );
						else
							SendChat( player, m_GHost->m_Language->ErrorAddingUserToAdminDatabase( Server, Name ) );
					}
				}
			}
	
			//
			// !AUTOHOST
			//
	
			if( Command == "autohost" )
			{
				if( Payload.empty( ) || Payload == "off" )
				{
					SendChat( player, m_GHost->m_Language->AutoHostDisabled( ) );
					m_GHost->m_AutoHostGameName.clear( );
					m_GHost->m_AutoHostMapCFG.clear( );
					m_GHost->m_AutoHostOwner.clear( );
					m_GHost->m_AutoHostServer.clear( );
					m_GHost->m_AutoHostMaximumGames = 0;
					m_GHost->m_AutoHostAutoStartPlayers = 0;
					m_GHost->m_LastAutoHostTime = GetTime( );
					m_GHost->m_AutoHostMatchMaking = false;
					m_GHost->m_AutoHostMinimumScore = 0.0;
					m_GHost->m_AutoHostMaximumScore = 0.0;
				}
				else
				{
					// extract the maximum games, auto start players, and the game name
					// e.g. "5 10 BattleShips Pro" -> maximum games: "5", auto start players: "10", game name: "BattleShips Pro"
	
					uint32_t MaximumGames;
					uint32_t AutoStartPlayers;
					string GameName;
					stringstream SS;
					SS << Payload;
					SS >> MaximumGames;
	
					if( SS.fail( ) || MaximumGames == 0 )
						CONSOLE_Print( "[ADMINGAME] bad input #1 to autohost command" );
					else
					{
						SS >> AutoStartPlayers;
	
						if( SS.fail( ) || AutoStartPlayers == 0 )
							CONSOLE_Print( "[ADMINGAME] bad input #2 to autohost command" );
						else
						{
							if( SS.eof( ) )
								CONSOLE_Print( "[ADMINGAME] missing input #3 to autohost command" );
							else
							{
								getline( SS, GameName );
								string :: size_type Start = GameName.find_first_not_of( " " );
	
								if( Start != string :: npos )
									GameName = GameName.substr( Start );
	
								SendChat( player, m_GHost->m_Language->AutoHostEnabled( ) );
								m_GHost->m_AutoHostGameName = GameName;
								m_GHost->m_AutoHostMapCFG = m_GHost->m_Map->GetCFGFile( );
								m_GHost->m_AutoHostOwner = User;
								m_GHost->m_AutoHostServer.clear( );
								m_GHost->m_AutoHostMaximumGames = MaximumGames;
								m_GHost->m_AutoHostAutoStartPlayers = AutoStartPlayers;
								m_GHost->m_LastAutoHostTime = GetTime( );
								m_GHost->m_AutoHostMatchMaking = false;
								m_GHost->m_AutoHostMinimumScore = 0.0;
								m_GHost->m_AutoHostMaximumScore = 0.0;
							}
						}
					}
				}
			}
	
			//
			// !AUTOHOSTMM
			//
	
			if( Command == "autohostmm" )
			{
				if( Payload.empty( ) || Payload == "off" )
				{
					SendChat( player, m_GHost->m_Language->AutoHostDisabled( ) );
					m_GHost->m_AutoHostGameName.clear( );
					m_GHost->m_AutoHostMapCFG.clear( );
					m_GHost->m_AutoHostOwner.clear( );
					m_GHost->m_AutoHostServer.clear( );
					m_GHost->m_AutoHostMaximumGames = 0;
					m_GHost->m_AutoHostAutoStartPlayers = 0;
					m_GHost->m_LastAutoHostTime = GetTime( );
					m_GHost->m_AutoHostMatchMaking = false;
					m_GHost->m_AutoHostMinimumScore = 0.0;
					m_GHost->m_AutoHostMaximumScore = 0.0;
				}
				else
				{
					// extract the maximum games, auto start players, and the game name
					// e.g. "5 10 800 1200 BattleShips Pro" -> maximum games: "5", auto start players: "10", minimum score: "800", maximum score: "1200", game name: "BattleShips Pro"
	
					uint32_t MaximumGames;
					uint32_t AutoStartPlayers;
					double MinimumScore;
					double MaximumScore;
					string GameName;
					stringstream SS;
					SS << Payload;
					SS >> MaximumGames;
	
					if( SS.fail( ) || MaximumGames == 0 )
						CONSOLE_Print( "[ADMINGAME] bad input #1 to autohostmm command" );
					else
					{
						SS >> AutoStartPlayers;
	
						if( SS.fail( ) || AutoStartPlayers == 0 )
							CONSOLE_Print( "[ADMINGAME] bad input #2 to autohostmm command" );
						else
						{
							SS >> MinimumScore;
	
							if( SS.fail( ) )
								CONSOLE_Print( "[ADMINGAME] bad input #3 to autohostmm command" );
							else
							{
								SS >> MaximumScore;
	
								if( SS.fail( ) )
									CONSOLE_Print( "[ADMINGAME] bad input #4 to autohostmm command" );
								else
								{
									if( SS.eof( ) )
										CONSOLE_Print( "[ADMINGAME] missing input #5 to autohostmm command" );
									else
									{
										getline( SS, GameName );
										string :: size_type Start = GameName.find_first_not_of( " " );
	
										if( Start != string :: npos )
											GameName = GameName.substr( Start );
	
										SendChat( player, m_GHost->m_Language->AutoHostEnabled( ) );
										m_GHost->m_AutoHostGameName = GameName;
										m_GHost->m_AutoHostMapCFG = m_GHost->m_Map->GetCFGFile( );
										m_GHost->m_AutoHostOwner = User;
										m_GHost->m_AutoHostServer.clear( );
										m_GHost->m_AutoHostMaximumGames = MaximumGames;
										m_GHost->m_AutoHostAutoStartPlayers = AutoStartPlayers;
										m_GHost->m_LastAutoHostTime = GetTime( );
										m_GHost->m_AutoHostMatchMaking = true;
										m_GHost->m_AutoHostMinimumScore = MinimumScore;
										m_GHost->m_AutoHostMaximumScore = MaximumScore;
									}
								}
							}
						}
					}
				}
			}
	
			//
			// !CHECKADMIN
			//
	
			if( Command == "checkadmin" && !Payload.empty( ) )
			{
				// extract the name and the server
				// e.g. "Varlock useast.battle.net" -> name: "Varlock", server: "useast.battle.net"
	
				string Name;
				string Server;
				stringstream SS;
				SS << Payload;
				SS >> Name;
	
				if( SS.eof( ) )
				{
					if( m_GHost->m_BNETs.size( ) == 1 )
						Server = m_GHost->m_BNETs[0]->GetServer( );
					else
						CONSOLE_Print( "[ADMINGAME] missing input #2 to checkadmin command" );
				}
				else
					SS >> Server;
	
				if( !Server.empty( ) )
				{
					if( m_GHost->m_DB->AdminCheck( Server, Name ) )
						SendChat( player, m_GHost->m_Language->UserIsAnAdmin( Server, Name ) );
					else
						SendChat( player, m_GHost->m_Language->UserIsNotAnAdmin( Server, Name ) );
				}
			}
	
			//
			// !COUNTADMINS
			//
	
			if( Command == "countadmins" )
			{
				string Server = Payload;
	
				if( Server.empty( ) && m_GHost->m_BNETs.size( ) == 1 )
					Server = m_GHost->m_BNETs[0]->GetServer( );
	
				if( !Server.empty( ) )
				{
					uint32_t Count = m_GHost->m_DB->AdminCount( Server );
	
					if( Count == 0 )
						SendChat( player, m_GHost->m_Language->ThereAreNoAdmins( Server ) );
					else if( Count == 1 )
						SendChat( player, m_GHost->m_Language->ThereIsAdmin( Server ) );
					else
						SendChat( player, m_GHost->m_Language->ThereAreAdmins( Server, UTIL_ToString( Count ) ) );
				}
			}
	
			//
			// !DELADMIN
			//
	
			if( Command == "deladmin" && !Payload.empty( ) )
			{
				// extract the name and the server
				// e.g. "Varlock useast.battle.net" -> name: "Varlock", server: "useast.battle.net"
	
				string Name;
				string Server;
				stringstream SS;
				SS << Payload;
				SS >> Name;
	
				if( SS.eof( ) )
				{
					if( m_GHost->m_BNETs.size( ) == 1 )
						Server = m_GHost->m_BNETs[0]->GetServer( );
					else
						CONSOLE_Print( "[ADMINGAME] missing input #2 to deladmin command" );
				}
				else
					SS >> Server;
	
				if( !Server.empty( ) )
				{
					if( !m_GHost->m_DB->AdminCheck( Server, Name ) )
						SendChat( player, m_GHost->m_Language->UserIsNotAnAdmin( Server, Name ) );
					else
					{
						if( m_GHost->m_DB->AdminRemove( Server, Name ) )
							SendChat( player, m_GHost->m_Language->DeletedUserFromAdminDatabase( Server, Name ) );
						else
							SendChat( player, m_GHost->m_Language->ErrorDeletingUserFromAdminDatabase( Server, Name ) );
					}
				}
			}
	
			//
			// !DISABLE
			//
	
			if( Command == "disable" )
			{
				SendChat( player, m_GHost->m_Language->BotDisabled( ) );
				m_GHost->m_Enabled = false;
			}
	
			//
			// !ENABLE
			//
	
			if( Command == "enable" )
			{
				SendChat( player, m_GHost->m_Language->BotEnabled( ) );
				m_GHost->m_Enabled = true;
			}
	
			//
			// !END
			//
	
			if( Command == "end" && !Payload.empty( ) )
			{
				// todotodo: what if a game ends just as you're typing this command and the numbering changes?
	
				uint32_t GameNumber = UTIL_ToUInt32( Payload ) - 1;
	
				if( GameNumber < m_GHost->m_Games.size( ) )
				{
					SendChat( player, m_GHost->m_Language->EndingGame( m_GHost->m_Games[GameNumber]->GetDescription( ) ) );
					CONSOLE_Print( "[GAME: " + m_GHost->m_Games[GameNumber]->GetGameName( ) + "] is over (admin ended game)" );
					m_GHost->m_Games[GameNumber]->StopPlayers( "was disconnected (admin ended game)" );
				}
				else
					SendChat( player, m_GHost->m_Language->GameNumberDoesntExist( Payload ) );
			}
	
			//
			// !EXIT
			// !QUIT
			//
	
			if( Command == "exit" || Command == "quit" )
			{
				if( Payload == "force" )
					m_Exiting = true;
				else
				{
					if( m_GHost->m_CurrentGame || !m_GHost->m_Games.empty( ) )
						SendChat( player, m_GHost->m_Language->AtLeastOneGameActiveUseForceToShutdown( ) );
					else
						m_Exiting = true;
				}
			}
	
			//
			// !GETGAME
			//
	
			if( Command == "getgame" && !Payload.empty( ) )
			{
				uint32_t GameNumber = UTIL_ToUInt32( Payload ) - 1;
	
				if( GameNumber < m_GHost->m_Games.size( ) )
					SendChat( player, m_GHost->m_Language->GameNumberIs( Payload, m_GHost->m_Games[GameNumber]->GetDescription( ) ) );
				else
					SendChat( player, m_GHost->m_Language->GameNumberDoesntExist( Payload ) );
			}
	
			//
			// !GETGAMES
			//
	
			if( Command == "getgames" )
			{
				if( m_GHost->m_CurrentGame )
					SendChat( player, m_GHost->m_Language->GameIsInTheLobby( m_GHost->m_CurrentGame->GetDescription( ), UTIL_ToString( m_GHost->m_Games.size( ) ), UTIL_ToString( m_GHost->m_MaxGames ) ) );
				else
					SendChat( player, m_GHost->m_Language->ThereIsNoGameInTheLobby( UTIL_ToString( m_GHost->m_Games.size( ) ), UTIL_ToString( m_GHost->m_MaxGames ) ) );
			}
	
			if( Command == "hostsg" && !Payload.empty( ) )
				m_GHost->CreateGame( GAME_PRIVATE, true, Payload, User, User, string( ), false );
	
			//
			// !LOAD (load config file)
			// !MAP
			//
	
			if( Command == "load" || Command == "map" )
			{
				if( Payload.empty( ) )
					SendChat( player, m_GHost->m_Language->CurrentlyLoadedMapCFGIs( m_GHost->m_Map->GetCFGFile( ) ) );
				else
				{
					// only load files in the current directory just to be safe
	
					if( Payload.find( "/" ) != string :: npos || Payload.find( "\\" ) != string :: npos )
						SendChat( player, m_GHost->m_Language->UnableToLoadConfigFilesOutside( ) );
					else
					{
						string File = m_GHost->m_MapCFGPath + Payload + ".cfg";
	
						if( UTIL_FileExists( File ) )
						{
							// we have to be careful here because we didn't copy the map data when creating the game (there's only one global copy)
							// therefore if we change the map data while a game is in the lobby everything will get screwed up
							// the easiest solution is to simply reject the command if a game is in the lobby
	
							if( m_GHost->m_CurrentGame )
								SendChat( player, m_GHost->m_Language->UnableToLoadConfigFileGameInLobby( ) );
							else
							{
								SendChat( player, m_GHost->m_Language->LoadingConfigFile( File ) );
								CConfig MapCFG;
								MapCFG.Read( File );
								m_GHost->m_Map->Load( &MapCFG, File );
							}
						}
						else
							SendChat( player, m_GHost->m_Language->UnableToLoadConfigFileDoesntExist( File ) );
					}
				}
			}
	
			//
			// !LOADSG
			//
	
			if( Command == "loadsg" && !Payload.empty( ) )
			{
				// only load files in the current directory just to be safe
	
				if( Payload.find( "/" ) != string :: npos || Payload.find( "\\" ) != string :: npos )
					SendChat( player, m_GHost->m_Language->UnableToLoadSaveGamesOutside( ) );
				else
				{
					string File = m_GHost->m_SaveGamePath + Payload + ".w3z";
					string FileNoPath = Payload + ".w3z";
	
					if( UTIL_FileExists( File ) )
					{
						if( m_GHost->m_CurrentGame )
							SendChat( player, m_GHost->m_Language->UnableToLoadSaveGameGameInLobby( ) );
						else
						{
							SendChat( player, m_GHost->m_Language->LoadingSaveGame( File ) );
							m_GHost->m_SaveGame->Load( File, false );
							m_GHost->m_SaveGame->ParseSaveGame( );
							m_GHost->m_SaveGame->SetFileName( File );
							m_GHost->m_SaveGame->SetFileNameNoPath( FileNoPath );
						}
					}
					else
						SendChat( player, m_GHost->m_Language->UnableToLoadSaveGameDoesntExist( File ) );
				}
			}
	
			//
			// !PRIV (host private game)
			//
	
			if( Command == "priv" && !Payload.empty( ) )
				m_GHost->CreateGame( GAME_PRIVATE, false, Payload, User, User, string( ), false );
	
			//
			// !PRIVBY (host private game by other player)
			//
	
			if( Command == "privby" && !Payload.empty( ) )
			{
				// extract the owner and the game name
				// e.g. "Varlock dota 6.54b arem ~~~" -> owner: "Varlock", game name: "dota 6.54b arem ~~~"
	
				string Owner;
				string GameName;
				string :: size_type GameNameStart = Payload.find( " " );
	
				if( GameNameStart != string :: npos )
				{
					Owner = Payload.substr( 0, GameNameStart );
					GameName = Payload.substr( GameNameStart + 1 );
					m_GHost->CreateGame( GAME_PRIVATE, false, GameName, Owner, User, string( ), false );
				}
			}
	
			//
			// !PUB (host public game)
			//
	
			if( Command == "pub" && !Payload.empty( ) )
				m_GHost->CreateGame( GAME_PUBLIC, false, Payload, User, User, string( ), false );
	
			//
			// !PUBBY (host public game by other player)
			//
	
			if( Command == "pubby" && !Payload.empty( ) )
			{
				// extract the owner and the game name
				// e.g. "Varlock dota 6.54b arem ~~~" -> owner: "Varlock", game name: "dota 6.54b arem ~~~"
	
				string Owner;
				string GameName;
				string :: size_type GameNameStart = Payload.find( " " );
	
				if( GameNameStart != string :: npos )
				{
					Owner = Payload.substr( 0, GameNameStart );
					GameName = Payload.substr( GameNameStart + 1 );
					m_GHost->CreateGame( GAME_PUBLIC, false, GameName, Owner, User, string( ), false );
				}
			}
	
			//
			// !SAYGAME
			//
	
			if( Command == "saygame" && !Payload.empty( ) )
			{
				// extract the game number and the message
				// e.g. "3 hello everyone" -> game number: "3", message: "hello everyone"
	
				uint32_t GameNumber;
				string Message;
				stringstream SS;
				SS << Payload;
				SS >> GameNumber;
	
				if( SS.fail( ) )
					CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to saygame command" );
				else
				{
					if( SS.eof( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to saygame command" );
					else
					{
						getline( SS, Message );
						string :: size_type Start = Message.find_first_not_of( " " );
	
						if( Start != string :: npos )
							Message = Message.substr( Start );
	
						if( GameNumber - 1 < m_GHost->m_Games.size( ) )
							m_GHost->m_Games[GameNumber - 1]->SendAllChat( "ADMIN: " + Message );
						else
							SendChat( player, m_GHost->m_Language->GameNumberDoesntExist( UTIL_ToString( GameNumber ) ) );
					}
				}
			}
	
			//
			// !SAYGAMES
			//
	
			if( Command == "saygames" && !Payload.empty( ) )
			{
				if( m_GHost->m_CurrentGame )
					m_GHost->m_CurrentGame->SendAllChat( Payload );
	
				for( vector<CBaseGame *> :: iterator i = m_GHost->m_Games.begin( ); i != m_GHost->m_Games.end( ); i++ )
					(*i)->SendAllChat( "ADMIN: " + Payload );
			}
	
			//
			// !UNHOST
			//
	
			if( Command == "unhost" )
			{
				if( m_GHost->m_CurrentGame )
				{
					if( m_GHost->m_CurrentGame->GetCountDownStarted( ) )
						SendChat( player, m_GHost->m_Language->UnableToUnhostGameCountdownStarted( m_GHost->m_CurrentGame->GetDescription( ) ) );
					else
					{
						SendChat( player, m_GHost->m_Language->UnhostingGame( m_GHost->m_CurrentGame->GetDescription( ) ) );
						m_GHost->m_CurrentGame->SetExiting( true );
					}
				}
				else
					SendChat( player, m_GHost->m_Language->UnableToUnhostGameNoGameInLobby( ) );
			}*/
			
			//
			// !PRIV (host private game)
			//
	
			if("priv".equals(command) && !payload.isEmpty())
				this.getGhost().CreateGame(Constants.GAME_PRIVATE, false, payload, User, User, "", false );
	
		} else {
			LOG.info( "User [" + User + "] sent command [" + command + "] with payload [" + payload + "]" );
		}
			
		// NON ADMIN COMMANDS 
	
		//
		// !PASSWORD
		//
	
		if ("password".equals(command) && !player.GetLoggedIn( ) ) {
			if (!this.password.isEmpty( ) && payload == this.password ) {
				LOG.info("User [" + User + "] logged in" );
				SendChat(player, MessageFactory.create(Messages.ADMIN_LOGGED_IN));
				player.SetLoggedIn( true );
			} else {
				int LoginAttempts = player.GetLoginAttempts() + 1;
				player.SetLoginAttempts( LoginAttempts );
				LOG.info("User [" + User + "] login attempt failed" );
				SendChat(player, MessageFactory.create(Messages.ADMIN_INVALID_PASSWORD, LoginAttempts));
				
				if (LoginAttempts >= 1) {
					player.SetDeleteMe( true );
					player.SetLeftReason( "was kicked for too many failed login attempts" );
					OpenSlot(GetSIDFromPID(player.GetPID()), false);
					// tempban for 5 seconds to prevent bruteforcing
					//TODO: implement temp ban
					//m_TempBans.push_back( TempBan( player->GetExternalIPString( ), GetTime( ) + 5 ) );
				}
			}
		}
	}
	
}

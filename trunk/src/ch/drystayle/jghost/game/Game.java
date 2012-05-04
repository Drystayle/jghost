package ch.drystayle.jghost.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.bnet.BnetConnection;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.game.stats.Stats;
import ch.drystayle.jghost.game.stats.StatsDota;
import ch.drystayle.jghost.game.stats.StatsW3MMD;
import ch.drystayle.jghost.i18n.MessageFactory;
import ch.drystayle.jghost.i18n.Messages;
import ch.drystayle.jghost.map.Map;
import ch.drystayle.jghost.protocol.IncomingAction;
import ch.drystayle.jghost.util.PingComparator;
import ch.drystayle.jghost.util.TimeUtil;

public class Game extends BaseGame {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(Game.class);
	
	//---- State
	
	private Stats stats;
	
	//---- Constructor
	
	public Game(JGhost host, Map map, SaveGame saveGame, short hostPort,
			char gameState, String gameName, String ownerName,
			String creatorName, String creatorServer) {
		super(host, map, saveGame, hostPort, gameState, gameName, ownerName,
				creatorName, creatorServer);
		/*m_DBBanLast = NULL;
		m_DBGame = new CDBGame( 0, string( ), m_Map->GetMapPath( ), string( ), string( ), string( ), 0 );
*/
		if (m_Map.GetMapType().equals("w3mmd"))
			stats = new StatsW3MMD(this, m_Map.GetMapStatsW3MMDCategory());
		else if (m_Map.GetMapType().equals("dota"))
			stats = new StatsDota(this);
		else
			stats = null;

		/*m_CallableGameAdd = NULL;*/
	}

	//---- Methods
	
	public boolean Update (Object fd) {
		// update callables

		/*for( vector<PairedBanCheck> :: iterator i = m_PairedBanChecks.begin( ); i != m_PairedBanChecks.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				CDBBan *Ban = i->second->GetResult( );

				if( Ban )
					SendAllChat( m_GHost->m_Language->UserWasBannedOnByBecause( i->second->GetServer( ), i->second->GetUser( ), Ban->GetDate( ), Ban->GetAdmin( ), Ban->GetReason( ) ) );
				else
					SendAllChat( m_GHost->m_Language->UserIsNotBanned( i->second->GetServer( ), i->second->GetUser( ) ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedBanChecks.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedBanAdd> :: iterator i = m_PairedBanAdds.begin( ); i != m_PairedBanAdds.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				if( i->second->GetResult( ) )
				{
					for( vector<CBNET *> :: iterator j = m_GHost->m_BNETs.begin( ); j != m_GHost->m_BNETs.end( ); j++ )
					{
						if( (*j)->GetServer( ) == i->second->GetServer( ) )
							(*j)->AddBan( i->second->GetUser( ), i->second->GetIP( ), i->second->GetGameName( ), i->second->GetAdmin( ), i->second->GetReason( ) );
					}

					SendAllChat( m_GHost->m_Language->PlayerWasBannedByPlayer( i->second->GetServer( ), i->second->GetUser( ), i->first ) );
				}

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedBanAdds.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedGPSCheck> :: iterator i = m_PairedGPSChecks.begin( ); i != m_PairedGPSChecks.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				CDBGamePlayerSummary *GamePlayerSummary = i->second->GetResult( );

				if( GamePlayerSummary )
				{
					if( i->first.empty( ) )
						SendAllChat( m_GHost->m_Language->HasPlayedGamesWithThisBot( i->second->GetName( ), GamePlayerSummary->GetFirstGameDateTime( ), GamePlayerSummary->GetLastGameDateTime( ), UTIL_ToString( GamePlayerSummary->GetTotalGames( ) ), UTIL_ToString( (float)GamePlayerSummary->GetAvgLoadingTime( ) / 1000, 2 ), UTIL_ToString( GamePlayerSummary->GetAvgLeftPercent( ) ) ) );
					else
					{
						CGamePlayer *Player = GetPlayerFromName( i->first, true );

						if( Player )
							SendChat( Player, m_GHost->m_Language->HasPlayedGamesWithThisBot( i->second->GetName( ), GamePlayerSummary->GetFirstGameDateTime( ), GamePlayerSummary->GetLastGameDateTime( ), UTIL_ToString( GamePlayerSummary->GetTotalGames( ) ), UTIL_ToString( (float)GamePlayerSummary->GetAvgLoadingTime( ) / 1000, 2 ), UTIL_ToString( GamePlayerSummary->GetAvgLeftPercent( ) ) ) );
					}
				}
				else
				{
					if( i->first.empty( ) )
						SendAllChat( m_GHost->m_Language->HasntPlayedGamesWithThisBot( i->second->GetName( ) ) );
					else
					{
						CGamePlayer *Player = GetPlayerFromName( i->first, true );

						if( Player )
							SendChat( Player, m_GHost->m_Language->HasntPlayedGamesWithThisBot( i->second->GetName( ) ) );
					}
				}

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedGPSChecks.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedDPSCheck> :: iterator i = m_PairedDPSChecks.begin( ); i != m_PairedDPSChecks.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				CDBDotAPlayerSummary *DotAPlayerSummary = i->second->GetResult( );

				if( DotAPlayerSummary )
				{
					string Summary = m_GHost->m_Language->HasPlayedDotAGamesWithThisBot(	i->second->GetName( ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalGames( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalWins( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalLosses( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalDeaths( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalCreepKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalCreepDenies( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalAssists( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalNeutralKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalTowerKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalRaxKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetTotalCourierKills( ) ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgKills( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgDeaths( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgCreepKills( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgCreepDenies( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgAssists( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgNeutralKills( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgTowerKills( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgRaxKills( ), 2 ),
																							UTIL_ToString( DotAPlayerSummary->GetAvgCourierKills( ), 2 ) );

					if( i->first.empty( ) )
						SendAllChat( Summary );
					else
					{
						CGamePlayer *Player = GetPlayerFromName( i->first, true );

						if( Player )
							SendChat( Player, Summary );
					}
				}
				else
				{
					if( i->first.empty( ) )
						SendAllChat( m_GHost->m_Language->HasntPlayedDotAGamesWithThisBot( i->second->GetName( ) ) );
					else
					{
						CGamePlayer *Player = GetPlayerFromName( i->first, true );

						if( Player )
							SendChat( Player, m_GHost->m_Language->HasntPlayedDotAGamesWithThisBot( i->second->GetName( ) ) );
					}
				}

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedDPSChecks.erase( i );
			}
			else
				i++;
		}*/

		return super.Update(fd);
	}

	public void EventPlayerDeleted( GamePlayer player ) {
		super.EventPlayerDeleted(player);

		// record everything we need to know about the player for storing in the database later
		// since we haven't stored the game yet (it's not over yet!) we can't link the gameplayer to the game
		// see the destructor for where these CDBGamePlayers are stored in the database
		// we could have inserted an incomplete record on creation and updated it later but this makes for a cleaner interface

		if( m_GameLoading || m_GameLoaded )
		{
			// todotodo: since we store players that crash during loading it's possible that the stats classes could have no information on them
			// that could result in a DBGamePlayer without a corresponding DBDotAPlayer - just be aware of the possibility

			char SID = GetSIDFromPID(player.GetPID());
			char Team = 255;
			char Colour = 255;

			if (SID < m_Slots.size()) {
				Team = m_Slots.get(SID).GetTeam( );
				Colour = m_Slots.get(SID).GetColour( );
			}

			//TODO ban
			/*m_DBGamePlayers.push_back( new CDBGamePlayer( 0, 0, player->GetName( ), player->GetExternalIPString( ), player->GetSpoofed( ) ? 1 : 0, player->GetSpoofedRealm( ), player->GetReserved( ) ? 1 : 0, player->GetFinishedLoading( ) ? player->GetFinishedLoadingTicks( ) - m_StartedLoadingTicks : 0, GetTime( ) - m_StartedLoadingTime, player->GetLeftReason( ), Team, Colour ) );

			// also keep track of the last player to leave for the !banlast command

			for( vector<CDBBan *> :: iterator i = m_DBBans.begin( ); i != m_DBBans.end( ); i++ )
			{
				if( (*i)->GetName( ) == player->GetName( ) )
					m_DBBanLast = *i;
			}*/
		}
	}

	public void EventPlayerAction(GamePlayer player, IncomingAction action) {
		super.EventPlayerAction( player, action );

		// give the stats class a chance to process the action

		if (stats != null && stats.processAction(action) && m_GameOverTime == 0 )
		{
			LOG.info( "[GAME: " + m_GameName + "] gameover timer started (stats class reported game over)" );
			SendEndMessage( );
			m_GameOverTime = TimeUtil.getTime();
		}
	}

	public void EventPlayerBotCommand(GamePlayer player, String command, String payload ) {
		// todotodo: don't be lazy

		String User = player.GetName();
		String Command = command;
		String Payload = payload;

		boolean AdminCheck = false;

		for (BnetConnection bConn : this.getGhost().getBnetConnections()) {
			if(bConn.GetServer().equals(player.GetSpoofedRealm()) && bConn.IsAdmin(User)) {
				AdminCheck = true;
				break;
			}
		}

		boolean RootAdminCheck = false;

		for(BnetConnection bConn : this.getGhost().getBnetConnections())
		{
			if(bConn.GetServer().equals(player.GetSpoofedRealm()) && bConn.IsRootAdmin(User))
			{
				RootAdminCheck = true;
				break;
			}
		}

		if( player.GetSpoofed() && ( AdminCheck || RootAdminCheck || IsOwner(User))) {
			LOG.info( "[" + m_GameName + "] admin [" + User + "] sent command [" + Command + "] with payload [" + Payload + "]" );

			if( !m_Locked || RootAdminCheck || IsOwner( User ) )
			{
				/*****************
				* ADMIN COMMANDS *
				******************/
				
				//
				// !ABORT (abort countdown)
				// !A
				//

				// we use "!a" as an alias for abort because you don't have much time to abort the countdown so it's useful for the abort command to be easy to type

				if((Command.equals("abort") || Command.equals("a")) && m_CountDownStarted && !m_GameLoading && !m_GameLoaded )
				{
					SendAllChat(MessageFactory.create(Messages.COUNTDOWN_ABORTED));
					m_CountDownStarted = false;
				}
				
				//
				// !COMP (computer slot)
				//

				if( Command.equals("comp") && !Payload.isEmpty() && !m_GameLoading && !m_GameLoaded && m_SaveGame == null)
				{
					// extract the slot and the skill
					// e.g. "1 2" -> slot: "1", skill: "2"

					String[] splitPayload = Payload.split(" ", 2);
					
					int Slot;
					int Skill = 1;
					try {
						Slot = Integer.valueOf(splitPayload[0]);
						if (splitPayload.length == 2) {
							Skill = Integer.valueOf(splitPayload[1]);
						}
						
						ComputerSlot((char)(Slot - 1), (char) Skill, true);
					} catch (Exception e) {
						LOG.info("[" + m_GameName + "] bad input to comp command: " + Payload);
					}
				}
				
				//
				// !DROP
				//

				if( Command.equals("drop") && m_GameLoaded) {
					StopLaggers("lagged out (dropped by admin)");
				}
				
				//
				// !END
				//

				if( Command == "end" && m_GameLoaded ) {
					LOG.info("[" + m_GameName + "] is over (admin ended game)");
					StopPlayers("was disconnected (game ended)");
				}
				
				//
				// !FROM
				//

				if(Command.equals("from")) {
					String Froms = "";

					for (int i = 0; i < this.m_Players.size(); i++) {
						// we reverse the byte order on the IP because it's stored in network byte order
						GamePlayer p = this.m_Players.get(i);
						
						Froms += p.GetName();
						Froms += ": (";
						Locale loc = p.getLocale();
						if (loc == null) {
							Froms += "NA";
						} else {
							Froms += loc.getCountry();
						}
						Froms += ")";

						if ( i != m_Players.size() - 1 ) {
							Froms += ", ";
						}
					}

					SendAllChat(MessageFactory.create(Froms));
				}
				
				//
				// !KICK (kick a player)
				//

				if( Command.equals("kick") && !Payload.isEmpty()) {
					List<GamePlayer> matches = GetPlayerFromNamePartial(Payload);

					if(matches.size() == 0) {
						SendAllChat(MessageFactory.create(Messages.UNABLE_TO_KICK_NO_MATCHES_FOUND, Payload));
					} else if (matches.size() == 1 ) {
						GamePlayer match = matches.get(0);
						match.SetDeleteMe(true);
						match.SetLeftReason(Messages.WAS_KICKED_BY_PLAYER.createMessage(User));

						if( !m_GameLoading && !m_GameLoaded ) {
							match.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
						} else {
							match.SetLeftCode(Constants.PLAYERLEAVE_LOST);
						}
							
						if( !m_GameLoading && !m_GameLoaded )
							OpenSlot(GetSIDFromPID(match.GetPID()), false );
					} else {
						SendAllChat(MessageFactory.create(Messages.UNABLE_TO_KICK_FOUND_MORE_THAN_ONE_MATCH.createMessage(Payload)));
					}
				}
				
				//
				// !PING
				//

				if( Command == "ping" )
				{
					// kick players with ping higher than payload if payload isn't empty
					// we only do this if the game hasn't started since we don't want to kick players from a game in progress

					int Kicked = 0;
					int KickPing = 0;

					if( !m_GameLoading && !m_GameLoaded && !Payload.isEmpty()) {
						try {
							KickPing = Integer.valueOf(Payload);
						} catch (Exception e) {
							LOG.info("Could not cast " + Payload + " to int");
						}
					}

					// copy the m_Players vector so we can sort by descending ping so it's easier to find players with high pings

					List<GamePlayer> sortedPlayers = new ArrayList<GamePlayer>(m_Players);
					Collections.sort(sortedPlayers, new PingComparator(getGhost().isLcPings()));
					String Pings = "";

					for(Iterator<GamePlayer> iPlayer = sortedPlayers.iterator(); iPlayer.hasNext(); ) {
						GamePlayer p = iPlayer.next();
						Pings += p.GetName();
						Pings += ": ";

						if(p.GetNumPings() > 0 )
						{
							Pings += p.GetPing(this.getGhost().isLcPings());

							if( !m_GameLoading && !m_GameLoaded && !p.GetReserved() && !Payload.isEmpty() && p.GetPing(this.getGhost().isLcPings()) > KickPing )
							{
								p.SetDeleteMe( true );
								p.SetLeftReason( "was kicked for excessive ping " + p.GetPing(this.getGhost().isLcPings()) + " > " + KickPing);
								p.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
								OpenSlot( GetSIDFromPID(p.GetPID()), false );
								Kicked++;
							}

							Pings += "ms";
						}
						else
							Pings += "N/A";

						if(iPlayer.hasNext()) {
							Pings += ", ";
						}
					}

					SendAllChat(MessageFactory.create(Pings));

					if( Kicked > 0 ) {
						SendAllChat(MessageFactory.create(Messages.KICKING_PLAYERS_WITH_PINGS_GREATER_THAN.createMessage(Kicked, KickPing)));
					}
				}

				//
				// !PRIV (rehost as private game)
				//

				if(Command.equals("priv") && !Payload.isEmpty( ) && !m_CountDownStarted && m_SaveGame == null)
				{
					LOG.info( "[GAME: " + m_GameName + "] trying to rehost as private game [" + Payload + "]" );
					m_GameState = Constants.GAME_PRIVATE;
					m_GameName = Payload;
					m_HostCounter = this.getGhost().getHostCounter();
					this.getGhost().setHostCounter(++this.m_HostCounter);
					m_RefreshError = false;

					for(BnetConnection bConn : getGhost().getBnetConnections())
					{
						bConn.QueueEnterChat();

						// we need to send the game creation message now because private games are not refreshed

						bConn.QueueGameCreate( m_GameState, m_GameName, "", m_Map, null, m_HostCounter);

						if (!bConn.GetPasswordHashType().equals("pvpgn")) {
							bConn.QueueEnterChat();
						}
					}

					m_CreationTime = TimeUtil.getTime();
					m_LastRefreshTime = TimeUtil.getTime();
				}
				
				//
				// !PUB (rehost as public game)
				//

				if( Command.equals("pub") && !Payload.isEmpty( ) && !m_CountDownStarted ) {
					LOG.info( "[" + m_GameName + "] trying to rehost as public game [" + Payload + "]" );
					m_GameState = Constants.GAME_PUBLIC;
					m_GameName = Payload;
					m_HostCounter = this.getGhost().getHostCounter();
					this.getGhost().setHostCounter(++this.m_HostCounter);
					m_RefreshError = false;
					
					for(BnetConnection bConn : this.getGhost().getBnetConnections()) {
						bConn.QueueGameUncreate();
						bConn.QueueEnterChat();

						// the game creation message will be sent on the next refresh
					}

					m_CreationTime = TimeUtil.getTime( );
					m_LastRefreshTime = TimeUtil.getTime( );
				}
				
				//
				// !START
				//

				if( Command.equals("start") && !m_CountDownStarted )
				{
					// if the player sent "!start force" skip the checks and start the countdown
					// otherwise check that the game is ready to start

					if(Payload.equals("force")) {
						StartCountDown( true );
					} else {
						StartCountDown( false );
					}
				}
				
				//
				// !SWAP (swap slots)
				//

				if( Command == "swap" && !Payload.isEmpty( ) && !m_GameLoading && !m_GameLoaded )
				{
					try {
						String[] splitPayload = Payload.split(" ", 2);
						if (splitPayload.length < 2) {
							throw new Exception("missing input for swap");
						}
						int SID1 = Integer.parseInt(splitPayload[0]);
						int SID2 = Integer.parseInt(splitPayload[1]);
						
						SwapSlots((char) (SID1 - 1), (char) (SID2 - 1));
					} catch (Exception e) {
						LOG.info( "[" + m_GameName + "] bad input to swap command");
					}
				}
				
				//
				// !UNHOST
				//

				if (Command.equals("unhost") && !m_CountDownStarted) {
					m_Exiting = true;
				}
				
				/*

				//
				// !ADDBAN
				// !BAN
				//

				if( ( Command == "addban" || Command == "ban" ) && !Payload.empty( ) && !m_GHost->m_BNETs.empty( ) )
				{
					// extract the victim and the reason
					// e.g. "Varlock leaver after dying" -> victim: "Varlock", reason: "leaver after dying"

					string Victim;
					string Reason;
					stringstream SS;
					SS << Payload;
					SS >> Victim;

					if( !SS.eof( ) )
					{
						getline( SS, Reason );
						string :: size_type Start = Reason.find_first_not_of( " " );

						if( Start != string :: npos )
							Reason = Reason.substr( Start );
					}

					if( m_GameLoaded )
					{
						string VictimLower = Victim;
						transform( VictimLower.begin( ), VictimLower.end( ), VictimLower.begin( ), (int(*)(int))tolower );
						uint32_t Matches = 0;
						CDBBan *LastMatch = NULL;

						// try to match each player with the passed string (e.g. "Varlock" would be matched with "lock")
						// we use the m_DBBans vector for this in case the player already left and thus isn't in the m_Players vector anymore

						for( vector<CDBBan *> :: iterator i = m_DBBans.begin( ); i != m_DBBans.end( ); i++ )
						{
							string TestName = (*i)->GetName( );
							transform( TestName.begin( ), TestName.end( ), TestName.begin( ), (int(*)(int))tolower );

							if( TestName.find( VictimLower ) != string :: npos )
							{
								Matches++;
								LastMatch = *i;
							}
						}

						if( Matches == 0 )
							SendAllChat( m_GHost->m_Language->UnableToBanNoMatchesFound( Victim ) );
						else if( Matches == 1 )
						{
							if( !LastMatch->GetServer( ).empty( ) )
							{
								// the user was spoof checked, ban only on the spoofed realm

								m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( LastMatch->GetServer( ), LastMatch->GetName( ), LastMatch->GetIP( ), m_GameName, User, Reason ) ) );
							}
							else
							{
								// the user wasn't spoof checked, ban on every realm

								for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
									m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( (*i)->GetServer( ), LastMatch->GetName( ), LastMatch->GetIP( ), m_GameName, User, Reason ) ) );
							}
						}
						else
							SendAllChat( m_GHost->m_Language->UnableToBanFoundMoreThanOneMatch( Victim ) );
					}
					else
					{
						CGamePlayer *LastMatch = NULL;
						uint32_t Matches = GetPlayerFromNamePartial( Victim, &LastMatch );

						if( Matches == 0 )
							SendAllChat( m_GHost->m_Language->UnableToBanNoMatchesFound( Victim ) );
						else if( Matches == 1 )
						{
							if( !LastMatch->GetSpoofedRealm( ).empty( ) )
							{
								// the user was spoof checked, ban only on the spoofed realm

								m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( LastMatch->GetSpoofedRealm( ), LastMatch->GetName( ), LastMatch->GetExternalIPString( ), m_GameName, User, Reason ) ) );
							}
							else
							{
								// the user wasn't spoof checked, ban on every realm

								for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
									m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( (*i)->GetServer( ), LastMatch->GetName( ), LastMatch->GetExternalIPString( ), m_GameName, User, Reason ) ) );
							}
						}
						else
							SendAllChat( m_GHost->m_Language->UnableToBanFoundMoreThanOneMatch( Victim ) );
					}
				}

				//
				// !ANNOUNCE
				//

				if( Command == "announce" && !m_CountDownStarted )
				{
					if( Payload.empty( ) || Payload == "off" )
					{
						SendAllChat( m_GHost->m_Language->AnnounceMessageDisabled( ) );
						SetAnnounce( 0, string( ) );
					}
					else
					{
						// extract the interval and the message
						// e.g. "30 hello everyone" -> interval: "30", message: "hello everyone"

						uint32_t Interval;
						string Message;
						stringstream SS;
						SS << Payload;
						SS >> Interval;

						if( SS.fail( ) || Interval == 0 )
							CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to announce command" );
						else
						{
							if( SS.eof( ) )
								CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to announce command" );
							else
							{
								getline( SS, Message );
								string :: size_type Start = Message.find_first_not_of( " " );

								if( Start != string :: npos )
									Message = Message.substr( Start );

								SendAllChat( m_GHost->m_Language->AnnounceMessageEnabled( ) );
								SetAnnounce( Interval, Message );
							}
						}
					}
				}

				//
				// !AUTOSAVE
				//

				if( Command == "autosave" )
				{
					if( Payload == "on" )
					{
						SendAllChat( m_GHost->m_Language->AutoSaveEnabled( ) );
						m_AutoSave = true;
					}
					else if( Payload == "off" )
					{
						SendAllChat( m_GHost->m_Language->AutoSaveDisabled( ) );
						m_AutoSave = false;
					}
				}

				//
				// !AUTOSTART
				//

				if( Command == "autostart" && !m_CountDownStarted )
				{
					if( Payload.empty( ) || Payload == "off" )
					{
						SendAllChat( m_GHost->m_Language->AutoStartDisabled( ) );
						m_AutoStartPlayers = 0;
					}
					else
					{
						uint32_t AutoStartPlayers = UTIL_ToUInt32( Payload );

						if( AutoStartPlayers != 0 )
						{
							SendAllChat( m_GHost->m_Language->AutoStartEnabled( UTIL_ToString( AutoStartPlayers ) ) );
							m_AutoStartPlayers = AutoStartPlayers;
						}
					}
				}

				//
				// !BANLAST
				//

				if( Command == "banlast" && m_GameLoaded && !m_GHost->m_BNETs.empty( ) && m_DBBanLast )
				{
					if( !m_DBBanLast->GetServer( ).empty( ) )
					{
						// the user was spoof checked, ban only on the spoofed realm

						m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( m_DBBanLast->GetServer( ), m_DBBanLast->GetName( ), m_DBBanLast->GetIP( ), m_GameName, User, Payload ) ) );
					}
					else
					{
						// the user wasn't spoof checked, ban on every realm

						for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
							m_PairedBanAdds.push_back( PairedBanAdd( User, m_GHost->m_DB->ThreadedBanAdd( (*i)->GetServer( ), m_DBBanLast->GetName( ), m_DBBanLast->GetIP( ), m_GameName, User, Payload ) ) );
					}
				}

				//
				// !CHECK
				//

				if( Command == "check" )
				{
					if( !Payload.empty( ) )
					{
						CGamePlayer *LastMatch = NULL;
						uint32_t Matches = GetPlayerFromNamePartial( Payload, &LastMatch );

						if( Matches == 0 )
							SendAllChat( m_GHost->m_Language->UnableToCheckPlayerNoMatchesFound( Payload ) );
						else if( Matches == 1 )
						{
							bool LastMatchAdminCheck = false;

							for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
							{
								if( (*i)->GetServer( ) == LastMatch->GetSpoofedRealm( ) && (*i)->IsAdmin( LastMatch->GetName( ) ) )
								{
									LastMatchAdminCheck = true;
									break;
								}
							}

							bool LastMatchRootAdminCheck = false;

							for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
							{
								if( (*i)->GetServer( ) == LastMatch->GetSpoofedRealm( ) && (*i)->IsRootAdmin( LastMatch->GetName( ) ) )
								{
									LastMatchRootAdminCheck = true;
									break;
								}
							}

							SendAllChat( m_GHost->m_Language->CheckedPlayer( LastMatch->GetName( ), LastMatch->GetNumPings( ) > 0 ? UTIL_ToString( LastMatch->GetPing( m_GHost->m_LCPings ) ) + "ms" : "N/A", m_GHost->m_DBLocal->FromCheck( UTIL_ByteArrayToUInt32( LastMatch->GetExternalIP( ), true ) ), LastMatchAdminCheck || LastMatchRootAdminCheck ? "Yes" : "No", IsOwner( LastMatch->GetName( ) ) ? "Yes" : "No", LastMatch->GetSpoofed( ) ? "Yes" : "No", LastMatch->GetSpoofedRealm( ).empty( ) ? "N/A" : LastMatch->GetSpoofedRealm( ), LastMatch->GetReserved( ) ? "Yes" : "No" ) );
						}
						else
							SendAllChat( m_GHost->m_Language->UnableToCheckPlayerFoundMoreThanOneMatch( Payload ) );
					}
					else
						SendAllChat( m_GHost->m_Language->CheckedPlayer( User, player->GetNumPings( ) > 0 ? UTIL_ToString( player->GetPing( m_GHost->m_LCPings ) ) + "ms" : "N/A", m_GHost->m_DBLocal->FromCheck( UTIL_ByteArrayToUInt32( player->GetExternalIP( ), true ) ), AdminCheck || RootAdminCheck ? "Yes" : "No", IsOwner( User ) ? "Yes" : "No", player->GetSpoofed( ) ? "Yes" : "No", player->GetSpoofedRealm( ).empty( ) ? "N/A" : player->GetSpoofedRealm( ), player->GetReserved( ) ? "Yes" : "No" ) );
				}

				//
				// !CHECKBAN
				//

				if( Command == "checkban" && !Payload.empty( ) && !m_GHost->m_BNETs.empty( ) )
				{
					for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
						m_PairedBanChecks.push_back( PairedBanCheck( User, m_GHost->m_DB->ThreadedBanCheck( (*i)->GetServer( ), Payload ) ) );
				}

				//
				// !CLOSE (close slot)
				//

				if( Command == "close" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded )
				{
					// close as many slots as specified, e.g. "5 10" closes slots 5 and 10

					stringstream SS;
					SS << Payload;

					while( !SS.eof( ) )
					{
						uint32_t SID;
						SS >> SID;

						if( SS.fail( ) )
						{
							CONSOLE_Print( "[GAME: " + m_GameName + "] bad input to close command" );
							break;
						}
						else
							CloseSlot( (unsigned char)( SID - 1 ), true );
					}
				}

				//
				// !CLOSEALL
				//

				if( Command == "closeall" && !m_GameLoading && !m_GameLoaded )
					CloseAllSlots( );


				//
				// !COMPCOLOUR (computer colour change)
				//

				if( Command == "compcolour" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded && !m_SaveGame )
				{
					// extract the slot and the colour
					// e.g. "1 2" -> slot: "1", colour: "2"

					uint32_t Slot;
					uint32_t Colour;
					stringstream SS;
					SS << Payload;
					SS >> Slot;

					if( SS.fail( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to compcolour command" );
					else
					{
						if( SS.eof( ) )
							CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to compcolour command" );
						else
						{
							SS >> Colour;

							if( SS.fail( ) )
								CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #2 to compcolour command" );
							else
							{
								unsigned char SID = (unsigned char)( Slot - 1 );

								if( m_Map->GetMapGameType( ) != GAMETYPE_CUSTOM && Colour < 12 && SID < m_Slots.size( ) )
								{
									if( m_Slots[SID].GetSlotStatus( ) == SLOTSTATUS_OCCUPIED && m_Slots[SID].GetComputer( ) == 1 )
										ColourSlot( SID, Colour );
								}
							}
						}
					}
				}

				//
				// !COMPHANDICAP (computer handicap change)
				//

				if( Command == "comphandicap" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded && !m_SaveGame )
				{
					// extract the slot and the handicap
					// e.g. "1 50" -> slot: "1", handicap: "50"

					uint32_t Slot;
					uint32_t Handicap;
					stringstream SS;
					SS << Payload;
					SS >> Slot;

					if( SS.fail( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to comphandicap command" );
					else
					{
						if( SS.eof( ) )
							CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to comphandicap command" );
						else
						{
							SS >> Handicap;

							if( SS.fail( ) )
								CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #2 to comphandicap command" );
							else
							{
								unsigned char SID = (unsigned char)( Slot - 1 );

								if( m_Map->GetMapGameType( ) != GAMETYPE_CUSTOM && ( Handicap == 50 || Handicap == 60 || Handicap == 70 || Handicap == 80 || Handicap == 90 || Handicap == 100 ) && SID < m_Slots.size( ) )
								{
									if( m_Slots[SID].GetSlotStatus( ) == SLOTSTATUS_OCCUPIED && m_Slots[SID].GetComputer( ) == 1 )
									{
										m_Slots[SID].SetHandicap( (unsigned char)Handicap );
										SendAllSlotInfo( );
									}
								}
							}
						}
					}
				}

				//
				// !COMPRACE (computer race change)
				//

				if( Command == "comprace" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded && !m_SaveGame )
				{
					// extract the slot and the race
					// e.g. "1 human" -> slot: "1", race: "human"

					uint32_t Slot;
					string Race;
					stringstream SS;
					SS << Payload;
					SS >> Slot;

					if( SS.fail( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to comprace command" );
					else
					{
						if( SS.eof( ) )
							CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to comprace command" );
						else
						{
							getline( SS, Race );
							string :: size_type Start = Race.find_first_not_of( " " );

							if( Start != string :: npos )
								Race = Race.substr( Start );

							transform( Race.begin( ), Race.end( ), Race.begin( ), (int(*)(int))tolower );
							unsigned char SID = (unsigned char)( Slot - 1 );

							if( m_Map->GetMapGameType( ) != GAMETYPE_CUSTOM && !( m_Map->GetMapFlags( ) & MAPFLAG_RANDOMRACES ) && SID < m_Slots.size( ) )
							{
								if( m_Slots[SID].GetSlotStatus( ) == SLOTSTATUS_OCCUPIED && m_Slots[SID].GetComputer( ) == 1 )
								{
									if( Race == "human" )
									{
										m_Slots[SID].SetRace( SLOTRACE_HUMAN );
										SendAllSlotInfo( );
									}
									else if( Race == "orc" )
									{
										m_Slots[SID].SetRace( SLOTRACE_ORC );
										SendAllSlotInfo( );
									}
									else if( Race == "night elf" )
									{
										m_Slots[SID].SetRace( SLOTRACE_NIGHTELF );
										SendAllSlotInfo( );
									}
									else if( Race == "undead" )
									{
										m_Slots[SID].SetRace( SLOTRACE_UNDEAD );
										SendAllSlotInfo( );
									}
									else if( Race == "random" )
									{
										m_Slots[SID].SetRace( SLOTRACE_RANDOM );
										SendAllSlotInfo( );
									}
									else
										CONSOLE_Print( "[GAME: " + m_GameName + "] unknown race [" + Race + "] sent to comprace command" );
								}
							}
						}
					}
				}

				//
				// !COMPTEAM (computer team change)
				//

				if( Command == "compteam" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded && !m_SaveGame )
				{
					// extract the slot and the team
					// e.g. "1 2" -> slot: "1", team: "2"

					uint32_t Slot;
					uint32_t Team;
					stringstream SS;
					SS << Payload;
					SS >> Slot;

					if( SS.fail( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #1 to compteam command" );
					else
					{
						if( SS.eof( ) )
							CONSOLE_Print( "[GAME: " + m_GameName + "] missing input #2 to compteam command" );
						else
						{
							SS >> Team;

							if( SS.fail( ) )
								CONSOLE_Print( "[GAME: " + m_GameName + "] bad input #2 to compteam command" );
							else
							{
								unsigned char SID = (unsigned char)( Slot - 1 );

								if( m_Map->GetMapGameType( ) != GAMETYPE_CUSTOM && Team < 12 && SID < m_Slots.size( ) )
								{
									if( m_Slots[SID].GetSlotStatus( ) == SLOTSTATUS_OCCUPIED && m_Slots[SID].GetComputer( ) == 1 )
									{
										m_Slots[SID].SetTeam( (unsigned char)( Team - 1 ) );
										SendAllSlotInfo( );
									}
								}
							}
						}
					}
				}

				//
				// !DBSTATUS
				//

				if( Command == "dbstatus" )
					SendAllChat( m_GHost->m_DB->GetStatus( ) );

				//
				// !DOWNLOAD
				// !DL
				//

				if( ( Command == "download" || Command == "dl" ) && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded )
				{
					CGamePlayer *LastMatch = NULL;
					uint32_t Matches = GetPlayerFromNamePartial( Payload, &LastMatch );

					if( Matches == 0 )
						SendAllChat( m_GHost->m_Language->UnableToStartDownloadNoMatchesFound( Payload ) );
					else if( Matches == 1 )
					{
						if( !LastMatch->GetDownloadStarted( ) && !LastMatch->GetDownloadFinished( ) )
						{
							unsigned char SID = GetSIDFromPID( LastMatch->GetPID( ) );

							if( SID < m_Slots.size( ) && m_Slots[SID].GetDownloadStatus( ) != 100 )
							{
								// inform the client that we are willing to send the map

								CONSOLE_Print( "[GAME: " + m_GameName + "] map download started for player [" + LastMatch->GetName( ) + "]" );
								Send( LastMatch, m_Protocol->SEND_W3GS_STARTDOWNLOAD( GetHostPID( ) ) );
								LastMatch->SetDownloadAllowed( true );
								LastMatch->SetDownloadStarted( true );
								LastMatch->SetStartedDownloadingTicks( GetTicks( ) );
							}
						}
					}
					else
						SendAllChat( m_GHost->m_Language->UnableToStartDownloadFoundMoreThanOneMatch( Payload ) );
				}

				//
				// !FAKEPLAYER
				//

				if( Command == "fakeplayer" && !m_CountDownStarted )
				{
					if( m_FakePlayerPID == 255 )
						CreateFakePlayer( );
					else
						DeleteFakePlayer( );
				}

				

				//
				// !HOLD (hold a slot for someone)
				//

				if( Command == "hold" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded )
				{
					// hold as many players as specified, e.g. "Varlock Kilranin" holds players "Varlock" and "Kilranin"

					stringstream SS;
					SS << Payload;

					while( !SS.eof( ) )
					{
						string HoldName;
						SS >> HoldName;

						if( SS.fail( ) )
						{
							CONSOLE_Print( "[GAME: " + m_GameName + "] bad input to hold command" );
							break;
						}
						else
						{
							SendAllChat( m_GHost->m_Language->AddedPlayerToTheHoldList( HoldName ) );
							AddToReserved( HoldName );
						}
					}
				}

				//
				// !LATENCY (set game latency)
				//

				if( Command == "latency" )
				{
					if( Payload.empty( ) )
						SendAllChat( m_GHost->m_Language->LatencyIs( UTIL_ToString( m_Latency ) ) );
					else
					{
						m_Latency = UTIL_ToUInt32( Payload );

						if( m_Latency <= 50 )
						{
							m_Latency = 50;
							SendAllChat( m_GHost->m_Language->SettingLatencyToMinimum( "50" ) );
						}
						else if( m_Latency >= 500 )
						{
							m_Latency = 500;
							SendAllChat( m_GHost->m_Language->SettingLatencyToMaximum( "500" ) );
						}
						else
							SendAllChat( m_GHost->m_Language->SettingLatencyTo( UTIL_ToString( m_Latency ) ) );
					}
				}

				//
				// !LOCK
				//

				if( Command == "lock" && ( RootAdminCheck || IsOwner( User ) ) )
				{
					SendAllChat( m_GHost->m_Language->GameLocked( ) );
					m_Locked = true;
				}

				//
				// !MUTE
				//

				if( Command == "mute" )
				{
					CGamePlayer *LastMatch = NULL;
					uint32_t Matches = GetPlayerFromNamePartial( Payload, &LastMatch );

					if( Matches == 0 )
						SendAllChat( m_GHost->m_Language->UnableToMuteNoMatchesFound( Payload ) );
					else if( Matches == 1 )
					{
						SendAllChat( m_GHost->m_Language->MutedPlayer( LastMatch->GetName( ), User ) );
						LastMatch->SetMuted( true );
					}
					else
						SendAllChat( m_GHost->m_Language->UnableToMuteFoundMoreThanOneMatch( Payload ) );
				}

				//
				// !MUTEALL
				//

				if( Command == "muteall" && m_GameLoaded )
				{
					SendAllChat( m_GHost->m_Language->GlobalChatMuted( ) );
					m_MuteAll = true;
				}

				//
				// !OPEN (open slot)
				//

				if( Command == "open" && !Payload.empty( ) && !m_GameLoading && !m_GameLoaded )
				{
					// open as many slots as specified, e.g. "5 10" opens slots 5 and 10

					stringstream SS;
					SS << Payload;

					while( !SS.eof( ) )
					{
						uint32_t SID;
						SS >> SID;

						if( SS.fail( ) )
						{
							CONSOLE_Print( "[GAME: " + m_GameName + "] bad input to open command" );
							break;
						}
						else
							OpenSlot( (unsigned char)( SID - 1 ), true );
					}
				}

				//
				// !OPENALL
				//

				if( Command == "openall" && !m_GameLoading && !m_GameLoaded )
					OpenAllSlots( );

				//
				// !OWNER (set game owner)
				//

				if( Command == "owner" )
				{
					if( RootAdminCheck || IsOwner( User ) || !GetPlayerFromName( m_OwnerName, false ) )
					{
						if( !Payload.empty( ) )
						{
							SendAllChat( m_GHost->m_Language->SettingGameOwnerTo( Payload ) );
							m_OwnerName = Payload;
						}
						else
						{
							SendAllChat( m_GHost->m_Language->SettingGameOwnerTo( User ) );
							m_OwnerName = User;
						}
					}
					else
						SendAllChat( m_GHost->m_Language->UnableToSetGameOwner( m_OwnerName ) );
				}

				//
				// !REFRESH (turn on or off refresh messages)
				//

				if( Command == "refresh" && !m_CountDownStarted )
				{
					if( Payload == "on" )
					{
						SendAllChat( m_GHost->m_Language->RefreshMessagesEnabled( ) );
						m_RefreshMessages = true;
					}
					else if( Payload == "off" )
					{
						SendAllChat( m_GHost->m_Language->RefreshMessagesDisabled( ) );
						m_RefreshMessages = false;
					}
				}

				//
				// !SENDLAN
				//

				if( Command == "sendlan" && !Payload.empty( ) && !m_CountDownStarted )
				{
					// extract the ip and the port
					// e.g. "1.2.3.4 6112" -> ip: "1.2.3.4", port: "6112"

					string IP;
					uint32_t Port = 6112;
					stringstream SS;
					SS << Payload;
					SS >> IP;

					if( !SS.eof( ) )
						SS >> Port;

					if( SS.fail( ) )
						CONSOLE_Print( "[GAME: " + m_GameName + "] bad inputs to sendlan command" );
					else
					{
						// we send 12 for SlotsTotal because this determines how many PID's Warcraft 3 allocates
						// we need to make sure Warcraft 3 allocates at least SlotsTotal + 1 but at most 12 PID's
						// this is because we need an extra PID for the virtual host player (but we always delete the virtual host player when the 12th person joins)
						// however, we can't send 13 for SlotsTotal because this causes Warcraft 3 to crash when sharing control of units
						// nor can we send SlotsTotal because then Warcraft 3 crashes when playing maps with less than 12 PID's (because of the virtual host player taking an extra PID)
						// we also send 12 for SlotsOpen because Warcraft 3 assumes there's always at least one player in the game (the host)
						// so if we try to send accurate numbers it'll always be off by one and results in Warcraft 3 assuming the game is full when it still needs one more player
						// the easiest solution is to simply send 12 for both so the game will always show up as (1/12) players

						BYTEARRAY MapGameType;

						// construct the correct W3GS_GAMEINFO packet

						if( m_SaveGame )
						{
							MapGameType.push_back( 0 );
							MapGameType.push_back( 2 );
							MapGameType.push_back( 0 );
							MapGameType.push_back( 0 );
							BYTEARRAY MapWidth;
							MapWidth.push_back( 0 );
							MapWidth.push_back( 0 );
							BYTEARRAY MapHeight;
							MapHeight.push_back( 0 );
							MapHeight.push_back( 0 );
							m_GHost->m_UDPSocket->SendTo( IP, Port, m_Protocol->SEND_W3GS_GAMEINFO( MapGameType, m_Map->GetMapGameFlags( ), MapWidth, MapHeight, m_GameName, "Varlock", GetTime( ) - m_CreationTime, "Save\\Multiplayer\\" + m_SaveGame->GetFileNameNoPath( ), m_SaveGame->GetMagicNumber( ), 12, 12, m_HostPort, m_HostCounter ) );
						}
						else
						{
							MapGameType.push_back( m_Map->GetMapGameType( ) );
							MapGameType.push_back( 0 );
							MapGameType.push_back( 0 );
							MapGameType.push_back( 0 );
							m_GHost->m_UDPSocket->SendTo( IP, Port, m_Protocol->SEND_W3GS_GAMEINFO( MapGameType, m_Map->GetMapGameFlags( ), m_Map->GetMapWidth( ), m_Map->GetMapHeight( ), m_GameName, "Varlock", GetTime( ) - m_CreationTime, m_Map->GetMapPath( ), m_Map->GetMapCRC( ), 12, 12, m_HostPort, m_HostCounter ) );
						}
					}
				}

				//
				// !SP
				//

				if( Command == "sp" && !m_CountDownStarted )
				{
					SendAllChat( m_GHost->m_Language->ShufflingPlayers( ) );
					ShuffleSlots( );
				}

				//
				// !SYNCLIMIT
				//

				if( Command == "synclimit" )
				{
					if( Payload.empty( ) )
						SendAllChat( m_GHost->m_Language->SyncLimitIs( UTIL_ToString( m_SyncLimit ) ) );
					else
					{
						m_SyncLimit = UTIL_ToUInt32( Payload );

						if( m_SyncLimit <= 10 )
						{
							m_SyncLimit = 10;
							SendAllChat( m_GHost->m_Language->SettingSyncLimitToMinimum( "10" ) );
						}
						else if( m_SyncLimit >= 10000 )
						{
							m_SyncLimit = 10000;
							SendAllChat( m_GHost->m_Language->SettingSyncLimitToMaximum( "10000" ) );
						}
						else
							SendAllChat( m_GHost->m_Language->SettingSyncLimitTo( UTIL_ToString( m_SyncLimit ) ) );
					}
				}

				//
				// !UNHOST
				//

				if( Command == "unhost" && !m_CountDownStarted )
					m_Exiting = true;

				//
				// !UNLOCK
				//

				if( Command == "unlock" && ( RootAdminCheck || IsOwner( User ) ) )
				{
					SendAllChat( m_GHost->m_Language->GameUnlocked( ) );
					m_Locked = false;
				}

				//
				// !UNMUTE
				//

				if( Command == "unmute" )
				{
					CGamePlayer *LastMatch = NULL;
					uint32_t Matches = GetPlayerFromNamePartial( Payload, &LastMatch );

					if( Matches == 0 )
						SendAllChat( m_GHost->m_Language->UnableToMuteNoMatchesFound( Payload ) );
					else if( Matches == 1 )
					{
						SendAllChat( m_GHost->m_Language->UnmutedPlayer( LastMatch->GetName( ), User ) );
						LastMatch->SetMuted( false );
					}
					else
						SendAllChat( m_GHost->m_Language->UnableToMuteFoundMoreThanOneMatch( Payload ) );
				}

				//
				// !UNMUTEALL
				//

				if( Command == "unmuteall" && m_GameLoaded )
				{
					SendAllChat( m_GHost->m_Language->GlobalChatUnmuted( ) );
					m_MuteAll = false;
				}

				//
				// !VIRTUALHOST
				//

				if( Command == "virtualhost" && !Payload.empty( ) && Payload.size( ) <= 15 && !m_CountDownStarted )
				{
					DeleteVirtualHost( );
					m_VirtualHostName = Payload;
				}

				//
				// !VOTECANCEL
				//

				if( Command == "votecancel" && !m_KickVotePlayer.empty( ) )
				{
					SendAllChat( m_GHost->m_Language->VoteKickCancelled( m_KickVotePlayer ) );
					m_KickVotePlayer.clear( );
					m_StartedKickVoteTime = 0;
				}*/
			} else {
				LOG.info( "[" + m_GameName + "] admin command ignored, the game is locked" );
				//TODO game locked
				//SendChat( player, m_GHost->m_Language->TheGameIsLocked( ) );
			}
		} else {
			LOG.info("[" + m_GameName + "] user [" + User + "] sent command [" + Command + "] with payload [" + Payload + "]" );
		}
			
		/*********************
		* NON ADMIN COMMANDS *
		*********************/

		/*//
		// !CHECKME
		//

		if( Command == "checkme" )
			SendChat( player, m_GHost->m_Language->CheckedPlayer( User, player->GetNumPings( ) > 0 ? UTIL_ToString( player->GetPing( m_GHost->m_LCPings ) ) + "ms" : "N/A", m_GHost->m_DBLocal->FromCheck( UTIL_ByteArrayToUInt32( player->GetExternalIP( ), true ) ), AdminCheck || RootAdminCheck ? "Yes" : "No", IsOwner( User ) ? "Yes" : "No", player->GetSpoofed( ) ? "Yes" : "No", player->GetSpoofedRealm( ).empty( ) ? "N/A" : player->GetSpoofedRealm( ), player->GetReserved( ) ? "Yes" : "No" ) );

		//
		// !STATS
		//

		if( Command == "stats" && GetTime( ) >= player->GetStatsSentTime( ) + 5 )
		{
			string StatsUser = User;

			if( !Payload.empty( ) )
				StatsUser = Payload;

			if( player->GetSpoofed( ) && ( AdminCheck || RootAdminCheck || IsOwner( User ) ) )
				m_PairedGPSChecks.push_back( PairedGPSCheck( string( ), m_GHost->m_DB->ThreadedGamePlayerSummaryCheck( StatsUser ) ) );
			else
				m_PairedGPSChecks.push_back( PairedGPSCheck( User, m_GHost->m_DB->ThreadedGamePlayerSummaryCheck( StatsUser ) ) );

			player->SetStatsSentTime( GetTime( ) );
		}

		//
		// !STATSDOTA
		//

		if( Command == "statsdota" && GetTime( ) >= player->GetStatsDotASentTime( ) + 5 )
		{
			string StatsUser = User;

			if( !Payload.empty( ) )
				StatsUser = Payload;

			if( player->GetSpoofed( ) && ( AdminCheck || RootAdminCheck || IsOwner( User ) ) )
				m_PairedDPSChecks.push_back( PairedDPSCheck( string( ), m_GHost->m_DB->ThreadedDotAPlayerSummaryCheck( StatsUser ) ) );
			else
				m_PairedDPSChecks.push_back( PairedDPSCheck( User, m_GHost->m_DB->ThreadedDotAPlayerSummaryCheck( StatsUser ) ) );

			player->SetStatsDotASentTime( GetTime( ) );
		}

		//
		// !VERSION
		//

		if( Command == "version" )
		{
			if( player->GetSpoofed( ) && ( AdminCheck || RootAdminCheck || IsOwner( User ) ) )
				SendChat( player, m_GHost->m_Language->VersionAdmin( m_GHost->m_Version ) );
			else
				SendChat( player, m_GHost->m_Language->VersionNotAdmin( m_GHost->m_Version ) );
		}

		//
		// !VOTEKICK
		//

		if( Command == "votekick" && m_GHost->m_VoteKickAllowed && !Payload.empty( ) )
		{
			if( !m_KickVotePlayer.empty( ) )
				SendAllChat( m_GHost->m_Language->UnableToVoteKickAlreadyInProgress( ) );
			else if( m_Players.size( ) == 2 )
				SendAllChat( m_GHost->m_Language->UnableToVoteKickNotEnoughPlayers( ) );
			else
			{
				CGamePlayer *LastMatch = NULL;
				uint32_t Matches = GetPlayerFromNamePartial( Payload, &LastMatch );

				if( Matches == 0 )
					SendAllChat( m_GHost->m_Language->UnableToVoteKickNoMatchesFound( Payload ) );
				else if( Matches == 1 )
				{
					if( LastMatch->GetReserved( ) )
						SendAllChat( m_GHost->m_Language->UnableToVoteKickPlayerIsReserved( LastMatch->GetName( ) ) );
					else
					{
						m_KickVotePlayer = LastMatch->GetName( );
						m_StartedKickVoteTime = GetTime( );

						for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
							(*i)->SetKickVote( false );

						player->SetKickVote( true );
						CONSOLE_Print( "[GAME: " + m_GameName + "] votekick against player [" + m_KickVotePlayer + "] started by player [" + User + "]" );
						SendAllChat( m_GHost->m_Language->StartedVoteKick( LastMatch->GetName( ), User, UTIL_ToString( (uint32_t)ceil( ( GetNumPlayers( ) - 1 ) * (float)m_GHost->m_VoteKickPercentage / 100 ) - 1 ) ) );
						SendAllChat( m_GHost->m_Language->TypeYesToVote( string( 1, m_GHost->m_CommandTrigger ) ) );
					}
				}
				else
					SendAllChat( m_GHost->m_Language->UnableToVoteKickFoundMoreThanOneMatch( Payload ) );
			}
		}

		//
		// !YES
		//

		if( Command == "yes" && !m_KickVotePlayer.empty( ) && player->GetName( ) != m_KickVotePlayer && !player->GetKickVote( ) )
		{
			player->SetKickVote( true );
			uint32_t VotesNeeded = (uint32_t)ceil( ( GetNumPlayers( ) - 1 ) * (float)m_GHost->m_VoteKickPercentage / 100 );
			uint32_t Votes = 0;

			for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
			{
				if( (*i)->GetKickVote( ) )
					Votes++;
			}

			if( Votes >= VotesNeeded )
			{
				CGamePlayer *Victim = GetPlayerFromName( m_KickVotePlayer, true );

				if( Victim )
				{
					Victim->SetDeleteMe( true );
					Victim->SetLeftReason( m_GHost->m_Language->WasKickedByVote( ) );

					if( !m_GameLoading && !m_GameLoaded )
						Victim->SetLeftCode( PLAYERLEAVE_LOBBY );
					else
						Victim->SetLeftCode( PLAYERLEAVE_LOST );

					if( !m_GameLoading && !m_GameLoaded )
						OpenSlot( GetSIDFromPID( Victim->GetPID( ) ), false );

					CONSOLE_Print( "[GAME: " + m_GameName + "] votekick against player [" + m_KickVotePlayer + "] passed with " + UTIL_ToString( Votes ) + "/" + UTIL_ToString( GetNumPlayers( ) ) + " votes" );
					SendAllChat( m_GHost->m_Language->VoteKickPassed( m_KickVotePlayer ) );
				}
				else
					SendAllChat( m_GHost->m_Language->ErrorVoteKickingPlayer( m_KickVotePlayer ) );

				m_KickVotePlayer.clear( );
				m_StartedKickVoteTime = 0;
			}
			else
				SendAllChat( m_GHost->m_Language->VoteKickAcceptedNeedMoreVotes( m_KickVotePlayer, User, UTIL_ToString( VotesNeeded - Votes ) ) );
		}*/
	}

	public void EventGameStarted() {
		super.EventGameStarted();

		// record everything we need to ban each player in case we decide to do so later
		// this is because when a player leaves the game an admin might want to ban that player
		// but since the player has already left the game we don't have access to their information anymore
		// so we create a "potential ban" for each player and only store it in the database if requested to by an admin

		//TODO ban
		//for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
		//	m_DBBans.push_back( new CDBBan( (*i)->GetSpoofedRealm( ), (*i)->GetName( ), (*i)->GetExternalIPString( ), string( ), string( ), string( ), string( ) ) );
	}

	public boolean IsGameDataSaved( ) {
		return false;
		//TODO save game
		//return m_CallableGameAdd && m_CallableGameAdd->GetReady( );
	}

	public void SaveGameData( ) {
		LOG.info( "[" + m_GameName + "] saving game data to database" );
		
		//TODO save game data
		//m_CallableGameAdd = m_GHost->m_DB->ThreadedGameAdd( m_GHost->m_BNETs.size( ) == 1 ? m_GHost->m_BNETs[0]->GetServer( ) : string( ), m_DBGame->GetMap( ), m_GameName, m_OwnerName, GetTime( ) - m_StartedLoadingTime, m_GameState, m_CreatorName, m_CreatorServer );
	}
	
}

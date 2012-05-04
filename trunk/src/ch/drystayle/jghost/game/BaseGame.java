package ch.drystayle.jghost.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.bnet.BnetConnection;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.i18n.Message;
import ch.drystayle.jghost.i18n.MessageFactory;
import ch.drystayle.jghost.i18n.Messages;
import ch.drystayle.jghost.map.Map;
import ch.drystayle.jghost.net.GhostServer;
import ch.drystayle.jghost.net.GhostTcpSocket;
import ch.drystayle.jghost.protocol.GameProtocol;
import ch.drystayle.jghost.protocol.IncomingAction;
import ch.drystayle.jghost.protocol.IncomingChatPlayer;
import ch.drystayle.jghost.protocol.IncomingJoinPlayer;
import ch.drystayle.jghost.protocol.IncomingMapSize;
import ch.drystayle.jghost.protocol.IncomingChatPlayer.ChatToHostType;
import ch.drystayle.jghost.util.TimeUtil;

public abstract class BaseGame {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BaseGame.class);
	
	//---- State
	
	private JGhost jGhost;
	protected GhostServer m_Socket;						// listening socket
	protected GameProtocol m_Protocol;					// game protocol
	protected List<GameSlot> m_Slots;					// vector of slots
	protected List<PotentialPlayer> m_Potentials;		// vector of potential players (connections that haven't sent a W3GS_REQJOIN packet yet)
	protected List<GamePlayer> m_Players;				// vector of players
	//List<CCallableScoreCheck *> m_ScoreChecks;
	protected Queue<IncomingAction> m_Actions;			// queue of actions to be sent
	protected List<String> m_Reserved;					// vector of player names with reserved slots (from the !hold command)
	protected Map m_Map;								// map data (this is a pointer to global data)
	protected SaveGame m_SaveGame;						// savegame data (this is a pointer to global data)
	//CReplay *m_Replay;								// replay
	protected boolean m_Exiting;						// set to true and this class will be deleted next update
	protected boolean m_Saving;							// if we're currently saving game data to the database
	protected int m_HostPort;							// the port to host games on
	protected char m_GameState;							// game state, public or private
	protected char m_VirtualHostPID;					// virtual host's PID
	protected char m_FakePlayerPID;						// the fake player's PID (if present)
	protected String m_GameName;						// game name
	protected String m_VirtualHostName;					// virtual host's name
	protected String m_OwnerName;						// name of the player who owns this game (should be considered an admin)
	protected String m_CreatorName;						// name of the player who created this game
	protected String m_CreatorServer;					// battle.net server the player who created this game was on
	protected String m_AnnounceMessage;					// a message to be sent every m_AnnounceInterval seconds
	protected String m_StatString;						// the stat string when the game started (used when saving replays)
	protected String m_KickVotePlayer;					// the player to be kicked with the currently running kick vote
	protected String m_PlayMode;
	protected int m_RandomSeed; 						// the random seed sent to the Warcraft III clients
	protected int m_HostCounter; 						// a unique game number
	protected int m_Latency; 							// the number of ms to wait between sending action packets (we queue any received during this time)
	protected int m_SyncLimit; 							// the maximum number of packets a player can fall out of sync before starting the lag screen
	protected int m_MaxSyncCounter; 					// the largest number of keepalives received from any one player (for determining if anyone is lagging)
	protected int m_GameTicks; // ingame ticks
	protected long m_CreationTime; // GetTime when the game was created
	protected long m_LastPingTime; // GetTime when the last ping was sent
	protected long m_LastRefreshTime; // GetTime when the last game refresh was sent
	protected long m_LastDownloadTicks; // GetTicks when the last map download cycle was performed
	protected long m_LastAnnounceTime; // GetTime when the last announce message was sent
	protected int m_AnnounceInterval; // how many seconds to wait between sending the m_AnnounceMessage
	protected long m_LastAutoStartTime; // the last time we tried to auto start the game
	protected int m_AutoStartPlayers; // auto start the game when there are this many players or more
	protected int m_LastCountDownTicks; // GetTicks when the last countdown message was sent
	protected int m_CountDownCounter; // the countdown is finished when this reaches zero
	protected int m_StartedLoadingTicks; // GetTicks when the game started loading
	protected long m_StartedLoadingTime; // GetTime when the game started loading
	protected int m_StartPlayers; // number of players when the game started
	protected int m_LastActionSentTicks; // GetTicks when the last action packet was sent
	protected long m_StartedLaggingTime; // GetTime when the last lag screen started
	protected long m_LastLagScreenTime; // GetTime when the last lag screen was active (continuously updated)
	protected long m_LastReservedSeen; // GetTime when the last reserved player was seen in the lobby
	protected int m_StartedKickVoteTime; // GetTime when the kick vote was started
	protected long m_GameOverTime; // GetTime when the game was over
	protected double m_MinimumScore; // the minimum allowed score for matchmaking mode
	protected double m_MaximumScore; // the maximum allowed score for matchmaking mode
	protected boolean m_Locked; // if the game owner is the only one allowed to run game commands or not
	protected boolean m_RefreshMessages; // if we should display "game refreshed..." messages or not
	protected boolean m_RefreshError; // if there was an error refreshing the game
	protected boolean m_MuteAll; // if we should stop forwarding ingame chat messages targeted for all players or not
	protected boolean m_MuteLobby; // if we should stop forwarding lobby chat messages
	protected boolean m_CountDownStarted; // if the game start countdown has started or not
	protected boolean m_GameLoading; // if the game is currently loading or not
	protected boolean m_GameLoaded; // if the game has loaded or not
	protected boolean m_Desynced; // if the game has desynced or not
	protected boolean m_Lagging; // if the lag screen is active or not
	protected boolean m_AutoSave; // if we should auto save the game before someone disconnects
	protected boolean m_GimpAll; // if all players are gimped
	protected boolean m_MatchMaking; // if matchmaking mode is enabled

	//---- Constructors
	
	public BaseGame (JGhost nGHost, Map nMap, SaveGame nSaveGame, short nHostPort, char nGameState, String nGameName, String nOwnerName, String nCreatorName, String nCreatorServer ) {
		this.setGhost(nGHost);
		m_Socket = new GhostServer();
		m_Protocol = new GameProtocol(this.getGhost());
		m_Map = nMap;
		m_SaveGame = nSaveGame;

		//TODO replay
		/*if ( m_GHost->m_SaveReplays && !m_SaveGame )
			m_Replay = new CReplay( m_GHost );
		else
			m_Replay = NULL;*/

		m_Exiting = false;
		m_Saving = false;
		m_HostPort = nHostPort;
		m_GameState = nGameState;
		m_VirtualHostPID = 255;
		m_FakePlayerPID = 255;
		m_GameName = nGameName;
		m_VirtualHostName = this.getGhost().getVirtualHostName();
		m_OwnerName = nOwnerName;
		m_CreatorName = nCreatorName;
		m_CreatorServer = nCreatorServer;
		m_RandomSeed = TimeUtil.getTicks( );
		m_HostCounter = this.getGhost().getHostCounter();
		this.getGhost().setHostCounter(this.m_HostCounter + 1);
		m_Latency = this.getGhost().getLatency();
		m_SyncLimit = this.getGhost().getSynclimit();
		m_MaxSyncCounter = 0;
		m_GameTicks = 0;
		m_CreationTime = TimeUtil.getTime();
		m_LastPingTime = TimeUtil.getTime();
		m_LastRefreshTime = TimeUtil.getTime();
		m_LastDownloadTicks = TimeUtil.getTime();
		m_LastAnnounceTime = 0;
		m_AnnounceInterval = 0;
		m_LastAutoStartTime = TimeUtil.getTime();
		m_AutoStartPlayers = 0;
		m_LastCountDownTicks = 0;
		m_CountDownCounter = 0;
		m_StartedLoadingTicks = 0;
		m_StartedLoadingTime = 0;
		m_StartPlayers = 0;
		m_LastActionSentTicks = 0;
		m_StartedLaggingTime = 0;
		m_LastLagScreenTime = 0;
		m_LastReservedSeen = TimeUtil.getTime();
		m_StartedKickVoteTime = 0;
		m_GameOverTime = 0;
		m_MinimumScore = 0.0;
		m_MaximumScore = 0.0;
		m_Locked = false;
		m_RefreshMessages = this.getGhost().isRefreshMessages();
		m_RefreshError = false;
		m_MuteAll = false;
		m_MuteLobby = false;
		m_CountDownStarted = false;
		m_GameLoading = false;
		m_GameLoaded = false;
		m_Desynced = false;
		m_Lagging = false;
		m_AutoSave = this.getGhost().isAutoSave();
		m_MatchMaking = false;	
		m_GimpAll = false;
		//TODO save game
		/*if( m_SaveGame )
		{
			m_Slots = m_SaveGame->GetSlots( );

			// the savegame slots contain player entries
			// we really just want the open/closed/computer entries
			// so open all the player slots

			for( vector<CGameSlot> :: iterator i = m_Slots.begin( ); i != m_Slots.end( ); i++ )
			{
				if( (*i).GetSlotStatus( ) == SLOTSTATUS_OCCUPIED && (*i).GetComputer( ) == 0 )
				{
					(*i).SetPID( 0 );
					(*i).SetDownloadStatus( 255 );
					(*i).SetSlotStatus( SLOTSTATUS_OPEN );
				}
			}
		} else*/
		m_Slots = m_Map.GetSlots( );
		m_Players = new ArrayList<GamePlayer>();
		m_Potentials = new ArrayList<PotentialPlayer>();
		m_Reserved = new ArrayList<String>();
		m_AnnounceMessage = "";
		m_KickVotePlayer = "";
		m_Actions = new LinkedList<IncomingAction>();
		
		// start listening for connections

		if( !this.getGhost().getBindAddress().isEmpty()) {
			LOG.info( "[" + m_GameName + "] attempting to bind to address [" + this.getGhost().getBindAddress() + "]" );
		} else {
			LOG.info( "[" + m_GameName + "] attempting to bind to all available addresses" );
		}
		
		if (m_Socket.Listen(this.getGhost().getBindAddress(), m_HostPort)) {
			LOG.info( "[" + m_GameName + "] listening on port " + m_HostPort);
		} else {
			LOG.error( "[" + m_GameName + "] error listening on port " + m_HostPort);
			m_Exiting = true;
		}
	}
	
	//---- Methods
	
	public SaveGame GetSaveGame( )			{ return m_SaveGame; }
	public int GetHostPort( )				{ return m_HostPort; }
	public char GetGameState( )				{ return m_GameState; }
	public String GetGameName( )			{ return m_GameName; }
	public String GetVirtualHostName( )		{ return m_VirtualHostName; }
	public String GetOwnerName( )			{ return m_OwnerName; }
	public String GetCreatorName( )			{ return m_CreatorName; }
	public String GetCreatorServer( )		{ return m_CreatorServer; }
	public int GetHostCounter( )			{ return m_HostCounter; }
	public long GetLastLagScreenTime( )		{ return m_LastLagScreenTime; }
	public boolean GetLocked( )				{ return m_Locked; }
	public boolean GetRefreshMessages( )	{ return m_RefreshMessages; }
	public boolean GetCountDownStarted( )	{ return m_CountDownStarted; }
	public boolean GetGameLoading( )		{ return m_GameLoading; }
	public boolean GetGameLoaded( )			{ return m_GameLoaded; }
	public boolean GetLagging( )			{ return m_Lagging; }
	
	public void SetExiting( boolean nExiting )					{ m_Exiting = nExiting; }
	public void SetAutoStartPlayers( int nAutoStartPlayers )	{ m_AutoStartPlayers = nAutoStartPlayers; }
	public void SetMinimumScore( double nMinimumScore )			{ m_MinimumScore = nMinimumScore; }
	public void SetMaximumScore( double nMaximumScore )			{ m_MaximumScore = nMaximumScore; }
	public void SetRefreshError( boolean nRefreshError )		{ m_RefreshError = nRefreshError; }
	public void SetPlayMode (String nPlayMode)					{ m_PlayMode = nPlayMode; }
	public void SetMatchMaking (boolean nMatchMaking )			{ m_MatchMaking = nMatchMaking; }
		
	public int GetSlotsOpen () {
		int NumSlotsOpen = 0;

		for (GameSlot slot : m_Slots) {
			if (slot.GetSlotStatus() == Constants.SLOTSTATUS_OPEN) {
				NumSlotsOpen++;
			}
		}

		return NumSlotsOpen;
	}
	
	public int GetNumPlayers () {
		int NumPlayers = 0;

		for (GamePlayer player : m_Players) {
			if (!player.GetLeftMessageSent()) {
				NumPlayers++;
			}
		}

		return NumPlayers;
	}
	
	public String GetDescription () {
		String Description = m_GameName + " : " + m_OwnerName + " : " + GetNumPlayers() + "/" + (m_GameLoading || m_GameLoaded ? m_StartPlayers : m_Slots.size());

		if( m_GameLoading || m_GameLoaded )
			Description += " : " + ((m_GameTicks / 1000 ) / 60) + "m";
		else
			Description += " : " + ((TimeUtil.getTime() - m_CreationTime) / 1000 / 60) + "m";

		return Description;
	}
	
		
	public void SetAnnounce (int interval, String message) {
		m_AnnounceInterval = interval;
		m_AnnounceMessage = message;
		m_LastAnnounceTime = TimeUtil.getTime();
	}
	
		// processing functions

	public int SetFD (Object fd, int nfds) {
		int NumFDs = 0;

		if (m_Socket != null) {
			m_Socket.SetFD(fd, nfds);
			NumFDs++;
		}

		for (PotentialPlayer player : m_Potentials) {
			if (player.GetSocket() != null) {
				player.GetSocket().SetFD(fd, nfds);
				NumFDs++;
			}
		}
		
		for (GamePlayer player : m_Players) {
			if (player.GetSocket() != null) {
				player.GetSocket().SetFD(fd, nfds);
				NumFDs++;
			}
		}

		return NumFDs;
	}
	
	public boolean Update (Object fd) {
		//TODO
		/*
		// update callables

	for( vector<CCallableScoreCheck *> :: iterator i = m_ScoreChecks.begin( ); i != m_ScoreChecks.end( ); )
	{
		if( (*i)->GetReady( ) )
		{
			double Score = (*i)->GetResult( );

			for( vector<CPotentialPlayer *> :: iterator j = m_Potentials.begin( ); j != m_Potentials.end( ); j++ )
			{
				if( (*j)->GetJoinPlayer( ) && (*j)->GetJoinPlayer( )->GetName( ) == (*i)->GetName( ) )
					EventPlayerJoinedWithScore( *j, (*j)->GetJoinPlayer( ), Score );
			}

			m_GHost->m_DB->RecoverCallable( *i );
			delete *i;
			i = m_ScoreChecks.erase( i );
		}
		else
			i++;
	}*/

	// update players

	
	
	for (Iterator<GamePlayer> iterator = this.m_Players.iterator(); iterator.hasNext();) {
		GamePlayer p = iterator.next();
		if( p.Update( fd ) ) {
			EventPlayerDeleted(p);
			p.delete();
			iterator.remove();
		}
	}
	
	for (Iterator<PotentialPlayer> iterator = this.m_Potentials.iterator(); iterator.hasNext();) {
		PotentialPlayer p = iterator.next();
		if (p.Update(fd)) {
			// flush the socket (e.g. in case a rejection message is queued)

			if (p.GetSocket() != null) {
				p.GetSocket().doSend();
			}
			p.delete();
			iterator.remove();
		}
	}

	// create the virtual host player

	if( !m_GameLoading && !m_GameLoaded && GetNumPlayers( ) < 12 )
		CreateVirtualHost();

	// unlock the game

	if( m_Locked && GetPlayerFromName(m_OwnerName, false) == null)
	{
		SendAllChat(MessageFactory.create(Messages.GAME_UNLOCKED.createMessage()));
		m_Locked = false;
	}

	// ping every 5 seconds
	// changed this to ping during game loading as well to hopefully fix some problems with people disconnecting during loading
	// changed this to ping during the game as well

	if (TimeUtil.getTime() >= m_LastPingTime + 5000) {
		// note: we must send pings to players who are downloading the map because Warcraft III disconnects from the lobby if it doesn't receive a ping every ~90 seconds
		// so if the player takes longer than 90 seconds to download the map they would be disconnected unless we keep sending pings
		// todotodo: ignore pings received from players who have recently finished downloading the map

		SendAll(m_Protocol.SEND_W3GS_PING_FROM_HOST());

		// we also broadcast the game to the local network every 5 seconds so we hijack this timer for our nefarious purposes
		// however we only want to broadcast if the countdown hasn't started
		// see the !sendlan code later in this file for some more information about how this works
		// todotodo: should we send a game cancel message somewhere? we'll need to implement a host counter for it to work

		if (!m_CountDownStarted) {
			Bytearray MapGameType = new Bytearray();

			// construct the correct W3GS_GAMEINFO packet

			//TODO implement for savegame
			/*if( m_SaveGame )
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
				m_GHost->m_UDPSocket->Broadcast( 6112, m_Protocol->SEND_W3GS_GAMEINFO( MapGameType, m_Map->GetMapGameFlags( ), MapWidth, MapHeight, m_GameName, "Varlock", GetTime( ) - m_CreationTime, "Save\\Multiplayer\\" + m_SaveGame->GetFileNameNoPath( ), m_SaveGame->GetMagicNumber( ), 12, 12, m_HostPort, m_HostCounter ) );
			}
			else
			{*/
				MapGameType.addChar(m_Map.GetMapGameType());
				MapGameType.addChar((char) 0);
				MapGameType.addChar((char) 0);
				MapGameType.addChar((char) 0);
				this.getGhost().getUdpSocket().Broadcast((short) 6112, m_Protocol.SEND_W3GS_GAMEINFO(MapGameType, m_Map.GetMapGameFlags( ), m_Map.GetMapWidth( ), m_Map.GetMapHeight( ), m_GameName, "Varlock",(int) (TimeUtil.getTime( ) - m_CreationTime), m_Map.GetMapPath(), m_Map.GetMapCRC(), 12, 12,(short) m_HostPort, m_HostCounter ) );
			//}
		}

		m_LastPingTime = TimeUtil.getTime();
	}

	// refresh every 3 seconds

	if( !m_RefreshError && !m_CountDownStarted && m_GameState == Constants.GAME_PUBLIC && GetSlotsOpen() > 0 && TimeUtil.getTime() >= m_LastRefreshTime + 3000) {
		// send a game refresh packet to each battle.net connection

		boolean Refreshed = false;

		
		for (BnetConnection conn : this.getGhost().getBnetConnections()) {
			// don't queue a game refresh message if the queue contains more than 1 packet because they're very low priority

			if (conn.GetOutPacketsQueued( ) <= 1 ) {
				conn.QueueGameRefresh(m_GameState, m_GameName, "", m_Map, m_SaveGame,(int) (TimeUtil.getTime( ) - m_CreationTime), m_HostCounter);
				Refreshed = true;
			}
		}

		// only print the "game refreshed" message if we actually refreshed on at least one battle.net server

		if (m_RefreshMessages && Refreshed) {
			SendAllChat(MessageFactory.create(Messages.GAME_REFRESHED.createMessage()));
		}

		m_LastRefreshTime = TimeUtil.getTime();
	}
	
	// send more map data

	if( !m_GameLoading && !m_GameLoaded && TimeUtil.getTicks() >= m_LastDownloadTicks + 250 )
	{
		int Downloaders = 0;
		int DownloadCounter = 0;

		for (GamePlayer p : this.m_Players) {
			if (p.GetDownloadStarted( ) && !p.GetDownloadFinished()) {
				Downloaders++;

				if (this.getGhost().getMaxDownloaders() > 0 && Downloaders > this.getGhost().getMaxDownloaders())
					break;

				// send up to 50 pieces of the map at once so that the download goes faster
				// if we wait for each MAPPART packet to be acknowledged by the client it'll take a long time to download
				// this is because we would have to wait the round trip time (the ping time) between sending every 1442 bytes of map data
				// doing it this way allows us to send at least 70 KB in each round trip interval which is much more reasonable
				// the theoretical throughput is [70 KB * 1000 / ping] in KB/sec so someone with 100 ping (round trip ping, not LC ping) could download at 700 KB/sec

				int MapSize = m_Map.GetMapSize().toInt();

				while (p.GetLastMapPartSent( ) < p.GetLastMapPartAcked() + 1442 * 50 && p.GetLastMapPartSent() < MapSize ) {
					// limit the download speed if we're sending too much data
					// we divide by 4 because we run this code every 250ms (i.e. four times per second)

					if (this.getGhost().getMaxDownloadSpeed() > 0 && DownloadCounter > this.getGhost().getMaxDownloadSpeed() * 1024 / 4 )
						break;

					Send(p, m_Protocol.SEND_W3GS_MAPPART( GetHostPID(), p.GetPID( ), p.GetLastMapPartSent(), m_Map.getByteMapData()));
					p.SetLastMapPartSent(p.GetLastMapPartSent() + 1442 );
					DownloadCounter += 1442;
				}
			}
		}

		m_LastDownloadTicks = TimeUtil.getTicks();
	}
	
	// announce every m_AnnounceInterval seconds

	if( !m_AnnounceMessage.isEmpty( ) && !m_CountDownStarted && TimeUtil.getTime( ) >= m_LastAnnounceTime + m_AnnounceInterval ) {
		SendAllChat(MessageFactory.create(m_AnnounceMessage));
		m_LastAnnounceTime = TimeUtil.getTime( );
	}
	
	// kick players who don't spoof check within 20 seconds when matchmaking is enabled

	if( !m_CountDownStarted && m_MatchMaking && m_AutoStartPlayers != 0 && !m_Map.GetMapMatchMakingCategory().isEmpty( ) && m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM && this.getGhost().getBnetConnections().size() == 1) {
		for (GamePlayer p : this.m_Players) {
			if( !p.GetSpoofed() && TimeUtil.getTime( ) >= p.GetJoinTime( ) + 20000)
			{
				p.SetDeleteMe(true);
				p.SetLeftReason(Messages.WAS_KICKED_FOR_NOT_SPOOF_CHECKING.createMessage());
				p.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
				OpenSlot(GetSIDFromPID(p.GetPID()), false );
			}
		}
	}
	
	// try to auto start every 10 seconds

	if( m_AutoStartPlayers != 0 && TimeUtil.getTime( ) >= m_LastAutoStartTime + 10000)
	{
		// require spoof checks when using matchmaking

		if( m_MatchMaking && m_AutoStartPlayers != 0 && !m_Map.GetMapMatchMakingCategory().isEmpty() && m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM && this.getGhost().getBnetConnections().size( ) == 1 )
		{
			int PlayersScored = 0;
			int PlayersNotScored = 0;
			double AverageScore = 0.0;
			double MinScore = 0.0;
			double MaxScore = 0.0;
			boolean Found = false;

			for (GamePlayer p : this.m_Players) {
				if (p.GetScore( ) < -99999.0 ) {
					PlayersNotScored++;
				} else {
					PlayersScored++;
					AverageScore += p.GetScore( );

					if( !Found || p.GetScore( ) < MinScore )
						MinScore = p.GetScore( );

					if( !Found || p.GetScore( ) > MaxScore )
						MaxScore = p.GetScore( );

					Found = true;
				}
			}

			double Spread = MaxScore - MinScore;

			// todotodo: don't start the countdown if the spread is too large

			StartCountDownAuto( true );
		}
		else
			StartCountDownAuto( false );

		m_LastAutoStartTime = TimeUtil.getTime();
	}
	
	// countdown every 500 ms

	if( m_CountDownStarted && TimeUtil.getTicks() >= (m_LastCountDownTicks + 500)) {
		if( m_CountDownCounter > 0 )
		{
			// we use a countdown counter rather than a "finish countdown time" here because it might alternately round up or down the count
			// this sometimes resulted in a countdown of e.g. "6 5 3 2 1" during my testing which looks pretty dumb
			// doing it this way ensures it's always "5 4 3 2 1" but each interval might not be *exactly* the same length

			SendAllChat(MessageFactory.create(m_CountDownCounter + ". . ."));
			m_CountDownCounter--;
		} else if( !m_GameLoading && !m_GameLoaded ) {
			m_StartedLoadingTicks = TimeUtil.getTicks();
			m_StartedLoadingTime = TimeUtil.getTime();
			m_GameLoading = true;
			EventGameStarted( );
		}

		m_LastCountDownTicks = TimeUtil.getTicks();
	}
	
	// check if the lobby is "abandoned" and needs to be closed since it will never start

	if( !m_GameLoading && !m_GameLoaded && m_AutoStartPlayers == 0 && this.getGhost().getLobbyTimeLimit() > 0) {
		// check if there's a player with reserved status in the game

		for (GamePlayer p : this.m_Players) {
			if (p.GetReserved( ) )
				m_LastReservedSeen = TimeUtil.getTime();
		}

		// check if we've hit the time limit

		if (TimeUtil.getTime() >= m_LastReservedSeen + this.getGhost().getLobbyTimeLimit() * 60 * 1000) {
			LOG.info("[" + m_GameName + "] is over (lobby time limit hit)" );
			return true;
		}
	}
	
	// check if the game is loaded

	if( m_GameLoading )
	{
		boolean FinishedLoading = true;

		for (GamePlayer p : this.m_Players) {
			FinishedLoading = p.GetFinishedLoading();

			if( !FinishedLoading )
				break;
		}

		if( FinishedLoading )
		{
			m_LastActionSentTicks = TimeUtil.getTicks();
			m_GameLoading = false;
			m_GameLoaded = true;
			EventGameLoaded( );
		}
	}

	// keep track of the largest sync counter (the number of keepalive packets received by each player)
	// if anyone falls behind by more than m_SyncLimit keepalives we start the lag screen
	
	if( m_GameLoaded )
	{
		// calculate the largest sync counter

		for (GamePlayer p : this.m_Players) {
			if (p.GetSyncCounter( ) > m_MaxSyncCounter)
				m_MaxSyncCounter = p.GetSyncCounter();
		}

		// check if anyone has started lagging
		// we consider a player to have started lagging if they're more than m_SyncLimit keepalives behind

		if( !m_Lagging )
		{
			String LaggingString = "";

			for (GamePlayer p : this.m_Players) {
				if( m_MaxSyncCounter - p.GetSyncCounter( ) > m_SyncLimit )
				{
					p.SetLagging( true );
					p.SetStartedLaggingTicks(TimeUtil.getTicks());
					m_Lagging = true;
					m_StartedLaggingTime = TimeUtil.getTime( );

					if( LaggingString.isEmpty())
						LaggingString = p.GetName( );
					else
						LaggingString += ", " + p.GetName( );
				}
			}

			if( m_Lagging )
			{
				// start the lag screen

				LOG.info( "[" + m_GameName + "] started lagging on [" + LaggingString + "]" );
				SendAll(m_Protocol.SEND_W3GS_START_LAG(m_Players));

				// reset everyone's drop vote

				for (GamePlayer p : this.m_Players) {
					p.SetDropVote(false);
				}
			}
		}
		
		if( m_Lagging )
		{
			// we cannot allow the lag screen to stay up for more than ~65 seconds because Warcraft III disconnects if it doesn't receive an action packet at least this often
			// one (easy) solution is to simply drop all the laggers if they lag for more than 60 seconds, which is what we do here

			if (TimeUtil.getTime() >= m_StartedLaggingTime + 60000)
				StopLaggers( "was automatically dropped after 60 seconds" );

			// check if anyone has stopped lagging normally
			// we consider a player to have stopped lagging if they're less than half m_SyncLimit keepalives behind

			for (GamePlayer p : this.m_Players) {
				if(p.GetLagging( ) && m_MaxSyncCounter - p.GetSyncCounter( ) < m_SyncLimit / 2) {
					// stop the lag screen for this player

					LOG.info("[" + m_GameName + "] stopped lagging on [" + p.GetName() + "]" );
					SendAll( m_Protocol.SEND_W3GS_STOP_LAG(p));
					p.SetLagging( false );
					p.SetStartedLaggingTicks(0);
				}
			}

			// check if everyone has stopped lagging

			boolean Lagging = false;

			for (GamePlayer p : this.m_Players) {
				if (p.GetLagging()) {
					Lagging = true;
				}
			}

			m_Lagging = Lagging;

			// reset m_LastActionSentTicks because we want the game to stop running while the lag screen is up

			m_LastActionSentTicks = TimeUtil.getTicks();

			// keep track of the last lag screen time so we can avoid timing out players

			m_LastLagScreenTime = TimeUtil.getTime();
		}
	}
	
	// send actions every m_Latency milliseconds
	// actions are at the heart of every Warcraft 3 game but luckily we don't need to know their contents to relay them
	// we queue player actions in EventPlayerAction then just resend them in batches to all players here

	if( m_GameLoaded && !m_Lagging && TimeUtil.getTicks() >= m_LastActionSentTicks + m_Latency )
		SendAllActions( );

	// expire the votekick
	
	if( !m_KickVotePlayer.isEmpty( ) && TimeUtil.getTime( ) >= m_StartedKickVoteTime + 60000 )
	{
		LOG.info( "[" + m_GameName + "] votekick against player [" + m_KickVotePlayer + "] expired" );
		SendAllChat(MessageFactory.create(Messages.VOTE_KICK_EXPIRED, m_KickVotePlayer));
		m_KickVotePlayer = "";
		m_StartedKickVoteTime = 0;
	}
	
	// start the gameover timer if there's only one player left

	if( m_Players.size( ) == 1 && m_FakePlayerPID == 255 && m_GameOverTime == 0 && ( m_GameLoading || m_GameLoaded ) )
	{
		LOG.info("[" + m_GameName + "] gameover timer started (one player left)" );
		m_GameOverTime = TimeUtil.getTime( );
	}

	// finish the gameover timer

	if( m_GameOverTime != 0 && TimeUtil.getTime( ) >= m_GameOverTime + 60000) {
		boolean AlreadyStopped = true;

		for (GamePlayer p : this.m_Players) {
			if(!p.GetDeleteMe()) {
				AlreadyStopped = false;
				break;
			}
		}

		if( !AlreadyStopped )
		{
			LOG.info("[" + m_GameName + "] is over (gameover timer finished)" );
			StopPlayers( "was disconnected (gameover timer finished)" );
		}
	}

	// end the game if there aren't any players left

	if( m_Players.isEmpty( ) && ( m_GameLoading || m_GameLoaded ) )
	{
		if (!m_Saving) {
			LOG.info("[" + m_GameName + "] is over (no players left)" );
			SaveGameData( );
			m_Saving = true;
		} else if( IsGameDataSaved( ) ) {
			return true;
		}
	}

	// accept new connections
	
	if( m_Socket != null) {
		GhostTcpSocket NewSocket = m_Socket.Accept(fd);

		if (NewSocket != null) {
			LOG.debug("New Connection on port: " + this.m_HostPort);
			m_Potentials.add(new PotentialPlayer( m_Protocol, this, NewSocket));
		}

		if( m_Socket.HasError()) {
			return true;
		}
	}

	return m_Exiting;
	}
	
	public void UpdatePost () {
		// we need to manually call DoSend on each player now because CGamePlayer :: Update doesn't do it
		// this is in case player 2 generates a packet for player 1 during the update but it doesn't get sent because player 1 already finished updating
		// in reality since we're queueing actions it might not make a big difference but oh well

		for (GamePlayer player : m_Players) {
			if (player.GetSocket() != null) {
				player.GetSocket().doSend();
			}
		}
		
		for (PotentialPlayer player : m_Potentials) {
			if (player.GetSocket() != null) {
				player.GetSocket().doSend();
			}
		}
	}
	
	// generic functions to send packets to players

	public void Send (GamePlayer player, Bytearray data) {
		if (player != null && player.GetSocket() != null) {
			player.GetSocket().PutBytes(data);
		}
	}

	public void SendAll (Bytearray data) {
		for (GamePlayer player : m_Players) {
			if (player.GetSocket() != null) {
				player.GetSocket().PutBytes(data);
			}
		}
	}
	
	public void Send (char PID, Bytearray data ) {
		Send(GetPlayerFromPID(PID), data);
	}

	public void Send (Bytearray PIDs, Bytearray data ) {
		for (int i = 0; i < PIDs.size(); i++ ) {
			Send(PIDs.getChar(i), data );
		}
	}
	
	public void SendChat (char fromPID, char toPID, Message message) {
		SendChat(fromPID, GetPlayerFromPID(toPID), message);
	}
	
	public void SendChat (GamePlayer player, Message message) {
		SendChat(GetHostPID(), player, message);
	}

	public void SendChat (char toPID, Message message) {
		SendChat(GetHostPID(), toPID, message);
	}
	
	public void SendChat (char fromPID, GamePlayer player, Message m) {
		// send a private message to one player - it'll be marked [Private] in Warcraft 3

		if (player != null) {
			//Convert Message to String
			String message = m.toString(player.getLocale());
			if (!m_GameLoaded) {
				if (message.length() > 254) {
					message = message.substring(0, 254);
				}
					
				Send(player, m_Protocol.SEND_W3GS_CHAT_FROM_HOST(fromPID, new Bytearray(player.GetPID()),(char) 16, new Bytearray(), message));
			} else {
				char[] ExtraFlags = { 3, 0, 0, 0 };

				// based on my limited testing it seems that the extra flags' first byte contains 3 plus the recipient's colour to denote a private message

				char SID = GetSIDFromPID(player.GetPID());

				if( SID < m_Slots.size( ) )
					ExtraFlags[0] = (char) (3 + m_Slots.get(SID).GetColour());

				if( message.length( ) > 127 )
					message = message.substring( 0, 127 );

				Send( player, m_Protocol.SEND_W3GS_CHAT_FROM_HOST( fromPID, new Bytearray(player.GetPID()),(char) 32, new Bytearray(ExtraFlags), message));
			}
		}
	}
	
	public void SendAllChat (char fromPID, Message m) {
		// send a public message to all players - it'll be marked [All] in Warcraft 3

		if( GetNumPlayers( ) > 0 ) {
			//String message = m.toString();
			if( !m_GameLoaded ) {
				//if( message.length( ) > 254 )
				//	message = message.substring( 0, 254 );

				// this is a lobby ghost chat message
				for (GamePlayer player : m_Players) {
					String message = m.toString(player.getLocale());
					if( message.length( ) > 254 ) {
						message = message.substring( 0, 254 );
					}
					Send(player, m_Protocol.SEND_W3GS_CHAT_FROM_HOST( fromPID, GetPIDs(),(char) 16, new Bytearray(), message));
				}
				//m_Protocol.SEND_W3GS_CHAT_FROM_HOST( fromPID, GetPIDs(),(char) 16, new Bytearray(), message)
			} else {
				//if( message.length() > 127 )
				//	message = message.substring(0, 127);

				// this is an ingame ghost chat message, print it to the console

				LOG.info( "[GAME: " + m_GameName + "] [Local]: " + m.toString());
				for (GamePlayer player : m_Players) {
					String message = m.toString(player.getLocale());
					if( message.length() > 127 ) {
						message = message.substring(0, 127);
					}
					Send(player, m_Protocol.SEND_W3GS_CHAT_FROM_HOST( fromPID, GetPIDs(),(char) 32, new Bytearray(0), message));
				}
				//SendAll(m_Protocol.SEND_W3GS_CHAT_FROM_HOST( fromPID, GetPIDs(),(char) 32, new Bytearray(0), message));

				//TODO replay
				/*if( m_Replay ) {
					m_Replay->AddChatMessage( fromPID, 32, 0, message );
				}*/
			}
		}
	}

	public void SendAllChat (Message message) {
		SendAllChat(GetHostPID( ), message);
	}
	
	public void SendAllSlotInfo () {
		if( !m_GameLoading && !m_GameLoaded ) {
			SendAll(m_Protocol.SEND_W3GS_SLOTINFO(
				m_Slots, m_RandomSeed,(char) (m_Map.GetMapGameType( ) == Constants.GAMETYPE_CUSTOM ? 3 : 0),(char) m_Map.GetMapNumPlayers() 
			));
		}
	}
	
	public void SendVirtualHostPlayerInfo (GamePlayer player) {
		if (m_VirtualHostPID == 255) {
			return;
		}

		Bytearray IP = new Bytearray();
		IP.addInt(0);
		Send(player, m_Protocol.SEND_W3GS_PLAYERINFO( m_VirtualHostPID, m_VirtualHostName, IP, IP ));
	}
	
	public void SendFakePlayerInfo (GamePlayer player) {
		if( m_FakePlayerPID == 255 )
			return;

		Bytearray IP = new Bytearray();
		IP.addInt(0);
		Send(player, m_Protocol.SEND_W3GS_PLAYERINFO( m_FakePlayerPID, "FakePlayer", IP, IP));
	}
	
	public void SendAllActions () {
		short SendInterval = (short) (TimeUtil.getTicks() - m_LastActionSentTicks);
		m_GameTicks += SendInterval;

		// add actions to replay

		//TODO
		/*if( m_Replay )
			m_Replay->AddTimeSlot( SendInterval, m_Actions );
		*/

		// we aren't allowed to send more than 1460 bytes in a single packet but it's possible we might have more than that many bytes waiting in the queue

		if (!m_Actions.isEmpty()) {
			// we use a "sub actions queue" which we keep adding actions to until we reach the size limit
			// start by adding one action to the sub actions queue

			Queue<IncomingAction> SubActions = new LinkedList<IncomingAction>(); ;
			IncomingAction Action = m_Actions.poll();
			SubActions.add( Action );
			int SubActionsLength = Action.GetLength();

			while( !m_Actions.isEmpty( ) ) {
				Action = m_Actions.poll();

				// check if adding the next action to the sub actions queue would put us over the limit (1452 because the INCOMING_ACTION and INCOMING_ACTION2 packets use an extra 8 bytes)

				if( SubActionsLength + Action.GetLength( ) > 1452 )
				{
					// we'd be over the limit if we added the next action to the sub actions queue
					// so send everything already in the queue and then clear it out
					// the W3GS_INCOMING_ACTION2 packet handles the overflow but it must be sent *before* the corresponding W3GS_INCOMING_ACTION packet

					SendAll(m_Protocol.SEND_W3GS_INCOMING_ACTION2(SubActions));

					while( !SubActions.isEmpty( ) )
					{
						SubActions.poll();
					}

					SubActionsLength = 0;
				}

				SubActions.add(Action);
				SubActionsLength += Action.GetLength();
			}

			SendAll( m_Protocol.SEND_W3GS_INCOMING_ACTION( SubActions, SendInterval ) );

			SubActions.clear();
		} else {
			SendAll( m_Protocol.SEND_W3GS_INCOMING_ACTION( m_Actions, SendInterval ) );
		}

		m_LastActionSentTicks = TimeUtil.getTicks( );
	}

	public void SendWelcomeMessage (GamePlayer player) {
		//TODO make custom welcome message
		SendChat(player, MessageFactory.create("GHost++" ));
		SendChat(player, MessageFactory.create("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-" ));
		SendChat(player, MessageFactory.create("          Game Name:     " + m_GameName ));
	}
	
	public void SendEndMessage () {
		//TODO make custom end message
	}
	
	// events
	// note: these are only called while iterating through the m_Potentials or m_Players vectors
	// therefore you can't modify those vectors and must use the player's m_DeleteMe member to flag for deletion

	public void EventPlayerDeleted (GamePlayer player) {
		LOG.info("[" + m_GameName + "] deleting player [" + player.GetName() + "]: " + player.GetLeftReason());

		// in some cases we're forced to send the left message early so don't send it again

		if (player.GetLeftMessageSent()) {
			return;
		}

		if (m_GameLoaded) {
			SendAllChat(MessageFactory.create(player.GetName() + " " + player.GetLeftReason() + "."));
		}
			
		if (player.GetLagging()) {
			SendAll(m_Protocol.SEND_W3GS_STOP_LAG(player));
		}
			
		// autosave
		//TODO
		/*if( m_AutoSave && m_GameLoaded && player->GetLeftCode( ) == PLAYERLEAVE_DISCONNECT )
		{
			string SaveGameName = UTIL_FileSafeName( "GHost++ AutoSave " + m_GameName + " (" + player->GetName( ) + ").w3z" );
			CONSOLE_Print( "[GAME: " + m_GameName + "] auto saving [" + SaveGameName + "] before player drop, shortened send interval = " + UTIL_ToString( GetTicks( ) - m_LastActionSentTicks ) );
			BYTEARRAY CRC;
			BYTEARRAY Action;
			Action.push_back( 6 );
			UTIL_AppendByteArray( Action, SaveGameName );
			m_Actions.push( new CIncomingAction( player->GetPID( ), CRC, Action ) );
			SendAllActions( );
		}*/

		// tell everyone about the player leaving

		SendAll(m_Protocol.SEND_W3GS_PLAYERLEAVE_OTHERS(player.GetPID(), player.GetLeftCode()));

		// set the replay's host PID and name to the last player to leave the game
		// this will get overwritten as each player leaves the game so it will eventually be set to the last player

		//TODO replay part missing
		/*if ( m_Replay && ( m_GameLoading || m_GameLoaded ) ) {
			m_Replay.SetHostPID(player.GetPID());
			m_Replay.SetHostName(player.GetName());

			// add leave message to replay

			m_Replay.AddLeaveGame( 1, player->GetPID( ), player->GetLeftCode( ) );
		}*/

		// abort the countdown if there was one in progress

		if( m_CountDownStarted && !m_GameLoading && !m_GameLoaded ) {
			SendAllChat(MessageFactory.create(Messages.COUNTDOWN_ABORTED));
			m_CountDownStarted = false;
		}

		// abort the votekick

		if (!m_KickVotePlayer.isEmpty()) {
			SendAllChat(MessageFactory.create(Messages.VOTE_KICK_CANCELLED, m_KickVotePlayer));
		}

		m_KickVotePlayer = "";
		m_StartedKickVoteTime = 0;
	}
	
	public void EventPlayerDisconnectTimedOut (GamePlayer player) {
		// not only do we not do any timeouts if the game is lagging, we allow for an additional grace period of 10 seconds
		// this is because Warcraft 3 stops sending packets during the lag screen
		// so when the lag screen finishes we would immediately disconnect everyone if we didn't give them some extra time

		if (TimeUtil.getTime() >= m_LastLagScreenTime + 10000 ) {
			player.SetDeleteMe( true );
			player.SetLeftReason(Messages.HAS_LOST_CONNECTION_TIMED_OUT.createMessage());
			player.SetLeftCode(Constants.PLAYERLEAVE_DISCONNECT);

			if( !m_GameLoading && !m_GameLoaded ) {
				OpenSlot(GetSIDFromPID( player.GetPID( ) ), false);
			}
		}
	}
	
	public void EventPlayerDisconnectPlayerError (GamePlayer player) {
		// at the time of this comment there's only one player error and that's when we receive a bad packet from the player
		// since TCP has checks and balances for data corruption the chances of this are pretty slim

		player.SetDeleteMe( true );
		player.SetLeftReason(Messages.HAS_LOST_CONNECTION_PLAYER_ERROR.createMessage(player.GetErrorString()));
		player.SetLeftCode(Constants.PLAYERLEAVE_DISCONNECT);

		if (!m_GameLoading && !m_GameLoaded) {
			OpenSlot(GetSIDFromPID(player.GetPID()), false);
		}
	}
	
	public void EventPlayerDisconnectSocketError (GamePlayer player) {
		player.SetDeleteMe(true);
		player.SetLeftReason(Messages.HAS_LOST_CONNECTION_SOCKET_ERROR.createMessage(player.GetSocket().getErrorString()));
		player.SetLeftCode(Constants.PLAYERLEAVE_DISCONNECT);

		if( !m_GameLoading && !m_GameLoaded ) {
			OpenSlot(GetSIDFromPID(player.GetPID()), false);
		}
	}
	
	public void EventPlayerDisconnectConnectionClosed (GamePlayer player) {
		player.SetDeleteMe( true );
		player.SetLeftReason(Messages.HAS_LOST_CONNECTION_CLOSED_BY_REMOTE_HOST.createMessage());
		player.SetLeftCode(Constants.PLAYERLEAVE_DISCONNECT);

		if( !m_GameLoading && !m_GameLoaded ) {
			OpenSlot(GetSIDFromPID(player.GetPID()), false);
		}
	}
	
	public void EventPlayerJoined (PotentialPlayer potential, IncomingJoinPlayer joinPlayer) {
		// check if the new player's name is empty or too long

		if (joinPlayer.GetName( ).isEmpty( ) || joinPlayer.GetName( ).length( ) > 15) {
			LOG.info("[" + m_GameName + "] player [" + joinPlayer.GetName() + "] is trying to join the game with an invalid name of length " + joinPlayer.GetName().length());
			potential.GetSocket().PutBytes(m_Protocol.SEND_W3GS_REJECTJOIN(Constants.REJECTJOIN_FULL));
			potential.SetDeleteMe(true);
			return;
		}

		// check if the new player's name is the same as the virtual host name

		if (joinPlayer.GetName().equals(m_VirtualHostName)) {
			LOG.info("[" + m_GameName + "] player [" + joinPlayer.GetName( ) + "] is trying to join the game with the virtual host name" );
			potential.GetSocket().PutBytes(m_Protocol.SEND_W3GS_REJECTJOIN(Constants.REJECTJOIN_FULL));
			potential.SetDeleteMe(true);
			return;
		}

		// check if the new player's name is already taken

		if (GetPlayerFromName(joinPlayer.GetName(), false) != null) {
			LOG.info("[" + m_GameName + "] player [" + joinPlayer.GetName( ) + "] is trying to join the game but that name is already taken" );
			potential.GetSocket().PutBytes(m_Protocol.SEND_W3GS_REJECTJOIN(Constants.REJECTJOIN_FULL));
			potential.SetDeleteMe(true);
			return;
		}

		// check if the new player's name is banned

		//TODO check for ban
		/*
		for( vector<CBNET *> :: iterator i = m_GHost->m_BNETs.begin( ); i != m_GHost->m_BNETs.end( ); i++ )
		{
			CDBBan *Ban = (*i)->IsBanned( joinPlayer->GetName( ) );

			if( Ban )
			{
				CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but is banned" );
				SendAllChat( m_GHost->m_Language->TryingToJoinTheGameButBanned( joinPlayer->GetName( ) ) );
				potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
				potential->SetDeleteMe( true );
				return;
			}
		}*/

		if( m_MatchMaking && m_AutoStartPlayers != 0 && !m_Map.GetMapMatchMakingCategory( ).isEmpty( ) && m_Map.GetMapGameType( ) == Constants.GAMETYPE_CUSTOM && getGhost().getBnetConnections().size()  == 1 ) {
			// matchmaking is enabled
			// start a database query to determine the player's score
			// when the query is complete we will call EventPlayerJoinedWithScore

			//TODO matchmaking code...
			//m_ScoreChecks.push_back( m_GHost->m_DB->ThreadedScoreCheck( m_Map->GetMapMatchMakingCategory( ), joinPlayer->GetName( ), m_GHost->m_BNETs[0]->GetServer( ) ) );
			return;
		}

		// try to find an empty slot

		char SID = GetEmptySlot( false );

		// check if the player is an admin or root admin on any connected realm for determining reserved status
		// we can't just use the spoof checked realm like in EventPlayerBotCommand because the player hasn't spoof checked yet

		boolean AnyAdminCheck = false;

		for (BnetConnection connection : this.getGhost().getBnetConnections()) {
			if (connection.IsAdmin(joinPlayer.GetName()) || connection.IsRootAdmin(joinPlayer.GetName())) {
				AnyAdminCheck = true;
				break;
			}
		}

		boolean Reserved = IsReserved(joinPlayer.GetName()) || AnyAdminCheck || IsOwner(joinPlayer.GetName());

		if( SID == 255 && Reserved )
		{
			// a reserved player is trying to join the game but it's full, try to find a reserved slot

			SID = GetEmptySlot( true );

			if (SID != 255) {
				GamePlayer KickedPlayer = GetPlayerFromSID( SID );

				if (KickedPlayer != null) {
					KickedPlayer.SetDeleteMe(true);
					KickedPlayer.SetLeftReason(Messages.WAS_KICKED_FOR_RESERVED_PLAYER.createMessage(joinPlayer.GetName()));
					KickedPlayer.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);

					// send a playerleave message immediately since it won't normally get sent until the player is deleted which is after we send a playerjoin message
					// we don't need to call OpenSlot here because we're about to overwrite the slot data anyway

					SendAll(m_Protocol.SEND_W3GS_PLAYERLEAVE_OTHERS( KickedPlayer.GetPID( ), KickedPlayer.GetLeftCode()));
					KickedPlayer.SetLeftMessageSent( true );
				}
			}
		}

		if (SID == 255 && IsOwner(joinPlayer.GetName())) {
			// the owner player is trying to join the game but it's full and we couldn't even find a reserved slot, kick the player in the lowest numbered slot
			// updated this to try to find a player slot so that we don't end up kicking a computer

			SID = 0;

			for (char i = 0; i < m_Slots.size( ); i++ ) {
				if (m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && m_Slots.get(i).GetComputer() == 0) {
					SID = i;
					break;
				}
			}

			GamePlayer KickedPlayer = GetPlayerFromSID(SID);

			if (KickedPlayer != null) {
				KickedPlayer.SetDeleteMe( true );
				KickedPlayer.SetLeftReason( Messages.WAS_KICKED_FOR_OWNER_PLAYER.createMessage(joinPlayer.GetName()));
				KickedPlayer.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);

				// send a playerleave message immediately since it won't normally get sent until the player is deleted which is after we send a playerjoin message
				// we don't need to call OpenSlot here because we're about to overwrite the slot data anyway

				SendAll(m_Protocol.SEND_W3GS_PLAYERLEAVE_OTHERS(KickedPlayer.GetPID( ), KickedPlayer.GetLeftCode()));
				KickedPlayer.SetLeftMessageSent(true);
			}
		}

		if( SID >= m_Slots.size( ) )
		{
			potential.GetSocket().PutBytes(m_Protocol.SEND_W3GS_REJECTJOIN(Constants.REJECTJOIN_FULL));
			potential.SetDeleteMe( true );
			return;
		}

		// we have a slot for the new player
		// make room for them by deleting the virtual host player if we have to

		if (GetNumPlayers() >= 11) {
			DeleteVirtualHost();
		}

		// turning the PotentialPlayer into a GamePlayer is a bit of a pain because we have to be careful not to close the socket
		// this problem is solved by setting the socket to NULL before deletion and handling the NULL case in the destructor
		// we also have to be careful to not modify the m_Potentials vector since we're currently looping through it

		LOG.info( "[" + m_GameName + "] player [" + joinPlayer.GetName() + "] joined the game" );
		GamePlayer Player = new GamePlayer( potential, GetNewPID( ), joinPlayer.GetName( ), joinPlayer.GetInternalIP(), Reserved);

		// consider the owner player to have already spoof checked
		// we will still attempt to spoof check them if it's enabled but this allows owners connecting over LAN to access admin commands

		if (IsOwner(joinPlayer.GetName())) {
			Player.SetSpoofed( true );
		}

		m_Players.add(Player);
		//set socket to null, otherwise it would be closed when the potential player is deleted
		potential.SetSocket(null);
		potential.SetDeleteMe(true);

		if ( m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			m_Slots.set(SID, new GameSlot( Player.GetPID(),(char) 255, Constants.SLOTSTATUS_OCCUPIED,(char) 0, m_Slots.get(SID).GetTeam( ), m_Slots.get(SID).GetColour( ), m_Slots.get(SID).GetRace()));
		} else {
			m_Slots.set(SID, new GameSlot( Player.GetPID(),(char) 255, Constants.SLOTSTATUS_OCCUPIED,(char) 0,(char) 12,(char) 12, Constants.SLOTRACE_RANDOM));

			// try to pick a team and colour
			// make sure there aren't too many other players already

			char NumOtherPlayers = 0;

			for(char i = 0; i < m_Slots.size( ); i++ ) {
				if( m_Slots.get(i).GetSlotStatus() == Constants.SLOTSTATUS_OCCUPIED && m_Slots.get(i).GetTeam( ) != 12) {
					NumOtherPlayers++;
				}
			}

			if( NumOtherPlayers < m_Map.GetMapNumPlayers()) {
				if( SID < m_Map.GetMapNumTeams()) {
					m_Slots.get(SID).SetTeam(SID);
				} else {
					m_Slots.get(SID).SetTeam((char) 0);
				}

				m_Slots.get(SID).SetColour(GetNewColour());
			}
		}

		// send slot info to the new player
		// the SLOTINFOJOIN packet also tells the client their assigned PID and that the join was successful

		Player.GetSocket().PutBytes(m_Protocol.SEND_W3GS_SLOTINFOJOIN(
			Player.GetPID( ), 
			Player.GetSocket().GetPort(), 
			Player.GetExternalIP(), 
			m_Slots, 
			m_RandomSeed,
			(char) (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM ? 3 : 0), 
			(char) m_Map.GetMapNumPlayers())
		);

		// send virtual host info and fake player info (if present) to the new player

		SendVirtualHostPlayerInfo(Player);
		SendFakePlayerInfo(Player);

		Bytearray BlankIP = new Bytearray();
		BlankIP.addInt( 0 );

		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent( ) && p != Player) {
				// send info about the new player to every other player

				if (p.GetSocket() != null) {
					if (this.getGhost().isHideIPAddresses()) {
						p.GetSocket().PutBytes(m_Protocol.SEND_W3GS_PLAYERINFO(Player.GetPID(), Player.GetName(), BlankIP, BlankIP ) );
					} else {
						p.GetSocket().PutBytes(m_Protocol.SEND_W3GS_PLAYERINFO(Player.GetPID(), Player.GetName(), Player.GetExternalIP(), Player.GetInternalIP()));
					}
				}

				// send info about every other player to the new player

				if (this.getGhost().isHideIPAddresses()) {
					Player.GetSocket().PutBytes(m_Protocol.SEND_W3GS_PLAYERINFO(p.GetPID(), p.GetName(), BlankIP, BlankIP));
				} else {
					Player.GetSocket().PutBytes(m_Protocol.SEND_W3GS_PLAYERINFO(p.GetPID(), p.GetName(), p.GetExternalIP(), p.GetInternalIP()));
				}
			}
		}

		// send a map check packet to the new player

		Player.GetSocket().PutBytes(m_Protocol.SEND_W3GS_MAPCHECK(m_Map.GetMapPath(), m_Map.GetMapSize(), m_Map.GetMapInfo(), m_Map.GetMapCRC(), m_Map.GetMapSHA1()));

		// send slot info to everyone, so the new player gets this info twice but everyone else still needs to know the new slot layout

		SendAllSlotInfo( );

		// send a welcome message

		SendWelcomeMessage( Player );

		// check for multiple IP usage

		if (getGhost().isCheckMultipleIPUsage()) {
			String Others = "";

			for (GamePlayer p : m_Players) {
				if (Player != p && Player.GetExternalIPString().equals(p.GetExternalIPString())) {
					if (Others.isEmpty()) {
						Others = p.GetName();
					} else {
						Others += ", " + p.GetName();
					}
				}
			}
			
			if (!Others.isEmpty()) {
				SendAllChat(MessageFactory.create(Messages.MULTIPLE_IP_ADDRESS_USAGE_DETECTED, joinPlayer.GetName(), Others));
			}
		}

		// abort the countdown if there was one in progress

		if( m_CountDownStarted && !m_GameLoading && !m_GameLoaded )
		{
			SendAllChat(MessageFactory.create(Messages.COUNTDOWN_ABORTED));
			m_CountDownStarted = false;
		}

		// auto lock the game

		if (this.getGhost().isAutoLock() && !m_Locked && IsOwner( joinPlayer.GetName())) {
			SendAllChat(MessageFactory.create(Messages.GAME_LOCKED));
			m_Locked = true;
		}
	}

	//TODO: implement for matchmaking with score
	/*public void EventPlayerJoinedWithScore (PotentialPlayer potential, IncomingJoinPlayer joinPlayer, double score) {
		// this function is only called when matchmaking is enabled
		// EventPlayerJoined will be called first in all cases
		// if matchmaking is enabled EventPlayerJoined will start a database query to retrieve the player's score and keep the connection open while we wait
		// when the database query is complete EventPlayerJoinedWithScore will be called

		// check if the new player's name is the same as the virtual host name

		if( joinPlayer->GetName( ) == m_VirtualHostName )
		{
			CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game with the virtual host name" );
			potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
			potential->SetDeleteMe( true );
			return;
		}

		// check if the new player's name is already taken

		if( GetPlayerFromName( joinPlayer->GetName( ), false ) )
		{
			CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but that name is already taken" );
			// SendAllChat( m_GHost->m_Language->TryingToJoinTheGameButTaken( joinPlayer->GetName( ) ) );
			potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
			potential->SetDeleteMe( true );
			return;
		}

		// check if the new player's score is within the limits

		if( score > -99999.0 && ( score < m_MinimumScore || score > m_MaximumScore ) )
		{
			CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but has a rating [" + UTIL_ToString( score, 2 ) + "] outside the limits [" + UTIL_ToString( m_MinimumScore, 2 ) + "] to [" + UTIL_ToString( m_MaximumScore, 2 ) + "]" );
			potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
			potential->SetDeleteMe( true );
			return;
		}

		// try to find an empty slot

		unsigned char SID = GetEmptySlot( false );

		if( SID == 255 )
		{
			// no empty slot found, time to do some matchmaking!
			// the general idea is that we're going to compute the average score of all players in the game
			// then we kick the player with the score furthest from that average (or a player without a score)
			// this ensures that the players' scores will tend to converge as players join the game
			// note: the database code uses a score of -100000 to denote "no score"

			// calculate the average score

			double AverageScore = 0.0;
			uint32_t PlayersScored = 0;

			if( score > -99999.0 )
			{
				AverageScore = score;
				PlayersScored = 1;
			}

			for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
			{
				if( (*i)->GetScore( ) > -99999.0 )
				{
					AverageScore += (*i)->GetScore( );
					PlayersScored++;
				}
			}

			if( PlayersScored > 0 )
				AverageScore /= PlayersScored;

			// calculate the furthest player from the average

			CGamePlayer *FurthestPlayer = NULL;

			for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
			{
				if( !FurthestPlayer || (*i)->GetScore( ) < -99999.0 || abs( (*i)->GetScore( ) - AverageScore ) > abs( FurthestPlayer->GetScore( ) - AverageScore ) )
					FurthestPlayer = *i;
			}

			if( !FurthestPlayer )
			{
				// this should be impossible

				CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but no furthest player was found (this should be impossible)" );
				potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
				potential->SetDeleteMe( true );
				return;
			}

			// kick the new player if they have the furthest score

			if( score < -99999.0 || abs( score - AverageScore ) > abs( FurthestPlayer->GetScore( ) - AverageScore ) )
			{
				if( score < -99999.0 )
					CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but has the furthest rating [N/A] from the average [" + UTIL_ToString( AverageScore, 2 ) + "]" );
				else
					CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] is trying to join the game but has the furthest rating [" + UTIL_ToString( score, 2 ) + "] from the average [" + UTIL_ToString( AverageScore, 2 ) + "]" );

				potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
				potential->SetDeleteMe( true );
				return;
			}

			// kick the furthest player

			SID = GetSIDFromPID( FurthestPlayer->GetPID( ) );
			FurthestPlayer->SetDeleteMe( true );

			if( FurthestPlayer->GetScore( ) < -99999.0 )
				FurthestPlayer->SetLeftReason( m_GHost->m_Language->WasKickedForHavingFurthestScore( "N/A", UTIL_ToString( AverageScore, 2 ) ) );
			else
				FurthestPlayer->SetLeftReason( m_GHost->m_Language->WasKickedForHavingFurthestScore( UTIL_ToString( FurthestPlayer->GetScore( ), 2 ), UTIL_ToString( AverageScore, 2 ) ) );

			FurthestPlayer->SetLeftCode( PLAYERLEAVE_LOBBY );

			// send a playerleave message immediately since it won't normally get sent until the player is deleted which is after we send a playerjoin message
			// we don't need to call OpenSlot here because we're about to overwrite the slot data anyway

			SendAll( m_Protocol->SEND_W3GS_PLAYERLEAVE_OTHERS( FurthestPlayer->GetPID( ), FurthestPlayer->GetLeftCode( ) ) );
			FurthestPlayer->SetLeftMessageSent( true );

			if( FurthestPlayer->GetScore( ) < -99999.0 )
				SendAllChat( "Player [" + FurthestPlayer->GetName( ) + "] was kicked for having the furthest rating [N/A] from the average [" + UTIL_ToString( AverageScore, 2 ) + "]" );
			else
				SendAllChat( "Player [" + FurthestPlayer->GetName( ) + "] was kicked for having the furthest rating [" + UTIL_ToString( FurthestPlayer->GetScore( ), 2 ) + "] from the average [" + UTIL_ToString( AverageScore, 2 ) + "]" );
		}

		if( SID >= m_Slots.size( ) )
		{
			potential->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_REJECTJOIN( REJECTJOIN_FULL ) );
			potential->SetDeleteMe( true );
			return;
		}

		// we have a slot for the new player
		// make room for them by deleting the virtual host player if we have to

		if( GetNumPlayers( ) >= 11 )
			DeleteVirtualHost( );

		// turning the CPotentialPlayer into a CGamePlayer is a bit of a pain because we have to be careful not to close the socket
		// this problem is solved by setting the socket to NULL before deletion and handling the NULL case in the destructor
		// we also have to be careful to not modify the m_Potentials vector since we're currently looping through it

		CONSOLE_Print( "[GAME: " + m_GameName + "] player [" + joinPlayer->GetName( ) + "] joined the game" );
		CGamePlayer *Player = new CGamePlayer( potential, GetNewPID( ), joinPlayer->GetName( ), joinPlayer->GetInternalIP( ), false );
		Player->SetScore( score );
		m_Players.push_back( Player );
		potential->SetSocket( NULL );
		potential->SetDeleteMe( true );
		m_Slots[SID] = CGameSlot( Player->GetPID( ), 255, SLOTSTATUS_OCCUPIED, 0, m_Slots[SID].GetTeam( ), m_Slots[SID].GetColour( ), m_Slots[SID].GetRace( ) );

		// send slot info to the new player
		// the SLOTINFOJOIN packet also tells the client their assigned PID and that the join was successful

		Player->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_SLOTINFOJOIN( Player->GetPID( ), Player->GetSocket( )->GetPort( ), Player->GetExternalIP( ), m_Slots, m_RandomSeed, m_Map->GetMapGameType( ) == GAMETYPE_CUSTOM ? 3 : 0, m_Map->GetMapNumPlayers( ) ) );

		// send virtual host info and fake player info (if present) to the new player

		SendVirtualHostPlayerInfo( Player );
		SendFakePlayerInfo( Player );

		BYTEARRAY BlankIP;
		BlankIP.push_back( 0 );
		BlankIP.push_back( 0 );
		BlankIP.push_back( 0 );
		BlankIP.push_back( 0 );

		for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
		{
			if( !(*i)->GetLeftMessageSent( ) && *i != Player )
			{
				// send info about the new player to every other player

				if( (*i)->GetSocket( ) )
				{
					if( m_GHost->m_HideIPAddresses )
						(*i)->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_PLAYERINFO( Player->GetPID( ), Player->GetName( ), BlankIP, BlankIP ) );
					else
						(*i)->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_PLAYERINFO( Player->GetPID( ), Player->GetName( ), Player->GetExternalIP( ), Player->GetInternalIP( ) ) );
				}

				// send info about every other player to the new player

				if( m_GHost->m_HideIPAddresses )
					Player->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_PLAYERINFO( (*i)->GetPID( ), (*i)->GetName( ), BlankIP, BlankIP ) );
				else
					Player->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_PLAYERINFO( (*i)->GetPID( ), (*i)->GetName( ), (*i)->GetExternalIP( ), (*i)->GetInternalIP( ) ) );
			}
		}

		// send a map check packet to the new player

		Player->GetSocket( )->PutBytes( m_Protocol->SEND_W3GS_MAPCHECK( m_Map->GetMapPath( ), m_Map->GetMapSize( ), m_Map->GetMapInfo( ), m_Map->GetMapCRC( ), m_Map->GetMapSHA1( ) ) );

		// send slot info to everyone, so the new player gets this info twice but everyone else still needs to know the new slot layout

		SendAllSlotInfo( );

		// send a welcome message

		SendWelcomeMessage( Player );

		if( score < -99999.0 )
			SendAllChat( m_GHost->m_Language->PlayerHasScore( joinPlayer->GetName( ), "N/A" ) );
		else
			SendAllChat( m_GHost->m_Language->PlayerHasScore( joinPlayer->GetName( ), UTIL_ToString( score, 2 ) ) );

		uint32_t PlayersScored = 0;
		uint32_t PlayersNotScored = 0;
		double AverageScore = 0.0;
		double MinScore = 0.0;
		double MaxScore = 0.0;
		bool Found = false;

		for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
		{
			if( (*i)->GetScore( ) < -99999.0 )
				PlayersNotScored++;
			else
			{
				PlayersScored++;
				AverageScore += (*i)->GetScore( );

				if( !Found || (*i)->GetScore( ) < MinScore )
					MinScore = (*i)->GetScore( );

				if( !Found || (*i)->GetScore( ) > MaxScore )
					MaxScore = (*i)->GetScore( );

				Found = true;
			}
		}

		double Spread = MaxScore - MinScore;
		SendAllChat( m_GHost->m_Language->RatedPlayersSpread( UTIL_ToString( PlayersScored ), UTIL_ToString( PlayersScored + PlayersNotScored ), UTIL_ToString( (uint32_t)Spread ) ) );

		// check for multiple IP usage

		if( m_GHost->m_CheckMultipleIPUsage )
		{
			string Others;

			for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
			{
				if( Player != *i && Player->GetExternalIPString( ) == (*i)->GetExternalIPString( ) )
				{
					if( Others.empty( ) )
						Others = (*i)->GetName( );
					else
						Others += ", " + (*i)->GetName( );
				}
			}

			if( !Others.empty( ) )
				SendAllChat( m_GHost->m_Language->MultipleIPAddressUsageDetected( joinPlayer->GetName( ), Others ) );
		}

		// abort the countdown if there was one in progress

		if( m_CountDownStarted && !m_GameLoading && !m_GameLoaded )
		{
			SendAllChat( m_GHost->m_Language->CountDownAborted( ) );
			m_CountDownStarted = false;
		}

		// auto lock the game

		if( m_GHost->m_AutoLock && !m_Locked && IsOwner( joinPlayer->GetName( ) ) )
		{
			SendAllChat( m_GHost->m_Language->GameLocked( ) );
			m_Locked = true;
		}

		// balance the slots

		if( m_AutoStartPlayers != 0 && GetNumPlayers( ) == m_AutoStartPlayers )
			BalanceSlots( );
	}*/
	
	public void EventPlayerLeft (GamePlayer player) {
		// this function is only called when a player leave packet is received, not when there's a socket error, kick, etc...

		player.SetDeleteMe(true);
		player.SetLeftReason(Messages.HAS_LEFT_VOLUNTARILY.createMessage());
		player.SetLeftCode(Constants.PLAYERLEAVE_LOST);

		if (!m_GameLoading && !m_GameLoaded) {
			OpenSlot(GetSIDFromPID(player.GetPID()), false);
		}
	}
	
	public void EventPlayerLoaded (GamePlayer player) {
		SendAll(m_Protocol.SEND_W3GS_GAMELOADED_OTHERS(player.GetPID()));
	}
	
	public void EventPlayerAction (GamePlayer player, IncomingAction action) {
		m_Actions.add(action);

		// check for players saving the game and notify everyone

		if (!action.GetAction().isEmpty() && action.GetAction().getChar(0) == 6) {
			LOG.info("[" + m_GameName + "] player [" + player.GetName() + "] is saving the game" );
			SendAllChat(MessageFactory.create(Messages.PLAYER_IS_SAVING_THE_GAME, player.GetName()));
		}
	}
	
	public void EventPlayerKeepAlive (GamePlayer player, int checkSum) {
		// check for desyncs

		int FirstCheckSum = player.GetCheckSums().peek();

		for (GamePlayer p : m_Players) {
			if (p.GetCheckSums().isEmpty()) {
				return;
			}

			if (!m_Desynced && p.GetCheckSums().peek() != FirstCheckSum) {
				m_Desynced = true;
				LOG.info("[" + m_GameName + "] desync detected");
				SendAllChat(MessageFactory.create(Messages.DESYNC_DETECTED));
			}
		}

		for (GamePlayer p : m_Players) {
			p.GetCheckSums().poll();
		}

		// add checksum to replay but only if we're not desynced

		//TODO implement for replay
		/*if (m_Replay && !m_Desynced) {
			m_Replay.AddCheckSum( FirstCheckSum );
		}*/
	}
	
	public void EventPlayerChatToHost (GamePlayer player, IncomingChatPlayer chatPlayer) {
		if (chatPlayer.GetFromPID() == player.GetPID()) {
			if (chatPlayer.GetType() == ChatToHostType.CTH_MESSAGE || chatPlayer.GetType( ) == ChatToHostType.CTH_MESSAGEEXTRA) {
				// relay the chat message to other players

				if( !m_GameLoading ) {
					boolean Relay = !player.GetMuted();
					Bytearray ExtraFlags = chatPlayer.GetExtraFlags();

					// calculate timestamp

					String MinString = "" + (int) (( m_GameTicks / 1000 ) / 60 );
					String SecString = "" + (int) (( m_GameTicks / 1000 ) % 60 );

					if (MinString.length() == 1) {
						MinString = "0" + MinString;
					}

					if (SecString.length() == 1) {
						SecString = "0" + SecString;
					}

					if (!ExtraFlags.isEmpty()) {
						if (ExtraFlags.getChar(0) == 0) {
							// this is an ingame [All] message, print it to the console

							LOG.info( "[" + m_GameName + "] (" + MinString + ":" + SecString + ") [All] [" + player.GetName() + "]: " + chatPlayer.GetMessage());

							// don't relay ingame messages targeted for all players if we're currently muting all
							// note that commands will still be processed even when muting all because we only stop relaying the messages, the rest of the function is unaffected

							if( m_MuteAll )
								Relay = false;
						} else {
							// this is a team message, print it to the console
							LOG.info("[" + m_GameName + "] (" + MinString + ":" + SecString + ") [Team] [" + player.GetName() + "]: " + chatPlayer.GetMessage());
						}

						if( Relay )
						{
							// add chat message to replay
							// this includes allied chat and private chat from both teams as long as it was relayed		
							//TODO implement for replay
							/*String msg = chatPlayer.GetMessage(); 
							boolean iscmd = false;
							if (!msg.isEmpty( ) && msg.charAt(0) == this.jGhost.getCommandTrigger()) {
								iscmd = true;
							}

							if( m_Replay && !iscmd)	{
								//if the player is gimped or all players are gimped, replace the original message with a random generated message
								if (player->GetGimped( ) || m_GimpAll) {
									string Message = m_GHost->m_Language->RandomGimpMessage( player->GetName( ) );
									CONSOLE_Print( "[GAME: " + m_GameName + "] (" + MinString + ":" + SecString + ") [GIMP] [" + player->GetName( ) + "]: Replaced previous message with: " + Message );
									m_Replay->AddChatMessage( chatPlayer.GetFromPID( ), chatPlayer->GetFlag( ), UTIL_ByteArrayToUInt32( chatPlayer->GetExtraFlags( ), false ), Message );
								} else {
									m_Replay.AddChatMessage( chatPlayer.GetFromPID( ), chatPlayer->GetFlag( ), UTIL_ByteArrayToUInt32( chatPlayer->GetExtraFlags( ), false ), chatPlayer->GetMessage( ) );
								}
							}*/
						}
					} else {
						// this is a lobby message, print it to the console

						LOG.info("[" + m_GameName + "] [Lobby] [" + player.GetName( ) + "]: " + chatPlayer.GetMessage());

						if (m_MuteLobby) {
							Relay = false;
						}
					}

					String msg = chatPlayer.GetMessage( ); 
					boolean iscmd = false;
					if (!msg.isEmpty( ) && msg.charAt(0) == this.getGhost().getCommandTrigger()) {
						iscmd = true;
					}

					if (Relay  && !iscmd) {
						//if the player is gimped or all players are gimped, replace the original message with a random generated message
						if (player.GetGimped() || m_GimpAll) {
							//TODO implement for gimp
							String Message = "random gimp message";
							LOG.info("[" + m_GameName + "] [GIMP] [" + player.GetName() + "]: Replaced previous message with: " + Message );
							Send(chatPlayer.GetToPIDs(), m_Protocol.SEND_W3GS_CHAT_FROM_HOST(chatPlayer.GetFromPID(), chatPlayer.GetToPIDs(), chatPlayer.GetFlag(), chatPlayer.GetExtraFlags(), Message));
						} else {
							Send(chatPlayer.GetToPIDs(), m_Protocol.SEND_W3GS_CHAT_FROM_HOST(chatPlayer.GetFromPID(), chatPlayer.GetToPIDs(), chatPlayer.GetFlag(), chatPlayer.GetExtraFlags(), chatPlayer.GetMessage()));
						}
					}
				}

				// handle bot commands

				String Message = chatPlayer.GetMessage();

				if (!Message.isEmpty() && Message.charAt(0) == this.getGhost().getCommandTrigger()) {
					// extract the command trigger, the command, and the payload
					// e.g. "!say hello world" -> command: "say", payload: "hello world"

					String Command = "";
					String Payload = "";
					
					Message = Message.trim();
					int index = Message.indexOf(' ');
					if (index == -1) {
						Command = Message.substring(1);
					} else {
						Command = Message.substring(1, index);
						Payload = Message.substring(index + 1);
					}
					
					EventPlayerBotCommand(player, Command, Payload);
				}
			} else if( chatPlayer.GetType() == ChatToHostType.CTH_TEAMCHANGE && !m_CountDownStarted ) {
				EventPlayerChangeTeam( player, chatPlayer.GetByte());
			} else if( chatPlayer.GetType() == ChatToHostType.CTH_COLOURCHANGE && !m_CountDownStarted ) {
				EventPlayerChangeColour( player, chatPlayer.GetByte());
			} else if( chatPlayer.GetType() == ChatToHostType.CTH_RACECHANGE && !m_CountDownStarted ) {
				EventPlayerChangeRace( player, chatPlayer.GetByte());
			} else if( chatPlayer.GetType() == ChatToHostType.CTH_HANDICAPCHANGE && !m_CountDownStarted ) {
				EventPlayerChangeHandicap( player, chatPlayer.GetByte());
			}
		}
	}
	
	public abstract void EventPlayerBotCommand (GamePlayer player, String command, String payload);
	
	public void EventPlayerChangeTeam (GamePlayer player, char team) {
		// player is requesting a team change

		if (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			char oldSID = GetSIDFromPID(player.GetPID());
			char newSID = GetEmptySlot(team, player.GetPID());
			SwapSlots( oldSID, newSID );
		} else {
			if (team > 12) {
				return;
			}

			if (team == 12) {
				if( m_Map.GetMapObservers() != Constants.MAPOBS_ALLOWED && m_Map.GetMapObservers( ) != Constants.MAPOBS_REFEREES) {
					return;
				}
			} else {
				if( team >= m_Map.GetMapNumTeams()) {
					return;
				}

				// make sure there aren't too many other players already

				char NumOtherPlayers = 0;

				for (char i = 0; i < m_Slots.size( ); i++ ) {
					if (m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && m_Slots.get(i).GetTeam() != 12 && m_Slots.get(i).GetPID( ) != player.GetPID()) {
						NumOtherPlayers++;
					}
				}

				if (NumOtherPlayers >= m_Map.GetMapNumPlayers()) {
					return;
				}
			}

			char SID = GetSIDFromPID(player.GetPID());

			if (SID < m_Slots.size()) {
				m_Slots.get(SID).SetTeam(team);

				if( team == 12 ) {
					// if they're joining the observer team give them the observer colour

					m_Slots.get(SID).SetColour((char) 12);
				} else if( m_Slots.get(SID).GetColour( ) == 12 ) {
					// if they're joining a regular team give them an unused colour

					m_Slots.get(SID).SetColour( GetNewColour( ) );
				}

				SendAllSlotInfo( );
			}
		}
	}

	public void EventPlayerChangeColour (GamePlayer player, char colour) {
		// player is requesting a colour change

		if (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			return;
		}

		if (colour > 11) {
			return;
		}

		char SID = GetSIDFromPID(player.GetPID());

		if (SID < m_Slots.size()) {
			// make sure the player isn't an observer

			if (m_Slots.get(SID).GetTeam() == 12) {
				return;
			}

			ColourSlot(SID, colour);
		}
	}
	
	public void EventPlayerChangeRace (GamePlayer player, char race) {
		// player is requesting a race change

		if (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			return;
		}

		if ((m_Map.GetMapFlags() & Constants.MAPFLAG_RANDOMRACES) != 0) {
			return;
		}
		
		if (race != Constants.SLOTRACE_HUMAN && race != Constants.SLOTRACE_ORC && race != Constants.SLOTRACE_NIGHTELF && race != Constants.SLOTRACE_UNDEAD && race != Constants.SLOTRACE_RANDOM) {
			return;
		}

		char SID = GetSIDFromPID(player.GetPID());

		if (SID < m_Slots.size()) {
			m_Slots.get(SID).SetRace(race);
			SendAllSlotInfo( );
		}
	}
	
	public void EventPlayerChangeHandicap (GamePlayer player, char handicap) {
		// player is requesting a handicap change

		if (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			return;
		}

		if (handicap != 50 && handicap != 60 && handicap != 70 && handicap != 80 && handicap != 90 && handicap != 100 ) {
			return;
		}

		char SID = GetSIDFromPID(player.GetPID());

		if (SID < m_Slots.size()) {
			m_Slots.get(SID).SetHandicap( handicap );
			SendAllSlotInfo( );
		}
	}
	
	public void EventPlayerDropRequest (GamePlayer player) {
		// todotodo: check that we've waited the full 45 seconds

		if (m_Lagging) {
			LOG.info("[" + m_GameName + "] player [" + player.GetName() + "] voted to drop laggers");
			SendAllChat(MessageFactory.create(Messages.PLAYER_VOTED_TO_DROP_LAGGERS, player.GetName()));

			// check if at least half the players voted to drop

			int Votes = 0;

			for (GamePlayer p : m_Players) {
				if (p.GetDropVote()) {
					Votes++;
				}
			}

			if (((float) Votes) / m_Players.size() > 0.49) {
				StopLaggers(Messages.LAGGED_OUT_DROPPED_BY_VOTE.createMessage());
			}
		}
	}

	public void EventPlayerMapSize (GamePlayer player, IncomingMapSize mapSize) {
		if( m_GameLoading || m_GameLoaded )
			return;

		//TODO todo: the variable names here are confusing due to extremely poor design on my part

		int MapSize = m_Map.GetMapSize().toInt();

		if (mapSize.GetSizeFlag() != 1 || mapSize.GetMapSize() != MapSize) {
			// the player doesn't have the map

			if (this.getGhost().getAllowDownloads() != 0 ) {
				//string *MapData = m_Map->GetMapData( );
				byte[] MapData = m_Map.getByteMapData();
				
				if (MapData != null && MapData.length != 0) {
					if(this.getGhost().getAllowDownloads() == 1 || ( this.getGhost().getAllowDownloads() == 2 && player.GetDownloadAllowed())) {
						if( !player.GetDownloadStarted( ) && mapSize.GetSizeFlag( ) == 1 ) {
							// inform the client that we are willing to send the map

							LOG.info("[GAME: " + m_GameName + "] map download started for player [" + player.GetName() + "]" );
							Send( player, m_Protocol.SEND_W3GS_STARTDOWNLOAD(GetHostPID()));
							player.SetDownloadStarted( true );
							player.SetStartedDownloadingTicks(TimeUtil.getTicks());
						} else {
							player.SetLastMapPartAcked(mapSize.GetMapSize());
						}
					}
				} else {
					player.SetDeleteMe( true );
					player.SetLeftReason( "doesn't have the map and there is no local copy of the map to send" );
					player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
					OpenSlot(GetSIDFromPID(player.GetPID()), false);
				}
			} else {
				player.SetDeleteMe(true);
				player.SetLeftReason("doesn't have the map and map downloads are disabled" );
				player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
				OpenSlot(GetSIDFromPID(player.GetPID()), false);
			}
		} else {
			if (player.GetDownloadStarted()) {
				// calculate download rate

				float Seconds = ((float) (TimeUtil.getTicks()) - player.GetStartedDownloadingTicks()) / 1000;
				float Rate = ((float) MapSize) / 1024 / Seconds;
				LOG.info("[GAME: " + m_GameName + "] map download finished for player [" + player.GetName( ) + "] in " + ((int) Seconds) + " seconds");
				SendAllChat(MessageFactory.create(Messages.PLAYER_DOWNLOADED_THE_MAP, player.GetName(), (int) Seconds, (int) Rate));
				player.SetDownloadFinished( true );
				player.SetFinishedDownloadingTicks(TimeUtil.getTicks());

				// add to database

				//TODO map download?
				//m_GHost->m_Callables.push_back( m_GHost->m_DB->ThreadedDownloadAdd( m_Map->GetMapPath( ), MapSize, player->GetName( ), player->GetExternalIPString( ), player->GetSpoofed( ) ? 1 : 0, player->GetSpoofedRealm( ), GetTicks( ) - player->GetStartedDownloadingTicks( ) ) );
			}
		}

		char NewDownloadStatus = (char) ((float)mapSize.GetMapSize() / MapSize * 100);
		char SID = GetSIDFromPID(player.GetPID());

		if (NewDownloadStatus > 100) {
			NewDownloadStatus = 100;
		}

		if (SID < m_Slots.size()) {
			// only send the slot info if the download status changed

			if (m_Slots.get(SID).GetDownloadStatus( ) != NewDownloadStatus) {
				m_Slots.get(SID).SetDownloadStatus( NewDownloadStatus );
				SendAllSlotInfo( );
			}
		}
	}
	
	public void EventPlayerPongToHost (GamePlayer player, int pong) {
		// autokick players with excessive pings but only if they're not reserved and we've received at least 3 pings from them
		// also don't kick anyone if the game is loading or loaded - this could happen because we send pings during loading but we stop sending them after the game is loaded
		// see the Update function for where we send pings

		if (!m_GameLoading && !m_GameLoaded && !player.GetReserved() && player.GetNumPings() >= 3 && player.GetPing(this.getGhost().isLcPings()) > this.getGhost().getAutoKickPing()) {
			// send a chat message because we don't normally do so when a player leaves the lobby

			SendAllChat(MessageFactory.create(Messages.AUTOKICKING_PLAYER_FOR_EXCESSIVE_PING, player.GetName(), player.GetPing(this.getGhost().isLcPings())));
			player.SetDeleteMe(true );
			player.SetLeftReason("was autokicked for excessive ping of " + player.GetPing(this.getGhost().isLcPings()));
			player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
			OpenSlot(GetSIDFromPID(player.GetPID()), false);
		}
	}

		// these events are called outside of any iterations

	public void EventGameStarted () {
		LOG.info("[" + m_GameName + "] started loading with " + GetNumPlayers() + " players" );

		// since we use a fake countdown to deal with leavers during countdown the COUNTDOWN_START and COUNTDOWN_END packets are sent in quick succession
		// send a start countdown packet

		SendAll(m_Protocol.SEND_W3GS_COUNTDOWN_START());

		// remove the virtual host player

		DeleteVirtualHost( );

		// send an end countdown packet

		SendAll(m_Protocol.SEND_W3GS_COUNTDOWN_END());

		// send a game loaded packet for the fake player (if present)

		if (m_FakePlayerPID != 255) {
			SendAll(m_Protocol.SEND_W3GS_GAMELOADED_OTHERS(m_FakePlayerPID));
		}

		// record the starting number of players

		m_StartPlayers = GetNumPlayers( );

		// close the listening socket

		//TODO close the listening socket
		//delete m_Socket;
		//m_Socket = NULL;

		// delete any potential players that are still hanging around

		m_Potentials.clear();

		// set initial values for replay

		//TODO replay implement
		/*if( m_Replay )
		{
			for( vector<CGamePlayer *> :: iterator i = m_Players.begin( ); i != m_Players.end( ); i++ )
				m_Replay->AddPlayer( (*i)->GetPID( ), (*i)->GetName( ) );

			m_Replay->SetSlots( m_Slots );
			m_Replay->SetRandomSeed( m_RandomSeed );
			m_Replay->SetSelectMode( m_Map->GetMapGameType( ) == GAMETYPE_CUSTOM ? 3 : 0 );
			m_Replay->SetStartSpotCount( m_Map->GetMapNumPlayers( ) );

			if( !m_Players.empty( ) )
			{
				// this might not be necessary since we're going to overwrite the replay's host PID and name everytime a player leaves

				m_Replay->SetHostPID( m_Players[0]->GetPID( ) );
				m_Replay->SetHostName( m_Players[0]->GetName( ) );
			}
		}*/

		// build a stat string for use when saving the replay
		// we have to build this now because the map data could change now that the game has started

		Bytearray StatString = new Bytearray();
		StatString.addBytearray(m_Map.GetMapGameFlags());
		StatString.addChar((char) 0 );
		StatString.addBytearray(m_Map.GetMapWidth());
		StatString.addBytearray(m_Map.GetMapHeight());
		StatString.addBytearray(m_Map.GetMapCRC());
		StatString.addString(m_Map.GetMapPath());
		StatString.addString("GHost++");
		StatString.addChar((char) 0 );
		StatString = StatString.encode();
		m_StatString = StatString.toString(); //TODO char or int string?

		// move the game to the games in progress vector

		this.getGhost().setCurrentGame(null);
		this.getGhost().getGames().add(this);

		// and finally reenter battle.net chat

		for (BnetConnection connection : this.getGhost().getBnetConnections()) {
			connection.QueueEnterChat();
		}
	}
	
	public void EventGameLoaded () {
		LOG.info("[" + m_GameName + "] finished loading with " + GetNumPlayers() + " players");

		// send shortest, longest, and personal load times to each player

		GamePlayer Shortest = null;
		GamePlayer Longest = null;

		for (GamePlayer p : m_Players) {
			if (Shortest == null || p.GetFinishedLoadingTicks() < Shortest.GetFinishedDownloadingTicks()) {
				Shortest = p;
			}
			
			if (Longest == null || p.GetFinishedDownloadingTicks() > Longest.GetFinishedDownloadingTicks()) {
				Longest = p;
			}
		}

		if (Shortest != null && Longest != null)
		{
			SendAllChat(MessageFactory.create(Messages.SHORTEST_LOAD_BY_PLAYER, Shortest.GetName(), ((float)( Shortest.GetFinishedLoadingTicks() - m_StartedLoadingTicks) / 1000)));
			SendAllChat(MessageFactory.create(Messages.LONGEST_LOAD_BY_PLAYER, Longest.GetName(), ((float)( Longest.GetFinishedLoadingTicks() - m_StartedLoadingTicks) / 1000)));
		}

		for (GamePlayer p : m_Players) {
			SendChat(p, MessageFactory.create(Messages.YOUR_LOADING_TIME_WAS, ((float)( p.GetFinishedLoadingTicks() - m_StartedLoadingTicks ) / 1000)));
		}
		
		// read from gameloaded.txt if available
		//TODO display gameloaded.txt text
		/*ifstream in;
		in.open( m_GHost->m_GameLoadedFile.c_str( ) );

		if( !in.fail( ) )
		{
			// don't print more than 8 lines

			uint32_t Count = 0;
			string Line;

			while( !in.eof( ) && Count < 8 )
			{
				getline( in, Line );

				if( in.eof( ) )
					break;

				if( Line.empty( ) )
					SendAllChat( " " );
				else
					SendAllChat( Line );

				Count++;
			}

			in.close( );
		}*/
	}
		
		
		
	// other functions

	public char GetSIDFromPID (char PID) {
		if (m_Slots.size() > 255)
			return 255;

		for(char i = 0; i < m_Slots.size( ); i++ ) {
			if( m_Slots.get(i).GetPID( ) == PID ) {
				return i;
			}
		}

		return 255;
	}
	
	public GamePlayer GetPlayerFromPID (char PID) {
		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent() && p.GetPID() == PID) {
				return p;
			}
		}
		
		return null;
	}
	
	public GamePlayer GetPlayerFromSID (char SID) {
		if (SID < m_Slots.size()) {
			return GetPlayerFromPID( m_Slots.get(SID).GetPID());
		}

		return null;
	}
	
	public GamePlayer GetPlayerFromName (String name, boolean sensitive) {
		if (!sensitive) {
			name = name.toLowerCase();
		}
		
		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent()) {
				String tmpName = p.GetName();
				if (!sensitive) {
					tmpName = tmpName.toLowerCase();
				}
				
				if (tmpName.equals(name)) {
					return p;
				}
			}
		}

		return null;
	}
	
	public List<GamePlayer> GetPlayerFromNamePartial (String name) {
		name = name.toLowerCase(); 
		
		List<GamePlayer> Matches = new ArrayList<GamePlayer>();

		// try to match each player with the passed string (e.g. "Varlock" would be matched with "lock")

		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent()) {
				String tmpName = p.GetName().toLowerCase();
				
				if (tmpName.contains(name)) {
					Matches.add(p);
				}
			}
		}
		
		return Matches;
	}
	
	public GamePlayer GetPlayerFromColour (char colour) {
		for(char i = 0; i < m_Slots.size(); i++ ) {
			if( m_Slots.get(i).GetColour() == colour )
				return GetPlayerFromSID(i);
		}

		return null;
	}
	
	public char GetNewPID () {
		// find an unused PID for a new player to use

		for(char TestPID = 1; TestPID < 255; TestPID++ ) {
			if( TestPID == m_VirtualHostPID || TestPID == m_FakePlayerPID )
				continue;

			boolean InUse = false;

			for (GamePlayer p : m_Players) {
				if (!p.GetLeftMessageSent() && p.GetPID() == TestPID) {
					InUse = true;
					break;
				}
			}

			if (!InUse) {
				return TestPID;
			}
		}

		// this should never happen

		return 255;
	}
	
	public char GetNewColour () {
		// find an unused colour for a player to use

		for(char TestColour = 0; TestColour < 12; TestColour++) {
			boolean InUse = false;

			for(char i = 0; i < m_Slots.size(); i++ ) {
				if (m_Slots.get(i).GetColour() == TestColour) {
					InUse = true;
					break;
				}
			}

			if (!InUse) {
				return TestColour;
			}
		}

		// this should never happen

		return 12;
	}
	
	public Bytearray GetPIDs () {
		Bytearray result = new Bytearray();

		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent()) {
				result.addChar(p.GetPID());
			}
		}

		return result;
	}
	
	public Bytearray GetPIDs (char excludePID) {
		Bytearray result = new Bytearray();

		for (GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent() && p.GetPID() != excludePID) {
				result.addChar(p.GetPID());
			}
		}

		return result;
	}
	
	public char GetHostPID () {
		// return the player to be considered the host (it can be any player) - mainly used for sending text messages from the bot
		// try to find the virtual host player first

		if( m_VirtualHostPID != 255 )
			return m_VirtualHostPID;

		// try to find the owner player next

		for(GamePlayer p : m_Players) {
			if( !p.GetLeftMessageSent( ) && p.GetName().equals(m_OwnerName)) {
				return p.GetPID( );
			}
		}

		// okay then, just use the first available player

		for(GamePlayer p : m_Players) {
			if (!p.GetLeftMessageSent()) {
				return p.GetPID();
			}
		}

		return 255;
	}
	
	public char GetEmptySlot (boolean reserved) {
		if( m_Slots.size( ) > 255 )
			return 255;

		if (m_SaveGame != null) {
			// unfortunately we don't know which slot each player was assigned in the savegame
			// but we do know which slots were occupied and which weren't so let's at least force players to use previously occupied slots

			
			List<GameSlot> SaveGameSlots = m_SaveGame.GetSlots( );

			for (char i = 0; i < m_Slots.size( ); i++ ) {
				if (
					m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN && 
					SaveGameSlots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && 
					SaveGameSlots.get(i).GetComputer() == 0
				) {
					return i;
				}
			}

			// don't bother with reserved slots in savegames
		}
		else
		{
			// look for an empty slot for a new player to occupy
			// if reserved is true then we're willing to use closed or occupied slots as long as it wouldn't displace a player with a reserved slot

			for(char i = 0; i < m_Slots.size( ); i++ ) {
				if (m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN) {
					return i;
				}
			}

			if (reserved) {
				// no empty slots, but since player is reserved give them a closed slot

				for(char i = 0; i < m_Slots.size( ); i++ ) {
					if( m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_CLOSED)
						return i;
				}

				// no closed slots either, give them an occupied slot but not one occupied by another reserved player

				for (char i = 0; i < m_Slots.size( ); i++ ) {
					GamePlayer Player = GetPlayerFromSID( i );

					if (Player != null && !Player.GetReserved()) {
						return i;
					}
				}
			}
		}

		return 255;
	}
	
	public char GetEmptySlot (char team, char PID) {
		if( m_Slots.size( ) > 255 )
			return 255;

		// find an empty slot based on player's current slot

		char StartSlot = GetSIDFromPID( PID );

		if( StartSlot < m_Slots.size( ) )
		{
			if( m_Slots.get(StartSlot).GetTeam( ) != team )
			{
				// player is trying to move to another team so start looking from the first slot on that team
				// we actually just start looking from the very first slot since the next few loops will check the team for us

				StartSlot = 0;
			}

			if (m_SaveGame != null) {
				List<GameSlot> SaveGameSlots = m_SaveGame.GetSlots( );

				for(char i = StartSlot; i < m_Slots.size( ); i++ ) {
					if( m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN && m_Slots.get(i).GetTeam( ) == team && SaveGameSlots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && SaveGameSlots.get(i).GetComputer( ) == 0 )
						return i;
				}

				for (char i = 0; i < StartSlot; i++ ) {
					if( m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN && m_Slots.get(i).GetTeam( ) == team && SaveGameSlots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && SaveGameSlots.get(i).GetComputer( ) == 0 )
						return i;
				}
			} else {
				// find an empty slot on the correct team starting from StartSlot

				for (char i = StartSlot; i < m_Slots.size( ); i++ ) {
					if( m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN && m_Slots.get(i).GetTeam( ) == team )
						return i;
				}

				// didn't find an empty slot, but we could have missed one with SID < StartSlot
				// e.g. in the DotA case where I am in slot 4 (yellow), slot 5 (orange) is occupied, and slot 1 (blue) is open and I am trying to move to another slot

				for (char i = 0; i < StartSlot; i++ ) {
					if( m_Slots.get(i).GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN && m_Slots.get(i).GetTeam( ) == team )
						return i;
				}
			}
		}

		return 255;
	}
	
	public void SwapSlots (char SID1, char SID2) {
		if( SID1 < m_Slots.size( ) && SID2 < m_Slots.size( ) && SID1 != SID2 )
		{
			GameSlot Slot1 = m_Slots.get(SID1);
			GameSlot Slot2 = m_Slots.get(SID2);

			if( m_Map.GetMapGameType( ) != Constants.GAMETYPE_CUSTOM ) {
				// regular game - swap everything
				m_Slots.set(SID1, Slot2);
				m_Slots.set(SID2, Slot1);
			} else {
				// custom game - don't swap the team, colour, or race
				m_Slots.set(SID1, new GameSlot( Slot2.GetPID( ), Slot2.GetDownloadStatus( ), Slot2.GetSlotStatus( ), Slot2.GetComputer( ), Slot1.GetTeam( ), Slot1.GetColour( ), Slot1.GetRace()));
				m_Slots.set(SID2, new GameSlot( Slot1.GetPID( ), Slot1.GetDownloadStatus( ), Slot1.GetSlotStatus( ), Slot1.GetComputer( ), Slot2.GetTeam( ), Slot2.GetColour( ), Slot2.GetRace()));
			}

			SendAllSlotInfo( );
		}
	}
	
	public void OpenSlot (char SID, boolean kick) {
		if (kick) {
			GamePlayer Player = GetPlayerFromSID(SID);

			if (Player != null) {
				Player.SetDeleteMe( true );
				Player.SetLeftReason( "was kicked when opening a slot" );
				Player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY );
			}
		}

		if( SID < m_Slots.size( ) ) {
			GameSlot Slot = m_Slots.get(SID);
			m_Slots.set(SID, new GameSlot((char) 0,(char) 255, Constants.SLOTSTATUS_OPEN,(char) 0, Slot.GetTeam( ), Slot.GetColour( ), Slot.GetRace( )));
			SendAllSlotInfo( );
		}
	}
	
	public void CloseSlot (char SID, boolean kick) {
		if( kick ) {
			GamePlayer Player = GetPlayerFromSID(SID);

			if (Player != null) {
				Player.SetDeleteMe(true);
				Player.SetLeftReason("was kicked when closing a slot");
				Player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
			}
		}

		if( SID < m_Slots.size( ) )
		{
			GameSlot Slot = m_Slots.get(SID);
			m_Slots.set(SID, new GameSlot((char) 0,(char) 255, Constants.SLOTSTATUS_CLOSED,(char) 0, Slot.GetTeam( ), Slot.GetColour( ), Slot.GetRace())); 
			SendAllSlotInfo( );
		}
	}
	
	public void ComputerSlot (char SID, char skill, boolean kick) {
		if( kick ) {
			GamePlayer Player = GetPlayerFromSID( SID );

			if (Player != null) {
				Player.SetDeleteMe(true);
				Player.SetLeftReason("was kicked when creating a computer in a slot");
				Player.SetLeftCode(Constants.PLAYERLEAVE_LOBBY);
			}
		}

		if( SID < m_Slots.size( ) && skill < 3 ) {
			GameSlot Slot = m_Slots.get(SID);
			m_Slots.set(SID, new GameSlot((char) 0,(char) 100, Constants.SLOTSTATUS_OCCUPIED,(char) 1, Slot.GetTeam( ), Slot.GetColour( ), Slot.GetRace( ), skill));
			SendAllSlotInfo( );
		}
	}
	
	public void ColourSlot (char SID,char colour) {
		if( SID < m_Slots.size( ) && colour < 12 )
		{
			// make sure the requested colour isn't already taken

			boolean Taken = false;
			char TakenSID = 0;

			for (char i = 0; i < m_Slots.size( ); i++ ) {
				if (m_Slots.get(i).GetColour( ) == colour ) {
					TakenSID = i;
					Taken = true;
				}
			}

			if( Taken && m_Slots.get(TakenSID).GetSlotStatus( ) != Constants.SLOTSTATUS_OCCUPIED ) {
				// the requested colour is currently "taken" by an unused (open or closed) slot
				// but we allow the colour to persist within a slot so if we only update the existing player's colour the unused slot will have the same colour
				// this isn't really a problem except that if someone then joins the game they'll receive the unused slot's colour resulting in a duplicate
				// one way to solve this (which we do here) is to swap the player's current colour into the unused slot

				m_Slots.get(TakenSID).SetColour( m_Slots.get(SID).GetColour( ) );
				m_Slots.get(SID).SetColour( colour );
				SendAllSlotInfo( );
			} else if( !Taken ) {
				// the requested colour isn't used by ANY slot

				m_Slots.get(SID).SetColour( colour );
				SendAllSlotInfo( );
			}
		}
	}
	
	public void OpenAllSlots () {
		boolean Changed = false;

		for (GameSlot slot : m_Slots) {
			if (slot.GetSlotStatus() == Constants.SLOTSTATUS_CLOSED) {
				slot.SetSlotStatus(Constants.SLOTSTATUS_OPEN);
				Changed = true;
			}
		}

		if( Changed )
			SendAllSlotInfo( );
	}
	
	public void CloseAllSlots () {
		boolean Changed = false;

		for (GameSlot slot : m_Slots) {
			if (slot.GetSlotStatus( ) == Constants.SLOTSTATUS_OPEN ) {
				slot.SetSlotStatus( Constants.SLOTSTATUS_CLOSED );
				Changed = true;
			}
		}

		if( Changed )
			SendAllSlotInfo( );
	}
	
	public void ShuffleSlots () {
		// we only want to shuffle the player slots
		// that means we need to prevent this function from shuffling the open/closed/computer slots too
		// so we start by copying the player slots to a temporary vector

		List<GameSlot> PlayerSlots = new ArrayList<GameSlot>();

		for(GameSlot slot : m_Slots)
		{
			if (slot.GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && slot.GetComputer( ) == 0 )
				PlayerSlots.add(slot);
		}

		// now we shuffle PlayerSlots

		if (m_Map.GetMapGameType() == Constants.GAMETYPE_CUSTOM) {
			// custom game
			// rather than rolling our own probably broken shuffle algorithm we use random_shuffle because it's guaranteed to do it properly
			// so in order to let random_shuffle do all the work we need a vector to operate on
			// unfortunately we can't just use PlayerSlots because the team/colour/race shouldn't be modified
			// so make a vector we can use

			List<Character> SIDs = new ArrayList<Character>();

			for(char i = 0; i < PlayerSlots.size( ); i++ ) {
				SIDs.add(i);
			}

			Collections.shuffle(SIDs);

			// now put the PlayerSlots vector in the same order as the SIDs vector

			List<GameSlot> Slots = new ArrayList<GameSlot>();

			// as usual don't modify the team/colour/race

			for (char i = 0; i < SIDs.size( ); i++ )
				Slots.add(new GameSlot(PlayerSlots.get(SIDs.get(i)).GetPID( ), PlayerSlots.get(SIDs.get(i)).GetDownloadStatus( ), PlayerSlots.get(SIDs.get(i)).GetSlotStatus( ), PlayerSlots.get(SIDs.get(i)).GetComputer( ), PlayerSlots.get(i).GetTeam( ), PlayerSlots.get(i).GetColour( ), PlayerSlots.get(i).GetRace()));

			PlayerSlots = Slots;
		} else {
			// regular game
			// it's easy when we're allowed to swap the team/colour/race!

			Collections.shuffle(PlayerSlots);
		}

		// now we put m_Slots back together again

		int CurrentPlayer = 0;
		List<GameSlot> Slots = new ArrayList<GameSlot>();

		for (GameSlot i : m_Slots) {
			if(i.GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && i.GetComputer() == 0 ) {
				Slots.add(PlayerSlots.get(CurrentPlayer));
				CurrentPlayer++;
			} else {
				Slots.add(i);
			}
		}

		m_Slots = Slots;

		// and finally tell everyone about the new slot configuration

		SendAllSlotInfo( );
	}
	
	public void BalanceSlots () {
		// todotodo: this isn't a very good balancing algorithm :)

		ShuffleSlots( );
	}
	
	public void AddToSpoofed (String server, String name, boolean sendMessage) {
		GamePlayer Player = GetPlayerFromName( name, true );

		if (Player != null) {
			Player.SetSpoofedRealm(server);
			Player.SetSpoofed(true);

			if (sendMessage) {
				SendAllChat(MessageFactory.create(Messages.SPOOF_CHECK_ACCEPTED_FOR, name, server));
			}
		}
	}
	
	public void AddToReserved (String name) {
		name = name.toLowerCase();

		// check that the user is not already reserved

		for (String r : m_Reserved) {
			if (r.endsWith(name)) {
				return;
			}
		}

		m_Reserved.add( name );

		// upgrade the user if they're already in the game

		for (GamePlayer p : m_Players) {
			String pNameLower = p.GetName().toLowerCase();
			
			if (pNameLower.equals(name)) {
				p.SetReserved(true);
			}
		}
	}
	
	public boolean IsOwner (String name) {
		return name.toLowerCase().equals(m_OwnerName.toLowerCase());
	}
	
	public boolean IsReserved (String name) {
		String nameLower = name.toLowerCase();
		
		for (String reservedPlayer : m_Reserved) {
			if (reservedPlayer.toLowerCase().equals(nameLower)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean IsDownloading () {
		// returns true if at least one player is downloading the map

		for (GamePlayer p : this.m_Players) {
			if (p.GetDownloadStarted( ) && !p.GetDownloadFinished()) {
				return true;
			}
		}

		return false;
	}
	
	public boolean IsGameDataSaved () {
		return true;
	}
	
	public void SaveGameData () {
		
	}
	
	public void StartCountDown (boolean force) {
		if (!m_CountDownStarted) {
			if (force) {
				m_CountDownStarted = true;
				m_CountDownCounter = 5;
			} else {
				// check if everyone has the map

				String StillDownloading = "";

				
				for (GameSlot slot : this.m_Slots) {
					if (slot.GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && slot.GetComputer( ) == 0 && slot.GetDownloadStatus( ) != 100 ) {
						GamePlayer Player = GetPlayerFromPID(slot.GetPID( ) );

						if (Player != null) {
							if( StillDownloading.isEmpty( ) )
								StillDownloading = Player.GetName( );
							else
								StillDownloading += ", " + Player.GetName( );
						}
					}
				}

				if (!StillDownloading.isEmpty()) {
					SendAllChat(MessageFactory.create(Messages.PLAYERS_STILL_DOWNLOADING, StillDownloading));
				}
					
				// check if everyone is spoof checked

				String NotSpoofChecked = "";

				if (this.getGhost().isSpoofChecks()) {
					for (GamePlayer p : this.m_Players) {
						if( !p.GetSpoofed()) {
							if (NotSpoofChecked.isEmpty()) {
								NotSpoofChecked = p.GetName();
							} else {
								NotSpoofChecked += ", " + p.GetName();
							}
						}
					}

					if( !NotSpoofChecked.isEmpty()) {
						SendAllChat(MessageFactory.create(Messages.PLAYERS_NOT_YET_SPOOF_CHECKED, NotSpoofChecked));

						if (this.getGhost().getBnetConnections().size( ) == 1) {
							Bytearray UniqueName = this.getGhost().getBnetConnections().get(0).GetUniqueName();

							if (m_GameState == Constants.GAME_PUBLIC)
								SendAllChat(MessageFactory.create(Messages.MANUALLY_SPOOF_CHECK_BY_WHISPERING, UniqueName.toCharString()));
							else if (m_GameState == Constants.GAME_PRIVATE)
								SendAllChat(MessageFactory.create(Messages.SPOOF_CHECK_BY_WHISPERING, UniqueName.toCharString()));
						} else {
							//TODO todo: figure something out with multiple realms here
						}
					}
				}

				// check if everyone has been pinged enough (3 times) that the autokicker would have kicked them by now
				// see function EventPlayerPongToHost for the autokicker code

				String NotPinged = "";

				for (GamePlayer p : this.m_Players) {
					if (p.GetNumPings( ) < 3) {
						if (NotPinged.isEmpty()) {
							NotPinged = p.GetName();
						} else {
							NotPinged += ", " + p.GetName();
						}
					}
				}

				if (!NotPinged.isEmpty()) {
					SendAllChat(MessageFactory.create(Messages.PLAYERS_NOT_YET_PINGED, NotPinged));
				}
					
				// if no problems found start the game

				if( StillDownloading.isEmpty( ) && NotSpoofChecked.isEmpty( ) && NotPinged.isEmpty()) {
					m_CountDownStarted = true;
					m_CountDownCounter = 5;
				}
			}
		}
	}
	
	public void StartCountDownAuto (boolean requireSpoofChecks) {
		if (!m_CountDownStarted) {
			// check if enough players are present

			if (GetNumPlayers() < m_AutoStartPlayers) {
				SendAllChat(MessageFactory.create(Messages.WAITING_FOR_PLAYERS_BEFORE_AUTO_START, m_AutoStartPlayers - GetNumPlayers()));
				return;
			}

			// check if everyone has the map

			String StillDownloading = "";

			
			for (GameSlot slot : this.m_Slots) {
				if (slot.GetSlotStatus( ) == Constants.SLOTSTATUS_OCCUPIED && slot.GetComputer( ) == 0 && slot.GetDownloadStatus( ) != 100 ) {
					GamePlayer Player = GetPlayerFromPID(slot.GetPID( ) );

					if (Player != null) {
						if( StillDownloading.isEmpty( ) )
							StillDownloading = Player.GetName( );
						else
							StillDownloading += ", " + Player.GetName( );
					}
				}
			}

			if (!StillDownloading.isEmpty()) {
				SendAllChat(MessageFactory.create(Messages.PLAYERS_STILL_DOWNLOADING, StillDownloading));
			}

			String NotSpoofChecked = "";

			if( requireSpoofChecks )
			{
				// check if everyone is spoof checked

				if (this.getGhost().isSpoofChecks()) {
					for (GamePlayer p : this.m_Players) {
						if( !p.GetSpoofed()) {
							if (NotSpoofChecked.isEmpty()) {
								NotSpoofChecked = p.GetName();
							} else {
								NotSpoofChecked += ", " + p.GetName();
							}
						}
					}

					if (!NotSpoofChecked.isEmpty()) {
						SendAllChat(MessageFactory.create(Messages.PLAYERS_NOT_YET_SPOOF_CHECKED, NotSpoofChecked));

						/*

						if (this.jGhost.getBnetConnections().size( ) == 1) {
							Bytearray UniqueName = this.jGhost.getBnetConnections().get(0).GetUniqueName();

							if (m_GameState == Constants.GAME_PUBLIC)
								SendAllChat( Messages.MANUALLY_SPOOF_CHECK_BY_WHISPERING.createMessage(UniqueName.toCharString()));
							else if (m_GameState == Constants.GAME_PRIVATE)
								SendAllChat( Messages.SPOOF_CHECK_BY_WHISPERING.createMessage(UniqueName.toCharString()));
						} else {
							//TODO todo: figure something out with multiple realms here
						}

						*/
					}
				}
			}

			// check if everyone has been pinged enough (3 times) that the autokicker would have kicked them by now
			// see function EventPlayerPongToHost for the autokicker code

			// check if everyone has been pinged enough (3 times) that the autokicker would have kicked them by now
			// see function EventPlayerPongToHost for the autokicker code

			String NotPinged = "";

			for (GamePlayer p : this.m_Players) {
				if (p.GetNumPings( ) < 3) {
					if (NotPinged.isEmpty()) {
						NotPinged = p.GetName();
					} else {
						NotPinged += ", " + p.GetName();
					}
				}
			}

			if (!NotPinged.isEmpty()) {
				SendAllChat(MessageFactory.create(Messages.PLAYERS_NOT_YET_PINGED, NotPinged));
			}

			// if no problems found start the game

			if( StillDownloading.isEmpty( ) && NotSpoofChecked.isEmpty( ) && NotPinged.isEmpty()) {
				m_CountDownStarted = true;
				m_CountDownCounter = 10;
			}
		}
	}
	
	public void StopPlayers (String reason) {
		// disconnect every player and set their left reason to the passed string
		// we use this function when we want the code in the Update function to run before the destructor (e.g. saving players to the database)
		// therefore calling this function when m_GameLoading || m_GameLoaded is roughly equivalent to setting m_Exiting = true
		// the only difference is whether the code in the Update function is executed or not

		for (GamePlayer p : this.m_Players) {
			p.SetDeleteMe(true);
			p.SetLeftReason(reason);
			p.SetLeftCode(Constants.PLAYERLEAVE_LOST);
		}
	}
	
	public void StopLaggers (String reason) {
		for (GamePlayer p : this.m_Players) {
			if (p.GetLagging()) {
				p.SetDeleteMe(true);
				p.SetLeftReason(reason);
				p.SetLeftCode(Constants.PLAYERLEAVE_DISCONNECT);
			}
		}
	}
	
	public void CreateVirtualHost () {
		if( m_VirtualHostPID != 255 )
			return;

		m_VirtualHostPID = GetNewPID( );
		Bytearray IP = new Bytearray();
		IP.addInt( 0 ); // 4 char with 0
		SendAll(m_Protocol.SEND_W3GS_PLAYERINFO(m_VirtualHostPID, m_VirtualHostName, IP, IP));
	}
	
	public void DeleteVirtualHost () {
		if( m_VirtualHostPID == 255 ) {
			return;
		}

		SendAll(m_Protocol.SEND_W3GS_PLAYERLEAVE_OTHERS(m_VirtualHostPID, Constants.PLAYERLEAVE_LOBBY));
		m_VirtualHostPID = 255;
	}
	
	public void CreateFakePlayer () {
		if( m_FakePlayerPID != 255 )
			return;

		char SID = GetEmptySlot( false );

		if (SID < m_Slots.size()) {
			m_FakePlayerPID = GetNewPID( );
			Bytearray IP = new Bytearray();
			IP.addInt(0); //4 char with value 0
			SendAll(m_Protocol.SEND_W3GS_PLAYERINFO( m_FakePlayerPID, "FakePlayer", IP, IP));
			m_Slots.set(SID, new GameSlot(m_FakePlayerPID,(char) 100, Constants.SLOTSTATUS_OCCUPIED,(char) 0, m_Slots.get(SID).GetTeam( ), m_Slots.get(SID).GetColour( ), m_Slots.get(SID).GetRace()));
			SendAllSlotInfo();
		}
	}
	
	public void DeleteFakePlayer () {
		if( m_FakePlayerPID == 255 ) {
			return;
		}

		for (char i = 0; i < m_Slots.size( ); i++ ) {
			if( m_Slots.get(i).GetPID( ) == m_FakePlayerPID )
				m_Slots.set(i, new GameSlot((char) 0,(char) 255, Constants.SLOTSTATUS_OPEN,(char) 0, m_Slots.get(i).GetTeam( ), m_Slots.get(i).GetColour( ), m_Slots.get(i).GetRace())); 
		}

		SendAll(m_Protocol.SEND_W3GS_PLAYERLEAVE_OTHERS(m_FakePlayerPID, Constants.PLAYERLEAVE_LOBBY));
		SendAllSlotInfo( );
		m_FakePlayerPID = 255;
	}

	public void setGhost(JGhost jGhost) {
		this.jGhost = jGhost;
	}

	public JGhost getGhost() {
		return jGhost;
	}
		
}

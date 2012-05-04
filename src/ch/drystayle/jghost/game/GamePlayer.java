package ch.drystayle.jghost.game;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import ch.drystayle.jghost.bnet.BnetConnection;
import ch.drystayle.jghost.bnet.CommandPacket;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.i18n.Messages;
import ch.drystayle.jghost.net.GhostTcpSocket;
import ch.drystayle.jghost.protocol.GameProtocol;
import ch.drystayle.jghost.protocol.GameProtocolEnum;
import ch.drystayle.jghost.protocol.IncomingAction;
import ch.drystayle.jghost.protocol.IncomingChatPlayer;
import ch.drystayle.jghost.protocol.IncomingMapSize;
import ch.drystayle.jghost.util.TimeUtil;

public class GamePlayer extends PotentialPlayer {
	
	//---- State
	
	private char m_PID;
	private String m_Name;						// the player's name
	private Bytearray m_InternalIP;				// the player's internal IP address as reported by the player when connecting
	private List<Integer> m_Pings;				// store the last few (20) pings received so we can take an average
	private Queue<Integer> m_CheckSums;			// the last few checksums the player has sent (for detecting desyncs)
	private String m_LeftReason;				// the reason the player left the game
	private String m_SpoofedRealm;				// the realm the player last spoof checked on
	private Locale locale;
	private int m_LeftCode;						// the code to be sent in W3GS_PLAYERLEAVE_OTHERS for why this player left the game
	private int m_LoginAttempts;				// the number of attempts to login (used with CAdminGame only)
	private int m_SyncCounter;					// the number of keepalive packets received from this player
	private long m_JoinTime;					// GetTime when the player joined the game (used to delay sending the /whois a few seconds to allow for some lag)
	private int m_LastMapPartSent;				// the last mappart sent to the player (for sending more than one part at a time)
	private int m_LastMapPartAcked;				// the last mappart acknowledged by the player
	private int m_StartedDownloadingTicks;		// GetTicks when the player started downloading the map
	private int m_FinishedDownloadingTicks;		// GetTime when the player finished downloading the map
	private int m_FinishedLoadingTicks;			// GetTicks when the player finished loading the game
	private int m_StartedLaggingTicks;			// GetTicks when the player started lagging
	private int m_StatsSentTime;				// GetTime when we sent this player's stats to the chat (to prevent players from spamming !stats)
	private int m_StatsDotASentTime;			// GetTime when we sent this player's dota stats to the chat (to prevent players from spamming !statsdota)
	private double m_Score;						// the player's generic "score" for the matchmaking algorithm
	private boolean m_LoggedIn;					// if the player has logged in or not (used with CAdminGame only)
	private boolean m_Spoofed;					// if the player has spoof checked or not
	private boolean m_Reserved;					// if the player is reserved (VIP) or not
	private boolean m_WhoisSent;				// if we've sent a battle.net /whois for this player yet (for spoof checking)
	private boolean m_DownloadAllowed;			// if we're allowed to download the map or not (used with permission based map downloads)
	private boolean m_DownloadStarted;			// if we've started downloading the map or not
	private boolean m_DownloadFinished;			// if we've finished downloading the map or not
	private boolean m_FinishedLoading;			// if the player has finished loading or not
	private boolean m_Lagging;					// if the player is lagging or not (on the lag screen)
	private boolean m_DropVote;					// if the player voted to drop the laggers or not (on the lag screen)
	private boolean m_KickVote;					// if the player voted to kick a player or not
	private boolean m_Muted;					// if the player is muted or not
	private boolean m_LeftMessageSent;			// if the playerleave message has been sent or not
	private boolean m_Gimped;					// if the player is gimped or not

	//---- Constructors
	
	public GamePlayer (GameProtocol nProtocol, BaseGame nGame, GhostTcpSocket nSocket, char nPID, String nName, Bytearray nInternalIP, boolean nReserved ) {
		super(nProtocol, nGame, nSocket);
		m_PID = nPID;
		m_Name = nName;
		m_InternalIP = nInternalIP;
		m_LeftCode = Constants.PLAYERLEAVE_LOBBY;
		m_LoginAttempts = 0;
		m_SyncCounter = 0;
		m_JoinTime = TimeUtil.getTime();
		m_LastMapPartSent = 0;
		m_LastMapPartAcked = 0;
		m_StartedDownloadingTicks = 0;
		m_FinishedDownloadingTicks = 0;
		m_FinishedLoadingTicks = 0;
		m_StartedLaggingTicks = 0;
		m_StatsSentTime = 0;
		m_StatsDotASentTime = 0;
		m_Score = -100000.0;
		m_LoggedIn = false;
		m_Spoofed = false;
		m_Reserved = nReserved;
		m_WhoisSent = false;
		m_DownloadAllowed = false;
		m_DownloadStarted = false;
		m_DownloadFinished = false;
		m_FinishedLoading = false;
		m_Lagging = false;
		m_DropVote = false;
		m_KickVote = false;
		m_Muted = false;
		m_LeftMessageSent = false;
		m_Gimped = false;
		m_Pings = new ArrayList<Integer>();
		m_CheckSums = new LinkedList<Integer>();
		locale = this.m_Game.getGhost().getIpToCountry().getLocale(GetExternalIP());
	}
		
	public GamePlayer (PotentialPlayer potential, char nPID, String nName, Bytearray nInternalIP, boolean nReserved) {
		super(potential.getProtocol(), potential.getGame(), potential.GetSocket());
		m_Packets = potential.GetPackets( );
		m_PID = nPID;
		m_Name = nName;
		m_InternalIP = nInternalIP;
		m_LeftCode = Constants.PLAYERLEAVE_LOBBY;
		m_LoginAttempts = 0;
		m_SyncCounter = 0;
		m_JoinTime = TimeUtil.getTime();
		m_LastMapPartSent = 0;
		m_LastMapPartAcked = 0;
		m_StartedDownloadingTicks = 0;
		m_FinishedDownloadingTicks = 0;
		m_FinishedLoadingTicks = 0;
		m_StartedLaggingTicks = 0;
		m_StatsSentTime = 0;
		m_StatsDotASentTime = 0;
		m_Score = -100000.0;
		m_LoggedIn = false;
		m_Spoofed = false;
		m_Reserved = nReserved;
		m_WhoisSent = false;
		m_DownloadAllowed = false;
		m_DownloadStarted = false;
		m_DownloadFinished = false;
		m_FinishedLoading = false;
		m_Lagging = false;
		m_DropVote = false;
		m_KickVote = false;
		m_Muted = false;
		m_LeftMessageSent = false;
		m_Gimped = false;
		m_Pings = new ArrayList<Integer>();
		m_CheckSums = new LinkedList<Integer>();
		locale = this.m_Game.getGhost().getIpToCountry().getLocale(GetExternalIP());
	}

	//---- Methods
	
	public char GetPID ()						{ return m_PID; }
	public String GetName ()					{ return m_Name; }
	public Bytearray GetInternalIP ()			{ return m_InternalIP; }
	public int GetNumPings ()					{ return m_Pings.size( ); }
	public int GetNumCheckSums ()				{ return m_CheckSums.size( ); }
	public Queue<Integer> GetCheckSums ()		{ return m_CheckSums; }
	public String GetLeftReason ()				{ return m_LeftReason; }
	public String GetSpoofedRealm ()			{ return m_SpoofedRealm; }
	public int GetLeftCode ()					{ return m_LeftCode; }
	public int GetLoginAttempts ()				{ return m_LoginAttempts; }
	public int GetSyncCounter ()				{ return m_SyncCounter; }
	public long GetJoinTime ()					{ return m_JoinTime; }
	public int GetLastMapPartSent ()			{ return m_LastMapPartSent; }
	public int GetLastMapPartAcked ()			{ return m_LastMapPartAcked; }
	public int GetStartedDownloadingTicks ()	{ return m_StartedDownloadingTicks; }
	public int GetFinishedDownloadingTicks ()	{ return m_FinishedDownloadingTicks; }
	public int GetFinishedLoadingTicks ()		{ return m_FinishedLoadingTicks; }
	public int GetStartedLaggingTicks ()		{ return m_StartedLaggingTicks; }
	public int GetStatsSentTime ()				{ return m_StatsSentTime; }
	public int GetStatsDotASentTime ()			{ return m_StatsDotASentTime; }
	public double GetScore ()					{ return m_Score; }
	public boolean GetLoggedIn ()				{ return m_LoggedIn; }
	public boolean GetSpoofed ()				{ return m_Spoofed; }
	public boolean GetReserved ()				{ return m_Reserved; }
	public boolean GetWhoisSent ()				{ return m_WhoisSent; }
	public boolean GetDownloadAllowed ()		{ return m_DownloadAllowed; }
	public boolean GetDownloadStarted ()		{ return m_DownloadStarted; }
	public boolean GetDownloadFinished ()		{ return m_DownloadFinished; }
	public boolean GetFinishedLoading ()		{ return m_FinishedLoading; }
	public boolean GetLagging ()				{ return m_Lagging; }
	public boolean GetDropVote ()				{ return m_DropVote; }
	public boolean GetKickVote ()				{ return m_KickVote; }
	public boolean GetMuted ()					{ return m_Muted; }
	public boolean GetLeftMessageSent ()		{ return m_LeftMessageSent; }
	public boolean GetGimped ()					{ return m_Gimped; }

	public void SetLeftReason (String nLeftReason)							{ m_LeftReason = nLeftReason; }
	public void SetSpoofedRealm (String nSpoofedRealm)						{ m_SpoofedRealm = nSpoofedRealm; }
	public void SetLeftCode (int nLeftCode)									{ m_LeftCode = nLeftCode; }
	public void SetLoginAttempts (int nLoginAttempts)						{ m_LoginAttempts = nLoginAttempts; }
	public void SetSyncCounter (int nSyncCounter)							{ m_SyncCounter = nSyncCounter; }
	public void SetLastMapPartSent (int nLastMapPartSent)					{ m_LastMapPartSent = nLastMapPartSent; }
	public void SetLastMapPartAcked (int nLastMapPartAcked )				{ m_LastMapPartAcked = nLastMapPartAcked; }
	public void SetStartedDownloadingTicks (int nStartedDownloadingTicks)	{ m_StartedDownloadingTicks = nStartedDownloadingTicks; }
	public void SetFinishedDownloadingTicks (int nFinishedDownloadingTicks)	{ m_FinishedDownloadingTicks = nFinishedDownloadingTicks; }
	public void SetStartedLaggingTicks (int nStartedLaggingTicks)			{ m_StartedLaggingTicks = nStartedLaggingTicks; }
	public void SetStatsSentTime (int nStatsSentTime)						{ m_StatsSentTime = nStatsSentTime; }
	public void SetStatsDotASentTime (int nStatsDotASentTime)				{ m_StatsDotASentTime = nStatsDotASentTime; }
	public void SetScore (double nScore)									{ m_Score = nScore; }
	public void SetLoggedIn (boolean nLoggedIn)								{ m_LoggedIn = nLoggedIn; }
	public void SetSpoofed (boolean nSpoofed)								{ m_Spoofed = nSpoofed; }
	public void SetReserved (boolean nReserved)								{ m_Reserved = nReserved; }
	public void SetDownloadAllowed (boolean nDownloadAllowed)				{ m_DownloadAllowed = nDownloadAllowed; }
	public void SetDownloadStarted (boolean nDownloadStarted)				{ m_DownloadStarted = nDownloadStarted; }
	public void SetDownloadFinished (boolean nDownloadFinished)				{ m_DownloadFinished = nDownloadFinished; }
	public void SetLagging (boolean nLagging)								{ m_Lagging = nLagging; }
	public void SetDropVote (boolean nDropVote)								{ m_DropVote = nDropVote; }
	public void SetKickVote (boolean nKickVote)								{ m_KickVote = nKickVote; }
	public void SetMuted (boolean nMuted)									{ m_Muted = nMuted; }
	public void SetLeftMessageSent (boolean nLeftMessageSent)				{ m_LeftMessageSent = nLeftMessageSent; }
	public void SetGimped (boolean nGimped)									{ m_Gimped = nGimped; }

	public int GetPing (boolean LCPing) {
		// just average all the pings in the vector, nothing fancy

		if (m_Pings.isEmpty()) {
			return 0;
		}

		int AvgPing = 0;

		for (int i = 0; i < m_Pings.size( ); i++ ) {
			AvgPing += m_Pings.get(i);
		}
			
		AvgPing /= m_Pings.size( );

		if (LCPing) {
			return AvgPing / 2;
		} else {
			return AvgPing;
		}
	}

	// processing functions

	public boolean Update (Object fd) {
		
		// wait 4 seconds after joining before sending the /whois or /w
		// if we send the /whois too early battle.net may not have caught up with where the player is and return erroneous results
		// when connecting to multiple realms we send a /whois or /w on every realm

		if( m_Game.getGhost().isSpoofChecks() && !m_WhoisSent && TimeUtil.getTime() >= m_JoinTime + 4000) {
			// todotodo: we could get kicked from battle.net for sending a command with invalid characters, do some basic checking

			
			for (BnetConnection bnetConnection : m_Game.getGhost().getBnetConnections()) {
				if (m_Game.GetGameState() == Constants.GAME_PUBLIC) {
					bnetConnection.QueueChatCommand("/whois " + m_Name);
				} else if (m_Game.GetGameState() == Constants.GAME_PRIVATE) {
					bnetConnection.QueueChatCommand(Messages.SPOOF_CHECK_BY_REPLYING.createMessage(), m_Name, true);
				}
			}

			m_WhoisSent = true;
		}

		// check for socket timeouts
		// if we don't receive anything from a player for 30 seconds we can assume they've dropped
		// this works because in the lobby we send pings every 5 seconds and expect a response to each one
		// and in the game the Warcraft 3 client sends keepalives frequently (at least once per second it looks like)
		if (m_Socket != null && TimeUtil.getTime() >= m_Socket.GetLastRecv( ) + 30000 ) {
			m_Game.EventPlayerDisconnectTimedOut( this );
		}

		// base class update

		boolean Deleting = super.Update(fd);

		if (Deleting) {
			// try to find out why we're requesting deletion
			// in cases other than the ones covered here m_LeftReason should have been set when m_DeleteMe was set

			if (m_Error) {
				m_Game.EventPlayerDisconnectPlayerError(this);
			}

			if (m_Socket != null) {
				if (m_Socket.HasError()) {
					m_Game.EventPlayerDisconnectSocketError( this );
				}

				if (!m_Socket.GetConnected()) {
					m_Game.EventPlayerDisconnectConnectionClosed( this );
				}
			}
		}

		return Deleting;
	}
	
	public void ProcessPackets() {
		if (m_Socket == null) {
			return;
		}

		IncomingAction Action = null;
		IncomingChatPlayer ChatPlayer = null;
		IncomingMapSize MapSize = null;
		boolean HasMap = false;
		int CheckSum = 0;
		int Pong = 0;

		// process all the received packets in the m_Packets queue

		while (!m_Packets.isEmpty()) {
			CommandPacket Packet = m_Packets.poll();

			if (Packet.GetPacketType( ) == Constants.W3GS_HEADER_CONSTANT ) {
				GameProtocolEnum protocol = GameProtocolEnum.forVal((char) Packet.GetID());
				
				switch (protocol) {
				case W3GS_LEAVEGAME:
					if(m_Protocol.RECEIVE_W3GS_LEAVEGAME(Packet.GetData())) {
						m_Game.EventPlayerLeft(this);
					}

					break;

				case W3GS_GAMELOADED_SELF:
					if(m_Protocol.RECEIVE_W3GS_GAMELOADED_SELF(Packet.GetData())) {
						m_FinishedLoading = true;
						m_FinishedLoadingTicks = TimeUtil.getTicks();
						m_Game.EventPlayerLoaded(this);
					}

					break;

				case W3GS_OUTGOING_ACTION:
					Action = m_Protocol.RECEIVE_W3GS_OUTGOING_ACTION(Packet.GetData(), m_PID);

					if (Action != null) {
						m_Game.EventPlayerAction( this, Action );
					}

					// don't delete Action here because the game is going to store it in a queue and delete it later

					break;

				case W3GS_OUTGOING_KEEPALIVE:
					CheckSum = m_Protocol.RECEIVE_W3GS_OUTGOING_KEEPALIVE(Packet.GetData());
					m_CheckSums.add(CheckSum);
					m_SyncCounter++;
					m_Game.EventPlayerKeepAlive( this, CheckSum );
					break;

				case W3GS_CHAT_TO_HOST:
					ChatPlayer = m_Protocol.RECEIVE_W3GS_CHAT_TO_HOST(Packet.GetData());

					if (ChatPlayer != null) {
						m_Game.EventPlayerChatToHost( this, ChatPlayer );
					}
					ChatPlayer = null;
					break;

				case W3GS_DROPREQ:
					// todotodo: no idea what's in this packet

					if( !m_DropVote ) {
						m_DropVote = true;
						m_Game.EventPlayerDropRequest( this );
					}

					break;

				case W3GS_MAPSIZE:
					MapSize = m_Protocol.RECEIVE_W3GS_MAPSIZE(Packet.GetData(), m_Game.getGhost().getMap().GetMapSize());

					if (MapSize != null) {
						m_Game.EventPlayerMapSize(this, MapSize);
					}

					MapSize = null;
					break;

				case W3GS_PONG_TO_HOST:
					Pong = m_Protocol.RECEIVE_W3GS_PONG_TO_HOST(Packet.GetData());

					// we discard pong values of 1
					// the client sends one of these when connecting plus we return 1 on error to kill two birds with one stone

					if (Pong != 1) {
						// we also discard pong values when we're downloading because they're almost certainly inaccurate
						// this statement also gives the player a 5 second grace period after downloading the map to allow queued (i.e. delayed) ping packets to be ignored

						if (!m_DownloadStarted || ( m_DownloadFinished && TimeUtil.getTicks() >= m_FinishedDownloadingTicks + 5000 ) ) {
							// we also discard pong values when anyone else is downloading if we're configured to

							if ( m_Game.getGhost().isPingDuringDownloads() || !m_Game.IsDownloading()) {
								m_Pings.add(TimeUtil.getTicks() - Pong );

								if ( m_Pings.size( ) > 20 ) {
									m_Pings.remove(0);
								}
							}
						}
					}

					m_Game.EventPlayerPongToHost(this, Pong);
					break;
				}
			}
		}
	}

	public void delete() {
		if (this.m_Socket != null) {
			this.m_Socket.Disconnect();
		}
	}
	
	public Locale getLocale () {
		return this.locale;
	}
}

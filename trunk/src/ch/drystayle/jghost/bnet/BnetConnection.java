package ch.drystayle.jghost.bnet;

import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.bnls.BnlsClient;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;
import ch.drystayle.jghost.db.Ban;
import ch.drystayle.jghost.game.BaseGame;
import ch.drystayle.jghost.game.SaveGame;
import ch.drystayle.jghost.map.Map;
import ch.drystayle.jghost.net.GhostTcpSocket;
import ch.drystayle.jghost.protocol.BnetProtocol;
import ch.drystayle.jghost.protocol.IncomingChatEvent;
import ch.drystayle.jghost.protocol.IncomingClanList;
import ch.drystayle.jghost.protocol.IncomingFriendList;
import ch.drystayle.jghost.protocol.IncomingGameHost;
import ch.drystayle.jghost.protocol.BnetProtocol.Protocol;
import ch.drystayle.jghost.util.TimeUtil;

public class BnetConnection {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BnetConnection.class);
	
	//---- State
	
	private JGhost jGhost;
	private GhostTcpSocket m_Socket;							// the connection to battle.net
	private BnetProtocol m_Protocol;						// battle.net protocol
	private BnlsClient m_BNLSClient;						// the BNLS client (for external warden handling)
	private Queue<CommandPacket> m_Packets;					// queue of incoming packets
	private BNCSUtilInterface m_BNCSUtil;					// the interface to the bncsutil library (used for logging into battle.net)
	private Queue<Bytearray> m_OutPackets;					// queue of outgoing packets to be sent (to prevent getting kicked for flooding)
	private List<IncomingFriendList> m_Friends;				// vector of friends
	private List<IncomingClanList> m_Clans;					// vector of clan members
	private List<String> m_Admins;							// vector of cached admins
	private List<Ban> m_Bans;							// vector of cached bans
	private boolean m_Exiting;								// set to true and this class will be deleted next update
	private String m_Server;								// battle.net server to connect to
	private String m_BNLSServer;							// BNLS server to connect to (for warden handling)
	private short m_BNLSPort;								// BNLS port
	private int m_BNLSWardenCookie;							// BNLS warden cookie
	private String m_CDKeyROC;								// ROC CD key
	private String m_CDKeyTFT;								// TFT CD key
	private String m_CountryAbbrev;							// country abbreviation
	private String m_Country;								// country
	private String m_UserName;								// battle.net username
	private String m_UserPassword;							// battle.net password
	private String m_FirstChannel;							// the first chat channel to join upon entering chat (note: we hijack this to store the last channel when entering a game)
	private String m_CurrentChannel;						// the current chat channel
	private String m_RootAdmin;								// the root admin
	private char m_CommandTrigger;							// the character prefix to identify commands
	private char m_War3Version;								// custom warcraft 3 version for PvPGN users
	private Bytearray m_EXEVersion;							// custom exe version for PvPGN users
	private Bytearray m_EXEVersionHash;						// custom exe version hash for PvPGN users
	private String m_PasswordHashType;						// password hash type for PvPGN users
	private int m_MaxMessageLength;							// maximum message length for PvPGN users
	private long m_NextConnectTime;							// GetTime when we should try connecting to battle.net next (after we get disconnected)
	private long m_LastNullTime;								// GetTime when the last null packet was sent for detecting disconnects
	private int m_LastOutPacketTicks;						// GetTicks when the last packet was sent for the m_OutPackets queue
	private long m_LastAdminRefreshTime;						// GetTime when the admin list was last refreshed from the database
	private long m_LastBanRefreshTime;						// GetTime when the ban list was last refreshed from the database
	private boolean m_WaitingToConnect;						// if we're waiting to reconnect to battle.net after being disconnected
	private boolean m_LoggedIn;								// if we've logged into battle.net or not
	private boolean m_InChat;								// if we've entered chat or not (but we're not necessarily in a chat channel yet)
	private boolean m_HoldFriends;							// whether to auto hold friends when creating a game or not
	private boolean m_HoldClan;								// whether to auto hold clan members when creating a game or not

	//---- Constructors
	
	public BnetConnection (JGhost nGHost, String nServer, String nBNLSServer, short nBNLSPort, int nBNLSWardenCookie, String nCDKeyROC, String nCDKeyTFT, String nCountryAbbrev, String nCountry, String nUserName, String nUserPassword, String nFirstChannel, String nRootAdmin, char nCommandTrigger, boolean nHoldFriends, boolean nHoldClan, char nWar3Version, Bytearray nEXEVersion, Bytearray nEXEVersionHash, String nPasswordHashType, int nMaxMessageLength) {
		jGhost = nGHost;
		m_Socket = new GhostTcpSocket();
		m_Protocol = new BnetProtocol();
		m_BNLSClient = null;
		m_BNCSUtil = new BNCSUtilInterface( nUserName, nUserPassword );
		m_Exiting = false;
		m_Server = nServer;
		m_BNLSServer = nBNLSServer;
		m_BNLSPort = nBNLSPort;
		m_BNLSWardenCookie = nBNLSWardenCookie;
		m_CDKeyROC = nCDKeyROC.toUpperCase();
		m_CDKeyTFT = nCDKeyTFT.toUpperCase();
		m_CountryAbbrev = nCountryAbbrev;
		m_Country = nCountry;
		m_UserName = nUserName;
		m_UserPassword = nUserPassword;
		m_FirstChannel = nFirstChannel;
		m_RootAdmin = nRootAdmin.toLowerCase();
		m_CommandTrigger = nCommandTrigger;
		m_War3Version = nWar3Version;
		m_EXEVersion = nEXEVersion;
		m_EXEVersionHash = nEXEVersionHash;
		m_PasswordHashType = nPasswordHashType;
		m_MaxMessageLength = nMaxMessageLength;
		m_NextConnectTime = TimeUtil.getTime();
		m_LastNullTime = 0;
		m_LastOutPacketTicks = 0;
		m_LastAdminRefreshTime = TimeUtil.getTime();
		m_LastBanRefreshTime = TimeUtil.getTime();
		m_WaitingToConnect = true;
		m_LoggedIn = false;
		m_InChat = false;
		m_HoldFriends = nHoldFriends;
		m_HoldClan = nHoldClan;
		m_Packets = new LinkedList<CommandPacket>();
		m_OutPackets = new LinkedList<Bytearray>();
		m_Admins = new ArrayList<String>();
		m_CurrentChannel = "";
		m_Clans = new ArrayList<IncomingClanList>();
		m_Friends = new ArrayList<IncomingFriendList>();
	}

	
	//---- Methods

	public boolean GetExiting ()				{ return m_Exiting; }
	public String GetServer ()					{ return m_Server; }
	public String GetCDKeyROC ()				{ return m_CDKeyROC; }
	public String GetCDKeyTFT ()				{ return m_CDKeyTFT; }
	public String GetUserName ()				{ return m_UserName; }
	public String GetUserPassword ()			{ return m_UserPassword; }
	public String GetFirstChannel ()			{ return m_FirstChannel; }
	public String GetCurrentChannel ()			{ return m_CurrentChannel; }
	public String GetRootAdmin ()				{ return m_RootAdmin; }
	public char GetCommandTrigger ()			{ return m_CommandTrigger; }
	public Bytearray GetEXEVersion ()			{ return m_EXEVersion; }
	public Bytearray GetEXEVersionHash ()		{ return m_EXEVersionHash; }
	public String GetPasswordHashType ()		{ return m_PasswordHashType; }
	public boolean GetLoggedIn ()				{ return m_LoggedIn; }
	public boolean GetInChat ()					{ return m_InChat; }
	public boolean GetHoldFriends ()			{ return m_HoldFriends; }
	public boolean GetHoldClan ()				{ return m_HoldClan; }
	public int GetOutPacketsQueued ()			{ return m_OutPackets.size( ); }
	
	public Bytearray GetUniqueName () {
		return m_Protocol.GetUniqueName();
	}

	// processing functions

	public int SetFD (Object fd, int nfds) {
		int NumFDs = 0;

		//TODO socket ?
		/*if( !m_Socket->HasError( ) && m_Socket->GetConnected( ) )
		{
			m_Socket->SetFD( (fd_set *)fd, nfds );
			NumFDs++;

			if( m_BNLSClient )
				NumFDs += m_BNLSClient->SetFD( fd, nfds );
		}*/

		return NumFDs;
	}
	
	public boolean Update (Object fd) {
		//
		// update callables
		//

		//TODO db..
		/*for( vector<PairedAdminCount> :: iterator i = m_PairedAdminCounts.begin( ); i != m_PairedAdminCounts.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				uint32_t Count = i->second->GetResult( );

				if( Count == 0 )
					QueueChatCommand( m_GHost->m_Language->ThereAreNoAdmins( m_Server ), i->first, !i->first.empty( ) );
				else if( Count == 1 )
					QueueChatCommand( m_GHost->m_Language->ThereIsAdmin( m_Server ), i->first, !i->first.empty( ) );
				else
					QueueChatCommand( m_GHost->m_Language->ThereAreAdmins( m_Server, UTIL_ToString( Count ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedAdminCounts.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedAdminAdd> :: iterator i = m_PairedAdminAdds.begin( ); i != m_PairedAdminAdds.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				if( i->second->GetResult( ) )
				{
					AddAdmin( i->second->GetUser( ) );
					QueueChatCommand( m_GHost->m_Language->AddedUserToAdminDatabase( m_Server, i->second->GetUser( ) ), i->first, !i->first.empty( ) );
				}
				else
					QueueChatCommand( m_GHost->m_Language->ErrorAddingUserToAdminDatabase( m_Server, i->second->GetUser( ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedAdminAdds.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedAdminRemove> :: iterator i = m_PairedAdminRemoves.begin( ); i != m_PairedAdminRemoves.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				if( i->second->GetResult( ) )
				{
					RemoveAdmin( i->second->GetUser( ) );
					QueueChatCommand( m_GHost->m_Language->DeletedUserFromAdminDatabase( m_Server, i->second->GetUser( ) ), i->first, !i->first.empty( ) );
				}
				else
					QueueChatCommand( m_GHost->m_Language->ErrorDeletingUserFromAdminDatabase( m_Server, i->second->GetUser( ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedAdminRemoves.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedBanCount> :: iterator i = m_PairedBanCounts.begin( ); i != m_PairedBanCounts.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				uint32_t Count = i->second->GetResult( );

				if( Count == 0 )
					QueueChatCommand( m_GHost->m_Language->ThereAreNoBannedUsers( m_Server ), i->first, !i->first.empty( ) );
				else if( Count == 1 )
					QueueChatCommand( m_GHost->m_Language->ThereIsBannedUser( m_Server ), i->first, !i->first.empty( ) );
				else
					QueueChatCommand( m_GHost->m_Language->ThereAreBannedUsers( m_Server, UTIL_ToString( Count ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedBanCounts.erase( i );
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
					AddBan( i->second->GetUser( ), i->second->GetIP( ), i->second->GetGameName( ), i->second->GetAdmin( ), i->second->GetReason( ) );
					QueueChatCommand( m_GHost->m_Language->BannedUser( i->second->GetServer( ), i->second->GetUser( ) ), i->first, !i->first.empty( ) );
				}
				else
					QueueChatCommand( m_GHost->m_Language->ErrorBanningUser( i->second->GetServer( ), i->second->GetUser( ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedBanAdds.erase( i );
			}
			else
				i++;
		}

		for( vector<PairedBanRemove> :: iterator i = m_PairedBanRemoves.begin( ); i != m_PairedBanRemoves.end( ); )
		{
			if( i->second->GetReady( ) )
			{
				if( i->second->GetResult( ) )
				{
					RemoveBan( i->second->GetUser( ) );
					QueueChatCommand( m_GHost->m_Language->UnbannedUser( i->second->GetUser( ) ), i->first, !i->first.empty( ) );
				}
				else
					QueueChatCommand( m_GHost->m_Language->ErrorUnbanningUser( i->second->GetUser( ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedBanRemoves.erase( i );
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
					QueueChatCommand( m_GHost->m_Language->HasPlayedGamesWithThisBot( i->second->GetName( ), GamePlayerSummary->GetFirstGameDateTime( ), GamePlayerSummary->GetLastGameDateTime( ), UTIL_ToString( GamePlayerSummary->GetTotalGames( ) ), UTIL_ToString( (float)GamePlayerSummary->GetAvgLoadingTime( ) / 1000, 2 ), UTIL_ToString( GamePlayerSummary->GetAvgLeftPercent( ) ) ), i->first, !i->first.empty( ) );
				else
					QueueChatCommand( m_GHost->m_Language->HasntPlayedGamesWithThisBot( i->second->GetName( ) ), i->first, !i->first.empty( ) );

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

					QueueChatCommand( Summary, i->first, !i->first.empty( ) );
				}
				else
					QueueChatCommand( m_GHost->m_Language->HasntPlayedDotAGamesWithThisBot( i->second->GetName( ) ), i->first, !i->first.empty( ) );

				m_GHost->m_DB->RecoverCallable( i->second );
				delete i->second;
				i = m_PairedDPSChecks.erase( i );
			}
			else
				i++;
		}

		// refresh the admin list every 5 minutes

		if( !m_CallableAdminList && GetTime( ) >= m_LastAdminRefreshTime + 300 ) {
			m_CallableAdminList = m_GHost->m_DB->ThreadedAdminList( m_Server );
		}

		if( m_CallableAdminList && m_CallableAdminList->GetReady( ) )
		{
			// CONSOLE_Print( "[BNET: " + m_Server + "] refreshed admin list (" + UTIL_ToString( m_Admins.size( ) ) + " -> " + UTIL_ToString( m_CallableAdminList->GetResult( ).size( ) ) + " admins)" );
			m_Admins = m_CallableAdminList->GetResult( );
			m_GHost->m_DB->RecoverCallable( m_CallableAdminList );
			delete m_CallableAdminList;
			m_CallableAdminList = NULL;
			m_LastAdminRefreshTime = GetTime( );
		}

		// refresh the ban list every 60 minutes

		if( !m_CallableBanList && GetTime( ) >= m_LastBanRefreshTime + 3600 )
			m_CallableBanList = m_GHost->m_DB->ThreadedBanList( m_Server );

		if( m_CallableBanList && m_CallableBanList->GetReady( ) )
		{
			// CONSOLE_Print( "[BNET: " + m_Server + "] refreshed ban list (" + UTIL_ToString( m_Bans.size( ) ) + " -> " + UTIL_ToString( m_CallableBanList->GetResult( ).size( ) ) + " bans)" );

			for( vector<CDBBan *> :: iterator i = m_Bans.begin( ); i != m_Bans.end( ); i++ )
				delete *i;

			m_Bans = m_CallableBanList->GetResult( );
			m_GHost->m_DB->RecoverCallable( m_CallableBanList );
			delete m_CallableBanList;
			m_CallableBanList = NULL;
			m_LastBanRefreshTime = GetTime( );
		}*/

		// we return at the end of each if statement so we don't have to deal with errors related to the order of the if statements
		// that means it might take a few ms longer to complete a task involving multiple steps (in this case, reconnecting) due to blocking or sleeping
		// but it's not a big deal at all, maybe 100ms in the worst possible case (based on a 50ms blocking time)

		if (m_Socket.HasError()) {
			// the socket has an error

			LOG.warn("[" + m_Server + "] disconnected from battle.net due to socket error" );
			LOG.info("[" + m_Server + "] waiting 30 seconds to reconnect" );
			
			jGhost.EventBNETDisconnected( this );
			m_BNLSClient = null;
			m_BNCSUtil.Reset(m_UserName, m_UserPassword);
			m_Socket.Reset();
			m_NextConnectTime = TimeUtil.getTime() + 30000;
			m_LoggedIn = false;
			m_InChat = false;
			m_WaitingToConnect = true;
			return m_Exiting;
		}

		if (!m_Socket.GetConnecting() && !m_Socket.GetConnected() && !m_WaitingToConnect) {
			// the socket was disconnected

			LOG.warn("[" + m_Server + "] disconnected from battle.net due to socket not connected" );
			LOG.info("[" + m_Server + "] waiting 30 seconds to reconnect" );
			jGhost.EventBNETDisconnected( this );
			m_BNLSClient = null;
			m_BNCSUtil.Reset( m_UserName, m_UserPassword );
			m_Socket.Reset( );
			m_NextConnectTime = TimeUtil.getTime() + 30000;
			m_LoggedIn = false;
			m_InChat = false;
			m_WaitingToConnect = true;
			return m_Exiting;
		}

		if (m_Socket.GetConnected()) {
			// the socket is connected and everything appears to be working properly

			m_Socket.DoRecv(fd);
			ExtractPackets();
			ProcessPackets();

			// update the BNLS client

			if (m_BNLSClient != null) {
				if (m_BNLSClient.Update(fd)) {
					LOG.warn("[" + m_Server + "] deleting BNLS client" );
					m_BNLSClient = null;
				} else {
					Bytearray WardenResponse = m_BNLSClient.GetWardenResponse();

					if (!WardenResponse.isEmpty()) {
						m_OutPackets.add( m_Protocol.SEND_SID_WARDEN(WardenResponse));
					}
				}
			}

			// check if at least one packet is waiting to be sent and if we've waited long enough to prevent flooding
			// the original VB source used a formula based on the message length but 2.9 seconds seems to work fine
			// note: updated this from 2 seconds to 2.5 then to 2.9 seconds because less is NOT enough

			if (!m_OutPackets.isEmpty( ) && TimeUtil.getTicks( ) >= m_LastOutPacketTicks + 2900 ) {
				m_Socket.PutBytes(m_OutPackets.poll());
				m_LastOutPacketTicks = TimeUtil.getTicks();
			}

			// send a null packet every 60 seconds to detect disconnects

			if(TimeUtil.getTime( ) >= m_LastNullTime + 60000 && TimeUtil.getTicks() >= m_LastOutPacketTicks + 60000 ) {
				m_Socket.PutBytes(m_Protocol.SEND_SID_NULL());
				m_LastNullTime = TimeUtil.getTime( );
			}

			m_Socket.doSend();
			return m_Exiting;
		}

		if(m_Socket.GetConnecting()) {
			// we are currently attempting to connect to battle.net

			if(m_Socket.CheckConnect()) {
				// the connection attempt completed

				LOG.info("[" + m_Server + "] connected" );
				jGhost.EventBNETConnected( this );
				m_Socket.PutBytes(m_Protocol.SEND_PROTOCOL_INITIALIZE_SELECTOR( ) );
				m_Socket.PutBytes(m_Protocol.SEND_SID_AUTH_INFO(m_War3Version, m_CountryAbbrev, m_Country ) );
				m_Socket.doSend();
				m_LastNullTime = TimeUtil.getTime();
				m_LastOutPacketTicks = TimeUtil.getTicks();

				m_OutPackets.clear();
				
				this.m_Socket.setConnecting(false);
				return m_Exiting;
			} else if(TimeUtil.getTime( ) >= m_NextConnectTime + 15000 ) {
				// the connection attempt timed out (15 seconds)

				LOG.info("[" + m_Server + "] connect timed out" );
				LOG.info("[" + m_Server + "] waiting 30 seconds to reconnect" );
				jGhost.EventBNETConnectTimedOut( this );
				m_Socket.Reset();
				m_NextConnectTime = TimeUtil.getTime() + 30000;
				m_WaitingToConnect = true;
				return m_Exiting;
			}
		}

		if(!m_Socket.GetConnecting() && !m_Socket.GetConnected() && TimeUtil.getTime() >= m_NextConnectTime)
		{
			// attempt to connect to battle.net

			LOG.info("[" + m_Server + "] connecting to server [" + m_Server + "] on port 6112" );
			jGhost.EventBNETConnecting( this );

			if (!jGhost.getBindAddress().isEmpty()) {
				LOG.info("[" + m_Server + "] attempting to bind to address [" + jGhost.getBindAddress() + "]" );
			}
			
			m_Socket.Connect(jGhost.getBindAddress(), m_Server, 6112 );
			m_WaitingToConnect = false;
			return m_Exiting;
		}

		return m_Exiting;
	}
	
	public void ExtractPackets () {
		// extract as many packets as possible from the socket's receive buffer and put them in the m_Packets queue

		Bytearray Bytes = m_Socket.GetBytes();

		// a packet is at least 4 bytes so loop as long as the buffer contains 4 bytes

		while (Bytes.size() >= 4) {
			// byte 0 is always 255

			if (Bytes.getChar(0) == Constants.BNET_HEADER_CONSTANT) {
				// bytes 2 and 3 contain the length of the packet

				short Length = Bytes.extract(2, 2).toShort();

				if( Length >= 4 ) {
					if( Bytes.size( ) >= Length ) {
						m_Packets.add(new CommandPacket(Constants.BNET_HEADER_CONSTANT, Bytes.getChar(1), Bytes.extract(0, Length)));
						Bytes = Bytes.extract(Length, Bytearray.END);
					} else {
						return;
					}
				} else {
					LOG.error("[" + m_Server + "] error - received invalid packet from battle.net (bad length), disconnecting");
					m_Socket.Disconnect( );
					return;
				}
			} else {
				LOG.error("[" + m_Server + "] error - received invalid packet from battle.net (bad header constant), disconnecting");
				m_Socket.Disconnect( );
				return;
			}
		}
	}
	
	public void ProcessPackets () {
		IncomingGameHost GameHost = null;
		IncomingChatEvent ChatEvent = null;
		Bytearray WardenData = new Bytearray();
		List<IncomingFriendList> Friends;
		List<IncomingClanList> Clans;

		// process all the received packets in the m_Packets queue
		// this normally means sending some kind of response
		
		while(!m_Packets.isEmpty()) {
			CommandPacket Packet = m_Packets.poll();
			
			if (Packet.GetPacketType() == Constants.BNET_HEADER_CONSTANT) {
				Protocol protocol = Packet.GetProtocol();
				
				if (protocol != null) {
					switch(protocol) {
					case SID_NULL:
						// warning: we do not respond to NULL packets with a NULL packet of our own
						// this is because PVPGN servers are programmed to respond to NULL packets so it will create a vicious cycle of useless traffic
						// official battle.net servers do not respond to NULL packets
	
						m_Protocol.RECEIVE_SID_NULL(Packet.GetData());
						break;
	
					case SID_GETADVLISTEX:
						GameHost = m_Protocol.RECEIVE_SID_GETADVLISTEX(Packet.GetData());
	
						if (GameHost != null) {
							LOG.info("[" + m_Server + "] joining game [" + GameHost.GetGameName() + "]" );
						}
							
						GameHost = null;
						break;
	
					case SID_ENTERCHAT:
						if (m_Protocol.RECEIVE_SID_ENTERCHAT(Packet.GetData())) {
							LOG.info( "[" + m_Server + "] joining channel [" + m_FirstChannel + "]" );
							m_InChat = true;
							m_Socket.PutBytes(m_Protocol.SEND_SID_JOINCHANNEL(m_FirstChannel));
						}
	
						break;
	
					case SID_CHATEVENT:
						ChatEvent = m_Protocol.RECEIVE_SID_CHATEVENT(Packet.GetData());
	
						if (ChatEvent != null) {
							ProcessChatEvent(ChatEvent);
						}
							
						ChatEvent = null;
						break;
	
					case SID_CHECKAD:
						m_Protocol.RECEIVE_SID_CHECKAD(Packet.GetData());
						break;
	
					case SID_STARTADVEX3:
						if (m_Protocol.RECEIVE_SID_STARTADVEX3(Packet.GetData())) {
							m_InChat = false;
							jGhost.EventBNETGameRefreshed(this);
						} else {
							LOG.warn("[" + m_Server + "] startadvex3 failed" );
							jGhost.EventBNETGameRefreshFailed(this);
						}
	
						break;
	
					case SID_PING:
						m_Socket.PutBytes(m_Protocol.SEND_SID_PING(m_Protocol.RECEIVE_SID_PING(Packet.GetData())));
						break;
	
					case SID_AUTH_INFO:
						if (m_Protocol.RECEIVE_SID_AUTH_INFO(Packet.GetData())) {
							if( m_BNCSUtil.HELP_SID_AUTH_CHECK(jGhost.getWarcraft3Path(), m_CDKeyROC, m_CDKeyTFT, m_Protocol.GetValueStringFormulaString(), m_Protocol.GetIX86VerFileNameString(), m_Protocol.GetClientToken(), m_Protocol.GetServerToken())) {
								// override the exe information generated by bncsutil if specified in the config file
								// apparently this is useful for pvpgn users
	
								if( m_EXEVersion.size( ) == 4 ) {
									LOG.info("[" + m_Server + "] using custom exe version bnet_custom_exeversion = " + m_EXEVersion.getChar(0) + " " + m_EXEVersion.getChar(1) + " " + m_EXEVersion.getChar(2) + " " + m_EXEVersion.getChar(3));
									m_BNCSUtil.SetEXEVersion(m_EXEVersion);
								}
	
								if( m_EXEVersionHash.size( ) == 4 ) {
									LOG.info("[" + m_Server + "] using custom exe version hash bnet_custom_exeversionhash = " + m_EXEVersionHash.getChar(0) + " " + m_EXEVersionHash.getChar(1) + " " + m_EXEVersionHash.getChar(2) + " " + m_EXEVersionHash.getChar(3));
									m_BNCSUtil.SetEXEVersionHash(m_EXEVersionHash);
								}
	
								m_Socket.PutBytes(m_Protocol.SEND_SID_AUTH_CHECK(m_Protocol.GetClientToken(), m_BNCSUtil.GetEXEVersion( ), m_BNCSUtil.GetEXEVersionHash( ), m_BNCSUtil.GetKeyInfoROC( ), m_BNCSUtil.GetKeyInfoTFT(), m_BNCSUtil.GetEXEInfo(), "GHost" ));
	
								// the Warden seed is the first 4 bytes of the ROC key hash
								// initialize the Warden handler
	
								if (!m_BNLSServer.isEmpty()) {
									LOG.info( "[" + m_Server + "] creating BNLS client" );
									m_BNLSClient = new BnlsClient(m_BNLSServer, m_BNLSPort, m_BNLSWardenCookie);
									m_BNLSClient.QueueWardenSeed(m_BNCSUtil.GetKeyInfoROC().extract(16, 4).toInt());
								}
							} else {
								LOG.error("[" + m_Server + "] logon failed - bncsutil key hash failed (check your Warcraft 3 path and cd keys), disconnecting" );
								m_Socket.Disconnect();
								return;
							}
						}
	
						break;
	
					case SID_AUTH_CHECK:
						if(m_Protocol.RECEIVE_SID_AUTH_CHECK(Packet.GetData())) {
							// cd keys accepted
	
							LOG.info("[" + m_Server + "] cd keys accepted");
							m_BNCSUtil.HELP_SID_AUTH_ACCOUNTLOGON();
							m_Socket.PutBytes(m_Protocol.SEND_SID_AUTH_ACCOUNTLOGON(m_BNCSUtil.GetClientKey(), m_UserName));
						} else {
							// cd keys not accepted
	
							switch(BnetProtocol.KeyResult.getEnum((char) m_Protocol.GetKeyState().toInt())) {
							case KR_ROC_KEY_IN_USE:
								LOG.warn( "[" + m_Server + "] logon failed - ROC CD key in use by user [" + m_Protocol.GetKeyStateDescription() + "], disconnecting" );
								break;
							case KR_TFT_KEY_IN_USE:
								LOG.warn( "[" + m_Server + "] logon failed - TFT CD key in use by user [" + m_Protocol.GetKeyStateDescription() + "], disconnecting" );
								break;
							case KR_OLD_GAME_VERSION:
								LOG.warn( "[" + m_Server + "] logon failed - game version is too old, disconnecting" );
								break;
							case KR_INVALID_VERSION:
								LOG.warn( "[" + m_Server + "] logon failed - game version is invalid, disconnecting" );
								break;
							default:
								LOG.warn( "[" + m_Server + "] logon failed - cd keys not accepted, disconnecting" );
								break;
							}
	
							m_Socket.Disconnect();
							return;
						}
	
						break;
	
					case SID_AUTH_ACCOUNTLOGON:
						if(m_Protocol.RECEIVE_SID_AUTH_ACCOUNTLOGON(Packet.GetData())) {
							LOG.info( "[BNET: " + m_Server + "] username [" + m_UserName + "] accepted" );
	
							if (m_PasswordHashType == "pvpgn") {
								// pvpgn logon
	
								LOG.info("[" + m_Server + "] using pvpgn logon type (for pvpgn servers only)" );
								m_BNCSUtil.HELP_PvPGNPasswordHash(m_UserPassword);
								m_Socket.PutBytes(m_Protocol.SEND_SID_AUTH_ACCOUNTLOGONPROOF(m_BNCSUtil.GetPvPGNPasswordHash()));
							} else {
								// battle.net logon
	
								LOG.info( "[" + m_Server + "] using battle.net logon type (for official battle.net servers only)" );
								m_BNCSUtil.HELP_SID_AUTH_ACCOUNTLOGONPROOF(m_Protocol.GetSalt(), m_Protocol.GetServerPublicKey());
								m_Socket.PutBytes( m_Protocol.SEND_SID_AUTH_ACCOUNTLOGONPROOF(m_BNCSUtil.GetM1()));
							}
						} else {
							LOG.warn("[" + m_Server + "] logon failed - invalid username, disconnecting" );
							m_Socket.Disconnect();
							return;
						}
	
						break;
	
					case SID_AUTH_ACCOUNTLOGONPROOF:
						if (m_Protocol.RECEIVE_SID_AUTH_ACCOUNTLOGONPROOF(Packet.GetData())) {
							// logon successful
	
							LOG.info("[" + m_Server + "] logon successful" );
							m_LoggedIn = true;
							jGhost.EventBNETLoggedIn(this);
							m_Socket.PutBytes( m_Protocol.SEND_SID_NETGAMEPORT(jGhost.getHostPort()));
							m_Socket.PutBytes( m_Protocol.SEND_SID_ENTERCHAT( ) );
							m_Socket.PutBytes( m_Protocol.SEND_SID_FRIENDSLIST( ) );
							m_Socket.PutBytes( m_Protocol.SEND_SID_CLANMEMBERLIST( ) );
						} else {
							LOG.warn("[" + m_Server + "] logon failed - invalid password, disconnecting" );
	
							// try to figure out if the user might be using the wrong logon type since too many people are confused by this
	
							String Server = m_Server.toLowerCase();
	
							if( m_PasswordHashType == "pvpgn" && ( Server == "useast.battle.net" || Server == "uswest.battle.net" || Server == "asia.battle.net" || Server == "europe.battle.net" ) )
								LOG.warn("[" + m_Server + "] it looks like you're trying to connect to a battle.net server using a pvpgn logon type, check your config file's \"battle.net custom data\" section" );
							else if( m_PasswordHashType != "pvpgn" && ( Server != "useast.battle.net" && Server != "uswest.battle.net" && Server != "asia.battle.net" && Server != "europe.battle.net" ) )
								LOG.warn("[" + m_Server + "] it looks like you're trying to connect to a pvpgn server using a battle.net logon type, check your config file's \"battle.net custom data\" section" );
	
							m_Socket.Disconnect();
							return;
						}
	
						break;
	
					case SID_WARDEN:
						WardenData = m_Protocol.RECEIVE_SID_WARDEN(Packet.GetData());
	
						if (m_BNLSClient != null) {
							m_BNLSClient.QueueWardenRaw( WardenData );
						} else {
							LOG.warn( "[" + m_Server + "] received warden packet but no BNLS server is available, you will be kicked from battle.net soon" );
						}
						
						break;
	
					case SID_FRIENDSLIST:
						m_Friends = m_Protocol.RECEIVE_SID_FRIENDSLIST(Packet.GetData());
						break;
	
					case SID_CLANMEMBERLIST:
						m_Clans = m_Protocol.RECEIVE_SID_CLANMEMBERLIST(Packet.GetData());
						break;
					}
				} else {
					LOG.warn("Received invalid package: " + Packet.GetData());
				}
			}
		}
	}
	
	public void ProcessChatEvent (IncomingChatEvent chatEvent) {
		//TODO
		/*CBNETProtocol :: IncomingChatEvent Event = chatEvent->GetChatEvent( );
		bool Whisper = ( Event == CBNETProtocol :: EID_WHISPER );
		string User = chatEvent->GetUser( );
		string Message = chatEvent->GetMessage( );

		if( Event == CBNETProtocol :: EID_WHISPER || Event == CBNETProtocol :: EID_TALK )
		{
			if( Event == CBNETProtocol :: EID_WHISPER )
				CONSOLE_Print( "[WHISPER: " + m_Server + "] [" + User + "] " + Message );
			else
				CONSOLE_Print( "[LOCAL: " + m_Server + "] [" + User + "] " + Message );

			// handle spoof checking for current game
			// this case covers whispers - we assume that anyone who sends a whisper to the bot with message "spoofcheck" should be considered spoof checked
			// note that this means you can whisper "spoofcheck" even in a public game to manually spoofcheck if the /whois fails

			if( Event == CBNETProtocol :: EID_WHISPER && m_GHost->m_CurrentGame )
			{
				if( Message == "spoofcheck" )
					m_GHost->m_CurrentGame->AddToSpoofed( m_Server, User, true );
				else if( Message.find( "entered a Warcraft III The Frozen Throne game called" ) != string :: npos && Message.find( m_GHost->m_CurrentGame->GetGameName( ) ) != string :: npos )
					m_GHost->m_CurrentGame->AddToSpoofed( m_Server, User, false );
			}

			// handle bot commands

			if( !Message.empty( ) && Message[0] == m_CommandTrigger )
			{
				// extract the command trigger, the command, and the payload
				// e.g. "!say hello world" -> command: "say", payload: "hello world"

				string Command;
				string Payload;
				string :: size_type PayloadStart = Message.find( " " );

				if( PayloadStart != string :: npos )
				{
					Command = Message.substr( 1, PayloadStart - 1 );
					Payload = Message.substr( PayloadStart + 1 );
				}
				else
					Command = Message.substr( 1 );

				transform( Command.begin( ), Command.end( ), Command.begin( ), (int(*)(int))tolower );

				if( IsAdmin( User ) || IsRootAdmin( User ) )
				{
					CONSOLE_Print( "[BNET: " + m_Server + "] admin [" + User + "] sent command [" + Message + "]" );

					//
					// ADMIN COMMANDS
					//

					//
					// !ADDADMIN
					//

					if( Command == "addadmin" && !Payload.empty( ) )
					{
						if( IsRootAdmin( User ) )
						{
							if( IsAdmin( Payload ) )
								QueueChatCommand( m_GHost->m_Language->UserIsAlreadyAnAdmin( m_Server, Payload ), User, Whisper );
							else
								m_PairedAdminAdds.push_back( PairedAdminAdd( Whisper ? User : string( ), m_GHost->m_DB->ThreadedAdminAdd( m_Server, Payload ) ) );
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !ADDBAN
					// !BAN
					//

					if( ( Command == "addban" || Command == "ban" ) && !Payload.empty( ) )
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

						if( IsBanned( Victim ) )
							QueueChatCommand( m_GHost->m_Language->UserIsAlreadyBanned( m_Server, Victim ), User, Whisper );
						else
							m_PairedBanAdds.push_back( PairedBanAdd( Whisper ? User : string( ), m_GHost->m_DB->ThreadedBanAdd( m_Server, Victim, string( ), string( ), User, Reason ) ) );
					}

					//
					// !ANNOUNCE
					//

					if( Command == "announce" && m_GHost->m_CurrentGame && !m_GHost->m_CurrentGame->GetCountDownStarted( ) )
					{
						if( Payload.empty( ) || Payload == "off" )
						{
							QueueChatCommand( m_GHost->m_Language->AnnounceMessageDisabled( ), User, Whisper );
							m_GHost->m_CurrentGame->SetAnnounce( 0, string( ) );
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
								CONSOLE_Print( "[BNET: " + m_Server + "] bad input #1 to announce command" );
							else
							{
								if( SS.eof( ) )
									CONSOLE_Print( "[BNET: " + m_Server + "] missing input #2 to announce command" );
								else
								{
									getline( SS, Message );
									string :: size_type Start = Message.find_first_not_of( " " );

									if( Start != string :: npos )
										Message = Message.substr( Start );

									QueueChatCommand( m_GHost->m_Language->AnnounceMessageEnabled( ), User, Whisper );
									m_GHost->m_CurrentGame->SetAnnounce( Interval, Message );
								}
							}
						}
					}

					//
					// !AUTOHOST
					//

					if( Command == "autohost" )
					{
						if( IsRootAdmin( User ) )
						{
							if( Payload.empty( ) || Payload == "off" )
							{
								QueueChatCommand( m_GHost->m_Language->AutoHostDisabled( ), User, Whisper );
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
									CONSOLE_Print( "[BNET: " + m_Server + "] bad input #1 to autohost command" );
								else
								{
									SS >> AutoStartPlayers;

									if( SS.fail( ) || AutoStartPlayers == 0 )
										CONSOLE_Print( "[BNET: " + m_Server + "] bad input #2 to autohost command" );
									else
									{
										if( SS.eof( ) )
											CONSOLE_Print( "[BNET: " + m_Server + "] missing input #3 to autohost command" );
										else
										{
											getline( SS, GameName );
											string :: size_type Start = GameName.find_first_not_of( " " );

											if( Start != string :: npos )
												GameName = GameName.substr( Start );

											QueueChatCommand( m_GHost->m_Language->AutoHostEnabled( ), User, Whisper );
											m_GHost->m_AutoHostGameName = GameName;
											m_GHost->m_AutoHostMapCFG = m_GHost->m_Map->GetCFGFile( );
											m_GHost->m_AutoHostOwner = User;
											m_GHost->m_AutoHostServer = m_Server;
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
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !AUTOHOSTMM
					//

					if( Command == "autohostmm" )
					{
						if( IsRootAdmin( User ) )
						{
							if( Payload.empty( ) || Payload == "off" )
							{
								QueueChatCommand( m_GHost->m_Language->AutoHostDisabled( ), User, Whisper );
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
								// extract the maximum games, auto start players, minimum score, maximum score, and the game name
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
									CONSOLE_Print( "[BNET: " + m_Server + "] bad input #1 to autohostmm command" );
								else
								{
									SS >> AutoStartPlayers;

									if( SS.fail( ) || AutoStartPlayers == 0 )
										CONSOLE_Print( "[BNET: " + m_Server + "] bad input #2 to autohostmm command" );
									else
									{
										SS >> MinimumScore;

										if( SS.fail( ) )
											CONSOLE_Print( "[BNET: " + m_Server + "] bad input #3 to autohostmm command" );
										else
										{
											SS >> MaximumScore;

											if( SS.fail( ) )
												CONSOLE_Print( "[BNET: " + m_Server + "] bad input #4 to autohostmm command" );
											else
											{
												if( SS.eof( ) )
													CONSOLE_Print( "[BNET: " + m_Server + "] missing input #5 to autohostmm command" );
												else
												{
													getline( SS, GameName );
													string :: size_type Start = GameName.find_first_not_of( " " );

													if( Start != string :: npos )
														GameName = GameName.substr( Start );

													QueueChatCommand( m_GHost->m_Language->AutoHostEnabled( ), User, Whisper );
													m_GHost->m_AutoHostGameName = GameName;
													m_GHost->m_AutoHostMapCFG = m_GHost->m_Map->GetCFGFile( );
													m_GHost->m_AutoHostOwner = User;
													m_GHost->m_AutoHostServer = m_Server;
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
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !AUTOSTART
					//

					if( Command == "autostart" && m_GHost->m_CurrentGame && !m_GHost->m_CurrentGame->GetCountDownStarted( ) )
					{
						if( Payload.empty( ) || Payload == "off" )
						{
							QueueChatCommand( m_GHost->m_Language->AutoStartDisabled( ), User, Whisper );
							m_GHost->m_CurrentGame->SetAutoStartPlayers( 0 );
						}
						else
						{
							uint32_t AutoStartPlayers = UTIL_ToUInt32( Payload );

							if( AutoStartPlayers != 0 )
							{
								QueueChatCommand( m_GHost->m_Language->AutoStartEnabled( UTIL_ToString( AutoStartPlayers ) ), User, Whisper );
								m_GHost->m_CurrentGame->SetAutoStartPlayers( AutoStartPlayers );
							}
						}
					}

					//
					// !CHANNEL (change channel)
					//

					if( Command == "channel" && !Payload.empty( ) )
						QueueChatCommand( "/join " + Payload );

					//
					// !CHECKADMIN
					//

					if( Command == "checkadmin" && !Payload.empty( ) )
					{
						if( IsRootAdmin( User ) )
						{
							if( IsAdmin( Payload ) )
								QueueChatCommand( m_GHost->m_Language->UserIsAnAdmin( m_Server, Payload ), User, Whisper );
							else
								QueueChatCommand( m_GHost->m_Language->UserIsNotAnAdmin( m_Server, Payload ), User, Whisper );
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !CHECKBAN
					//

					if( Command == "checkban" && !Payload.empty( ) )
					{
						CDBBan *Ban = IsBanned( Payload );

						if( Ban )
							QueueChatCommand( m_GHost->m_Language->UserWasBannedOnByBecause( m_Server, Payload, Ban->GetDate( ), Ban->GetAdmin( ), Ban->GetReason( ) ), User, Whisper );
						else
							QueueChatCommand( m_GHost->m_Language->UserIsNotBanned( m_Server, Payload ), User, Whisper );
					}

					//
					// !CLOSE (close slot)
					//

					if( Command == "close" && !Payload.empty( ) && m_GHost->m_CurrentGame )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
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
									CONSOLE_Print( "[BNET: " + m_Server + "] bad input to close command" );
									break;
								}
								else
									m_GHost->m_CurrentGame->CloseSlot( (unsigned char)( SID - 1 ), true );
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !CLOSEALL
					//

					if( Command == "closeall" && m_GHost->m_CurrentGame )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
							m_GHost->m_CurrentGame->CloseAllSlots( );
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !COUNTADMINS
					//

					if( Command == "countadmins" )
					{
						if( IsRootAdmin( User ) )
							m_PairedAdminCounts.push_back( PairedAdminCount( Whisper ? User : string( ), m_GHost->m_DB->ThreadedAdminCount( m_Server ) ) );
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !COUNTBANS
					//

					if( Command == "countbans" )
						m_PairedBanCounts.push_back( PairedBanCount( Whisper ? User : string( ), m_GHost->m_DB->ThreadedBanCount( m_Server ) ) );

					//
					// !DBSTATUS
					//

					if( Command == "dbstatus" )
						QueueChatCommand( m_GHost->m_DB->GetStatus( ), User, Whisper );

					//
					// !DELADMIN
					//

					if( Command == "deladmin" && !Payload.empty( ) )
					{
						if( IsRootAdmin( User ) )
						{
							if( !IsAdmin( Payload ) )
								QueueChatCommand( m_GHost->m_Language->UserIsNotAnAdmin( m_Server, Payload ), User, Whisper );
							else
								m_PairedAdminRemoves.push_back( PairedAdminRemove( Whisper ? User : string( ), m_GHost->m_DB->ThreadedAdminRemove( m_Server, Payload ) ) );
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !DELBAN
					// !UNBAN
					//

					if( ( Command == "delban" || Command == "unban" ) && !Payload.empty( ) )
						m_PairedBanRemoves.push_back( PairedBanRemove( Whisper ? User : string( ), m_GHost->m_DB->ThreadedBanRemove( Payload ) ) );

					//
					// !DISABLE
					//

					if( Command == "disable" )
					{
						if( IsRootAdmin( User ) )
						{
							QueueChatCommand( m_GHost->m_Language->BotDisabled( ), User, Whisper );
							m_GHost->m_Enabled = false;
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !ENABLE
					//

					if( Command == "enable" )
					{
						if( IsRootAdmin( User ) )
						{
							QueueChatCommand( m_GHost->m_Language->BotEnabled( ), User, Whisper );
							m_GHost->m_Enabled = true;
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
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
							QueueChatCommand( m_GHost->m_Language->EndingGame( m_GHost->m_Games[GameNumber]->GetDescription( ) ), User, Whisper );
							CONSOLE_Print( "[GAME: " + m_GHost->m_Games[GameNumber]->GetGameName( ) + "] is over (admin ended game)" );
							m_GHost->m_Games[GameNumber]->StopPlayers( "was disconnected (admin ended game)" );
						}
						else
							QueueChatCommand( m_GHost->m_Language->GameNumberDoesntExist( Payload ), User, Whisper );
					}

					//
					// !EXIT
					// !QUIT
					//

					if( Command == "exit" || Command == "quit" )
					{
						if( IsRootAdmin( User ) )
						{
							if( Payload == "force" )
								m_Exiting = true;
							else
							{
								if( m_GHost->m_CurrentGame || !m_GHost->m_Games.empty( ) )
									QueueChatCommand( m_GHost->m_Language->AtLeastOneGameActiveUseForceToShutdown( ), User, Whisper );
								else
									m_Exiting = true;
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !GETCLAN
					//

					if( Command == "getclan" )
					{
						SendGetClanList( );
						QueueChatCommand( m_GHost->m_Language->UpdatingClanList( ), User, Whisper );
					}

					//
					// !GETFRIENDS
					//

					if( Command == "getfriends" )
					{
						SendGetFriendsList( );
						QueueChatCommand( m_GHost->m_Language->UpdatingFriendsList( ), User, Whisper );
					}

					//
					// !GETGAME
					//

					if( Command == "getgame" && !Payload.empty( ) )
					{
						uint32_t GameNumber = UTIL_ToUInt32( Payload ) - 1;

						if( GameNumber < m_GHost->m_Games.size( ) )
							QueueChatCommand( m_GHost->m_Language->GameNumberIs( Payload, m_GHost->m_Games[GameNumber]->GetDescription( ) ), User, Whisper );
						else
							QueueChatCommand( m_GHost->m_Language->GameNumberDoesntExist( Payload ), User, Whisper );
					}

					//
					// !GETGAMES
					//

					if( Command == "getgames" )
					{
						if( m_GHost->m_CurrentGame )
							QueueChatCommand( m_GHost->m_Language->GameIsInTheLobby( m_GHost->m_CurrentGame->GetDescription( ), UTIL_ToString( m_GHost->m_Games.size( ) ), UTIL_ToString( m_GHost->m_MaxGames ) ), User, Whisper );
						else
							QueueChatCommand( m_GHost->m_Language->ThereIsNoGameInTheLobby( UTIL_ToString( m_GHost->m_Games.size( ) ), UTIL_ToString( m_GHost->m_MaxGames ) ), User, Whisper );
					}

					//
					// !HOLD (hold a slot for someone)
					//

					if( Command == "hold" && !Payload.empty( ) && m_GHost->m_CurrentGame )
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
								CONSOLE_Print( "[BNET: " + m_Server + "] bad input to hold command" );
								break;
							}
							else
							{
								QueueChatCommand( m_GHost->m_Language->AddedPlayerToTheHoldList( HoldName ), User, Whisper );
								m_GHost->m_CurrentGame->AddToReserved( HoldName );
							}
						}
					}

					//
					// !HOSTSG
					//

					if( Command == "hostsg" && !Payload.empty( ) )
						m_GHost->CreateGame( GAME_PRIVATE, true, Payload, User, User, m_Server, Whisper );

					//
					// !LOAD (load config file)
					// !MAP
					//

					if( Command == "load" || Command == "map" )
					{
						if( Payload.empty( ) )
							QueueChatCommand( m_GHost->m_Language->CurrentlyLoadedMapCFGIs( m_GHost->m_Map->GetCFGFile( ) ), User, Whisper );
						else
						{
							// only load files in the current directory just to be safe

							if( Payload.find( "/" ) != string :: npos || Payload.find( "\\" ) != string :: npos )
								QueueChatCommand( m_GHost->m_Language->UnableToLoadConfigFilesOutside( ), User, Whisper );
							else
							{
								string File = m_GHost->m_MapCFGPath + Payload + ".cfg";

								if( UTIL_FileExists( File ) )
								{
									// we have to be careful here because we didn't copy the map data when creating the game (there's only one global copy)
									// therefore if we change the map data while a game is in the lobby everything will get screwed up
									// the easiest solution is to simply reject the command if a game is in the lobby

									if( m_GHost->m_CurrentGame )
										QueueChatCommand( m_GHost->m_Language->UnableToLoadConfigFileGameInLobby( ), User, Whisper );
									else
									{
										QueueChatCommand( m_GHost->m_Language->LoadingConfigFile( File ), User, Whisper );
										CConfig MapCFG;
										MapCFG.Read( File );
										m_GHost->m_Map->Load( &MapCFG, File );
									}
								}
								else
									QueueChatCommand( m_GHost->m_Language->UnableToLoadConfigFileDoesntExist( File ), User, Whisper );
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
							QueueChatCommand( m_GHost->m_Language->UnableToLoadSaveGamesOutside( ), User, Whisper );
						else
						{
							string File = m_GHost->m_SaveGamePath + Payload + ".w3z";
							string FileNoPath = Payload + ".w3z";

							if( UTIL_FileExists( File ) )
							{
								if( m_GHost->m_CurrentGame )
									QueueChatCommand( m_GHost->m_Language->UnableToLoadSaveGameGameInLobby( ), User, Whisper );
								else
								{
									QueueChatCommand( m_GHost->m_Language->LoadingSaveGame( File ), User, Whisper );
									m_GHost->m_SaveGame->Load( File, false );
									m_GHost->m_SaveGame->ParseSaveGame( );
									m_GHost->m_SaveGame->SetFileName( File );
									m_GHost->m_SaveGame->SetFileNameNoPath( FileNoPath );
								}
							}
							else
								QueueChatCommand( m_GHost->m_Language->UnableToLoadSaveGameDoesntExist( File ), User, Whisper );
						}
					}

					//
					// !OPEN (open slot)
					//

					if( Command == "open" && !Payload.empty( ) && m_GHost->m_CurrentGame )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
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
									CONSOLE_Print( "[BNET: " + m_Server + "] bad input to open command" );
									break;
								}
								else
									m_GHost->m_CurrentGame->OpenSlot( (unsigned char)( SID - 1 ), true );
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !OPENALL
					//

					if( Command == "openall" && m_GHost->m_CurrentGame )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
							m_GHost->m_CurrentGame->OpenAllSlots( );
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !PRIV (host private game)
					//

					if( Command == "priv" && !Payload.empty( ) )
						m_GHost->CreateGame( GAME_PRIVATE, false, Payload, User, User, m_Server, Whisper );

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
							m_GHost->CreateGame( GAME_PRIVATE, false, GameName, Owner, User, m_Server, Whisper );
						}
					}

					//
					// !PUB (host public game)
					//

					if( Command == "pub" && !Payload.empty( ) )
						m_GHost->CreateGame( GAME_PUBLIC, false, Payload, User, User, m_Server, Whisper );

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
							m_GHost->CreateGame( GAME_PUBLIC, false, GameName, Owner, User, m_Server, Whisper );
						}
					}

					//
					// !SAY
					//

					if( Command == "say" && !Payload.empty( ) )
						QueueChatCommand( Payload );

					//
					// !SAYGAME
					//

					if( Command == "saygame" && !Payload.empty( ) )
					{
						if( IsRootAdmin( User ) )
						{
							// extract the game number and the message
							// e.g. "3 hello everyone" -> game number: "3", message: "hello everyone"

							uint32_t GameNumber;
							string Message;
							stringstream SS;
							SS << Payload;
							SS >> GameNumber;

							if( SS.fail( ) )
								CONSOLE_Print( "[BNET: " + m_Server + "] bad input #1 to saygame command" );
							else
							{
								if( SS.eof( ) )
									CONSOLE_Print( "[BNET: " + m_Server + "] missing input #2 to saygame command" );
								else
								{
									getline( SS, Message );
									string :: size_type Start = Message.find_first_not_of( " " );

									if( Start != string :: npos )
										Message = Message.substr( Start );

									if( GameNumber - 1 < m_GHost->m_Games.size( ) )
										m_GHost->m_Games[GameNumber - 1]->SendAllChat( "ADMIN: " + Message );
									else
										QueueChatCommand( m_GHost->m_Language->GameNumberDoesntExist( UTIL_ToString( GameNumber ) ), User, Whisper );
								}
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !SAYGAMES
					//

					if( Command == "saygames" && !Payload.empty( ) )
					{
						if( IsRootAdmin( User ) )
						{
							if( m_GHost->m_CurrentGame )
								m_GHost->m_CurrentGame->SendAllChat( Payload );

							for( vector<CBaseGame *> :: iterator i = m_GHost->m_Games.begin( ); i != m_GHost->m_Games.end( ); i++ )
								(*i)->SendAllChat( "ADMIN: " + Payload );
						}
						else
							QueueChatCommand( m_GHost->m_Language->YouDontHaveAccessToThatCommand( ), User, Whisper );
					}

					//
					// !SP
					//

					if( Command == "sp" && m_GHost->m_CurrentGame && !m_GHost->m_CurrentGame->GetCountDownStarted( ) )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
						{
							m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->ShufflingPlayers( ) );
							m_GHost->m_CurrentGame->ShuffleSlots( );
						}
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !START
					//

					if( Command == "start" && m_GHost->m_CurrentGame && !m_GHost->m_CurrentGame->GetCountDownStarted( ) && m_GHost->m_CurrentGame->GetNumPlayers( ) > 0 )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
						{
							// if the player sent "!start force" skip the checks and start the countdown
							// otherwise check that the game is ready to start

							if( Payload == "force" )
								m_GHost->m_CurrentGame->StartCountDown( true );
							else
								m_GHost->m_CurrentGame->StartCountDown( false );
						}
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !SWAP (swap slots)
					//

					if( Command == "swap" && !Payload.empty( ) && m_GHost->m_CurrentGame )
					{
						if( !m_GHost->m_CurrentGame->GetLocked( ) )
						{
							uint32_t SID1;
							uint32_t SID2;
							stringstream SS;
							SS << Payload;
							SS >> SID1;

							if( SS.fail( ) )
								CONSOLE_Print( "[BNET: " + m_Server + "] bad input #1 to swap command" );
							else
							{
								if( SS.eof( ) )
									CONSOLE_Print( "[BNET: " + m_Server + "] missing input #2 to swap command" );
								else
								{
									SS >> SID2;

									if( SS.fail( ) )
										CONSOLE_Print( "[BNET: " + m_Server + "] bad input #2 to swap command" );
									else
										m_GHost->m_CurrentGame->SwapSlots( (unsigned char)( SID1 - 1 ), (unsigned char)( SID2 - 1 ) );
								}
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->TheGameIsLockedBNET( ), User, Whisper );
					}

					//
					// !UNHOST
					//

					if( Command == "unhost" )
					{
						if( m_GHost->m_CurrentGame )
						{
							if( m_GHost->m_CurrentGame->GetCountDownStarted( ) )
								QueueChatCommand( m_GHost->m_Language->UnableToUnhostGameCountdownStarted( m_GHost->m_CurrentGame->GetDescription( ) ), User, Whisper );
							else
							{
								QueueChatCommand( m_GHost->m_Language->UnhostingGame( m_GHost->m_CurrentGame->GetDescription( ) ), User, Whisper );
								m_GHost->m_CurrentGame->SetExiting( true );
							}
						}
						else
							QueueChatCommand( m_GHost->m_Language->UnableToUnhostGameNoGameInLobby( ), User, Whisper );
					}

					//
					// !WARDENSTATUS
					//

					if( Command == "wardenstatus" )
					{
						if( m_BNLSClient )
							QueueChatCommand( "WARDEN STATUS --- " + UTIL_ToString( m_BNLSClient->GetTotalWardenIn( ) ) + " requests received, " + UTIL_ToString( m_BNLSClient->GetTotalWardenOut( ) ) + " responses sent.", User, Whisper );
						else
							QueueChatCommand( "WARDEN STATUS --- Not connected to BNLS server.", User, Whisper );
					}
				}
				else
					CONSOLE_Print( "[BNET: " + m_Server + "] user [" + User + "] sent command [" + Message + "]" );

				//
				//NON ADMIN COMMANDS *
				//

				// don't respond to non admins if there are more than 3 messages already in the queue
				// this prevents malicious users from filling up the bot's chat queue and crippling the bot
				// in some cases the queue may be full of legitimate messages but we don't really care if the bot ignores one of these commands once in awhile
				// e.g. when several users join a game at the same time and cause multiple /whois messages to be queued at once

				if( IsAdmin( User ) || IsRootAdmin( User ) || m_OutPackets.size( ) <= 3 )
				{
					//
					// !STATS
					//

					if( Command == "stats" )
					{
						string StatsUser = User;

						if( !Payload.empty( ) )
							StatsUser = Payload;

						// check for potential abuse

						if( !StatsUser.empty( ) && StatsUser.size( ) < 16 && StatsUser[0] != '/' )
							m_PairedGPSChecks.push_back( PairedGPSCheck( Whisper ? User : string( ), m_GHost->m_DB->ThreadedGamePlayerSummaryCheck( StatsUser ) ) );
					}

					//
					// !STATSDOTA
					//

					if( Command == "statsdota" )
					{
						string StatsUser = User;

						if( !Payload.empty( ) )
							StatsUser = Payload;

						// check for potential abuse

						if( !StatsUser.empty( ) && StatsUser.size( ) < 16 && StatsUser[0] != '/' )
							m_PairedDPSChecks.push_back( PairedDPSCheck( Whisper ? User : string( ), m_GHost->m_DB->ThreadedDotAPlayerSummaryCheck( StatsUser ) ) );
					}

					//
					// !VERSION
					//

					if( Command == "version" )
					{
						if( IsAdmin( User ) || IsRootAdmin( User ) )
							QueueChatCommand( m_GHost->m_Language->VersionAdmin( m_GHost->m_Version ), User, Whisper );
						else
							QueueChatCommand( m_GHost->m_Language->VersionNotAdmin( m_GHost->m_Version ), User, Whisper );
					}
				}
			}
		}
		else if( Event == CBNETProtocol :: EID_CHANNEL )
		{
			// keep track of current channel so we can rejoin it after hosting a game

			CONSOLE_Print( "[BNET: " + m_Server + "] joined channel [" + Message + "]" );
			m_CurrentChannel = Message;
		}
		else if( Event == CBNETProtocol :: EID_INFO )
		{
			CONSOLE_Print( "[INFO: " + m_Server + "] " + Message );

			// extract the first word which we hope is the username
			// this is not necessarily true though since info messages also include channel MOTD's and such

			string UserName;
			string :: size_type Split = Message.find( " " );

			if( Split != string :: npos )
				UserName = Message.substr( 0, Split );
			else
				UserName = Message.substr( 0 );

			// handle spoof checking for current game
			// this case covers whois results which are used when hosting a public game (we send out a "/whois [player]" for each player)
			// at all times you can still /w the bot with "spoofcheck" to manually spoof check

			if( m_GHost->m_CurrentGame && m_GHost->m_CurrentGame->GetGameState( ) == GAME_PUBLIC )
			{
				// we don't need to check if the player is in the game before spamming the chat because the bot only sends out a /whois on players that join the game
				// however, we DO need to check that we're only connected to one realm because if we're connected to multiple realms we send a /whois on every realm for every player
				// so we're always guaranteed to get negative results with multiple realms - we're only interested in the positive ones

				if( m_GHost->m_BNETs.size( ) == 1 )
				{
					if( Message.find( "is away" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofPossibleIsAway( UserName ) );
					else if( Message.find( "is unavailable" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofPossibleIsUnavailable( UserName ) );
					else if( Message.find( "is refusing messages" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofPossibleIsRefusingMessages( UserName ) );
					else if( Message.find( "is using Warcraft III The Frozen Throne in the channel" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofDetectedIsNotInGame( UserName ) );
					else if( Message.find( "is using Warcraft III The Frozen Throne in channel" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofDetectedIsNotInGame( UserName ) );
					else if( Message.find( "is using Warcraft III The Frozen Throne in a private channel" ) != string :: npos )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofDetectedIsInPrivateChannel( UserName ) );
				}

				if( Message.find( "is using Warcraft III The Frozen Throne in game" ) != string :: npos || Message.find( "is using Warcraft III Frozen Throne and is currently in  game" ) != string :: npos )
				{
					if( Message.find( m_GHost->m_CurrentGame->GetGameName( ) ) != string :: npos )
						m_GHost->m_CurrentGame->AddToSpoofed( m_Server, UserName, false );
					else if( m_GHost->m_BNETs.size( ) == 1 )
						m_GHost->m_CurrentGame->SendAllChat( m_GHost->m_Language->SpoofDetectedIsInAnotherGame( UserName ) );
				}
			}
		}
		else if( Event == CBNETProtocol :: EID_ERROR )
			CONSOLE_Print( "[ERROR: " + m_Server + "] " + Message );*/
	}

	// functions to send packets to battle.net

	public void SendJoinChannel (String channel) {
		if (m_LoggedIn && m_InChat) {
			m_Socket.PutBytes( m_Protocol.SEND_SID_JOINCHANNEL(channel));
		}
	}
	
	public void SendGetFriendsList () {
		if (m_LoggedIn) {
			m_Socket.PutBytes( m_Protocol.SEND_SID_FRIENDSLIST( ) );
		}
	}
	
	public void SendGetClanList () {
		if (m_LoggedIn) {
			m_Socket.PutBytes( m_Protocol.SEND_SID_CLANMEMBERLIST( ) );
		}
	}
	
	public void QueueEnterChat () {
		if (m_LoggedIn) {
			m_OutPackets.add( m_Protocol.SEND_SID_ENTERCHAT( ) );
		}
	}
	
	public void QueueChatCommand (String chatCommand) {
		if( chatCommand.isEmpty()) {
			return;
		}

		if (m_LoggedIn) {
			if( m_PasswordHashType == "pvpgn" && chatCommand.length() > m_MaxMessageLength ) {
				chatCommand = chatCommand.substring( 0, m_MaxMessageLength );
			}

			if( chatCommand.length( ) > 255 ) {
				chatCommand = chatCommand.substring( 0, 255 );
			}

			LOG.info("[QUEUED: " + m_Server + "] " + chatCommand );
			m_OutPackets.add( m_Protocol.SEND_SID_CHATCOMMAND(chatCommand));
		}
	}
	
	public void QueueChatCommand (String chatCommand, String user, boolean whisper) {
		if (chatCommand.isEmpty()) {
			return;
		}

		// if whisper is true send the chat command as a whisper to user, otherwise just queue the chat command

		if (whisper) {
			QueueChatCommand("/w " + user + " " + chatCommand);
		} else {
			QueueChatCommand(chatCommand);
		}
	}
	
	public void QueueGameCreate (char state, String gameName, String hostName, Map map, SaveGame saveGame, int hostCounter) {
		if( m_LoggedIn && map != null)
		{
			if( !m_CurrentChannel.isEmpty( ) ) {
				m_FirstChannel = m_CurrentChannel;
			}

			m_InChat = false;

			// a game creation message is just a game refresh message with upTime = 0

			QueueGameRefresh( state, gameName, hostName, map, saveGame, 0, hostCounter );
		}
	}
	
	public void QueueGameRefresh (char state, String gameName, String hostName, Map map, SaveGame saveGame, int upTime, int hostCounter) {
		if (hostName.isEmpty()) {
			hostName = m_Protocol.GetUniqueName().toCharString();
		}

		if( m_LoggedIn && map != null) {
			Bytearray MapGameType = new Bytearray();

			// construct the correct SID_STARTADVEX3 packet

			if (saveGame != null) {
				MapGameType.addChar((char) 0);
				MapGameType.addChar((char) 10);
				MapGameType.addChar((char) 0);
				MapGameType.addChar((char) 0);
				Bytearray MapWidth = new Bytearray();
				MapWidth.addChar((char) 0);
				MapWidth.addChar((char) 0);
				Bytearray MapHeight = new Bytearray();
				MapHeight.addChar((char) 0);
				MapHeight.addChar((char) 0);
				m_OutPackets.add(m_Protocol.SEND_SID_STARTADVEX3( state, MapGameType, map.GetMapGameFlags( ), MapWidth, MapHeight, gameName, hostName, upTime, "Save\\Multiplayer\\" + saveGame.GetFileNameNoPath( ), saveGame.GetMagicNumber(), map.GetMapSHA1(), hostCounter));
			} else {
				MapGameType.addChar( map.GetMapGameType( ) );
				MapGameType.addChar((char) 32);
				MapGameType.addChar((char) 73);
				MapGameType.addChar((char) 0);
				m_OutPackets.add(m_Protocol.SEND_SID_STARTADVEX3( state, MapGameType, map.GetMapGameFlags( ), map.GetMapWidth( ), map.GetMapHeight( ), gameName, hostName, upTime, map.GetMapPath( ), map.GetMapCRC(), map.GetMapSHA1(), hostCounter));
			}
		}
	}

	// other functions

	public boolean IsAdmin (String name) {
		name = name.toLowerCase();

		for (String admin : m_Admins) {
			if (admin.equals(name)) {
				return true;
			}
		}

		return false;
	}
	
	public boolean IsRootAdmin (String name) {
		// m_RootAdmin was already transformed to lower case in the constructor
		return name.toLowerCase().equals(m_RootAdmin);
	}
	
	public Ban IsBanned (String name) {
		name = name.toLowerCase();

		for (Ban ban : m_Bans) {
			if (ban.getPlayer().getName().equals(name)) {
				return ban;
			}
		}

		return null;
	}
	
	public void AddAdmin(String name) {
		name = name.toLowerCase();

		for (String admin : this.m_Admins) {
			if (admin.equals(name))  {
				return;
			}
		}
		
		m_Admins.add(name);
	}
	
	public void AddBan (String name, String ip, String gamename, String admin, String reason) {
		name = name.toLowerCase();
		
		for (Ban ban : this.m_Bans) {
			if (ban.getPlayer().getName().equals(name)) {
				return;
			}
		}
		
		m_Bans.add(new Ban(m_Server, name, ip, "N/A", gamename, admin, reason));
	}
	
	public void RemoveAdmin (String name) {
		name = name.toLowerCase();
		
		for (String admin : this.m_Admins) {
			if (admin.equals(name)) {
				this.m_Admins.remove(admin);
				break;
			}
		}
	}
	
	public void RemoveBan (String name) {
		name = name.toLowerCase();
		
		for (Ban ban : this.m_Bans) {
			if (ban.getPlayer().getName().equals(name)) {
				this.m_Bans.remove(ban);
				break;
			}
		}
	}
	
	public void HoldFriends (BaseGame game){
		if (game != null) {
			for (IncomingFriendList fl : m_Friends) {
				game.AddToReserved(fl.GetAccount());
			}
		}
	}
	
	public void HoldClan (BaseGame game) {
		if (game != null) {
			for (IncomingClanList cl : m_Clans) {
				game.AddToReserved(cl.getName());
			}
		}
	}


	public void QueueGameUncreate() {
		if (m_LoggedIn) {
			m_OutPackets.add(m_Protocol.SEND_SID_STOPADV());
		}
	}
}

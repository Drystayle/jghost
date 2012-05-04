package ch.drystayle.jghost.i18n;

import java.util.Locale;

import ch.drystayle.jghost.util.StringUtils;

public enum Messages {
	
	//---- Static
	
	ADMIN_INVALID_PASSWORD("adminInvalidPassword"),
	ADMIN_LOGGED_IN("adminLoggedIn"),
	AUTOKICKING_PLAYER_FOR_EXCESSIVE_PING("autokickingPlayerForExcessivePing"),
	BNET_GAME_HOSTING_FAILED("bnetGameHostingFailed"),
	BNET_GAME_HOSTING_SUCCEEDED("bnetGameHostingSucceeded"),
	CONNECTED_TO_BNET("connectedToBnet"),
	CONNECTING_TO_BNET("connectingToBnet"),
	CONNECTING_TO_BNET_TIMED_OUT("connectingToBnetTimedOut"),
	COUNTDOWN_ABORTED("countdownAborted"),
	CREATING_PRIVATE_GAME("creatingPrivateGame"),
	CREATING_PUBLIC_GAME("creatingPublicGame"),
	DESYNC_DETECTED("desyncDetected"),
	DISCONNECTED_FROM_BNET("disconnectedFromBnet"),
	FILE_NOT_OPENED("fileNotOpened"),
	FILE_NOT_FOUND("fileNotFound"),
	FOUND_BNET_CONNECTION("foundBnetConnection"),
	GAME_IS_OVER("gameIsOver"),
	GAME_LOCKED("gameLocked"),
	GAME_REFRESHED("gameRefreshed"),
	GAME_UNLOCKED("gameUnlocked"),
	HAS_LEFT_VOLUNTARILY("hasLeftVoluntarily"),
	HAS_LOST_CONNECTION_CLOSED_BY_REMOTE_HOST("hasLostConnectionClosedByRemoteHost"),
	HAS_LOST_CONNECTION_PLAYER_ERROR("hasLostConnectionPlayerError"),
	HAS_LOST_CONNECTION_SOCKET_ERROR("hasLostConnectionSocketError"),
	HAS_LOST_CONNECTION_TIMED_OUT("hasLostConnectionTimedOut"),
	INIT_GHOST("initGhost"),
	KICKING_PLAYERS_WITH_PINGS_GREATER_THAN("kickingPlayersWithPingsGreaterThan"),
	LAGGED_OUT_DROPPED_BY_VOTE("laggedOutDroppedByVote"),
	LOAD_CONFIG("loadConfig"),
	LOAD_NATIVE_LIB("loadNativeLib"),
	LOGGED_IN_TO_BNET("loggedInToBnet"),
	LONGEST_LOAD_BY_PLAYER("longestLoadByPlayer"),
	MANUALLY_SPOOF_CHECK_BY_WHISPERING("manuallySpoofCheckByWhispering"),
	MULTIPLE_IP_ADDRESS_USAGE_DETECTED("multipleIPAddressUsageDetected"),
	PASSWORD_EMTPY("passwordEmpty"),
	PLAYER_DOWNLOADED_THE_MAP("playerDownloadedTheMap"),
	PLAYER_IS_SAVING_THE_GAME("playerIsSavingTheGame"),
	PLAYER_VOTED_TO_DROP_LAGGERS("PlayerVotedToDropLaggers"),
	PLAYERS_NOT_YET_PINGED("playersNotYetPinged"),
	PLAYERS_NOT_YET_SPOOF_CHECKED("playersNotYetSpoofChecked"),
	PLAYERS_STILL_DOWNLOADING("playersStillDownloading"),
	ROC_CD_KEY_EMPTY("rocCdKeyEmpty"),
	SHORTEST_LOAD_BY_PLAYER("shortestLoadByPlayer"),
	SPOOF_CHECK_ACCEPTED_FOR("spoofCheckAcceptedFor"),
	SPOOF_CHECK_BY_REPLYING("spoofCheckByReplying"),
	SPOOF_CHECK_BY_WHISPERING("spoofCheckByWhispering"),
	TFT_CD_KEY_EMPTY("tftCdKeyEmpty"),
	UNABLE_TO_CREATE_GAME_ANOTHER_GAME_IN_LOBBY("unableToCreateGameAnotherGameInLobby"),
	UNABLE_TO_CREATE_GAME_INVALID_MAP("unableToCreateGameInvalidMap"),
	UNABLE_TO_CREATE_GAME_NAME_TOO_LONG("unableToCreateGameNameTooLong"),
	UNABLE_TO_CREATE_GAME_TRY_ANOTHER_NAME("unableToCreateGameTryAnotherName"),
	UNABLE_TO_KICK_FOUND_MORE_THAN_ONE_MATCH("unableToKickFoundMoreThanOneMatch"),
	UNABLE_TO_KICK_NO_MATCHES_FOUND("unableToKickNoMatchesFound"),
	USER_NAME_EMTPY("userNameEmpty"),
	VOTE_KICK_CANCELLED("voteKickCancelled"),
	VOTE_KICK_EXPIRED("voteKickExpired"),
	WAITING_FOR_PLAYERS_BEFORE_AUTO_START("waitingForPlayersBeforeAutoStart"),
	WAS_KICKED_BY_PLAYER("wasKickedByPlayer"),
	WAS_KICKED_FOR_NOT_SPOOF_CHECKING("wasKickedForNotSpoofChecking"),
	WAS_KICKED_FOR_OWNER_PLAYER("wasKickedForOwnerPlayer"),
	WAS_KICKED_FOR_RESERVED_PLAYER("wasKickedForReservedPlayer"),
	YOUR_LOADING_TIME_WAS("yourLoadingTimeWas"),
	;
	
	//---- State
	
	private String propertyName;
	
	//---- Constructors
	
	private Messages () {
		this.propertyName = StringUtils.enumNameToPropertyName(name());
	}
	
	private Messages (String propertyName) {
		this.propertyName = propertyName;
	}
	
	//---- Methods

	public String createMessage (Object... objects) {
		return createMessageFromArray(objects);
	}
	
	public String createMessageFromArray (Object[] objects) {
		return MessagesUtil.getMessage(propertyName, objects);
	}
	
	public String createMessage (Locale locale, Object... objects) {
		return createMessageFromArray(locale, objects);
	}
	
	public String createMessageFromArray (Locale locale, Object... objects) {
		return MessagesUtil.getMessage(propertyName, locale, objects);
	}
	
	//---- Object
	
	public String toString () {
		return createMessage();
	}
	
}

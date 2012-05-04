package ch.drystayle.jghost.game.stats;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.game.BaseGame;
import ch.drystayle.jghost.game.DotaPlayer;
import ch.drystayle.jghost.game.GamePlayer;
import ch.drystayle.jghost.protocol.IncomingAction;
import ch.drystayle.jghost.util.StringUtils;

public class StatsDota extends StatsImpl {

	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(StatsDota.class);
	
	private static final int MAX_DOTA_PLAYER = 12;
	
	//---- State
	
	private DotaPlayer[] players;
	private int winner;
	private int min;
	private int sec;
	
	//---- Constructors
	
	public StatsDota(BaseGame game) {
		super(game);
		this.players = new DotaPlayer[MAX_DOTA_PLAYER];
		this.winner = 0;
		this.min = 0;
		this.sec = 0;
	}

	//---- Stats
	
	@Override
	public boolean processAction(IncomingAction Action) {
		int i = 0;
		Bytearray ActionData = Action.GetAction();
		Bytearray Data = new Bytearray();
		Bytearray Key = new Bytearray();
		Bytearray Value = new Bytearray();

		// dota actions with real time replay data start with 0x6b then the null terminated string "dr.x"
		// unfortunately more than one action can be sent in a single packet and the length of each action isn't explicitly represented in the packet
		// so we have to either parse all the actions and calculate the length based on the type or we can search for an identifying sequence
		// parsing the actions would be more correct but would be a lot more difficult to write for relatively little gain
		// so we take the easy route (which isn't always guaranteed to work) and search the data for the sequence "6b 64 72 2e 78 00" and hope it identifies an action

		while( ActionData.size( ) >= i + 6 ) {
			if( ActionData.getChar(i) == 0x6b && ActionData.getChar(i + 1) == 0x64 && ActionData.getChar(i + 2) == 0x72 && ActionData.getChar(i + 3) == 0x2e && ActionData.getChar(i + 4) == 0x78 && ActionData.getChar(i + 5) == 0x00 ) {
				// we think we've found an action with real time replay data (but we can't be 100% sure)
				// next we parse out two null terminated strings and a 4 byte integer

				if( ActionData.size( ) >= i + 7 ) {
					// the first null terminated string should either be the strings "Data" or "Global" or a player id in ASCII representation, e.g. "1" or "2"

					Data = ActionData.extractCString(i + 6);

					if( ActionData.size( ) >= i + 8 + Data.size( ) ) {
						// the second null terminated string should be the key

						Key = ActionData.extractCString(i + 7 + Data.size());

						if( ActionData.size( ) >= i + 12 + Data.size( ) + Key.size( ) )
						{
							// the 4 byte integer should be the value

							String DataString = Data.toCharString();
							String KeyString = Key.toCharString();
							int ValueInt = ActionData.extract(i + 8 + Data.size() + Key.size(), 4).toInt();

							LOG.debug( "[STATS] " + DataString + ", " + KeyString + ", " + ValueInt);

							if (DataString.equals("Data")) {
								// these are received during the game
								// you could use these to calculate killing sprees and double or triple kills (you'd have to make up your own time restrictions though)
								// you could also build a table of "who killed who" data

								if (KeyString.length() >= 5 && KeyString.substring(0, 4).equals("Hero")) {
									// a hero died

									String VictimColourString = KeyString.substring(4);
									int VictimColour = StringUtils.toInt32(VictimColourString);
									GamePlayer Killer = this.bGame.GetPlayerFromColour((char) ValueInt);
									GamePlayer Victim = this.bGame.GetPlayerFromColour((char) VictimColour);

									if (Killer != null && Victim != null) {
										LOG.info("[" + this.bGame.GetGameName( ) + "] player [" + Killer.GetName() + "] killed player [" + Victim.GetName() + "]" );
									} else if (Victim != null) {
										if( ValueInt == 0 )
											LOG.info("[" + this.bGame.GetGameName( ) + "] the Sentinel killed player [" + Victim.GetName( ) + "]" );
										else if( ValueInt == 6 )
											LOG.info("[" +this.bGame.GetGameName( ) + "] the Scourge killed player [" + Victim.GetName( ) + "]" );
									}
								} else if (KeyString.length() >= 8 && KeyString.substring( 0, 7 ).equals("Courier")) {
									// a courier died

									if( ( ValueInt >= 1 && ValueInt <= 5 ) || ( ValueInt >= 7 && ValueInt <= 11 ) )
									{
										if (players[ValueInt] == null) {
											this.players[ValueInt] = new DotaPlayer();
										}

										this.players[ValueInt].SetCourierKills(this.players[ValueInt].GetCourierKills() + 1);
									}

									String VictimColourString = KeyString.substring( 7 );
									int VictimColour = StringUtils.toInt32(VictimColourString);
									GamePlayer Killer = this.bGame.GetPlayerFromColour((char) ValueInt);
									GamePlayer Victim = this.bGame.GetPlayerFromColour((char) VictimColour);

									if (Killer != null && Victim != null) {
										LOG.info( "[" + this.bGame.GetGameName( ) + "] player [" + Killer.GetName( ) + "] killed a courier owned by player [" + Victim.GetName() + "]" );
									} else if (Victim != null) {
										if( ValueInt == 0 )
											LOG.info( "[STATSDOTA: " + this.bGame.GetGameName() + "] the Sentinel killed a courier owned by player [" + Victim.GetName() + "]" );
										else if( ValueInt == 6 )
											LOG.info( "[STATSDOTA: " + this.bGame.GetGameName() + "] the Scourge killed a courier owned by player [" + Victim.GetName( ) + "]" );
									}
								} else if( KeyString.length( ) >= 8 && KeyString.substring( 0, 5 ) == "Tower" ) {
									// a tower died

									if( ( ValueInt >= 1 && ValueInt <= 5 ) || ( ValueInt >= 7 && ValueInt <= 11 ) )
									{
										if (this.players[ValueInt] == null)
											this.players[ValueInt] = new DotaPlayer();

										this.players[ValueInt].SetTowerKills(this.players[ValueInt].GetTowerKills() + 1 );
									}

									String Alliance = KeyString.substring( 5, 1 );
									String Level = KeyString.substring( 6, 1 );
									String Side = KeyString.substring( 7, 1 );
									GamePlayer Killer = this.bGame.GetPlayerFromColour((char) ValueInt);
									String AllianceString;
									String SideString;

									if( Alliance == "0" )
										AllianceString = "Sentinel";
									else if( Alliance == "1" )
										AllianceString = "Scourge";
									else
										AllianceString = "unknown";

									if( Side == "0" )
										SideString = "top";
									else if( Side == "1" )
										SideString = "mid";
									else if( Side == "2" )
										SideString = "bottom";
									else
										SideString = "unknown";

									if (Killer != null)
										LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] player [" + Killer.GetName( ) + "] destroyed a level [" + Level + "] " + AllianceString + " tower (" + SideString + ")" );
									else
									{
										if( ValueInt == 0 )
											LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] the Sentinel destroyed a level [" + Level + "] " + AllianceString + " tower (" + SideString + ")" );
										else if( ValueInt == 6 )
											LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] the Scourge destroyed a level [" + Level + "] " + AllianceString + " tower (" + SideString + ")" );
									}
								} else if (KeyString.length() >= 6 && KeyString.substring( 0, 3 ) == "Rax" ) {
									// a rax died

									if( ( ValueInt >= 1 && ValueInt <= 5 ) || ( ValueInt >= 7 && ValueInt <= 11 ) )
									{
										if (this.players[ValueInt] == null)
											this.players[ValueInt] = new DotaPlayer();

										this.players[ValueInt].SetRaxKills(this.players[ValueInt].GetRaxKills() + 1 );
									}

									String Alliance = KeyString.substring( 3, 1 );
									String Side = KeyString.substring( 4, 1 );
									String Type = KeyString.substring( 5, 1 );
									GamePlayer Killer = this.bGame.GetPlayerFromColour((char) ValueInt);
									String AllianceString;
									String SideString;
									String TypeString;

									if( Alliance == "0" )
										AllianceString = "Sentinel";
									else if( Alliance == "1" )
										AllianceString = "Scourge";
									else
										AllianceString = "unknown";

									if( Side == "0" )
										SideString = "top";
									else if( Side == "1" )
										SideString = "mid";
									else if( Side == "2" )
										SideString = "bottom";
									else
										SideString = "unknown";

									if( Type == "0" )
										TypeString = "melee";
									else if( Type == "1" )
										TypeString = "ranged";
									else
										TypeString = "unknown";

									if (Killer != null)
										LOG.info( "[" + this.bGame.GetGameName( ) + "] player [" + Killer.GetName( ) + "] destroyed a " + TypeString + " " + AllianceString + " rax (" + SideString + ")" );
									else
									{
										if( ValueInt == 0 )
											LOG.info( "[" + this.bGame.GetGameName() + "] the Sentinel destroyed a " + TypeString + " " + AllianceString + " rax (" + SideString + ")" );
										else if( ValueInt == 6 )
											LOG.info( "[" + this.bGame.GetGameName() + "] the Scourge destroyed a " + TypeString + " " + AllianceString + " rax (" + SideString + ")" );
									}
								}
								else if( KeyString.length() >= 6 && KeyString.substring( 0, 6 ) == "Throne" )
								{
									// the frozen throne got hurt

									LOG.info( "[STATSDOTA: " + this.bGame.GetGameName() + "] the Frozen Throne is now at " + ValueInt + "% HP" );
								}
								else if( KeyString.length() >= 4 && KeyString.substring( 0, 4 ) == "Tree" )
								{
									// the world tree got hurt

									LOG.info( "[STATSDOTA: " + this.bGame.GetGameName() + "] the World Tree is now at " + ValueInt + "% HP" );
								}
								else if( KeyString.length() >= 2 && KeyString.substring( 0, 2 ) == "CK" )
								{
									// a player disconnected
								}
							}
							else if( DataString == "Global" )
							{
								// these are only received at the end of the game

								if( KeyString == "Winner" )
								{
									// Value 1 -> sentinel
									// Value 2 -> scourge

									winner = ValueInt;

									if( winner == 1 )
										LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] detected winner: Sentinel" );
									else if( winner == 2 )
										LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] detected winner: Scourge" );
									else
										LOG.info( "[STATSDOTA: " + this.bGame.GetGameName( ) + "] detected winner: " + ValueInt);
								}
								else if( KeyString == "m" )
									min = ValueInt;
								else if( KeyString == "s" )
									sec = ValueInt;
							} else if( DataString.length() <= 2 && StringUtils.findFirstNotOf(DataString, "1234567890") != -1) {
								// these are only received at the end of the game
								int ID = StringUtils.toInt32(DataString);

								if( ( ID >= 1 && ID <= 5 ) || ( ID >= 7 && ID <= 11 ) )
								{
									if (this.players[ID] == null)
									{
										this.players[ID] = new DotaPlayer();
										this.players[ID].SetColour(ID);
									}

									// Key "1"		-> Kills
									// Key "2"		-> Deaths
									// Key "3"		-> Creep Kills
									// Key "4"		-> Creep Denies
									// Key "5"		-> Assists
									// Key "6"		-> Current Gold
									// Key "7"		-> Neutral Kills
									// Key "8_0"	-> Item 1
									// Key "8_1"	-> Item 2
									// Key "8_2"	-> Item 3
									// Key "8_3"	-> Item 4
									// Key "8_4"	-> Item 5
									// Key "8_5"	-> Item 6
									// Key "id"		-> ID (1-5 for sentinel, 6-10 for scourge, accurate after using -sp and/or -switch)

									if( KeyString == "1" )
										this.players[ID].SetKills( ValueInt );
									else if( KeyString == "2" )
										this.players[ID].SetDeaths( ValueInt );
									else if( KeyString == "3" )
										this.players[ID].SetCreepKills( ValueInt );
									else if( KeyString == "4" )
										this.players[ID].SetCreepDenies( ValueInt );
									else if( KeyString == "5" )
										this.players[ID].SetAssists( ValueInt );
									else if( KeyString == "6" )
										this.players[ID].SetGold( ValueInt );
									else if( KeyString == "7" )
										this.players[ID].SetNeutralKills( ValueInt );
//									else if( KeyString == "8_0" )
//										this.players[ID].SetItem( 0, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "8_1" )
//										this.players[ID].SetItem( 1, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "8_2" )
//										this.players[ID].SetItem( 2, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "8_3" )
//										this.players[ID].SetItem( 3, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "8_4" )
//										this.players[ID].SetItem( 4, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "8_5" )
//										this.players[ID].SetItem( 5, string( Value.rbegin( ), Value.rend( ) ) );
//									else if( KeyString == "9" )
//										this.players[ID].SetHero( string( Value.rbegin( ), Value.rend( ) ) );
									else if( KeyString == "id" )
									{
										// DotA sends id values from 1-10 with 1-5 being sentinel players and 6-10 being scourge players
										// unfortunately the actual player colours are from 1-5 and from 7-11 so we need to deal with this case here

										if( ValueInt >= 6 )
											this.players[ID].SetNewColour( ValueInt + 1 );
										else
											this.players[ID].SetNewColour( ValueInt );
									}
								}
							}

							i += 12 + Data.size( ) + Key.size( );
						} else {
							i++;
						}
					} else {
						i++;
					}
				} else {
					i++;
				}
			} else {
				i++;
			}
		}

		return this.winner != 0;
	}

	@Override
	public boolean save(JGhost ghost) {
		/*
		if( DB->Begin( ) )
	{
		// since we only record the end game information it's possible we haven't recorded anything yet if the game didn't end with a tree/throne death
		// this will happen if all the players leave before properly finishing the game
		// the dotagame stats are always saved (with winner = 0 if the game didn't properly finish)
		// the dotaplayer stats are only saved if the game is properly finished

		unsigned int Players = 0;

		// save the dotagame

		GHost->m_Callables.push_back( DB->ThreadedDotAGameAdd( GameID, m_Winner, m_Min, m_Sec ) );

		// check for invalid colours and duplicates
		// this can only happen if DotA sends us garbage in the "id" value but we should check anyway

		for( unsigned int i = 0; i < 12; i++ )
		{
			if( m_Players[i] )
			{
				uint32_t Colour = m_Players[i]->GetNewColour( );

				if( !( ( Colour >= 1 && Colour <= 5 ) || ( Colour >= 7 && Colour <= 11 ) ) )
				{
					CONSOLE_Print( "[STATSDOTA: " + m_Game->GetGameName( ) + "] discarding player data, invalid colour found" );
					DB->Commit( );
					return;
				}

				for( unsigned int j = i + 1; j < 12; j++ )
				{
					if( m_Players[j] && Colour == m_Players[j]->GetNewColour( ) )
					{
						CONSOLE_Print( "[STATSDOTA: " + m_Game->GetGameName( ) + "] discarding player data, duplicate colour found" );
						DB->Commit( );
						return;
					}
				}
			}
		}

		// save the dotaplayers

		for( unsigned int i = 0; i < 12; i++ )
		{
			if( m_Players[i] )
			{
				GHost->m_Callables.push_back( DB->ThreadedDotAPlayerAdd( GameID, m_Players[i]->GetColour( ), m_Players[i]->GetKills( ), m_Players[i]->GetDeaths( ), m_Players[i]->GetCreepKills( ), m_Players[i]->GetCreepDenies( ), m_Players[i]->GetAssists( ), m_Players[i]->GetGold( ), m_Players[i]->GetNeutralKills( ), m_Players[i]->GetItem( 0 ), m_Players[i]->GetItem( 1 ), m_Players[i]->GetItem( 2 ), m_Players[i]->GetItem( 3 ), m_Players[i]->GetItem( 4 ), m_Players[i]->GetItem( 5 ), m_Players[i]->GetHero( ), m_Players[i]->GetNewColour( ), m_Players[i]->GetTowerKills( ), m_Players[i]->GetRaxKills( ), m_Players[i]->GetCourierKills( ) ) );
				Players++;
			}
		}

		if( DB->Commit( ) )
			CONSOLE_Print( "[STATSDOTA: " + m_Game->GetGameName( ) + "] saving " + UTIL_ToString( Players ) + " players" );
		else
			CONSOLE_Print( "[STATSDOTA: " + m_Game->GetGameName( ) + "] unable to commit database transaction, data not saved" );
	}
	else
		CONSOLE_Print( "[STATSDOTA: " + m_Game->GetGameName( ) + "] unable to begin database transaction, data not saved" );
	*/
		return false;
	}

}

package ch.drystayle.jghost.util;

import java.util.Comparator;

import ch.drystayle.jghost.game.GamePlayer;

public class PingComparator implements Comparator<GamePlayer> {

	//---- State
	
	private boolean lcPings;
	
	//---- Constructors
	
	public PingComparator (boolean lcPings) {
		this.lcPings = lcPings;
	}
	
	//---- Methods
	
	@Override
	public int compare(GamePlayer p1, GamePlayer p2) {
		int p1Ping = p1.GetPing(this.lcPings);
		int p2Ping = p2.GetPing(this.lcPings);
		if (p2Ping > p1Ping) {
			return 1;
		} else if (p2Ping < p1Ping) {
			return -1;
		} else {
			return 0;
		}
	}

}

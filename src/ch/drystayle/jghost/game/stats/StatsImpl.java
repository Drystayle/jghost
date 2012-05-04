package ch.drystayle.jghost.game.stats;

import ch.drystayle.jghost.game.BaseGame;

public abstract class StatsImpl implements Stats {

	//---- Methods
	
	protected BaseGame bGame;
	
	//---- Constructors
	
	public StatsImpl (BaseGame game) {
		this.bGame = game;
	}

}

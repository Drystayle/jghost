package ch.drystayle.jghost.game.stats;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.game.BaseGame;
import ch.drystayle.jghost.protocol.IncomingAction;

public class StatsW3MMD extends StatsImpl {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(StatsW3MMD.class);
	
	//---- State
	
	//---- Constructors
	
	public StatsW3MMD(BaseGame game, String string) {
		super(game);
		// TODO Auto-generated constructor stub
	}

	//---- Methods
	
	


	//---- Stats
	
	@Override
	public boolean processAction(IncomingAction Action) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean save(JGhost ghost) {
		// TODO Auto-generated method stub
		return false;
	}

}

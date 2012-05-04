package ch.drystayle.jghost.game.stats;

import ch.drystayle.jghost.JGhost;
import ch.drystayle.jghost.protocol.IncomingAction;

public interface Stats {

	boolean processAction (IncomingAction Action);
	
	boolean save (JGhost ghost);
	
}

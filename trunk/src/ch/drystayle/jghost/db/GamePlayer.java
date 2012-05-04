package ch.drystayle.jghost.db;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class GamePlayer extends BaseEntity<Integer> {

	//---- Static
	
	private static final long serialVersionUID = 3419775711921436933L;
	
	//---- State
	
	@ManyToOne(fetch=FetchType.LAZY)
	private Player player;
	@ManyToOne(fetch=FetchType.LAZY)
	private Game game;
	
	//---- Methods
	
	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Game getGame() {
		return game;
	}
	
}

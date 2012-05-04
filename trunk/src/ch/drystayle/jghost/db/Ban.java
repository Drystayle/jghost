package ch.drystayle.jghost.db;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class Ban extends BaseEntity<Integer> {

	//---- Static
	
	private static final long serialVersionUID = 3419775711921436933L;
	
	//---- State
	
	@ManyToOne(fetch=FetchType.LAZY)
	private Player player;
	/** IP stored as 127.0.0.1 */
	private String ip;
	private Date date;
	@ManyToOne(fetch=FetchType.LAZY)
	private Game game;
	private String reason;
	@ManyToOne(fetch=FetchType.LAZY)
	private Player by;
	
	//---- Methods
	
	public Ban(String server, String name, String ip, String string,
			String gamename, String admin, String reason) {
		// TODO Auto-generated constructor stub
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Game getGame() {
		return game;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}

	public void setBy(Player by) {
		this.by = by;
	}

	public Player getBy() {
		return by;
	}
	
	
}

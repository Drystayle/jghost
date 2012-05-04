package ch.drystayle.jghost.db;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class Game extends BaseEntity<Integer> {

	//---- Static
	
	private static final long serialVersionUID = 3419775711921436933L;
	
	//---- State
	
	private String name;
	@ManyToOne(fetch=FetchType.LAZY)
	private Server server;
	private String map;
	@ManyToOne(fetch=FetchType.LAZY)
	private Player owner;
	private Date startTime;
	/** Duration in milliseconds */
	private int duration;

	//---- Methods

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Server getServer() {
		return server;
	}

	public void setMap(String map) {
		this.map = map;
	}

	public String getMap() {
		return map;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public Player getOwner() {
		return owner;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getDuration() {
		return duration;
	}
}

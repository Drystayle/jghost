package ch.drystayle.jghost.db;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class Player extends BaseEntity<Integer> {

	//---- Static
	
	private static final long serialVersionUID = 3419775711921436933L;

	//---- State
	
	private String name;
	@ManyToOne(fetch=FetchType.LAZY)
	private Server server;
	private boolean admin;
	
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

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isAdmin() {
		return admin;
	}
	
}

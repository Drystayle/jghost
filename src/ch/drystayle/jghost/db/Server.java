package ch.drystayle.jghost.db;

import javax.persistence.Entity;

@Entity
public class Server extends BaseEntity<Integer> {

	//---- Static
	
	private static final long serialVersionUID = 3419775711921436933L;

	//---- State
	
	private String name;
	
	//---- Methods

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
}

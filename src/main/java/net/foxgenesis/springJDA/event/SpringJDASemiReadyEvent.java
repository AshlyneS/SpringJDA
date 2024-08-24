package net.foxgenesis.springJDA.event;

import net.foxgenesis.springJDA.SpringJDA;

public class SpringJDASemiReadyEvent extends SpringJDAEvent {

	private static final long serialVersionUID = -807079749489761643L;

	public SpringJDASemiReadyEvent(SpringJDA source) {
		super(source);
	}

}

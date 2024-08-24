package net.foxgenesis.springJDA.event;

import org.springframework.context.ApplicationEvent;

import net.foxgenesis.springJDA.SpringJDA;

public abstract class SpringJDAEvent extends ApplicationEvent {

	private static final long serialVersionUID = -7635731586255541730L;

	public SpringJDAEvent(SpringJDA source) {
		super(source);
	}

	public SpringJDA getSource() {
		return (SpringJDA) super.getSource();
	}
}

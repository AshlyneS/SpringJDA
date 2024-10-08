package net.foxgenesis.springJDA.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring-jda")
public record SpringJDAConfiguration(Boolean useSharding, Boolean annotationConfiguration, boolean updateCommands, Boolean eventAutoRegister) {

	public SpringJDAConfiguration {
		if(useSharding == null)
			useSharding = true;
		if (annotationConfiguration == null)
			annotationConfiguration = true;
		if(eventAutoRegister == null)
			eventAutoRegister = true;
	}
}

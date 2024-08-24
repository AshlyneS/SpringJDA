package net.foxgenesis.springJDA.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring-jda")
public record SpringJDAConfiguration(Boolean useSharding, Boolean annotationConfiguration, Boolean updateCommands) {

	public SpringJDAConfiguration {
		if(useSharding == null)
			useSharding = true;
		if (annotationConfiguration == null)
			annotationConfiguration = true;
		if (updateCommands == null)
			updateCommands = false;
	}
}

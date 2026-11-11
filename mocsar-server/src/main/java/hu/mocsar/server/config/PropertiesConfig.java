package hu.mocsar.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MocsarProperties.class)
public class PropertiesConfig {
}

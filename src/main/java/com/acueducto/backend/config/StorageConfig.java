package com.acueducto.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageConfig {
    private String basePath;
    private String configPath;
    private String publicacionesPath;
    private String facturasPath;
    private String recibosPath;
}

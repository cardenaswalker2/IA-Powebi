package com.nexainsights.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfig {

    @Autowired
    private MappingMongoConverter mappingMongoConverter;

    /**
     * Reemplaza puntos en claves de mapas automáticamente (ej. URLs o correos como claves en frecuencias)
     * para evitar el error de MongoDB: "Map key contains dots but no replacement was configured"
     */
    @PostConstruct
    public void configureMongoConverter() {
        mappingMongoConverter.setMapKeyDotReplacement("_dot_");
    }
}

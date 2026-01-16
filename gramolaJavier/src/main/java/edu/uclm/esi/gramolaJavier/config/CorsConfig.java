package edu.uclm.esi.gramolaJavier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // ✅ PERMITIR CREDENCIALES
        config.setAllowCredentials(true);
        
        // ✅ PERMITIR AMBAS URLS (localhost y 127.0.0.1)
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
        ));
        
        // ✅ PERMITIR TODOS LOS HEADERS
        config.addAllowedHeader("*");
        
        // ✅ PERMITIR TODOS LOS MÉTODOS HTTP
        config.addAllowedMethod("*");
        
        // ✅ EXPONER HEADERS NECESARIOS
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // ✅ TIEMPO DE CACHE PARA PREFLIGHT
        config.setMaxAge(3600L);
        
        // ✅ APLICAR A TODOS LOS ENDPOINTS
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
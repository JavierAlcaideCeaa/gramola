package edu.uclm.esi.gramolaJavier.models;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpotifyConfig {
    
    @Value("${spotify.redirect.uri}")
    private String redirectUri;
    
    public String getRedirectUri() {
        return redirectUri;
    }
}
package edu.uclm.esi.gramolaJavier.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import edu.uclm.esi.gramolaJavier.Dao.SpotifyTokenDao;
import edu.uclm.esi.gramolaJavier.Dao.userDao;
import edu.uclm.esi.gramolaJavier.models.SpotifyToken;
import edu.uclm.esi.gramolaJavier.models.User;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class   SpotifyService {

    @Autowired
    private SpotifyTokenDao spotifyTokenDao;

    @Autowired
    private userDao userDao;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    /**
     * PRINCIPAL: Obtiene tokens de Spotify usando el código de autorización
     */
    public Map<String, Object> getAuthorizationToken(String code, String email) {
        System.out.println("═══════════════════════════════════");
        System.out.println("INTERCAMBIANDO CÓDIGO POR TOKENS");
        System.out.println("═══════════════════════════════════");
        System.out.println("Email: " + email);
        System.out.println("Code (primeros 20): " + code.substring(0, Math.min(20, code.length())) + "...");

        try {
            // 1️⃣ Buscar usuario
            User user = userDao.findById(email)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                ));

            String clientId = user.getClientId();
            String clientSecret = user.getClientSecret();

            System.out.println("Client ID: " + clientId);
            System.out.println("Client Secret (primeros 10): " + clientSecret.substring(0, Math.min(10, clientSecret.length())) + "...");
            System.out.println("Redirect URI: " + redirectUri);

            // 2️⃣ Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);

            System.out.println("Authorization Header: Basic " + encodedCredentials.substring(0, 20) + "...");

            // 3️⃣ Preparar body
            String body = String.format(
                "grant_type=authorization_code&code=%s&redirect_uri=%s",
                code,
                redirectUri
            );

            System.out.println("📦 Request Body:");
            System.out.println("   grant_type: authorization_code");
            System.out.println("   code: " + code.substring(0, 20) + "...");
            System.out.println("   redirect_uri: " + redirectUri);

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // 4️⃣ Hacer petición a Spotify
            System.out.println("Enviando petición a Spotify...");
            System.out.println("URL: https://accounts.spotify.com/api/token");
            
            ResponseEntity<String> response = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                request,
                String.class
            );

            System.out.println("📥 Respuesta de Spotify:");
            System.out.println("   Status Code: " + response.getStatusCode());
            System.out.println("   Body (primeros 100 chars): " + 
                response.getBody().substring(0, Math.min(100, response.getBody().length())) + "...");

            // 5️⃣ Parsear respuesta
            JSONObject json = new JSONObject(response.getBody());
            
            String accessToken = json.getString("access_token");
            String refreshToken = json.getString("refresh_token");
            int expiresIn = json.getInt("expires_in");

            System.out.println("Tokens obtenidos exitosamente");
            System.out.println("   Access Token (primeros 20): " + accessToken.substring(0, 20) + "...");
            System.out.println("   Refresh Token (primeros 20): " + refreshToken.substring(0, 20) + "...");
            System.out.println("   Expira en: " + expiresIn + " segundos");

            // 6️⃣ Calcular fecha de expiración
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

            // 7️⃣ Guardar o actualizar tokens en BD
            Optional<SpotifyToken> existingToken = spotifyTokenDao.findByEmail(email);
            
            SpotifyToken tokenEntity;
            if (existingToken.isPresent()) {
                System.out.println("🔄 Actualizando tokens existentes para: " + email);
                tokenEntity = existingToken.get();
                tokenEntity.setAccessToken(accessToken);
                tokenEntity.setRefreshToken(refreshToken);
                tokenEntity.setExpiresAt(expiresAt);
            } else {
                System.out.println("➕ Creando nuevos tokens para: " + email);
                tokenEntity = new SpotifyToken(email, accessToken, refreshToken, expiresAt);
            }

            spotifyTokenDao.save(tokenEntity);
            System.out.println("💾 Tokens guardados en BD");

            // 8️⃣ Retornar datos
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("refresh_token", refreshToken);
            result.put("expires_in", expiresIn);
            result.put("token_type", "Bearer");

            System.out.println("═══════════════════════════════════");
            System.out.println("PROCESO COMPLETADO EXITOSAMENTE");
            System.out.println("═══════════════════════════════════");

            return result;

        } catch (ResponseStatusException e) {
            System.err.println("ResponseStatusException: " + e.getReason());
            throw e;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("═══════════════════════════════════");
            System.err.println("ERROR DE SPOTIFY (HTTP 4XX)");
            System.err.println("═══════════════════════════════════");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("═══════════════════════════════════");
            
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Spotify rechazó la petición: " + e.getResponseBodyAsString()
            );
            
        } catch (Exception e) {
            System.err.println("═══════════════════════════════════");
            System.err.println("ERROR INESPERADO");
            System.err.println("═══════════════════════════════════");
            System.err.println("Tipo: " + e.getClass().getName());
            System.err.println("Mensaje: " + e.getMessage());
            e.printStackTrace();
            System.err.println("═══════════════════════════════════");
            
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al obtener tokens de Spotify: " + e.getMessage()
            );
        }
    }

    /**
     * Obtiene el access token de un usuario (desde BD)
     */
    public String getAccessToken(String email) {
        SpotifyToken token = spotifyTokenDao.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No hay tokens de Spotify para este usuario"
            ));
        
        // Verificar si el token ha expirado
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            System.out.println("⚠️ Token expirado, refrescando...");
            return refreshAccessToken(email);
        }
        
        return token.getAccessToken();
    }

    /**
     * Refresca un access token usando el refresh token
     */
    public String refreshAccessToken(String email) {
        System.out.println("🔄 Refrescando access token para: " + email);
        
        try {
            // Obtener usuario y tokens
            User user = userDao.findById(email)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                ));

            SpotifyToken tokenEntity = spotifyTokenDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No hay tokens de Spotify para este usuario"
                ));

            String clientId = user.getClientId();
            String clientSecret = user.getClientSecret();
            String refreshToken = tokenEntity.getRefreshToken();

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);

            // Preparar body
            String body = String.format(
                "grant_type=refresh_token&refresh_token=%s",
                refreshToken
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            // Hacer petición
            ResponseEntity<String> response = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                request,
                String.class
            );

            // Parsear respuesta
            JSONObject json = new JSONObject(response.getBody());
            
            String newAccessToken = json.getString("access_token");
            int expiresIn = json.getInt("expires_in");
            
            // Actualizar en BD
            tokenEntity.setAccessToken(newAccessToken);
            tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            spotifyTokenDao.save(tokenEntity);

            System.out.println("✅ Token refrescado exitosamente");
            
            return newAccessToken;

        } catch (Exception e) {
            System.err.println("❌ Error al refrescar token: " + e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al refrescar token: " + e.getMessage()
            );
        }
    }

    /**
     * ✅ NUEVO: Añade una canción a la cola de Spotify
     */
    public void addToQueue(String accessToken, String trackUri, String deviceId) {
        try {
            System.out.println("═══════════════════════════════════");
            System.out.println("➕ AÑADIENDO CANCIÓN A LA COLA");
            System.out.println("═══════════════════════════════════");
            System.out.println("🎵 Track URI: " + trackUri);
            System.out.println("📱 Device ID: " + deviceId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            
            String url = String.format(
                "https://api.spotify.com/v1/me/player/queue?uri=%s&device_id=%s",
                trackUri,
                deviceId
            );
            
            System.out.println("🔗 URL: " + url);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Canción añadida a la cola exitosamente");
            } else {
                System.err.println("❌ Error: " + response.getStatusCode());
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comunicarse con Spotify"
                );
            }
            
            System.out.println("═══════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("❌ Excepción: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al añadir canción a la cola: " + e.getMessage()
            );
        }
    }
}
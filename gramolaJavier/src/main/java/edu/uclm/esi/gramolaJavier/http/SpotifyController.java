package edu.uclm.esi.gramolaJavier.http;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.uclm.esi.gramolaJavier.services.SpotifyService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("spotify")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class SpotifyController {
    
    @Autowired
    private SpotifyService spotifyService;
    
    @GetMapping("/getAuthorizationToken")
    public ResponseEntity<Map<String, Object>> getAuthorizationToken(
            @RequestParam String code,
            @RequestParam String email) {
        
        System.out.println("═══════════════════════════════════");
        System.out.println("🔐 GET /spotify/getAuthorizationToken");
        System.out.println("═══════════════════════════════════");
        System.out.println("📝 Code: " + code.substring(0, Math.min(20, code.length())) + "...");
        System.out.println("📧 Email: " + email);
        System.out.println("⏰ Timestamp: " + System.currentTimeMillis());
        System.out.println("═══════════════════════════════════");
        
        try {
            Map<String, Object> tokenData = this.spotifyService.getAuthorizationToken(code, email);
            
            System.out.println("✅ Token obtenido exitosamente");
            System.out.println("🔑 Access Token presente: " + (tokenData.containsKey("access_token") ? "SÍ" : "NO"));
            System.out.println("═══════════════════════════════════");
            
            return ResponseEntity.ok(tokenData);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al obtener token:");
            System.err.println("   Mensaje: " + e.getMessage());
            System.err.println("   Tipo: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("═══════════════════════════════════");
            
            throw e;
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "No hay sesión activa"
            ));
        }
        
        String newAccessToken = this.spotifyService.refreshAccessToken(email);
        
        return ResponseEntity.ok(Map.of(
            "access_token", newAccessToken,
            "message", "Token refrescado exitosamente"
        ));
    }
    
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getToken(HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "No hay sesión activa"
            ));
        }
        
        String accessToken = this.spotifyService.getAccessToken(email);
        
        return ResponseEntity.ok(Map.of(
            "access_token", accessToken
        ));
    }
}
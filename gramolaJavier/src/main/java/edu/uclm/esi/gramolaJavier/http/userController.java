package edu.uclm.esi.gramolaJavier.http;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.gramolaJavier.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("user")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class userController {
    
    @Autowired
    private UserService service;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String barName = body.get("barName");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");
        String address = body.get("address"); // NUEVO: Dirección postal
        String signature = body.get("signature"); // NUEVO: Firma digital
        
        // Extraer coordenadas GPS (pueden ser null si no se proporcionan)
        Double latitude = null;
        Double longitude = null;
        
        try {
            if (body.get("latitude") != null) {
                latitude = Double.parseDouble(body.get("latitude"));
            }
            if (body.get("longitude") != null) {
                longitude = Double.parseDouble(body.get("longitude"));
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Coordenadas GPS inválidas");
        }
        
        if (email == null || pwd1 == null || pwd2 == null || 
            barName == null || clientId == null || clientSecret == null) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "Faltan datos obligatorios");
        }
        
        this.service.register(email, pwd1, pwd2, barName, clientId, clientSecret, address, latitude, longitude, signature);
    }
    
    /**
     * ✅ ACTUALIZADO: Devuelve JSON con email, barName y clientId
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> body, 
            HttpSession session) {
        
        String email = body.get("email");
        String pwd = body.get("pwd");
        
        if (email == null || pwd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Email y contraseña requeridos");
        }
        
        // El login ahora devuelve Map con datos del usuario
        Map<String, String> userData = this.service.login(email, pwd);
        
        // Guardar email en sesión para uso posterior
        session.setAttribute("email", email);  // Cambiar "userEmail" a "email"
        session.setAttribute("userEmail", email);  // Mantener por compatibilidad
        
        System.out.println("🔐 Sesión creada para: " + email);
        
        return ResponseEntity.ok(userData);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }
    
    @GetMapping("/confirmToken/{email}")
    public void confirmToken(
            @PathVariable String email, 
            @RequestParam String token, 
            HttpServletResponse response) throws IOException {
        
        try {
            this.service.confirmToken(email, token);
            
            // ✅ CAMBIAR A 127.0.0.1
            String redirectUrl = "http://127.0.0.1:4200/confirm?email=" + 
                            email + "&token=" + token;
            
            System.out.println("✅ Cuenta confirmada: " + email);
            System.out.println("🔀 Redirigiendo a: " + redirectUrl);
            
            response.sendRedirect(redirectUrl);
            
        } catch (ResponseStatusException e) {
            // ✅ CAMBIAR A 127.0.0.1
            String errorUrl = "http://127.0.0.1:4200/register?error=" + 
                            e.getReason().replace(" ", "+");
            response.sendRedirect(errorUrl);
        }
    }

    @PostMapping("/verifyPassword")
    public ResponseEntity<String> verifyPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        
        System.out.println("🔐 Verificando contraseña para: " + email);
        
        try {
            boolean isValid = this.service.verifyPassword(email, password);
            if (isValid) {
                System.out.println("✅ Contraseña correcta");
                return ResponseEntity.ok("Password válido");
            } else {
                System.out.println("❌ Contraseña incorrecta");
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Contraseña incorrecta"
                );
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error al verificar contraseña: " + e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al verificar contraseña"
            );
        }
    }

    @PostMapping("/requestPasswordReset")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        
        System.out.println("📧 Solicitud de recuperación de contraseña para: " + email);
        
        try {
            this.service.requestPasswordReset(email);
            return ResponseEntity.ok("Email de recuperación enviado");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error al solicitar recuperación: " + e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al enviar email de recuperación"
            );
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        
        System.out.println("🔑 Restableciendo contraseña para: " + email);
        
        try {
            this.service.resetPassword(email, token, newPassword);
            return ResponseEntity.ok("Contraseña restablecida exitosamente");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error al restablecer contraseña: " + e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al restablecer contraseña"
            );
        }
    }

    
}
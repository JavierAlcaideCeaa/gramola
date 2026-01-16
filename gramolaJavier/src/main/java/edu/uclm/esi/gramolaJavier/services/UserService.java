package edu.uclm.esi.gramolaJavier.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import edu.uclm.esi.gramolaJavier.Dao.userDao;
import edu.uclm.esi.gramolaJavier.models.User;
import edu.uclm.esi.gramolaJavier.models.Token;

@Service
public class UserService {
    
    @Autowired
    private userDao userDao;
    
    @Autowired
    private EmailService emailService;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * Registra un nuevo usuario (bar) en el sistema
     */
    public void register(String email, String pwd1, String pwd2, String barName, String clientId, String clientSecret) {
        // ============================================
        // VALIDACIONES DE ENTRADA
        // ============================================
        
        // Validar que las contraseñas coinciden
        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "Las contraseñas no coinciden");
        }
        
        // Validación de email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "Email inválido");
        }
        
        // Validación de contraseña
        if (pwd1.length() < 6) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "La contraseña debe tener al menos 6 caracteres");
        }
        
        // Validación de nombre de bar
        if (barName == null || barName.trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "El nombre del bar debe tener al menos 3 caracteres");
        }
        
        // Validación de credenciales de Spotify
        if (clientId == null || clientId.trim().isEmpty() || 
            clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                "Las credenciales de Spotify son obligatorias");
        }
        
        // ============================================
        // MANEJO DE ESCENARIOS ALTERNATIVOS
        // ============================================
        
        Optional<User> existingUser = this.userDao.findById(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // ESCENARIO 1: Usuario existe, confirmado Y pagado → ERROR 409
            if (user.isAccountConfirmed() && user.isPaymentConfirmed()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "El bar ya está registrado y activo. Por favor, inicia sesión.");
            }
            
            // ESCENARIO 2: Usuario existe pero NO confirmó → BORRAR y crear nuevo
            if (!user.isAccountConfirmed()) {
                System.out.println("⚠️ Usuario " + email + " no confirmó su cuenta anterior.");
                System.out.println("🗑️ Borrando cuenta antigua y creando nueva...");
                this.userDao.delete(user);
            }
            
            // ESCENARIO 3: Usuario existe, confirmado pero NO pagó → BORRAR y crear nuevo
            else if (user.isAccountConfirmed() && !user.isPaymentConfirmed()) {
                System.out.println("⚠️ Usuario " + email + " confirmó pero no pagó.");
                System.out.println("🗑️ Borrando cuenta antigua y creando nueva...");
                this.userDao.delete(user);
            }
        }
        
        // ============================================
        // CREAR NUEVO USUARIO
        // ============================================
        
        // Crear token de confirmación
        Token token = new Token();
        
        // Crear usuario
        User newUser = new User(email, pwd1, barName, clientId, clientSecret, token);
        this.userDao.save(newUser);
        
        // ============================================
        // ENVIAR CORREO DE CONFIRMACIÓN
        // ============================================
        
        String confirmationUrl = "http://localhost:8080/user/confirmToken/" + email + "?token=" + token.getId();
        
        // Enviar correo electrónico real
        this.emailService.sendConfirmationEmail(email, barName, confirmationUrl);
        
        System.out.println("✅ Usuario registrado correctamente: " + email);
        System.out.println("📧 Correo de confirmación enviado a: " + email);
    }
    
    /**
     * Autentica un usuario y devuelve sus datos básicos + clientId
     * @return Map con email, barName y clientId
     */
    public Map<String, String> login(String email, String password) {
        // Buscar usuario
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        
        // Verificar contraseña
        String encryptedInputPassword = user.encryptPassword(password);
        
        if (!user.getPassword().equals(encryptedInputPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "Contraseña incorrecta");
        }
        
        // Verificar que la cuenta esté confirmada
        if (!user.isAccountConfirmed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Debe confirmar su cuenta. Revise su correo electrónico.");
        }
        
        // Verificar que el pago esté confirmado
        if (!user.isPaymentConfirmed()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, 
                "Debe completar el pago para acceder a la gramola");
        }
        
        System.out.println("✅ Login exitoso: " + email);
        System.out.println("🏪 Bar: " + user.getBarName());
        System.out.println("🎵 ClientId: " + user.getClientId());
        
        // Crear respuesta con los datos necesarios para el frontend
        Map<String, String> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("barName", user.getBarName());
        response.put("clientId", user.getClientId());
        
        return response;
    }
    
    /**
     * Confirma el token de activación de cuenta
     */
    public void confirmToken(String email, String tokenValue) {
        // Buscar usuario
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        Token token = user.getToken();
        
        // Validar que existe token
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "No hay token asociado a esta cuenta");
        }
        
        // Validar que el token es correcto
        if (!token.getId().equals(tokenValue)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "Token incorrecto");
        }
        
        // Validar que el token no ha sido usado
        if (token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, 
                "El token ya ha sido usado");
        }
        
        // Validar que el token no ha expirado (30 minutos)
        long tokenAge = System.currentTimeMillis() - token.getCreationTime();
        long THIRTY_MINUTES = 30 * 60 * 1000;
        
        if (tokenAge > THIRTY_MINUTES) {
            throw new ResponseStatusException(HttpStatus.GONE, 
                "El token ha expirado. Por favor, regístrese nuevamente.");
        }
        
        // ============================================
        // CONFIRMAR CUENTA
        // ============================================
        
        // Marcar token como usado
        token.Use();
        
        // Marcar cuenta como confirmada
        user.setAccountConfirmed(true);
        
        // Guardar cambios
        this.userDao.save(user);
        
        System.out.println("✅ Cuenta confirmada: " + email);
        System.out.println("🔀 Redirigiendo a página de pago...");
    }
    
    /**
     * Elimina un usuario del sistema
     */
    public void delete(String email) {
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        this.userDao.delete(user);
        
        System.out.println("🗑️ Usuario eliminado: " + email);
    }
}
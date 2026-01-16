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
    
    @Autowired
    private GeocodingService geocodingService;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * Registra un nuevo usuario (bar) en el sistema
     * @param latitude Latitud GPS (si se proporciona directamente)
     * @param longitude Longitud GPS (si se proporciona directamente)
     */
    public void register(String email, String pwd1, String pwd2, String barName, String clientId, String clientSecret, String address, Double latitude, Double longitude) {
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
        
        // Obtener coordenadas:
        // 1. Si vienen directamente (latitude/longitude) -> usar esas
        // 2. Si viene dirección -> hacer geocoding
        
        if (latitude != null && longitude != null) {
            // Coordenadas GPS proporcionadas directamente
            System.out.println("📍 Coordenadas GPS recibidas: " + latitude + ", " + longitude);
        } else if (address != null && !address.trim().isEmpty()) {
            // Hacer geocoding de la dirección
            try {
                double[] coords = geocodingService.getCoordinates(address);
                latitude = coords[0];
                longitude = coords[1];
                System.out.println("📍 Coordenadas obtenidas por geocoding: " + latitude + ", " + longitude);
            } catch (Exception e) {
                System.err.println("⚠️ No se pudieron obtener coordenadas para: " + address);
                // No lanzamos error, simplemente no guardamos coordenadas
            }
        }
        
        // Crear usuario con dirección y coordenadas
        User newUser;
        if (address != null && !address.trim().isEmpty()) {
            newUser = new User(email, pwd1, barName, clientId, clientSecret, token, address, latitude, longitude);
        } else {
            newUser = new User(email, pwd1, barName, clientId, clientSecret, token);
        }
        
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
        
        // Incluir coordenadas GPS si están disponibles
        if (user.getLatitude() != null && user.getLongitude() != null) {
            response.put("latitude", String.valueOf(user.getLatitude()));
            response.put("longitude", String.valueOf(user.getLongitude()));
            System.out.println("📍 Coordenadas: " + user.getLatitude() + ", " + user.getLongitude());
        }
        
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
    
    /**
     * Busca un usuario por su email
     * @param email El email del usuario
     * @return El usuario si existe, null si no existe
     */
    public User findByEmail(String email) {
        Optional<User> userOpt = this.userDao.findById(email);
        return userOpt.orElse(null);
    }

    /**
     * Verifica si la contraseña proporcionada es correcta para el usuario
     * @return true si la contraseña es correcta, false en caso contrario
     */
    public boolean verifyPassword(String email, String password) {
        // Buscar usuario
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        
        // Encriptar la contraseña proporcionada
        String encryptedInputPassword = user.encryptPassword(password);
        
        // Comparar con la contraseña almacenada
        return user.getPassword().equals(encryptedInputPassword);
    }

    /**
     * Solicita restablecimiento de contraseña (envía email con token)
     */
    public void requestPasswordReset(String email) {
        // Buscar usuario
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        
        // Crear token de recuperación
        Token resetToken = new Token();
        user.setToken(resetToken);
        this.userDao.save(user);
        
        // Construir enlace de recuperación
        String resetLink = "http://127.0.0.1:4200/reset-password?email=" + 
                          email + "&token=" + resetToken.getId();
        
        // Enviar email de recuperación
        this.emailService.sendPasswordResetEmail(email, user.getBarName(), resetLink);
        
        System.out.println("📧 Email de recuperación enviado a: " + email);
    }

    /**
     * Restablece la contraseña usando el token
     */
    public void resetPassword(String email, String tokenId, String newPassword) {
        // Buscar usuario
        Optional<User> userOpt = this.userDao.findById(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "El usuario no existe");
        }
        
        User user = userOpt.get();
        Token token = user.getToken();
        
        // Verificar que el token existe y coincide
        if (token == null || !token.getId().equals(tokenId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Token inválido");
        }
        
        // Verificar que el token no ha sido usado
        if (token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Token ya utilizado");
        }
        
        // Verificar que el token no ha expirado (24 horas)
        long tokenAge = System.currentTimeMillis() - token.getCreationTime();
        long maxAge = 24 * 60 * 60 * 1000; // 24 horas en milisegundos
        
        if (tokenAge > maxAge) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Token expirado. Solicita un nuevo enlace de recuperación.");
        }
        
        // Actualizar contraseña
        String encryptedPassword = user.encryptPassword(newPassword);
        user.setPassword(encryptedPassword);
        
        // Marcar token como usado
        token.Use();
        
        this.userDao.save(user);
        
        System.out.println("✅ Contraseña restablecida para: " + email);
    }
}
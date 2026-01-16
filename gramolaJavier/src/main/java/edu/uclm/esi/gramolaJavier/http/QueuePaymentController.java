package edu.uclm.esi.gramolaJavier.http;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import edu.uclm.esi.gramolaJavier.dto.QueuePrepayRequest;
import edu.uclm.esi.gramolaJavier.models.QueuePaymentTransaction;
import edu.uclm.esi.gramolaJavier.services.QueuePaymentService;
import edu.uclm.esi.gramolaJavier.services.LocationService;
import edu.uclm.esi.gramolaJavier.services.UserService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("queue")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class QueuePaymentController {
    
    @Autowired
    private QueuePaymentService queuePaymentService;
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Prepara el pago para encolar una canción
     */
    @PostMapping("/prepay")
    public ResponseEntity<String> prepay(
            @RequestBody QueuePrepayRequest request, 
            HttpSession session) {
        
        System.out.println("═══════════════════════════════════");
        System.out.println("📥 POST /queue/prepay");
        System.out.println("═══════════════════════════════════");
        
        try {
            // VALIDAR UBICACIÓN DEL USUARIO
            String barEmail = (String) session.getAttribute("email");
            if (barEmail == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "No hay sesión activa");
            }
            
            // Obtener coordenadas del bar
            var bar = userService.findByEmail(barEmail);
            if (bar == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Bar no encontrado");
            }
            
            // Validar que el bar tenga coordenadas configuradas
            if (bar.getLatitude() == null || bar.getLongitude() == null) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, 
                    "El bar no tiene ubicación configurada. Por favor, actualiza tu perfil con la dirección del bar.");
            }
            
            // Validar que el usuario envíe su ubicación
            if (request.getUserLatitude() == null || request.getUserLongitude() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Debes permitir el acceso a tu ubicación para usar la gramola");
            }
            
            // Verificar si el usuario está dentro del rango permitido (100 metros)
            boolean withinRange = locationService.isWithinRange(
                request.getUserLatitude(), 
                request.getUserLongitude(),
                bar.getLatitude(),
                bar.getLongitude()
            );
            
            if (!withinRange) {
                double distance = locationService.calculateDistance(
                    request.getUserLatitude(), 
                    request.getUserLongitude(),
                    bar.getLatitude(),
                    bar.getLongitude()
                );
                
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    String.format(
                        "Debes estar en el bar para usar la gramola. " +
                        "Distancia actual: %.0f metros (máximo permitido: 100 metros)",
                        distance
                    ));
            }
            
            System.out.println("✅ Usuario dentro del rango permitido");
            
            String clientSecret = queuePaymentService.prepay(request, session);
            return ResponseEntity.ok(clientSecret);
            
        } catch (ResponseStatusException e) {
            System.err.println("❌ ResponseStatusException: " + e.getReason());
            throw e;
            
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error inesperado: " + e.getMessage()
            );
        }
    }
    
    /**
     * Confirma el pago y encola la canción
     */
    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(
            HttpSession session,
            @RequestParam(required = false) String deviceId) {
        
        System.out.println("═══════════════════════════════════");
        System.out.println("📥 GET /queue/confirm");
        System.out.println("═══════════════════════════════════");
        
        try {
            queuePaymentService.confirm(session, deviceId);
            return ResponseEntity.ok("Canción encolada exitosamente");
            
        } catch (ResponseStatusException e) {
            System.err.println("❌ ResponseStatusException: " + e.getReason());
            throw e;
            
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error inesperado: " + e.getMessage()
            );
        }
    }
    
    /**
     * Obtiene el historial de canciones pagadas por un usuario
     */
    @GetMapping("/history/{email}")
    public ResponseEntity<List<QueuePaymentTransaction>> getHistory(@PathVariable String email) {
        try {
            List<QueuePaymentTransaction> transactions = queuePaymentService.getHistoryByEmail(email);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al obtener historial: " + e.getMessage()
            );
        }
    }
    
    /**
     * Obtiene todas las transacciones completadas (para el propietario)
     */
    @GetMapping("/all")
    public ResponseEntity<List<QueuePaymentTransaction>> getAllCompleted() {
        try {
            List<QueuePaymentTransaction> transactions = queuePaymentService.getAllCompleted();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al obtener transacciones: " + e.getMessage()
            );
        }
    }
}
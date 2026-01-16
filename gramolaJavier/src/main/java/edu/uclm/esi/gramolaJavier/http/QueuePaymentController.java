package edu.uclm.esi.gramolaJavier.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import edu.uclm.esi.gramolaJavier.dto.QueuePrepayRequest;
import edu.uclm.esi.gramolaJavier.services.QueuePaymentService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("queue")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class QueuePaymentController {
    
    @Autowired
    private QueuePaymentService queuePaymentService;
    
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
}
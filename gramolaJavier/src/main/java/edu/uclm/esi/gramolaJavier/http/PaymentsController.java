package edu.uclm.esi.gramolaJavier.http;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.stripe.exception.StripeException;
import edu.uclm.esi.gramolaJavier.models.StripeTransaction;
import edu.uclm.esi.gramolaJavier.services.PaymentService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class PaymentsController {
    
    @Autowired
    private PaymentService service;
    
    /**
     * Prepara el pago creando un PaymentIntent en Stripe
     */
    @GetMapping("/prepay")
    public StripeTransaction prepay(HttpSession session) {
        try {
            StripeTransaction transactionDetails = this.service.prepay();
            session.setAttribute("transactionDetails", transactionDetails);
            return transactionDetails;
        } catch (StripeException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Error al preparar el pago: " + e.getMessage()
            );
        }
    }
    
    /**
     * Confirma el pago después de que Stripe lo procese
     */
    @PostMapping("/confirm")
    public void confirm(@RequestBody Map<String, Object> body) {
        try {
            // Extraer datos del body
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentIntent = (Map<String, Object>) body.get("paymentIntent");
            String paymentIntentId = (String) paymentIntent.get("id");
            
            Long transactionId = Long.valueOf(body.get("transactionId").toString());
            String token = (String) body.get("token");
            String email = (String) body.get("email");
            
            // Validar datos
            if (email == null || token == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "Email y token son requeridos"
                );
            }
            
            // Confirmar pago y activar cuenta
            this.service.confirm(paymentIntentId, transactionId, token, email);
            
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "ID de transacción inválido"
            );
        }
    }
}
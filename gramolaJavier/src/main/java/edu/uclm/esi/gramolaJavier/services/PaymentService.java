package edu.uclm.esi.gramolaJavier.services;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import edu.uclm.esi.gramolaJavier.Dao.StripeTransactionDao;
import edu.uclm.esi.gramolaJavier.Dao.userDao;
import edu.uclm.esi.gramolaJavier.models.StripeTransaction;
import edu.uclm.esi.gramolaJavier.models.User;

@Service
public class PaymentService {
    
    // ⚠️ CAMBIA ESTO POR TU SECRET KEY DE STRIPE
    static {
        Stripe.apiKey = "sk_test_51RJHbY00mVaZlVqd0NSRizvf088qWsP0Fh2yZyR3hnQ3kDTeSTX45vgaFC74bmENx21rPF7FzVpOgxDMZ1A89QcF00wcCu4W4Q";
    }
    
    @Autowired
    private StripeTransactionDao stripeTransactionDao;
    
    @Autowired
    private userDao userDao;
    
    /**
     * Prepara el pago creando un PaymentIntent en Stripe
     */
    public StripeTransaction prepay() throws StripeException {
        // Crear PaymentIntent en Stripe
        PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
            .setCurrency("eur")
            .setAmount(2999L) // 29.99€ en céntimos
            .build();
        
        PaymentIntent intent = PaymentIntent.create(createParams);
        
        // Convertir respuesta de Stripe a JSON
        JSONObject transactionDetails = new JSONObject(intent.toJson());
        
        // Guardar transacción en base de datos
        StripeTransaction st = new StripeTransaction();
        st.setData(transactionDetails);
        this.stripeTransactionDao.save(st);
        
        System.out.println("💳 PaymentIntent creado: " + intent.getId());
        
        return st;
    }
    
    /**
     * Confirma el pago y activa la cuenta del usuario
     */
    public void confirm(String paymentIntentId, Long transactionId, String token, String email) {
        // Buscar usuario por email
        User user = this.userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Usuario no encontrado"
            ));
        
        // Verificar que la cuenta está confirmada
        if (!user.isAccountConfirmed()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Debe confirmar su cuenta primero"
            );
        }
        
        // Verificar que el token coincide
        if (user.getToken() == null || !user.getToken().getId().equals(token)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Token inválido"
            );
        }
        
        // Marcar pago como confirmado
        user.setPaymentConfirmed(true);
        this.userDao.save(user);
        
        System.out.println("✅ Pago confirmado para: " + email);
        System.out.println("🎉 Cuenta completamente activada");
    }
}
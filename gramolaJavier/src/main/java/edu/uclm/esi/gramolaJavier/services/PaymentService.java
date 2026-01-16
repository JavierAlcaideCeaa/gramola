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
import edu.uclm.esi.gramolaJavier.Dao.PriceDao;
import edu.uclm.esi.gramolaJavier.models.StripeTransaction;
import edu.uclm.esi.gramolaJavier.models.User;
import edu.uclm.esi.gramolaJavier.models.Price;
import java.util.List;

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
    
    @Autowired
    private PriceDao priceDao;
    
    /**
     * Obtiene todos los precios disponibles
     */
    public List<Price> getAllPrices() {
        return this.priceDao.findAll();
    }
    
    /**
     * Prepara el pago creando un PaymentIntent en Stripe
     * @param subscriptionType "monthly" o "annual"
     */
    public StripeTransaction prepay(String subscriptionType) throws StripeException {
        // Obtener precio desde BD según tipo de suscripción
        Long priceInCents;
        if ("annual".equalsIgnoreCase(subscriptionType)) {
            priceInCents = 29900L; // 299€ anual (valor por defecto)
        } else {
            priceInCents = 2999L; // 29.99€ mensual (valor por defecto)
        }
        
        // Intentar obtener precio de BD
        Price price = this.priceDao.findById(priceInCents).orElse(null);
        if (price != null) {
            priceInCents = price.getPriceCents();
            System.out.println("💰 Precio desde BD: " + price.getEuros() + "€ - " + price.getDescription());
        } else {
            System.out.println("⚠️ Precio no encontrado en BD, usando valor por defecto");
        }
        
        // Crear PaymentIntent en Stripe
        PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
            .setCurrency("eur")
            .setAmount(priceInCents)
            .build();
        
        PaymentIntent intent = PaymentIntent.create(createParams);
        
        // Convertir respuesta de Stripe a JSON
        JSONObject transactionDetails = new JSONObject(intent.toJson());
        
        // Guardar transacción en base de datos
        StripeTransaction st = new StripeTransaction();
        st.setData(transactionDetails);
        this.stripeTransactionDao.save(st);
        
        System.out.println("💳 PaymentIntent creado: " + intent.getId());
        System.out.println("💵 Monto: " + (priceInCents / 100.0) + "€ (" + subscriptionType + ")");
        
        return st;
    }
    
    /**
     * Confirma el pago y activa la cuenta del usuario
     */
    public void confirm(String paymentIntentId, Long transactionId, String token, String email, String subscriptionType) {
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
        
        // Calcular fecha de expiración
        long now = System.currentTimeMillis();
        long expirationDate;
        
        if ("annual".equalsIgnoreCase(subscriptionType)) {
            // 365 días
            expirationDate = now + (365L * 24 * 60 * 60 * 1000);
        } else {
            // 30 días (mensual por defecto)
            expirationDate = now + (30L * 24 * 60 * 60 * 1000);
        }
        
        // Marcar pago como confirmado y guardar suscripción
        user.setPaymentConfirmed(true);
        user.setSubscriptionType(subscriptionType);
        user.setSubscriptionExpirationDate(expirationDate);
        this.userDao.save(user);
        
        System.out.println("✅ Pago confirmado para: " + email);
        System.out.println("📅 Suscripción: " + subscriptionType);
        System.out.println("🕒 Expira: " + new java.util.Date(expirationDate));
        System.out.println("🎉 Cuenta completamente activada");
    }
}
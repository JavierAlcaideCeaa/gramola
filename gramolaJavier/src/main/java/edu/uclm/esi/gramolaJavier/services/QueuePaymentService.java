package edu.uclm.esi.gramolaJavier.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import edu.uclm.esi.gramolaJavier.Dao.PriceDao;
import edu.uclm.esi.gramolaJavier.Dao.QueuePaymentTransactionDao;
import edu.uclm.esi.gramolaJavier.dto.QueuePrepayRequest;
import edu.uclm.esi.gramolaJavier.models.Price;
import edu.uclm.esi.gramolaJavier.models.QueuePaymentTransaction;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueuePaymentService {
    
    static {
        Stripe.apiKey = "sk_test_51RJHbY00mVaZlVqd0NSRizvf088qWsP0Fh2yZyR3hnQ3kDTeSTX45vgaFC74bmENx21rPF7FzVpOgxDMZ1A89QcF00wcCu4W4Q";
    }
    
    @Autowired
    private PriceDao priceDao;
    
    @Autowired
    private QueuePaymentTransactionDao queuePaymentTransactionDao;
    
    @Autowired
    private SpotifyService spotifyService;
    
    /**
     * Prepara el pago creando un PaymentIntent en Stripe
     */
    public String prepay(QueuePrepayRequest request, HttpSession session) {
        System.out.println("═══════════════════════════════════");
        System.out.println("💳 PREPARANDO PAGO DE ENCOLAMIENTO");
        System.out.println("═══════════════════════════════════");
        System.out.println("� Nombre cliente: " + request.getCustomerName());
        System.out.println("💰 Price Code: " + request.getPriceCode());
        System.out.println("🎵 Track: " + request.getTrackName());
        System.out.println("🎤 Artista: " + request.getArtistName());
        System.out.println("📱 Device ID: " + request.getDeviceId());
        
        // Validar precio
        Price price = priceDao.findById(request.getPriceCode())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Precio inválido: " + request.getPriceCode()
            ));
        
        System.out.println("✅ Precio válido: " + price.getEuros() + "€");
        
        // Obtener email del bar desde sesión
        String barEmail = (String) session.getAttribute("email");
        if (barEmail == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No hay sesión activa. Debe iniciar sesión primero."
            );
        }
        
        try {
            // Crear PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(price.getPriceCents())
                .setCurrency(price.getCurrency())
                .setDescription("Encolamiento: " + request.getTrackName() + " - " + request.getCustomerName())
                .putMetadata("customerName", request.getCustomerName())
                .putMetadata("barEmail", barEmail)
                .putMetadata("trackUri", request.getTrackUri())
                .putMetadata("deviceId", request.getDeviceId())
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            System.out.println("✅ PaymentIntent creado: " + paymentIntent.getId());
            
            // Guardar transacción con información completa
            QueuePaymentTransaction transaction = new QueuePaymentTransaction(
                barEmail,  // Email del bar
                paymentIntent.getId(),
                request.getTrackUri(),
                request.getTrackName(),
                request.getArtistName(),
                request.getAlbumName(),
                request.getCustomerName(),  // Nombre del cliente
                request.getDeviceId(),
                price.getPriceCents()
            );
            queuePaymentTransactionDao.save(transaction);
            
            System.out.println("💾 Transacción guardada en BD:");
            System.out.println("   🎵 Canción: " + request.getTrackName());
            System.out.println("   🎤 Artista: " + request.getArtistName());
            System.out.println("   💿 Álbum: " + request.getAlbumName());
            System.out.println("   👤 Cliente: " + request.getCustomerName());
            
            // Guardar datos en sesión
            session.setAttribute("queuePaymentIntentId", paymentIntent.getId());
            session.setAttribute("queueAccessToken", request.getAccessToken());
            session.setAttribute("queueTrackUri", request.getTrackUri());
            session.setAttribute("queueDeviceId", request.getDeviceId());
            
            System.out.println("📦 Datos guardados en sesión");
            System.out.println("🔑 Client Secret: " + paymentIntent.getClientSecret().substring(0, 20) + "...");
            System.out.println("═══════════════════════════════════");
            
            return paymentIntent.getClientSecret();
            
        } catch (StripeException e) {
            System.err.println("❌ Error de Stripe: " + e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al crear el pago: " + e.getMessage()
            );
        }
    }
    
    /**
     * Confirma el pago y encola la canción en Spotify
     */
    public void confirm(HttpSession session, String deviceIdParam) throws Exception {
        System.out.println("═══════════════════════════════════");
        System.out.println("✅ CONFIRMANDO PAGO DE ENCOLAMIENTO");
        System.out.println("═══════════════════════════════════");
        
        String piId = (String) session.getAttribute("queuePaymentIntentId");
        
        if (piId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No hay PaymentIntent en sesión"
            );
        }
        
        System.out.println("💳 Payment Intent ID: " + piId);
        
        // Buscar transacción
        QueuePaymentTransaction transaction = queuePaymentTransactionDao
            .findByPaymentIntentId(piId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Transacción no encontrada"
            ));
        
        System.out.println("📦 Transacción encontrada: ID " + transaction.getId());
        
        // Verificar pago con Stripe
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(piId);
            
            System.out.println("🔍 Estado del pago en Stripe: " + paymentIntent.getStatus());
            
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "El pago no fue exitoso: " + paymentIntent.getStatus()
                );
            }
            
            System.out.println("✅ Pago verificado en Stripe");
            
        } catch (StripeException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error al verificar el pago con Stripe: " + e.getMessage()
            );
        }
        
        // Obtener datos de la sesión
        String accessToken = (String) session.getAttribute("queueAccessToken");
        String trackUri = (String) session.getAttribute("queueTrackUri");
        
        String deviceId = (deviceIdParam != null && !deviceIdParam.isEmpty()) 
            ? deviceIdParam 
            : (String) session.getAttribute("queueDeviceId");
        
        if (accessToken == null || trackUri == null || deviceId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Faltan datos en la sesión (accessToken, trackUri o deviceId)"
            );
        }
        
        System.out.println("🎵 Track URI: " + trackUri);
        System.out.println("📱 Device ID: " + deviceId);
        System.out.println("🔑 Access Token: " + accessToken.substring(0, 20) + "...");
        
        // Encolar en Spotify
        System.out.println("🎵 Encolando canción en Spotify...");
        spotifyService.addToQueue(accessToken, trackUri, deviceId);
        
        // Actualizar transacción
        transaction.setStatus("completed");
        transaction.setCompletedAt(LocalDateTime.now());
        queuePaymentTransactionDao.save(transaction);
        
        System.out.println("💾 Transacción marcada como completada");
        
        // Limpiar sesión
        session.removeAttribute("queuePaymentIntentId");
        session.removeAttribute("queueAccessToken");
        session.removeAttribute("queueTrackUri");
        session.removeAttribute("queueDeviceId");
        
        System.out.println("🧹 Sesión limpiada");
        System.out.println("═══════════════════════════════════");
        System.out.println("✅ CANCIÓN ENCOLADA EXITOSAMENTE");
        System.out.println("═══════════════════════════════════");
    }
    
    /**
     * Obtiene el historial de canciones pagadas por un usuario
     */
    public List<QueuePaymentTransaction> getHistoryByEmail(String email) {
        return queuePaymentTransactionDao.findByEmailAndStatus(email, "completed");
    }
    
    /**
     * Obtiene todas las transacciones completadas
     */
    public List<QueuePaymentTransaction> getAllCompleted() {
        List<QueuePaymentTransaction> allTransactions = queuePaymentTransactionDao.findAll();
        return allTransactions.stream()
            .filter(t -> "completed".equals(t.getStatus()))
            .sorted((a, b) -> {
                if (a.getCompletedAt() == null) return 1;
                if (b.getCompletedAt() == null) return -1;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            })
            .toList();
    }
}
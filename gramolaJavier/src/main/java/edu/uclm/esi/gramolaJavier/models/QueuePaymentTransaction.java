package edu.uclm.esi.gramolaJavier.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_payment_transactions")
public class QueuePaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String paymentIntentId;
    
    @Column(nullable = false)
    private String trackUri;
    
    @Column(nullable = false)
    private String deviceId;
    
    @Column(nullable = false)
    private Long priceCents;
    
    @Column(nullable = false)
    private String status = "pending";  // pending, completed, failed
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    public QueuePaymentTransaction() {}
    
    public QueuePaymentTransaction(String email, String paymentIntentId, String trackUri, 
                                   String deviceId, Long priceCents) {
        this.email = email;
        this.paymentIntentId = paymentIntentId;
        this.trackUri = trackUri;
        this.deviceId = deviceId;
        this.priceCents = priceCents;
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { 
        this.paymentIntentId = paymentIntentId; 
    }
    
    public String getTrackUri() { return trackUri; }
    public void setTrackUri(String trackUri) { this.trackUri = trackUri; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { 
        this.completedAt = completedAt; 
    }
}
package edu.uclm.esi.gramolaJavier.models;

import java.security.MessageDigest;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String barName;
    
    @Column(nullable = false)
    private String clientId;
    
    @Column(nullable = false)
    private String clientSecret;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "token_id")
    private Token token;
    
    @Column(nullable = false)
    private boolean accountConfirmed = false;
    
    @Column(nullable = false)
    private boolean paymentConfirmed = false;
    
    @Column
    private String subscriptionType; // "monthly" o "annual"
    
    @Column
    private Long subscriptionExpirationDate; // timestamp en milisegundos
    
    @Column(length = 500)
    private String address; // Dirección postal del bar
    
    @Column
    private Double latitude; // Latitud (coordenada geográfica)
    
    @Column
    private Double longitude; // Longitud (coordenada geográfica)
    
    @Column(columnDefinition = "LONGTEXT")
    private String signature; // Firma del dueño (imagen en base64)
    
    public User() {}
    
    public User(String email, String password, String barName, String clientId, String clientSecret, Token token) {
        this.email = email;
        this.password = encryptPassword(password);
        this.barName = barName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.token = token;
        this.accountConfirmed = false;
        this.paymentConfirmed = false;
    }
    
    public User(String email, String password, String barName, String clientId, String clientSecret, Token token, String address, Double latitude, Double longitude) {
        this(email, password, barName, clientId, clientSecret, token);
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public User(String email, String password, String barName, String clientId, String clientSecret, Token token, String address, Double latitude, Double longitude, String signature) {
        this(email, password, barName, clientId, clientSecret, token, address, latitude, longitude);
        this.signature = signature;
    }
    
    // ✅ HACER PÚBLICO
    public String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar contraseña", e);
        }
    }
    
    // Getters y Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getBarName() { return barName; }
    public void setBarName(String barName) { this.barName = barName; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    
    public Token getToken() { return token; }
    public void setToken(Token token) { this.token = token; }
    
    public boolean isAccountConfirmed() { return accountConfirmed; }
    public void setAccountConfirmed(boolean accountConfirmed) { this.accountConfirmed = accountConfirmed; }
    
    public boolean isPaymentConfirmed() { return paymentConfirmed; }
    public void setPaymentConfirmed(boolean paymentConfirmed) { this.paymentConfirmed = paymentConfirmed; }
    
    public String getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(String subscriptionType) { this.subscriptionType = subscriptionType; }
    
    public Long getSubscriptionExpirationDate() { return subscriptionExpirationDate; }
    public void setSubscriptionExpirationDate(Long subscriptionExpirationDate) { this.subscriptionExpirationDate = subscriptionExpirationDate; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
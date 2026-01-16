package edu.uclm.esi.gramolaJavier.dto;

public class QueuePrepayRequest {
    private String email;
    private String accessToken;
    private String trackUri;
    private String deviceId;
    private Long priceCode;  // 199 o 299
    
    public QueuePrepayRequest() {}
    
    public QueuePrepayRequest(String email, String accessToken, String trackUri, 
                             String deviceId, Long priceCode) {
        this.email = email;
        this.accessToken = accessToken;
        this.trackUri = trackUri;
        this.deviceId = deviceId;
        this.priceCode = priceCode;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getTrackUri() { return trackUri; }
    public void setTrackUri(String trackUri) { this.trackUri = trackUri; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public Long getPriceCode() { return priceCode; }
    public void setPriceCode(Long priceCode) { this.priceCode = priceCode; }
}
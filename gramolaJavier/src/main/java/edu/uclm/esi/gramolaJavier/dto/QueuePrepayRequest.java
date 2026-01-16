package edu.uclm.esi.gramolaJavier.dto;

public class QueuePrepayRequest {
    private String customerName;  // Nombre de quien paga
    private String accessToken;
    private String trackUri;
    private String trackName;
    private String artistName;
    private String albumName;
    private String deviceId;
    private Long priceCode;  // 199 o 299
    private Double userLatitude;  // Ubicación del usuario
    private Double userLongitude; // Ubicación del usuario
    
    public QueuePrepayRequest() {}
    
    public QueuePrepayRequest(String customerName, String accessToken, String trackUri, 
                             String trackName, String artistName, String albumName,
                             String deviceId, Long priceCode, Double userLatitude, Double userLongitude) {
        this.customerName = customerName;
        this.accessToken = accessToken;
        this.trackUri = trackUri;
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.deviceId = deviceId;
        this.priceCode = priceCode;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
    }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getTrackUri() { return trackUri; }
    public void setTrackUri(String trackUri) { this.trackUri = trackUri; }
    
    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }
    
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    public String getAlbumName() { return albumName; }
    public void setAlbumName(String albumName) { this.albumName = albumName; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public Long getPriceCode() { return priceCode; }
    public void setPriceCode(Long priceCode) { this.priceCode = priceCode; }
    
    public Double getUserLatitude() { return userLatitude; }
    public void setUserLatitude(Double userLatitude) { this.userLatitude = userLatitude; }
    
    public Double getUserLongitude() { return userLongitude; }
    public void setUserLongitude(Double userLongitude) { this.userLongitude = userLongitude; }
}
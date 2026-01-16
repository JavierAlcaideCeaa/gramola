package edu.uclm.esi.gramolaJavier.services;

import org.springframework.stereotype.Service;

/**
 * Servicio para validar ubicaciones geográficas
 * Calcula distancias usando la fórmula de Haversine
 */
@Service
public class LocationService {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_DISTANCE_METERS = 100.0;
    
    /**
     * Verifica si el usuario está dentro del radio permitido del bar
     * 
     * @param userLat Latitud del usuario
     * @param userLon Longitud del usuario
     * @param barLat Latitud del bar
     * @param barLon Longitud del bar
     * @return true si está dentro de 100 metros, false en caso contrario
     */
    public boolean isWithinRange(Double userLat, Double userLon, Double barLat, Double barLon) {
        // Validar que todas las coordenadas existan
        if (userLat == null || userLon == null || barLat == null || barLon == null) {
            System.err.println("⚠️ Coordenadas inválidas o nulas");
            return false;
        }
        
        double distance = calculateDistance(userLat, userLon, barLat, barLon);
        
        System.out.println("📍 Distancia calculada: " + String.format("%.2f", distance) + " metros");
        System.out.println("📍 Usuario: (" + userLat + ", " + userLon + ")");
        System.out.println("📍 Bar: (" + barLat + ", " + barLon + ")");
        System.out.println("📍 Dentro de rango (100m): " + (distance <= MAX_DISTANCE_METERS));
        
        return distance <= MAX_DISTANCE_METERS;
    }
    
    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * 
     * @param lat1 Latitud del punto 1
     * @param lon1 Longitud del punto 1
     * @param lat2 Latitud del punto 2
     * @param lon2 Longitud del punto 2
     * @return Distancia en metros
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convertir grados a radianes
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        // Diferencias
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        
        // Fórmula de Haversine
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // Distancia en kilómetros
        double distanceKm = EARTH_RADIUS_KM * c;
        
        // Convertir a metros
        return distanceKm * 1000.0;
    }
    
    /**
     * Obtiene la distancia máxima permitida en metros
     */
    public double getMaxDistanceMeters() {
        return MAX_DISTANCE_METERS;
    }
}

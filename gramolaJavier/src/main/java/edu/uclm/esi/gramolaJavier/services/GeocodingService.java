package edu.uclm.esi.gramolaJavier.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servicio para obtener coordenadas geográficas desde direcciones postales
 * Utiliza la API de Nominatim (OpenStreetMap) - gratuita y sin necesidad de API key
 */
@Service
public class GeocodingService {

    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";
    
    /**
     * Obtiene las coordenadas (latitud, longitud) desde una dirección postal
     * 
     * @param address Dirección postal completa (ejemplo: "Calle Mayor 1, Madrid, España")
     * @return Array con [latitud, longitud]
     * @throws ResponseStatusException Si no se pueden obtener las coordenadas
     */
    public double[] getCoordinates(String address) {
        try {
            // Codificar la dirección para URL
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            
            // Construir URL de la API
            String urlString = NOMINATIM_API_URL + 
                "?q=" + encodedAddress + 
                "&format=json" +
                "&limit=1";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Configurar headers (User-Agent es OBLIGATORIO para Nominatim)
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GramolaJavier/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            // Leer respuesta
            int responseCode = conn.getResponseCode();
            
            if (responseCode != 200) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                    "Error al conectar con el servicio de geocodificación");
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // Parsear JSON
            JSONArray results = new JSONArray(response.toString());
            
            if (results.length() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No se encontraron coordenadas para la dirección proporcionada");
            }
            
            // Obtener primer resultado
            JSONObject firstResult = results.getJSONObject(0);
            double lat = firstResult.getDouble("lat");
            double lon = firstResult.getDouble("lon");
            
            System.out.println("📍 Coordenadas obtenidas para: " + address);
            System.out.println("   Latitud: " + lat + ", Longitud: " + lon);
            
            return new double[]{lat, lon};
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error al obtener coordenadas: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error al obtener coordenadas de la dirección");
        }
    }
    
    /**
     * Valida que una dirección sea válida intentando geocodificarla
     * 
     * @param address Dirección a validar
     * @return true si se pudieron obtener coordenadas, false en caso contrario
     */
    public boolean isValidAddress(String address) {
        try {
            getCoordinates(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

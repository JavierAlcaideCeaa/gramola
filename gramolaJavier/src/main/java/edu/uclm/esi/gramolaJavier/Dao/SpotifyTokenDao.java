package edu.uclm.esi.gramolaJavier.Dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import edu.uclm.esi.gramolaJavier.models.SpotifyToken;

@Repository
public interface SpotifyTokenDao extends JpaRepository<SpotifyToken, Long> {
    
    // ✅ CORREGIDO: findByEmail en lugar de findByUserEmail
    Optional<SpotifyToken> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    void deleteByEmail(String email);
}
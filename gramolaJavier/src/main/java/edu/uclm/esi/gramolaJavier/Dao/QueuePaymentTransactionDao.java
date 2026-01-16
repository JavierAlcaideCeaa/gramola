package edu.uclm.esi.gramolaJavier.Dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import edu.uclm.esi.gramolaJavier.models.QueuePaymentTransaction;
import java.util.Optional;
import java.util.List;

@Repository
public interface QueuePaymentTransactionDao extends JpaRepository<QueuePaymentTransaction, Long> {
    
    Optional<QueuePaymentTransaction> findByPaymentIntentId(String paymentIntentId);
    
    List<QueuePaymentTransaction> findByEmail(String email);
    
    List<QueuePaymentTransaction> findByEmailAndStatus(String email, String status);
}
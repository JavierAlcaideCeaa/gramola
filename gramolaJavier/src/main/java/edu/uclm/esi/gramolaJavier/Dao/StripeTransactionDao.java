package edu.uclm.esi.gramolaJavier.Dao;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.gramolaJavier.models.StripeTransaction;

public interface StripeTransactionDao extends JpaRepository<StripeTransaction, Long> {
}
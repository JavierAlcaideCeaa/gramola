package edu.uclm.esi.gramolaJavier.Dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import edu.uclm.esi.gramolaJavier.models.Price;

@Repository
public interface PriceDao extends JpaRepository<Price, Long> {
}
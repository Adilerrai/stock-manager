package com.gestion.repository;

import com.gestion.persistent.enums.StatutPromesse;
import com.gestion.persistent.model.PromessePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromessePaiementRepository extends JpaRepository<PromessePaiement, Long> {

    List<PromessePaiement> findByClientIdOrderByDateEcheancePromiseDesc(Long clientId);

    List<PromessePaiement> findByStatutOrderByDateEcheancePromiseAsc(StatutPromesse statut);

    @Query("SELECT p FROM PromessePaiement p WHERE p.statut = 'EN_ATTENTE' AND p.dateEcheancePromise < :date")
    List<PromessePaiement> findPromessesDepassees(@Param("date") LocalDate date);
}

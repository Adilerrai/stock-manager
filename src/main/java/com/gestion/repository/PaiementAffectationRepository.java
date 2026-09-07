package com.gestion.repository;

import com.gestion.persistent.model.PaiementAffectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementAffectationRepository extends JpaRepository<PaiementAffectation, Long> {
    List<PaiementAffectation> findByPaiementId(Long paiementId);
    List<PaiementAffectation> findByFactureId(Long factureId);
}

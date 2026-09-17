package com.gestion.repository;

import com.gestion.persistent.model.ReleveBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReleveBancaireRepository extends JpaRepository<ReleveBancaire, Long> {

    List<ReleveBancaire> findByPointDeVenteIdOrderByDateDebutDesc(Long pointDeVenteId);

    List<ReleveBancaire> findByCompteFinancierIdAndPointDeVenteIdOrderByDateDebutDesc(Long compteFinancierId, Long pointDeVenteId);

    Optional<ReleveBancaire> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<ReleveBancaire> findFirstByCompteFinancierIdAndPointDeVenteIdOrderByDateFinDesc(Long compteFinancierId, Long pointDeVenteId);
}

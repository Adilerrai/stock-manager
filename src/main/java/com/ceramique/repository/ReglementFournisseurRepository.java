package com.ceramique.repository;

import com.ceramique.persistent.model.ReglementFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReglementFournisseurRepository extends JpaRepository<ReglementFournisseur, Long> {
    List<ReglementFournisseur> findByPointDeVenteId(Long pointDeVenteId);
    Optional<ReglementFournisseur> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    List<ReglementFournisseur> findByFactureAchatIdAndPointDeVenteId(Long factureAchatId, Long pointDeVenteId);
}

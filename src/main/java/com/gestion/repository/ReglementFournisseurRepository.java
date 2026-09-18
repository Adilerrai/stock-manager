package com.gestion.repository;

import com.gestion.persistent.model.ReglementFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReglementFournisseurRepository extends JpaRepository<ReglementFournisseur, Long> {
    List<ReglementFournisseur> findByPointDeVenteId(Long pointDeVenteId);
    Optional<ReglementFournisseur> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    List<ReglementFournisseur> findByFactureAchatIdAndPointDeVenteId(Long factureAchatId, Long pointDeVenteId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM ReglementFournisseur r WHERE r.pointDeVenteId = :pointDeVenteId AND r.dateReglement BETWEEN :dateDebut AND :dateFin ORDER BY r.dateReglement DESC")
    List<ReglementFournisseur> findByPeriodeAndPointDeVenteId(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDateTime dateDebut,
                                                              @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDateTime dateFin,
                                                              @org.springframework.data.repository.query.Param("pointDeVenteId") Long pointDeVenteId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM ReglementFournisseur r WHERE r.factureAchat.fournisseur.id = :fournisseurId AND r.pointDeVenteId = :pointDeVenteId ORDER BY r.dateReglement ASC")
    List<ReglementFournisseur> findByFournisseurIdAndPointDeVenteId(@org.springframework.data.repository.query.Param("fournisseurId") Long fournisseurId,
                                                                    @org.springframework.data.repository.query.Param("pointDeVenteId") Long pointDeVenteId);
}


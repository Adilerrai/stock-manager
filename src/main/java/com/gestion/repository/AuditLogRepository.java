package com.gestion.repository;

import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Historique d'un objet précis (ex: toutes les actions sur l'écriture #42) */
    List<AuditLog> findByEntiteAndEntiteIdAndPointDeVenteIdOrderByDateActionDesc(
            String entite, Long entiteId, Long pointDeVenteId);

    @Query("SELECT a FROM AuditLog a WHERE a.entite = :entite AND a.entiteId = :entiteId " +
           "AND (:pointDeVenteId IS NULL OR a.pointDeVenteId = :pointDeVenteId) ORDER BY a.dateAction DESC")
    List<AuditLog> findByEntiteAndEntiteIdAndOptionalPointDeVenteId(
            @Param("entite") String entite,
            @Param("entiteId") Long entiteId,
            @Param("pointDeVenteId") Long pointDeVenteId);

    /** Historique par utilisateur */
    Page<AuditLog> findByUtilisateurAndPointDeVenteIdOrderByDateActionDesc(
            String utilisateur, Long pointDeVenteId, Pageable pageable);

    /** Historique par période */
    Page<AuditLog> findByPointDeVenteIdAndDateActionBetweenOrderByDateActionDesc(
            Long pointDeVenteId, LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable);

    /** Les N dernières actions par point de vente (ou tous si pointDeVenteId est null) */
    Page<AuditLog> findByPointDeVenteIdOrderByDateActionDesc(Long pointDeVenteId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (:pointDeVenteId IS NULL OR a.pointDeVenteId = :pointDeVenteId) ORDER BY a.dateAction DESC")
    Page<AuditLog> findByPointDeVenteIdOrAllOrderByDateActionDesc(
            @Param("pointDeVenteId") Long pointDeVenteId, Pageable pageable);

    /** Recherche multi-critères filtrée par point de vente */
    @Query("SELECT a FROM AuditLog a WHERE (:pointDeVenteId IS NULL OR a.pointDeVenteId = :pointDeVenteId) " +
           "AND (:entite IS NULL OR a.entite = :entite) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:utilisateur IS NULL OR LOWER(a.utilisateur) LIKE LOWER(CONCAT('%', :utilisateur, '%'))) " +
           "AND (:dateDebut IS NULL OR a.dateAction >= :dateDebut) " +
           "AND (:dateFin IS NULL OR a.dateAction <= :dateFin) " +
           "ORDER BY a.dateAction DESC")
    Page<AuditLog> rechercher(
            @Param("pointDeVenteId") Long pointDeVenteId,
            @Param("entite") String entite,
            @Param("action") ActionAudit action,
            @Param("utilisateur") String utilisateur,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable);
}

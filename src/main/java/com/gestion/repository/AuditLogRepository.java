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

    /** Historique par utilisateur */
    Page<AuditLog> findByUtilisateurAndPointDeVenteIdOrderByDateActionDesc(
            String utilisateur, Long pointDeVenteId, Pageable pageable);

    /** Historique par période */
    Page<AuditLog> findByPointDeVenteIdAndDateActionBetweenOrderByDateActionDesc(
            Long pointDeVenteId, LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable);

    /** Les N dernières actions (dashboard) */
    Page<AuditLog> findByPointDeVenteIdOrderByDateActionDesc(Long pointDeVenteId, Pageable pageable);

    /** Recherche multi-critères */
    @Query("SELECT a FROM AuditLog a WHERE a.pointDeVenteId = :tenantId " +
           "AND (:entite IS NULL OR a.entite = :entite) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:utilisateur IS NULL OR a.utilisateur = :utilisateur) " +
           "AND (:dateDebut IS NULL OR a.dateAction >= :dateDebut) " +
           "AND (:dateFin IS NULL OR a.dateAction <= :dateFin) " +
           "ORDER BY a.dateAction DESC")
    Page<AuditLog> rechercher(
            @Param("tenantId") Long tenantId,
            @Param("entite") String entite,
            @Param("action") ActionAudit action,
            @Param("utilisateur") String utilisateur,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable);
}

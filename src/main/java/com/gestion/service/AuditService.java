package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.repository.PointDeVenteRepository;
import com.gestion.persistent.dto.AuditLogDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.model.AuditLog;
import com.gestion.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service d'audit pour tracer toutes les actions par Point de Vente et utilisateur.
 * Permet aux responsables et collaborateurs d'un point de vente de consulter les actions de leurs collaborateurs.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final Map<Long, String> pdvNameCache = new ConcurrentHashMap<>();

    public AuditService(AuditLogRepository auditLogRepository, PointDeVenteRepository pointDeVenteRepository) {
        this.auditLogRepository = auditLogRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    /**
     * Récupère le point de vente actif de l'utilisateur connecté.
     * Priorise l'affectation pointDeVente de l'utilisateur, sinon le TenantContext.
     */
    public Long getCurrentPointDeVenteId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User u) {
                if (u.getPointDeVenteId() != null) {
                    return u.getPointDeVenteId();
                }
            }
        } catch (Exception ignored) {}
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                if (auth.getPrincipal() instanceof User u && u.getNomComplet() != null && !u.getNomComplet().isBlank()) {
                    return u.getNomComplet() + " (" + u.getEmail() + ")";
                }
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "Système";
    }

    // =========================================================================
    // MÉTHODES D'ÉCRITURE (Tracent chaque action avec le Point de Vente)
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreation(String entite, Long entiteId, String description) {
        logCreation(entite, entiteId, description, getCurrentPointDeVenteId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreation(String entite, Long entiteId, String description, Long pointDeVenteId) {
        Long pdv = pointDeVenteId != null ? pointDeVenteId : getCurrentPointDeVenteId();
        AuditLog log = AuditLog.creation(entite, entiteId, description, getCurrentUser(), pdv);
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logModification(String entite, Long entiteId, String champModifie,
                                 String ancienneValeur, String nouvelleValeur, String description) {
        logModification(entite, entiteId, champModifie, ancienneValeur, nouvelleValeur, description, getCurrentPointDeVenteId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logModification(String entite, Long entiteId, String champModifie,
                                 String ancienneValeur, String nouvelleValeur, String description, Long pointDeVenteId) {
        Long pdv = pointDeVenteId != null ? pointDeVenteId : getCurrentPointDeVenteId();
        AuditLog log = AuditLog.modification(entite, entiteId, champModifie,
                ancienneValeur, nouvelleValeur, description, getCurrentUser(), pdv);
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuppression(String entite, Long entiteId, String description) {
        logSuppression(entite, entiteId, description, getCurrentPointDeVenteId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuppression(String entite, Long entiteId, String description, Long pointDeVenteId) {
        Long pdv = pointDeVenteId != null ? pointDeVenteId : getCurrentPointDeVenteId();
        AuditLog log = AuditLog.action(ActionAudit.SUPPRESSION, entite, entiteId,
                description, getCurrentUser(), pdv);
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logValidation(String entite, Long entiteId, String description) {
        logValidation(entite, entiteId, description, getCurrentPointDeVenteId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logValidation(String entite, Long entiteId, String description, Long pointDeVenteId) {
        Long pdv = pointDeVenteId != null ? pointDeVenteId : getCurrentPointDeVenteId();
        AuditLog log = AuditLog.action(ActionAudit.VALIDATION, entite, entiteId,
                description, getCurrentUser(), pdv);
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(ActionAudit action, String entite, Long entiteId, String description) {
        logAction(action, entite, entiteId, description, getCurrentPointDeVenteId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(ActionAudit action, String entite, Long entiteId, String description, Long pointDeVenteId) {
        Long pdv = pointDeVenteId != null ? pointDeVenteId : getCurrentPointDeVenteId();
        AuditLog log = AuditLog.action(action, entite, entiteId,
                description, getCurrentUser(), pdv);
        auditLogRepository.save(log);
    }

    // =========================================================================
    // MÉTHODES DE LECTURE (Filtrées par Point de Vente)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistoriqueEntite(String entite, Long entiteId) {
        return getHistoriqueEntite(entite, entiteId, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistoriqueEntite(String entite, Long entiteId, Long pointDeVenteId) {
        return auditLogRepository.findByEntiteAndEntiteIdAndOptionalPointDeVenteId(
                entite, entiteId, pointDeVenteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getActionsRecentes(Long pointDeVenteId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByPointDeVenteIdOrAllOrderByDateActionDesc(pointDeVenteId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> rechercher(Long pointDeVenteId, String entite, ActionAudit action, String utilisateur,
                                         LocalDateTime dateDebut, LocalDateTime dateFin,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.rechercher(pointDeVenteId, entite, action, utilisateur,
                dateDebut, dateFin, pageable)
                .map(this::toDto);
    }

    // =========================================================================
    // MAPPING & ENRICHISSEMENT
    // =========================================================================

    private AuditLogDTO toDto(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setEntite(log.getEntite());
        dto.setEntiteId(log.getEntiteId());
        dto.setAction(log.getAction());
        dto.setChampModifie(log.getChampModifie());
        dto.setAncienneValeur(log.getAncienneValeur());
        dto.setNouvelleValeur(log.getNouvelleValeur());
        dto.setDescription(log.getDescription());
        dto.setUtilisateur(log.getUtilisateur());
        dto.setDateAction(log.getDateAction());
        dto.setPointDeVenteId(log.getPointDeVenteId());

        if (log.getPointDeVenteId() != null) {
            String nomPdv = pdvNameCache.computeIfAbsent(log.getPointDeVenteId(), id -> {
                try {
                    return pointDeVenteRepository.findById(id)
                            .map(PointDeVente::getNomPointDeVente)
                            .orElse("Point de Vente #" + id);
                } catch (Exception e) {
                    return "Point de Vente #" + id;
                }
            });
            dto.setNomPointDeVente(nomPdv);
        }

        return dto;
    }
}

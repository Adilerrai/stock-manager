package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
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
import java.util.stream.Collectors;

/**
 * Service d'audit pour tracer toutes les actions sur les entités comptables.
 * Utilise REQUIRES_NEW pour s'assurer que les logs sont persistés même en cas de rollback.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    private Long getTenantId() {
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "Système";
    }

    // =========================================================================
    // MÉTHODES D'ÉCRITURE (appelées par les services métier)
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreation(String entite, Long entiteId, String description) {
        AuditLog log = AuditLog.creation(entite, entiteId, description, getCurrentUser(), getTenantId());
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logModification(String entite, Long entiteId, String champModifie,
                                 String ancienneValeur, String nouvelleValeur, String description) {
        AuditLog log = AuditLog.modification(entite, entiteId, champModifie,
                ancienneValeur, nouvelleValeur, description, getCurrentUser(), getTenantId());
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuppression(String entite, Long entiteId, String description) {
        AuditLog log = AuditLog.action(ActionAudit.SUPPRESSION, entite, entiteId,
                description, getCurrentUser(), getTenantId());
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logValidation(String entite, Long entiteId, String description) {
        AuditLog log = AuditLog.action(ActionAudit.VALIDATION, entite, entiteId,
                description, getCurrentUser(), getTenantId());
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(ActionAudit action, String entite, Long entiteId, String description) {
        AuditLog log = AuditLog.action(action, entite, entiteId,
                description, getCurrentUser(), getTenantId());
        auditLogRepository.save(log);
    }

    // =========================================================================
    // MÉTHODES DE LECTURE (appelées par le controller)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistoriqueEntite(String entite, Long entiteId) {
        return auditLogRepository.findByEntiteAndEntiteIdAndPointDeVenteIdOrderByDateActionDesc(
                entite, entiteId, getTenantId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getActionsRecentes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByPointDeVenteIdOrderByDateActionDesc(getTenantId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> rechercher(String entite, ActionAudit action, String utilisateur,
                                         LocalDateTime dateDebut, LocalDateTime dateFin,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.rechercher(getTenantId(), entite, action, utilisateur,
                dateDebut, dateFin, pageable)
                .map(this::toDto);
    }

    // =========================================================================
    // MAPPING
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
        return dto;
    }
}

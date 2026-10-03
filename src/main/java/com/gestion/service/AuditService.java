package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.AuditLogDTO;
import com.gestion.persistent.dto.AuditStatsResponseDTO;
import com.gestion.persistent.dto.CollaborateurAuditStatDTO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service d'audit pour tracer toutes les actions sur les entites comptables et commerciales.
 * Utilise REQUIRES_NEW pour s'assurer que les logs sont persistes meme en cas
 * de rollback.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Long getCurrentPointDeVenteId() {
        return getTenantId();
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
        } catch (Exception ignored) {
        }
        return "Systeme";
    }

    // =========================================================================
    // METHODES D'ECRITURE (appelees par les services metier)
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
    // METHODES DE LECTURE (appelees par le controller)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistoriqueEntite(String entite, Long entiteId) {
        return getHistoriqueEntite(entite, entiteId, getTenantId());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getHistoriqueEntite(String entite, Long entiteId, Long pointDeVenteId) {
        return auditLogRepository.findByEntiteAndEntiteIdAndOptionalPointDeVenteId(
                entite, entiteId, pointDeVenteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getActionsRecentes(int page, int size) {
        return getActionsRecentes(getTenantId(), page, size);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getActionsRecentes(Long pointDeVenteId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByPointDeVenteIdOrAllOrderByDateActionDesc(pointDeVenteId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> rechercher(String entite, ActionAudit action, String utilisateur,
            LocalDateTime dateDebut, LocalDateTime dateFin,
            int page, int size) {
        return rechercher(getTenantId(), entite, action, utilisateur, dateDebut, dateFin, page, size);
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

    @Transactional(readOnly = true)
    public List<String> getCollaborateursPointDeVente(Long pointDeVenteId) {
        return auditLogRepository.findDistinctUtilisateursByPointDeVenteId(pointDeVenteId);
    }

    @Transactional(readOnly = true)
    public AuditStatsResponseDTO getStatistiquesPointDeVente(Long pointDeVenteId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        AuditStatsResponseDTO stats = new AuditStatsResponseDTO();
        stats.setPointDeVenteId(pointDeVenteId);

        List<AuditLog> logs = auditLogRepository.findForStats(pointDeVenteId, dateDebut, dateFin);
        stats.setTotalActions(logs.size());

        long creations = 0;
        long modifications = 0;
        long suppressions = 0;
        long validations = 0;
        long annulations = 0;

        Map<String, Long> entiteMap = new HashMap<>();
        Map<String, CollaborateurAuditStatDTO> userStatsMap = new HashMap<>();

        for (AuditLog l : logs) {
            if (l.getAction() != null) {
                switch (l.getAction()) {
                    case CREATION -> creations++;
                    case MODIFICATION -> modifications++;
                    case SUPPRESSION -> suppressions++;
                    case VALIDATION -> validations++;
                    case ANNULATION -> annulations++;
                    default -> {}
                }
            }

            if (l.getEntite() != null) {
                entiteMap.merge(l.getEntite(), 1L, Long::sum);
            }

            String user = l.getUtilisateur() != null ? l.getUtilisateur() : "Inconnu";
            CollaborateurAuditStatDTO cStat = userStatsMap.computeIfAbsent(user, CollaborateurAuditStatDTO::new);
            cStat.setTotalActions(cStat.getTotalActions() + 1);
            if (l.getAction() != null) {
                switch (l.getAction()) {
                    case CREATION -> cStat.setCreations(cStat.getCreations() + 1);
                    case MODIFICATION -> cStat.setModifications(cStat.getModifications() + 1);
                    case SUPPRESSION -> cStat.setSuppressions(cStat.getSuppressions() + 1);
                    case VALIDATION -> cStat.setValidations(cStat.getValidations() + 1);
                    case ANNULATION -> cStat.setAnnulations(cStat.getAnnulations() + 1);
                    default -> {}
                }
            }
            if (cStat.getDerniereDateAction() == null || (l.getDateAction() != null && l.getDateAction().isAfter(cStat.getDerniereDateAction()))) {
                cStat.setDerniereDateAction(l.getDateAction());
                cStat.setDerniereEntite(l.getEntite());
            }
        }

        stats.setTotalCreations(creations);
        stats.setTotalModifications(modifications);
        stats.setTotalSuppressions(suppressions);
        stats.setTotalValidations(validations);
        stats.setTotalAnnulations(annulations);
        stats.setRepartitionParEntite(entiteMap);
        stats.setCollaborateurs(new ArrayList<>(userStatsMap.values()));

        return stats;
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
        dto.setPointDeVenteId(log.getPointDeVenteId());
        return dto;
    }
}

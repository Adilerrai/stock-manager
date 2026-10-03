package com.gestion.controller;

import com.acommon.persistant.model.User;
import com.gestion.persistent.dto.AuditLogDTO;
import com.gestion.persistent.dto.AuditStatsResponseDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrôleur d'audit accessible par Point de Vente.
 * Permet aux managers et collaborateurs de consulter la traçabilité des actions de leurs collaborateurs.
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_GESTIONNAIRE', 'ADMIN_GESTION', 'COMPTA_READ', 'USER_READ')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Résout le Point de Vente effectif :
     * - Pour SuperAdmin / Admin : accepte le paramètre pointDeVenteId (ou null pour voir tous)
     * - Pour les autres utilisateurs (Manager / Collaborateur de Point de Vente) : restreint strictement à LEUR point de vente.
     */
    private Long resolveEffectivePointDeVenteId(Long requestedPdvId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdminOrAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_SUPERADMIN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));

        if (isSuperAdminOrAdmin) {
            return requestedPdvId; // SuperAdmin peut filtrer ou tout voir (null)
        }

        // Pour un manager ou collaborateur de magasin : restreint à son Point de Vente
        if (auth != null && auth.getPrincipal() instanceof User u) {
            if (u.getPointDeVenteId() != null) {
                return u.getPointDeVenteId();
            }
        }
        return auditService.getCurrentPointDeVenteId();
    }

    /**
     * Historique d'un objet précis (ex: toutes les actions sur l'écriture ou la commande #42)
     */
    @GetMapping("/{entite}/{entiteId}")
    public ResponseEntity<List<AuditLogDTO>> getHistoriqueEntite(
            @PathVariable String entite,
            @PathVariable Long entiteId,
            @RequestParam(required = false) Long pointDeVenteId) {
        Long effectivePdv = resolveEffectivePointDeVenteId(pointDeVenteId);
        return ResponseEntity.ok(auditService.getHistoriqueEntite(entite, entiteId, effectivePdv));
    }

    /**
     * Les dernières actions d'audit filtrées par Point de Vente
     */
    @GetMapping("/recents")
    public ResponseEntity<Page<AuditLogDTO>> getActionsRecentes(
            @RequestParam(required = false) Long pointDeVenteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long effectivePdv = resolveEffectivePointDeVenteId(pointDeVenteId);
        return ResponseEntity.ok(auditService.getActionsRecentes(effectivePdv, page, size));
    }

    /**
     * Recherche multi-critères (par entité, action, utilisateur, dates et point de vente)
     */
    @GetMapping("/recherche")
    public ResponseEntity<Page<AuditLogDTO>> rechercher(
            @RequestParam(required = false) Long pointDeVenteId,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) ActionAudit action,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long effectivePdv = resolveEffectivePointDeVenteId(pointDeVenteId);
        return ResponseEntity.ok(auditService.rechercher(effectivePdv, entite, action, utilisateur, dateDebut, dateFin, page, size));
    }

    /**
     * Statistiques consolidées des activités collaborateurs par Point de Vente
     */
    @GetMapping("/statistiques")
    public ResponseEntity<AuditStatsResponseDTO> getStatistiques(
            @RequestParam(required = false) Long pointDeVenteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        Long effectivePdv = resolveEffectivePointDeVenteId(pointDeVenteId);
        return ResponseEntity.ok(auditService.getStatistiquesPointDeVente(effectivePdv, dateDebut, dateFin));
    }

    /**
     * Liste des collaborateurs ayant une activité enregistrée sur ce Point de Vente
     */
    @GetMapping("/collaborateurs")
    public ResponseEntity<List<String>> getCollaborateurs(
            @RequestParam(required = false) Long pointDeVenteId) {
        Long effectivePdv = resolveEffectivePointDeVenteId(pointDeVenteId);
        return ResponseEntity.ok(auditService.getCollaborateursPointDeVente(effectivePdv));
    }

}

package com.gestion.controller;

import com.gestion.persistent.dto.AuditLogDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Historique d'un objet précis (ex: toutes les actions sur l'écriture #42)
     */
    @GetMapping("/{entite}/{entiteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<AuditLogDTO>> getHistoriqueEntite(
            @PathVariable String entite,
            @PathVariable Long entiteId) {
        return ResponseEntity.ok(auditService.getHistoriqueEntite(entite, entiteId));
    }

    /**
     * Les dernières actions (dashboard d'audit)
     */
    @GetMapping("/recents")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<Page<AuditLogDTO>> getActionsRecentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getActionsRecentes(page, size));
    }

    /**
     * Recherche multi-critères (par entité, action, utilisateur, dates)
     */
    @GetMapping("/recherche")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<Page<AuditLogDTO>> rechercher(
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) ActionAudit action,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.rechercher(entite, action, utilisateur, dateDebut, dateFin, page, size));
    }
}

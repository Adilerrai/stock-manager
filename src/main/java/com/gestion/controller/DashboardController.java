package com.gestion.controller;

import com.gestion.persistent.dto.DashboardDTO;
import com.gestion.persistent.dto.JournalActiviteDTO;
import com.gestion.service.DashboardService;
import com.gestion.service.JournalActiviteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final JournalActiviteService journalActiviteService;

    public DashboardController(DashboardService dashboardService, JournalActiviteService journalActiviteService) {
        this.dashboardService = dashboardService;
        this.journalActiviteService = journalActiviteService;
    }

    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard() {
        DashboardDTO metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Journal d'activité : devis créés, BC clients, BL créés/expédiés/livrés, factures, avoirs,
     * BC fournisseurs, réceptions, encaissements, règlements, dépenses — du plus récent au plus ancien.
     * Période par défaut : aujourd'hui. {@code types} = liste séparée par des virgules (vide = tous).
     * Réservé à l'admin de l'entreprise et aux rôles ayant l'habilitation JOURNAL_ACTIVITE_READ.
     */
    @GetMapping("/journal")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'JOURNAL_ACTIVITE_READ')")
    public ResponseEntity<JournalActiviteDTO> getJournal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(journalActiviteService.getJournal(dateDebut, dateFin, types, q, page, size));
    }
}

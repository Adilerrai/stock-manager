package com.gestion.controller;

import com.gestion.persistent.dto.AnomalieDTO;
import com.gestion.service.AnomalieService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@CrossOrigin(origins = "*")
public class AnomalieController {

    private final AnomalieService anomalieService;

    public AnomalieController(AnomalieService anomalieService) {
        this.anomalieService = anomalieService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<AnomalieDTO>> getToutesLesAnomalies() {
        List<AnomalieDTO> list = anomalieService.detecterToutesLesAnomalies();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/critiques")
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCritiques() {
        List<AnomalieDTO> list = anomalieService.detecterParSeverite("CRITIQUE");
        return ResponseEntity.ok(list);
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCredits() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCredits());
    }

    @GetMapping("/factures")
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesFactures() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesFactures());
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'STOCK_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesStock() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesStock());
    }

    @GetMapping("/caisse")
    @PreAuthorize("hasAnyAuthority('ANOMALIES_VOIR', 'TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCaisse() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCaisse());
    }
}

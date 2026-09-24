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
    @PreAuthorize("hasAuthority('DASHBOARD_VOIR') or hasAuthority('RAPPORT_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getToutesLesAnomalies() {
        List<AnomalieDTO> list = anomalieService.detecterToutesLesAnomalies();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/critiques")
    @PreAuthorize("hasAuthority('DASHBOARD_VOIR') or hasAuthority('RAPPORT_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCritiques() {
        List<AnomalieDTO> list = anomalieService.detecterParSeverite("CRITIQUE");
        return ResponseEntity.ok(list);
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAuthority('VENTE_READ') or hasAuthority('CLIENT_READ') or hasAuthority('DASHBOARD_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCredits() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCredits());
    }

    @GetMapping("/factures")
    @PreAuthorize("hasAuthority('VENTE_READ') or hasAuthority('ACHAT_FACTURE_READ') or hasAuthority('DASHBOARD_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesFactures() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesFactures());
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('STOCK_READ') or hasAuthority('DASHBOARD_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesStock() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesStock());
    }

    @GetMapping("/caisse")
    @PreAuthorize("hasAuthority('TRESORERIE_READ') or hasAuthority('TRESORERIE_GESTION') or hasAuthority('DASHBOARD_VOIR')")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCaisse() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCaisse());
    }
}

package com.gestion.controller;

import com.gestion.persistent.dto.AnomalieDTO;
import com.gestion.service.AnomalieService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<AnomalieDTO>> getToutesLesAnomalies() {
        List<AnomalieDTO> list = anomalieService.detecterToutesLesAnomalies();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/critiques")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCritiques() {
        List<AnomalieDTO> list = anomalieService.detecterParSeverite("CRITIQUE");
        return ResponseEntity.ok(list);
    }

    @GetMapping("/credits")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCredits() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCredits());
    }

    @GetMapping("/factures")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesFactures() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesFactures());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesStock() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesStock());
    }

    @GetMapping("/caisse")
    public ResponseEntity<List<AnomalieDTO>> getAnomaliesCaisse() {
        return ResponseEntity.ok(anomalieService.detecterAnomaliesCaisse());
    }
}

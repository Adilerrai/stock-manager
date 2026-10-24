package com.gestion.controller;

import com.gestion.persistent.dto.RecapCoutRevientDTO;
import com.gestion.persistent.dto.SimulationCoutRevientRequest;
import com.gestion.service.CoutRevientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commandes")
public class CoutRevientController {

    private final CoutRevientService coutRevientService;

    public CoutRevientController(CoutRevientService coutRevientService) {
        this.coutRevientService = coutRevientService;
    }

    @GetMapping("/{commandeId}/cout-revient")
    public ResponseEntity<RecapCoutRevientDTO> getCoutRevient(@PathVariable Long commandeId) {
        return ResponseEntity.ok(coutRevientService.recalculerEtPersister(commandeId));
    }

    @PostMapping("/{commandeId}/cout-revient/recalculer")
    public ResponseEntity<RecapCoutRevientDTO> recalculerCoutRevient(@PathVariable Long commandeId) {
        return ResponseEntity.ok(coutRevientService.recalculerEtPersister(commandeId));
    }

    @PostMapping("/cout-revient/simuler")
    public ResponseEntity<RecapCoutRevientDTO> simulerCoutRevient(@RequestBody SimulationCoutRevientRequest request) {
        return ResponseEntity.ok(coutRevientService.simulerCoutRevient(request));
    }
}

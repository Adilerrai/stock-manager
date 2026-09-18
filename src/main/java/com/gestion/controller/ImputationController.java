package com.gestion.controller;

import com.gestion.persistent.dto.ImputationSuggestionDTO;
import com.gestion.service.ImputationIntelligenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/comptabilite/imputation")
@CrossOrigin(origins = "*")
public class ImputationController {

    private final ImputationIntelligenteService imputationService;

    public ImputationController(ImputationIntelligenteService imputationService) {
        this.imputationService = imputationService;
    }

    @GetMapping("/suggerer")
    public ResponseEntity<ImputationSuggestionDTO> suggererImputation(
            @RequestParam String libelle,
            @RequestParam(required = false, defaultValue = "ACHAT") String typeOperation,
            @RequestParam(required = false) BigDecimal montant) {

        return ResponseEntity.ok(imputationService.suggererImputation(libelle, typeOperation, montant));
    }
}

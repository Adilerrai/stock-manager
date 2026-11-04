package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest;
import com.gestion.persistent.dto.compta.ConsultationDtos.PageLignesLettrageDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.PropositionLettrageDTO;
import com.gestion.service.compta.LettrageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : lettrage")
public class LettrageComptaController {

    private final LettrageService service;

    public LettrageComptaController(LettrageService service) {
        this.service = service;
    }

    @GetMapping("/lettrage/lignes")
    @Operation(operationId = "comptaLettrageLignes", summary = "Lignes d'un tiers sur un compte lettrable")
    public ResponseEntity<PageLignesLettrageDTO> lignes(@RequestParam String compte, @RequestParam Long tiersId,
                                                        @RequestParam(defaultValue = "OUVERTES") String etat,
                                                        @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.lignes(compte, tiersId, etat, curseur, taille));
    }

    @GetMapping("/lettrage/propositions")
    @Operation(operationId = "comptaLettragePropositions", summary = "Rapprochements proposés : jamais entre deux tiers, jamais sans confirmation")
    public ResponseEntity<List<PropositionLettrageDTO>> propositions(@RequestParam String compte, @RequestParam Long tiersId) {
        return ResponseEntity.ok(service.propositions(compte, tiersId));
    }

    @PostMapping("/lettrages")
    @Operation(operationId = "comptaLettrageLettrer", summary = "Lettre des lignes d'un même tiers")
    public ResponseEntity<LettrageDTO> lettrer(@Valid @RequestBody LettrageRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.lettrer(demande));
    }

    @DeleteMapping("/lettrages/{id}")
    @Operation(operationId = "comptaLettrageDelettrer", summary = "Défait un seul groupe de lettrage")
    public ResponseEntity<Void> delettrer(@PathVariable Long id) {
        service.delettrer(id);
        return ResponseEntity.noContent().build();
    }
}

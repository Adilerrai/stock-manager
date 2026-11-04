package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.JournalCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.service.compta.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta/journaux", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : journaux")
public class JournalController {

    private final JournalService service;

    public JournalController(JournalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "comptaJournalLister", summary = "Journaux du livre")
    public ResponseEntity<List<JournalDTO>> lister() {
        return ResponseEntity.ok(service.lister());
    }

    @PostMapping
    @Operation(operationId = "comptaJournalCreer", summary = "Ajoute un journal de banque ou de caisse")
    public ResponseEntity<JournalDTO> creer(@Valid @RequestBody JournalCreationRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }
}

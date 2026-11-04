package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.RapprochementDtos.ComptabiliserReleveRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.CompteRapprochementDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatRapprochementDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatValideDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.PropositionPointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ValiderEtatRequest;
import com.gestion.service.compta.RapprochementComptaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta/rapprochement", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : rapprochement")
public class RapprochementComptaController {

    private final RapprochementComptaService service;

    public RapprochementComptaController(RapprochementComptaService service) {
        this.service = service;
    }

    @GetMapping("/comptes")
    @Operation(operationId = "comptaRapprochementComptes", summary = "Les comptes de banque du livre, et leur compte comptable")
    public ResponseEntity<List<CompteRapprochementDTO>> comptes() {
        return ResponseEntity.ok(service.comptes());
    }

    @GetMapping("/{compteFinancierId}")
    @Operation(operationId = "comptaRapprochementEtat", summary = "L'état à la date demandée, et les deux listes non pointées")
    public ResponseEntity<EtatRapprochementDTO> etat(@PathVariable Long compteFinancierId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok(service.etat(compteFinancierId, arrete));
    }

    @GetMapping("/{compteFinancierId}/propositions")
    @Operation(operationId = "comptaRapprochementPropositions", summary = "Rapproche sur le montant et la date, et attend une confirmation")
    public ResponseEntity<List<PropositionPointageDTO>> propositions(@PathVariable Long compteFinancierId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok(service.propositions(compteFinancierId, arrete));
    }

    @GetMapping("/{compteFinancierId}/pointages")
    @Operation(operationId = "comptaRapprochementPointages", summary = "Les pointages de ce compte")
    public ResponseEntity<List<PointageDTO>> pointages(@PathVariable Long compteFinancierId) {
        return ResponseEntity.ok(service.pointages(compteFinancierId));
    }

    @GetMapping("/{compteFinancierId}/etats")
    @Operation(operationId = "comptaRapprochementEtats", summary = "Les états validés de ce compte")
    public ResponseEntity<List<EtatValideDTO>> etats(@PathVariable Long compteFinancierId) {
        return ResponseEntity.ok(service.etats(compteFinancierId));
    }

    @PostMapping(value = "/{compteFinancierId}/valider", consumes = "application/json")
    @Operation(operationId = "comptaRapprochementValider", summary = "Valide l'état : il doit tomber juste")
    public ResponseEntity<EtatValideDTO> valider(@PathVariable Long compteFinancierId, @Valid @RequestBody ValiderEtatRequest demande) {
        return ResponseEntity.ok(service.valider(compteFinancierId, demande));
    }

    @PostMapping(value = "/pointages", consumes = "application/json")
    @Operation(operationId = "comptaRapprochementPointer", summary = "Pointe une ligne d'écriture sur une ligne de relevé")
    public ResponseEntity<PointageDTO> pointer(@Valid @RequestBody PointageRequest demande) {
        return ResponseEntity.ok(service.pointer(demande));
    }

    @DeleteMapping("/pointages/{id}")
    @Operation(operationId = "comptaRapprochementDepointer", summary = "Retire un pointage")
    public ResponseEntity<Void> depointer(@PathVariable Long id) {
        service.depointer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/lignes-releve/{id}/comptabiliser", consumes = "application/json")
    @Operation(operationId = "comptaRapprochementComptabiliser", summary = "Passe l'écriture d'une ligne de relevé et la pointe dans la même transaction")
    public ResponseEntity<PointageDTO> comptabiliser(@PathVariable Long id, @Valid @RequestBody ComptabiliserReleveRequest demande) {
        return ResponseEntity.ok(service.comptabiliser(id, demande));
    }
}

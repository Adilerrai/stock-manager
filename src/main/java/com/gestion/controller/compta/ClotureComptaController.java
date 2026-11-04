package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ClotureDtos.AffectationRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArchiveDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreteDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureDefinitiveDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureExerciceDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.LeverArreteRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.StatutExerciceDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.service.compta.ClotureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/compta/exercices/{id}", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : clôture")
public class ClotureComptaController {

    private final ClotureService service;

    public ClotureComptaController(ClotureService service) {
        this.service = service;
    }

    @GetMapping("/cloture")
    @Operation(operationId = "comptaClotureEtat", summary = "Où en est la clôture de l'exercice")
    public ResponseEntity<ClotureExerciceDTO> etat(@PathVariable Long id) {
        return ResponseEntity.ok(service.etat(id));
    }

    @PostMapping(value = "/arreter", consumes = "application/json")
    @Operation(operationId = "comptaClotureArreter", summary = "Arrête l'exercice : contrôles rejoués, états figés, plus aucune écriture")
    public ResponseEntity<ArreteDTO> arreter(@PathVariable Long id, @Valid @RequestBody ArreterRequest demande) {
        return ResponseEntity.ok(service.arreter(id, demande));
    }

    @PostMapping(value = "/lever-arrete", consumes = "application/json")
    @Operation(operationId = "comptaClotureLever", summary = "Lève l'arrêté, avec un motif gardé en trace")
    public ResponseEntity<StatutExerciceDTO> lever(@PathVariable Long id, @Valid @RequestBody LeverArreteRequest demande) {
        return ResponseEntity.ok(service.leverArrete(id, demande));
    }

    @PostMapping(value = "/cloturer", consumes = "application/json")
    @Operation(operationId = "comptaClotureCloturer", summary = "Clôture définitive, sans retour : écriture de clôture, à-nouveaux, extournes, archive")
    public ResponseEntity<ClotureDefinitiveDTO> cloturer(@PathVariable Long id, @Valid @RequestBody CloturerRequest demande) {
        return ResponseEntity.ok(service.cloturer(id, demande));
    }

    @PostMapping("/archiver")
    @Operation(operationId = "comptaClotureArchiver", summary = "Rejoue l'archive de clôture si elle manque")
    public ResponseEntity<ArchiveDTO> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(new ArchiveDTO(service.archiver(id)));
    }

    @PostMapping(value = "/affectation", consumes = "application/json")
    @Operation(operationId = "comptaClotureAffecter", summary = "Affecte le résultat de l'exercice clos, dans l'exercice suivant")
    public ResponseEntity<EcritureDTO> affecter(@PathVariable Long id, @Valid @RequestBody AffectationRequest demande) {
        return ResponseEntity.ok(service.affecter(id, demande));
    }
}

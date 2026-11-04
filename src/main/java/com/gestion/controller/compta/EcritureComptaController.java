package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.ContrepassationRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.PieceJointeRequest;
import com.gestion.service.compta.EcritureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : brouillons et écritures")
public class EcritureComptaController {

    private final EcritureService service;

    public EcritureComptaController(EcritureService service) {
        this.service = service;
    }

    @GetMapping("/brouillons")
    @Operation(operationId = "comptaEcritureBrouillons", summary = "Brouillons du livre : aucun état ne les lit")
    public ResponseEntity<List<BrouillonDTO>> brouillons(@RequestParam(required = false) Long journalId, @RequestParam(required = false) Long periodeId) {
        return ResponseEntity.ok(service.listerBrouillons(journalId, periodeId));
    }

    @GetMapping("/brouillons/{id}")
    @Operation(operationId = "comptaEcritureBrouillon", summary = "Un brouillon et ses anomalies")
    public ResponseEntity<BrouillonDTO> brouillon(@PathVariable Long id) {
        return ResponseEntity.ok(service.lireBrouillon(id));
    }

    @PostMapping("/brouillons")
    @Operation(operationId = "comptaEcritureCreer", summary = "Crée un brouillon, même déséquilibré")
    public ResponseEntity<BrouillonDTO> creer(@Valid @RequestBody BrouillonRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerBrouillon(demande));
    }

    @PutMapping("/brouillons/{id}")
    @Operation(operationId = "comptaEcritureModifier", summary = "Remplace un brouillon")
    public ResponseEntity<BrouillonDTO> modifier(@PathVariable Long id, @Valid @RequestBody BrouillonRequest demande) {
        return ResponseEntity.ok(service.modifierBrouillon(id, demande));
    }

    @DeleteMapping("/brouillons/{id}")
    @Operation(operationId = "comptaEcritureSupprimer", summary = "Supprime un brouillon")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimerBrouillon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/brouillons/{id}/valider")
    @Operation(operationId = "comptaEcritureValider", summary = "Valide : l'écriture entre au journal, numérotée par la base")
    public ResponseEntity<EcritureDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(service.validerBrouillon(id));
    }

    @GetMapping("/ecritures/{id}")
    @Operation(operationId = "comptaEcritureEcriture", summary = "Une écriture validée")
    public ResponseEntity<EcritureDTO> ecriture(@PathVariable Long id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @GetMapping("/ecritures")
    @Operation(operationId = "comptaEcritureRechercher", summary = "Recherche par pièce, date, montant de ligne, libellé, compte ou période. Les 200 plus récentes")
    public ResponseEntity<List<EcritureDTO>> rechercher(@RequestParam(required = false) String refPiece,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePiece,
                                                        @io.swagger.v3.oas.annotations.Parameter(schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")) @RequestParam(required = false) BigDecimal montant,
                                                        @RequestParam(required = false) String libelle,
                                                        @RequestParam(required = false) String compte,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return ResponseEntity.ok(service.rechercher(refPiece, datePiece, montant, libelle, compte, du, au));
    }

    @PostMapping("/ecritures/{id}/contrepasser")
    @Operation(operationId = "comptaEcritureContrepasser", summary = "Contre-passe une écriture : seul moyen de la corriger")
    public ResponseEntity<EcritureDTO> contrepasser(@PathVariable Long id, @Valid @RequestBody ContrepassationRequest demande) {
        return ResponseEntity.ok(service.contrepasser(id, demande));
    }

    @PostMapping("/ecritures/{id}/pieces-jointes")
    @Operation(operationId = "comptaEcritureLierPiece", summary = "Lie un document de la GED à une écriture")
    public ResponseEntity<EcritureDTO> lierPiece(@PathVariable Long id, @Valid @RequestBody PieceJointeRequest demande) {
        return ResponseEntity.ok(service.lierPieceJointe(id, demande.documentId()));
    }
}

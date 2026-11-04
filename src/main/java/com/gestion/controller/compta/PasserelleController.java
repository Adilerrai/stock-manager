package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.CompteFinancierDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.CompteurSourceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.CorrespondanceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.EcarterRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.LotDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PageFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PropositionDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.VentilationRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.VerificationRequest;
import com.gestion.service.compta.CorrespondanceService;
import com.gestion.service.compta.FileComptabilisationService;
import com.gestion.service.compta.MoteurComptabilisation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta/passerelle", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : passerelle")
public class PasserelleController {

    private final FileComptabilisationService file;
    private final MoteurComptabilisation moteur;
    private final CorrespondanceService correspondances;

    public PasserelleController(FileComptabilisationService file, MoteurComptabilisation moteur, CorrespondanceService correspondances) {
        this.file = file;
        this.moteur = moteur;
        this.correspondances = correspondances;
    }

    @GetMapping("/compteurs")
    @Operation(operationId = "comptaPasserelleCompteurs", summary = "Combien de pièces par type et par état, sur un exercice")
    public ResponseEntity<List<CompteurSourceDTO>> compteurs(@RequestParam Long exerciceId) {
        return ResponseEntity.ok(file.compteurs(exerciceId));
    }

    @GetMapping("/file")
    @Operation(operationId = "comptaPasserelleFile", summary = "La file, calculée à la lecture : du et au bornent le calcul")
    public ResponseEntity<PageFileDTO> file(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                                            @RequestParam(required = false) String sourceType, @RequestParam(required = false) String etat, @RequestParam(required = false) String q,
                                            @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(file.file(du, au, sourceType, etat, q, curseur, taille));
    }

    @GetMapping("/pieces/{sourceType}/{sourceId}/proposition")
    @Operation(operationId = "comptaPasserelleProposition", summary = "Les lignes qui seront écrites : ce qui s'affiche est ce qui se poste")
    public ResponseEntity<PropositionDTO> proposition(@PathVariable String sourceType, @PathVariable Long sourceId, @RequestParam(defaultValue = "COMPTABILISATION") String evenement) {
        return ResponseEntity.ok(file.proposition(sourceType, sourceId, evenement));
    }

    @PostMapping(value = "/apercu", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleApercu", summary = "Sans rien écrire : les totaux par compte et les anomalies d'une sélection")
    public ResponseEntity<ApercuDTO> apercu(@Valid @RequestBody ApercuRequest demande) {
        return ResponseEntity.ok(file.apercu(demande));
    }

    @PostMapping(value = "/comptabiliser", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleComptabiliser", summary = "Passe les pièces au journal, chacune dans sa transaction ; réponse 200 même en cas d'échec partiel")
    public ResponseEntity<LotDTO> comptabiliser(@Valid @RequestBody ComptabiliserRequest demande) {
        return ResponseEntity.ok(moteur.comptabiliser(demande));
    }

    @GetMapping("/lots/{id}")
    @Operation(operationId = "comptaPasserelleLot", summary = "Le rapport d'un lot passé")
    public ResponseEntity<LotDTO> lot(@PathVariable Long id) {
        return ResponseEntity.ok(moteur.lot(id));
    }

    @PostMapping(value = "/pieces/{sourceType}/{sourceId}/ecarter", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleEcarter", summary = "Écarte une pièce, avec un motif")
    public ResponseEntity<Void> ecarter(@PathVariable String sourceType, @PathVariable Long sourceId, @Valid @RequestBody EcarterRequest demande) {
        moteur.ecarter(sourceType, sourceId, demande.motif());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pieces/{sourceType}/{sourceId}/ecarter")
    @Operation(operationId = "comptaPasserelleReprendre", summary = "Reprend une pièce écartée")
    public ResponseEntity<Void> reprendre(@PathVariable String sourceType, @PathVariable Long sourceId) {
        moteur.reprendre(sourceType, sourceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/pieces/{sourceType}/{sourceId}/ventilation", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleVentiler", summary = "Ce que la source ne porte pas : HT, TVA, compte, numéro de facture du fournisseur")
    public ResponseEntity<Void> ventiler(@PathVariable String sourceType, @PathVariable Long sourceId, @Valid @RequestBody VentilationRequest demande) {
        moteur.ventiler(sourceType, sourceId, demande);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/verifier-modifications", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleVerifier", summary = "Compare chaque lien à sa source : pièces modifiées, annulées ou disparues")
    public ResponseEntity<List<PieceFileDTO>> verifier(@Valid @RequestBody VerificationRequest demande) {
        return ResponseEntity.ok(file.verifier(demande.du(), demande.au()));
    }

    @GetMapping("/correspondances")
    @Operation(operationId = "comptaPasserelleCorrespondances", summary = "La table entre le commercial et le plan")
    public ResponseEntity<List<CorrespondanceDTO>> correspondances() {
        return ResponseEntity.ok(correspondances.lister());
    }

    @PutMapping(value = "/correspondances", consumes = "application/json")
    @Operation(operationId = "comptaPasserelleModifierCorrespondances", summary = "Modifie des lignes de la table ; un compte vide retire la ligne")
    public ResponseEntity<List<CorrespondanceDTO>> modifierCorrespondances(@RequestBody List<@Valid CorrespondanceDTO> lignes) {
        return ResponseEntity.ok(correspondances.modifier(lignes));
    }

    @GetMapping("/comptes-financiers")
    @Operation(operationId = "comptaPasserelleComptesFinanciers", summary = "Les comptes financiers des points de vente du livre")
    public ResponseEntity<List<CompteFinancierDTO>> comptesFinanciers() {
        return ResponseEntity.ok(correspondances.comptesFinanciers());
    }
}

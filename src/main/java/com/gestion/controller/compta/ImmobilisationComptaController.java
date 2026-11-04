package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ImmobilisationDtos.AnnulationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ComptesLiesDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationsRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationResumeDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.InventaireRequest;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneAEnregistrerDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieRequest;
import com.gestion.service.compta.ImmobilisationComptaService;
import com.gestion.service.compta.InventaireClotureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : immobilisations et inventaire")
public class ImmobilisationComptaController {

    private final ImmobilisationComptaService service;
    private final InventaireClotureService inventaire;

    public ImmobilisationComptaController(ImmobilisationComptaService service, InventaireClotureService inventaire) {
        this.service = service;
        this.inventaire = inventaire;
    }

    @GetMapping("/immobilisations")
    @Operation(operationId = "comptaImmoLister", summary = "Le registre, avec le cumul lu dans les dotations passées")
    public ResponseEntity<List<ImmobilisationResumeDTO>> lister() {
        return ResponseEntity.ok(service.lister());
    }

    @GetMapping("/immobilisations/comptes-lies")
    @Operation(operationId = "comptaImmoComptesLies", summary = "Les comptes d'amortissement et de dotation proposés")
    public ResponseEntity<ComptesLiesDTO> comptesLies(@RequestParam String compte) {
        return ResponseEntity.ok(service.comptesLies(compte));
    }

    @GetMapping("/immobilisations/a-enregistrer")
    @Operation(operationId = "comptaImmoAEnregistrer", summary = "Les lignes de classe 2 au journal qui n'ont pas encore de fiche")
    public ResponseEntity<List<LigneAEnregistrerDTO>> aEnregistrer() {
        return ResponseEntity.ok(service.aEnregistrer());
    }

    @PostMapping(value = "/immobilisations", consumes = "application/json")
    @Operation(operationId = "comptaImmoCreer", summary = "Crée une fiche : passe l'écriture d'acquisition, ou reprend une écriture déjà au journal")
    public ResponseEntity<ImmobilisationDTO> creer(@Valid @RequestBody ImmobilisationRequest demande) {
        return ResponseEntity.ok(service.creer(demande));
    }

    @GetMapping("/immobilisations/{id}")
    @Operation(operationId = "comptaImmoLire", summary = "La fiche, ses dotations passées et son plan prévisionnel")
    public ResponseEntity<ImmobilisationDTO> lire(@PathVariable Long id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PostMapping(value = "/immobilisations/{id}/sortie", consumes = "application/json")
    @Operation(operationId = "comptaImmoSortie", summary = "Cession ou retrait : complément de dotation, sortie, prix")
    public ResponseEntity<ImmobilisationDTO> sortie(@PathVariable Long id, @Valid @RequestBody SortieRequest demande) {
        return ResponseEntity.ok(service.sortir(id, demande));
    }

    @PostMapping(value = "/immobilisations/{id}/sortie/annuler", consumes = "application/json")
    @Operation(operationId = "comptaImmoSortieAnnuler", summary = "Annule une sortie erronée, par contre-passation")
    public ResponseEntity<ImmobilisationDTO> annulerSortie(@PathVariable Long id, @Valid @RequestBody AnnulationRequest demande) {
        return ResponseEntity.ok(service.annulerSortie(id, demande));
    }

    @PostMapping(value = "/exercices/{id}/dotations", consumes = "application/json")
    @Operation(operationId = "comptaDotations", summary = "Calcule les dotations de l'exercice, ou les passe en période 99")
    public ResponseEntity<DotationsDTO> dotations(@PathVariable Long id, @Valid @RequestBody DotationsRequest demande) {
        return ResponseEntity.ok(service.dotations(id, demande));
    }

    @PostMapping(value = "/exercices/{id}/dotations/annuler", consumes = "application/json")
    @Operation(operationId = "comptaDotationsAnnuler", summary = "Annule les dotations de l'exercice, par contre-passation")
    public ResponseEntity<DotationsDTO> annulerDotations(@PathVariable Long id, @Valid @RequestBody AnnulationRequest demande) {
        return ResponseEntity.ok(service.annulerDotations(id, demande));
    }

    @GetMapping("/exercices/{id}/inventaire")
    @Operation(operationId = "comptaInventaireLire", summary = "L'inventaire de clôture et le stock d'ouverture")
    public ResponseEntity<InventaireDTO> lireInventaire(@PathVariable Long id) {
        return ResponseEntity.ok(inventaire.lire(id));
    }

    @PutMapping(value = "/exercices/{id}/inventaire", consumes = "application/json")
    @Operation(operationId = "comptaInventaireEnregistrer", summary = "Enregistre le brouillon, ou la déclaration qu'il n'y a aucun stock")
    public ResponseEntity<InventaireDTO> enregistrerInventaire(@PathVariable Long id, @Valid @RequestBody InventaireRequest demande) {
        return ResponseEntity.ok(inventaire.enregistrer(id, demande));
    }

    @PostMapping("/exercices/{id}/inventaire/proposer")
    @Operation(operationId = "comptaInventaireProposer", summary = "Propose les lignes depuis les inventaires validés au commercial")
    public ResponseEntity<InventaireDTO> proposerInventaire(@PathVariable Long id) {
        return ResponseEntity.ok(inventaire.proposer(id));
    }

    @PostMapping("/exercices/{id}/inventaire/valider")
    @Operation(operationId = "comptaInventaireValider", summary = "Valide l'inventaire et passe la variation de stock en période 99")
    public ResponseEntity<InventaireDTO> validerInventaire(@PathVariable Long id) {
        return ResponseEntity.ok(inventaire.valider(id));
    }

    @PostMapping(value = "/exercices/{id}/inventaire/rouvrir", consumes = "application/json")
    @Operation(operationId = "comptaInventaireRouvrir", summary = "Rouvre l'inventaire, par contre-passation de ses écritures")
    public ResponseEntity<InventaireDTO> rouvrirInventaire(@PathVariable Long id, @Valid @RequestBody AnnulationRequest demande) {
        return ResponseEntity.ok(inventaire.rouvrir(id, demande));
    }
}

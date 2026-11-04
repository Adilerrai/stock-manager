package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpCalculRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDeclarationDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpDepotRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpTrimestreDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpValidationRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheancesDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.JourFerieDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisResumeDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasNatureRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasRetenueRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.TiersFiscalDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.TiersFormeRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.service.compta.DelaisComptaService;
import com.gestion.service.compta.EcheanceComptaService;
import com.gestion.service.compta.RasComptaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : délais, retenue à la source, échéancier")
public class DeclarationsComptaController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DelaisComptaService delais;
    private final RasComptaService ras;
    private final EcheanceComptaService echeances;

    public DeclarationsComptaController(DelaisComptaService delais, RasComptaService ras, EcheanceComptaService echeances) {
        this.delais = delais;
        this.ras = ras;
        this.echeances = echeances;
    }

    // --- Délais de paiement

    @GetMapping("/delais/{annee}")
    @Operation(operationId = "comptaDelaisTrimestres", summary = "Les quatre trimestres de l'année")
    public ResponseEntity<List<DpTrimestreDTO>> trimestres(@PathVariable int annee) {
        return ResponseEntity.ok(delais.trimestres(annee));
    }

    @GetMapping("/delais/{annee}/{trimestre}")
    @Operation(operationId = "comptaDelaisLire", summary = "La déclaration d'un trimestre")
    public ResponseEntity<DpDeclarationDTO> lireDelais(@PathVariable int annee, @PathVariable int trimestre) {
        return ResponseEntity.ok(delais.lire(annee, trimestre));
    }

    @PostMapping("/delais/{annee}/{trimestre}/calculer")
    @Operation(operationId = "comptaDelaisCalculer", summary = "Calcule le trimestre depuis les factures et les règlements fournisseurs")
    public ResponseEntity<DpDeclarationDTO> calculerDelais(@PathVariable int annee, @PathVariable int trimestre, @RequestBody DpCalculRequest r) {
        return ResponseEntity.ok(delais.calculer(annee, trimestre, r));
    }

    @PostMapping("/delais/{annee}/{trimestre}/valider")
    @Operation(operationId = "comptaDelaisValider", summary = "Valide et passe l'amende : D 65831 / C 4458")
    public ResponseEntity<DpDeclarationDTO> validerDelais(@PathVariable int annee, @PathVariable int trimestre, @Valid @RequestBody DpValidationRequest r) {
        return ResponseEntity.ok(delais.valider(annee, trimestre, r));
    }

    @PostMapping("/delais/{annee}/{trimestre}/rouvrir")
    @Operation(operationId = "comptaDelaisRouvrir", summary = "Rouvre et contre-passe l'amende")
    public ResponseEntity<DpDeclarationDTO> rouvrirDelais(@PathVariable int annee, @PathVariable int trimestre, @Valid @RequestBody MotifRequest r) {
        return ResponseEntity.ok(delais.rouvrir(annee, trimestre, r));
    }

    @PostMapping("/delais/{annee}/{trimestre}/depot")
    @Operation(operationId = "comptaDelaisDeposer", summary = "Enregistre le dépôt fait sur le portail")
    public ResponseEntity<DpDeclarationDTO> deposerDelais(@PathVariable int annee, @PathVariable int trimestre, @Valid @RequestBody DpDepotRequest r) {
        return ResponseEntity.ok(delais.deposer(annee, trimestre, r));
    }

    @GetMapping(value = "/delais/{annee}/{trimestre}/etat.xlsx", produces = XLSX)
    @Operation(operationId = "comptaDelaisEtatXlsx", summary = "L'état annexe, à viser",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> etatDelais(@PathVariable int annee, @PathVariable int trimestre) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"delais-" + annee + "-T" + trimestre + ".xlsx\"").contentType(MediaType.parseMediaType(XLSX)).body(delais.etatXlsx(annee, trimestre));
    }

    @GetMapping("/delais/factures/{id}")
    @Operation(operationId = "comptaDelaisFiche", summary = "Ce que la facture d'achat ne porte pas : livraison, délai convenu, litige")
    public ResponseEntity<DpFactureDTO> fiche(@PathVariable Long id) {
        return ResponseEntity.ok(delais.fiche(id));
    }

    @PutMapping("/delais/factures/{id}")
    @Operation(operationId = "comptaDelaisFicheModifier", summary = "Modifie la livraison, le délai convenu et le litige d'une facture")
    public ResponseEntity<DpFactureDTO> modifierFiche(@PathVariable Long id, @RequestBody DpFactureRequest r) {
        return ResponseEntity.ok(delais.modifierFiche(id, r));
    }

    // --- Retenue à la source

    @GetMapping("/ras/{annee}")
    @Operation(operationId = "comptaRasAnnee", summary = "Les douze mois de l'année")
    public ResponseEntity<List<RasMoisResumeDTO>> rasAnnee(@PathVariable int annee) {
        return ResponseEntity.ok(ras.annee(annee));
    }

    @GetMapping("/ras/{annee}/{mois}")
    @Operation(operationId = "comptaRasMois", summary = "Les règlements du mois et leur retenue")
    public ResponseEntity<RasMoisDTO> rasMois(@PathVariable int annee, @PathVariable int mois) {
        return ResponseEntity.ok(ras.mois(annee, mois));
    }

    @GetMapping(value = "/ras/{annee}/{mois}/bordereau.xlsx", produces = XLSX)
    @Operation(operationId = "comptaRasBordereauXlsx", summary = "Le bordereau du mois",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> bordereau(@PathVariable int annee, @PathVariable int mois) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ras-" + annee + "-" + mois + ".xlsx\"").contentType(MediaType.parseMediaType(XLSX)).body(ras.bordereauXlsx(annee, mois));
    }

    @PutMapping("/ras/factures/{id}/nature")
    @Operation(operationId = "comptaRasNature", summary = "La nature de l'achat : marchandise, bien d'équipement, travaux, service")
    public ResponseEntity<Void> nature(@PathVariable Long id, @Valid @RequestBody RasNatureRequest r) {
        ras.modifierNature(id, r.nature());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ras/reglements/{id}/retenue")
    @Operation(operationId = "comptaRasRetenir", summary = "Passe la retenue de ce règlement : D 4411 / C 4458")
    public ResponseEntity<RasMoisDTO> retenir(@PathVariable Long id, @Valid @RequestBody RasRetenueRequest r) {
        return ResponseEntity.ok(ras.retenir(id, r));
    }

    @PostMapping("/ras/operations/{id}/annuler")
    @Operation(operationId = "comptaRasAnnuler", summary = "Annule une retenue et contre-passe son écriture")
    public ResponseEntity<RasMoisDTO> annuler(@PathVariable Long id, @Valid @RequestBody MotifRequest r) {
        return ResponseEntity.ok(ras.annuler(id, r));
    }

    @GetMapping("/ras/tiers/{id}")
    @Operation(operationId = "comptaRasTiers", summary = "La forme du fournisseur et ses attestations")
    public ResponseEntity<TiersFiscalDTO> ficheTiers(@PathVariable Long id) {
        return ResponseEntity.ok(ras.ficheTiers(id));
    }

    @PutMapping("/ras/tiers/{id}/forme")
    @Operation(operationId = "comptaRasTiersForme", summary = "Personne physique ou morale")
    public ResponseEntity<TiersFiscalDTO> forme(@PathVariable Long id, @Valid @RequestBody TiersFormeRequest r) {
        return ResponseEntity.ok(ras.modifierForme(id, r.personnePhysique()));
    }

    @PostMapping("/ras/tiers/{id}/attestations")
    @Operation(operationId = "comptaRasAttestationAjouter", summary = "Enregistre une attestation de régularité fiscale")
    public ResponseEntity<TiersFiscalDTO> ajouterAttestation(@PathVariable Long id, @Valid @RequestBody AttestationRequest r) {
        return ResponseEntity.ok(ras.ajouterAttestation(id, r));
    }

    @DeleteMapping("/ras/tiers/{id}/attestations/{attestationId}")
    @Operation(operationId = "comptaRasAttestationRetirer", summary = "Retire une attestation")
    public ResponseEntity<TiersFiscalDTO> retirerAttestation(@PathVariable Long id, @PathVariable Long attestationId) {
        return ResponseEntity.ok(ras.retirerAttestation(id, attestationId));
    }

    // --- Échéancier

    @GetMapping("/echeances")
    @Operation(operationId = "comptaEcheancesLister", summary = "Les échéances calculées pour une année")
    public ResponseEntity<EcheancesDTO> echeances(@RequestParam int annee) {
        return ResponseEntity.ok(echeances.lister(annee));
    }

    @GetMapping("/echeances/prochaines")
    @Operation(operationId = "comptaEcheancesProchaines", summary = "Les prochaines échéances à faire")
    public ResponseEntity<List<EcheanceDTO>> prochaines(@RequestParam(defaultValue = "8") int nombre) {
        return ResponseEntity.ok(echeances.prochaines(nombre));
    }

    @PostMapping("/echeances/generer")
    @Operation(operationId = "comptaEcheancesGenerer", summary = "Calcule l'échéancier de l'année, selon la périodicité réelle")
    public ResponseEntity<EcheancesDTO> generer(@RequestParam int annee) {
        return ResponseEntity.ok(echeances.generer(annee));
    }

    @PutMapping("/echeances/{id}")
    @Operation(operationId = "comptaEcheanceMarquer", summary = "Marque une échéance faite ou sans objet")
    public ResponseEntity<EcheanceDTO> marquer(@PathVariable Long id, @Valid @RequestBody EcheanceRequest r) {
        return ResponseEntity.ok(echeances.marquer(id, r));
    }

    @GetMapping("/jours-feries")
    @Operation(operationId = "comptaJoursFeries", summary = "Les jours fériés légaux chargés. Vide tant que la liste n'a pas été lue")
    public ResponseEntity<List<JourFerieDTO>> joursFeries() {
        return ResponseEntity.ok(delais.joursFeries());
    }
}

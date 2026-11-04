package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.RapprochementDtos.ClassementRequest;
import com.gestion.persistent.dto.compta.RapprochementDtos.LigneClasseeDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterCodeRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaAccepterRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaControleDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCorrectionLigneRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaCreationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeclarationDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDepotRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEncaissementDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEtatFichierRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaFichierDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaIdentiteRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaOperationRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPaiementRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaPeriodeDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattrapageDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRattraperRequest;
import com.gestion.persistent.dto.compta.TvaDtos.TvaRubriqueDTO;
import com.gestion.service.compta.TvaDeclarationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Un refus métier répond 409 ou 422, jamais 401 ni 403 : le client web déconnecte l'utilisateur sur ces deux codes. */
@RestController
@RequestMapping(value = "/api/v1/compta/tva", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : TVA")
public class TvaController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ZIP = "application/zip";

    private final TvaDeclarationService service;

    public TvaController(TvaDeclarationService service) {
        this.service = service;
    }

    @GetMapping("/parametres/{annee}")
    @Operation(operationId = "comptaTvaParametres", summary = "Fait générateur, périodicité, prorata et crédit initial de l'année")
    public ResponseEntity<TvaParametresDTO> parametres(@PathVariable int annee) {
        return ResponseEntity.ok(service.parametres(annee));
    }

    @PutMapping(value = "/parametres/{annee}", consumes = "application/json")
    @Operation(operationId = "comptaTvaParametresEnregistrer", summary = "Enregistre les paramètres de l'année")
    public ResponseEntity<TvaParametresDTO> enregistrerParametres(@PathVariable int annee, @Valid @RequestBody TvaParametresRequest demande) {
        return ResponseEntity.ok(service.enregistrerParametres(annee, demande));
    }

    @GetMapping("/rubriques")
    @Operation(operationId = "comptaTvaRubriques", summary = "Les rubriques de déduction, lues sur le formulaire")
    public ResponseEntity<List<TvaRubriqueDTO>> rubriques() {
        return ResponseEntity.ok(service.rubriques());
    }

    @GetMapping("/declarations")
    @Operation(operationId = "comptaTvaPeriodes", summary = "Les périodes de l'année, avec ou sans déclaration")
    public ResponseEntity<List<TvaPeriodeDTO>> periodes(@RequestParam int annee) {
        return ResponseEntity.ok(service.periodes(annee));
    }

    @PostMapping(value = "/declarations", consumes = "application/json")
    @Operation(operationId = "comptaTvaCreer", summary = "Ouvre la déclaration d'une période et la calcule")
    public ResponseEntity<TvaDeclarationDTO> creer(@Valid @RequestBody TvaCreationRequest demande) {
        return ResponseEntity.ok(service.creer(demande));
    }

    @GetMapping("/declarations/{id}")
    @Operation(operationId = "comptaTvaLire", summary = "En-tête et lignes du formulaire")
    public ResponseEntity<TvaDeclarationDTO> lire(@PathVariable Long id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PostMapping("/declarations/{id}/calculer")
    @Operation(operationId = "comptaTvaCalculer", summary = "Recalcule le brouillon depuis les paiements")
    public ResponseEntity<TvaDeclarationDTO> calculer(@PathVariable Long id) {
        return ResponseEntity.ok(service.calculer(id));
    }

    @GetMapping("/declarations/{id}/encaissements")
    @Operation(operationId = "comptaTvaEncaissements", summary = "Les opérations de TVA collectée")
    public ResponseEntity<List<TvaEncaissementDTO>> encaissements(@PathVariable Long id) {
        return ResponseEntity.ok(service.encaissements(id));
    }

    @GetMapping("/declarations/{id}/deductions")
    @Operation(operationId = "comptaTvaDeductions", summary = "Les opérations de TVA déductible")
    public ResponseEntity<List<TvaDeductionDTO>> deductions(@PathVariable Long id) {
        return ResponseEntity.ok(service.deductions(id));
    }

    @PatchMapping(value = "/operations/{type}/{id}", consumes = "application/json")
    @Operation(operationId = "comptaTvaOperationModifier", summary = "Corrige une opération, avec un motif : la correction survit au recalcul")
    public ResponseEntity<TvaDeclarationDTO> modifierOperation(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody TvaOperationRequest demande) {
        return ResponseEntity.ok(service.modifierOperation(type, id, demande));
    }

    @PutMapping(value = "/declarations/{id}/lignes", consumes = "application/json")
    @Operation(operationId = "comptaTvaLigneCorriger", summary = "Saisit ou corrige une ligne du formulaire, avec un motif ; sans valeur, la correction se retire")
    public ResponseEntity<TvaDeclarationDTO> corrigerLigne(@PathVariable Long id, @Valid @RequestBody TvaCorrectionLigneRequest demande) {
        return ResponseEntity.ok(service.corrigerLigne(id, demande));
    }

    @PutMapping(value = "/declarations/{id}/identite", consumes = "application/json")
    @Operation(operationId = "comptaTvaIdentite", summary = "Corrige l'IF et l'ICE du dossier, avec un motif")
    public ResponseEntity<TvaDeclarationDTO> corrigerIdentite(@PathVariable Long id, @Valid @RequestBody TvaIdentiteRequest demande) {
        return ResponseEntity.ok(service.corrigerIdentite(id, demande));
    }

    @GetMapping("/declarations/{id}/a-rattraper")
    @Operation(operationId = "comptaTvaARattraper", summary = "Les paiements passés que personne n'a déclarés")
    public ResponseEntity<List<TvaRattrapageDTO>> aRattraper(@PathVariable Long id) {
        return ResponseEntity.ok(service.aRattraper(id));
    }

    @PostMapping(value = "/declarations/{id}/rattraper", consumes = "application/json")
    @Operation(operationId = "comptaTvaRattraper", summary = "Ajoute ces paiements à la déclaration")
    public ResponseEntity<TvaDeclarationDTO> rattraper(@PathVariable Long id, @Valid @RequestBody TvaRattraperRequest demande) {
        return ResponseEntity.ok(service.rattraper(id, demande));
    }

    @GetMapping("/declarations/{id}/pointage")
    @Operation(operationId = "comptaTvaPointage", summary = "Le relevé de la période : pointage et classement de chaque ligne")
    public ResponseEntity<List<LigneClasseeDTO>> pointage(@PathVariable Long id) {
        return ResponseEntity.ok(service.pointage(id));
    }

    @PutMapping(value = "/releve-lignes/{id}/classement", consumes = "application/json")
    @Operation(operationId = "comptaTvaClasser", summary = "Classe une ligne de relevé : rubrique, hors champ, non taxable, ou AUCUN pour retirer")
    public ResponseEntity<LigneClasseeDTO> classer(@PathVariable Long id, @Valid @RequestBody ClassementRequest demande) {
        return ResponseEntity.ok(service.classer(id, demande));
    }

    @GetMapping("/declarations/{id}/controles")
    @Operation(operationId = "comptaTvaControles", summary = "Les contrôles de la déclaration")
    public ResponseEntity<List<TvaControleDTO>> controles(@PathVariable Long id) {
        return ResponseEntity.ok(service.listeControles(id));
    }

    @PostMapping(value = "/controles/{id}/accepter", consumes = "application/json")
    @Operation(operationId = "comptaTvaControleAccepter", summary = "Accepte une alerte avec un motif")
    public ResponseEntity<TvaControleDTO> accepter(@PathVariable Long id, @Valid @RequestBody TvaAccepterRequest demande) {
        return ResponseEntity.ok(service.accepter(id, demande));
    }

    @PostMapping(value = "/declarations/{id}/controles/accepter", consumes = "application/json")
    @Operation(operationId = "comptaTvaControlesAccepterCode", summary = "Accepte toutes les alertes ouvertes d'un code, avec le même motif")
    public ResponseEntity<List<TvaControleDTO>> accepterCode(@PathVariable Long id, @Valid @RequestBody TvaAccepterCodeRequest demande) {
        return ResponseEntity.ok(service.accepterCode(id, demande));
    }

    @PostMapping("/declarations/{id}/valider")
    @Operation(operationId = "comptaTvaValider", summary = "Fige la déclaration et produit le relevé")
    public ResponseEntity<TvaDeclarationDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(service.valider(id));
    }

    @PostMapping("/declarations/{id}/devalider")
    @Operation(operationId = "comptaTvaDevalider", summary = "Rouvre une déclaration validée, ou rejetée par la DGI")
    public ResponseEntity<TvaDeclarationDTO> devalider(@PathVariable Long id) {
        return ResponseEntity.ok(service.devalider(id));
    }

    @GetMapping("/declarations/{id}/fichiers")
    @Operation(operationId = "comptaTvaFichiers", summary = "Les fichiers produits et leur état DGI recopié")
    public ResponseEntity<List<TvaFichierDTO>> fichiers(@PathVariable Long id) {
        return ResponseEntity.ok(service.listeFichiers(id));
    }

    @GetMapping(value = "/declarations/{id}/fichiers/{type}", produces = ZIP)
    @Operation(operationId = "comptaTvaFichier", summary = "Le ZIP à déposer",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = ZIP, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> fichier(@PathVariable Long id, @PathVariable String type) {
        Object[] f = service.fichier(id, type);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f[0] + "\"").contentType(MediaType.parseMediaType(ZIP)).body((byte[]) f[1]);
    }

    @PutMapping(value = "/declarations/{id}/fichiers/{type}/etat", consumes = "application/json")
    @Operation(operationId = "comptaTvaFichierEtat", summary = "Recopie l'état lu dans le suivi de la DGI")
    public ResponseEntity<TvaDeclarationDTO> etatFichier(@PathVariable Long id, @PathVariable String type, @Valid @RequestBody TvaEtatFichierRequest demande) {
        return ResponseEntity.ok(service.etatFichier(id, type, demande));
    }

    @PostMapping(value = "/declarations/{id}/depot", consumes = "application/json")
    @Operation(operationId = "comptaTvaDepot", summary = "Enregistre le dépôt et passe l'écriture de liquidation")
    public ResponseEntity<TvaDeclarationDTO> depot(@PathVariable Long id, @Valid @RequestBody TvaDepotRequest demande) {
        return ResponseEntity.ok(service.depot(id, demande));
    }

    @PostMapping(value = "/declarations/{id}/paiement", consumes = "application/json")
    @Operation(operationId = "comptaTvaPaiement", summary = "Enregistre le paiement et passe son écriture")
    public ResponseEntity<TvaDeclarationDTO> paiement(@PathVariable Long id, @Valid @RequestBody TvaPaiementRequest demande) {
        return ResponseEntity.ok(service.paiement(id, demande));
    }

    @GetMapping(value = "/declarations/{id}/exports/fiche", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(operationId = "comptaTvaExportFiche", summary = "La fiche de saisie en PDF",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> fiche(@PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fiche-tva-" + id + ".pdf\"").contentType(MediaType.APPLICATION_PDF).body(service.fiche(id));
    }

    @GetMapping(value = "/declarations/{id}/exports/releve", produces = XLSX)
    @Operation(operationId = "comptaTvaExportReleve", summary = "Le relevé en Excel",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> releve(@PathVariable Long id) {
        return excel("releve-tva-" + id, service.releveXlsx(id));
    }

    @GetMapping(value = "/declarations/{id}/exports/couverture", produces = XLSX)
    @Operation(operationId = "comptaTvaExportCouverture", summary = "La couverture en Excel",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> couverture(@PathVariable Long id) {
        return excel("couverture-tva-" + id, service.couverture(id));
    }

    @GetMapping(value = "/declarations/{id}/exports/detail", produces = XLSX)
    @Operation(operationId = "comptaTvaExportDetail", summary = "Le détail des encaissements et de la récupération en Excel",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> detail(@PathVariable Long id) {
        return excel("detail-tva-" + id, service.detail(id));
    }

    private static ResponseEntity<byte[]> excel(String nom, byte[] contenu) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + ".xlsx\"").contentType(MediaType.parseMediaType(XLSX)).body(contenu);
    }
}

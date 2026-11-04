package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.FiscalDtos.AcomptesDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.DepotLiasseRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.IsDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LiasseDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LiquidationIsRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.MotifRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.PaiementAcompteRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.RegistreRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.SaisiesRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.TableauDTO;
import com.gestion.service.compta.IsComptaService;
import com.gestion.service.compta.LiasseComptaService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : IS et liasse")
public class FiscalComptaController {

    private final IsComptaService is;
    private final LiasseComptaService liasse;

    public FiscalComptaController(IsComptaService is, LiasseComptaService liasse) {
        this.is = is;
        this.liasse = liasse;
    }

    @GetMapping("/exercices/{id}/is")
    @Operation(operationId = "comptaIsLire", summary = "L'IS de l'exercice, avec les taux et leur source")
    public ResponseEntity<IsDTO> lire(@PathVariable Long id) {
        return ResponseEntity.ok(is.lire(id));
    }

    @PostMapping("/exercices/{id}/is/calculer")
    @Operation(operationId = "comptaIsCalculer", summary = "Calcule l'IS depuis le résultat et le tableau T3")
    public ResponseEntity<IsDTO> calculer(@PathVariable Long id) {
        return ResponseEntity.ok(is.calculer(id));
    }

    @PostMapping("/exercices/{id}/is/valider")
    @Operation(operationId = "comptaIsValider", summary = "Passe la charge d'impôt en période 99")
    public ResponseEntity<IsDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(is.valider(id));
    }

    @PostMapping(value = "/exercices/{id}/is/rouvrir", consumes = "application/json")
    @Operation(operationId = "comptaIsRouvrir", summary = "Rouvre le calcul, par contre-passation de la charge")
    public ResponseEntity<IsDTO> rouvrir(@PathVariable Long id, @Valid @RequestBody MotifRequest demande) {
        return ResponseEntity.ok(is.rouvrir(id, demande));
    }

    @PostMapping(value = "/exercices/{id}/is/liquidation", consumes = "application/json")
    @Operation(operationId = "comptaIsLiquider", summary = "Solde l'impôt : acomptes imputés et reliquat payé")
    public ResponseEntity<IsDTO> liquider(@PathVariable Long id, @Valid @RequestBody LiquidationIsRequest demande) {
        return ResponseEntity.ok(is.liquider(id, demande));
    }

    @GetMapping("/exercices/{id}/is/acomptes")
    @Operation(operationId = "comptaIsAcomptes", summary = "Les quatre acomptes, depuis le bénéfice de l'exercice clos")
    public ResponseEntity<AcomptesDTO> acomptes(@PathVariable Long id) {
        return ResponseEntity.ok(is.acomptes(id));
    }

    @PostMapping(value = "/exercices/{id}/is/acomptes/{numero}/paiement", consumes = "application/json")
    @Operation(operationId = "comptaIsAcomptePayer", summary = "Enregistre le paiement d'un acompte et passe son écriture")
    public ResponseEntity<AcomptesDTO> payer(@PathVariable Long id, @PathVariable int numero, @Valid @RequestBody PaiementAcompteRequest demande) {
        return ResponseEntity.ok(is.payerAcompte(id, numero, demande));
    }

    @GetMapping("/is/deficits")
    @Operation(operationId = "comptaIsDeficits", summary = "Les déficits reportables, par origine et par nature")
    public ResponseEntity<List<DeficitDTO>> deficits() {
        return ResponseEntity.ok(is.deficits());
    }

    @PostMapping(value = "/is/deficits", consumes = "application/json")
    @Operation(operationId = "comptaIsDeficitSaisir", summary = "Reprend un déficit antérieur au démarrage")
    public ResponseEntity<List<DeficitDTO>> saisirDeficit(@Valid @RequestBody DeficitRequest demande) {
        return ResponseEntity.ok(is.saisirDeficit(demande));
    }

    @GetMapping("/exercices/{id}/liasse")
    @Operation(operationId = "comptaLiasseLire", summary = "Les tableaux de la liasse et le statut de chacun")
    public ResponseEntity<LiasseDTO> liasse(@PathVariable Long id) {
        return ResponseEntity.ok(liasse.liste(id));
    }

    @GetMapping("/exercices/{id}/liasse/{tableau}")
    @Operation(operationId = "comptaLiasseTableau", summary = "Un tableau : valeurs, cellules à saisir, registre, cadrages")
    public ResponseEntity<TableauDTO> tableau(@PathVariable Long id, @PathVariable String tableau) {
        return ResponseEntity.ok(liasse.tableau(id, tableau));
    }

    @PutMapping(value = "/exercices/{id}/liasse/{tableau}/saisies", consumes = "application/json")
    @Operation(operationId = "comptaLiasseSaisir", summary = "Enregistre les cellules saisies ; une cellule vide se retire")
    public ResponseEntity<TableauDTO> saisir(@PathVariable Long id, @PathVariable String tableau, @Valid @RequestBody SaisiesRequest demande) {
        return ResponseEntity.ok(liasse.saisir(id, tableau, demande));
    }

    @PutMapping(value = "/exercices/{id}/liasse/{tableau}/registre", consumes = "application/json")
    @Operation(operationId = "comptaLiasseRegistre", summary = "Remplace le registre d'un tableau")
    public ResponseEntity<TableauDTO> registre(@PathVariable Long id, @PathVariable String tableau, @Valid @RequestBody RegistreRequest demande) {
        return ResponseEntity.ok(liasse.enregistrerRegistre(id, tableau, demande));
    }

    @PostMapping(value = "/exercices/{id}/liasse/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(operationId = "comptaLiasseXml", summary = "Produit le XML, marqué non validé",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> xml(@PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liasse-NON-VALIDE-" + id + ".xml\"").contentType(MediaType.APPLICATION_XML).body(liasse.produireXml(id));
    }

    @PostMapping(value = "/exercices/{id}/liasse/depot", consumes = "application/json")
    @Operation(operationId = "comptaLiasseDepot", summary = "Enregistre le dépôt de la liasse")
    public ResponseEntity<LiasseDTO> deposer(@PathVariable Long id, @Valid @RequestBody DepotLiasseRequest demande) {
        return ResponseEntity.ok(liasse.deposer(id, demande));
    }
}

package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.EtatDtos.DetailCelluleDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RapportControlesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseSoldesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseSoldesRequest;
import com.gestion.service.compta.ControleService;
import com.gestion.service.compta.EtatService;
import com.gestion.service.compta.FecService;
import com.gestion.service.compta.ParametreFiscalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : états et contrôles")
public class EtatComptaController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final EtatService etats;
    private final ControleService controles;
    private final FecService fec;
    private final ParametreFiscalService parametres;

    public EtatComptaController(EtatService etats, ControleService controles, FecService fec, ParametreFiscalService parametres) {
        this.etats = etats;
        this.controles = controles;
        this.fec = fec;
        this.parametres = parametres;
    }

    @GetMapping("/exercices/{id}/etats/{code}")
    @Operation(operationId = "comptaEtatLire", summary = "Un état de synthèse, calculé à la date demandée")
    public ResponseEntity<EtatDTO> etat(@PathVariable Long id, @PathVariable String code,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok(etats.etat(id, code, arrete));
    }

    @GetMapping("/exercices/{id}/etats/{code}/lignes/{ligne}/detail")
    @Operation(operationId = "comptaEtatDetail", summary = "Les comptes qui composent une cellule")
    public ResponseEntity<DetailCelluleDTO> detail(@PathVariable Long id, @PathVariable String code, @PathVariable String ligne, @RequestParam String colonne,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok(etats.detail(id, code, ligne, colonne, arrete));
    }

    @GetMapping(value = "/exercices/{id}/etats/{code}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(operationId = "comptaEtatPdf", summary = "L'état en PDF, depuis le même calcul",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @PathVariable String code,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(etats.pdf(id, code, arrete));
    }

    @GetMapping(value = "/exercices/{id}/etats/{code}.xlsx", produces = XLSX)
    @Operation(operationId = "comptaEtatXlsx", summary = "L'état en Excel, depuis le même calcul",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> xlsx(@PathVariable Long id, @PathVariable String code,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX)).body(etats.xlsx(id, code, arrete));
    }

    @GetMapping("/exercices/{id}/controles")
    @Operation(operationId = "comptaControleDernier", summary = "La dernière exécution des contrôles ; ce qui n'a pas tourné reste non effectué")
    public ResponseEntity<RapportControlesDTO> dernier(@PathVariable Long id) {
        return ResponseEntity.ok(controles.dernier(id));
    }

    @PostMapping("/exercices/{id}/controles")
    @Operation(operationId = "comptaControleRejouer", summary = "Rejoue les contrôles et garde la trace de l'exécution")
    public ResponseEntity<RapportControlesDTO> rejouer(@PathVariable Long id) {
        return ResponseEntity.ok(controles.rejouer(id));
    }

    @GetMapping(value = "/exercices/{id}/fec", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(operationId = "comptaFec", summary = "Le FEC de l'exercice, en flux",
            responses = @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", format = "binary"))))
    public void fec(@PathVariable Long id, @RequestParam(defaultValue = "|") String separateur, @RequestParam(defaultValue = "UTF-8") String encodage,
                    @RequestParam(defaultValue = "yyyyMMdd") String formatDate, HttpServletResponse reponse) throws IOException {
        // Sur le fil de la requête, écrit directement : rien n'est gardé en mémoire et le contexte de la requête reste là pendant tout le flux.
        FecService.ExportFec export = fec.preparer(id, new FecService.OptionsFec(separateur, encodage, formatDate));
        reponse.setContentType("text/plain; charset=" + encodage);
        reponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.nomFichier() + "\"");
        fec.ecrire(export, reponse.getOutputStream());
    }

    @GetMapping("/exercices/{id}/reprise")
    @Operation(operationId = "comptaRepriseLire", summary = "La balance reprise de l'exercice précédent, s'il est absent de l'application")
    public ResponseEntity<RepriseSoldesDTO> reprise(@PathVariable Long id) {
        return ResponseEntity.ok(etats.reprise(id));
    }

    @PutMapping(value = "/exercices/{id}/reprise", consumes = "application/json")
    @Operation(operationId = "comptaRepriseEnregistrer", summary = "Remplace la balance reprise, équilibrée")
    public ResponseEntity<RepriseSoldesDTO> enregistrerReprise(@PathVariable Long id, @Valid @RequestBody RepriseSoldesRequest demande) {
        return ResponseEntity.ok(etats.enregistrerReprise(id, demande));
    }

    @GetMapping("/parametres-fiscaux")
    @Operation(operationId = "comptaParametresFiscaux", summary = "Taux et seuils datés, avec leur source")
    public ResponseEntity<List<ParametreFiscalDTO>> parametresFiscaux(@RequestParam(required = false) String code) {
        return ResponseEntity.ok(parametres.lister(code));
    }
}

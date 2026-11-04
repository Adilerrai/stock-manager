package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.CompteCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ComptePatchRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.PageCompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TitrePlanDTO;
import com.gestion.service.compta.PlanComptableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("comptaCompteController")
@RequestMapping(value = "/api/v1/compta/comptes", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : plan comptable")
public class CompteController {

    private final PlanComptableService service;

    public CompteController(PlanComptableService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "comptaCompteLister", summary = "Comptes du livre, par page")
    public ResponseEntity<PageCompteDTO> lister(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer classe,
                                                @RequestParam(required = false) Boolean imputables,
                                                @RequestParam(required = false) String curseur,
                                                @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.lister(q, classe, imputables, curseur, taille));
    }

    @GetMapping("/titres")
    @Operation(operationId = "comptaCompteTitres", summary = "Rubriques et postes du plan")
    public ResponseEntity<List<TitrePlanDTO>> titres() {
        return ResponseEntity.ok(service.titres());
    }

    @GetMapping("/{numero}")
    @Operation(operationId = "comptaCompteLire", summary = "Un compte")
    public ResponseEntity<CompteDTO> lire(@PathVariable String numero) {
        return ResponseEntity.ok(service.lire(numero));
    }

    @PostMapping
    @Operation(operationId = "comptaCompteCreer", summary = "Crée un sous-compte")
    public ResponseEntity<CompteDTO> creer(@Valid @RequestBody CompteCreationRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @PatchMapping("/{numero}")
    @Operation(operationId = "comptaCompteModifier", summary = "Modifie un compte")
    public ResponseEntity<CompteDTO> modifier(@PathVariable String numero, @RequestBody ComptePatchRequest demande) {
        return ResponseEntity.ok(service.modifier(numero, demande));
    }

    @DeleteMapping("/{numero}")
    @Operation(operationId = "comptaCompteSupprimer", summary = "Supprime un sous-compte sans mouvement")
    public ResponseEntity<Void> supprimer(@PathVariable String numero) {
        service.supprimer(numero);
        return ResponseEntity.noContent().build();
    }
}

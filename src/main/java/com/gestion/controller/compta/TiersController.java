package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.PageTiersDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersPatchRequest;
import com.gestion.service.compta.TiersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/compta/tiers", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : tiers")
public class TiersController {

    private final TiersService service;

    public TiersController(TiersService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "comptaTiersLister", summary = "Tiers du livre, par page")
    public ResponseEntity<PageTiersDTO> lister(@RequestParam(required = false) String type,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(required = false) String curseur,
                                               @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.lister(type, q, curseur, taille));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "comptaTiersLire", summary = "Un tiers")
    public ResponseEntity<TiersDTO> lire(@PathVariable Long id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PostMapping
    @Operation(operationId = "comptaTiersCreer", summary = "Crée un tiers hors commercial")
    public ResponseEntity<TiersDTO> creer(@Valid @RequestBody TiersCreationRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "comptaTiersModifier", summary = "Modifie un tiers")
    public ResponseEntity<TiersDTO> modifier(@PathVariable Long id, @RequestBody TiersPatchRequest demande) {
        return ResponseEntity.ok(service.modifier(id, demande));
    }

    @PostMapping("/{id}/synchroniser")
    @Operation(operationId = "comptaTiersSynchroniser", summary = "Relit nom, ICE et IF dans le commercial")
    public ResponseEntity<TiersDTO> synchroniser(@PathVariable Long id) {
        return ResponseEntity.ok(service.synchroniser(id));
    }
}

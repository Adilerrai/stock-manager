package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.RattachementPointDeVenteRequest;
import com.gestion.service.compta.LivreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/compta/livre", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : livre")
public class LivreController {

    private final LivreService service;

    public LivreController(LivreService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "comptaLivreEtat", summary = "État du module pour le dossier courant : répond toujours 200 et n'écrit rien")
    public ResponseEntity<LivreDTO> etat() {
        return ResponseEntity.ok(service.etat());
    }

    @GetMapping("/identite-proposee")
    @Operation(operationId = "comptaLivreIdentiteProposee", summary = "Identité reprise du profil ou du dossier, pour préremplir l'activation")
    public ResponseEntity<IdentiteLivreDTO> identiteProposee() {
        return ResponseEntity.ok(service.identiteProposee());
    }

    @PostMapping("/activation")
    @Operation(operationId = "comptaLivreActiver", summary = "Ouvre le livre : plan, journaux, exercice et périodes")
    public ResponseEntity<LivreDTO> activer(@Valid @RequestBody ActivationLivreRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.activer(demande));
    }

    @PatchMapping
    @Operation(operationId = "comptaLivreModifierIdentite", summary = "Corrige l'identité du livre")
    public ResponseEntity<LivreDTO> modifierIdentite(@Valid @RequestBody IdentiteLivreDTO identite) {
        return ResponseEntity.ok(service.modifierIdentite(identite));
    }

    @PostMapping("/points-de-vente")
    @Operation(operationId = "comptaLivreRattacher", summary = "Rattache un autre point de vente au livre")
    public ResponseEntity<LivreDTO> rattacher(@Valid @RequestBody RattachementPointDeVenteRequest demande) {
        return ResponseEntity.ok(service.rattacher(demande));
    }
}

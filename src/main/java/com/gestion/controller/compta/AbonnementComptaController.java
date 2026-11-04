package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import com.gestion.persistent.dto.compta.LivreDtos.AbonnementRequest;
import com.gestion.service.compta.AbonnementComptaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/compta/abonnements", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : abonnement")
@PreAuthorize("hasAuthority('ROLE_SUPERADMIN')")
public class AbonnementComptaController {

    private final AbonnementComptaService service;

    public AbonnementComptaController(AbonnementComptaService service) {
        this.service = service;
    }

    @GetMapping("/{pointDeVenteId}")
    @Operation(operationId = "comptaAbonnementLire", summary = "Abonnement d'un point de vente")
    public ResponseEntity<AbonnementDTO> lire(@PathVariable Long pointDeVenteId) {
        return ResponseEntity.ok(service.lire(pointDeVenteId));
    }

    @PutMapping("/{pointDeVenteId}")
    @Operation(operationId = "comptaAbonnementModifier", summary = "Ouvre ou ferme le module pour un point de vente")
    public ResponseEntity<AbonnementDTO> modifier(@PathVariable Long pointDeVenteId, @Valid @RequestBody AbonnementRequest demande) {
        return ResponseEntity.ok(service.modifier(pointDeVenteId, demande.actif()));
    }
}

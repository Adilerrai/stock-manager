package com.gestion.controller;

import com.gestion.persistent.dto.DepenseDTO;
import com.gestion.persistent.dto.ResultatEntrepriseDTO;
import com.gestion.service.DepenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/depenses")
@CrossOrigin(origins = "*")
public class DepenseController {

    private final DepenseService depenseService;

    public DepenseController(DepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<DepenseDTO> creerDepense(@RequestBody DepenseDTO dto,
                                                   @RequestParam(required = false) Long userId) {
        DepenseDTO cree = depenseService.creerDepense(dto, userId);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<DepenseDTO>> getDepenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (dateDebut != null || dateFin != null) {
            return ResponseEntity.ok(depenseService.getDepensesByPeriode(dateDebut, dateFin));
        }
        return ResponseEntity.ok(depenseService.getAllDepenses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<DepenseDTO> getDepense(@PathVariable Long id) {
        return ResponseEntity.ok(depenseService.getDepenseById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE')")
    public ResponseEntity<Void> supprimerDepense(@PathVariable Long id) {
        depenseService.supprimerDepense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<Map<String, BigDecimal>> getStatistiques(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(depenseService.getStatistiquesParCategorie(dateDebut, dateFin));
    }

    /**
     * Calcule le résultat d'exploitation net réel de l'entreprise :
     * Résultat Net = Marge Commerciale Nette - Total Dépenses
     */
    @GetMapping("/resultat-net")
    @PreAuthorize("hasAnyAuthority('MARGES_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<ResultatEntrepriseDTO> getResultatNet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        ResultatEntrepriseDTO resultat = depenseService.calculerResultatNet(dateDebut, dateFin);
        return ResponseEntity.ok(resultat);
    }
}

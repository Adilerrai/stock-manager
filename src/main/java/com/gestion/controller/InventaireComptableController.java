package com.gestion.controller;

import com.gestion.persistent.dto.DemandeRegularisationDTO;
import com.gestion.persistent.dto.RegularisationResultDTO;
import com.gestion.service.InventaireComptableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/comptabilite/inventaire")
@CrossOrigin(origins = "*")
public class InventaireComptableController {

    private final InventaireComptableService inventaireService;

    public InventaireComptableController(InventaireComptableService inventaireService) {
        this.inventaireService = inventaireService;
    }

    @PostMapping("/cca")
    public ResponseEntity<RegularisationResultDTO> creerCCA(@RequestBody DemandeRegularisationDTO dto) {
        return new ResponseEntity<>(inventaireService.creerCCA(dto), HttpStatus.CREATED);
    }

    @PostMapping("/pca")
    public ResponseEntity<RegularisationResultDTO> creerPCA(@RequestBody DemandeRegularisationDTO dto) {
        return new ResponseEntity<>(inventaireService.creerPCA(dto), HttpStatus.CREATED);
    }

    @PostMapping("/provisions-clients")
    public ResponseEntity<RegularisationResultDTO> creerProvisionClient(
            @RequestParam Long clientId,
            @RequestParam BigDecimal montantCreance,
            @RequestParam(required = false, defaultValue = "100") BigDecimal pourcentageProvision,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateInventaire,
            @RequestParam(required = false) String motif) {

        return new ResponseEntity<>(
                inventaireService.creerProvisionClientDouteux(clientId, montantCreance, pourcentageProvision, dateInventaire, motif),
                HttpStatus.CREATED
        );
    }
}

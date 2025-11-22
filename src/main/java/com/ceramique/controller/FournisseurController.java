package com.ceramique.controller;

import com.ceramique.mapper.FournisseurMapper;
import com.ceramique.persistent.dto.FournisseurDTO;
import com.ceramique.persistent.dto.FournisseurSearchCriteria;
import com.ceramique.persistent.model.Fournisseur;
import com.ceramique.service.FournisseurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fournisseurs")
public class FournisseurController {

    private final FournisseurService fournisseurService;
    private final FournisseurMapper fournisseurMapper;

    public FournisseurController(FournisseurService fournisseurService,
                                 FournisseurMapper fournisseurMapper) {
        this.fournisseurService = fournisseurService;
        this.fournisseurMapper = fournisseurMapper;
    }

    @PostMapping("/create")
    public ResponseEntity<FournisseurDTO> createFournisseur(@RequestBody FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = fournisseurService.createFournisseur(fournisseurDTO);
        return ResponseEntity.ok(fournisseurMapper.toDto(fournisseur));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FournisseurDTO>> getAllFournisseurs() {
        List<Fournisseur> fournisseurs = fournisseurService.getAllFournisseursActifs();
        List<FournisseurDTO> fournisseurDTOs = fournisseurs.stream()
                .map(fournisseurMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fournisseurDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FournisseurDTO> getFournisseurById(@PathVariable("id") Long id) {
        Fournisseur fournisseur = fournisseurService.getFournisseurById(id);
        return ResponseEntity.ok(fournisseurMapper.toDto(fournisseur));
    }

    @PutMapping("/update")
    public ResponseEntity<FournisseurDTO> updateFournisseur(
                                                           @RequestBody FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = fournisseurService.updateFournisseur( fournisseurDTO);
        return ResponseEntity.ok(fournisseurMapper.toDto(fournisseur));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFournisseur(@PathVariable("id") Long id) {
        fournisseurService.deleteFournisseur(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
  public ResponseEntity<Page<FournisseurDTO>> searchFournisseurs(@RequestBody FournisseurSearchCriteria criteria, Pageable pageable) {
        Page<Fournisseur> fournisseursPage = fournisseurService.searchFournisseurs(criteria, pageable);
        Page<FournisseurDTO> fournisseurDTOsPage = fournisseursPage.map(fournisseurMapper::toDto);
        return ResponseEntity.ok(fournisseurDTOsPage);
    }

}
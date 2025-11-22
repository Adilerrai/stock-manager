package com.ceramique.controller;

import com.ceramique.persistent.dto.ProduitDTO;
import com.ceramique.mapper.ProduitMapper;
import com.ceramique.persistent.dto.ProduitSearchCriteria;
import com.ceramique.persistent.model.Produit;
import com.ceramique.service.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;

    @Autowired
    private ProduitMapper produitMapper;

    @PostMapping("/add")
    public ProduitDTO createProduit(@RequestBody ProduitDTO produitDTO) {
        Produit produit = produitMapper.toEntity(produitDTO);
        Produit savedProduit = produitService.createProduit(produit);
        return produitMapper.toDto(savedProduit);
    }



    @GetMapping("/get/{id}")
    public ProduitDTO getProduitById(@PathVariable("id") Long id) {
        try {
            Produit produit = produitService.getProduitWithImageById(id);
            ProduitDTO dto = produitMapper.toDto(produit);
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/update")
    public ProduitDTO updateProduit(@RequestBody ProduitDTO produitDTO) {

        Produit updatedProduit = produitService.updateProduit(produitDTO);
        return produitMapper.toDto(updatedProduit);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/all")
    public List<ProduitDTO> getAllProduits() {
        try {
            List<Produit> produits = produitService.getAllProduits();

            List<ProduitDTO> result = produits.stream()
                    .map(produit -> {
                        try {
                            ProduitDTO dto = produitMapper.toDto(produit);
                            return dto;
                        } catch (Exception e) {
                            throw new RuntimeException("Erreur mapping produit " + produit.getId(), e);
                        }
                    })
                    .collect(Collectors.toList());
                    
            System.out.println("=== Fin getAllProduits ===");
            return result;
        } catch (Exception e) {
            System.err.println("Erreur dans ProduitController.getAllProduits: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/search")
    public Page<ProduitDTO> searchProduits(@RequestBody ProduitSearchCriteria criteria, Pageable pageable) {
        Page<Produit> produits = produitService.searchProduits(criteria, pageable);
        return produits.map(produitMapper::toDto);
    }

    /**
     * Upload d'image pour un produit existant
     */
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<ProduitDTO> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile file) {
        try {
            Produit produit = produitService.uploadImageToProduit(id, file);
            return ResponseEntity.ok(produitMapper.toDto(produit));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Suppression de l'image du produit (met l'image à null)
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<ProduitDTO> deleteImage(@PathVariable("id") Long id) {
        produitService.deleteImageFromProduit(id);
        Produit produit = produitService.getProduitById(id);
        return ResponseEntity.ok(produitMapper.toDto(produit));
    }



}

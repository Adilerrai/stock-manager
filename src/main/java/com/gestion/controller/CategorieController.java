package com.gestion.controller;

import com.gestion.persistent.dto.CategorieDTO;
import com.gestion.persistent.dto.CategorieNodeDTO;
import com.gestion.persistent.model.Categorie;
import com.gestion.persistent.model.Produit;
import com.gestion.service.CategorieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/categories", "/api/categories"})
@CrossOrigin(origins = "*")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    /**
     * Liste plate de toutes les catégories
     */
    @GetMapping
    public ResponseEntity<List<CategorieDTO>> getAllCategories() {
        return ResponseEntity.ok(categorieService.getAllCategories());
    }

    /**
     * Retourne l'arbre hiérarchique récursif complet (Catégories -> Sous-catégories)
     */
    @GetMapping({"/arbre", "/tree"})
    public ResponseEntity<List<CategorieNodeDTO>> getArbreCategories() {
        return ResponseEntity.ok(categorieService.getArbreCategories());
    }

    /**
     * Retourne uniquement les catégories racines (niveau 0, sans parent)
     */
    @GetMapping({"/racines", "/roots"})
    public ResponseEntity<List<CategorieDTO>> getCategoriesRacines() {
        return ResponseEntity.ok(categorieService.getCategoriesRacines());
    }

    /**
     * Retourne les sous-catégories directes d'une catégorie
     */
    @GetMapping("/{id}/sous-categories")
    public ResponseEntity<List<CategorieDTO>> getSousCategories(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.getSousCategories(id));
    }

    /**
     * Retourne tous les produits rattachés à cette catégorie OU à l'une de ses sous-catégories
     * (Ex: Filtration -> retourne Filtre à huile, Filtre à air, etc.)
     */
    @GetMapping("/{id}/produits")
    public ResponseEntity<List<Produit>> getProduitsParCategorie(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.getProduitsParCategorieRecursive(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieDTO> getCategorieById(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.getCategorieDtoById(id));
    }

    @PostMapping
    public ResponseEntity<Categorie> creerCategorie(@RequestBody Categorie categorie) {
        Categorie created = categorieService.creerCategorie(categorie);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Créer directement une sous-catégorie sous un parent
     */
    @PostMapping("/{parentId}/sous-categories")
    public ResponseEntity<Categorie> creerSousCategorie(@PathVariable Long parentId, @RequestBody Categorie sousCategorie) {
        Categorie created = categorieService.creerSousCategorie(parentId, sousCategorie);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categorie> modifierCategorie(@PathVariable Long id, @RequestBody Categorie categorie) {
        return ResponseEntity.ok(categorieService.modifierCategorie(id, categorie));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerCategorie(@PathVariable Long id) {
        categorieService.supprimerCategorie(id);
        return ResponseEntity.noContent().build();
    }
}

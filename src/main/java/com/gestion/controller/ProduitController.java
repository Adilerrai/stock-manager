package com.gestion.controller;

import com.gestion.mapper.ProduitMapper;
import com.gestion.persistent.dto.EtiquetteCodeBarreRequest;
import com.gestion.persistent.dto.ProduitCodeBarreDTO;
import com.gestion.persistent.dto.ProduitDTO;
import com.gestion.persistent.dto.ProduitSearchCriteria;
import com.gestion.persistent.model.Produit;
import com.gestion.service.BarcodeService;
import com.gestion.service.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Autowired
    private BarcodeService barcodeService;

    @PostMapping("/add")
    public ProduitDTO createProduit(@RequestBody ProduitDTO produitDTO) {
        Produit produit = produitMapper.toEntity(produitDTO);
        Produit savedProduit = produitService.createProduit(produit);
        return produitMapper.toDto(savedProduit);
    }

    @GetMapping("/get/{id}")
    public ProduitDTO getProduitById(@PathVariable("id") Long id) {
        Produit produit = produitService.getProduitWithImageById(id);
        return produitMapper.toDto(produit);
    }

    @PostMapping("/update")
    public ProduitDTO updateProduit(@RequestBody ProduitDTO produitDTO) {
        Produit updatedProduit = produitService.updateProduit(produitDTO);
        return produitMapper.toDto(updatedProduit);
    }

    @GetMapping("/{id}/historique-prix")
    public ResponseEntity<List<com.gestion.persistent.model.HistoriquePrixProduit>> getHistoriquePrix(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.getHistoriquePrixByProduitId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<ProduitDTO> getAllProduits() {
        List<Produit> produits = produitService.getAllProduits();
        return produits.stream()
                .map(produitMapper::toDto)
                .collect(Collectors.toList());
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
     * Récupération directe du flux binaire de l'image d'un produit
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getProduitImage(@PathVariable Long id) {
        com.gestion.persistent.model.ProduitImage image = produitService.getProduitImage(id);
        if (image == null || image.getImageData() == null || image.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String ct = image.getContentType() != null ? image.getContentType() : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(ct))
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .body(image.getImageData());
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

    // =========================================================================
    // CODE-BARRES & ÉTIQUETTES
    // =========================================================================

    /**
     * Récupère la liste des produits avec leur code-barre en Base64 (JSON).
     */
    @GetMapping("/code-barres/liste")
    public ResponseEntity<List<ProduitCodeBarreDTO>> getListeCodeBarres(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) Long categorieId) {
        return ResponseEntity.ok(barcodeService.getProduitsCodeBarres(ids, categorieId));
    }

    /**
     * Récupère la liste des produits avec code-barre pour une sélection d'IDs passée en body JSON.
     */
    @PostMapping("/code-barres/liste")
    public ResponseEntity<List<ProduitCodeBarreDTO>> getListeCodeBarresParIds(
            @RequestBody(required = false) List<Long> ids) {
        return ResponseEntity.ok(barcodeService.getProduitsCodeBarres(ids, null));
    }

    /**
     * Génère et télécharge le PDF d'étiquettes de codes-barres A4 (GET).
     * Modèles : "sticker" (défaut, épuré) ou "grille" (inventaire avec cartouche bleu).
     */
    @GetMapping("/code-barres/etiquettes-pdf")
    public ResponseEntity<byte[]> imprimerEtiquettesPdf(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false, defaultValue = "sticker") String modele,
            @RequestParam(required = false, defaultValue = "2") Integer colonnes,
            @RequestParam(required = false, defaultValue = "1") Integer exemplaires,
            @RequestParam(required = false, defaultValue = "false") Boolean afficherPrix) {

        EtiquetteCodeBarreRequest req = new EtiquetteCodeBarreRequest();
        req.setProduitIds(ids);
        req.setCategorieId(categorieId);
        req.setModele(modele);
        req.setColonnes(colonnes);
        req.setExemplaires(exemplaires);
        req.setAfficherPrix(afficherPrix);

        byte[] pdf = barcodeService.genererEtiquettesPdf(req);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"etiquettes-codes-barres.pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    /**
     * Génère et télécharge le PDF d'étiquettes de codes-barres A4 (POST avec body complet).
     */
    @PostMapping("/code-barres/etiquettes-pdf")
    public ResponseEntity<byte[]> imprimerEtiquettesPdfPost(
            @RequestBody(required = false) EtiquetteCodeBarreRequest request) {

        EtiquetteCodeBarreRequest req = (request != null) ? request : new EtiquetteCodeBarreRequest();
        byte[] pdf = barcodeService.genererEtiquettesPdf(req);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"etiquettes-codes-barres.pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    /**
     * Récupère l'image PNG directe du code-barre d'un produit par son ID.
     */
    @GetMapping("/{id}/code-barre-image")
    public ResponseEntity<byte[]> getCodeBarreImageProduit(@PathVariable Long id) {
        Produit p = produitService.getProduitById(id);
        String code = barcodeService.resolveCode(p);
        byte[] png = barcodeService.generateBarcodePng(code, 220, 65);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .body(png);
    }

    /**
     * Génère à la volée une image PNG de code-barre Code 128 pour n'importe quel code passé en paramètre.
     */
    @GetMapping("/code-barre-image")
    public ResponseEntity<byte[]> getCodeBarreImageGenerique(@RequestParam String code) {
        byte[] png = barcodeService.generateBarcodePng(code, 220, 65);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .body(png);
    }
}

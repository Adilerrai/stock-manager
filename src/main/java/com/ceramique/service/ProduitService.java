package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.ceramique.mapper.ProduitMapper;
import com.ceramique.persistent.dto.ProduitDTO;
import com.ceramique.persistent.dto.ProduitSearchCriteria;
import com.ceramique.persistent.model.Produit;
import com.ceramique.persistent.model.ProduitImage;
import com.ceramique.repository.ProduitImageRepository;
import com.ceramique.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final ProduitMapper produitMapper;
    private final ProduitImageRepository produitImageRepository;
    @Autowired
    private ImageCompressionService imageCompressionService;

    public ProduitService(
            ProduitRepository produitRepository,
            PointDeVenteRepository pointDeVenteRepository,
            ProduitMapper produitMapper, ProduitImageRepository produitImageRepository) {
        this.produitRepository = produitRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.produitMapper = produitMapper;
        this.produitImageRepository = produitImageRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un nouveau produit")
    public Produit createProduit(Produit produit) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        String reference = "PROD-" + System.currentTimeMillis();
        produit.setReference(reference);
        produit.setPointDeVente(pointDeVente);

        return produitRepository.save(produit);
    }

    @Transactional(readOnly = true)
    @MultitenantSearchMethod(description = "Récupération de tous les produits du point de vente")
    public List<Produit> getAllProduits() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        List<Produit> produits = produitRepository.findByPointDeVente_IdWithImages(pointDeVente.getId());
        
        // Debug: vérifier les données d'image
        for (Produit produit : produits) {
            if (produit.getImage() != null) {
                System.out.println("Produit " + produit.getId() + " - Image ID: " + produit.getImage().getId());
                System.out.println("  - Image data length: " + (produit.getImage().getImageData() != null ? produit.getImage().getImageData().length : "NULL"));
                System.out.println("  - Content type: " + produit.getImage().getContentType());
            }
        }
        
        return produits;
    }

    @MultitenantSearchMethod(description = "Récupération de tous les produits sans images")
    public List<Produit> getAllProduitsWithoutImages() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return produitRepository.findByPointDeVente_Id(pointDeVente.getId());
    }

    @MultitenantSearchMethod(description = "Récupération d'un produit par ID")
    public Produit getProduitById(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return produitRepository.findByIdAndPointDeVente_Id(produitId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));
    }

    @MultitenantSearchMethod(description = "Récupération d'un produit avec image par ID")
    public Produit getProduitWithImageById(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return produitRepository.findByIdAndPointDeVente_Id(produitId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));
    }


    @Transactional
    @MultitenantSearchMethod(description = "Mise à jour d'un produit")
    public Produit updateProduit(ProduitDTO produitDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Produit produit = produitRepository.findByIdAndPointDeVente_Id(produitDTO.getId(), pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitDTO.getId()));
        
        produitMapper.updateEntityFromDto(produitDTO, produit);

        // Debug et gestion de l'image
        if (produitDTO.getImage() != null) {

            try {
                ProduitImage produitImage = produit.getImage();
                if (produitImage == null) {
                    produitImage = new ProduitImage();
                }
                
                byte[] imageDataToCompress = null;
                
                // Priorité aux données binaires, sinon base64
                if (produitDTO.getImage().getImageData() != null) {
                    imageDataToCompress = produitDTO.getImage().getImageData();
                } else if (produitDTO.getImage().getBase64Data() != null) {
                    imageDataToCompress = java.util.Base64.getDecoder().decode(produitDTO.getImage().getBase64Data());
                }
                
                if (imageDataToCompress != null) {
                    // Compresser l'image
                    byte[] compressedImageData = imageCompressionService.compressImage(
                        imageDataToCompress, 
                        produitDTO.getImage().getContentType()
                    );
                    
                    System.out.println("Image compressée: " + compressedImageData.length + " bytes");
                    
                    produitImage.setFileName(produitDTO.getImage().getFileName());
                    produitImage.setImageData(compressedImageData);
                    produitImage.setContentType(produitDTO.getImage().getContentType());
                    produitImage.setProduit(produit);
                    
                    produit.setImage(produitImage);
                } else {
                    System.err.println("ERREUR: Aucune donnée d'image trouvée!");
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du traitement de l'image: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Erreur lors de la compression de l'image: " + e.getMessage());
            }
        }

        return produitRepository.save(produit);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Suppression d'un produit")
    public void deleteProduit(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Produit produit = produitRepository.findByIdAndPointDeVente_Id(produitId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        produitRepository.delete(produit);
    }

    @MultitenantSearchMethod(description = "Recherche de produits par critères")
    public Page<Produit> searchProduits(ProduitSearchCriteria criteria, Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return produitRepository.findByCriteria(criteria, pointDeVente.getId(), pageable);
    }

    /**
     * Upload d'une image pour un produit existant
     */
    @Transactional
    @MultitenantSearchMethod(description = "Upload d'image pour un produit")
    public Produit uploadImageToProduit(Long produitId, org.springframework.web.multipart.MultipartFile file) throws Exception {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Produit produit = produitRepository.findByIdAndPointDeVente_Id(produitId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        if (file == null || file.isEmpty()) {
            throw new CommonException("Fichier image vide", HttpStatus.BAD_REQUEST, "IMAGE_EMPTY");
        }
        // Limite de taille (5MB) cohérente avec properties
        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new CommonException("Fichier trop volumineux (max 5MB)", HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE");
        }
        // Types autorisés
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/webp"))) {
            throw new CommonException("Type de fichier non supporté (JPEG, PNG, WEBP seulement)", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_TYPE_INVALID");
        }

        byte[] compressedImageData = imageCompressionService.compressImage(
                file.getBytes(),
                contentType
        );

        if (produit.getImage() != null) {
            produitImageRepository.delete(produit.getImage());
        }

        ProduitImage produitImage = new ProduitImage();
        produitImage.setFileName(file.getOriginalFilename());
        produitImage.setImageData(compressedImageData);
        produitImage.setContentType(contentType);
        produitImage.setProduit(produit);

        produit.setImage(produitImage);

        return produitRepository.save(produit);
    }

    /**
     * Suppression de l'image d'un produit
     */
    @Transactional
    @MultitenantSearchMethod(description = "Suppression de l'image d'un produit")
    public void deleteImageFromProduit(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Produit produit = produitRepository.findByIdAndPointDeVente_Id(produitId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        if (produit.getImage() != null) {
            produitImageRepository.delete(produit.getImage());
            produit.setImage(null);
            produitRepository.save(produit);
        }
    }

    /**
     * Création d'un produit avec image en une seule opération
     */
    @Transactional
    @MultitenantSearchMethod(description = "Création d'un produit avec image")
    public Produit createProduitWithImage(
            String reference,
            String designation,
            String description,
            java.math.BigDecimal prixAchat,
            java.math.BigDecimal prixVente,
            org.springframework.web.multipart.MultipartFile imageFile) throws Exception {

        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        // Créer le produit
        Produit produit = new Produit();
        produit.setReference(reference != null && !reference.isEmpty() ? reference : "PROD-" + System.currentTimeMillis());
        produit.setDesignation(designation);
        produit.setDescription(description);
        produit.setPrixAchat(prixAchat);
        produit.setPrixVente(prixVente);
        produit.setPointDeVente(pointDeVente);
        produit.setActif(true);

        // Ajouter l'image si fournie
        if (imageFile != null && !imageFile.isEmpty()) {
            byte[] compressedImageData = imageCompressionService.compressImage(
                    imageFile.getBytes(),
                    imageFile.getContentType()
            );

            ProduitImage produitImage = new ProduitImage();
            produitImage.setFileName(imageFile.getOriginalFilename());
            produitImage.setImageData(compressedImageData);
            produitImage.setContentType(imageFile.getContentType());
            produitImage.setProduit(produit);

            produit.setImage(produitImage);
        }

        return produitRepository.save(produit);
    }

}

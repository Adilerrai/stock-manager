package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.ProduitMapper;
import com.gestion.persistent.dto.ProduitDTO;
import com.gestion.persistent.dto.ProduitSearchCriteria;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.ProduitImage;
import com.gestion.repository.ProduitImageRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;
    private final ProduitImageRepository produitImageRepository;
    private final CodificationService codificationService;
    private final com.gestion.repository.HistoriquePrixProduitRepository historiquePrixProduitRepository;
    @Autowired
    private ImageCompressionService imageCompressionService;

    public ProduitService(
            ProduitRepository produitRepository,
            ProduitMapper produitMapper,
            ProduitImageRepository produitImageRepository,
            CodificationService codificationService,
            com.gestion.repository.HistoriquePrixProduitRepository historiquePrixProduitRepository) {
        this.produitRepository = produitRepository;
        this.produitMapper = produitMapper;
        this.produitImageRepository = produitImageRepository;
        this.codificationService = codificationService;
        this.historiquePrixProduitRepository = historiquePrixProduitRepository;
    }

    @Transactional
    public Produit createProduit(Produit produit) {
        if (produit.getReference() == null || produit.getReference().trim().isEmpty()) {
            try {
                String reference = codificationService.genererNumero(TypeDocumentCodification.PRODUIT);
                produit.setReference(reference);
            } catch (Exception e) {
                // Fallback si la codification n'est pas encore configurée pour ce tenant
                produit.setReference("PROD-" + System.currentTimeMillis());
            }
        }

        if (produit.getPointDeVenteId() == null) {
            Long tenantId = TenantContext.getCurrentTenant();
            produit.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        }

        // Gestion et compression de l'image si fournie
        if (produit.getImage() != null) {
            ProduitImage img = produit.getImage();
            img.setProduit(produit);
            if (img.getImageData() != null && img.getImageData().length > 0) {
                try {
                    byte[] compressed = imageCompressionService.compressImage(img.getImageData(), img.getContentType());
                    img.setImageData(compressed);
                } catch (Exception e) {
                    System.err.println("Avertissement compression image produit lors de la création: " + e.getMessage());
                }
            }
        }

        return produitRepository.save(produit);
    }

    @Transactional(readOnly = true)
    public List<Produit> getAllProduits() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Produit> produits = produitRepository.findWithImagesByPointDeVenteId(tenantId != null ? tenantId : 1L);
        return produits;
    }

    public List<Produit> getAllProduitsWithoutImages() {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenant = tenantId != null ? tenantId : 1L;
        return produitRepository.findByPointDeVenteId(effectiveTenant);
    }

    public Produit getProduitById(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return produitRepository.findByIdAndPointDeVenteId(produitId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));
    }

    public Produit getProduitWithImageById(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return produitRepository.findWithImageByIdAndPointDeVenteId(produitId, tenantId != null ? tenantId : 1L)
                .or(() -> produitRepository.findById(produitId))
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));
    }

    @Transactional(readOnly = true)
    public ProduitImage getProduitImage(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Produit produit = produitRepository.findWithImageByIdAndPointDeVenteId(produitId, tenantId != null ? tenantId : 1L)
                .or(() -> produitRepository.findById(produitId))
                .orElse(null);
        return produit != null ? produit.getImage() : null;
    }


    @Transactional
    public Produit updateProduit(ProduitDTO produitDTO) {

        Produit produit = produitRepository.findById(produitDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitDTO.getId()));
        
        java.math.BigDecimal ancienPrixVente = produit.getPrixVente();
        java.math.BigDecimal ancienPrixMin = produit.getPrixVenteMin();

        produitMapper.updateEntityFromDto(produitDTO, produit);

        // Traçabilité historique des prix de vente si le prix normal ou le prix min a changé
        java.math.BigDecimal nouveauPrixVente = produit.getPrixVente();
        java.math.BigDecimal nouveauPrixMin = produit.getPrixVenteMin();
        boolean prixVenteChange = (ancienPrixVente == null && nouveauPrixVente != null) ||
                (ancienPrixVente != null && nouveauPrixVente != null && ancienPrixVente.compareTo(nouveauPrixVente) != 0);
        boolean prixMinChange = (ancienPrixMin == null && nouveauPrixMin != null) ||
                (ancienPrixMin != null && nouveauPrixMin != null && ancienPrixMin.compareTo(nouveauPrixMin) != 0);

        if (prixVenteChange || prixMinChange) {
            com.gestion.persistent.model.HistoriquePrixProduit hist = new com.gestion.persistent.model.HistoriquePrixProduit();
            hist.setProduit(produit);
            hist.setPointDeVenteId(produit.getPointDeVenteId() != null ? produit.getPointDeVenteId() : 1L);
            hist.setAncienPrixVente(ancienPrixVente);
            hist.setNouveauPrixVente(nouveauPrixVente);
            hist.setAncienPrixMin(ancienPrixMin);
            hist.setNouveauPrixMin(nouveauPrixMin);
            hist.setDateModification(java.time.LocalDateTime.now());
            hist.setMotif("Mise à jour via fiche produit");
            historiquePrixProduitRepository.save(hist);
        }

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
                throw new RuntimeException("Erreur lors de la compression de l'image: " + e.getMessage());
            }
        }

        return produitRepository.save(produit);
    }

    @Transactional
    public void deleteProduit(Long produitId) {

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        produitRepository.delete(produit);
    }

    public Page<Produit> searchProduits(ProduitSearchCriteria criteria, Pageable pageable) {

        return produitRepository.findByCriteria(criteria, pageable);
    }

    /**
     * Upload d'une image pour un produit existant
     */
    @Transactional
    public Produit uploadImageToProduit(Long produitId, org.springframework.web.multipart.MultipartFile file) throws Exception {

        Produit produit = produitRepository.findById(produitId)
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
    public void deleteImageFromProduit(Long produitId) {

        Produit produit = produitRepository.findById(produitId)
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
    public Produit createProduitWithImage(
            String reference,
            String designation,
            String description,
            java.math.BigDecimal prixAchat,
            java.math.BigDecimal prixVente,
            org.springframework.web.multipart.MultipartFile imageFile) throws Exception {
        // Créer le produit
        Produit produit = new Produit();
        produit.setReference(reference != null && !reference.isEmpty() ? reference : "PROD-" + System.currentTimeMillis());
        produit.setDesignation(designation);
        produit.setDescription(description);
        produit.setPrixAchat(prixAchat);
        produit.setPrixVente(prixVente);
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

    @Transactional(readOnly = true)
    public List<com.gestion.persistent.model.HistoriquePrixProduit> getHistoriquePrixByProduitId(Long produitId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenant = tenantId != null ? tenantId : 1L;
        return historiquePrixProduitRepository.findByProduitIdAndPointDeVenteIdOrderByDateModificationDesc(produitId, effectiveTenant);
    }

}


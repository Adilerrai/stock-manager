package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.DocumentLivreurCandidateDTO;
import com.gestion.persistent.enums.TypeActionLivreur;
import com.gestion.persistent.enums.TypeDocumentLivreur;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.PreuveDepotLivreur;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.repository.PreuveDepotLivreurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LivreurService {

    private static final Logger log = LoggerFactory.getLogger(LivreurService.class);
    private static final String UPLOAD_DIR = "uploads/ged/livraisons/";

    private final PreuveDepotLivreurRepository preuveDepotLivreurRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final FactureRepository factureRepository;

    public LivreurService(PreuveDepotLivreurRepository preuveDepotLivreurRepository,
                          BonLivraisonClientRepository bonLivraisonClientRepository,
                          FactureRepository factureRepository) {
        this.preuveDepotLivreurRepository = preuveDepotLivreurRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.factureRepository = factureRepository;
    }

    private Long getCurrentTenant() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<DocumentLivreurCandidateDTO> searchDocuments(String query, String typeDocument) {
        Long tenantId = getCurrentTenant();
        String q = (query != null) ? query.trim().toLowerCase() : "";
        List<DocumentLivreurCandidateDTO> candidates = new ArrayList<>();

        boolean searchBl = typeDocument == null || typeDocument.equalsIgnoreCase("ALL") || typeDocument.equalsIgnoreCase("BON_LIVRAISON");
        boolean searchFacture = typeDocument == null || typeDocument.equalsIgnoreCase("ALL") || typeDocument.equalsIgnoreCase("FACTURE");

        if (searchBl) {
            List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByPointDeVenteId(tenantId);
            for (BonLivraisonClient bl : bls) {
                if (bl.getStatut() == com.gestion.persistent.enums.StatutLivraison.ANNULEE) {
                    continue;
                }
                String num = bl.getNumeroBl() != null ? bl.getNumeroBl().toLowerCase() : "";
                String clientNom = (bl.getClient() != null && bl.getClient().getNomComplet() != null)
                        ? bl.getClient().getNomComplet().toLowerCase() : "";

                if (q.isEmpty() || num.contains(q) || clientNom.contains(q)) {
                    DocumentLivreurCandidateDTO dto = new DocumentLivreurCandidateDTO();
                    dto.setId(bl.getId());
                    dto.setType("BON_LIVRAISON");
                    dto.setNumero(bl.getNumeroBl());
                    if (bl.getClient() != null) {
                        dto.setClientNom(bl.getClient().getNomComplet());
                        dto.setAdresse(bl.getClient().getAdresse());
                        dto.setTelephone(bl.getClient().getTelephone());
                        dto.setVille(bl.getClient().getVille());
                    }
                    dto.setDateCreation(bl.getDateBl());
                    dto.setMontantTotal(bl.getMontantTotal());
                    dto.setStatut(bl.getStatut() != null ? bl.getStatut().name() : "EN_ATTENTE");
                    candidates.add(dto);
                }
            }
        }

        if (searchFacture) {
            List<Facture> factures = factureRepository.findByPointDeVenteIdOrderByDateFactureDesc(tenantId);
            for (Facture f : factures) {
                if (f.getStatut() == com.gestion.persistent.enums.StatutFacture.ANNULEE) {
                    continue;
                }
                String num = f.getNumeroFacture() != null ? f.getNumeroFacture().toLowerCase() : "";
                String clientNom = (f.getClient() != null && f.getClient().getNomComplet() != null)
                        ? f.getClient().getNomComplet().toLowerCase() : "";

                if (q.isEmpty() || num.contains(q) || clientNom.contains(q)) {
                    DocumentLivreurCandidateDTO dto = new DocumentLivreurCandidateDTO();
                    dto.setId(f.getId());
                    dto.setType("FACTURE");
                    dto.setNumero(f.getNumeroFacture());
                    if (f.getClient() != null) {
                        dto.setClientNom(f.getClient().getNomComplet());
                        dto.setAdresse(f.getClient().getAdresse());
                        dto.setTelephone(f.getClient().getTelephone());
                        dto.setVille(f.getClient().getVille());
                    }
                    dto.setDateCreation(f.getDateFacture() != null ? f.getDateFacture().atStartOfDay() : null);
                    dto.setMontantTotal(f.getMontantTTC());
                    dto.setStatut(f.getStatut() != null ? f.getStatut().name() : "VALIDEE");
                    candidates.add(dto);
                }
            }
        }

        // Tri décroissant par date
        candidates.sort((a, b) -> {
            if (a.getDateCreation() == null) return 1;
            if (b.getDateCreation() == null) return -1;
            return b.getDateCreation().compareTo(a.getDateCreation());
        });

        if (candidates.size() > 50) {
            return candidates.subList(0, 50);
        }
        return candidates;
    }

    @Transactional
    public PreuveDepotLivreur declarerAction(MultipartFile photo,
                                             TypeDocumentLivreur documentType,
                                             Long documentId,
                                             String numeroDocument,
                                             String clientNom,
                                             TypeActionLivreur typeAction,
                                             String commentaire,
                                             String nomLivreur,
                                             Long livreurUserId,
                                             Double latitude,
                                             Double longitude) throws IOException {
        Long tenantId = getCurrentTenant();

        PreuveDepotLivreur preuve = new PreuveDepotLivreur();
        preuve.setPointDeVenteId(tenantId);
        preuve.setDocumentType(documentType);
        preuve.setDocumentId(documentId);
        preuve.setNumeroDocument(numeroDocument);
        preuve.setClientNom(clientNom);
        preuve.setTypeAction(typeAction);
        preuve.setCommentaire(commentaire);
        preuve.setNomLivreur(nomLivreur);
        preuve.setLivreurUserId(livreurUserId);
        preuve.setLatitude(latitude);
        preuve.setLongitude(longitude);
        preuve.setDateDeclaration(LocalDateTime.now());

        if (photo != null && !photo.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = photo.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            } else {
                extension = ".jpg";
            }

            String filename = "preuve_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(filename);
            photo.transferTo(filePath.toFile());

            preuve.setNomFichierPhoto(originalName);
            preuve.setPhotoUrl(UPLOAD_DIR + filename);
            log.info("Photo de livraison/facture enregistrée: {}", filePath);
        }

        PreuveDepotLivreur saved = preuveDepotLivreurRepository.save(preuve);
        log.info("Action livreur déclarée avec succès (Preuve ID: {}, Action: {}, Document: {}) SANS modifier l'état métier existant.",
                saved.getId(), typeAction, numeroDocument);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PreuveDepotLivreur> getHistorique() {
        Long tenantId = getCurrentTenant();
        return preuveDepotLivreurRepository.findByPointDeVenteIdOrderByDateDeclarationDesc(tenantId);
    }
}

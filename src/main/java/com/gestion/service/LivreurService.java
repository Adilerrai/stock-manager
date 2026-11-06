package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import org.springframework.http.HttpStatus;
import com.gestion.persistent.dto.DocumentLivreurCandidateDTO;
import com.gestion.persistent.enums.TypeActionLivreur;
import com.gestion.persistent.enums.TypeDocumentLivreur;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.PreuveDepotLivreur;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.repository.PreuveDepotLivreurRepository;
import com.gestion.service.push.ExpeditionBlNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LivreurService {

    private static final Logger log = LoggerFactory.getLogger(LivreurService.class);
    /** Type de pièce GED des photos de décharge : les distingue des pièces ajoutées au bureau. */
    static final String TYPE_PIECE_DECHARGE = "DECHARGE_LIVREUR";

    private final PreuveDepotLivreurRepository preuveDepotLivreurRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final FactureRepository factureRepository;
    private final ExpeditionBlNotifier expeditionBlNotifier;
    private final GedService gedService;

    public LivreurService(PreuveDepotLivreurRepository preuveDepotLivreurRepository,
                          BonLivraisonClientRepository bonLivraisonClientRepository,
                          FactureRepository factureRepository,
                          ExpeditionBlNotifier expeditionBlNotifier,
                          GedService gedService) {
        this.preuveDepotLivreurRepository = preuveDepotLivreurRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.factureRepository = factureRepository;
        this.expeditionBlNotifier = expeditionBlNotifier;
        this.gedService = gedService;
    }

    private Long getCurrentTenant() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<DocumentLivreurCandidateDTO> searchDocuments(String query, String typeDocument) {
        Long tenantId = getCurrentTenant();
        String q = (query != null) ? query.trim().toLowerCase() : "";
        // Exiger un code complété comportant au moins un chiffre (ne pas retourner de résultats si seules des lettres/préfixes sont saisis)
        if (q.isEmpty() || !q.matches(".*\\d.*")) {
            return List.of();
        }
        List<DocumentLivreurCandidateDTO> candidates = new ArrayList<>();

        boolean searchBl = typeDocument == null || typeDocument.equalsIgnoreCase("ALL") || typeDocument.equalsIgnoreCase("BON_LIVRAISON") || typeDocument.equalsIgnoreCase("BL");
        boolean searchFacture = typeDocument == null || typeDocument.equalsIgnoreCase("ALL") || typeDocument.equalsIgnoreCase("FACTURE");

        if (searchBl) {
            List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByPointDeVenteId(tenantId);
            for (BonLivraisonClient bl : bls) {
                if (bl.getStatut() == com.gestion.persistent.enums.StatutLivraison.ANNULEE) {
                    continue;
                }
                String num = bl.getNumeroBl() != null ? bl.getNumeroBl().toLowerCase() : "";
                String factureNum = (bl.getFacture() != null && bl.getFacture().getNumeroFacture() != null)
                        ? bl.getFacture().getNumeroFacture().toLowerCase() : "";

                if (num.contains(q) || (!factureNum.isEmpty() && factureNum.contains(q))) {
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
                boolean blMatch = f.getBonsLivraison() != null && f.getBonsLivraison().stream()
                        .anyMatch(b -> b.getNumeroBl() != null && b.getNumeroBl().toLowerCase().contains(q));

                if (num.contains(q) || blMatch) {
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
    public PreuveDepotLivreur declarerActionFlexible(MultipartFile photo,
                                                     String documentTypeStr,
                                                     Long documentId,
                                                     String numeroDocument,
                                                     String clientNom,
                                                     String typeActionStr,
                                                     String commentaire,
                                                     String nomLivreur,
                                                     Long livreurUserId,
                                                     Double latitude,
                                                     Double longitude) throws IOException {
        TypeActionLivreur typeAction;
        try {
            typeAction = TypeActionLivreur.valueOf(typeActionStr.toUpperCase().trim());
        } catch (Exception e) {
            typeAction = TypeActionLivreur.LIVRAISON_EFFECTUEE;
        }

        TypeDocumentLivreur documentType = null;
        if (documentTypeStr != null && !documentTypeStr.isBlank()) {
            String upper = documentTypeStr.toUpperCase().trim();
            if (upper.equals("BL") || upper.equals("BON_LIVRAISON")) {
                documentType = TypeDocumentLivreur.BON_LIVRAISON;
            } else if (upper.equals("FACTURE")) {
                documentType = TypeDocumentLivreur.FACTURE;
            }
        }

        if (documentType == null) {
            if (typeAction == TypeActionLivreur.FACTURE_DEPOSEE) {
                documentType = TypeDocumentLivreur.FACTURE;
            } else {
                documentType = TypeDocumentLivreur.BON_LIVRAISON;
            }
        }

        if ((numeroDocument == null || numeroDocument.isBlank() || clientNom == null || clientNom.isBlank()) && documentId != null) {
            if (documentType == TypeDocumentLivreur.BON_LIVRAISON) {
                var optBl = bonLivraisonClientRepository.findById(documentId);
                if (optBl.isPresent()) {
                    var bl = optBl.get();
                    if (numeroDocument == null || numeroDocument.isBlank()) numeroDocument = bl.getNumeroBl();
                    if ((clientNom == null || clientNom.isBlank()) && bl.getClient() != null) clientNom = bl.getClient().getNomComplet();
                }
            } else {
                var optF = factureRepository.findById(documentId);
                if (optF.isPresent()) {
                    var f = optF.get();
                    if (numeroDocument == null || numeroDocument.isBlank()) numeroDocument = f.getNumeroFacture();
                    if ((clientNom == null || clientNom.isBlank()) && f.getClient() != null) clientNom = f.getClient().getNomComplet();
                }
            }
        }

        return declarerAction(photo, documentType, documentId, numeroDocument, clientNom, typeAction, commentaire, nomLivreur, livreurUserId, latitude, longitude);
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

        // Le document doit appartenir à la société du livreur : sinon ni statut, ni photo, ni notification
        BonLivraisonClient blDeclare = null;
        Facture factureDeclaree = null;
        if (documentType == TypeDocumentLivreur.BON_LIVRAISON) {
            blDeclare = bonLivraisonClientRepository.findByIdAndPointDeVenteId(documentId, tenantId)
                    .orElseThrow(() -> new CommonException("Bon de livraison introuvable", HttpStatus.NOT_FOUND));
        } else if (documentType == TypeDocumentLivreur.FACTURE) {
            factureDeclaree = factureRepository.findByIdAndPointDeVenteId(documentId, tenantId)
                    .orElseThrow(() -> new CommonException("Facture introuvable", HttpStatus.NOT_FOUND));
        }

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

        // Hors du try plus bas : une photo qui ne s'enregistre pas fait échouer la déclaration au lieu de se perdre
        if (photo != null && !photo.isEmpty()) {
            preuve.setNomFichierPhoto(photo.getOriginalFilename());
            preuve.setPhotoUrl(rattacherDechargeAGed(photo, blDeclare, factureDeclaree, nomLivreur, commentaire));
        }

        PreuveDepotLivreur saved = preuveDepotLivreurRepository.save(preuve);
        log.info("Action livreur déclarée avec succès (Preuve ID: {}, Action: {}, Document: {})",
                saved.getId(), typeAction, numeroDocument);

        // Association automatique de la pièce jointe et mise à jour des statuts métier :
        // BL -> CACHETE / EXPEDIE, Facture -> DEPOSEE
        try {
            if (blDeclare != null) {
                java.util.Optional.of(blDeclare).ifPresent(bl -> {
                    bl.setDateLivraisonEffective(LocalDateTime.now());
                    if (commentaire != null && !commentaire.isBlank()) {
                        bl.setCommentaireLivreur(commentaire);
                    }
                    StatutLivraison statutPrecedent = bl.getStatut();
                    if (bl.getStatut() != StatutLivraison.ANNULEE) {
                        bl.setStatut((typeAction == TypeActionLivreur.LIVRAISON_EFFECTUEE || typeAction == TypeActionLivreur.BL_EXPEDIE || typeAction == TypeActionLivreur.LES_DEUX) ? StatutLivraison.CACHETE : StatutLivraison.EXPEDIE);
                    }
                    bonLivraisonClientRepository.save(bl);
                    expeditionBlNotifier.signalerDeclarationLivreur(bl, statutPrecedent);

                    if (bl.getFacture() != null) {
                        Facture fac = bl.getFacture();
                        if (fac.getStatut() != StatutFacture.ANNULEE) {
                            fac.setStatut(StatutFacture.DEPOSEE);
                            factureRepository.save(fac);
                        }

                        if (photo != null && !photo.isEmpty() && saved.getPhotoUrl() != null) {
                            PreuveDepotLivreur preuveFac = new PreuveDepotLivreur();
                            preuveFac.setPointDeVenteId(tenantId);
                            preuveFac.setDocumentType(TypeDocumentLivreur.FACTURE);
                            preuveFac.setDocumentId(fac.getId());
                            preuveFac.setNumeroDocument(fac.getNumeroFacture());
                            preuveFac.setClientNom(clientNom);
                            preuveFac.setTypeAction(TypeActionLivreur.FACTURE_DEPOSEE);
                            preuveFac.setCommentaire("Pièce jointe décharge (BL " + numeroDocument + "). " + (commentaire != null ? commentaire : ""));
                            preuveFac.setNomLivreur(nomLivreur);
                            preuveFac.setLivreurUserId(livreurUserId);
                            preuveFac.setLatitude(latitude);
                            preuveFac.setLongitude(longitude);
                            preuveFac.setDateDeclaration(LocalDateTime.now());
                            preuveFac.setNomFichierPhoto(saved.getNomFichierPhoto());
                            preuveFac.setPhotoUrl(saved.getPhotoUrl());
                            preuveDepotLivreurRepository.save(preuveFac);
                        }
                    }
                });
            } else if (factureDeclaree != null) {
                java.util.Optional.of(factureDeclaree).ifPresent(f -> {
                    if (f.getStatut() != StatutFacture.ANNULEE) {
                        f.setStatut(StatutFacture.DEPOSEE);
                        factureRepository.save(f);
                    }

                    if (f.getBonsLivraison() != null && !f.getBonsLivraison().isEmpty()) {
                        for (BonLivraisonClient bl : f.getBonsLivraison()) {
                            bl.setDateLivraisonEffective(LocalDateTime.now());
                            if (bl.getStatut() != StatutLivraison.ANNULEE) {
                                bl.setStatut(StatutLivraison.CACHETE);
                            }
                            if (commentaire != null && !commentaire.isBlank()) {
                                bl.setCommentaireLivreur(commentaire);
                            }
                            bonLivraisonClientRepository.save(bl);

                            if (photo != null && !photo.isEmpty() && saved.getPhotoUrl() != null) {
                                PreuveDepotLivreur preuveBl = new PreuveDepotLivreur();
                                preuveBl.setPointDeVenteId(tenantId);
                                preuveBl.setDocumentType(TypeDocumentLivreur.BON_LIVRAISON);
                                preuveBl.setDocumentId(bl.getId());
                                preuveBl.setNumeroDocument(bl.getNumeroBl());
                                preuveBl.setClientNom(clientNom);
                                preuveBl.setTypeAction(TypeActionLivreur.LIVRAISON_EFFECTUEE);
                                preuveBl.setCommentaire("Pièce jointe décharge (Facture " + numeroDocument + "). " + (commentaire != null ? commentaire : ""));
                                preuveBl.setNomLivreur(nomLivreur);
                                preuveBl.setLivreurUserId(livreurUserId);
                                preuveBl.setLatitude(latitude);
                                preuveBl.setLongitude(longitude);
                                preuveBl.setDateDeclaration(LocalDateTime.now());
                                preuveBl.setNomFichierPhoto(saved.getNomFichierPhoto());
                                preuveBl.setPhotoUrl(saved.getPhotoUrl());
                                preuveDepotLivreurRepository.save(preuveBl);
                            }
                        }
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("Impossible d'associer la décharge ou mettre à jour les statuts : {}", ex.getMessage());
        }

        return saved;
    }

    /**
     * La photo va dans la GED de la société : elle apparaît dans les pièces jointes du BL et de sa facture,
     * avec son empreinte SHA-256. Une seule pièce quand le BL et sa facture vont ensemble ;
     * chaque BL de plus sur la même facture reçoit sa propre copie, la GED ne liant qu'un BL par pièce.
     *
     * @return l'URL de visualisation de la pièce principale
     */
    private String rattacherDechargeAGed(MultipartFile photo, BonLivraisonClient bl, Facture facture,
                                         String nomLivreur, String commentaire) throws IOException {
        String description = "Décharge livreur"
                + (nomLivreur != null && !nomLivreur.isBlank() ? " (" + nomLivreur.trim() + ")" : "")
                + (commentaire != null && !commentaire.isBlank() ? " : " + commentaire.trim() : "");
        if (description.length() > 500) {
            description = description.substring(0, 500);
        }
        if (bl != null) {
            Facture sienne = bl.getFacture();
            return stockerDecharge(photo, description, bl.getId(), sienne != null ? sienne.getId() : null);
        }
        if (facture != null) {
            List<BonLivraisonClient> bls = facture.getBonsLivraison() != null ? new ArrayList<>(facture.getBonsLivraison()) : List.of();
            String url = stockerDecharge(photo, description, bls.isEmpty() ? null : bls.get(0).getId(), facture.getId());
            for (int i = 1; i < bls.size(); i++) {
                stockerDecharge(photo, description, bls.get(i).getId(), null);
            }
            return url;
        }
        return stockerDecharge(photo, description, null, null);
    }

    private String stockerDecharge(MultipartFile photo, String description, Long blId, Long factureId) throws IOException {
        return gedService.stockerDocument(photo, TYPE_PIECE_DECHARGE, description,
                null, null, factureId, null, null, null, null, blId).getUrlVisualisation();
    }

    @Transactional(readOnly = true)
    public List<PreuveDepotLivreur> getHistorique() {
        Long tenantId = getCurrentTenant();
        return preuveDepotLivreurRepository.findByPointDeVenteIdOrderByDateDeclarationDesc(tenantId);
    }
}

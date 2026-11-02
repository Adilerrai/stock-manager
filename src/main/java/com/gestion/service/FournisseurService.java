package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.FournisseurDTO;
import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.model.Fournisseur;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.LigneCommandeRepository;
import com.gestion.persistent.model.LigneCommande;
import com.gestion.persistent.dto.FournisseurProduitResumeDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public FournisseurService(FournisseurRepository fournisseurRepository,
                              LigneCommandeRepository ligneCommandeRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new org.springframework.security.access.AccessDeniedException("Impossible d'identifier l'entreprise (tenant) courante");
        }
        return tenant;
    }

    @Transactional
    public Fournisseur createFournisseur(FournisseurDTO fournisseurDTO) {
        Long tenantId = getTenantId();
        if (fournisseurRepository.existsByRaisonSocialeAndPointDeVenteId(fournisseurDTO.getRaisonSociale(), tenantId)) {
            throw new IllegalArgumentException("Un fournisseur avec ce nom existe déjà");
        }

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        fournisseur.setAdresse(fournisseurDTO.getAdresse());
        fournisseur.setTelephone(fournisseurDTO.getTelephone());
        fournisseur.setEmail(fournisseurDTO.getEmail());
        fournisseur.setContact(fournisseurDTO.getContact());
        fournisseur.setIce(fournisseurDTO.getIce());
        fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        fournisseur.setPatente(fournisseurDTO.getPatente());
        fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours() != null ? fournisseurDTO.getDelaiPaiementJours() : 30);
        fournisseur.setVille(fournisseurDTO.getVille());
        fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        fournisseur.setPays(fournisseurDTO.getPays() != null ? fournisseurDTO.getPays() : "Maroc");
        fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        fournisseur.setActif(true);
        fournisseur.setPointDeVenteId(tenantId);

        if (fournisseurDTO.getTypeFournisseur() != null) {
            fournisseur.setTypeFournisseur(fournisseurDTO.getTypeFournisseur());
        }
        if (fournisseurDTO.getDevise() != null && !fournisseurDTO.getDevise().isBlank()) {
            fournisseur.setDevise(fournisseurDTO.getDevise());
        }

        return fournisseurRepository.save(fournisseur);
    }

    public List<Fournisseur> getAllFournisseursActifs() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return List.of();
        }
        return fournisseurRepository.findActiveByPointDeVenteIdOrderByNom(tenant);
    }

    public Fournisseur getFournisseurById(Long fournisseurId) {
        Long tenantId = getTenantId();
        return fournisseurRepository.findByIdAndPointDeVenteId(fournisseurId, tenantId)
                .orElseGet(() -> fournisseurRepository.findById(fournisseurId)
                        .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", fournisseurId)));
    }

    @Transactional
    public Fournisseur updateFournisseur(FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = getFournisseurById(fournisseurDTO.getId());
        
        // Mise à jour des champs
        if (fournisseurDTO.getRaisonSociale() != null) {
            fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        }
        if (fournisseurDTO.getAdresse() != null) {
            fournisseur.setAdresse(fournisseurDTO.getAdresse());
        }
        if (fournisseurDTO.getTelephone() != null) {
            fournisseur.setTelephone(fournisseurDTO.getTelephone());
        }
        if (fournisseurDTO.getEmail() != null) {
            fournisseur.setEmail(fournisseurDTO.getEmail());
        }
        if (fournisseurDTO.getContact() != null) {
            fournisseur.setContact(fournisseurDTO.getContact());
        }
        if (fournisseurDTO.getIce() != null) {
            fournisseur.setIce(fournisseurDTO.getIce());
        }
        if (fournisseurDTO.getNumeroRegistreCommerce() != null) {
            fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        }
        if (fournisseurDTO.getNumeroIdentificationFiscale() != null) {
            fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        }
        if (fournisseurDTO.getPatente() != null) {
            fournisseur.setPatente(fournisseurDTO.getPatente());
        }
        if (fournisseurDTO.getRibBancaire() != null) {
            fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        }
        if (fournisseurDTO.getBanqueNom() != null) {
            fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        }
        if (fournisseurDTO.getDelaiPaiementJours() != null) {
            fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours());
        }
        if (fournisseurDTO.getVille() != null) {
            fournisseur.setVille(fournisseurDTO.getVille());
        }
        if (fournisseurDTO.getCodePostal() != null) {
            fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        }
        if (fournisseurDTO.getPays() != null) {
            fournisseur.setPays(fournisseurDTO.getPays());
        }
        if (fournisseurDTO.getConditionsPaiement() != null) {
            fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        }
        if (fournisseurDTO.getActif() != null) {
            fournisseur.setActif(fournisseurDTO.getActif());
        }
        if (fournisseurDTO.getTypeFournisseur() != null) {
            fournisseur.setTypeFournisseur(fournisseurDTO.getTypeFournisseur());
        }
        if (fournisseurDTO.getDevise() != null) {
            fournisseur.setDevise(fournisseurDTO.getDevise());
        }

        return fournisseurRepository.save(fournisseur);
    }

    @Transactional
    public void deleteFournisseur(Long fournisseurId) {
        Fournisseur fournisseur = getFournisseurById(fournisseurId);
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    public Page<Fournisseur> searchFournisseurs(FournisseurSearchCriteria criteria, Pageable pageable) {
        return fournisseurRepository.findByCriteria(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public List<FournisseurProduitResumeDTO> getEtatProduitsCommandes(Long fournisseurId, Long produitId, String search) {
        Long tenantId = getTenantId();
        List<LigneCommande> lignes;
        if (produitId != null) {
            lignes = ligneCommandeRepository.findByFournisseurAndProduitAndTenant(fournisseurId, produitId, tenantId);
        } else {
            lignes = ligneCommandeRepository.findByFournisseurAndTenant(fournisseurId, tenantId);
        }

        Map<Long, FournisseurProduitResumeDTO> resumeMap = new LinkedHashMap<>();

        for (LigneCommande lc : lignes) {
            if (lc.getProduit() == null) continue;
            Long pId = lc.getProduit().getId();

            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                boolean matchRef = lc.getProduit().getReference() != null && lc.getProduit().getReference().toLowerCase().contains(term);
                boolean matchNom = lc.getProduit().getDesignation() != null && lc.getProduit().getDesignation().toLowerCase().contains(term);
                if (!matchRef && !matchNom) {
                    continue;
                }
            }

            FournisseurProduitResumeDTO dto = resumeMap.computeIfAbsent(pId, id -> {
                FournisseurProduitResumeDTO r = new FournisseurProduitResumeDTO();
                r.setProduitId(id);
                r.setReference(lc.getProduit().getReference());
                r.setDesignation(lc.getProduit().getDesignation());
                if (lc.getProduit().getCategorie() != null) {
                    r.setCategorie(lc.getProduit().getCategorie().getNom());
                }
                return r;
            });

            int qteCmd = lc.getQuantiteCommandee() != null ? lc.getQuantiteCommandee() : 0;
            int qteLiv = lc.getQuantiteLivree() != null ? lc.getQuantiteLivree() : 0;
            BigDecimal prixHT = lc.getPrixUnitaireHT() != null ? lc.getPrixUnitaireHT() : (lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO);
            BigDecimal mntHT = lc.getMontantHT() != null ? lc.getMontantHT() : prixHT.multiply(BigDecimal.valueOf(qteCmd));
            BigDecimal mntTTC = lc.getMontantTTC() != null ? lc.getMontantTTC() : mntHT.multiply(new BigDecimal("1.20"));

            dto.setQuantiteTotaleCommandee(dto.getQuantiteTotaleCommandee() + qteCmd);
            dto.setQuantiteTotaleLivree(dto.getQuantiteTotaleLivree() + qteLiv);
            dto.setMontantTotalHT(dto.getMontantTotalHT().add(mntHT));
            dto.setMontantTotalTTC(dto.getMontantTotalTTC().add(mntTTC));
            dto.setNombreCommandes(dto.getNombreCommandes() + 1);

            if (dto.getDernierPrixAchat().compareTo(BigDecimal.ZERO) == 0 && prixHT.compareTo(BigDecimal.ZERO) > 0) {
                dto.setDernierPrixAchat(prixHT);
            }
            if (dto.getDateDerniereCommande() == null && lc.getCommande() != null) {
                dto.setDateDerniereCommande(lc.getCommande().getDateCommande());
            }

            FournisseurProduitResumeDTO.CommandeReferenceResumeDTO cmdRef = new FournisseurProduitResumeDTO.CommandeReferenceResumeDTO();
            if (lc.getCommande() != null) {
                cmdRef.setCommandeId(lc.getCommande().getId());
                cmdRef.setNumeroCommande(lc.getCommande().getNumeroCommande());
                cmdRef.setDateCommande(lc.getCommande().getDateCommande());
                cmdRef.setStatut(lc.getCommande().getStatut() != null ? lc.getCommande().getStatut().name() : null);
            }
            cmdRef.setQuantiteCommandee(qteCmd);
            cmdRef.setQuantiteLivree(qteLiv);
            cmdRef.setPrixUnitaireHT(prixHT);
            cmdRef.setMontantHT(mntHT);
            dto.getCommandes().add(cmdRef);
        }

        for (FournisseurProduitResumeDTO r : resumeMap.values()) {
            if (r.getQuantiteTotaleCommandee() > 0 && r.getMontantTotalHT().compareTo(BigDecimal.ZERO) > 0) {
                r.setPrixMoyen(r.getMontantTotalHT().divide(BigDecimal.valueOf(r.getQuantiteTotaleCommandee()), 2, RoundingMode.HALF_UP));
            } else {
                r.setPrixMoyen(r.getDernierPrixAchat());
            }
        }

        return new ArrayList<>(resumeMap.values());
    }
}


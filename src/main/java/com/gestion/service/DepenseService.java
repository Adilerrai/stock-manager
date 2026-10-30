package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.DepenseDTO;
import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.persistent.dto.LigneDepenseConsommableDTO;
import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.ResultatEntrepriseDTO;
import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.model.Consommable;
import com.gestion.persistent.model.Depense;
import com.gestion.persistent.model.LigneDepenseConsommable;
import com.gestion.persistent.model.Prestataire;
import com.gestion.repository.ConsommableRepository;
import com.gestion.repository.DepenseRepository;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.LigneDepenseConsommableRepository;
import com.gestion.repository.PrestataireRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final UserRepository userRepository;
    private final MargeService margeService;
    private final FournisseurRepository fournisseurRepository;
    private final PrestataireRepository prestataireRepository;
    private final ConsommableRepository consommableRepository;
    private final LigneDepenseConsommableRepository ligneDepenseConsommableRepository;
    private final GedService gedService;

    public DepenseService(DepenseRepository depenseRepository,
                          UserRepository userRepository,
                          MargeService margeService,
                          FournisseurRepository fournisseurRepository,
                          PrestataireRepository prestataireRepository,
                          ConsommableRepository consommableRepository,
                          LigneDepenseConsommableRepository ligneDepenseConsommableRepository,
                          @Lazy GedService gedService) {
        this.depenseRepository = depenseRepository;
        this.userRepository = userRepository;
        this.margeService = margeService;
        this.fournisseurRepository = fournisseurRepository;
        this.prestataireRepository = prestataireRepository;
        this.consommableRepository = consommableRepository;
        this.ligneDepenseConsommableRepository = ligneDepenseConsommableRepository;
        this.gedService = gedService;
    }

    public DepenseDTO creerDepense(DepenseDTO dto, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Depense depense = new Depense();
        depense.setReference(genererReference());
        depense.setDesignation(dto.getDesignation());
        depense.setDateDepense(dto.getDateDepense() != null ? dto.getDateDepense() : LocalDate.now());
        depense.setCategorie(dto.getCategorie() != null ? dto.getCategorie() : CategorieDepense.AUTRES_CHARGES);
        depense.setModePaiement(dto.getModePaiement() != null ? dto.getModePaiement() : ModePaiement.ESPECES);

        // Association Prestataire (La dépense est attachée au Prestataire de service)
        if (dto.getPrestataireId() != null) {
            Prestataire p = prestataireRepository.findById(dto.getPrestataireId()).orElse(null);
            depense.setPrestataire(p);
            if ((dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty()) && p != null) {
                depense.setBeneficiaire(p.getRaisonSociale());
            } else {
                depense.setBeneficiaire(dto.getBeneficiaire());
            }
        } else if (dto.getFournisseurId() != null) {
            // Rétrocompatibilité / transition si un ancien appel transmet un ID
            Prestataire p = prestataireRepository.findById(dto.getFournisseurId()).orElse(null);
            if (p != null) {
                depense.setPrestataire(p);
                if (dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty()) {
                    depense.setBeneficiaire(p.getRaisonSociale());
                } else {
                    depense.setBeneficiaire(dto.getBeneficiaire());
                }
            } else {
                com.gestion.persistent.model.Fournisseur f = fournisseurRepository.findById(dto.getFournisseurId()).orElse(null);
                depense.setFournisseur(f);
                if ((dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty()) && f != null) {
                    depense.setBeneficiaire(f.getRaisonSociale());
                } else {
                    depense.setBeneficiaire(dto.getBeneficiaire());
                }
            }
        } else {
            depense.setBeneficiaire(dto.getBeneficiaire());
        }

        depense.setArticlesDetail(dto.getArticlesDetail());
        depense.setNumeroFactureJustificatif(dto.getNumeroFactureJustificatif());
        depense.setNotes(dto.getNotes());
        depense.setCreePar(user);

        Long tenantId = TenantContext.getCurrentTenant();
        depense.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        depense.setDateCreation(LocalDateTime.now());

        // Traitement des lignes de consommables achetés
        BigDecimal totalLignesConsommables = BigDecimal.ZERO;
        List<LigneDepenseConsommable> lignesToSave = new ArrayList<>();

        if (dto.getLignesConsommables() != null && !dto.getLignesConsommables().isEmpty()) {
            for (LigneDepenseConsommableDTO ligneDto : dto.getLignesConsommables()) {
                if (ligneDto.getConsommableId() != null) {
                    Consommable c = consommableRepository.findById(ligneDto.getConsommableId()).orElse(null);
                    if (c != null) {
                        BigDecimal qte = ligneDto.getQuantite() != null ? ligneDto.getQuantite() : BigDecimal.ONE;
                        BigDecimal pu = ligneDto.getPrixUnitaire() != null ? ligneDto.getPrixUnitaire() : c.getDernierPrixAchat();

                        LigneDepenseConsommable ligne = new LigneDepenseConsommable(depense, c, qte, pu);
                        ligne.setNotes(ligneDto.getNotes());
                        lignesToSave.add(ligne);
                        totalLignesConsommables = totalLignesConsommables.add(ligne.getMontantTotal());

                        // Réapprovisionnement automatique du stock de consommables
                        c.setStockActuel(c.getStockActuel().add(qte));
                        if (pu.compareTo(BigDecimal.ZERO) > 0) {
                            c.setDernierPrixAchat(pu);
                        }
                        consommableRepository.save(c);
                    }
                }
            }
        }

        // Montant de la dépense : soit celui du DTO, soit somme des consommables
        if (dto.getMontant() != null && dto.getMontant().compareTo(BigDecimal.ZERO) > 0) {
            depense.setMontant(dto.getMontant());
        } else if (totalLignesConsommables.compareTo(BigDecimal.ZERO) > 0) {
            depense.setMontant(totalLignesConsommables);
        } else {
            depense.setMontant(BigDecimal.ZERO);
        }

        depense.getLignesConsommables().addAll(lignesToSave);

        Depense saved = depenseRepository.save(depense);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DepenseDTO> getAllDepenses() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return depenseRepository.findByPointDeVenteIdOrderByDateDepenseDesc(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepenseDTO> getDepensesByPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return depenseRepository.findByPeriodeAndPointDeVenteId(debut, fin, tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepenseDTO getDepenseById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        Depense d = (tenantId != null ? depenseRepository.findByIdAndPointDeVenteId(id, tenantId) : depenseRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));
        return toDto(d);
    }

    public void supprimerDepense(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        Depense d = (tenantId != null ? depenseRepository.findByIdAndPointDeVenteId(id, tenantId) : depenseRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));

        // Réajustement des stocks de consommables si des lignes existaient
        if (d.getLignesConsommables() != null) {
            for (LigneDepenseConsommable ligne : d.getLignesConsommables()) {
                if (ligne.getConsommable() != null) {
                    Consommable c = ligne.getConsommable();
                    BigDecimal nouveauStock = c.getStockActuel().subtract(ligne.getQuantite());
                    if (nouveauStock.compareTo(BigDecimal.ZERO) < 0) nouveauStock = BigDecimal.ZERO;
                    c.setStockActuel(nouveauStock);
                    consommableRepository.save(c);
                }
            }
        }

        depenseRepository.delete(d);
    }

    public DepenseDTO declarerPayee(Long id, ModePaiement modePaiement, LocalDate datePaiement, String referencePaiement) {
        Long tenantId = TenantContext.getCurrentTenant();
        Depense d = (tenantId != null ? depenseRepository.findByIdAndPointDeVenteId(id, tenantId) : depenseRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));

        if (modePaiement != null) d.setModePaiement(modePaiement);
        if (datePaiement != null) d.setDateDepense(datePaiement);
        if (referencePaiement != null && !referencePaiement.trim().isEmpty()) {
            d.setNumeroFactureJustificatif(referencePaiement);
        }

        Depense saved = depenseRepository.save(d);
        return toDto(saved);
    }

    public DepenseDTO modifierDepense(Long id, DepenseDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();
        Depense d = (tenantId != null ? depenseRepository.findByIdAndPointDeVenteId(id, tenantId) : depenseRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));

        if (dto.getDesignation() != null) d.setDesignation(dto.getDesignation());
        if (dto.getMontant() != null) d.setMontant(dto.getMontant());
        if (dto.getDateDepense() != null) d.setDateDepense(dto.getDateDepense());
        if (dto.getCategorie() != null) d.setCategorie(dto.getCategorie());
        if (dto.getModePaiement() != null) d.setModePaiement(dto.getModePaiement());
        if (dto.getBeneficiaire() != null) d.setBeneficiaire(dto.getBeneficiaire());
        if (dto.getNumeroFactureJustificatif() != null) d.setNumeroFactureJustificatif(dto.getNumeroFactureJustificatif());
        if (dto.getNotes() != null) d.setNotes(dto.getNotes());
        if (dto.getArticlesDetail() != null) d.setArticlesDetail(dto.getArticlesDetail());

        if (dto.getPrestataireId() != null) {
            Prestataire p = prestataireRepository.findById(dto.getPrestataireId()).orElse(null);
            d.setPrestataire(p);
            if (p != null && (dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty())) {
                d.setBeneficiaire(p.getRaisonSociale());
            }
        } else if (dto.getFournisseurId() != null) {
            Prestataire p = prestataireRepository.findById(dto.getFournisseurId()).orElse(null);
            if (p != null) {
                d.setPrestataire(p);
                if (dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty()) {
                    d.setBeneficiaire(p.getRaisonSociale());
                }
            } else {
                d.setFournisseur(fournisseurRepository.findById(dto.getFournisseurId()).orElse(null));
            }
        }

        Depense saved = depenseRepository.save(d);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getStatistiquesParCategorie(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        Long tenantId = TenantContext.getCurrentTenant();
        List<Object[]> rows = tenantId != null
                ? depenseRepository.findTotauxParCategorieAndPointDeVenteId(debut, fin, tenantId)
                : depenseRepository.findTotauxParCategorie(debut, fin);
        Map<String, BigDecimal> map = new LinkedHashMap<>();

        if (rows != null) {
            for (Object[] r : rows) {
                if (r[0] != null) {
                    CategorieDepense cat = (CategorieDepense) r[0];
                    BigDecimal montant = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                    map.put(cat.getLibelle(), montant);
                }
            }
        }
        return map;
    }

    /**
     * Calcule le résultat d'exploitation net réel de l'entreprise sur la période :
     * Résultat Net = Marge Commerciale Nette - Total Dépenses
     */
    @Transactional(readOnly = true)
    public ResultatEntrepriseDTO calculerResultatNet(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        Long tenantId = TenantContext.getCurrentTenant();
        MargeDTO marge = margeService.calculerMargeGlobale(debut, fin);
        BigDecimal totalDepenses = tenantId != null
                ? depenseRepository.sumMontantByPeriodeAndPointDeVenteId(debut, fin, tenantId)
                : depenseRepository.sumMontantByPeriode(debut, fin);
        if (totalDepenses == null) totalDepenses = BigDecimal.ZERO;

        BigDecimal resultatNet = marge.getMargeNetteCommerciale().subtract(totalDepenses);

        BigDecimal rentabiliteNette = BigDecimal.ZERO;
        if (marge.getChiffreAffairesHT().compareTo(BigDecimal.ZERO) > 0) {
            rentabiliteNette = resultatNet.multiply(new BigDecimal("100")).divide(marge.getChiffreAffairesHT(), 2, RoundingMode.HALF_UP);
        }

        ResultatEntrepriseDTO resultat = new ResultatEntrepriseDTO();
        resultat.setDateDebut(debut);
        resultat.setDateFin(fin);
        resultat.setChiffreAffairesHT(marge.getChiffreAffairesHT());
        resultat.setCoutMarchandisesHT(marge.getCoutMarchandisesHT());
        resultat.setTotalRemises(marge.getTotalRemises());
        resultat.setTotalRetours(marge.getTotalRetoursAvoirs());
        resultat.setMargeCommerciale(marge.getMargeNetteCommerciale());
        resultat.setTauxMarge(marge.getTauxMarge());
        resultat.setTotalDepenses(totalDepenses);
        resultat.setDepensesParCategorie(getStatistiquesParCategorie(debut, fin));
        resultat.setResultatNetEstime(resultatNet);
        resultat.setRentabiliteNette(rentabiliteNette);

        return resultat;
    }

    public org.springframework.data.domain.Page<DepenseDTO> searchDepenses(com.gestion.persistent.dto.DepenseSearchCriteria criteria, org.springframework.data.domain.Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Depense> list = tenantId != null ? depenseRepository.findByPointDeVenteId(tenantId) : depenseRepository.findAll();

        List<DepenseDTO> filtered = list.stream()
                .filter(d -> {
                    if (criteria == null) return true;
                    if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                        String q = criteria.getQuery().toLowerCase().trim();
                        String ref = d.getReference() != null ? d.getReference().toLowerCase() : "";
                        String des = d.getDesignation() != null ? d.getDesignation().toLowerCase() : "";
                        String ben = d.getBeneficiaire() != null ? d.getBeneficiaire().toLowerCase() : "";
                        String fn = d.getFournisseur() != null && d.getFournisseur().getRaisonSociale() != null ? d.getFournisseur().getRaisonSociale().toLowerCase() : "";
                        String pr = d.getPrestataire() != null && d.getPrestataire().getRaisonSociale() != null ? d.getPrestataire().getRaisonSociale().toLowerCase() : "";
                        String notes = d.getNotes() != null ? d.getNotes().toLowerCase() : "";
                        boolean matches = ref.contains(q) || des.contains(q) || ben.contains(q) || fn.contains(q) || pr.contains(q) || notes.contains(q);
                        if (!matches) return false;
                    }
                    if (criteria.getReference() != null && !criteria.getReference().trim().isEmpty()) {
                        if (d.getReference() == null || !d.getReference().toLowerCase().contains(criteria.getReference().toLowerCase())) return false;
                    }
                    if (criteria.getDesignation() != null && !criteria.getDesignation().trim().isEmpty()) {
                        if (d.getDesignation() == null || !d.getDesignation().toLowerCase().contains(criteria.getDesignation().toLowerCase())) return false;
                    }
                    if (criteria.getBeneficiaire() != null && !criteria.getBeneficiaire().trim().isEmpty()) {
                        String b = d.getBeneficiaire();
                        String fn = d.getFournisseur() != null ? d.getFournisseur().getRaisonSociale() : null;
                        String pn = d.getPrestataire() != null ? d.getPrestataire().getRaisonSociale() : null;
                        String q = criteria.getBeneficiaire().toLowerCase();
                        boolean matchB = b != null && b.toLowerCase().contains(q);
                        boolean matchFn = fn != null && fn.toLowerCase().contains(q);
                        boolean matchPn = pn != null && pn.toLowerCase().contains(q);
                        if (!matchB && !matchFn && !matchPn) return false;
                    }
                    if (criteria.getCategorie() != null && d.getCategorie() != criteria.getCategorie()) {
                        return false;
                    }
                    if (criteria.getModePaiement() != null && d.getModePaiement() != criteria.getModePaiement()) {
                        return false;
                    }
                    if (criteria.getDateDebut() != null && d.getDateDepense() != null && d.getDateDepense().isBefore(criteria.getDateDebut())) {
                        return false;
                    }
                    if (criteria.getDateFin() != null && d.getDateDepense() != null && d.getDateDepense().isAfter(criteria.getDateFin())) {
                        return false;
                    }
                    if (criteria.getMontantMin() != null && d.getMontant() != null && d.getMontant().compareTo(criteria.getMontantMin()) < 0) {
                        return false;
                    }
                    if (criteria.getMontantMax() != null && d.getMontant() != null && d.getMontant().compareTo(criteria.getMontantMax()) > 0) {
                        return false;
                    }
                    if (criteria.getFournisseurId() != null) {
                        if (d.getFournisseur() == null || !d.getFournisseur().getId().equals(criteria.getFournisseurId())) return false;
                    }
                    if (criteria.getPrestataireId() != null) {
                        if (d.getPrestataire() == null || !d.getPrestataire().getId().equals(criteria.getPrestataireId())) return false;
                    }
                    if (criteria.getConsommableId() != null) {
                        boolean hasCons = d.getLignesConsommables() != null && d.getLignesConsommables().stream()
                                .anyMatch(l -> l.getConsommable() != null && l.getConsommable().getId().equals(criteria.getConsommableId()));
                        if (!hasCons) return false;
                    }
                    if (criteria.getDeverseeCompta() != null) {
                        boolean dev = Boolean.TRUE.equals(d.getDeverseeCompta());
                        if (dev != criteria.getDeverseeCompta()) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Depense::getDateDepense, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Depense::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<DepenseDTO> pageContent = (start <= end && start < filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filtered.size());
    }

    public DepenseDTO toDto(Depense d) {
        DepenseDTO dto = new DepenseDTO();
        dto.setId(d.getId());
        dto.setReference(d.getReference());
        dto.setDesignation(d.getDesignation());
        dto.setMontant(d.getMontant());
        dto.setDateDepense(d.getDateDepense());
        dto.setCategorie(d.getCategorie());
        dto.setModePaiement(d.getModePaiement());
        // Prestataire (Prioritaire pour une dépense)
        if (d.getPrestataire() != null) {
            dto.setPrestataireId(d.getPrestataire().getId());
            dto.setPrestataireNom(d.getPrestataire().getRaisonSociale());
            dto.setPrestataireTelephone(d.getPrestataire().getTelephone());
            dto.setPrestataireIce(d.getPrestataire().getIce());
            dto.setTypePrestation(d.getPrestataire().getTypePrestation());

            // Rétrocompatibilité d'affichage si l'UI lit fournisseurNom
            if (dto.getFournisseurNom() == null) {
                dto.setFournisseurNom(d.getPrestataire().getRaisonSociale());
                dto.setFournisseurId(d.getPrestataire().getId());
            }
            if (dto.getBeneficiaire() == null || dto.getBeneficiaire().trim().isEmpty()) {
                dto.setBeneficiaire(d.getPrestataire().getRaisonSociale());
            }
        }

        // Fournisseur (Rétrocompatibilité historique)
        if (d.getFournisseur() != null && dto.getPrestataireId() == null) {
            dto.setFournisseurId(d.getFournisseur().getId());
            dto.setFournisseurNom(d.getFournisseur().getRaisonSociale());
            dto.setFournisseurTelephone(d.getFournisseur().getTelephone());
            dto.setFournisseurIce(d.getFournisseur().getIce());
        }

        // Lignes de consommables
        if (d.getLignesConsommables() != null && !d.getLignesConsommables().isEmpty()) {
            List<LigneDepenseConsommableDTO> lignes = d.getLignesConsommables().stream()
                    .map(l -> {
                        LigneDepenseConsommableDTO lDto = new LigneDepenseConsommableDTO();
                        lDto.setId(l.getId());
                        lDto.setDepenseId(d.getId());
                        if (l.getConsommable() != null) {
                            lDto.setConsommableId(l.getConsommable().getId());
                            lDto.setConsommableReference(l.getConsommable().getReference());
                            lDto.setConsommableDesignation(l.getConsommable().getDesignation());
                            lDto.setUniteMesure(l.getConsommable().getUniteMesure() != null ? l.getConsommable().getUniteMesure().getLibelle() : "");
                        }
                        lDto.setQuantite(l.getQuantite());
                        lDto.setPrixUnitaire(l.getPrixUnitaire());
                        lDto.setMontantTotal(l.getMontantTotal());
                        lDto.setNotes(l.getNotes());
                        return lDto;
                    })
                    .collect(Collectors.toList());
            dto.setLignesConsommables(lignes);
        }

        // Pièces jointes GED attachées à cette dépense
        if (d.getId() != null && gedService != null) {
            try {
                List<DocumentComptableDTO> docs = gedService.getDocumentsParDepense(d.getId());
                dto.setDocuments(docs);
                dto.setNombrePiecesJointes(docs != null ? docs.size() : 0);
            } catch (Exception ignored) {
                dto.setNombrePiecesJointes(0);
            }
        }

        dto.setArticlesDetail(d.getArticlesDetail());
        dto.setDeverseeCompta(Boolean.TRUE.equals(d.getDeverseeCompta()));
        dto.setDateDeversement(d.getDateDeversement());
        dto.setEcritureId(d.getEcritureId());
        dto.setNumeroFactureJustificatif(d.getNumeroFactureJustificatif());
        dto.setNotes(d.getNotes());
        if (d.getCreePar() != null) {
            dto.setCreeParUserId(d.getCreePar().getId());
            dto.setCreeParNom(d.getCreePar().getNomComplet() != null ? d.getCreePar().getNomComplet() : d.getCreePar().getUsername());
        }
        dto.setPointDeVenteId(d.getPointDeVenteId());
        dto.setDateCreation(d.getDateCreation());
        if (d.getCommande() != null) {
            dto.setCommandeId(d.getCommande().getId());
            dto.setNumeroCommande(d.getCommande().getNumeroCommande());
        }
        dto.setEstFraisImport(Boolean.TRUE.equals(d.getEstFraisImport()));
        dto.setTypeFraisImport(d.getTypeFraisImport());
        return dto;
    }

    private String genererReference() {
        String prefixe = "DEP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = depenseRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}

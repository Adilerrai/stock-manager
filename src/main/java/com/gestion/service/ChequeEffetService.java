package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ChequeEffetDTO;
import com.gestion.persistent.dto.ChequeEffetSearchCriteria;
import com.gestion.persistent.dto.PortefeuilleStatsDTO;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChequeEffetService {

    private final ChequeEffetRepository chequeRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final BordereauRemiseRepository bordereauRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final JournalComptableRepository journalRepository;
    private final CompteComptableRepository compteRepository;

    public ChequeEffetService(ChequeEffetRepository chequeRepository,
                              ClientRepository clientRepository,
                              FournisseurRepository fournisseurRepository,
                              BordereauRemiseRepository bordereauRepository,
                              EcritureComptableRepository ecritureRepository,
                              JournalComptableRepository journalRepository,
                              CompteComptableRepository compteRepository) {
        this.chequeRepository = chequeRepository;
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.bordereauRepository = bordereauRepository;
        this.ecritureRepository = ecritureRepository;
        this.journalRepository = journalRepository;
        this.compteRepository = compteRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChequeEffetDTO> searchChequesEffets(ChequeEffetSearchCriteria criteria, Pageable pageable) {
        Page<ChequeEffet> page = chequeRepository.findByCriteria(criteria, pageable);
        return page.map(this::toDto);
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<ChequeEffetDTO> getAllCheques(SensEffet sens, StatutEffet statut) {
        Long tenantId = getTenantId();
        List<ChequeEffet> liste;

        if (sens != null && statut != null) {
            liste = chequeRepository.findByPointDeVenteIdAndSensAndStatutOrderByDateEcheanceAsc(tenantId, sens, statut);
        } else if (sens != null) {
            liste = chequeRepository.findByPointDeVenteIdAndSensOrderByDateEcheanceAsc(tenantId, sens);
        } else if (statut != null) {
            liste = chequeRepository.findByPointDeVenteIdAndStatutOrderByDateEcheanceAsc(tenantId, statut);
        } else {
            liste = chequeRepository.findByPointDeVenteIdOrderByDateEcheanceAsc(tenantId);
        }

        return liste.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChequeEffetDTO> listerCheques(StatutEffet statut, SensEffet sens) {
        return getAllCheques(sens, statut);
    }

    @Transactional(readOnly = true)
    public ChequeEffetDTO getChequeById(Long id) {
        ChequeEffet c = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque ou effet introuvable: " + id));
        return toDto(c);
    }

    public ChequeEffetDTO creerCheque(ChequeEffetDTO dto) {
        Long tenantId = getTenantId();

        if (dto.getNumeroPiece() == null || dto.getNumeroPiece().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro du chèque ou de l'effet est obligatoire");
        }
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        ChequeEffet cheque = new ChequeEffet();
        cheque.setNumeroPiece(dto.getNumeroPiece().trim());
        cheque.setTypeEffet(dto.getTypeEffet() != null ? dto.getTypeEffet() : TypeEffet.CHEQUE);
        cheque.setSens(dto.getSens() != null ? dto.getSens() : SensEffet.ENCAISSEMENT_CLIENT);
        cheque.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutEffet.EN_PORTEFEUILLE);
        cheque.setMontant(dto.getMontant());
        cheque.setDateEmission(dto.getDateEmission() != null ? dto.getDateEmission() : LocalDate.now());
        cheque.setDateEcheance(dto.getDateEcheance() != null ? dto.getDateEcheance() : cheque.getDateEmission());
        cheque.setBanqueEmettrice(dto.getBanqueEmettrice());
        cheque.setTireur(dto.getTireur());
        cheque.setBeneficiaire(dto.getBeneficiaire());
        cheque.setCompteBancaireDepot(dto.getCompteBancaireDepot());
        cheque.setReferencePaiement(dto.getReferencePaiement());
        cheque.setNotes(dto.getNotes());
        cheque.setPointDeVenteId(tenantId);
        cheque.setDateCreation(LocalDateTime.now());

        if (dto.getClientId() != null) {
            clientRepository.findById(dto.getClientId()).ifPresent(c -> {
                cheque.setClient(c);
                if (cheque.getTireur() == null || cheque.getTireur().isEmpty()) {
                    cheque.setTireur(c.getNom());
                }
            });
        }

        if (dto.getFournisseurId() != null) {
            fournisseurRepository.findById(dto.getFournisseurId()).ifPresent(f -> {
                cheque.setFournisseur(f);
                if (cheque.getBeneficiaire() == null || cheque.getBeneficiaire().isEmpty()) {
                    cheque.setBeneficiaire(f.getNom());
                }
            });
        }

        ChequeEffet saved = chequeRepository.save(cheque);
        return toDto(saved);
    }

    public ChequeEffetDTO remettreEnBanque(Long id, Long bordereauId, String compteBancaire, LocalDate dateRemise) {
        ChequeEffet cheque = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable: " + id));

        cheque.setStatut(StatutEffet.REMIS_A_L_ENCAISSEMENT);
        cheque.setDateRemise(dateRemise != null ? dateRemise : LocalDate.now());
        if (compteBancaire != null) {
            cheque.setCompteBancaireDepot(compteBancaire);
        }

        if (bordereauId != null) {
            bordereauRepository.findById(bordereauId).ifPresent(cheque::setBordereauRemise);
        }

        return toDto(chequeRepository.save(cheque));
    }

    public ChequeEffetDTO remettreEnBanque(Long id, String bordereauNumero, LocalDate dateRemise) {
        ChequeEffet cheque = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable: " + id));

        cheque.setStatut(StatutEffet.REMIS_A_L_ENCAISSEMENT);
        cheque.setDateRemise(dateRemise != null ? dateRemise : LocalDate.now());
        if (bordereauNumero != null && !bordereauNumero.trim().isEmpty()) {
            bordereauRepository.findByNumeroBordereau(bordereauNumero.trim())
                .ifPresent(cheque::setBordereauRemise);
        }

        return toDto(chequeRepository.save(cheque));
    }

    public ChequeEffetDTO encaisserCheque(Long id, LocalDate dateEncaissement) {
        ChequeEffet cheque = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable: " + id));

        cheque.setStatut(StatutEffet.ENCAISSE_COMPTABILISE);
        cheque.setDateEncaissement(dateEncaissement != null ? dateEncaissement : LocalDate.now());

        // Générer l'écriture comptable automatique d'encaissement définitif
        genererEcritureEncaissementCheque(cheque);

        return toDto(chequeRepository.save(cheque));
    }

    public ChequeEffetDTO confirmerEncaissement(Long id, LocalDate dateEncaissement) {
        return encaisserCheque(id, dateEncaissement);
    }

    public ChequeEffetDTO rejeterCheque(Long id, String motif) {
        ChequeEffet cheque = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable: " + id));

        cheque.setStatut(StatutEffet.IMPAYE_REJETE);
        cheque.setMotifRejet(motif != null ? motif : "Impayé bancaire");

        return toDto(chequeRepository.save(cheque));
    }

    public ChequeEffetDTO annulerCheque(Long id) {
        ChequeEffet cheque = chequeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable: " + id));

        cheque.setStatut(StatutEffet.ANNULE);
        return toDto(chequeRepository.save(cheque));
    }

    @Transactional(readOnly = true)
    public List<ChequeEffetDTO> getEcheancesProches(int jours) {
        Long tenantId = getTenantId();
        LocalDate dateLimite = LocalDate.now().plusDays(jours > 0 ? jours : 7);
        List<ChequeEffet> cheques = chequeRepository.findEcheancesProches(tenantId, dateLimite);
        return cheques.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistiquesFinance() {
        Long tenantId = getTenantId();

        BigDecimal encaissementEnPortefeuille = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.EN_PORTEFEUILLE);
        BigDecimal encaissementRemis = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.REMIS_A_L_ENCAISSEMENT);
        BigDecimal encaissementEncaisse = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.ENCAISSE_COMPTABILISE);
        BigDecimal encaissementImpayes = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.IMPAYE_REJETE);

        BigDecimal decaissementEnPortefeuille = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.DECAISSEMENT_FOURNISSEUR, StatutEffet.EN_PORTEFEUILLE);
        BigDecimal decaissementDebite = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.DECAISSEMENT_FOURNISSEUR, StatutEffet.ENCAISSE_COMPTABILISE);

        Map<String, Object> stats = new HashMap<>();
        stats.put("clientsEnPortefeuille", encaissementEnPortefeuille);
        stats.put("clientsRemisEncaissement", encaissementRemis);
        stats.put("clientsEncaisses", encaissementEncaisse);
        stats.put("clientsImpayes", encaissementImpayes);
        stats.put("fournisseursEnCirculation", decaissementEnPortefeuille);
        stats.put("fournisseursDebites", decaissementDebite);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ChequeEffetDTO> getChequesAEcheanceProche(int jours) {
        return getEcheancesProches(jours);
    }

    @Transactional(readOnly = true)
    public PortefeuilleStatsDTO getStatsPortefeuille() {
        Long tenantId = getTenantId();
        PortefeuilleStatsDTO dto = new PortefeuilleStatsDTO();

        BigDecimal encaissementEnPortefeuille = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.EN_PORTEFEUILLE);
        BigDecimal encaissementRemis = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.REMIS_A_L_ENCAISSEMENT);
        BigDecimal encaissementEncaisse = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.ENCAISSE_COMPTABILISE);
        BigDecimal encaissementImpayes = chequeRepository.sumMontantBySensAndStatut(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.IMPAYE_REJETE);

        dto.setTotalEnPortefeuille(encaissementEnPortefeuille != null ? encaissementEnPortefeuille : BigDecimal.ZERO);
        dto.setTotalRemisBanque(encaissementRemis != null ? encaissementRemis : BigDecimal.ZERO);
        dto.setTotalEncaisse(encaissementEncaisse != null ? encaissementEncaisse : BigDecimal.ZERO);
        dto.setTotalRejeteImpaye(encaissementImpayes != null ? encaissementImpayes : BigDecimal.ZERO);

        List<ChequeEffet> enPortefeuilleList = chequeRepository.findByPointDeVenteIdAndSensAndStatutOrderByDateEcheanceAsc(
            tenantId, SensEffet.ENCAISSEMENT_CLIENT, StatutEffet.EN_PORTEFEUILLE);
        dto.setNbChequesEnPortefeuille((long) enPortefeuilleList.size());

        LocalDate dateLimite = LocalDate.now().plusDays(7);
        List<ChequeEffet> echeances = chequeRepository.findEcheancesProches(tenantId, dateLimite);
        dto.setNbChequesAEcheance((long) echeances.size());

        return dto;
    }

    private void genererEcritureEncaissementCheque(ChequeEffet cheque) {
        Long tenantId = cheque.getPointDeVenteId();
        String refPiece = "ENC-" + cheque.getNumeroPiece();

        if (ecritureRepository.findByReferencePieceAndPointDeVenteId(refPiece, tenantId).isPresent()) {
            return;
        }

        JournalComptable journalBanque = journalRepository.findByCodeAndPointDeVenteId("BQ", tenantId).orElse(null);
        if (journalBanque == null) return;

        CompteComptable compteBanque = findOrCreateCompte("51410000", "Banque", 5, SensCompte.DEBIT, tenantId);
        CompteComptable compteChequeRemis = findOrCreateCompte("51110000", "Chèques à encaisser", 5, SensCompte.DEBIT, tenantId);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalBanque);
        ecriture.setDateEcriture(cheque.getDateEncaissement() != null ? cheque.getDateEncaissement() : LocalDate.now());
        ecriture.setLibelle("Encaissement chèque N° " + cheque.getNumeroPiece() + " - " + (cheque.getTireur() != null ? cheque.getTireur() : ""));
        ecriture.setReferencePiece(refPiece);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece("BQ-" + ecriture.getDateEcriture().getYear() + "-" + System.currentTimeMillis() % 100000);
        ecriture.setValidee(true);

        // Débit Banque
        ecriture.addLigne(new LigneEcriture(compteBanque, cheque.getMontant(), BigDecimal.ZERO, "Crédit en compte chèque " + cheque.getNumeroPiece(), tenantId));
        // Crédit Chèques à encaisser
        ecriture.addLigne(new LigneEcriture(compteChequeRemis, BigDecimal.ZERO, cheque.getMontant(), "Sortie chèque encaissé", tenantId));

        ecritureRepository.save(ecriture);
    }

    private CompteComptable findOrCreateCompte(String numero, String libelle, int classe, SensCompte sens, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(numero, tenantId)
            .orElseGet(() -> compteRepository.save(new CompteComptable(numero, libelle, classe, sens, tenantId)));
    }

    private ChequeEffetDTO toDto(ChequeEffet c) {
        ChequeEffetDTO dto = new ChequeEffetDTO();
        dto.setId(c.getId());
        dto.setNumeroPiece(c.getNumeroPiece());
        dto.setTypeEffet(c.getTypeEffet());
        dto.setSens(c.getSens());
        dto.setStatut(c.getStatut());
        dto.setMontant(c.getMontant());
        dto.setDateEmission(c.getDateEmission());
        dto.setDateEcheance(c.getDateEcheance());
        dto.setDateRemise(c.getDateRemise());
        dto.setDateEncaissement(c.getDateEncaissement());
        dto.setBanqueEmettrice(c.getBanqueEmettrice());
        dto.setTireur(c.getTireur());
        dto.setBeneficiaire(c.getBeneficiaire());
        dto.setCompteBancaireDepot(c.getCompteBancaireDepot());
        dto.setReferencePaiement(c.getReferencePaiement());
        dto.setMotifRejet(c.getMotifRejet());
        dto.setNotes(c.getNotes());

        if (c.getClient() != null) {
            dto.setClientId(c.getClient().getId());
            dto.setClientNom(c.getClient().getNom());
        }
        if (c.getFournisseur() != null) {
            dto.setFournisseurId(c.getFournisseur().getId());
            dto.setFournisseurNom(c.getFournisseur().getNom());
        }
        if (c.getBordereauRemise() != null) {
            dto.setBordereauRemiseId(c.getBordereauRemise().getId());
            dto.setNumeroBordereau(c.getBordereauRemise().getNumeroBordereau());
        }

        return dto;
    }
}

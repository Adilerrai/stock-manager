package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.CompteFinancierDTO;
import com.gestion.persistent.dto.MouvementTresorerieDTO;
import com.gestion.persistent.dto.SyntheseTresorerieDTO;
import com.gestion.persistent.enums.TypeCompteFinancier;
import com.gestion.persistent.enums.TypeMouvementTresorerie;
import com.gestion.persistent.model.CompteFinancier;
import com.gestion.persistent.model.MouvementTresorerie;
import com.gestion.repository.CompteFinancierRepository;
import com.gestion.repository.MouvementTresorerieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TresorerieAvanceeService {

    private final CompteFinancierRepository compteRepository;
    private final MouvementTresorerieRepository mouvementRepository;
    private final UserRepository userRepository;

    public TresorerieAvanceeService(CompteFinancierRepository compteRepository,
                                   MouvementTresorerieRepository mouvementRepository,
                                   UserRepository userRepository) {
        this.compteRepository = compteRepository;
        this.mouvementRepository = mouvementRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retourne la synthèse globale de trésorerie :
     * « Où est mon argent à cet instant précis ? »
     */
    @Transactional(readOnly = true)
    public SyntheseTresorerieDTO getSynthese() {
        BigDecimal totalCaisses = compteRepository.sumSoldeByType(TypeCompteFinancier.CAISSE_PHYSIQUE);
        BigDecimal totalBanques = compteRepository.sumSoldeByType(TypeCompteFinancier.COMPTE_BANCAIRE);
        if (totalCaisses == null) totalCaisses = BigDecimal.ZERO;
        if (totalBanques == null) totalBanques = BigDecimal.ZERO;

        List<CompteFinancierDTO> caisses = compteRepository.findByTypeAndActifTrue(TypeCompteFinancier.CAISSE_PHYSIQUE)
                .stream().map(this::toDto).collect(Collectors.toList());

        List<CompteFinancierDTO> banques = compteRepository.findByTypeAndActifTrue(TypeCompteFinancier.COMPTE_BANCAIRE)
                .stream().map(this::toDto).collect(Collectors.toList());

        SyntheseTresorerieDTO synthese = new SyntheseTresorerieDTO();
        synthese.setTotalCaisses(totalCaisses);
        synthese.setTotalBanques(totalBanques);
        synthese.setTresorerieDisponibleGlobale(totalCaisses.add(totalBanques));
        synthese.setCaisses(caisses);
        synthese.setComptesBancaires(banques);

        return synthese;
    }

    @Transactional(readOnly = true)
    public List<CompteFinancierDTO> getTousLesComptes() {
        return compteRepository.findByActifTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CompteFinancierDTO creerCompte(CompteFinancierDTO dto) {
        CompteFinancier c = new CompteFinancier();
        c.setCode(dto.getCode() != null ? dto.getCode().toUpperCase() : "CPT-" + System.currentTimeMillis());
        c.setNom(dto.getNom());
        c.setType(dto.getType() != null ? dto.getType() : TypeCompteFinancier.CAISSE_PHYSIQUE);
        c.setSoldeActuel(dto.getSoldeActuel() != null ? dto.getSoldeActuel() : BigDecimal.ZERO);
        c.setDevise(dto.getDevise() != null ? dto.getDevise() : "DZD");
        c.setNumeroCompteRib(dto.getNumeroCompteRib());
        c.setNomBanque(dto.getNomBanque());
        c.setActif(true);

        Long tenantId = TenantContext.getCurrentTenant();
        c.setPointDeVenteId(tenantId != null ? tenantId : 1L);

        CompteFinancier saved = compteRepository.save(c);
        return toDto(saved);
    }

    /**
     * Enregistre un mouvement de trésorerie (retrait gérant, versement banque, apport, etc.)
     * et met à jour immédiatement les soldes en direct.
     */
    public MouvementTresorerieDTO enregistrerMouvement(MouvementTresorerieDTO dto, Long userId) {
        if (dto.getCompteSourceId() == null) {
            throw new IllegalArgumentException("Le compte source est obligatoire");
        }
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du mouvement doit être strictement supérieur à 0");
        }

        CompteFinancier source = compteRepository.findById(dto.getCompteSourceId())
                .orElseThrow(() -> new RuntimeException("Compte source non trouvé"));

        CompteFinancier destination = null;
        if (dto.getCompteDestinationId() != null) {
            destination = compteRepository.findById(dto.getCompteDestinationId())
                    .orElseThrow(() -> new RuntimeException("Compte destination non trouvé"));
        }

        TypeMouvementTresorerie type = dto.getTypeMouvement() != null ? dto.getTypeMouvement() : TypeMouvementTresorerie.RETRAIT_ESPECES_GERANT;
        BigDecimal montant = dto.getMontant();

        // Application des flux sur les soldes
        switch (type) {
            case RETRAIT_ESPECES_GERANT:
            case DECAISSEMENT_DIVERS:
                if (source.getSoldeActuel().compareTo(montant) < 0) {
                    throw new IllegalStateException("Solde insuffisant sur le compte source (" + source.getNom() + " : " + source.getSoldeActuel() + " " + source.getDevise() + ")");
                }
                source.setSoldeActuel(source.getSoldeActuel().subtract(montant));
                break;

            case APPORT_FONDS:
            case ENCAISSEMENT_DIVERS:
                source.setSoldeActuel(source.getSoldeActuel().add(montant));
                break;

            case DEPOT_BANQUE:
            case TRANSFERT_INTERNE:
                if (destination == null) {
                    throw new IllegalArgumentException("Un compte de destination est obligatoire pour un transfert ou dépôt bancaire");
                }
                if (source.getSoldeActuel().compareTo(montant) < 0) {
                    throw new IllegalStateException("Solde insuffisant pour effectuer le transfert (" + source.getNom() + " : " + source.getSoldeActuel() + " " + source.getDevise() + ")");
                }
                source.setSoldeActuel(source.getSoldeActuel().subtract(montant));
                destination.setSoldeActuel(destination.getSoldeActuel().add(montant));
                compteRepository.save(destination);
                break;

            case AJUSTEMENT_SOLDE:
                source.setSoldeActuel(montant);
                break;
        }

        compteRepository.save(source);

        User effectuePar = null;
        if (userId != null) {
            effectuePar = userRepository.findById(userId).orElse(null);
        }

        MouvementTresorerie mvt = new MouvementTresorerie();
        mvt.setReference(genererReferenceMouvement());
        mvt.setTypeMouvement(type);
        mvt.setCompteSource(source);
        mvt.setCompteDestination(destination);
        mvt.setMontant(montant);
        mvt.setDateMouvement(dto.getDateMouvement() != null ? dto.getDateMouvement() : LocalDateTime.now());
        mvt.setMotif(dto.getMotif() != null ? dto.getMotif() : type.getLibelle());
        mvt.setJustificatifReference(dto.getJustificatifReference());
        mvt.setEffectuePar(effectuePar);
        mvt.setSoldeApresSource(source.getSoldeActuel());
        mvt.setSoldeApresDestination(destination != null ? destination.getSoldeActuel() : null);

        Long tenantId = TenantContext.getCurrentTenant();
        mvt.setPointDeVenteId(tenantId != null ? tenantId : 1L);

        MouvementTresorerie saved = mouvementRepository.save(mvt);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MouvementTresorerieDTO> getHistorique(Long compteId, LocalDate debut, LocalDate fin) {
        LocalDateTime debutDT = (debut != null ? debut : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime finDT = (fin != null ? fin : LocalDate.now()).atTime(LocalTime.MAX);

        List<MouvementTresorerie> liste;
        if (compteId != null) {
            liste = mouvementRepository.findByCompteAndPeriode(compteId, debutDT, finDT);
        } else {
            liste = mouvementRepository.findByPeriode(debutDT, finDT);
        }

        return liste.stream()
                .sorted(Comparator.comparing(MouvementTresorerie::getDateMouvement).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CompteFinancierDTO toDto(CompteFinancier c) {
        CompteFinancierDTO dto = new CompteFinancierDTO();
        dto.setId(c.getId());
        dto.setCode(c.getCode());
        dto.setNom(c.getNom());
        dto.setType(c.getType());
        dto.setSoldeActuel(c.getSoldeActuel());
        dto.setDevise(c.getDevise());
        dto.setNumeroCompteRib(c.getNumeroCompteRib());
        dto.setNomBanque(c.getNomBanque());
        dto.setActif(c.getActif());
        dto.setPointDeVenteId(c.getPointDeVenteId());
        dto.setDateCreation(c.getDateCreation());
        return dto;
    }

    private MouvementTresorerieDTO toDto(MouvementTresorerie m) {
        MouvementTresorerieDTO dto = new MouvementTresorerieDTO();
        dto.setId(m.getId());
        dto.setReference(m.getReference());
        dto.setTypeMouvement(m.getTypeMouvement());
        if (m.getCompteSource() != null) {
            dto.setCompteSourceId(m.getCompteSource().getId());
            dto.setCompteSourceNom(m.getCompteSource().getNom());
        }
        if (m.getCompteDestination() != null) {
            dto.setCompteDestinationId(m.getCompteDestination().getId());
            dto.setCompteDestinationNom(m.getCompteDestination().getNom());
        }
        dto.setMontant(m.getMontant());
        dto.setDateMouvement(m.getDateMouvement());
        dto.setMotif(m.getMotif());
        dto.setJustificatifReference(m.getJustificatifReference());
        if (m.getEffectuePar() != null) {
            dto.setEffectueParUserId(m.getEffectuePar().getId());
            dto.setEffectueParNom(m.getEffectuePar().getNomComplet() != null ? m.getEffectuePar().getNomComplet() : m.getEffectuePar().getUsername());
        }
        dto.setSoldeApresSource(m.getSoldeApresSource());
        dto.setSoldeApresDestination(m.getSoldeApresDestination());
        return dto;
    }

    private String genererReferenceMouvement() {
        String prefixe = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = mouvementRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}

package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BanqueDTO;
import com.gestion.persistent.dto.BanqueStatDTO;
import com.gestion.persistent.enums.TypeCompteFinancier;
import com.gestion.persistent.model.Banque;
import com.gestion.persistent.model.CompteFinancier;
import com.gestion.repository.BanqueRepository;
import com.gestion.repository.CompteFinancierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BanqueService {

    private final BanqueRepository banqueRepository;
    private final CompteFinancierRepository compteFinancierRepository;

    public BanqueService(BanqueRepository banqueRepository, CompteFinancierRepository compteFinancierRepository) {
        this.banqueRepository = banqueRepository;
        this.compteFinancierRepository = compteFinancierRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<BanqueDTO> getBanquesActives() {
        Long tenantId = getTenantId();
        List<Banque> banques = banqueRepository.findByPointDeVenteIdAndActifTrueOrderByNomAsc(tenantId);
        if (banques.isEmpty()) {
            banques = banqueRepository.findByActifTrueOrderByNomAsc();
        }
        return banques.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BanqueDTO> getAllBanques() {
        Long tenantId = getTenantId();
        List<Banque> banques = banqueRepository.findByPointDeVenteIdOrderByNomAsc(tenantId);
        if (banques.isEmpty()) {
            banques = banqueRepository.findAll();
        }
        return banques.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BanqueDTO getBanqueById(Long id) {
        Banque b = banqueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banque non trouvée avec l'id : " + id));
        return toDto(b);
    }

    public BanqueDTO creerBanque(BanqueDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la banque est obligatoire");
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            dto.setCode(dto.getNom().toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(6, dto.getNom().length())));
        }

        Banque b = new Banque();
        b.setCode(dto.getCode().trim().toUpperCase());
        b.setNom(dto.getNom().trim());
        b.setNomCourt(dto.getNomCourt());
        b.setSwiftBic(dto.getSwiftBic());
        b.setCouleur(dto.getCouleur() != null ? dto.getCouleur() : "#2563EB");
        b.setLogo(dto.getLogo());
        b.setActif(dto.getActif() != null ? dto.getActif() : true);
        b.setPointDeVenteId(tenantId);
        b.setDateCreation(LocalDateTime.now());

        return toDto(banqueRepository.save(b));
    }

    public BanqueDTO modifierBanque(Long id, BanqueDTO dto) {
        Banque b = banqueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banque non trouvée : " + id));

        if (dto.getNom() != null && !dto.getNom().trim().isEmpty()) {
            b.setNom(dto.getNom().trim());
        }
        if (dto.getCode() != null && !dto.getCode().trim().isEmpty()) {
            b.setCode(dto.getCode().trim().toUpperCase());
        }
        if (dto.getNomCourt() != null) {
            b.setNomCourt(dto.getNomCourt());
        }
        if (dto.getSwiftBic() != null) {
            b.setSwiftBic(dto.getSwiftBic());
        }
        if (dto.getCouleur() != null) {
            b.setCouleur(dto.getCouleur());
        }
        if (dto.getLogo() != null) {
            b.setLogo(dto.getLogo());
        }
        if (dto.getActif() != null) {
            b.setActif(dto.getActif());
        }

        return toDto(banqueRepository.save(b));
    }

    public void supprimerBanque(Long id) {
        Banque b = banqueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banque non trouvée : " + id));
        b.setActif(false);
        banqueRepository.save(b);
    }

    /**
     * Statistiques consolidées par Banque et par Agence
     * Permet d'avoir pour une même banque plusieurs comptes dans des agences différentes
     * avec le total de la banque et le détail par agence.
     */
    @Transactional(readOnly = true)
    public List<BanqueStatDTO> getStatistiquesBanquesEtAgences() {
        Long tenantId = getTenantId();
        List<Banque> banques = banqueRepository.findByPointDeVenteIdAndActifTrueOrderByNomAsc(tenantId);
        if (banques.isEmpty()) {
            banques = banqueRepository.findByActifTrueOrderByNomAsc();
        }

        List<CompteFinancier> tousComptes = compteFinancierRepository.findByTypeAndActifTrue(TypeCompteFinancier.COMPTE_BANCAIRE);

        List<BanqueStatDTO> statsList = new ArrayList<>();

        for (Banque b : banques) {
            BanqueStatDTO stat = new BanqueStatDTO();
            stat.setBanqueId(b.getId());
            stat.setCodeBanque(b.getCode());
            stat.setNomBanque(b.getNom());
            stat.setNomCourt(b.getNomCourt());
            stat.setCouleur(b.getCouleur());
            stat.setLogo(b.getLogo());

            List<CompteFinancier> comptesDeLaBanque = tousComptes.stream()
                    .filter(c -> (c.getBanque() != null && c.getBanque().getId().equals(b.getId()))
                            || (c.getBanque() == null && c.getNomBanque() != null && c.getNomBanque().equalsIgnoreCase(b.getNom())))
                    .collect(Collectors.toList());

            stat.setNombreComptes(comptesDeLaBanque.size());

            BigDecimal total = BigDecimal.ZERO;
            List<BanqueStatDTO.AgenceCompteStatDTO> agences = new ArrayList<>();

            for (CompteFinancier c : comptesDeLaBanque) {
                total = total.add(c.getSoldeActuel() != null ? c.getSoldeActuel() : BigDecimal.ZERO);
                agences.add(new BanqueStatDTO.AgenceCompteStatDTO(
                        c.getId(),
                        c.getCode(),
                        c.getNom(),
                        c.getAgence() != null && !c.getAgence().trim().isEmpty() ? c.getAgence() : "Agence principale",
                        c.getCodeAgence(),
                        c.getNumeroCompteRib(),
                        c.getSoldeActuel(),
                        c.getDevise()
                ));
            }

            stat.setTotalSolde(total);
            stat.setComptesAgences(agences);

            statsList.add(stat);
        }

        return statsList;
    }

    public void initialiserBanquesStandardsSiVide(Long tenantId) {
        if (banqueRepository.count() == 0) {
            List<Banque> standards = List.of(
                    new Banque("AWB", "Attijariwafa Bank", "Attijariwafa", "#D97706"),
                    new Banque("BCP", "Banque Centrale Populaire", "Chaabi", "#EA580C"),
                    new Banque("BOA", "Bank of Africa", "BOA", "#2563EB"),
                    new Banque("CIH", "CIH Bank", "CIH", "#0D9488"),
                    new Banque("SGMB", "Société Générale Maroc", "SGMB", "#DC2626"),
                    new Banque("BMCI", "Banque Marocaine pour le Commerce et l'Industrie", "BMCI", "#16A34A"),
                    new Banque("CDM", "Crédit du Maroc", "CDM", "#0284C7"),
                    new Banque("ALBARID", "Al Barid Bank", "Barid Bank", "#CA8A04")
            );
            for (Banque b : standards) {
                b.setPointDeVenteId(tenantId != null ? tenantId : 1L);
                banqueRepository.save(b);
            }
        }
    }

    private BanqueDTO toDto(Banque b) {
        BanqueDTO dto = new BanqueDTO();
        dto.setId(b.getId());
        dto.setCode(b.getCode());
        dto.setNom(b.getNom());
        dto.setNomCourt(b.getNomCourt());
        dto.setSwiftBic(b.getSwiftBic());
        dto.setLogo(b.getLogo());
        dto.setCouleur(b.getCouleur());
        dto.setActif(b.getActif());
        dto.setPointDeVenteId(b.getPointDeVenteId());
        dto.setDateCreation(b.getDateCreation());
        if (b.getComptes() != null) {
            dto.setNombreComptes(b.getComptes().size());
        }
        return dto;
    }
}

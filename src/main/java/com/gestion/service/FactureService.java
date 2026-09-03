package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.mapper.BonLivraisonClientMapper;
import com.gestion.mapper.FactureMapper;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.FacturationBLRequest;
import com.gestion.persistent.dto.FactureDTO;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.*;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.repository.LigneFactureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FactureService {

    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final FactureMapper factureMapper;
    private final BonLivraisonClientMapper bonLivraisonClientMapper;

    public FactureService(FactureRepository factureRepository,
                          LigneFactureRepository ligneFactureRepository,
                          BonLivraisonClientRepository bonLivraisonClientRepository,
                          ClientRepository clientRepository,
                          UserRepository userRepository,
                          FactureMapper factureMapper,
                          BonLivraisonClientMapper bonLivraisonClientMapper) {
        this.factureRepository = factureRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.factureMapper = factureMapper;
        this.bonLivraisonClientMapper = bonLivraisonClientMapper;
    }

    /**
     * Crée une facture en regroupant un ou plusieurs Bons de Livraison (BLs) choisis manuellement.
     * Les BLs sont associés à la facture et ne pourront plus être refacturés.
     */
    public FactureDTO creerFactureDepuisBonsLivraison(FacturationBLRequest request) {
        if (request.getClientId() == null) {
            throw new IllegalArgumentException("Le clientId est obligatoire");
        }
        if (request.getBonLivraisonIds() == null || request.getBonLivraisonIds().isEmpty()) {
            throw new IllegalArgumentException("Au moins un bon de livraison doit être sélectionné pour la facturation");
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + request.getClientId()));

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }
        if (user == null) {
            user = userRepository.findByUsername("admin")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("Aucun utilisateur disponible")));
        }

        Facture facture = new Facture();
        facture.setClient(client);
        facture.setEmisePar(user);
        facture.setNumeroFacture(genererNumeroFacture());
        LocalDate dateFacture = request.getDateFacture() != null ? request.getDateFacture() : LocalDate.now();
        facture.setDateFacture(dateFacture);

        LocalDate echeance = request.getDateEcheance();
        if (echeance == null && client.getDelaiPaiementJours() != null) {
            echeance = dateFacture.plusDays(client.getDelaiPaiementJours());
        }
        facture.setDateEcheance(echeance);
        facture.setDateCreation(LocalDateTime.now());
        facture.setStatut(StatutFacture.EN_ATTENTE);
        facture.setNotes(request.getNotes());

        List<BonLivraisonClient> blsSelectionnes = new ArrayList<>();
        List<String> blNumeros = new ArrayList<>();

        for (Long blId : request.getBonLivraisonIds()) {
            BonLivraisonClient bl = bonLivraisonClientRepository.findById(blId)
                    .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé avec l'id: " + blId));

            if (bl.getFacture() != null) {
                throw new IllegalStateException("Le bon de livraison " + bl.getNumeroBl() + " est déjà rattaché à la facture " + bl.getFacture().getNumeroFacture());
            }

            if (!bl.getClient().getId().equals(client.getId())) {
                throw new IllegalArgumentException("Le bon de livraison " + bl.getNumeroBl() + " n'appartient pas au client " + client.getNomComplet());
            }

            blsSelectionnes.add(bl);
            blNumeros.add(bl.getNumeroBl());

            // Convertir chaque ligne du BL en ligne de facture
            for (LigneBonLivraisonClient ligneBl : bl.getLignes()) {
                LigneFacture ligneFacture = new LigneFacture();
                ligneFacture.setFacture(facture);
                ligneFacture.setProduit(ligneBl.getProduit());
                ligneFacture.setDesignation(ligneBl.getProduit() != null ? ligneBl.getProduit().getNom() : "Article BL " + bl.getNumeroBl());
                ligneFacture.setReference(ligneBl.getProduit() != null ? ligneBl.getProduit().getReference() : bl.getNumeroBl());
                ligneFacture.setQuantite(ligneBl.getQuantiteLivree() != null ? ligneBl.getQuantiteLivree() : BigDecimal.ONE);
                ligneFacture.setSurfaceM2(ligneBl.getQuantiteLivree());

                BigDecimal pu = ligneBl.getPrixVente();
                if (pu == null && ligneBl.getProduit() != null) {
                    pu = ligneBl.getProduit().getPrixVenteHT() != null ? ligneBl.getProduit().getPrixVenteHT() : ligneBl.getProduit().getPrixVenteTTC();
                }
                ligneFacture.setPrixUnitaireHT(pu != null ? pu : BigDecimal.ZERO);
                ligneFacture.setTauxTVA(new BigDecimal("19.00"));
                ligneFacture.setRemisePourcentage(BigDecimal.ZERO);
                ligneFacture.calculerMontants();

                facture.addLigne(ligneFacture);
            }
        }

        facture.setRemiseGlobale(request.getRemiseGlobale() != null ? request.getRemiseGlobale() : BigDecimal.ZERO);
        facture.calculerMontants();

        if (facture.getNotes() == null || facture.getNotes().isBlank()) {
            facture.setNotes("Facturation des BLs: " + String.join(", ", blNumeros));
        }

        // Sauvegarder la facture
        Facture savedFacture = factureRepository.save(facture);

        // Rattacher les BLs à la facture créée
        for (BonLivraisonClient bl : blsSelectionnes) {
            bl.setFacture(savedFacture);
            bonLivraisonClientRepository.save(bl);
            savedFacture.getBonsLivraison().add(bl);
        }

        return factureMapper.toDto(savedFacture);
    }

    /**
     * Récupère la liste des BLs non encore facturés pour un client donné
     */
    public List<BonLivraisonClientDTO> getBonsLivraisonNonFacturesByClient(Long clientId) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;
        List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByClientIdAndFactureIsNullAndPointDeVenteId(clientId, tenantId);
        return bls.stream()
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les BLs non encore facturés du point de vente
     */
    public List<BonLivraisonClientDTO> getAllBonsLivraisonNonFactures() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;
        List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByFactureIsNullAndPointDeVenteId(tenantId);
        return bls.stream()
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FactureDTO> getAllFactures() {
        return factureRepository.findAll().stream()
                .map(factureMapper::toDto)
                .collect(Collectors.toList());
    }

    public FactureDTO getFactureById(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + id));
        return factureMapper.toDto(facture);
    }

    public List<FactureDTO> getFacturesByClient(Long clientId) {
        return factureRepository.findByClientId(clientId).stream()
                .map(factureMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FactureDTO> getFacturesImpayees() {
        return factureRepository.findFacturesImpayees().stream()
                .map(factureMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FactureDTO> getFacturesEchues() {
        return factureRepository.findFacturesEchues(LocalDate.now()).stream()
                .map(factureMapper::toDto)
                .collect(Collectors.toList());
    }

    public FactureDTO validerFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        if (facture.getLignes().isEmpty()) {
            throw new RuntimeException("Impossible de valider une facture sans lignes");
        }

        facture.setStatut(StatutFacture.VALIDEE);
        return factureMapper.toDto(factureRepository.save(facture));
    }

    public FactureDTO annulerFacture(Long factureId, String motif, Long userId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        if (facture.getAnnulee()) {
            throw new RuntimeException("Cette facture est déjà annulée");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        facture.setAnnulee(true);
        facture.setDateAnnulation(LocalDateTime.now());
        facture.setMotifAnnulation(motif);
        facture.setAnnuleePar(user);
        facture.setStatut(StatutFacture.ANNULEE);

        // Libérer les BLs associés pour qu'ils puissent être refacturés si besoin
        if (facture.getBonsLivraison() != null) {
            for (BonLivraisonClient bl : facture.getBonsLivraison()) {
                bl.setFacture(null);
                bonLivraisonClientRepository.save(bl);
            }
        }

        return factureMapper.toDto(factureRepository.save(facture));
    }

    public FactureDTO creerFacture(FactureDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Facture facture = factureMapper.toEntity(dto);
        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            facture.setClient(client);
        }
        facture.setEmisePar(user);
        facture.setNumeroFacture(genererNumeroFacture());
        LocalDate dateFacture = dto.getDateFacture() != null ? dto.getDateFacture() : LocalDate.now();
        facture.setDateFacture(dateFacture);

        LocalDate echeance = dto.getDateEcheance();
        if (echeance == null && facture.getClient() != null && facture.getClient().getDelaiPaiementJours() != null) {
            echeance = dateFacture.plusDays(facture.getClient().getDelaiPaiementJours());
        }
        facture.setDateEcheance(echeance);

        facture.setDateCreation(LocalDateTime.now());
        facture.setStatut(StatutFacture.EN_ATTENTE);
        facture.calculerMontants();

        return factureMapper.toDto(factureRepository.save(facture));
    }

    private String genererNumeroFacture() {
        String annee = String.valueOf(LocalDate.now().getYear());
        long count = factureRepository.count() + 1;
        return "FACT-" + annee + "-" + String.format("%06d", count);
    }
}

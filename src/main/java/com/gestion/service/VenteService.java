package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.mapper.VenteMapper;
import com.gestion.persistent.dto.LigneVenteDTO;
import com.gestion.persistent.dto.VenteDTO;
import com.gestion.persistent.enums.StatutVente;
import com.gestion.persistent.model.*;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.LigneVenteRepository;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VenteService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ProduitRepository produitRepository;
    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final StockService stockService;
    private final UserRepository userRepository;
    private final VenteMapper venteMapper;

    public VenteService(VenteRepository venteRepository,
                        LigneVenteRepository ligneVenteRepository,
                        ProduitRepository produitRepository,
                        ClientService clientService,
                        ClientRepository clientRepository,
                        StockService stockService,
                        UserRepository userRepository,
                        VenteMapper venteMapper) {
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.produitRepository = produitRepository;
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.stockService = stockService;
        this.userRepository = userRepository;
        this.venteMapper = venteMapper;
    }

    public VenteDTO creerVente(VenteDTO dto, Long vendeurId) {
        User vendeur = userRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé avec l'id: " + vendeurId));

        Vente vente = new Vente();
        vente.setVendeur(vendeur);
        vente.setNumeroTicket(genererNumeroTicket());
        vente.setDateVente(LocalDateTime.now());
        vente.setStatut(StatutVente.EN_COURS);
        vente.setNotes(dto.getNotes());

        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + dto.getClientId()));
            vente.setClient(client);
        }

        if (dto.getLignes() != null) {
            for (LigneVenteDTO ligneDto : dto.getLignes()) {
                LigneVente ligne = new LigneVente();
                ligne.setVente(vente);

                Produit produit = null;
                if (ligneDto.getProduitId() != null) {
                    produit = produitRepository.findById(ligneDto.getProduitId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + ligneDto.getProduitId()));
                    ligne.setProduit(produit);
                }

                ligne.setDesignation(ligneDto.getDesignation() != null ? ligneDto.getDesignation() : (produit != null ? produit.getNom() : "Article"));
                ligne.setReference(ligneDto.getReference() != null ? ligneDto.getReference() : (produit != null ? produit.getReference() : ""));
                ligne.setQuantite(ligneDto.getQuantite() != null ? ligneDto.getQuantite() : BigDecimal.ONE);
                ligne.setSurfaceM2(ligneDto.getSurfaceM2() != null ? ligneDto.getSurfaceM2() : ligne.getQuantite());

                BigDecimal pu = ligneDto.getPrixUnitaireHT();
                if (pu == null && produit != null) {
                    pu = produit.getPrixVenteHt() != null ? produit.getPrixVenteHt() : produit.getPrixVenteTtc();
                }
                ligne.setPrixUnitaireHT(pu != null ? pu : BigDecimal.ZERO);
                ligne.setTauxTVA(ligneDto.getTauxTVA() != null ? ligneDto.getTauxTVA() : new BigDecimal("19.00"));
                ligne.setRemisePourcentage(ligneDto.getRemisePourcentage() != null ? ligneDto.getRemisePourcentage() : BigDecimal.ZERO);
                ligne.calculerMontants();

                vente.addLigne(ligne);
            }
        }

        BigDecimal remiseGlobale = dto.getRemiseGlobale();
        if ((remiseGlobale == null || remiseGlobale.compareTo(BigDecimal.ZERO) == 0) && vente.getClient() != null && vente.getClient().getRemiseDefaut() != null) {
            remiseGlobale = vente.getClient().getRemiseDefaut();
        }
        vente.setRemiseGlobale(remiseGlobale != null ? remiseGlobale : BigDecimal.ZERO);
        vente.calculerMontants();

        Vente saved = venteRepository.save(vente);
        return venteMapper.toDto(saved);
    }

    public VenteDTO validerVente(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + venteId));

        if (vente.getLignes().isEmpty()) {
            throw new RuntimeException("Impossible de valider une vente sans lignes");
        }

        // Déduire le stock
        for (LigneVente ligne : vente.getLignes()) {
            if (ligne.getProduit() != null) {
                stockService.sortieStock(ligne.getProduit().getId(), ligne.getQuantite(),
                        "Vente " + vente.getNumeroTicket());
            }
        }

        vente.setStatut(StatutVente.VALIDEE);

        // Mettre à jour la date de dernière visite du client
        if (vente.getClient() != null) {
            clientService.mettreAJourDerniereVisite(vente.getClient().getId());
        }

        Vente saved = venteRepository.save(vente);
        return venteMapper.toDto(saved);
    }

    public VenteDTO annulerVente(Long venteId, String motif, Long userId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + venteId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + userId));

        // Remettre le stock si la vente était validée
        if (vente.getStatut() == StatutVente.VALIDEE) {
            for (LigneVente ligne : vente.getLignes()) {
                if (ligne.getProduit() != null) {
                    stockService.entreeStock(ligne.getProduit().getId(), ligne.getQuantite(),
                            "Annulation vente " + vente.getNumeroTicket());
                }
            }
        }

        vente.setStatut(StatutVente.ANNULEE);
        vente.setDateAnnulation(LocalDateTime.now());
        vente.setMotifAnnulation(motif);
        vente.setAnnulePar(user);

        Vente saved = venteRepository.save(vente);
        return venteMapper.toDto(saved);
    }

    public Vente getVenteEntityById(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + id));
    }

    public VenteDTO getVenteById(Long id) {
        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + id));
        return venteMapper.toDto(vente);
    }

    public List<VenteDTO> getAllVentes() {
        return venteRepository.findAll().stream()
                .map(venteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<VenteDTO> getVentesByClient(Long clientId) {
        return venteRepository.findByClientId(clientId).stream()
                .map(venteMapper::toDto)
                .collect(Collectors.toList());
    }

    public VenteDTO appliquerRemiseGlobale(Long venteId, BigDecimal remise) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + venteId));
        vente.setRemiseGlobale(remise);
        vente.calculerMontants();
        Vente saved = venteRepository.save(vente);
        return venteMapper.toDto(saved);
    }

    private String genererNumeroTicket() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = venteRepository.count() + 1;
        return "TK-" + dateStr + "-" + String.format("%06d", count);
    }
}

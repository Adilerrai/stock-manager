package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.UserRepository;
import com.gestion.mapper.DevisMapper;
import com.gestion.persistent.dto.DevisDTO;
import com.gestion.persistent.dto.LigneDevisDTO;
import com.gestion.persistent.dto.LigneReservationClientDTO;
import com.gestion.persistent.dto.ReservationClientDTO;
import com.gestion.persistent.dto.ReservationClientSearchCriteria;
import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.enums.StatutReservationClient;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
public class ReservationClientService {

    private final ReservationClientRepository reservationClientRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final DevisService devisService;
    private final DevisRepository devisRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final DevisMapper devisMapper;

    public ReservationClientService(ReservationClientRepository reservationClientRepository,
                                    ClientRepository clientRepository,
                                    ProduitRepository produitRepository,
                                    UserRepository userRepository,
                                    DevisService devisService,
                                    DevisRepository devisRepository,
                                    CommandeClientRepository commandeClientRepository,
                                    DevisMapper devisMapper) {
        this.reservationClientRepository = reservationClientRepository;
        this.clientRepository = clientRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.devisService = devisService;
        this.devisRepository = devisRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.devisMapper = devisMapper;
    }

    @Transactional(readOnly = true)
    public Page<ReservationClientDTO> searchReservations(ReservationClientSearchCriteria criteria, Pageable pageable) {
        return reservationClientRepository.findByCriteria(criteria, pageable)
                .map(this::toDto);
    }

    public ReservationClientDTO creerReservation(ReservationClientDTO dto, Long forcedClientId, Long userId) {
        Long clientId = forcedClientId != null ? forcedClientId : dto.getClientId();
        if (clientId == null) {
            throw new CommonException("Le client est obligatoire pour créer une réservation d'articles.", HttpStatus.BAD_REQUEST);
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CommonException("Client introuvable avec l'id: " + clientId, HttpStatus.NOT_FOUND));

        if (dto.getLignes() == null || dto.getLignes().isEmpty()) {
            throw new CommonException("Veuillez sélectionner au moins un article pour créer une réservation.", HttpStatus.BAD_REQUEST);
        }

        Long tenantId = client.getPointDeVenteId() != null
                ? client.getPointDeVenteId()
                : (TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : 1L);

        ReservationClient reservation = new ReservationClient();
        reservation.setClient(client);
        reservation.setPointDeVenteId(tenantId);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateLivraisonSouhaitee(dto.getDateLivraisonSouhaitee());
        reservation.setObservationsClient(dto.getObservationsClient());
        reservation.setNotesTenant(dto.getNotesTenant());
        reservation.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutReservationClient.SOUMISE);
        reservation.setNumeroReservation(genererNumeroReservation(tenantId));

        if (userId != null) {
            userRepository.findById(userId).ifPresent(reservation::setCreePar);
        }

        for (LigneReservationClientDTO lDto : dto.getLignes()) {
            if (lDto.getProduitId() == null) {
                throw new CommonException("L'identifiant du produit est obligatoire sur chaque ligne de réservation.", HttpStatus.BAD_REQUEST);
            }
            Produit produit = produitRepository.findById(lDto.getProduitId())
                    .orElseThrow(() -> new CommonException("Article introuvable ID: " + lDto.getProduitId(), HttpStatus.NOT_FOUND));

            BigDecimal qte = lDto.getQuantiteDemandee() != null ? lDto.getQuantiteDemandee() : BigDecimal.ONE;
            if (qte.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("La quantité demandée doit être supérieure à 0.", HttpStatus.BAD_REQUEST);
            }

            LigneReservationClient ligne = new LigneReservationClient();
            ligne.setProduit(produit);
            ligne.setQuantiteDemandee(qte);
            ligne.setRemarque(lDto.getRemarque());
            reservation.addLigne(ligne);
        }

        ReservationClient saved = reservationClientRepository.save(reservation);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationClientDTO> getReservationsDuTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        return reservationClientRepository.findByPointDeVenteIdOrderByDateReservationDesc(effectiveTenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationClientDTO> getReservationsDuClient(Long clientId) {
        return reservationClientRepository.findByClientIdOrderByDateReservationDesc(clientId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationClientDTO getReservationById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        ReservationClient res = (tenantId != null)
                ? reservationClientRepository.findByIdAndPointDeVenteId(id, tenantId)
                    .or(() -> reservationClientRepository.findById(id))
                    .orElseThrow(() -> new CommonException("Réservation introuvable avec l'id: " + id, HttpStatus.NOT_FOUND))
                : reservationClientRepository.findById(id)
                    .orElseThrow(() -> new CommonException("Réservation introuvable avec l'id: " + id, HttpStatus.NOT_FOUND));
        return toDto(res);
    }

    public ReservationClientDTO changerStatut(Long id, StatutReservationClient statut, String notesTenant) {
        ReservationClient res = reservationClientRepository.findById(id)
                .orElseThrow(() -> new CommonException("Réservation introuvable avec l'id: " + id, HttpStatus.NOT_FOUND));
        if (statut != null) {
            res.setStatut(statut);
        }
        if (notesTenant != null) {
            res.setNotesTenant(notesTenant);
        }
        return toDto(reservationClientRepository.save(res));
    }

    public ReservationClientDTO annulerReservationParClient(Long id, Long clientId) {
        ReservationClient res = reservationClientRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new CommonException("Réservation introuvable.", HttpStatus.NOT_FOUND));

        if (res.getStatut() != StatutReservationClient.BROUILLON && res.getStatut() != StatutReservationClient.SOUMISE) {
            throw new CommonException("Impossible d'annuler cette réservation car elle fait déjà l'objet d'un devis ou d'une commande.", HttpStatus.BAD_REQUEST);
        }
        res.setStatut(StatutReservationClient.ANNULEE);
        return toDto(reservationClientRepository.save(res));
    }

    /**
     * Permet au Tenant de générer un Devis à partir de la réservation d'articles d'un client.
     */
    public DevisDTO creerDevisDepuisReservation(Long reservationId, DevisDTO customDevisDto, Long userId) {
        ReservationClient res = reservationClientRepository.findById(reservationId)
                .orElseThrow(() -> new CommonException("Réservation client introuvable avec l'id: " + reservationId, HttpStatus.NOT_FOUND));

        if (res.getStatut() == StatutReservationClient.ANNULEE) {
            throw new CommonException("Impossible de créer un devis sur une réservation annulée.", HttpStatus.BAD_REQUEST);
        }

        DevisDTO devisRequest = (customDevisDto != null) ? customDevisDto : new DevisDTO();
        devisRequest.setClientId(res.getClient().getId());
        devisRequest.setReservationClientId(res.getId());
        devisRequest.setPointDeVenteId(res.getPointDeVenteId());
        if (devisRequest.getDateDevis() == null) {
            devisRequest.setDateDevis(LocalDate.now());
        }
        if (devisRequest.getDateValidite() == null) {
            devisRequest.setDateValidite(LocalDate.now().plusDays(30));
        }
        if (devisRequest.getStatut() == null) {
            devisRequest.setStatut(StatutDevis.ENVOYE);
        }
        if (devisRequest.getNotes() == null || devisRequest.getNotes().isBlank()) {
            devisRequest.setNotes("Devis établi suite à la réservation " + res.getNumeroReservation()
                    + (res.getObservationsClient() != null && !res.getObservationsClient().isBlank() ? " - " + res.getObservationsClient() : ""));
        }

        // Si le tenant n'a pas passé de lignes personnalisées, on reprend toutes les lignes de la réservation avec les tarifs du produit/client
        if (devisRequest.getLignes() == null || devisRequest.getLignes().isEmpty()) {
            List<LigneDevisDTO> lignesDevis = new ArrayList<>();
            BigDecimal remiseClient = res.getClient().getRemiseDefaut() != null ? res.getClient().getRemiseDefaut() : BigDecimal.ZERO;

            for (LigneReservationClient lr : res.getLignes()) {
                Produit p = lr.getProduit();
                LigneDevisDTO ld = new LigneDevisDTO();
                ld.setProduitId(p.getId());
                ld.setQuantite(lr.getQuantiteDemandee());
                BigDecimal puHt = p.getPrixVenteHt() != null ? p.getPrixVenteHt() : (p.getPrixVente() != null ? p.getPrixVente() : BigDecimal.ZERO);
                ld.setPrixUnitaireHT(puHt);
                ld.setTauxTVA(BigDecimal.valueOf(20));
                ld.setTauxRemise(remiseClient);
                ld.setDescription(lr.getRemarque() != null && !lr.getRemarque().isBlank()
                        ? (p.getDesignation() != null ? p.getDesignation() : p.getDescription()) + " (" + lr.getRemarque() + ")"
                        : (p.getDesignation() != null ? p.getDesignation() : p.getDescription()));
                lignesDevis.add(ld);
            }
            devisRequest.setLignes(lignesDevis);
        }

        Devis devisCree = devisService.creerDevis(devisRequest, userId);

        res.setDevisGenereId(devisCree.getId());
        if (devisCree.getStatut() == StatutDevis.ENVOYE) {
            res.setStatut(StatutReservationClient.DEVIS_ENVOYE);
        } else {
            res.setStatut(StatutReservationClient.EN_COURS_DEVIS);
        }
        reservationClientRepository.save(res);

        return devisService.enrichirDevisDto(devisMapper.toDto(devisCree));
    }

    private String genererNumeroReservation(Long tenantId) {
        int year = LocalDate.now().getYear();
        long count = reservationClientRepository.findByPointDeVenteIdOrderByDateReservationDesc(tenantId).size() + 1;
        String candidate;
        int attempts = 0;
        do {
            candidate = String.format("RES-%d-%04d", year, count + attempts);
            attempts++;
        } while (reservationClientRepository.findByPointDeVenteIdAndNumeroReservation(tenantId, candidate).isPresent() && attempts < 1000);
        return candidate;
    }

    public ReservationClientDTO toDto(ReservationClient entity) {
        if (entity == null) return null;
        ReservationClientDTO dto = new ReservationClientDTO();
        dto.setId(entity.getId());
        dto.setNumeroReservation(entity.getNumeroReservation());
        dto.setDateReservation(entity.getDateReservation());
        dto.setDateLivraisonSouhaitee(entity.getDateLivraisonSouhaitee());
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
            dto.setClientNom(entity.getClient().getNomComplet() != null ? entity.getClient().getNomComplet() : entity.getClient().getNom());
            dto.setClientIce(entity.getClient().getIce());
            dto.setClientTelephone(entity.getClient().getTelephone());
        }
        if (entity.getCreePar() != null) {
            dto.setCreeParId(entity.getCreePar().getId());
            dto.setCreeParNom(entity.getCreePar().getNomComplet() != null ? entity.getCreePar().getNomComplet() : entity.getCreePar().getUsername());
        }
        dto.setStatut(entity.getStatut());
        dto.setObservationsClient(entity.getObservationsClient());
        dto.setNotesTenant(entity.getNotesTenant());
        dto.setDevisGenereId(entity.getDevisGenereId());
        if (entity.getDevisGenereId() != null) {
            devisRepository.findById(entity.getDevisGenereId()).ifPresent(d -> {
                dto.setNumeroDevisGenere(d.getNumeroDevis());
                dto.setStatutDevisGenere(d.getStatut() != null ? d.getStatut().name() : null);
                if (dto.getCommandeGenereeId() == null && d.getCommandeGenereeId() != null) {
                    dto.setCommandeGenereeId(d.getCommandeGenereeId());
                }
            });
        }
        if (entity.getCommandeGenereeId() != null) {
            dto.setCommandeGenereeId(entity.getCommandeGenereeId());
        }
        if (dto.getCommandeGenereeId() != null) {
            commandeClientRepository.findById(dto.getCommandeGenereeId())
                    .ifPresent(c -> dto.setNumeroCommandeGeneree(c.getNumeroCommande()));
        }
        dto.setPointDeVenteId(entity.getPointDeVenteId());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        List<LigneReservationClientDTO> lignesDto = new ArrayList<>();
        if (entity.getLignes() != null) {
            for (LigneReservationClient l : entity.getLignes()) {
                LigneReservationClientDTO lDto = new LigneReservationClientDTO();
                lDto.setId(l.getId());
                if (l.getProduit() != null) {
                    lDto.setProduitId(l.getProduit().getId());
                    lDto.setProduitReference(l.getProduit().getReference());
                    lDto.setProduitDesignation(l.getProduit().getDesignation() != null ? l.getProduit().getDesignation() : l.getProduit().getDescription());
                    lDto.setProduitDescription(l.getProduit().getDescription());
                    lDto.setCategorieNom(l.getProduit().getCategorieArticle());
                    lDto.setUniteMesure(l.getProduit().getUniteMesureStock() != null ? l.getProduit().getUniteMesureStock().name() : null);
                }
                lDto.setQuantiteDemandee(l.getQuantiteDemandee());
                lDto.setRemarque(l.getRemarque());
                lignesDto.add(lDto);
            }
        }
        dto.setLignes(lignesDto);
        return dto;
    }
}

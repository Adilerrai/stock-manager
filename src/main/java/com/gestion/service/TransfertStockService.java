package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.LigneTransfertStockDTO;
import com.gestion.persistent.dto.TransfertStockDTO;
import com.gestion.persistent.enums.StatutTransfert;
import com.gestion.persistent.enums.TypeMouvement;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransfertStockService {

    private final TransfertStockRepository transfertRepository;
    private final DepotRepository depotRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final MouvementStockRepository mouvementStockRepository;

    public TransfertStockService(TransfertStockRepository transfertRepository,
                                 DepotRepository depotRepository,
                                 ProduitRepository produitRepository,
                                 UserRepository userRepository,
                                 MouvementStockRepository mouvementStockRepository) {
        this.transfertRepository = transfertRepository;
        this.depotRepository = depotRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.mouvementStockRepository = mouvementStockRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<TransfertStockDTO> getAllTransferts(StatutTransfert statut) {
        Long tenantId = getTenantId();
        List<TransfertStock> liste;
        if (statut != null) {
            liste = transfertRepository.findByPointDeVenteIdAndStatutOrderByDateTransfertDesc(tenantId, statut);
        } else {
            liste = transfertRepository.findByPointDeVenteIdOrderByDateTransfertDescIdDesc(tenantId);
        }
        return liste.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransfertStockDTO getTransfertById(Long id) {
        TransfertStock t = transfertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable: " + id));
        return toDto(t);
    }

    public TransfertStockDTO creerTransfert(TransfertStockDTO dto, Long userId) {
        Long tenantId = getTenantId();

        if (dto.getDepotSourceId() == null || dto.getDepotDestinationId() == null) {
            throw new IllegalArgumentException("Les dépôts source et destination sont obligatoires");
        }
        if (Objects.equals(dto.getDepotSourceId(), dto.getDepotDestinationId())) {
            throw new IllegalArgumentException("Le dépôt source et le dépôt destination doivent être distincts");
        }
        if (dto.getLignes() == null || dto.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Le transfert doit contenir au moins un produit");
        }

        Depot source = depotRepository.findById(dto.getDepotSourceId())
            .orElseThrow(() -> new IllegalArgumentException("Dépôt source introuvable: " + dto.getDepotSourceId()));
        Depot destination = depotRepository.findById(dto.getDepotDestinationId())
            .orElseThrow(() -> new IllegalArgumentException("Dépôt destination introuvable: " + dto.getDepotDestinationId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        TransfertStock transfert = new TransfertStock();
        transfert.setDepotSource(source);
        transfert.setDepotDestination(destination);
        transfert.setDateTransfert(dto.getDateTransfert() != null ? dto.getDateTransfert() : LocalDate.now());
        transfert.setMotif(dto.getMotif());
        transfert.setCreePar(user);
        transfert.setPointDeVenteId(tenantId);
        transfert.setDateCreation(LocalDateTime.now());
        transfert.setStatut(StatutTransfert.BROUILLON);

        String num = (dto.getNumeroTransfert() != null && !dto.getNumeroTransfert().trim().isEmpty())
            ? dto.getNumeroTransfert().trim()
            : genererNumeroTransfert(tenantId);
        transfert.setNumeroTransfert(num);

        for (LigneTransfertStockDTO lDto : dto.getLignes()) {
            if (lDto.getProduitId() == null) continue;
            Produit produit = produitRepository.findById(lDto.getProduitId())
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable ID: " + lDto.getProduitId()));
            BigDecimal qte = lDto.getQuantite() != null && lDto.getQuantite().compareTo(BigDecimal.ZERO) > 0
                ? lDto.getQuantite() : BigDecimal.ONE;

            transfert.addLigne(new LigneTransfertStock(produit, qte, lDto.getNotes(), tenantId));
        }

        TransfertStock saved = transfertRepository.save(transfert);
        return toDto(saved);
    }

    public TransfertStockDTO expedierTransfert(Long id, Long userId) {
        TransfertStock transfert = transfertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable: " + id));

        if (transfert.getStatut() != StatutTransfert.BROUILLON) {
            throw new IllegalStateException("Seul un transfert en statut BROUILLON peut être expédié");
        }

        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;
        transfert.setValidePar(user);
        transfert.setStatut(StatutTransfert.EN_TRANSIT);

        // Sortie de stock du dépôt source
        for (LigneTransfertStock ligne : transfert.getLignes()) {
            Produit p = ligne.getProduit();
            MouvementStock mvt = new MouvementStock();
            mvt.setProduit(p);
            mvt.setTypeMouvement(TypeMouvement.TRANSFERT_SORTIE);
            mvt.setQuantite(ligne.getQuantite());
            mvt.setReferenceDocument(transfert.getNumeroTransfert());
            mvt.setMotif("Expédition transfert vers " + transfert.getDepotDestination().getNom());
            mvt.setDateMouvement(LocalDateTime.now());
            mvt.setUtilisateur(user != null ? (user.getNomComplet() != null ? user.getNomComplet() : user.getUsername()) : "Système");
            mvt.setDepot(transfert.getDepotSource());

            mouvementStockRepository.save(mvt);
        }

        return toDto(transfertRepository.save(transfert));
    }

    public TransfertStockDTO recevoirTransfert(Long id, Long userId) {
        TransfertStock transfert = transfertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable: " + id));

        if (transfert.getStatut() != StatutTransfert.EN_TRANSIT) {
            throw new IllegalStateException("Seul un transfert EN_TRANSIT peut être réceptionné");
        }

        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;
        transfert.setStatut(StatutTransfert.RECU_VALIDE);
        transfert.setDateReception(LocalDate.now());

        // Entrée en stock sur le dépôt de destination
        for (LigneTransfertStock ligne : transfert.getLignes()) {
            Produit p = ligne.getProduit();
            MouvementStock mvt = new MouvementStock();
            mvt.setProduit(p);
            mvt.setTypeMouvement(TypeMouvement.TRANSFERT_ENTREE);
            mvt.setQuantite(ligne.getQuantite());
            mvt.setReferenceDocument(transfert.getNumeroTransfert());
            mvt.setMotif("Réception transfert depuis " + transfert.getDepotSource().getNom());
            mvt.setDateMouvement(LocalDateTime.now());
            mvt.setUtilisateur(user != null ? (user.getNomComplet() != null ? user.getNomComplet() : user.getUsername()) : "Système");
            mvt.setDepot(transfert.getDepotDestination());

            mouvementStockRepository.save(mvt);
        }

        return toDto(transfertRepository.save(transfert));
    }

    public TransfertStockDTO annulerTransfert(Long id, Long userId) {
        TransfertStock transfert = transfertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable: " + id));

        if (transfert.getStatut() == StatutTransfert.RECU_VALIDE) {
            throw new IllegalStateException("Impossible d'annuler un transfert déjà réceptionné et validé");
        }

        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;

        // Si était déjà expédié (en transit), on remet le stock à la source
        if (transfert.getStatut() == StatutTransfert.EN_TRANSIT) {
            for (LigneTransfertStock ligne : transfert.getLignes()) {
                Produit p = ligne.getProduit();
                MouvementStock mvt = new MouvementStock();
                mvt.setProduit(p);
                mvt.setTypeMouvement(TypeMouvement.AJUSTEMENT_POSITIF);
                mvt.setQuantite(ligne.getQuantite());
                mvt.setReferenceDocument(transfert.getNumeroTransfert());
                mvt.setMotif("Annulation du transfert en transit " + transfert.getNumeroTransfert());
                mvt.setDateMouvement(LocalDateTime.now());
                mvt.setUtilisateur(user != null ? (user.getNomComplet() != null ? user.getNomComplet() : user.getUsername()) : "Système");
                mvt.setDepot(transfert.getDepotSource());

                mouvementStockRepository.save(mvt);
            }
        }

        transfert.setStatut(StatutTransfert.ANNULE);
        return toDto(transfertRepository.save(transfert));
    }

    private String genererNumeroTransfert(Long tenantId) {
        int year = LocalDate.now().getYear();
        String prefix = "TRF-" + year + "-";
        Long count = transfertRepository.countByPrefixAndTenant(prefix, tenantId);
        return String.format("%s%05d", prefix, (count != null ? count : 0) + 1);
    }

    private TransfertStockDTO toDto(TransfertStock t) {
        TransfertStockDTO dto = new TransfertStockDTO();
        dto.setId(t.getId());
        dto.setNumeroTransfert(t.getNumeroTransfert());
        dto.setDateTransfert(t.getDateTransfert());
        dto.setDateReception(t.getDateReception());
        dto.setStatut(t.getStatut());
        dto.setMotif(t.getMotif());

        if (t.getDepotSource() != null) {
            dto.setDepotSourceId(t.getDepotSource().getId());
            dto.setDepotSourceNom(t.getDepotSource().getNom());
        }
        if (t.getDepotDestination() != null) {
            dto.setDepotDestinationId(t.getDepotDestination().getId());
            dto.setDepotDestinationNom(t.getDepotDestination().getNom());
        }
        if (t.getCreePar() != null) {
            dto.setCreeParUserId(t.getCreePar().getId());
            dto.setCreeParNom(t.getCreePar().getNomComplet() != null ? t.getCreePar().getNomComplet() : t.getCreePar().getUsername());
        }
        if (t.getValidePar() != null) {
            dto.setValideParUserId(t.getValidePar().getId());
            dto.setValideParNom(t.getValidePar().getNomComplet() != null ? t.getValidePar().getNomComplet() : t.getValidePar().getUsername());
        }

        if (t.getLignes() != null) {
            List<LigneTransfertStockDTO> lignesDto = new ArrayList<>();
            for (LigneTransfertStock l : t.getLignes()) {
                LigneTransfertStockDTO lDto = new LigneTransfertStockDTO();
                lDto.setId(l.getId());
                lDto.setQuantite(l.getQuantite());
                lDto.setNotes(l.getNotes());
                if (l.getProduit() != null) {
                    lDto.setProduitId(l.getProduit().getId());
                    lDto.setProduitReference(l.getProduit().getReference());
                    lDto.setProduitDesignation(l.getProduit().getDesignation());
                }
                lignesDto.add(lDto);
            }
            dto.setLignes(lignesDto);
        }

        return dto;
    }
}

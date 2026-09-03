package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.PromessePaiementDTO;
import com.gestion.persistent.dto.RelanceClientDTO;
import com.gestion.persistent.enums.CanalRelance;
import com.gestion.persistent.enums.StatutPromesse;
import com.gestion.persistent.model.*;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.repository.PromessePaiementRepository;
import com.gestion.repository.RelanceClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecouvrementService {

    private final RelanceClientRepository relanceRepository;
    private final PromessePaiementRepository promesseRepository;
    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;
    private final UserRepository userRepository;

    public RecouvrementService(RelanceClientRepository relanceRepository,
                               PromessePaiementRepository promesseRepository,
                               ClientRepository clientRepository,
                               FactureRepository factureRepository,
                               UserRepository userRepository) {
        this.relanceRepository = relanceRepository;
        this.promesseRepository = promesseRepository;
        this.clientRepository = clientRepository;
        this.factureRepository = factureRepository;
        this.userRepository = userRepository;
    }

    /**
     * Enregistre une relance client (Appel, Email, Visite...)
     */
    public RelanceClientDTO enregistrerRelance(RelanceClientDTO dto, Long userId) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        Facture facture = null;
        if (dto.getFactureId() != null) {
            facture = factureRepository.findById(dto.getFactureId()).orElse(null);
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        RelanceClient r = new RelanceClient();
        r.setClient(client);
        r.setFacture(facture);
        r.setDateRelance(dto.getDateRelance() != null ? dto.getDateRelance() : LocalDateTime.now());
        r.setCanal(dto.getCanal() != null ? dto.getCanal() : CanalRelance.TELEPHONE);
        r.setInterlocuteur(dto.getInterlocuteur());
        r.setCommentaire(dto.getCommentaire() != null ? dto.getCommentaire() : "Relance sans commentaire");
        r.setEffectuePar(user);

        Long tenantId = TenantContext.getCurrentTenant();
        r.setPointDeVenteId(tenantId != null ? tenantId : 1L);

        RelanceClient saved = relanceRepository.save(r);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RelanceClientDTO> getRelancesParClient(Long clientId) {
        return relanceRepository.findByClientIdOrderByDateRelanceDesc(clientId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Enregistre une promesse formelle de paiement du client
     */
    public PromessePaiementDTO enregistrerPromesse(PromessePaiementDTO dto, Long userId) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        Facture facture = null;
        if (dto.getFactureId() != null) {
            facture = factureRepository.findById(dto.getFactureId()).orElse(null);
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        PromessePaiement p = new PromessePaiement();
        p.setClient(client);
        p.setFacture(facture);
        p.setDatePromesse(LocalDateTime.now());
        p.setDateEcheancePromise(dto.getDateEcheancePromise() != null ? dto.getDateEcheancePromise() : LocalDate.now().plusDays(7));
        p.setMontantPromis(dto.getMontantPromis() != null ? dto.getMontantPromis() : BigDecimal.ZERO);
        p.setStatut(StatutPromesse.EN_ATTENTE);
        p.setNotes(dto.getNotes());
        p.setEnregistrePar(user);

        Long tenantId = TenantContext.getCurrentTenant();
        p.setPointDeVenteId(tenantId != null ? tenantId : 1L);

        PromessePaiement saved = promesseRepository.save(p);
        return toDto(saved);
    }

    public PromessePaiementDTO marquerStatutPromesse(Long promesseId, StatutPromesse statut) {
        PromessePaiement p = promesseRepository.findById(promesseId)
                .orElseThrow(() -> new RuntimeException("Promesse de paiement non trouvée"));
        p.setStatut(statut);
        return toDto(promesseRepository.save(p));
    }

    /**
     * Retourne toutes les promesses de paiement en attente avec calcul automatique des retards
     */
    public List<PromessePaiementDTO> getPromessesEnCours() {
        actualiserPromessesDepassees();
        return promesseRepository.findByStatutOrderByDateEcheancePromiseAsc(StatutPromesse.EN_ATTENTE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les promesses de paiement non honorées (rompues)
     */
    public List<PromessePaiementDTO> getPromessesRompues() {
        actualiserPromessesDepassees();
        return promesseRepository.findByStatutOrderByDateEcheancePromiseAsc(StatutPromesse.ROMPUE_NON_TENUE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromessePaiementDTO> getPromessesParClient(Long clientId) {
        return promesseRepository.findByClientIdOrderByDateEcheancePromiseDesc(clientId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Marque automatiquement comme ROMPUE les promesses dont l'échéance est passée
     */
    private void actualiserPromessesDepassees() {
        List<PromessePaiement> depassees = promesseRepository.findPromessesDepassees(LocalDate.now());
        for (PromessePaiement p : depassees) {
            p.setStatut(StatutPromesse.ROMPUE_NON_TENUE);
            promesseRepository.save(p);
        }
    }

    private RelanceClientDTO toDto(RelanceClient r) {
        RelanceClientDTO dto = new RelanceClientDTO();
        dto.setId(r.getId());
        if (r.getClient() != null) {
            dto.setClientId(r.getClient().getId());
            dto.setClientNom(r.getClient().getNomComplet() != null ? r.getClient().getNomComplet() : r.getClient().getNom());
        }
        if (r.getFacture() != null) {
            dto.setFactureId(r.getFacture().getId());
            dto.setFactureNumero(r.getFacture().getNumeroFacture());
        }
        dto.setDateRelance(r.getDateRelance());
        dto.setCanal(r.getCanal());
        dto.setInterlocuteur(r.getInterlocuteur());
        dto.setCommentaire(r.getCommentaire());
        if (r.getEffectuePar() != null) {
            dto.setEffectueParUserId(r.getEffectuePar().getId());
            dto.setEffectueParNom(r.getEffectuePar().getNomComplet() != null ? r.getEffectuePar().getNomComplet() : r.getEffectuePar().getUsername());
        }
        return dto;
    }

    private PromessePaiementDTO toDto(PromessePaiement p) {
        PromessePaiementDTO dto = new PromessePaiementDTO();
        dto.setId(p.getId());
        if (p.getClient() != null) {
            dto.setClientId(p.getClient().getId());
            dto.setClientNom(p.getClient().getNomComplet() != null ? p.getClient().getNomComplet() : p.getClient().getNom());
        }
        if (p.getFacture() != null) {
            dto.setFactureId(p.getFacture().getId());
            dto.setFactureNumero(p.getFacture().getNumeroFacture());
        }
        dto.setDatePromesse(p.getDatePromesse());
        dto.setDateEcheancePromise(p.getDateEcheancePromise());
        dto.setMontantPromis(p.getMontantPromis());
        dto.setStatut(p.getStatut());
        dto.setNotes(p.getNotes());
        if (p.getEnregistrePar() != null) {
            dto.setEnregistreParUserId(p.getEnregistrePar().getId());
            dto.setEnregistreParNom(p.getEnregistrePar().getNomComplet() != null ? p.getEnregistrePar().getNomComplet() : p.getEnregistrePar().getUsername());
        }
        if (p.getDateEcheancePromise() != null && p.getDateEcheancePromise().isBefore(LocalDate.now()) && p.getStatut() != StatutPromesse.HONOREE_TENUE) {
            dto.setEstEnRetard(true);
        }
        return dto;
    }
}

package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.enums.TypeUtilisateur;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.mapper.BonLivraisonClientMapper;
import com.gestion.mapper.CategorieMapper;
import com.gestion.mapper.CommandeClientMapper;
import com.gestion.mapper.DevisMapper;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.enums.StatutReservationClient;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service dédié au Portail "Espace Client".
 * Garantit que le client connecté ne voit que ses propres documents (Réservations, Devis, Commandes, Bons de Livraison)
 * et que le Catalogue Produit ne divulgue AUCUN prix ni AUCUNE disponibilité/stock.
 */
@Service
@Transactional
public class EspaceClientPortalService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final CategorieMapper categorieMapper;
    private final ReservationClientService reservationClientService;
    private final ReservationClientRepository reservationClientRepository;
    private final DevisRepository devisRepository;
    private final DevisService devisService;
    private final DevisMapper devisMapper;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeClientMapper commandeClientMapper;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final BonLivraisonClientMapper bonLivraisonClientMapper;
    private final EntrepriseProfileRepository entrepriseProfileRepository;

    public EspaceClientPortalService(ClientRepository clientRepository,
                                     UserRepository userRepository,
                                     ProduitRepository produitRepository,
                                     CategorieRepository categorieRepository,
                                     CategorieMapper categorieMapper,
                                     ReservationClientService reservationClientService,
                                     ReservationClientRepository reservationClientRepository,
                                     DevisRepository devisRepository,
                                     DevisService devisService,
                                     DevisMapper devisMapper,
                                     CommandeClientRepository commandeClientRepository,
                                     CommandeClientMapper commandeClientMapper,
                                     BonLivraisonClientRepository bonLivraisonClientRepository,
                                     BonLivraisonClientMapper bonLivraisonClientMapper,
                                     EntrepriseProfileRepository entrepriseProfileRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.categorieMapper = categorieMapper;
        this.reservationClientService = reservationClientService;
        this.reservationClientRepository = reservationClientRepository;
        this.devisRepository = devisRepository;
        this.devisService = devisService;
        this.devisMapper = devisMapper;
        this.commandeClientRepository = commandeClientRepository;
        this.commandeClientMapper = commandeClientMapper;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.bonLivraisonClientMapper = bonLivraisonClientMapper;
        this.entrepriseProfileRepository = entrepriseProfileRepository;
    }

    /**
     * Résout l'utilisateur connecté depuis le SecurityContext.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof User u) {
            return u;
        }
        if (auth.getName() != null) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * Résout et vérifie le Client associé à l'utilisateur connecté.
     * Si l'utilisateur est de type CLIENT, son clientId est forcé et ne peut pas être contourné.
     */
    @Transactional(readOnly = true)
    public Client resolveClientConnecte(Long optionalClientId) {
        User user = getCurrentUser();
        Long resolvedClientId = null;

        if (user != null) {
            if (user.getClientId() != null) {
                resolvedClientId = user.getClientId();
            } else if (user.getUserType() == TypeUtilisateur.CLIENT) {
                throw new CommonException("Votre compte utilisateur client n'est rattaché à aucune fiche client.", HttpStatus.FORBIDDEN);
            }
        }

        if (resolvedClientId == null) {
            resolvedClientId = optionalClientId;
        }

        if (resolvedClientId == null) {
            throw new CommonException("Impossible d'identifier le client connecté pour l'Espace Client.", HttpStatus.BAD_REQUEST);
        }

        final Long targetId = resolvedClientId;
        return clientRepository.findById(targetId)
                .orElseThrow(() -> new CommonException("Fiche client introuvable (ID: " + targetId + ").", HttpStatus.NOT_FOUND));
    }

    private Long resolveTenantId(Client client) {
        if (client != null && client.getPointDeVenteId() != null) {
            return client.getPointDeVenteId();
        }
        User user = getCurrentUser();
        if (user != null && user.getTenantId() != null) {
            return user.getTenantId();
        }
        Long ctxTenant = TenantContext.getCurrentTenant();
        return ctxTenant != null ? ctxTenant : 1L;
    }

    /**
     * 1. Accueil / Dashboard de l'Espace Client.
     */
    @Transactional(readOnly = true)
    public EspaceClientDashboardDTO getDashboard(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long clientId = client.getId();
        Long tenantId = resolveTenantId(client);

        EspaceClientDashboardDTO dashboard = new EspaceClientDashboardDTO();
        dashboard.setClientId(clientId);
        dashboard.setCodeClient(client.getCodeClient());
        dashboard.setClientNomComplet(client.getNomComplet() != null ? client.getNomComplet() : client.getNom());
        dashboard.setClientIce(client.getIce());
        dashboard.setClientEmail(client.getEmail());
        dashboard.setClientTelephone(client.getTelephone());
        dashboard.setClientAdresse(client.getAdresse());
        dashboard.setTenantId(tenantId);

        entrepriseProfileRepository.findByPointDeVenteId(tenantId)
                .ifPresent(ep -> dashboard.setNomTenant(ep.getNomEntreprise()));

        List<ReservationClientDTO> allReservations = reservationClientService.getReservationsDuClient(clientId);
        List<Devis> allDevis = devisRepository.findByClientIdOrderByDateDevisDesc(clientId).stream()
                .filter(d -> d.getStatut() != StatutDevis.BROUILLON)
                .collect(Collectors.toList());
        List<CommandeClient> allCommandes = commandeClientRepository.findByClientIdOrderByDateCommandeDesc(clientId);
        List<BonLivraisonClient> allBls = bonLivraisonClientRepository.findByClientIdOrderByDateBlDesc(clientId);

        dashboard.setTotalReservations(allReservations.size());
        dashboard.setReservationsEnAttente(allReservations.stream()
                .filter(r -> r.getStatut() == StatutReservationClient.SOUMISE || r.getStatut() == StatutReservationClient.EN_COURS_DEVIS)
                .count());

        dashboard.setDevisEnAttenteValidation(allDevis.stream()
                .filter(d -> d.getStatut() == StatutDevis.ENVOYE)
                .count());
        dashboard.setDevisAcceptes(allDevis.stream()
                .filter(d -> d.getStatut() == StatutDevis.ACCEPTE || d.getStatut() == StatutDevis.TRANSFORME_EN_COMMANDE)
                .count());

        dashboard.setTotalCommandes(allCommandes.size());
        dashboard.setCommandesEnCours(allCommandes.stream()
                .filter(c -> c.getStatut() != StatutCommandeClient.ANNULEE && c.getStatut() != StatutCommandeClient.LIVREE)
                .count());

        dashboard.setBlEnAttente(allBls.stream()
                .filter(bl -> bl.getStatut() == StatutLivraison.EN_ATTENTE)
                .count());
        dashboard.setBlExpedies(allBls.stream()
                .filter(bl -> bl.getStatut() == StatutLivraison.EXPEDIE || bl.getStatut() == StatutLivraison.EN_LIVRAISON)
                .count());
        dashboard.setBlLivres(allBls.stream()
                .filter(bl -> bl.getStatut() == StatutLivraison.LIVREE)
                .count());

        dashboard.setDernieresReservations(allReservations.stream().limit(5).collect(Collectors.toList()));
        dashboard.setDevisEnAttente(allDevis.stream().limit(5)
                .map(d -> devisService.enrichirDevisDto(devisMapper.toDto(d)))
                .collect(Collectors.toList()));
        dashboard.setCommandesRecentes(allCommandes.stream().limit(5)
                .map(commandeClientMapper::toDto)
                .collect(Collectors.toList()));
        dashboard.setDerniersBonsLivraison(allBls.stream().limit(5)
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList()));

        return dashboard;
    }

    /**
     * 2. Catalogue Produits pour l'Espace Client : AUCUN PRIX, AUCUNE DISPONIBILITÉ/STOCK.
     */
    @Transactional(readOnly = true)
    public List<CatalogueProduitClientDTO> getCatalogueProduits(String search, Long categorieId, String categorieNom, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        List<Produit> produits = produitRepository.findWithImagesByPointDeVenteId(tenantId);
        return produits.stream()
                .filter(p -> p.getActif() == null || Boolean.TRUE.equals(p.getActif()))
                .filter(p -> {
                    if (categorieId != null) {
                        return p.getCategorie() != null && categorieId.equals(p.getCategorie().getId());
                    }
                    return true;
                })
                .filter(p -> {
                    if (categorieNom != null && !categorieNom.isBlank()) {
                        String q = categorieNom.trim().toLowerCase();
                        String catArt = p.getCategorieArticle() != null ? p.getCategorieArticle().toLowerCase() : "";
                        String catObj = (p.getCategorie() != null && p.getCategorie().getNom() != null) ? p.getCategorie().getNom().toLowerCase() : "";
                        return catArt.contains(q) || catObj.contains(q);
                    }
                    return true;
                })
                .filter(p -> {
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase();
                        String ref = p.getReference() != null ? p.getReference().toLowerCase() : "";
                        String des = p.getDesignation() != null ? p.getDesignation().toLowerCase() : "";
                        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        String cat = p.getCategorieArticle() != null ? p.getCategorieArticle().toLowerCase() : "";
                        return ref.contains(q) || des.contains(q) || desc.contains(q) || cat.contains(q);
                    }
                    return true;
                })
                .map(this::toCatalogueProduitClientDto)
                .collect(Collectors.toList());
    }

    /**
     * Recherche paginée avec ProduitSearchCriteria dans le Catalogue Client (SANS PRIX NI DISPONIBILITÉ).
     */
    @Transactional(readOnly = true)
    public Page<CatalogueProduitClientDTO> searchCatalogueProduits(ProduitSearchCriteria criteria, Pageable pageable, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        ProduitSearchCriteria safeCriteria = (criteria != null) ? criteria : new ProduitSearchCriteria();
        safeCriteria.setSocieteId(tenantId);
        safeCriteria.setActif(true);
        // Sécurité stricte : interdire tout filtrage par prix côté client
        safeCriteria.setPrixMin(null);
        safeCriteria.setPrixMax(null);

        return produitRepository.findByCriteria(safeCriteria, pageable)
                .map(this::toCatalogueProduitClientDto);
    }

    @Transactional(readOnly = true)
    public List<CategorieDTO> getCategoriesCatalogue(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);
        return categorieRepository.findByPointDeVenteIdAndActifTrueOrderByNomAsc(tenantId)
                .stream()
                .map(categorieMapper::toDto)
                .collect(Collectors.toList());
    }

    private CatalogueProduitClientDTO toCatalogueProduitClientDto(Produit p) {
        CatalogueProduitClientDTO dto = new CatalogueProduitClientDTO();
        dto.setId(p.getId());
        dto.setReference(p.getReference());
        dto.setDesignation(p.getDesignation() != null && !p.getDesignation().isBlank() ? p.getDesignation() : p.getDescription());
        dto.setDescription(p.getDescription());
        if (p.getCategorie() != null) {
            dto.setCategorieId(p.getCategorie().getId());
            dto.setCategorieNom(p.getCategorie().getNom());
        } else {
            dto.setCategorieNom(p.getCategorieArticle());
        }
        dto.setGroupeArticle(p.getGroupeArticle());
        dto.setCodeBarre(p.getCodeBarre());
        dto.setUniteMesure(p.getUniteMesureStock());
        dto.setTypeProduit(p.getTypeProduit());
        dto.setAttributes(p.getAttributes());

        if (p.getImage() != null && p.getImage().getImageData() != null && p.getImage().getImageData().length > 0) {
            dto.setHasImage(true);
            dto.setImageContentType(p.getImage().getContentType() != null ? p.getImage().getContentType() : "image/jpeg");
            dto.setImageBase64(Base64.getEncoder().encodeToString(p.getImage().getImageData()));
        } else {
            dto.setHasImage(false);
        }
        return dto;
    }

    /**
     * 3. Réservations d'articles du Client (Panier de réservation sans prix ni stock).
     */
    @Transactional(readOnly = true)
    public List<ReservationClientDTO> getMesReservations(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        return reservationClientService.getReservationsDuClient(client.getId());
    }

    @Transactional(readOnly = true)
    public Page<ReservationClientDTO> searchMesReservations(ReservationClientSearchCriteria criteria, Pageable pageable, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        ReservationClientSearchCriteria safeCriteria = (criteria != null) ? criteria : new ReservationClientSearchCriteria();
        safeCriteria.setClientId(client.getId());
        safeCriteria.setSocieteId(tenantId);

        return reservationClientService.searchReservations(safeCriteria, pageable);
    }

    @Transactional(readOnly = true)
    public ReservationClientDTO getMaReservationById(Long reservationId, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        ReservationClient res = reservationClientRepository.findByIdAndClientId(reservationId, client.getId())
                .orElseThrow(() -> new CommonException("Réservation introuvable dans votre espace client.", HttpStatus.NOT_FOUND));
        return reservationClientService.toDto(res);
    }

    public ReservationClientDTO creerMaReservation(ReservationClientDTO dto, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        return reservationClientService.creerReservation(dto, client.getId(), userId);
    }

    public ReservationClientDTO annulerMaReservation(Long reservationId, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        return reservationClientService.annulerReservationParClient(reservationId, client.getId());
    }

    /**
     * 4. Devis du Client + Accord (Accepter / Refuser le devis).
     */
    @Transactional(readOnly = true)
    public List<DevisDTO> getMesDevis(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        return devisRepository.findByClientIdOrderByDateDevisDesc(client.getId()).stream()
                .filter(d -> d.getStatut() != StatutDevis.BROUILLON)
                .map(d -> devisService.enrichirDevisDto(devisMapper.toDto(d)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DevisDTO> searchMesDevis(DevisSearchCriteria criteria, Pageable pageable, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        DevisSearchCriteria safeCriteria = (criteria != null) ? criteria : new DevisSearchCriteria();
        safeCriteria.setClientId(client.getId());
        safeCriteria.setSocieteId(tenantId);
        if (safeCriteria.getInclureAnnulees() == null) {
            safeCriteria.setInclureAnnulees(true);
        }

        return devisService.searchDevis(safeCriteria, pageable)
                .map(d -> devisService.enrichirDevisDto(devisMapper.toDto(d)));
    }

    @Transactional(readOnly = true)
    public DevisDTO getMonDevisById(Long devisId, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Devis devis = devisRepository.findById(devisId)
                .filter(d -> d.getClient() != null && client.getId().equals(d.getClient().getId()))
                .orElseThrow(() -> new CommonException("Devis introuvable dans votre espace client.", HttpStatus.NOT_FOUND));
        return devisService.enrichirDevisDto(devisMapper.toDto(devis));
    }

    /**
     * Le client donne son accord sur le devis : le devis passe au statut ACCEPTE,
     * ce qui permet ensuite au Tenant de le transformer en Bon de Commande.
     */
    public DevisDTO accepterMonDevis(Long devisId, String numeroAchatClient, String commentaireClient, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Devis devis = devisRepository.findById(devisId)
                .filter(d -> d.getClient() != null && client.getId().equals(d.getClient().getId()))
                .orElseThrow(() -> new CommonException("Devis introuvable dans votre espace client.", HttpStatus.NOT_FOUND));

        if (devis.getStatut() == StatutDevis.TRANSFORME_EN_COMMANDE) {
            throw new CommonException("Ce devis a déjà été transformé en commande.", HttpStatus.BAD_REQUEST);
        }
        if (devis.getStatut() == StatutDevis.EXPIRE) {
            throw new CommonException("Ce devis est expiré et ne peut plus être accepté.", HttpStatus.BAD_REQUEST);
        }

        if (numeroAchatClient != null && !numeroAchatClient.isBlank()) {
            devis.setNumeroAchatClient(numeroAchatClient.trim());
        }
        if (commentaireClient != null && !commentaireClient.isBlank()) {
            String notesActuelles = devis.getNotes() != null ? devis.getNotes() + "\n" : "";
            devis.setNotes(notesActuelles + "[Accord Client] " + commentaireClient.trim());
        }
        devis.setDateReponseClient(LocalDateTime.now());
        devis.setStatut(StatutDevis.ACCEPTE);
        Devis saved = devisRepository.save(devis);

        // Synchroniser la réservation d'articles liée
        if (saved.getReservationClientId() != null) {
            reservationClientRepository.findById(saved.getReservationClientId()).ifPresent(res -> {
                res.setStatut(StatutReservationClient.ACCEPTEE);
                reservationClientRepository.save(res);
            });
        } else {
            reservationClientRepository.findFirstByDevisGenereId(saved.getId()).ifPresent(res -> {
                res.setStatut(StatutReservationClient.ACCEPTEE);
                reservationClientRepository.save(res);
            });
        }

        return devisService.enrichirDevisDto(devisMapper.toDto(saved));
    }

    /**
     * Le client refuse le devis.
     */
    public DevisDTO refuserMonDevis(Long devisId, String motifRefus, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Devis devis = devisRepository.findById(devisId)
                .filter(d -> d.getClient() != null && client.getId().equals(d.getClient().getId()))
                .orElseThrow(() -> new CommonException("Devis introuvable dans votre espace client.", HttpStatus.NOT_FOUND));

        if (devis.getStatut() == StatutDevis.TRANSFORME_EN_COMMANDE) {
            throw new CommonException("Impossible de refuser un devis déjà transformé en commande.", HttpStatus.BAD_REQUEST);
        }

        devis.setDateReponseClient(LocalDateTime.now());
        devis.setMotifRefusClient(motifRefus);
        devis.setStatut(StatutDevis.REFUSE);
        Devis saved = devisRepository.save(devis);

        if (saved.getReservationClientId() != null) {
            reservationClientRepository.findById(saved.getReservationClientId()).ifPresent(res -> {
                res.setStatut(StatutReservationClient.REFUSEE);
                reservationClientRepository.save(res);
            });
        } else {
            reservationClientRepository.findFirstByDevisGenereId(saved.getId()).ifPresent(res -> {
                res.setStatut(StatutReservationClient.REFUSEE);
                reservationClientRepository.save(res);
            });
        }

        return devisService.enrichirDevisDto(devisMapper.toDto(saved));
    }

    /**
     * 5. Bons de Commande du Client.
     */
    @Transactional(readOnly = true)
    public List<CommandeClientDTO> getMesCommandes(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        return commandeClientRepository.findByClientIdOrderByDateCommandeDesc(client.getId())
                .stream()
                .map(commandeClientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CommandeClientDTO> searchMesCommandes(CommandeClientSearchCriteria criteria, Pageable pageable, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        CommandeClientSearchCriteria safeCriteria = (criteria != null) ? criteria : new CommandeClientSearchCriteria();
        safeCriteria.setClientId(client.getId());
        safeCriteria.setSocieteId(tenantId);

        return commandeClientRepository.findByCriteria(safeCriteria, pageable)
                .map(commandeClientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CommandeClientDTO getMaCommandeById(Long commandeId, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        CommandeClient cmd = commandeClientRepository.findByIdAndClientId(commandeId, client.getId())
                .orElseThrow(() -> new CommonException("Bon de commande introuvable dans votre espace client.", HttpStatus.NOT_FOUND));
        return commandeClientMapper.toDto(cmd);
    }

    /**
     * 6. Bons de Livraison (BL) du Client avec suivi du statut (EN_ATTENTE, EXPEDIE, LIVREE...).
     */
    @Transactional(readOnly = true)
    public List<BonLivraisonClientDTO> getMesBonsLivraison(Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        return bonLivraisonClientRepository.findByClientIdOrderByDateBlDesc(client.getId())
                .stream()
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<BonLivraisonClientDTO> searchMesBonsLivraison(BonLivraisonClientSearchCriteria criteria, Pageable pageable, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        Long tenantId = resolveTenantId(client);

        BonLivraisonClientSearchCriteria safeCriteria = (criteria != null) ? criteria : new BonLivraisonClientSearchCriteria();
        safeCriteria.setClientId(client.getId());
        safeCriteria.setSocieteId(tenantId);

        return bonLivraisonClientRepository.findByCriteria(safeCriteria, pageable)
                .map(bonLivraisonClientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public BonLivraisonClientDTO getMonBonLivraisonById(Long blId, Long optionalClientId) {
        Client client = resolveClientConnecte(optionalClientId);
        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndClientId(blId, client.getId())
                .orElseThrow(() -> new CommonException("Bon de livraison introuvable dans votre espace client.", HttpStatus.NOT_FOUND));
        return bonLivraisonClientMapper.toDto(bl);
    }
}

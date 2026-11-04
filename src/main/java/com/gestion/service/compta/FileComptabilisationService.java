package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceCompteDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceJournalDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.AnomaliePieceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuAnomalieDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuCompteDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ApercuRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.CompteurSourceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.LignePropositionDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PageFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceFileDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PropositionDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.TiersProposeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.repository.compta.ConsultationRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.repository.compta.PasserelleRepository;
import com.gestion.repository.compta.PasserelleRepository.Lien;
import com.gestion.repository.compta.PasserelleRepository.Ventilation;
import com.gestion.repository.compta.SourcesRepository;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.ContexteRegles.Complement;
import com.gestion.service.compta.ContexteRegles.LigneVentilation;
import com.gestion.service.compta.PieceSource.LigneSource;
import com.gestion.service.compta.PieceSource.TiersSource;
import com.gestion.service.compta.PropositionEcriture.AnomaliePiece;
import com.gestion.service.compta.PropositionEcriture.LigneProposee;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** La file calculée : l'état d'une pièce se déduit de la source et des liens, il n'est jamais stocké. Ce service ne fait que lire. */
@Service
public class FileComptabilisationService {

    public static final String A_COMPTABILISER = "A_COMPTABILISER";
    public static final String SANS_OBJET = "SANS_OBJET";
    public static final String COMPTABILISEE = "COMPTABILISEE";
    public static final String MODIFIEE = "MODIFIEE";
    public static final String ANNULEE = "ANNULEE";
    public static final String DISPARUE = "DISPARUE";
    public static final String ANOMALIE = "ANOMALIE";
    public static final String ECARTEE = "ECARTEE";

    /** Dans un lot, ce qui crée le tiers et sa dette passe d'abord, et le paiement avant son effet. */
    private static final List<String> ORDRE = List.of("FACTURE", "VENTE", "AVOIR_CLIENT", "FACTURE_ACHAT", "AVOIR_FOURNISSEUR", "DEPENSE", "PAIEMENT", "EFFET", "REGLEMENT_FOURNISSEUR", "MOUVEMENT_TRESORERIE", "LIGNE_RELEVE");
    private static final List<String> ORDRE_EVENEMENTS = List.of("COMPTABILISATION", "IMPUTATION_ACOMPTE", "REMISE", "ENCAISSEMENT", "ECHEANCE", "REJET", "DEPART", "ARRIVEE", "ANNULATION");
    /** Jamais en lot : un encaissement divers peut doubler un règlement, et une ligne de relevé attend son compte. */
    private static final Set<String> UNITAIRES = Set.of("MOUVEMENT_TRESORERIE", "LIGNE_RELEVE");

    /** Une pièce et ce que la file en dit. piece nulle : la source a disparu, il reste le lien. */
    public record PieceEtat(String type, Long id, String evenement, PieceSource piece, String etat, Lien lien, PropositionEcriture proposition, String empreinte, Map<String, String> instantane) {
    }

    private final SourcesRepository sources;
    private final PasserelleRepository passerelle;
    private final CorrespondanceService correspondances;
    private final ExerciceRepository exercices;
    private final LivreRepository livres;
    private final JournauxRepository journaux;
    private final JournalRepository journal;
    private final ConsultationRepository consultation;
    private final TiersRepository tiers;
    private final DatesComptables dates;
    private final LivreCourantService livreCourant;
    private final ObjectMapper json;

    public FileComptabilisationService(SourcesRepository sources, PasserelleRepository passerelle, CorrespondanceService correspondances, ExerciceRepository exercices, LivreRepository livres,
                                       JournauxRepository journaux, JournalRepository journal, ConsultationRepository consultation, TiersRepository tiers, DatesComptables dates,
                                       LivreCourantService livreCourant, ObjectMapper json) {
        this.sources = sources;
        this.passerelle = passerelle;
        this.correspondances = correspondances;
        this.exercices = exercices;
        this.livres = livres;
        this.journaux = journaux;
        this.journal = journal;
        this.consultation = consultation;
        this.tiers = tiers;
        this.dates = dates;
        this.livreCourant = livreCourant;
        this.json = json;
    }

    public List<CompteurSourceDTO> compteurs(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exercices.lire(livreId, exerciceId).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", exerciceId));
        List<CompteurSourceDTO> compteurs = new ArrayList<>();
        for (String type : ORDRE) {
            Map<String, Long> parEtat = new HashMap<>();
            for (PieceEtat p : lire(livreId, type, exercice.du(), exercice.au(), null)) {
                parEtat.merge(p.etat(), 1L, Long::sum);
            }
            compteurs.add(new CompteurSourceDTO(type, parEtat.getOrDefault(A_COMPTABILISER, 0L), parEtat.getOrDefault(COMPTABILISEE, 0L), parEtat.getOrDefault(ANOMALIE, 0L),
                    parEtat.getOrDefault(ECARTEE, 0L), parEtat.getOrDefault(MODIFIEE, 0L), parEtat.getOrDefault(ANNULEE, 0L), parEtat.getOrDefault(DISPARUE, 0L), parEtat.getOrDefault(SANS_OBJET, 0L)));
        }
        return compteurs;
    }

    /** du et au sont obligatoires : ils bornent le calcul de l'empreinte. */
    public PageFileDTO file(LocalDate du, LocalDate au, String sourceType, String etat, String q, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        String recherche = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        List<PieceEtat> toutes = new ArrayList<>();
        for (String type : sourceType == null || sourceType.isBlank() ? ORDRE : List.of(exigerType(sourceType))) {
            for (PieceEtat p : lire(livreId, type, du, au, null)) {
                if ((etat == null || etat.isBlank() || etat.equals(p.etat())) && (recherche == null || correspond(p, recherche))) {
                    toutes.add(p);
                }
            }
        }
        toutes.sort(Comparator.comparing(FileComptabilisationService::cle));
        String apres = Curseur.decoder(curseur);
        int limite = Curseur.taille(taille);
        List<PieceFileDTO> contenu = new ArrayList<>();
        String derniere = null;
        boolean suite = false;
        for (PieceEtat p : toutes) {
            String c = cle(p);
            if (apres != null && c.compareTo(apres) <= 0) {
                continue;
            }
            if (contenu.size() == limite) {
                suite = true;
                break;
            }
            contenu.add(versDto(p));
            derniere = c;
        }
        return new PageFileDTO(contenu, suite ? Curseur.encoder(derniere) : null);
    }

    public List<PieceFileDTO> verifier(LocalDate du, LocalDate au) {
        Long livreId = livreCourant.exiger().livreId();
        List<PieceFileDTO> ecarts = new ArrayList<>();
        for (String type : ORDRE) {
            for (PieceEtat p : lire(livreId, type, du, au, null)) {
                if (MODIFIEE.equals(p.etat()) || DISPARUE.equals(p.etat()) || ANNULEE.equals(p.etat())) {
                    ecarts.add(versDto(p));
                }
            }
        }
        return ecarts;
    }

    public PropositionDTO proposition(String type, Long id, String evenement) {
        Long livreId = livreCourant.exiger().livreId();
        PieceEtat p = une(livreId, exigerType(type), id, evenement);
        Map<String, String> libelles = consultation.libellesComptes(livreId);
        List<LignePropositionDTO> lignes = new ArrayList<>();
        Long contrepassationDe = null;
        JournalDTO j = null;
        LocalDate datePiece = null;
        LocalDate dateEcriture = null;
        String ref = null;
        String libelle = null;
        List<AnomaliePieceDTO> anomalies = new ArrayList<>();
        List<String> requis = List.of();
        if (ANNULEE.equals(p.etat()) || DISPARUE.equals(p.etat())) {
            // Ce qui sera passé : le miroir de l'écriture active
            contrepassationDe = p.lien().ecritureId();
            var origine = journal.lire(livreId, p.lien().ecritureId()).orElseThrow();
            j = journaux.lire(livreId, origine.journal().id()).orElseThrow();
            datePiece = origine.datePiece();
            ref = origine.refPiece();
            libelle = "Contre-passation de " + origine.numeroComplet();
            for (LigneJournal l : journal.lignesBrutes(livreId, p.lien().ecritureId())) {
                lignes.add(new LignePropositionDTO(new ReferenceCompteDTO(l.compte(), libelles.getOrDefault(l.compte(), l.compte())), tiersLu(livreId, l.tiersId()), l.credit(), l.debit(), l.tauxTva(), l.baseTva()));
            }
        } else if (p.proposition() != null) {
            PropositionEcriture r = p.proposition();
            if (MODIFIEE.equals(p.etat())) {
                contrepassationDe = p.lien().ecritureId();
            }
            j = journaux.lister(livreId).stream().filter(x -> x.code().equals(r.journal())).findFirst().orElse(null);
            datePiece = r.datePiece();
            ref = r.refPiece();
            libelle = r.libelle();
            requis = r.complementsRequis();
            for (LigneProposee l : r.lignes()) {
                lignes.add(new LignePropositionDTO(new ReferenceCompteDTO(l.compte(), libelles.getOrDefault(l.compte(), l.compte())), tiersPropose(livreId, l.tiers()), l.debit(), l.credit(), l.taux(), l.base()));
            }
            r.anomalies().forEach(a -> anomalies.add(new AnomaliePieceDTO(a.code(), a.message(), a.bloquante())));
        }
        if (j != null && datePiece != null) {
            try {
                dateEcriture = dates.resoudre(livreId, j.id(), datePiece);
            } catch (CommonException e) {
                anomalies.add(new AnomaliePieceDTO(e.getErrorCode(), e.getMessage(), false));
            }
        }
        return new PropositionDTO(p.type(), p.id(), p.evenement(), p.lien() == null ? null : p.lien().version(), p.empreinte(), p.etat(),
                j == null ? null : new ReferenceJournalDTO(j.id(), j.code(), j.libelle()), datePiece, dateEcriture, ref, libelle, contrepassationDe, lignes, anomalies, requis);
    }

    public ApercuDTO apercu(ApercuRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        Map<String, String> libelles = consultation.libellesComptes(livreId);
        Map<String, BigDecimal[]> parCompte = new TreeMap<>();
        List<ApercuAnomalieDTO> anomalies = new ArrayList<>();
        Map<String, Map<String, PieceEtat>> parType = new HashMap<>();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        int pieces = 0;
        for (PieceDemandee d : demande.pieces()) {
            String type = exigerType(d.sourceType());
            Map<String, PieceEtat> index = parType.computeIfAbsent(type, t -> indexer(lire(livreId, t, demande.du(), demande.au(), null)));
            PieceEtat p = index.get(d.sourceId() + "|" + d.evenement());
            if (p == null || p.proposition() == null) {
                continue;
            }
            for (AnomaliePiece a : p.proposition().anomalies()) {
                anomalies.add(new ApercuAnomalieDTO(type, d.sourceId(), p.piece() == null ? null : p.piece().numero(), a.code(), a.message(), a.bloquante()));
            }
            if (A_COMPTABILISER.equals(p.etat()) || MODIFIEE.equals(p.etat())) {
                pieces++;
                for (LigneProposee l : p.proposition().lignes()) {
                    BigDecimal[] t = parCompte.computeIfAbsent(l.compte(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    t[0] = t[0].add(l.debit());
                    t[1] = t[1].add(l.credit());
                    debit = debit.add(l.debit());
                    credit = credit.add(l.credit());
                }
            }
        }
        List<ApercuCompteDTO> comptes = new ArrayList<>();
        parCompte.forEach((compte, t) -> comptes.add(new ApercuCompteDTO(compte, libelles.get(compte), t[0], t[1])));
        return new ApercuDTO(comptes, anomalies, pieces, debit, credit);
    }

    /** Une pièce, relue au moment de la poster : la même lecture que la file. */
    public PieceEtat une(Long livreId, String type, Long id, String evenement) {
        for (PieceEtat p : lire(livreId, type, null, null, id)) {
            if (p.id().equals(id) && p.evenement().equals(evenement)) {
                return p;
            }
        }
        throw new CommonException("Rien à faire pour " + type + " " + id + " (" + evenement + ") : la pièce est absente ou cet événement n'est pas dû.", HttpStatus.NOT_FOUND, "COMPTA_PIECE_INTROUVABLE");
    }

    /** Toutes les pièces d'un type sur une plage, ou une seule. Lectures ensemblistes, puis règles pures en mémoire. */
    public List<PieceEtat> lire(Long livreId, String type, LocalDate du, LocalDate au, Long id) {
        Map<String, String> table = correspondances.assurer(livreId);
        String remiseMode = livres.lire(livreId).remiseGlobaleMode();
        Map<Long, List<LigneSource>> lignes = new HashMap<>();
        for (SourcesRepository.LigneLue l : sources.lignes(livreId, type, du, au, id)) {
            lignes.computeIfAbsent(l.pieceId(), k -> new ArrayList<>()).add(new LigneSource(l.produitId(), l.typeProduit(), l.categorieId(), l.tauxTva(), l.ht(), l.tva(), l.ttc()));
        }
        Map<String, Lien> liens = new HashMap<>();
        for (Lien l : passerelle.derniersLiens(livreId, type, id)) {
            liens.put(l.sourceId() + "|" + l.evenement(), l);
        }
        Set<Long> ecartees = new HashSet<>(passerelle.piecesEcartees(livreId, type, id));
        Map<Long, List<Ventilation>> ventilations = passerelle.ventilations(livreId, type, id);
        Map<String, PasserelleRepository.Complement> complements = passerelle.complements(livreId, type, id);
        Map<Long, Boolean> paiementsActifs = "EFFET".equals(type) ? paiementsActifs(livreId) : Map.of();
        LocalDate aujourdhui = LocalDate.now();

        List<PieceEtat> resultat = new ArrayList<>();
        Set<Long> lus = new HashSet<>();
        for (SourcesRepository.EnTete e : sources.enTetes(livreId, type, du, au, id)) {
            lus.add(e.id());
            PieceSource piece = piece(type, e, lignes.getOrDefault(e.id(), List.of()));
            List<LigneVentilation> ventilation = ventilations.getOrDefault(e.id(), List.of()).stream()
                    .map(v -> new LigneVentilation(v.compte(), v.montantHt(), v.tauxTva(), v.montantTva(), v.tvaDeductible(), v.estImmobilisation(), v.numeroFactureFournisseur())).toList();
            List<String> dus = ReglesComptabilisation.evenementsDus(piece, aujourdhui);
            Set<String> passes = new HashSet<>();
            for (String evenement : ORDRE_EVENEMENTS) {
                Lien l = liens.get(e.id() + "|" + evenement);
                if (l != null && l.actif()) {
                    passes.add(evenement);
                }
            }
            // Un événement déjà passé reste dans la file, même si le statut commercial a bougé depuis
            List<String> aMontrer = new ArrayList<>(dus);
            for (String evenement : ORDRE_EVENEMENTS) {
                if (passes.contains(evenement) && !aMontrer.contains(evenement)) {
                    aMontrer.add(evenement);
                }
            }
            for (String evenement : aMontrer) {
                PasserelleRepository.Complement cs = complements.get(e.id() + "|" + evenement);
                Complement complement = cs == null ? Complement.AUCUN : new Complement(cs.compteFinancierId(), cs.dateEffet(), cs.referenceDocument());
                boolean prealable = !"EFFET".equals(type) || (piece.attr("paiement_id") != null && paiementsActifs.getOrDefault(Long.valueOf(piece.attr("paiement_id")), false));
                ContexteRegles contexte = new ContexteRegles(table, remiseMode, ventilation, complement, passes, prealable);
                PropositionEcriture proposition = "ANNULATION".equals(evenement) ? null : ReglesComptabilisation.proposer(piece, evenement, contexte);
                String empreinte = empreinte(piece, evenement);
                Lien lien = liens.get(e.id() + "|" + evenement);
                String etat = etat(piece, evenement, lien, liens.get(e.id() + "|ANNULATION"), ecartees.contains(e.id()), proposition, empreinte, dus.contains(evenement), prealable);
                resultat.add(new PieceEtat(type, e.id(), evenement, piece, etat, lien, proposition, empreinte, null));
            }
        }
        ajouterDisparues(livreId, type, du, au, liens, lus, resultat);
        return resultat;
    }

    /** Un lien actif dont la source n'existe plus : la plage de dates ne suffit pas à le dire, une pièce a pu changer de date. */
    private void ajouterDisparues(Long livreId, String type, LocalDate du, LocalDate au, Map<String, Lien> liens, Set<Long> lus, List<PieceEtat> resultat) {
        List<Lien> candidats = liens.values().stream().filter(l -> l.actif() && !lus.contains(l.sourceId())).toList();
        if (candidats.isEmpty()) {
            return;
        }
        Set<Long> existants = new HashSet<>(sources.existants(livreId, type, candidats.stream().map(Lien::sourceId).distinct().toList()));
        for (Lien l : candidats) {
            if (existants.contains(l.sourceId())) {
                continue;
            }
            Map<String, String> instantane = lireInstantane(l.instantane());
            String date = instantane.get("date");
            LocalDate jour = date == null ? null : LocalDate.parse(date);
            if (jour == null || ((du == null || !jour.isBefore(du)) && (au == null || !jour.isAfter(au)))) {
                resultat.add(new PieceEtat(type, l.sourceId(), l.evenement(), null, DISPARUE, l, null, l.empreinte(), instantane));
            }
        }
    }

    private Map<Long, Boolean> paiementsActifs(Long livreId) {
        Map<Long, Boolean> actifs = new HashMap<>();
        for (Lien l : passerelle.derniersLiens(livreId, "PAIEMENT", null)) {
            actifs.put(l.sourceId(), l.actif());
        }
        return actifs;
    }

    /** La table de la section 6.1. */
    private static String etat(PieceSource p, String evenement, Lien lien, Lien annulation, boolean ecartee, PropositionEcriture proposition, String empreinte, boolean du, boolean prealable) {
        boolean actif = lien != null && lien.actif();
        if (ecartee && !actif) {
            return ECARTEE;
        }
        if ("VENTE".equals(p.type()) && annulation != null && annulation.actif()) {
            return SANS_OBJET;
        }
        if ("ANNULATION".equals(evenement)) {
            return COMPTABILISEE;
        }
        if (!actif) {
            if ("FACTURE".equals(p.type()) && p.annulee() && !"ANNULEE".equals(p.statut())) {
                return ANOMALIE;
            }
            if (p.annulee() || ReglesComptabilisation.sansObjet(p) || !du) {
                return SANS_OBJET;
            }
            // Effet d'un paiement annulé jamais comptabilisé : rien n'a été écrit et aucun encaissement n'a eu lieu
            if ("EFFET".equals(p.type()) && p.vrai("paiement_annule") && !prealable) {
                return SANS_OBJET;
            }
            if ("LIGNE_RELEVE".equals(p.type()) && proposition != null && proposition.lignes().isEmpty()) {
                return SANS_OBJET;
            }
            return proposition != null && proposition.bloquee() ? ANOMALIE : A_COMPTABILISER;
        }
        // Le service commercial annule le paiement au rejet : R06 passe le rejet, le paiement ne s'inverse pas une deuxième fois
        if ("PAIEMENT".equals(p.type()) && p.annulee() && p.vrai("effet_rejete")) {
            return COMPTABILISEE;
        }
        if (p.annulee()) {
            return ANNULEE;
        }
        return lien.empreinte().equals(empreinte) ? COMPTABILISEE : MODIFIEE;
    }

    /** L'empreinte d'un effet dépend de l'événement : un encaissement ne modifie pas la remise passée avant lui. */
    private static String empreinte(PieceSource p, String evenement) {
        if (!"EFFET".equals(p.type())) {
            return p.empreinte();
        }
        String cle = switch (evenement) {
            case ReglesComptabilisation.REMISE -> "empreinte_remise";
            case ReglesComptabilisation.ENCAISSEMENT, ReglesComptabilisation.ECHEANCE -> "empreinte_encaissement";
            default -> "empreinte_autre";
        };
        String valeur = p.attr(cle);
        return valeur != null ? valeur : p.empreinte();
    }

    private PieceSource piece(String type, SourcesRepository.EnTete e, List<LigneSource> lignes) {
        TiersSource t = e.tiersType() == null || e.tiersId() == null ? null : new TiersSource(e.tiersType(), e.tiersId(), e.tiersNom(), e.tiersPays());
        return new PieceSource(type, e.id(), e.numero(), e.date(), e.pointDeVenteId(), t, e.montant(), e.ht(), e.tva(), e.ttc(), e.remise(), e.statut(), e.annulee(), e.dateAnnulation(),
                e.empreinte(), lireInstantane(e.attributs()), lignes);
    }

    private Map<String, String> lireInstantane(String texte) {
        try {
            Map<String, Object> brut = json.readValue(texte, new TypeReference<>() {
            });
            Map<String, String> valeurs = new LinkedHashMap<>();
            brut.forEach((k, v) -> valeurs.put(k, v == null ? null : String.valueOf(v)));
            return valeurs;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Attributs illisibles", e);
        }
    }

    private static Map<String, PieceEtat> indexer(List<PieceEtat> pieces) {
        Map<String, PieceEtat> index = new HashMap<>();
        pieces.forEach(p -> index.put(p.id() + "|" + p.evenement(), p));
        return index;
    }

    private static boolean correspond(PieceEtat p, String recherche) {
        if (p.piece() == null) {
            return String.valueOf(p.instantane()).toLowerCase().contains(recherche);
        }
        return (p.piece().numero() != null && p.piece().numero().toLowerCase().contains(recherche))
                || (p.piece().tiers() != null && p.piece().tiers().nom() != null && p.piece().tiers().nom().toLowerCase().contains(recherche));
    }

    /** Date, puis ordre de passage, puis identifiant : la clé du curseur et l'ordre du lot. */
    public static String cle(PieceEtat p) {
        LocalDate date = p.piece() != null ? p.proposition() != null ? p.proposition().datePiece() : p.piece().date() : null;
        String jour = date != null ? date.toString() : p.instantane() != null && p.instantane().get("date") != null ? p.instantane().get("date") : "0000-00-00";
        return "%s|%02d|%012d|%02d".formatted(jour, ORDRE.indexOf(p.type()), p.id(), ORDRE_EVENEMENTS.indexOf(p.evenement()));
    }

    public static boolean unitaire(String type) {
        return UNITAIRES.contains(type);
    }

    private static String exigerType(String type) {
        if (!SourcesRepository.types().contains(type)) {
            throw new CommonException("Type de pièce inconnu : " + type, HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TYPE_INCONNU");
        }
        return type;
    }

    private PieceFileDTO versDto(PieceEtat p) {
        List<AnomaliePieceDTO> anomalies = p.proposition() == null ? List.of() : p.proposition().anomalies().stream().map(a -> new AnomaliePieceDTO(a.code(), a.message(), a.bloquante())).toList();
        Lien l = p.lien();
        if (p.piece() == null) {
            Map<String, String> i = p.instantane();
            return new PieceFileDTO(p.type(), p.id(), p.evenement(), i.get("numero"), i.get("date") == null ? null : LocalDate.parse(i.get("date")), l.pointDeVenteId(), i.get("tiersNom"),
                    i.get("montant") == null ? null : new BigDecimal(i.get("montant")), p.etat(), p.empreinte(), l.version(), l.ecritureId(), anomalies, unitaire(p.type()));
        }
        PieceSource s = p.piece();
        TiersSource t = s.tiers();
        LocalDate date = p.proposition() != null ? p.proposition().datePiece() : s.date();
        String nom = t != null ? t.nom() : "VENTE".equals(s.type()) || s.attr("vente_id") != null ? ReglesComptabilisation.CLIENTS_DIVERS.nom() : null;
        return new PieceFileDTO(p.type(), p.id(), p.evenement(), s.numero(), date, s.pointDeVenteId(), nom, s.montant(), p.etat(), p.empreinte(), l == null ? null : l.version(),
                l != null && l.actif() ? l.ecritureId() : null, anomalies, unitaire(p.type()));
    }

    private TiersProposeDTO tiersLu(Long livreId, Long tiersId) {
        if (tiersId == null) {
            return null;
        }
        return tiers.lire(livreId, tiersId).map(t -> new TiersProposeDTO(t.id(), t.codeAuxiliaire(), t.nom())).orElse(null);
    }

    private TiersProposeDTO tiersPropose(Long livreId, TiersSource source) {
        if (source == null) {
            return null;
        }
        TiersDTO existant = (source.id() == null ? tiers.parNom(livreId, source.nom()) : tiers.parSource(livreId, source.type(), source.id())).orElse(null);
        return existant != null ? new TiersProposeDTO(existant.id(), existant.codeAuxiliaire(), existant.nom()) : new TiersProposeDTO(null, null, source.nom());
    }
}

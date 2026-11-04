package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComplementsRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.EcritureEnregistree;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.PasserelleRepository;
import com.gestion.repository.compta.PasserelleRepository.Lien;
import com.gestion.repository.compta.TiersRepository;
import com.gestion.service.compta.FileComptabilisationService.PieceEtat;
import com.gestion.service.compta.PieceSource.TiersSource;
import com.gestion.service.compta.PropositionEcriture.AnomaliePiece;
import com.gestion.service.compta.PropositionEcriture.LigneProposee;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le travail d'une pièce, dans sa propre transaction : relire la source, contre-passer la version active si besoin, appliquer la règle, poster, écrire le lien.
 * Une pièce qui échoue n'emporte pas les autres. La double comptabilisation est refusée par la base, même à deux en parallèle (uk_compta_lie).
 */
@Service
public class PosteurPiece {

    /** @param ecritureId l'écriture passée : le miroir, si la pièce est seulement annulée ou disparue */
    public record Poste(Long ecritureId, String numeroComplet) {
    }

    private final FileComptabilisationService file;
    private final PasserelleRepository passerelle;
    private final PosteurEcritures posteur;
    private final JournalRepository journal;
    private final JournauxRepository journaux;
    private final ExerciceRepository exercices;
    private final TiersRepository tiers;
    private final LettrageService lettrage;
    private final DatesComptables dates;
    private final LivreCourantService livreCourant;
    private final ObjectMapper json;

    public PosteurPiece(FileComptabilisationService file, PasserelleRepository passerelle, PosteurEcritures posteur, JournalRepository journal, JournauxRepository journaux,
                        ExerciceRepository exercices, TiersRepository tiers, LettrageService lettrage, DatesComptables dates, LivreCourantService livreCourant, ObjectMapper json) {
        this.file = file;
        this.passerelle = passerelle;
        this.posteur = posteur;
        this.journal = journal;
        this.journaux = journaux;
        this.exercices = exercices;
        this.tiers = tiers;
        this.lettrage = lettrage;
        this.dates = dates;
        this.livreCourant = livreCourant;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Poste traiter(Long livreId, PieceDemandee d) {
        garderComplements(livreId, d);
        PieceEtat p = file.une(livreId, d.sourceType(), d.sourceId(), d.evenement());
        refuserSiChangee(d, p);
        switch (p.etat()) {
            case FileComptabilisationService.A_COMPTABILISER -> {
                return poster(livreId, p, p.proposition(), p.lien() == null ? 1 : p.lien().version() + 1);
            }
            case FileComptabilisationService.MODIFIEE -> {
                inverser(livreId, p.lien(), null);
                return poster(livreId, p, p.proposition(), p.lien().version() + 1);
            }
            case FileComptabilisationService.ANNULEE -> {
                if ("VENTE".equals(p.type())) {
                    return annulerVente(livreId, p);
                }
                return inverser(livreId, p.lien(), p.piece().dateAnnulation());
            }
            case FileComptabilisationService.DISPARUE -> {
                return inverser(livreId, p.lien(), null);
            }
            case FileComptabilisationService.ANOMALIE -> {
                AnomaliePiece a = p.proposition().anomalies().stream().filter(AnomaliePiece::bloquante).findFirst()
                        .orElse(new AnomaliePiece("COMPTA_PIECE_ANOMALIE", "La pièce est en anomalie.", true));
                throw new CommonException(a.message(), HttpStatus.UNPROCESSABLE_ENTITY, a.code());
            }
            default -> throw new CommonException("Rien à faire : la pièce est " + p.etat() + ".", HttpStatus.CONFLICT, "COMPTA_PIECE_RIEN_A_FAIRE");
        }
    }

    /** Une écriture pour plusieurs ventes d'un même jour, point de vente et client. Chacune garde son lien vers elle. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Poste traiterVentes(Long livreId, List<PieceDemandee> demandes, String refPiece) {
        List<PieceEtat> pieces = new ArrayList<>();
        List<PropositionEcriture> propositions = new ArrayList<>();
        for (PieceDemandee d : demandes) {
            PieceEtat p = file.une(livreId, d.sourceType(), d.sourceId(), d.evenement());
            refuserSiChangee(d, p);
            if (!FileComptabilisationService.A_COMPTABILISER.equals(p.etat())) {
                throw new CommonException("La vente " + p.piece().numero() + " est " + p.etat() + " : elle ne se regroupe pas.", HttpStatus.CONFLICT, "COMPTA_PIECE_RIEN_A_FAIRE");
            }
            pieces.add(p);
            propositions.add(p.proposition());
        }
        PropositionEcriture groupee = ReglesComptabilisation.regrouperVentes(propositions, refPiece);
        EcritureEnregistree ecriture = posterEcriture(livreId, groupee, pieces.get(0).piece().pointDeVenteId(), null);
        for (PieceEtat p : pieces) {
            ecrireLien(livreId, p, p.lien() == null ? 1 : p.lien().version() + 1, ecriture.id());
        }
        return new Poste(ecriture.id(), numero(livreId, ecriture.id()));
    }

    /** Une vente annulée après regroupement : l'écriture commune ne se contre-passe pas, seule la part de cette vente s'inverse. */
    private Poste annulerVente(Long livreId, PieceEtat p) {
        PropositionEcriture r = p.proposition();
        List<LigneProposee> inverses = r.lignes().stream().map(l -> new LigneProposee(l.compte(), l.tiers(), l.credit(), l.debit(), l.taux(), l.base(), l.natureMouvement(), l.horsFlux())).toList();
        LocalDate date = p.piece().dateAnnulation() != null ? p.piece().dateAnnulation() : r.datePiece();
        PropositionEcriture annulation = new PropositionEcriture("ANNULATION", r.journal(), date, r.refPiece(), "Annulation de la vente " + p.piece().numero(), inverses, List.of(), List.of());
        EcritureEnregistree ecriture = posterEcriture(livreId, annulation, p.piece().pointDeVenteId(), null);
        passerelle.ecrireLien(livreId, p.type(), p.id(), "ANNULATION", 1, p.piece().pointDeVenteId(), p.empreinte(), instantane(p), ecriture.id(), livreCourant.auteur());
        return new Poste(ecriture.id(), numero(livreId, ecriture.id()));
    }

    private void garderComplements(Long livreId, PieceDemandee d) {
        ComplementsRequest c = d.complements();
        if (c != null && (c.compteFinancierId() != null || c.dateEffet() != null || c.referenceDocument() != null)) {
            passerelle.ecrireComplement(livreId, d.sourceType(), d.sourceId(), d.evenement(),
                    new PasserelleRepository.Complement(c.compteFinancierId(), c.dateEffet(), c.referenceDocument()), livreCourant.auteur());
        }
    }

    private static void refuserSiChangee(PieceDemandee d, PieceEtat p) {
        if (d.empreinteVue() != null && !d.empreinteVue().equals(p.empreinte())) {
            throw new CommonException("La pièce a changé depuis que vous l'avez vue : relisez-la.", HttpStatus.CONFLICT, "COMPTA_PIECE_MODIFIEE");
        }
    }

    private Poste poster(Long livreId, PieceEtat p, PropositionEcriture r, int version) {
        EcritureEnregistree ecriture = posterEcriture(livreId, r, p.piece().pointDeVenteId(), null);
        ecrireLien(livreId, p, version, ecriture.id());
        if (ReglesComptabilisation.REJET.equals(p.evenement()) && "ENCAISSEMENT_CLIENT".equals(p.piece().attr("sens"))) {
            lettrerRejet(livreId, p, ecriture.id());
        }
        return new Poste(ecriture.id(), numero(livreId, ecriture.id()));
    }

    private void ecrireLien(Long livreId, PieceEtat p, int version, Long ecritureId) {
        passerelle.ecrireLien(livreId, p.type(), p.id(), p.evenement(), version, p.piece().pointDeVenteId(), p.empreinte(), instantane(p), ecritureId, livreCourant.auteur());
    }

    private EcritureEnregistree posterEcriture(Long livreId, PropositionEcriture r, Long pointDeVenteId, Long corrige) {
        JournalDTO j = journaux.lister(livreId).stream().filter(x -> x.code().equals(r.journal())).findFirst().orElseThrow(() -> new CommonException(
                "Le journal " + r.journal() + " n'existe pas dans ce livre.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_JOURNAL_INCONNU"));
        LocalDate date = dates.resoudre(livreId, j.id(), r.datePiece());
        List<NouvelleLigne> lignes = new ArrayList<>();
        Map<String, Long> tiersVus = new LinkedHashMap<>();
        for (LigneProposee l : r.lignes()) {
            Long tiersId = l.tiers() == null ? null : tiersVus.computeIfAbsent(l.tiers().type() + "|" + l.tiers().id() + "|" + l.tiers().nom(), k -> assurerTiers(livreId, l.tiers(), l.compte()));
            lignes.add(new NouvelleLigne(l.compte(), tiersId, l.debit(), l.credit(), null, null, l.natureMouvement(), l.horsFlux(), null, null, l.taux(), l.base(), null, pointDeVenteId));
        }
        return posteur.poster(livreId, new PosteurEcritures.Proposition(j.id(), date, r.datePiece(), r.refPiece(), r.libelle(), "COURANTE", "PASSERELLE", null, corrige, null, false, null, null, lignes));
    }

    /** Le tiers comptable naît de la première pièce qui le demande, depuis le commercial. */
    private Long assurerTiers(Long livreId, TiersSource source, String compteLigne) {
        var existant = source.id() == null ? tiers.parNom(livreId, source.nom()) : tiers.parSource(livreId, source.type(), source.id());
        if (existant.isPresent()) {
            return existant.get().id();
        }
        String collectif = "CLIENT".equals(source.type()) ? "3421" : "4411";
        if (source.id() == null) {
            return tiers.creer(livreId, new TiersCreationRequest(source.type(), source.nom(), null, null, collectif, null, null, null));
        }
        var identite = tiers.identiteSource(livreId, source.type(), source.id());
        return tiers.creerDepuisSource(livreId, source.type(), source.id(), identite.map(TiersRepository.IdentiteSource::nom).orElse(source.nom()),
                identite.map(TiersRepository.IdentiteSource::ice).orElse(null), identite.map(TiersRepository.IdentiteSource::identifiantFiscal).orElse(null), collectif);
    }

    /**
     * Même exercice : le miroir, lettré avec son origine. Exercice clos : une écriture de correction, car ses comptes de gestion ont été soldés ;
     * les classes 6 et 7 passent sur le compte des exercices antérieurs du même poste.
     */
    private Poste inverser(Long livreId, Lien lien, LocalDate dateAnnulation) {
        EcritureDTO origine = journal.lire(livreId, lien.ecritureId()).orElseThrow();
        ExerciceDTO exercice = exercices.lire(livreId, origine.exerciceId()).orElseThrow();
        List<LigneJournal> lignesOrigine = journal.lignesBrutes(livreId, lien.ecritureId());
        LocalDate reference = dateAnnulation != null ? dateAnnulation : origine.dateEcriture();
        boolean clos = "CLOTURE".equals(exercice.statut());
        LocalDate date = dates.resoudre(livreId, origine.journal().id(), reference);
        List<NouvelleLigne> lignes = new ArrayList<>();
        for (LigneJournal l : lignesOrigine) {
            String compte = clos ? compteExercicesAnterieurs(l.compte()) : l.compte();
            lignes.add(new NouvelleLigne(compte, l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(), l.devise(), l.montantDevise(),
                    l.tauxTva(), l.baseTva(), clos ? null : l.id(), l.pointDeVenteId()));
        }
        String prefixe = clos ? "Correction de " : "Contre-passation de ";
        EcritureEnregistree ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), date, origine.datePiece(), origine.refPiece(), prefixe + origine.numeroComplet(),
                "COURANTE", "PASSERELLE", clos ? null : lien.ecritureId(), clos ? lien.ecritureId() : null, "Pièce annulée, modifiée ou disparue", false, null, null, lignes));
        if (!clos) {
            lettrage.lettrerMiroir(livreId, lignesOrigine, journal.lignesBrutes(livreId, ecriture.id()), date);
        }
        return new Poste(ecriture.id(), numero(livreId, ecriture.id()));
    }

    /** Le 8 en quatrième position indique les exercices antérieurs. Les postes 613 et 614 partagent 6148. */
    static String compteExercicesAnterieurs(String compte) {
        if (!compte.startsWith("6") && !compte.startsWith("7")) {
            return compte;
        }
        if (compte.startsWith("613") || compte.startsWith("614")) {
            return "6148";
        }
        return compte.substring(0, 3) + "8";
    }

    /** La facture redevient due : le groupe du paiement se défait, et la ligne du paiement se lettre avec celle du rejet. */
    private void lettrerRejet(Long livreId, PieceEtat p, Long ecritureRejet) {
        if (p.piece().attr("paiement_id") == null) {
            return;
        }
        passerelle.dernierLien(livreId, "PAIEMENT", Long.valueOf(p.piece().attr("paiement_id")), ReglesComptabilisation.COMPTABILISATION).filter(Lien::actif).ifPresent(lien -> {
            LigneJournal lignePaiement = journal.lignesBrutes(livreId, lien.ecritureId()).stream().filter(l -> "3421".equals(l.compte())).findFirst().orElse(null);
            LigneJournal ligneRejet = journal.lignesBrutes(livreId, ecritureRejet).stream().filter(l -> "3421".equals(l.compte())).findFirst().orElse(null);
            if (lignePaiement != null && ligneRejet != null) {
                lettrage.lettrerRejet(livreId, lignePaiement, ligneRejet, journal.lire(livreId, ecritureRejet).orElseThrow().dateEcriture());
            }
        });
    }

    private String numero(Long livreId, Long ecritureId) {
        return journal.lire(livreId, ecritureId).map(EcritureDTO::numeroComplet).orElse(null);
    }

    private String instantane(PieceEtat p) {
        PieceSource s = p.piece();
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("numero", s.numero());
        valeurs.put("date", s.date() == null ? null : s.date().toString());
        BigDecimal montant = s.montant();
        valeurs.put("montant", montant == null ? null : montant.toPlainString());
        valeurs.put("tiersNom", s.tiers() == null ? null : s.tiers().nom());
        valeurs.put("factureOrigineId", s.attr("facture_origine_id"));
        try {
            return json.writeValueAsString(valeurs);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}

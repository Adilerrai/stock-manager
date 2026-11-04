package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.PasserelleDtos.AnomaliePieceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.ComptabiliserRequest;
import com.gestion.persistent.dto.compta.PasserelleDtos.LotDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.PieceDemandee;
import com.gestion.persistent.dto.compta.PasserelleDtos.ResultatPieceDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.VentilationLigneDTO;
import com.gestion.persistent.dto.compta.PasserelleDtos.VentilationRequest;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.PasserelleRepository;
import com.gestion.repository.compta.PasserelleRepository.ResultatLu;
import com.gestion.repository.compta.PasserelleRepository.Ventilation;
import com.gestion.repository.compta.SourcesRepository;
import com.gestion.service.AuditService;
import com.gestion.service.compta.FileComptabilisationService.PieceEtat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parcourt un lot par date, appelle le posteur pièce par pièce, et garde le rapport à part : un échec n'annule pas les autres pièces et ne perd pas le compte rendu.
 * Aucune transaction ici : deux pièces n'en partagent jamais une.
 */
@Service
public class MoteurComptabilisation {

    private static final Logger LOG = LoggerFactory.getLogger(MoteurComptabilisation.class);
    private static final Pattern CONTRAINTE = Pattern.compile("constraint \"([a-z_0-9]+)\"");

    private final FileComptabilisationService file;
    private final PosteurPiece posteur;
    private final PasserelleRepository passerelle;
    private final LivreCourantService livreCourant;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public MoteurComptabilisation(FileComptabilisationService file, PosteurPiece posteur, PasserelleRepository passerelle, LivreCourantService livreCourant, AuditService audit,
                                  PlatformTransactionManager transactions) {
        this.file = file;
        this.posteur = posteur;
        this.passerelle = passerelle;
        this.livreCourant = livreCourant;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactions);
    }

    public LotDTO comptabiliser(ComptabiliserRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        String auteur = livreCourant.auteur();
        Long lotId = transaction.execute(s -> passerelle.ouvrirLot(livreId, demande.pieces().size(), auteur));
        boolean seule = demande.pieces().size() == 1;

        List<ResultatPieceDTO> resultats = new ArrayList<>();
        List<Tache> taches = new ArrayList<>();
        for (PieceDemandee d : avecPrealables(livreId, demande.pieces())) {
            if (FileComptabilisationService.unitaire(d.sourceType()) && !seule) {
                resultats.add(echec(d, "COMPTA_CONFIRMATION_UNITAIRE", "Cette pièce se confirme seule, jamais en lot."));
                continue;
            }
            taches.add(new Tache(d, lireOuNul(livreId, d)));
        }
        taches.sort(Comparator.comparing(t -> t.piece() == null ? "9999" : FileComptabilisationService.cle(t.piece())));

        // Les ventes à comptabiliser d'un même jour, point de vente et client tiennent en une écriture
        Map<String, List<Tache>> ventes = new LinkedHashMap<>();
        for (Tache t : taches) {
            if (estVenteARegrouper(t)) {
                ventes.computeIfAbsent(cleVente(t.piece()), k -> new ArrayList<>()).add(t);
            }
        }
        for (Tache t : taches) {
            if (estVenteARegrouper(t)) {
                List<Tache> groupe = ventes.remove(cleVente(t.piece()));
                if (groupe != null) {
                    resultats.addAll(executerGroupe(livreId, groupe));
                }
            } else {
                resultats.add(executer(livreId, t.demande()));
            }
        }

        int reussies = (int) resultats.stream().filter(ResultatPieceDTO::reussi).count();
        int echecs = resultats.size() - reussies;
        transaction.executeWithoutResult(s -> {
            for (ResultatPieceDTO r : resultats) {
                passerelle.ecrireResultat(livreId, lotId, r.sourceType(), r.sourceId(), r.evenement(), r.reussi(), r.ecritureId(), r.codeErreur(), tronquer(r.message()));
            }
            passerelle.fermerLot(livreId, lotId, reussies, echecs);
            audit.logAction(ActionAudit.DEVERSEMENT, "compta_lots", lotId, reussies + " pièce(s) comptabilisée(s), " + echecs + " échec(s)");
        });
        return new LotDTO(lotId, reussies, echecs, resultats);
    }

    /** Si la remise d'un effet n'est pas passée, elle passe d'abord : l'écran montre les deux écritures. */
    private List<PieceDemandee> avecPrealables(Long livreId, List<PieceDemandee> pieces) {
        List<PieceDemandee> liste = new ArrayList<>();
        for (PieceDemandee d : pieces) {
            if ("EFFET".equals(d.sourceType()) && !ReglesComptabilisation.REMISE.equals(d.evenement()) && pieces.stream().noneMatch(x -> x.sourceId().equals(d.sourceId()) && ReglesComptabilisation.REMISE.equals(x.evenement()))) {
                PieceEtat remise = lireOuNul(livreId, new PieceDemandee(d.sourceType(), d.sourceId(), ReglesComptabilisation.REMISE, null, null));
                if (remise != null && FileComptabilisationService.A_COMPTABILISER.equals(remise.etat())) {
                    liste.add(new PieceDemandee(d.sourceType(), d.sourceId(), ReglesComptabilisation.REMISE, null, null));
                }
            }
            liste.add(d);
        }
        return liste;
    }

    private PieceEtat lireOuNul(Long livreId, PieceDemandee d) {
        try {
            return file.une(livreId, d.sourceType(), d.sourceId(), d.evenement());
        } catch (CommonException e) {
            return null;
        }
    }

    private static boolean estVenteARegrouper(Tache t) {
        return "VENTE".equals(t.demande().sourceType()) && t.piece() != null && FileComptabilisationService.A_COMPTABILISER.equals(t.piece().etat());
    }

    private static String cleVente(PieceEtat p) {
        PieceSource s = p.piece();
        return s.date() + "|" + s.pointDeVenteId() + "|" + (s.tiers() == null ? "" : s.tiers().id());
    }

    private List<ResultatPieceDTO> executerGroupe(Long livreId, List<Tache> groupe) {
        List<PieceDemandee> demandes = groupe.stream().map(Tache::demande).toList();
        List<ResultatPieceDTO> resultats = new ArrayList<>();
        try {
            PosteurPiece.Poste poste = posteur.traiterVentes(livreId, demandes, "VENTES-" + groupe.get(0).piece().piece().date());
            demandes.forEach(d -> resultats.add(reussite(d, poste)));
        } catch (RuntimeException e) {
            demandes.forEach(d -> resultats.add(depuisErreur(d, e)));
        }
        return resultats;
    }

    private ResultatPieceDTO executer(Long livreId, PieceDemandee d) {
        try {
            return reussite(d, posteur.traiter(livreId, d));
        } catch (RuntimeException e) {
            return depuisErreur(d, e);
        }
    }

    private static ResultatPieceDTO reussite(PieceDemandee d, PosteurPiece.Poste p) {
        return new ResultatPieceDTO(d.sourceType(), d.sourceId(), d.evenement(), true, p.ecritureId(), p.numeroComplet(), null, null);
    }

    private static ResultatPieceDTO echec(PieceDemandee d, String code, String message) {
        return new ResultatPieceDTO(d.sourceType(), d.sourceId(), d.evenement(), false, null, null, code, message);
    }

    /** Un refus de la base se rapporte sur la pièce, avec le nom de sa contrainte. Une erreur inattendue reste dans le journal du serveur, sans détail technique à l'écran. */
    private static ResultatPieceDTO depuisErreur(PieceDemandee d, RuntimeException e) {
        if (e instanceof CommonException c) {
            return echec(d, c.getErrorCode(), c.getMessage());
        }
        if (e instanceof DataAccessException a && a.getMostSpecificCause().getMessage() != null) {
            Matcher m = CONTRAINTE.matcher(a.getMostSpecificCause().getMessage());
            if (m.find()) {
                return echec(d, m.group(1), "Refusé par la base : " + m.group(1) + ".");
            }
        }
        LOG.error("Pièce {} {} non comptabilisée", d.sourceType(), d.sourceId(), e);
        return echec(d, "COMPTA_ERREUR_INATTENDUE", "Erreur inattendue : voir le journal du serveur.");
    }

    private static String tronquer(String message) {
        return message == null || message.length() <= 500 ? message : message.substring(0, 500);
    }

    @Transactional(readOnly = true)
    public LotDTO lot(Long lotId) {
        Long livreId = livreCourant.exiger().livreId();
        if (!passerelle.lotExiste(livreId, lotId)) {
            throw new ResourceNotFoundException("Lot", "id", lotId);
        }
        List<ResultatLu> lus = passerelle.resultats(livreId, lotId);
        List<ResultatPieceDTO> resultats = lus.stream().map(r -> new ResultatPieceDTO(r.sourceType(), r.sourceId(), r.evenement(), r.reussi(), r.ecritureId(), r.numeroComplet(), r.codeErreur(), r.message())).toList();
        int reussies = (int) resultats.stream().filter(ResultatPieceDTO::reussi).count();
        return new LotDTO(lotId, reussies, resultats.size() - reussies, resultats);
    }

    @Transactional
    public void ecarter(String type, Long id, String motif) {
        Long livreId = livreCourant.exiger().livreId();
        exigerType(type);
        passerelle.ecarter(livreId, type, id, motif.trim(), livreCourant.auteur());
    }

    @Transactional
    public void reprendre(String type, Long id) {
        Long livreId = livreCourant.exiger().livreId();
        exigerType(type);
        passerelle.reprendre(livreId, type, id);
    }

    /** Ce que la source ne porte pas : HT et TVA d'une dépense, ligne immobilisée, numéro de facture du fournisseur. Une ventilation change l'empreinte. */
    @Transactional
    public List<AnomaliePieceDTO> ventiler(String type, Long id, VentilationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        exigerType(type);
        List<Ventilation> lignes = new ArrayList<>();
        for (VentilationLigneDTO l : demande.lignes()) {
            if (l.montantHt().signum() < 0) {
                throw new CommonException("Le hors taxes ne peut pas être négatif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_VENTILATION_INVALIDE");
            }
            lignes.add(new Ventilation(l.compte().trim(), l.montantHt(), l.tauxTva() == null ? BigDecimal.ZERO : l.tauxTva(), l.montantTva() == null ? BigDecimal.ZERO : l.montantTva(),
                    !Boolean.FALSE.equals(l.tvaDeductible()), l.motifNonDeductible(), Boolean.TRUE.equals(l.estImmobilisation()), l.numeroFactureFournisseur(), l.dateFactureFournisseur(), l.rubriqueTva()));
        }
        passerelle.remplacerVentilation(livreId, type, id, lignes, livreCourant.auteur());
        return List.of();
    }

    private static void exigerType(String type) {
        if (!SourcesRepository.types().contains(type)) {
            throw new CommonException("Type de pièce inconnu : " + type, HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_TYPE_INCONNU");
        }
    }

    private record Tache(PieceDemandee demande, PieceEtat piece) {
    }
}

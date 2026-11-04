package com.gestion.service.compta;

import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.repository.compta.LivreRepository.LigneLivre;
import com.gestion.persistent.dto.compta.RapprochementDtos.LigneClasseeDTO;
import com.gestion.repository.compta.ParametreFiscalRepository;
import com.gestion.repository.compta.RapprochementComptaRepository;
import com.gestion.repository.compta.TvaRepository;
import com.gestion.repository.compta.TvaRepository.Acceptation;
import com.gestion.repository.compta.TvaRepository.Declaration;
import com.gestion.repository.compta.TvaRepository.FactureAchat;
import com.gestion.repository.compta.TvaRepository.IdentiteSource;
import com.gestion.repository.compta.TvaRepository.LigneRetenue;
import com.gestion.repository.compta.TvaRepository.OperationCollectee;
import com.gestion.repository.compta.TvaRepository.OperationDeductible;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Les trente et un contrôles de la déclaration. Un contrôle qui n'a pas tourné s'affiche « non effectué », jamais comme réussi.
 * Un motif déjà donné sur une alerte survit au recalcul tant que la même alerte revient, avec les mêmes montants.
 */
@Component
public class TvaControles {

    private static final BigDecimal UN_CENTIME = new BigDecimal("0.01");
    private static final BigDecimal DEUX_CENTIMES = new BigDecimal("0.02");
    private static final BigDecimal CENT = new BigDecimal("100");
    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CHIFFRES = Pattern.compile("\\d+");
    /** Les rubriques que l'art. 106-I exclut en partie : frais de mission, de réception. */
    private static final Set<String> RUBRIQUES_A_VOIR = Set.of("HOTEL");
    private static final BigDecimal LIMITE_ZIP = new BigDecimal("1048576");

    private final TvaRepository repository;
    private final LivreRepository livres;
    private final ParametreFiscalRepository parametres;
    private final FormulaireTva formulaire;
    private final RapprochementComptaRepository rapprochement;

    public TvaControles(TvaRepository repository, LivreRepository livres, ParametreFiscalRepository parametres, FormulaireTva formulaire, RapprochementComptaRepository rapprochement) {
        this.rapprochement = rapprochement;
        this.repository = repository;
        this.livres = livres;
        this.parametres = parametres;
        this.formulaire = formulaire;
    }

    private static String m(BigDecimal montant) {
        return MontantTexte.fr(montant);
    }

    public record Constat(String code, String gravite, String message, String objetType, Long objetId) {
    }

    /** Rejoue tout, et garde les motifs déjà donnés. */
    public void rejouer(Long livreId, Declaration d, TvaParametresDTO p, BigDecimal creditAttendu, boolean precedenteAbsente, boolean creditSaisi, long tailleZip, String remiseMode) {
        Map<String, Acceptation> acceptations = new HashMap<>();
        repository.acceptations(livreId, d.id()).forEach(a -> acceptations.put(cleAcceptation(a.code(), a.message()), a));
        repository.viderControles(livreId, d.id());
        for (Constat c : constats(livreId, d, p, creditAttendu, precedenteAbsente, creditSaisi, tailleZip, remiseMode)) {
            Acceptation a = acceptations.get(cleAcceptation(c.code(), c.message()));
            boolean accepte = a != null && !"NON_EFFECTUE".equals(c.gravite());
            repository.ecrireControle(livreId, d.id(), c.code(), c.gravite(), c.message(), c.objetType(), c.objetId(), accepte ? "ACCEPTE" : "OUVERT", accepte ? a.motif() : null, accepte ? a.auteur() : null);
        }
    }

    /** Par le contenu : un recalcul refait les opérations et leurs identifiants. Si le montant change, l'alerte n'est plus la même et demande un nouveau motif. */
    private static String cleAcceptation(String code, String message) {
        return code + "|" + (message.length() > 500 ? message.substring(0, 500) : message);
    }

    public List<Constat> constats(Long livreId, Declaration d, TvaParametresDTO p, BigDecimal creditAttendu, boolean precedenteAbsente, boolean creditSaisi, long tailleZip, String remiseMode) {
        List<Constat> constats = new ArrayList<>();
        List<OperationCollectee> collectees = repository.operationsCollectees(livreId, d.id());
        List<OperationDeductible> deductibles = repository.operationsDeductibles(livreId, d.id());
        Map<String, LigneRetenue> lignes = new HashMap<>();
        repository.lignes(livreId, d.id()).forEach(l -> lignes.put(l.code(), l));
        identite(livreId, d, constats);
        // Sans numéro du fournisseur saisi en ventilation, seul le numéro du commercial existe : il peut avoir été généré.
        Set<String> numerosInternes = repository.facturesAchat(livreId, deductibles.stream().map(OperationDeductible::factureAchatId).filter(Objects::nonNull).distinct().toList()).stream()
                .map(FactureAchat::numero).collect(Collectors.toSet());
        for (OperationDeductible o : deductibles) {
            operation(d, o, numerosInternes, constats);
        }
        collectees(d, collectees, constats);
        recoupements(livreId, d, deductibles, lignes, constats);
        especes(deductibles, constats);
        credit(d, lignes, creditAttendu, precedenteAbsente, creditSaisi, constats);
        remises(livreId, collectees, remiseMode, constats);
        if (BigDecimal.valueOf(tailleZip).compareTo(LIMITE_ZIP) > 0) {
            constats.add(new Constat("T28", "ALERTE", "Le fichier du relevé pèse " + tailleZip + " octets, au-dessus de la limite prudente de 1 Mo.", "DECLARATION", d.id()));
        }
        journal(livreId, d, constats);
        periodicite(livreId, d, constats);
        releve(livreId, d, collectees, deductibles, constats);
        return constats;
    }

    private void identite(Long livreId, Declaration d, List<Constat> constats) {
        LigneLivre livre = livres.lire(livreId);
        String fiscal = livre.identifiantFiscal();
        if (fiscal == null || !CHIFFRES.matcher(fiscal).matches() || fiscal.length() > 8) {
            constats.add(new Constat("T01", "BLOQUANT", "L'identifiant fiscal du dossier manque ou n'est pas fait de 8 chiffres au plus : " + (fiscal == null ? "absent" : fiscal) + ".", "LIVRE", livreId));
        }
        int maximum = TvaCalcul.nombreDePeriodes(d.periodicite());
        if (d.numeroPeriode() < 1 || d.numeroPeriode() > maximum || d.regimeDgi() != TvaCalcul.regimeDgi(d.periodicite())) {
            constats.add(new Constat("T02", "BLOQUANT", "Période " + d.numeroPeriode() + " et régime " + d.regimeDgi() + " incohérents pour une déclaration " + d.periodicite().toLowerCase() + ".", "DECLARATION", d.id()));
        }
        for (IdentiteSource s : repository.identitesSources(livreId)) {
            if (s.identifiantFiscal() != null && !s.identifiantFiscal().equals(fiscal)) {
                constats.add(new Constat("T31", "BLOQUANT", "IF du dossier (" + fiscal + ") différent de celui du point de vente " + s.pointDeVenteId() + " (" + s.identifiantFiscal() + ").", "LIVRE", livreId));
            }
            if (s.ice() != null && !s.ice().equals(livre.ice())) {
                constats.add(new Constat("T31", "BLOQUANT", "ICE du dossier (" + (livre.ice() == null ? "absent" : livre.ice()) + ") différent de celui du point de vente " + s.pointDeVenteId() + " (" + s.ice() + ").", "LIVRE", livreId));
            }
        }
    }

    private void operation(Declaration d, OperationDeductible o, Set<String> numerosInternes, List<Constat> constats) {
        String nom = "Facture " + o.numeroFacture() + " de " + o.fournisseurNom();
        LocalDate plancher = LocalDate.of(2000, 1, 1);
        LocalDate plafond = d.au().plusYears(1);
        boolean regularisation = "REGULARISATION".equals(o.sourceType());
        if (o.dateFacture().isBefore(plancher) || o.dateFacture().isAfter(plafond) || o.datePaiement().isBefore(plancher) || o.datePaiement().isAfter(d.au())) {
            constats.add(new Constat("T03", "BLOQUANT", nom + " : date de facture " + FR.format(o.dateFacture()) + " ou de paiement " + FR.format(o.datePaiement()) + " invalide.", "DEDUCTIBLE", o.id()));
        }
        if (o.mpId() < 1 || o.mpId() > 7 || (o.mpId() != 1 && (o.reference() == null || o.reference().isBlank()))) {
            constats.add(new Constat("T04", "BLOQUANT", nom + " : mode de paiement " + o.mpId() + " sans référence de paiement.", "DEDUCTIBLE", o.id()));
        }
        if (o.fournisseurIce() == null || o.fournisseurIce().isBlank()) {
            constats.add(new Constat("T05", "BLOQUANT", nom + " : ICE du fournisseur absent.", "DEDUCTIBLE", o.id()));
        } else if (o.fournisseurIce().length() != 15 || !CHIFFRES.matcher(o.fournisseurIce()).matches()) {
            constats.add(new Constat("T06", "ALERTE", nom + " : l'ICE " + o.fournisseurIce() + " ne compte pas 15 chiffres.", "DEDUCTIBLE", o.id()));
        }
        if (o.fournisseurIf() == null || o.fournisseurIf().isBlank()) {
            constats.add(new Constat("T07", "BLOQUANT", nom + " : IF du fournisseur absent.", "DEDUCTIBLE", o.id()));
        } else if (o.fournisseurIf().length() > 8 || !CHIFFRES.matcher(o.fournisseurIf()).matches()) {
            constats.add(new Constat("T07", "ALERTE", nom + " : l'IF " + o.fournisseurIf() + " n'est pas fait de 8 chiffres au plus.", "DEDUCTIBLE", o.id()));
        }
        if (o.ht().add(o.tva()).subtract(o.ttc()).abs().compareTo(UN_CENTIME) > 0) {
            constats.add(new Constat("T08", "BLOQUANT", nom + " : HT " + m(o.ht()) + " + TVA " + m(o.tva()) + " différent du TTC " + m(o.ttc()) + ".", "DEDUCTIBLE", o.id()));
        }
        BigDecimal attendue = o.ht().multiply(o.taux()).divide(CENT, 2, RoundingMode.HALF_UP);
        if (o.tva().subtract(attendue).abs().compareTo(DEUX_CENTIMES) > 0) {
            constats.add(new Constat("T09", "ALERTE", nom + " : TVA " + m(o.tva()) + " pour " + m(attendue) + " attendus à " + m(o.taux()) + " %.", "DEDUCTIBLE", o.id()));
        }
        if (o.ligneCode() == null) {
            constats.add(new Constat("T12", "ALERTE", nom + " : la rubrique " + o.rubrique() + " n'a pas de ligne au taux de " + m(o.taux()) + " %.", "DEDUCTIBLE", o.id()));
        }
        bareme(o.taux(), o.dateFacture(), nom, "DEDUCTIBLE", o.id(), constats);
        if (o.datePaiement().isBefore(d.du()) && !regularisation) {
            constats.add(new Constat("T15", "ALERTE", nom + " : payée le " + FR.format(o.datePaiement()) + ", hors de la période.", "DEDUCTIBLE", o.id()));
            if (o.datePaiement().plusMonths(12).isBefore(d.du())) {
                constats.add(new Constat("T16", "ALERTE_FORTE", nom + " : payée il y a plus de 12 mois, droit à déduction à examiner (art. 101-3° et 204 bis).", "DEDUCTIBLE", o.id()));
            }
        }
        if (o.datePaiement().isBefore(o.dateFacture()) && !regularisation) {
            constats.add(new Constat("T17", "ALERTE", nom + " : payée le " + FR.format(o.datePaiement()) + ", avant sa date du " + FR.format(o.dateFacture()) + ".", "DEDUCTIBLE", o.id()));
        }
        if (RUBRIQUES_A_VOIR.contains(o.rubrique())) {
            constats.add(new Constat("T19", "ALERTE", nom + " : la rubrique " + o.rubrique() + " peut tomber sous l'exclusion de l'art. 106-I.", "DEDUCTIBLE", o.id()));
        }
        if (numerosInternes.contains(o.numeroFacture())) {
            constats.add(new Constat("T20", "ALERTE", nom + " : numéro du commercial, peut-être interne. Indiquez celui de la facture du fournisseur.", "DEDUCTIBLE", o.id()));
        }
        if (regularisation) {
            constats.add(new Constat("T32", "ALERTE", nom + " : la source a changé depuis sa déclaration, régularisation de " + m(o.tvaDeductible()) + " proposée. Traitement à valider par un expert-comptable.", "DEDUCTIBLE", o.id()));
        }
    }

    private void bareme(BigDecimal taux, LocalDate date, String nom, String type, Long id, List<Constat> constats) {
        if (taux.signum() > 0 && parametres.aLaDate("TVA_TAUX", FormulaireTva.cleTaux(taux), date).isEmpty()) {
            constats.add(new Constat("T12", "ALERTE", nom + " : le taux de " + m(taux) + " % n'est pas au barème du " + FR.format(date) + ".", type, id));
        }
    }

    private void collectees(Declaration d, List<OperationCollectee> collectees, List<Constat> constats) {
        for (OperationCollectee o : collectees) {
            String nom = "Encaissement " + (o.reference() == null ? o.sourceId() : o.reference()) + " de " + o.tiersNom();
            if (o.taux().signum() == 0 && "TAXABLE".equals(o.traitement())) {
                if (o.factureNumero() == null) {
                    constats.add(new Constat("T14", "BLOQUANT", nom + " : acompte de " + m(o.ttc()) + " sans facture, indiquez son taux.", "COLLECTEE", o.id()));
                } else {
                    constats.add(new Constat("T13", "BLOQUANT", nom + " : ligne à 0 % de " + o.factureNumero() + " sans traitement fiscal.", "COLLECTEE", o.id()));
                }
            }
            bareme(o.taux(), o.date(), nom, "COLLECTEE", o.id(), constats);
            if ("REGULARISATION".equals(o.sourceType())) {
                constats.add(new Constat("T32", "ALERTE", nom + " : la source a changé depuis sa déclaration, régularisation de " + m(o.tva()) + " proposée. Traitement à valider par un expert-comptable.", "COLLECTEE", o.id()));
            }
        }
    }

    private void recoupements(Long livreId, Declaration d, List<OperationDeductible> deductibles, Map<String, LigneRetenue> lignes, List<Constat> constats) {
        Map<String, BigDecimal> releve = new LinkedHashMap<>();
        BigDecimal sansLigne = BigDecimal.ZERO;
        for (OperationDeductible o : deductibles) {
            if (o.ligneCode() == null) {
                sansLigne = sansLigne.add(o.tvaDeductible());
            } else {
                releve.merge(o.ligneCode(), o.tvaDeductible(), BigDecimal::add);
            }
        }
        for (Map.Entry<String, BigDecimal> e : releve.entrySet()) {
            LigneRetenue l = lignes.get(e.getKey());
            BigDecimal fiche = l == null || l.taxeRetenue() == null ? BigDecimal.ZERO : l.taxeRetenue();
            if (fiche.compareTo(e.getValue()) != 0) {
                constats.add(new Constat("T10", "BLOQUANT", "Ligne " + e.getKey() + " : relevé " + m(e.getValue()) + ", fiche " + m(fiche) + ", écart " + m(e.getValue().subtract(fiche)) + ".", "LIGNE", null));
            }
        }
        if (sansLigne.signum() != 0) {
            constats.add(new Constat("T10", "BLOQUANT", "Des opérations pour " + m(sansLigne) + " de TVA n'ont pas de ligne du formulaire : le relevé ne recoupe pas la fiche.", "LIGNE", null));
        }
        BigDecimal totalB = BigDecimal.ZERO;
        for (LigneRetenue l : lignes.values()) {
            if (formulaire.ligne(l.code()).filter(x -> "B".equals(x.section())).isPresent() && !"120".equals(l.code()) && l.baseRetenue() != null) {
                totalB = totalB.add(l.baseRetenue());
            }
        }
        BigDecimal l060 = lignes.containsKey("060") && lignes.get("060").baseRetenue() != null ? lignes.get("060").baseRetenue() : BigDecimal.ZERO;
        if (totalB.compareTo(l060) != 0) {
            constats.add(new Constat("T11", "BLOQUANT", "La ventilation du chiffre d'affaires imposable est incorrecte : total B " + m(totalB) + ", ligne 060 " + m(l060) + ", écart " + m(l060.subtract(totalB)) + ".", "LIGNE", null));
        }
        // Un règlement qui ne couvre pas exactement ce qu'il impute se voit ici : T21.
        Map<Long, BigDecimal> parReglement = new LinkedHashMap<>();
        for (OperationDeductible o : deductibles) {
            if ("REGLEMENT_FOURNISSEUR".equals(o.sourceType())) {
                parReglement.merge(o.sourceId(), o.ttc(), BigDecimal::add);
            }
        }
        for (var r : repository.reglementsParIds(livreId, parReglement.keySet())) {
            BigDecimal ecart = r.montant().subtract(parReglement.get(r.id()));
            if (ecart.abs().compareTo(UN_CENTIME) > 0) {
                constats.add(new Constat("T21", "ALERTE", "Règlement " + r.numero() + " : " + m(r.montant()) + " payés, " + m(parReglement.get(r.id())) + " imputés sur des factures, écart " + m(ecart) + ".", "REGLEMENT", r.id()));
            }
        }
    }

    /** Art. 106-II : 5 000 DH par jour et par fournisseur, 50 000 DH par mois et par fournisseur. Bloquant, sauf exception motivée. */
    private void especes(List<OperationDeductible> deductibles, List<Constat> constats) {
        Map<String, BigDecimal> parJour = new LinkedHashMap<>();
        Map<String, BigDecimal> parMois = new LinkedHashMap<>();
        Map<String, Long> objet = new HashMap<>();
        for (OperationDeductible o : deductibles) {
            if (o.mpId() == 1) {
                String jour = o.fournisseurNom() + "|" + o.datePaiement();
                String mois = o.fournisseurNom() + "|" + o.datePaiement().withDayOfMonth(1);
                parJour.merge(jour, o.ttc(), BigDecimal::add);
                parMois.merge(mois, o.ttc(), BigDecimal::add);
                objet.putIfAbsent(jour, o.id());
                objet.putIfAbsent(mois, o.id());
            }
        }
        BigDecimal plafondJour = plafond("JOUR", "5000");
        BigDecimal plafondMois = plafond("MOIS", "50000");
        parJour.forEach((cle, total) -> {
            if (total.compareTo(plafondJour) > 0) {
                constats.add(new Constat("T18", "BLOQUANT", cle.split("\\|")[0] + " : " + m(total) + " payés en espèces le " + FR.format(LocalDate.parse(cle.split("\\|")[1])) + ", au-delà de " + m(plafondJour) + " par jour (art. 106-II).", "DEDUCTIBLE", objet.get(cle)));
            }
        });
        parMois.forEach((cle, total) -> {
            if (total.compareTo(plafondMois) > 0) {
                constats.add(new Constat("T18", "BLOQUANT", cle.split("\\|")[0] + " : " + m(total) + " payés en espèces dans le mois, au-delà de " + m(plafondMois) + " (art. 106-II).", "DEDUCTIBLE", objet.get(cle)));
            }
        });
    }

    private BigDecimal plafond(String cle, String parDefaut) {
        return parametres.aLaDate("TVA_ESPECES_PLAFOND", cle, LocalDate.now()).map(v -> v.valeur()).orElse(new BigDecimal(parDefaut));
    }

    private void credit(Declaration d, Map<String, LigneRetenue> lignes, BigDecimal attendu, boolean precedenteAbsente, boolean creditSaisi, List<Constat> constats) {
        LigneRetenue l170 = lignes.get("170");
        BigDecimal retenu = l170 == null || l170.taxeRetenue() == null ? BigDecimal.ZERO : l170.taxeRetenue();
        if (retenu.compareTo(attendu) != 0) {
            if (creditSaisi) {
                constats.add(new Constat("T25", "ALERTE", "Crédit antérieur modifié : " + m(retenu) + " retenus pour " + m(attendu) + " attendus. Motif : " + l170.motif(), "LIGNE", null));
            } else {
                constats.add(new Constat("T25", "BLOQUANT", "Crédit antérieur de " + m(retenu) + " pour " + m(attendu) + " à la période précédente, sans motif.", "LIGNE", null));
            }
        }
        if (precedenteAbsente) {
            constats.add(new Constat("T26", "ALERTE", "Aucune déclaration validée ne finit le " + FR.format(d.du().minusDays(1)) + " : le crédit vient du paramètre annuel (" + m(attendu) + ").", "DECLARATION", d.id()));
        }
    }

    private void remises(Long livreId, List<OperationCollectee> collectees, String remiseMode, List<Constat> constats) {
        if (!"NET_TVA_FACTUREE".equals(remiseMode)) {
            return;
        }
        List<Long> factures = collectees.stream().map(OperationCollectee::factureId).filter(Objects::nonNull).distinct().toList();
        repository.factures(livreId, factures).stream().filter(f -> f.remise().signum() > 0).forEach(f -> constats.add(new Constat("T27", "ALERTE",
                "Facture " + f.numero() + " : remise globale de " + m(f.remise()) + " avec la TVA de la facture inchangée (art. 119-II).", "FACTURE", f.id())));
    }

    /** Recoupement, jamais une source de calcul : ce qui est déclaré ne doit pas dépasser ce que le journal porte. */
    private void journal(Long livreId, Declaration d, List<Constat> constats) {
        BigDecimal collecte = repository.cumulCollecte(livreId, d.au());
        BigDecimal auJournal = repository.mouvementsHorsLiquidation(livreId, "4455", d.au(), true);
        if (collecte.subtract(auJournal).compareTo(DEUX_CENTIMES) > 0) {
            constats.add(new Constat("T29", "ALERTE", "4455 : " + m(collecte) + " de TVA déclarés depuis le début, " + m(auJournal) + " au journal, écart " + m(collecte.subtract(auJournal)) + ". Des pièces restent sans doute à transmettre.", "COMPTE", null));
        }
        for (String compte : List.of("34552", "34551")) {
            BigDecimal deduit = repository.cumulDeduit(livreId, compte, d.au());
            BigDecimal porte = repository.mouvementsHorsLiquidation(livreId, compte, d.au(), false);
            if (deduit.subtract(porte).compareTo(DEUX_CENTIMES) > 0) {
                constats.add(new Constat("T29", "ALERTE", compte + " : " + m(deduit) + " de TVA déduits depuis le début, " + m(porte) + " au journal, écart " + m(deduit.subtract(porte)) + ". Des pièces restent sans doute à transmettre.", "COMPTE", null));
            }
        }
    }

    /** Sans relevé sur la période, ces contrôles n'ont pas tourné : ils le disent. Ils ne se lisent jamais comme réussis. */
    private void releve(Long livreId, Declaration d, List<OperationCollectee> collectees, List<OperationDeductible> deductibles, List<Constat> constats) {
        List<LigneClasseeDTO> lignes = rapprochement.lignesDePeriode(livreId, d.du(), d.au());
        if (lignes.isEmpty()) {
            constats.add(new Constat("T22", "NON_EFFECTUE", "Débits classés contre règlements du relevé : aucun relevé sur la période.", "DECLARATION", d.id()));
            constats.add(new Constat("T23", "NON_EFFECTUE", "Débits du relevé classés : aucun relevé sur la période.", "DECLARATION", d.id()));
            constats.add(new Constat("T24", "NON_EFFECTUE", "Remises contre crédits du relevé : aucun relevé sur la période.", "DECLARATION", d.id()));
            return;
        }
        BigDecimal debitsPointes = lignes.stream().filter(l -> l.debit().signum() > 0).map(LigneClasseeDTO::pointe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reglements = deductibles.stream().filter(o -> o.mpId() != 1 && o.datePaiement().compareTo(d.du()) >= 0 && "REGLEMENT_FOURNISSEUR".equals(o.sourceType())).map(OperationDeductible::ttc).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (reglements.subtract(debitsPointes).abs().compareTo(UN_CENTIME) > 0) {
            constats.add(new Constat("T22", "ALERTE", "Règlements hors espèces " + m(reglements) + ", débits du relevé pointés " + m(debitsPointes) + ", écart " + m(reglements.subtract(debitsPointes)) + ".", "DECLARATION", d.id()));
        }
        List<LigneClasseeDTO> nonClasses = lignes.stream().filter(l -> l.debit().signum() > 0 && l.classement() == null && l.pointe().compareTo(l.debit()) < 0).toList();
        if (!nonClasses.isEmpty()) {
            BigDecimal montant = nonClasses.stream().map(l -> l.debit().subtract(l.pointe())).reduce(BigDecimal.ZERO, BigDecimal::add);
            constats.add(new Constat("T23", "ALERTE", nonClasses.size() + " débit(s) du relevé ni pointés ni classés, pour " + m(montant) + ".", "DECLARATION", d.id()));
        }
        BigDecimal creditsReleve = lignes.stream().filter(l -> l.credit().signum() > 0).map(LigneClasseeDTO::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remises = collectees.stream().filter(o -> !"ESPECES".equals(o.mode()) && "PAIEMENT".equals(o.sourceType())).map(OperationCollectee::ttc).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (remises.subtract(creditsReleve).abs().compareTo(UN_CENTIME) > 0) {
            constats.add(new Constat("T24", "ALERTE", "Encaissements hors espèces " + m(remises) + ", crédits du relevé " + m(creditsReleve) + ", écart " + m(remises.subtract(creditsReleve)) + ".", "DECLARATION", d.id()));
        }
    }

    private void periodicite(Long livreId, Declaration d, List<Constat> constats) {
        BigDecimal ca = repository.chiffreAffairesAnnee(livreId, d.annee() - 1);
        if (ca.signum() == 0) {
            constats.add(new Constat("T30", "NON_EFFECTUE", "Périodicité : aucun chiffre d'affaires connu pour " + (d.annee() - 1) + ".", "DECLARATION", d.id()));
            return;
        }
        BigDecimal seuil = parametres.aLaDate("TVA_SEUIL_MENSUEL", "", d.du()).map(v -> v.valeur()).orElse(new BigDecimal("1000000"));
        if ("TRIMESTRIELLE".equals(d.periodicite()) && ca.compareTo(seuil) >= 0) {
            constats.add(new Constat("T30", "ALERTE", "Chiffre d'affaires " + (d.annee() - 1) + " de " + m(ca) + " : la déclaration devrait être mensuelle (art. 108).", "DECLARATION", d.id()));
        }
    }
}

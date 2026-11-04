package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpTrimestreDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheanceRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.EcheancesDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.RasMoisResumeDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.AcompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.DelaisComptaRepository;
import com.gestion.repository.compta.EcheanceComptaRepository;
import com.gestion.repository.compta.EcheanceComptaRepository.Echeance;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.FiscalComptaRepository;
import com.gestion.repository.compta.ParametreFiscalRepository;
import com.gestion.repository.compta.TvaRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Le calendrier fiscal du livre (annexe B § 7). La TVA suit la périodicité réelle, et un report au premier jour ouvrable n'existe que si la liste des jours fériés est remplie.
 * La paie n'est pas suivie : ses obligations n'apparaissent pas.
 */
@Service
public class EcheanceComptaService {

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            Map.entry("TVA", "TVA : déclaration et paiement"),
            Map.entry("TVA_OPTION_MENSUELLE", "TVA : option pour la déclaration mensuelle"),
            Map.entry("TVA_OPTION_DEBITS", "TVA : option pour les débits"),
            Map.entry("TVA_PRORATA", "TVA : déclaration du prorata"),
            Map.entry("RAS_TVA", "Retenue à la source sur la TVA, avec bordereau"),
            Map.entry("IS_ACOMPTE", "IS : acompte provisionnel"),
            Map.entry("CM_MINIMUM", "Minimum de cotisation minimale"),
            Map.entry("IS_RESULTAT", "IS : déclaration de résultat, pièces annexes et solde"),
            Map.entry("ETATS_SYNTHESE", "États de synthèse"),
            Map.entry("CSS", "Contribution sociale de solidarité"),
            Map.entry("REMUNERATIONS_TIERS", "Déclaration des rémunérations à des tiers"),
            Map.entry("DELAIS_PAIEMENT", "Délais de paiement : déclaration"));

    private final EcheanceComptaRepository repository;
    private final DelaisComptaRepository delais;
    private final DelaisComptaService delaisService;
    private final RasComptaService ras;
    private final TvaRepository tva;
    private final ExerciceRepository exercices;
    private final FiscalComptaRepository fiscal;
    private final ParametreFiscalRepository parametres;
    private final LivreCourantService livreCourant;
    private final AuditService audit;
    private final Clock horloge;

    public EcheanceComptaService(EcheanceComptaRepository repository, DelaisComptaRepository delais, DelaisComptaService delaisService, RasComptaService ras, TvaRepository tva, ExerciceRepository exercices, FiscalComptaRepository fiscal, ParametreFiscalRepository parametres,
                                 LivreCourantService livreCourant, AuditService audit, java.util.Optional<Clock> horloge) {
        this.repository = repository;
        this.delais = delais;
        this.delaisService = delaisService;
        this.ras = ras;
        this.tva = tva;
        this.exercices = exercices;
        this.fiscal = fiscal;
        this.parametres = parametres;
        this.livreCourant = livreCourant;
        this.audit = audit;
        this.horloge = horloge.orElseGet(Clock::systemDefaultZone);
    }

    private Long livre() {
        return livreCourant.exiger().livreId();
    }

    // --- Lecture

    @Transactional(readOnly = true)
    public EcheancesDTO lister(int annee) {
        Long livreId = livre();
        return dto(livreId, annee, avertissements(livreId, annee));
    }

    @Transactional(readOnly = true)
    public List<EcheanceDTO> prochaines(int nombre) {
        return repository.prochaines(livre(), Math.max(1, Math.min(nombre, 50))).stream().map(this::ligne).toList();
    }

    private EcheancesDTO dto(Long livreId, int annee, List<String> avertissements) {
        return new EcheancesDTO(annee, repository.lister(livreId, annee).stream().map(this::ligne).toList(), avertissements, parametres.joursFeries().size());
    }

    private EcheanceDTO ligne(Echeance e) {
        return new EcheanceDTO(e.id(), e.type(), LIBELLES.getOrDefault(e.type(), e.type()), e.periode(), e.dateLimite(), e.statut(), e.article(), e.faitLe(), e.reference(), "A_FAIRE".equals(e.statut()) && e.dateLimite().isBefore(LocalDate.now(horloge)));
    }

    private List<String> avertissements(Long livreId, int annee) {
        List<String> liste = new ArrayList<>();
        if (tva.parametres(livreId, annee).isEmpty()) {
            liste.add("Paramètres TVA de " + annee + " absents : les échéances de TVA ne sont pas calculées.");
        }
        if (parametres.joursFeries().size() == 0) {
            liste.add("Liste des jours fériés vide : aucun report au premier jour ouvrable n'est calculé.");
        }
        List<ExerciceDTO> deLAnnee = exercices.lister(livreId).stream().filter(e -> e.du().getYear() == annee).toList();
        if (deLAnnee.isEmpty()) {
            liste.add("Aucun exercice ouvert en " + annee + " : les échéances d'IS et de clôture ne sont pas calculées.");
        } else if (deLAnnee.stream().allMatch(e -> e.precedentId() == null)) {
            liste.add("Premier exercice du livre : pas d'exercice de référence, donc pas d'échéance d'acompte ni de minimum de cotisation.");
        }
        return liste;
    }

    // --- Calcul

    @Transactional
    public EcheancesDTO generer(int annee) {
        Long livreId = livre();
        Set<LocalDate> feries = parametres.joursFeries();
        Set<String> gardees = new HashSet<>();
        Optional<TvaParametresDTO> parametres = tva.parametres(livreId, annee);
        parametres.filter(TvaParametresDTO::assujetti).ifPresent(p -> {
            boolean trimestrielle = "TRIMESTRIELLE".equals(p.periodicite());
            int debut = p.premierePeriode() == null ? 1 : p.premierePeriode();
            for (int n = debut; n <= TvaCalcul.nombreDePeriodes(p.periodicite()); n++) {
                LocalDate limite = TvaCalcul.dateLimite(TvaCalcul.finDePeriode(annee, p.periodicite(), n));
                poser(livreId, gardees, feries, "TVA", trimestrielle ? annee + "-T" + n : String.format("%d-%02d", annee, n), limite, trimestrielle ? "CGI art. 111, 155" : "CGI art. 110, 155");
            }
            poser(livreId, gardees, feries, "TVA_OPTION_MENSUELLE", String.valueOf(annee), LocalDate.of(annee, 1, 30), "CGI art. 108-II");
            poser(livreId, gardees, feries, "TVA_OPTION_DEBITS", String.valueOf(annee), LocalDate.of(annee, 12, 31), "CGI art. 95");
            if (p.prorata().compareTo(BigDecimal.valueOf(100)) < 0) {
                poser(livreId, gardees, feries, "TVA_PRORATA", String.valueOf(annee), YearMonth.of(annee, 2).atEndOfMonth(), "CGI art. 113");
            }
        });
        for (int m = 1; m <= 12; m++) {
            poser(livreId, gardees, feries, "RAS_TVA", String.format("%d-%02d", annee, m), TvaCalcul.dateLimite(YearMonth.of(annee, m).atEndOfMonth()), "CGI art. 117");
        }
        delaisDePaiement(livreId, annee, gardees, feries);
        for (ExerciceDTO e : exercices.lister(livreId)) {
            if (e.du().getYear() != annee) {
                continue;
            }
            String code = String.valueOf(annee);
            if (e.precedentId() != null) {
                for (int n = 1; n <= 4; n++) {
                    poser(livreId, gardees, feries, "IS_ACOMPTE", code + "-AC" + n, IsCalcul.echeanceAcompte(e.du(), n), "CGI art. 170-I");
                }
                poser(livreId, gardees, feries, "CM_MINIMUM", code, IsCalcul.echeanceAcompte(e.du(), 1), "CGI art. 170-I");
            }
            LocalDate troisMois = YearMonth.from(e.au().plusMonths(3)).atEndOfMonth();
            poser(livreId, gardees, feries, "IS_RESULTAT", code, troisMois, "CGI art. 20-I, 170-IV");
            poser(livreId, gardees, feries, "ETATS_SYNTHESE", code, troisMois, "Loi 9-88 art. 18");
            poser(livreId, gardees, feries, "CSS", code, troisMois, "CGI art. 270, 271");
            poser(livreId, gardees, feries, "REMUNERATIONS_TIERS", code, troisMois, "CGI art. 151-I");
        }
        repository.retirerAutres(livreId, annee, gardees);
        rapprocher(livreId, annee);
        return dto(livreId, annee, avertissements(livreId, annee));
    }

    private void delaisDePaiement(Long livreId, int annee, Set<String> gardees, Set<LocalDate> feries) {
        Optional<BigDecimal> ca = delaisService.chiffreAffaires(livreId, LocalDate.of(annee, 1, 1));
        if (ca.filter(v -> v.compareTo(BigDecimal.valueOf(2_000_000)) <= 0).isPresent()) {
            return;
        }
        if (annee >= 2026) {
            for (int t = 1; t <= 4; t++) {
                poser(livreId, gardees, feries, "DELAIS_PAIEMENT", annee + "-T" + t, TvaCalcul.dateLimite(TvaCalcul.finDePeriode(annee, "TRIMESTRIELLE", t)), "C. com. art. 78-4");
            }
        } else if (annee >= 2024) {
            poser(livreId, gardees, feries, "DELAIS_PAIEMENT", String.valueOf(annee), LocalDate.of(annee + 1, 3, 31), "Loi 69-21 art. 2");
        }
    }

    private void poser(Long livreId, Set<String> gardees, Set<LocalDate> feries, String type, String periode, LocalDate limite, String article) {
        repository.ecrire(livreId, type, periode, DelaisCalcul.reporter(limite, feries), article);
        gardees.add(type + "|" + periode);
    }

    /** Ce que le module sait déjà : une TVA déposée, un acompte payé, une liasse déposée se marquent seuls. */
    private void rapprocher(Long livreId, int annee) {
        for (TvaRepository.Declaration d : tva.lister(livreId, annee)) {
            if ("DEPOSEE".equals(d.statut()) || "PAYEE".equals(d.statut())) {
                String periode = "TRIMESTRIELLE".equals(d.periodicite()) ? annee + "-T" + d.numeroPeriode() : String.format("%d-%02d", annee, d.numeroPeriode());
                repository.marquerSiOuverte(livreId, "TVA", periode, "FAIT", d.deposeLe() != null ? d.deposeLe() : d.payeLe(), d.numeroRecu());
            }
        }
        for (DpTrimestreDTO t : delaisService.trimestres(annee)) {
            if ("DEPOSEE".equals(t.statut())) {
                repository.marquerSiOuverte(livreId, "DELAIS_PAIEMENT", annee + "-T" + t.trimestre(), "FAIT", delais.declaration(livreId, annee, t.trimestre()).map(DelaisComptaRepository.Declaration::deposeLe).orElse(null), null);
            }
        }
        LocalDate aujourdhui = LocalDate.now(horloge);
        for (Echeance o : repository.lister(livreId, annee)) {
            if (o.type().startsWith("TVA_OPTION_") && o.dateLimite().isBefore(aujourdhui)) {
                repository.marquerSiOuverte(livreId, o.type(), o.periode(), "SANS_OBJET", null, null);
            }
        }
        for (RasMoisResumeDTO m : ras.annee(annee)) {
            if (m.totalRetenu().signum() == 0 && m.aQualifier() == 0 && m.aRetenir() == 0 && YearMonth.of(annee, m.mois()).atEndOfMonth().isBefore(aujourdhui)) {
                repository.marquerSiOuverte(livreId, "RAS_TVA", String.format("%d-%02d", annee, m.mois()), "SANS_OBJET", null, null);
            }
        }
        for (ExerciceDTO e : exercices.lister(livreId)) {
            if (e.du().getYear() != annee) {
                continue;
            }
            String code = String.valueOf(annee);
            for (AcompteDTO a : fiscal.acomptes(livreId, e.id())) {
                if (a.ecritureId() != null) {
                    repository.marquerSiOuverte(livreId, "IS_ACOMPTE", code + "-AC" + a.numero(), "FAIT", a.datePaiement(), a.reference());
                }
            }
            fiscal.liasse(livreId, e.id()).filter(l -> l.deposeLe() != null).ifPresent(l -> repository.marquerSiOuverte(livreId, "IS_RESULTAT", code, "FAIT", l.deposeLe(), l.numeroRecu()));
        }
    }

    // --- Marque de l'utilisateur

    @Transactional
    public EcheanceDTO marquer(Long id, EcheanceRequest r) {
        Long livreId = livre();
        Echeance e = repository.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Échéance", "id", id));
        if (!Set.of("A_FAIRE", "FAIT", "SANS_OBJET").contains(r.statut())) {
            throw new CommonException("Statut inconnu : " + r.statut() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECHEANCE_STATUT");
        }
        if ("FAIT".equals(r.statut()) && r.faitLe() == null) {
            throw new CommonException("Indiquez la date de l'obligation faite.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_ECHEANCE_DATE");
        }
        repository.marquer(livreId, id, r.statut(), "FAIT".equals(r.statut()) ? r.faitLe() : null, "FAIT".equals(r.statut()) ? r.reference() : null);
        audit.logAction(ActionAudit.MODIFICATION, "compta_echeances", id, e.type() + " " + e.periode() + " : " + r.statut());
        return ligne(repository.lire(livreId, id).orElseThrow());
    }
}

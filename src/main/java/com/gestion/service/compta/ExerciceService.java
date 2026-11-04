package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service("comptaExerciceService")
public class ExerciceService {

    private static final int PERIODES_OUVERTES_MAX = 2;

    private final ExerciceRepository exercices;
    private final PeriodeRepository periodes;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public ExerciceService(ExerciceRepository exercices, PeriodeRepository periodes, LivreCourantService livreCourant, AuditService audit) {
        this.exercices = exercices;
        this.periodes = periodes;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ExerciceDTO> lister() {
        return exercices.lister(livreCourant.exiger().livreId());
    }

    @Transactional(readOnly = true)
    public List<PeriodeDTO> periodes(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        exiger(livreId, exerciceId);
        return periodes.lister(livreId, exerciceId);
    }

    @Transactional
    public ExerciceDTO creerSuivant(Long exerciceId, ExerciceDatesRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO precedent = exiger(livreId, exerciceId);
        if (exercices.aUnSuivant(livreId, exerciceId)) {
            throw new CommonException("L'exercice " + precedent.code() + " a déjà un exercice suivant",
                    HttpStatus.CONFLICT, "COMPTA_EXERCICE_SUIVANT_EXISTANT");
        }
        if (!demande.du().equals(precedent.au().plusDays(1))) {
            throw new CommonException("L'exercice suivant commence le " + precedent.au().plusDays(1) + ", pas le " + demande.du(),
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_EXERCICE_NON_CONTIGU");
        }
        Long id = creer(livreId, demande, precedent.id(), demande.du(), livreCourant.auteur());
        audit.logAction(ActionAudit.CREATION, "compta_exercices", id, "Exercice " + demande.code());
        return exiger(livreId, id);
    }

    /**
     * Crée l'exercice et ses périodes, en ordre chronologique : la base refuse une ouverture hors ordre. Les périodes antérieures
     * au démarrage naissent clôturées. Une période courante s'ouvre tant que le livre en a moins de deux.
     */
    public Long creer(Long livreId, ExerciceDatesRequest dates, Long precedentId, LocalDate demarrage, String auteur) {
        controlerDates(dates);
        Long exerciceId = exercices.creer(livreId, dates.code().trim(), dates.du(), dates.au(), precedentId);
        periodes.inserer(livreId, exerciceId, 0, "OUVERTURE", dates.du(), dates.du(), "OUVERTE", auteur);
        long ouvertes = periodes.nbCourantesOuvertes(livreId);
        LocalDate debut = dates.du();
        for (int numero = 1; !debut.isAfter(dates.au()); numero++) {
            LocalDate fin = debut.withDayOfMonth(debut.lengthOfMonth());
            if (fin.isAfter(dates.au())) {
                fin = dates.au();
            }
            String statut;
            if (fin.isBefore(demarrage)) {
                statut = "CLOTUREE";
            } else if (ouvertes < PERIODES_OUVERTES_MAX) {
                statut = "OUVERTE";
                ouvertes++;
            } else {
                statut = "A_VENIR";
            }
            periodes.inserer(livreId, exerciceId, numero, "COURANTE", debut, fin, statut, auteur);
            debut = fin.plusDays(1);
        }
        periodes.inserer(livreId, exerciceId, 99, "CLOTURE", dates.au(), dates.au(), "A_VENIR", auteur);
        return exerciceId;
    }

    private static void controlerDates(ExerciceDatesRequest dates) {
        if (dates.au().isBefore(dates.du())) {
            throw new CommonException("La fin de l'exercice précède son début", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_EXERCICE_DATES");
        }
        if (!dates.au().isBefore(dates.du().plusMonths(12))) {
            throw new CommonException("Un exercice ne dépasse pas 12 mois (loi 9-88, art. 7)", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_EXERCICE_DUREE");
        }
    }

    private ExerciceDTO exiger(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }
}

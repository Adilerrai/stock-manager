package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.repository.compta.PeriodeRepository.LignePeriode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * La date d'écriture d'une pièce. Elle garde toujours sa date de pièce ; la date d'écriture suit LOG-06 :
 * le maximum de la date de la pièce, du premier jour de la première période ouverte, et du premier jour du mois de la dernière écriture du journal.
 */
@Component
public class DatesComptables {

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PeriodeRepository periodes;
    private final JournalRepository journal;

    public DatesComptables(PeriodeRepository periodes, JournalRepository journal) {
        this.periodes = periodes;
        this.journal = journal;
    }

    public LocalDate resoudre(Long livreId, Long journalId, LocalDate datePiece) {
        Optional<LignePeriode> periode = periodes.courantePourDate(livreId, datePiece);
        LocalDate date = datePiece;
        Long exerciceId;
        if (periode.isPresent() && "OUVERTE".equals(periode.get().statut())) {
            exerciceId = periode.get().exerciceId();
        } else if (periode.isPresent() && "A_VENIR".equals(periode.get().statut())) {
            throw new CommonException("La période du " + FR.format(periode.get().du()) + " au " + FR.format(periode.get().au()) + " n'est pas encore ouverte : la pièce reste dans la file.",
                    HttpStatus.CONFLICT, "COMPTA_PERIODE_A_VENIR");
        } else {
            LignePeriode ouverte = periodes.premiereOuverte(livreId).orElseThrow(() -> new CommonException("Aucune période ouverte : créez l'exercice suivant.",
                    HttpStatus.CONFLICT, "COMPTA_PERIODE_INCONNUE"));
            if (periode.isEmpty() && datePiece.isAfter(ouverte.au())) {
                throw new CommonException("Aucun exercice ne couvre le " + FR.format(datePiece) + " : créez l'exercice suivant.", HttpStatus.CONFLICT, "COMPTA_PERIODE_INCONNUE");
            }
            date = ouverte.du();
            exerciceId = ouverte.exerciceId();
        }
        Optional<LocalDate> derniere = journal.derniereDate(livreId, exerciceId, journalId);
        if (derniere.isPresent() && date.isBefore(derniere.get().withDayOfMonth(1))) {
            date = derniere.get().withDayOfMonth(1);
        }
        return date;
    }
}

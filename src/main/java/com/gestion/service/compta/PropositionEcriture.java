package com.gestion.service.compta;

import com.gestion.service.compta.PieceSource.TiersSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Ce qu'une règle propose. L'écran montre ces lignes, et ce sont elles qui sont postées : une proposition affichée est une proposition postée. */
public record PropositionEcriture(String evenement, String journal, LocalDate datePiece, String refPiece, String libelle, List<LigneProposee> lignes,
                                  List<AnomaliePiece> anomalies, List<String> complementsRequis) {

    public record LigneProposee(String compte, TiersSource tiers, BigDecimal debit, BigDecimal credit, BigDecimal taux, BigDecimal base,
                                String natureMouvement, boolean horsFlux) {
    }

    /** @param bloquante faux : une alerte, qui se montre et n'empêche rien */
    public record AnomaliePiece(String code, String message, boolean bloquante) {
    }

    public boolean bloquee() {
        return anomalies.stream().anyMatch(AnomaliePiece::bloquante);
    }
}

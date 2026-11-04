package com.gestion.compta;

import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.ConsultationDtos.CentralisateurLigneDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ConsultationService;
import com.gestion.service.compta.EcritureService;
import com.gestion.service.compta.LivreService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConsultationJournauxIT extends AbstractComptaIT {

    private static final LocalDate DU = LocalDate.parse("2026-01-01");
    private static final LocalDate AU = LocalDate.parse("2026-12-31");

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private ConsultationService consultation;
    @Autowired private EcritureService ecritures;
    @Autowired private JournauxRepository journaux;

    private LivreDTO livre;
    private int numero = 1;

    @BeforeEach
    void preparer() {
        Long pdv = SocieteDeTest.creer().pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        abonnements.modifier(pdv, true);
        livre = livres.activer(new ActivationLivreRequest(DU, new ExerciceDatesRequest("2026", DU, AU),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca", "Négoce", null, null, "GENERAL")));
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    private static BigDecimal m(String s) {
        return new BigDecimal(s);
    }

    private EcritureDTO passer(String date, String journal, String libelle, String debit, String credit, String montant) {
        JournalDTO j = journaux.lister(livre.livreId()).stream().filter(x -> x.code().equals(journal)).findFirst().orElseThrow();
        LocalDate jour = LocalDate.parse(date);
        BrouillonDTO b = ecritures.creerBrouillon(new BrouillonRequest(j.id(), jour, jour, "P-" + (numero++), libelle, "COURANTE", null, false, List.of(
                new BrouillonLigneRequest(debit, null, m(montant), BigDecimal.ZERO, null, null, null, null, null, null, null, null),
                new BrouillonLigneRequest(credit, null, BigDecimal.ZERO, m(montant), null, null, null, null, null, null, null, null))));
        return ecritures.validerBrouillon(b.id());
    }

    @Test
    void leCentralisateurTotaliseChaqueJournalParPeriode() {
        passer("2026-01-05", "BQ", "Apport", "5141", "1111", "1000.00");
        passer("2026-01-20", "BQ", "Apport", "5141", "1111", "250.50");
        passer("2026-01-25", "OD", "Reclassement", "6111", "1111", "40.00");
        passer("2026-02-02", "BQ", "Apport", "5141", "1111", "9.50");

        List<CentralisateurLigneDTO> lignes = consultation.centralisateur(livre.exerciceCourantId());
        assertEquals(3, lignes.size());
        CentralisateurLigneDTO banqueJanvier = lignes.stream().filter(l -> "BQ".equals(l.journalCode()) && l.du().getMonthValue() == 1).findFirst().orElseThrow();
        assertEquals(2, banqueJanvier.ecritures());
        assertEquals(m("1250.50"), banqueJanvier.debit());
        assertEquals(m("1250.50"), banqueJanvier.credit());
        assertEquals(m("1300.00"), lignes.stream().map(CentralisateurLigneDTO::debit).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(1, lignes.get(0).du().getMonthValue());
        assertEquals(2, lignes.get(2).du().getMonthValue());
    }

    @Test
    void laRechercheCroiseSesCriteres() {
        EcritureDTO apport = passer("2026-01-05", "BQ", "Apport du gérant", "5141", "1111", "1000.00");
        EcritureDTO reclassement = passer("2026-01-25", "OD", "Reclassement", "6111", "1111", "40.00");
        EcritureDTO fevrier = passer("2026-02-02", "BQ", "Apport du gérant", "5141", "1111", "9.50");

        assertEquals(List.of(fevrier.id(), apport.id()), ids(ecritures.rechercher(null, null, null, "apport", null, null, null)));
        assertEquals(List.of(reclassement.id()), ids(ecritures.rechercher(null, null, null, null, "61", null, null)));
        assertEquals(List.of(reclassement.id()), ids(ecritures.rechercher(null, null, m("40.00"), null, null, null, null)));
        assertEquals(List.of(apport.id()), ids(ecritures.rechercher(null, null, null, "apport", null, DU, LocalDate.parse("2026-01-31"))));
        assertEquals(List.of(apport.id()), ids(ecritures.rechercher(apport.refPiece(), apport.datePiece(), null, null, null, null, null)));
        assertTrue(ecritures.rechercher(null, null, m("40.00"), "apport", null, null, null).isEmpty());
    }

    private static List<Long> ids(List<EcritureDTO> liste) {
        return liste.stream().map(EcritureDTO::id).toList();
    }
}

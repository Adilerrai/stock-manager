package com.gestion.repository;

import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.JournalActiviteDTO.Evenement;
import com.gestion.persistent.dto.JournalActiviteDTO.Resume;
import com.gestion.persistent.enums.TypeEvenementJournal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gestion.persistent.enums.TypeEvenementJournal.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journal d'activité sur le vrai schéma Liquibase : chaque branche du UNION lit les bonnes colonnes,
 * l'ordre chronologique, la recherche et l'isolation par point de vente.
 */
class JournalActiviteRepositoryIT extends AbstractComptaIT {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 5);
    private static final LocalDateTime DEBUT = JOUR.atStartOfDay();
    private static final LocalDateTime FIN = JOUR.plusDays(1).atStartOfDay();

    private NamedParameterJdbcTemplate jdbc;
    private JournalActiviteRepository repository;
    private SocieteDeTest societe;

    @BeforeEach
    void preparer() {
        jdbc = new NamedParameterJdbcTemplate(base());
        repository = new JournalActiviteRepository(jdbc);
        societe = SocieteDeTest.creer();
        long pdv = societe.pointDeVenteId();

        long user = inserer("INSERT INTO users (email, password, nom_complet, username) "
                + "VALUES (:email, 'x', 'Assistante Test', :email) RETURNING id",
                Map.of("email", societe.code() + "@test.ma"));
        long client = inserer("INSERT INTO clients (nom, point_de_vente_id) VALUES ('GRAND CERAME', :pdv) RETURNING id",
                Map.of("pdv", pdv));
        long fournisseur = inserer("INSERT INTO fournisseurs (raison_social, point_de_vente_id) "
                + "VALUES ('MAROC TRANSMISSION', :pdv) RETURNING id", Map.of("pdv", pdv));

        inserer("""
                INSERT INTO devis (numero_devis, date_devis, client_id, cree_par_user_id, montant_ttc, montant_final, statut,
                                   point_de_vente_id, date_creation)
                VALUES ('DV-1', :jour, :client, :user, 1200, 1200, 'ENVOYE', :pdv, :t) RETURNING id""",
                Map.of("jour", JOUR, "client", client, "user", user, "pdv", pdv, "t", heure(9)));
        // Hors période : ne doit jamais remonter
        inserer("""
                INSERT INTO devis (numero_devis, date_devis, client_id, montant_ttc, montant_final, statut, point_de_vente_id, date_creation)
                VALUES ('DV-0', :jour, :client, 50, 50, 'BROUILLON', :pdv, :t) RETURNING id""",
                Map.of("jour", JOUR.minusDays(1), "client", client, "pdv", pdv, "t", Timestamp.valueOf(DEBUT.minusHours(1))));

        long commandeClient = inserer("""
                INSERT INTO commandes_client (numero_commande, client_id, statut, date_commande, montant_ttc, point_de_vente_id)
                VALUES ('BC-1', :client, 'CONFIRMEE', :t, 1200, :pdv) RETURNING id""",
                Map.of("client", client, "t", heure(10), "pdv", pdv));
        inserer("""
                INSERT INTO bons_livraison_client (numero_bl, date_bl, client_id, commande_client_id, statut, montant_ttc,
                                                   montant_total, livreur_user_id, date_expedition, date_livraison_effective,
                                                   point_de_vente_id)
                VALUES ('BL-1', :t1, :client, :cc, 'LIVREE', 1200, 1200, :user, :t2, :t3, :pdv) RETURNING id""",
                Map.of("t1", heure(11), "client", client, "cc", commandeClient, "user", user,
                        "t2", heure(12), "t3", heure(15), "pdv", pdv));
        long facture = inserer("""
                INSERT INTO factures (numero_facture, date_facture, client_id, emise_par_user_id, montant_ttc, montant_final,
                                      statut, point_de_vente_id, date_creation)
                VALUES ('F-1', :jour, :client, :user, 1200, 1200, 'VALIDEE', :pdv, :t) RETURNING id""",
                Map.of("jour", JOUR, "client", client, "user", user, "pdv", pdv, "t", heure(16)));
        inserer("""
                INSERT INTO paiements (numero_paiement, date_paiement, facture_id, montant, mode_paiement,
                                       encaisse_par_user_id, point_de_vente_id)
                VALUES (:num, :t, :facture, 1200, 'ESPECES', :user, :pdv) RETURNING id""",
                Map.of("num", numeroPaiement(), "t", heure(17), "facture", facture, "user", user, "pdv", pdv));

        long commande = inserer("""
                INSERT INTO commandes (numero_commande, fournisseur_id, statut, date_commande, montant_total, point_de_vente_id)
                VALUES ('P001', :frn, 'PASSEE', :t, 500, :pdv) RETURNING id""",
                Map.of("frn", fournisseur, "t", heure(8), "pdv", pdv));
        inserer("""
                INSERT INTO reglements_fournisseur (numero_reglement, date_reglement, commande_id, montant, mode_paiement,
                                                    point_de_vente_id)
                VALUES (:num, :t, :cmd, 500, 'VIREMENT', :pdv) RETURNING id""",
                Map.of("num", "RF-" + societe.code(), "t", heure(18), "cmd", commande, "pdv", pdv));
    }

    @Test
    void resumeCompteChaqueEvenementDeLaJournee() {
        Map<String, Resume> resume = repository.resume(societe.pointDeVenteId(), DEBUT, FIN, null).stream()
                .collect(Collectors.toMap(Resume::type, r -> r));

        assertThat(resume.keySet()).containsExactlyInAnyOrder(
                "DEVIS_CREE", "BC_CLIENT_CREE", "BL_CREE", "BL_EXPEDIE", "BL_LIVRE", "FACTURE_CREEE",
                "ENCAISSEMENT", "BC_FOURNISSEUR_CREE", "REGLEMENT_FOURNISSEUR");
        assertThat(resume.values()).allSatisfy(r -> assertThat(r.nombre()).isEqualTo(1));
        assertThat(resume.get("ENCAISSEMENT").montant()).isEqualByComparingTo("1200");
        assertThat(resume.get("REGLEMENT_FOURNISSEUR").montant()).isEqualByComparingTo("500");
    }

    @Test
    void evenementsDuPlusRecentAuPlusAncienAvecTiersEtReferences() {
        List<Evenement> journal = repository.evenements(societe.pointDeVenteId(), DEBUT, FIN, null,
                EnumSet.allOf(TypeEvenementJournal.class), 0, 50);

        assertThat(journal).extracting(Evenement::type).containsExactly(
                "REGLEMENT_FOURNISSEUR", "ENCAISSEMENT", "FACTURE_CREEE", "BL_LIVRE", "BL_EXPEDIE",
                "BL_CREE", "BC_CLIENT_CREE", "DEVIS_CREE", "BC_FOURNISSEUR_CREE");

        Evenement encaissement = journal.get(1);
        assertThat(encaissement.numero()).isEqualTo(numeroPaiement());
        assertThat(encaissement.tiers()).isEqualTo("GRAND CERAME"); // client retrouvé via la facture
        assertThat(encaissement.reference()).isEqualTo("F-1");
        assertThat(encaissement.detail()).isEqualTo("ESPECES");
        assertThat(encaissement.utilisateur()).isEqualTo("Assistante Test");
        assertThat(encaissement.statut()).isEqualTo("VALIDE");
        assertThat(encaissement.dateEvenement()).isEqualTo(JOUR.atTime(17, 0));

        Evenement blLivre = journal.get(3);
        assertThat(blLivre.numero()).isEqualTo("BL-1");
        assertThat(blLivre.reference()).isEqualTo("BC-1");
        assertThat(blLivre.montant()).isEqualByComparingTo(BigDecimal.valueOf(1200));

        Evenement reglement = journal.get(0);
        assertThat(reglement.tiers()).isEqualTo("MAROC TRANSMISSION"); // fournisseur retrouvé via la commande
        assertThat(reglement.reference()).isEqualTo("P001");
    }

    @Test
    void filtreParTypesEtPagination() {
        assertThat(repository.evenements(societe.pointDeVenteId(), DEBUT, FIN, null,
                EnumSet.of(ENCAISSEMENT, REGLEMENT_FOURNISSEUR), 0, 50))
                .extracting(Evenement::type).containsExactly("REGLEMENT_FOURNISSEUR", "ENCAISSEMENT");

        // Sans DEVIS_CREE en tête du UNION, les colonnes restent bien nommées
        assertThat(repository.evenements(societe.pointDeVenteId(), DEBUT, FIN, null,
                EnumSet.of(BL_LIVRE), 0, 50)).singleElement()
                .satisfies(e -> assertThat(e.numero()).isEqualTo("BL-1"));

        assertThat(repository.evenements(societe.pointDeVenteId(), DEBUT, FIN, null,
                EnumSet.allOf(TypeEvenementJournal.class), 2, 3))
                .extracting(Evenement::type).containsExactly("FACTURE_CREEE", "BL_LIVRE", "BL_EXPEDIE");
    }

    @Test
    void rechercheInsensibleALaCasse() {
        List<Resume> resume = repository.resume(societe.pointDeVenteId(), DEBUT, FIN, "cerame");
        assertThat(resume).extracting(Resume::type)
                .doesNotContain("BC_FOURNISSEUR_CREE", "REGLEMENT_FOURNISSEUR")
                .contains("ENCAISSEMENT", "BL_LIVRE");

        assertThat(repository.evenements(societe.pointDeVenteId(), DEBUT, FIN, "f-1",
                EnumSet.allOf(TypeEvenementJournal.class), 0, 50))
                .extracting(Evenement::type).containsExactlyInAnyOrder("FACTURE_CREEE", "ENCAISSEMENT");
    }

    @Test
    void uneAutreSocieteNeVoitRien() {
        SocieteDeTest autre = SocieteDeTest.creer();
        assertThat(repository.resume(autre.pointDeVenteId(), DEBUT, FIN, null)).isEmpty();
        assertThat(repository.evenements(autre.pointDeVenteId(), DEBUT, FIN, null,
                EnumSet.allOf(TypeEvenementJournal.class), 0, 50)).isEmpty();
    }

    /** numero_paiement est unique toutes sociétés confondues. */
    private String numeroPaiement() {
        return "PAI-" + societe.code();
    }

    private long inserer(String sql, Map<String, ?> valeurs) {
        Long id = jdbc.queryForObject(sql, new MapSqlParameterSource(valeurs), Long.class);
        return id != null ? id : -1;
    }

    private static Timestamp heure(int h) {
        return Timestamp.valueOf(JOUR.atTime(h, 0));
    }
}

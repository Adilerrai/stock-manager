package com.gestion.compta;

import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.ReleveBancaireDTO;
import com.gestion.service.RapprochementBancaireService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S14 : l'import CSV d'un relevé, avec le point-virgule et avec la virgule. Un fichier sans opération donne une erreur lisible. */
@SpringBootTest
class ImportReleveCsvIT extends AbstractComptaIT {

    @Autowired private RapprochementBancaireService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private Long compte;

    @BeforeEach
    void preparer() {
        Long pdv = SocieteDeTest.creer().pointDeVenteId();
        ContexteDeTest.surPointDeVente(pdv);
        compte = new TransactionTemplate(transactions).execute(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            return jdbc.queryForObject("INSERT INTO comptes_financiers (code, nom, type, devise, solde_actuel, point_de_vente_id, actif) VALUES (?, 'Banque', 'COMPTE_BANCAIRE', 'MAD', 1000, ?, TRUE) RETURNING id", Long.class, "BQ-" + pdv, pdv);
        });
    }

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    private ReleveBancaireDTO importer(String contenu) {
        return service.importerReleveCsv(compte, "releve.csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    private List<BigDecimal[]> lignes(Long releve) {
        return jdbc.query("SELECT debit, credit FROM lignes_releves_bancaires WHERE releve_bancaire_id = ? ORDER BY date_operation, id", (rs, i) -> new BigDecimal[] {rs.getBigDecimal(1), rs.getBigDecimal(2)}, releve);
    }

    private static void egal(String attendu, BigDecimal obtenu) {
        assertEquals(0, new BigDecimal(attendu).compareTo(obtenu), attendu + " attendu, obtenu " + obtenu);
    }

    @Test
    void lePointVirguleAvecEnTete() {
        ReleveBancaireDTO r = importer("Date;Libellé;Débit;Crédit\n05/01/2026;Virement reçu;0;1500,00\n06/01/2026;Frais;120,50;0\n");
        List<BigDecimal[]> l = lignes(r.getId());
        assertEquals(2, l.size());
        egal("1500.00", l.get(0)[1]);
        egal("120.50", l.get(1)[0]);
        assertTrue(r.getReferenceReleve().startsWith("REL-BQ-"), r.getReferenceReleve());
    }

    @Test
    void laVirguleSansEnTete() {
        ReleveBancaireDTO r = importer("2026-01-05,Virement recu,0,1500.00\n2026-01-06,Frais,120.50,0\n");
        List<BigDecimal[]> l = lignes(r.getId());
        assertEquals(2, l.size());
        egal("1500.00", l.get(0)[1]);
        egal("120.50", l.get(1)[0]);
    }

    /** Une colonne crédit vide en fin de ligne reste un débit : il ne doit pas passer pour un montant positif. */
    @Test
    void uneColonneCreditVideResteUnDebit() {
        ReleveBancaireDTO r = importer("Date;Libellé;Débit;Crédit\n06/01/2026;Frais;120,50;\n07/01/2026;Virement reçu;;300,00\n");
        List<BigDecimal[]> l = lignes(r.getId());
        assertEquals(2, l.size());
        egal("120.50", l.get(0)[0]);
        egal("0", l.get(0)[1]);
        egal("0", l.get(1)[0]);
        egal("300.00", l.get(1)[1]);
    }

    @Test
    void leFormatAMontantSigneEtLeBom() {
        ReleveBancaireDTO r = importer("﻿Date;Libellé;Montant\n05/01/2026;Virement reçu;1500,00\n06/01/2026;Frais;-120,50\n");
        List<BigDecimal[]> l = lignes(r.getId());
        assertEquals(2, l.size());
        egal("1500.00", l.get(0)[1]);
        egal("120.50", l.get(1)[0]);
    }

    @Test
    void unFichierSansOperationDonneUneErreurLisible() {
        IllegalArgumentException vide = assertThrows(IllegalArgumentException.class, () -> importer(""));
        assertTrue(vide.getMessage().contains("aucune ligne"), vide.getMessage());
        assertThrows(IllegalArgumentException.class, () -> importer("Date;Libellé;Débit;Crédit\n"));
        assertThrows(IllegalArgumentException.class, () -> importer("n'importe quoi\nsans colonnes\n"));
    }
}

package com.gestion.compta.essais;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Suite : les contraintes citées par le document qu'aucun essai n'avait encore vues refuser. Usage : EssaisInvariants3 url */
public class EssaisInvariants3 {
    static BigDecimal m(String v) { return new BigDecimal(v); }
    static final BigDecimal Z = BigDecimal.ZERO;
    static long l1(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.l1(c, q, p); }
    static int ex(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.ex(c, q, p); }

    static final String LIEN = "insert into compta_liens_source(livre_id, source_type, source_id, evenement, version, point_de_vente_id, empreinte, instantane, ecriture_id, cree_par) "
            + "values (?,'FACTURE',4821,'COMPTABILISATION',?,1,'00000000000000000000000000000000','{}'::jsonb,?,'t')";

    public static void main(String[] a) throws Exception {
        EssaisInvariants.url = a[0];
        final long soc, socB, e25, e25B, tA; final String c5141, c6111, c7111, c3421, c8100, c2340, c2834, c61934;
        final String suffixe = Long.toString(System.nanoTime() % 100000);
        try (Connection c = EssaisInvariants.cx()) {
            soc = EssaisInvariants.activer(c, "TROIS" + suffixe);
            socB = EssaisInvariants.activer(c, "QUATRE" + suffixe);
            e25 = EssaisInvariants.creerExercice(c, soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null);
            e25B = EssaisInvariants.creerExercice(c, socB, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null);
            c5141 = EssaisInvariants.cpt(soc,"5141",c); c6111 = EssaisInvariants.cpt(soc,"6111",c); c7111 = EssaisInvariants.cpt(soc,"7111",c);
            c3421 = EssaisInvariants.cpt(soc,"3421",c); c8100 = EssaisInvariants.cpt(soc,"8100",c);
            c2340 = EssaisInvariants.cpt(soc,"2340",c); c2834 = EssaisInvariants.cpt(soc,"2834",c); c61934 = EssaisInvariants.cpt(soc,"61934",c);
            tA = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',1, null, 'Client A') returning id", soc);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id in (?,?) and numero=1", e25, e25B);
            c.commit();
        }
        final LocalDate j = LocalDate.of(2025,1,15);
        final long[] e1 = new long[1], eB = new long[1], fa = new long[1], re = new long[1];
        try (Connection c = EssaisInvariants.cx()) {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", e25);
            e1[0] = EssaisInvariants.ecrire(c, soc, e25, per, "OD", j, "COURANTE", null, new Object[][]{{c6111, null, m("100.00"), Z},{c5141, null, Z, m("100.00")}});
            long eV = EssaisInvariants.ecrire(c, soc, e25, per, "VT", j, "COURANTE", null, new Object[][]{{c3421, tA, m("1200.00"), Z},{c7111, null, Z, m("1200.00")}});
            long eP = EssaisInvariants.ecrire(c, soc, e25, per, "BQ", j, "COURANTE", null, new Object[][]{{c5141, null, m("1200.00"), Z},{c3421, tA, Z, m("1200.00")}});
            fa[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", eV, c3421);
            re[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", eP, c3421);
            long perB = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", e25B);
            String b6111 = EssaisInvariants.cpt(socB,"6111",c), b5141 = EssaisInvariants.cpt(socB,"5141",c);
            eB[0] = EssaisInvariants.ecrire(c, socB, e25B, perB, "OD", j, "COURANTE", null, new Object[][]{{b6111, null, m("10.00"), Z},{b5141, null, Z, m("10.00")}});
            c.commit();
        }

        // Liens vers les pièces commerciales : un seul par (type, id, événement, version), en ajout seul, dans la même société
        EssaisInvariants.passe("lien d'une pièce vers son écriture", c -> ex(c, LIEN, soc, 1, e1[0]));
        EssaisInvariants.refuse("même pièce, même événement, même version : double comptabilisation", "uk_compta_lie", c -> ex(c, LIEN, soc, 1, e1[0]));
        EssaisInvariants.refuse("version suivante alors que la précédente reste active", "compta_lien_deja_actif", c -> ex(c, LIEN, soc, 2, e1[0]));
        EssaisInvariants.passe("contre-passation, puis version suivante de la même pièce", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", e25);
            EssaisInvariants.ecrire(c, soc, e25, per, "OD", j, "COURANTE", e1[0], new Object[][]{{c6111, null, Z, m("100.00")},{c5141, null, m("100.00"), Z}});
            long e2 = EssaisInvariants.ecrire(c, soc, e25, per, "OD", j, "COURANTE", null, new Object[][]{{c6111, null, m("100.00"), Z},{c5141, null, Z, m("100.00")}});
            ex(c, LIEN, soc, 2, e2);
        });
        EssaisInvariants.refuse("lien modifié", "COMPTA_IMMUABLE", c -> ex(c, "update compta_liens_source set version=9 where livre_id=?", soc));
        EssaisInvariants.refuse("lien supprimé", "COMPTA_IMMUABLE", c -> ex(c, "delete from compta_liens_source where livre_id=?", soc));
        EssaisInvariants.refuse("lien vers l'écriture d'une autre société", "fk_compta_lie_ecriture", c -> ex(c, LIEN.replace("4821","4822"), soc, 1, eB[0]));

        // TVA exigible : une opération dans une seule déclaration
        final long[] d = new long[2];
        String insTvc = "insert into compta_tva_operations_collectees(livre_id, declaration_id, source_type, source_id, facture_id, point_de_vente_id, empreinte_source, date_generateur, taux, ttc, ht, tva) "
                + "values (?,?,'PAIEMENT',77,5,1,'00000000000000000000000000000000','2026-01-10',10,110,100,10)";
        EssaisInvariants.passe("encaissement déclaré en janvier", c -> {
            d[0] = l1(c, "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite) values (?,2026,1,'2026-01-01','2026-01-31','MENSUELLE',1,'ENCAISSEMENT','2026-02-28') returning id", soc);
            d[1] = l1(c, "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite) values (?,2026,2,'2026-02-01','2026-02-28','MENSUELLE',1,'ENCAISSEMENT','2026-03-31') returning id", soc);
            ex(c, insTvc, soc, d[0]);
        });
        EssaisInvariants.refuse("même encaissement déclaré en février", "uq_compta_tvc_unique", c -> ex(c, insTvc, soc, d[1]));

        // Prorata de TVA : une valeur explicite entre 0 et 100, jamais de repli silencieux (audit T6)
        EssaisInvariants.refuse("prorata de TVA supérieur à 100", "ck_compta_tvp_prorata", c -> ex(c, "insert into compta_tva_parametres(livre_id, annee, fait_generateur, periodicite, prorata) values (?,2026,'ENCAISSEMENT','MENSUELLE',150)", soc));

        // Rapprochement : un état validé tombe juste
        String insRap = "insert into compta_rapprochements(livre_id, compte_numero, compte_financier_id, date_arrete, solde_comptable, solde_releve, solde_comptable_rectifie, solde_releve_rectifie, contenu, valide_par) values (?,?,8,?,?,?,?,?,'{}'::jsonb,'t')";
        EssaisInvariants.refuse("état de rapprochement validé avec un écart", "ck_compta_rap_egalite", c -> ex(c, insRap, soc, c5141, LocalDate.of(2025,1,31), m("8000"), m("10000"), m("8000"), m("12000")));
        EssaisInvariants.passe("état de rapprochement validé sans écart (S13)", c -> ex(c, insRap, soc, c5141, LocalDate.of(2025,1,31), m("8000"), m("10000"), m("8000"), m("8000")));

        // Une ligne d'à-nouveau ne se pointe pas : elle reprend un solde, pas une opération bancaire (audit RA2)
        String insPoi = "insert into compta_pointages(livre_id, ligne_releve_id, ligne_id, montant, date_pointage, pointe_par) values (?,?,?,?,'2025-01-31','t')";
        EssaisInvariants.passe("pointage d'une ligne courante de banque", c -> {
            long ligne = l1(c, "select id from compta_lignes where ecriture_id=? and credit>0", eB[0]);
            ex(c, insPoi, socB, 9001L, ligne, m("10.00"));
        });
        EssaisInvariants.refuse("pointage d'une ligne d'à-nouveau", "COMPTA_POINTAGE_A_NOUVEAU", c -> {
            long per0 = l1(c, "select id from compta_periodes where exercice_id=? and numero=0", e25B);
            String b5141 = EssaisInvariants.cpt(socB,"5141",c), b1111 = EssaisInvariants.cpt(socB,"1111",c);
            long an = EssaisInvariants.ecrire(c, socB, e25B, per0, "AN", LocalDate.of(2025,1,1), "A_NOUVEAU", null, new Object[][]{{b5141, null, m("500.00"), Z},{b1111, null, Z, m("500.00")}});
            long ligne = l1(c, "select id from compta_lignes where ecriture_id=? and debit>0", an);
            ex(c, insPoi, socB, 9002L, ligne, m("500.00"));
        });

        // Immobilisation : née d'une écriture de la même société
        EssaisInvariants.refuse("immobilisation liée à l'écriture d'une autre société", "fk_compta_imm_acquisition", c -> ex(c,
                "insert into compta_immobilisations(livre_id, code, designation, compte_immo, compte_amort, compte_dotation, date_acquisition, date_mise_en_service, valeur_brute, tva_deduite, mode_amortissement, duree_mois, ecriture_acquisition_id, cree_par) values (?,'IMM3','Fourgon',?,?,?,?,?,100000,20000,'LINEAIRE',60,?,'t')",
                soc, c2340, c2834, c61934, j, j, eB[0]));

        // Filets : même si les déclencheurs étaient contournés, l'index refuse encore le doublon
        EssaisInvariants.refuse("numéro d'écriture en double, déclencheurs contournés", "uk_compta_ecr_numero", c -> {
            ex(c, "set local session_replication_role = replica");
            ex(c, "insert into compta_ecritures(livre_id, exercice_id, periode_id, journal_id, type_journal, numero, date_ecriture, date_piece, ref_piece, libelle, nature, origine, est_contrepassation, nb_lignes, total_debit, total_credit, saisie_le, saisie_par, valide_le, valide_par) "
                    + "select livre_id, exercice_id, periode_id, journal_id, type_journal, numero, date_ecriture, date_piece, ref_piece, libelle, nature, origine, est_contrepassation, nb_lignes, total_debit, total_credit, saisie_le, saisie_par, valide_le, valide_par from compta_ecritures where id=?", e1[0]);
        });
        final long[] g = new long[1];
        EssaisInvariants.passe("lettrage total du client A", c -> {
            g[0] = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','TOTAL','MANUEL',?,'t') returning id", soc, c3421, tA, j);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", fa[0], g[0], soc, c3421, tA);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", re[0], g[0], soc, c3421, tA);
        });
        EssaisInvariants.refuse("numéro de lettrage en double, déclencheurs contournés", "uk_compta_let_numero", c -> {
            ex(c, "set local session_replication_role = replica");
            ex(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) select livre_id, compte_numero, tiers_id, numero, 'ZZZ', statut, origine, date_lettrage, cree_par from compta_lettrages where id=?", g[0]);
        });

        // Clôture : une seule écriture de clôture, lettrages gelés une fois l'exercice clos
        try (Connection c = EssaisInvariants.cx()) {
            for (int k = 1; k <= 12; k++) {
                ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", e25, k);
                if (k < 12) ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", e25, k + 1);
                ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=?", e25, k);
            }
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=99", e25);
            ex(c, "update compta_exercices set statut='ARRETE', arrete_le=now(), arrete_par='t' where id=?", e25);
            c.commit();
        }
        final LocalDate fin = LocalDate.of(2025,12,31);
        EssaisInvariants.passe("écriture de clôture", c -> EssaisInvariants.ecrire(c, soc, e25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", e25), "CL", fin, "CLOTURE", null, new Object[][]{{c8100, null, m("100.00"), Z},{c6111, null, Z, m("100.00")}}));
        EssaisInvariants.refuse("deuxième écriture de clôture", "uq_compta_ecr_cl", c -> EssaisInvariants.ecrire(c, soc, e25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", e25), "CL", fin, "CLOTURE", null, new Object[][]{{c8100, null, m("10.00"), Z},{c6111, null, Z, m("10.00")}}));
        EssaisInvariants.passe("clôture définitive", c -> {
            ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=99", e25);
            ex(c, "update compta_exercices set statut='CLOTURE', cloture_le=now(), cloture_par='t' where id=?", e25);
        });
        EssaisInvariants.refuse("délettrage d'une ligne d'un exercice clos", "COMPTA_LETTRAGE_EXERCICE_CLOS", c -> ex(c, "delete from compta_lettrage_lignes where lettrage_id=?", g[0]));
        System.out.println("----\nRéussis " + EssaisInvariants.ok + ", échecs " + EssaisInvariants.ko);
    }
}

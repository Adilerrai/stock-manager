package com.gestion.compta.essais;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Suite : cycle d'exercice, extourne, immobilisations, TVA, rapprochement, IS, paramètres. Usage : EssaisInvariants2 url */
public class EssaisInvariants2 {
    static BigDecimal m(String v) { return new BigDecimal(v); }
    static final BigDecimal Z = BigDecimal.ZERO;
    static long l1(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.l1(c, q, p); }
    static int ex(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.ex(c, q, p); }

    static long ids[] = new long[20];

    public static void main(String[] a) throws Exception {
        EssaisInvariants.url = a[0];
        long soc, e25; String c5141, c6111, c7111, c3491, c1111, c1191, c8100, c2332, c2833x, c61933, c6513, c7513, c3481, c4455, c34552, c34551, c4411, c3421;
        long tF, tC;
        try (Connection c = EssaisInvariants.cx()) {
            soc = EssaisInvariants.activer(c, "CYCLE");
            e25 = EssaisInvariants.creerExercice(c, soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null);
            c.commit();
            c5141 = EssaisInvariants.cpt(soc,"5141",c); c6111 = EssaisInvariants.cpt(soc,"6111",c); c7111 = EssaisInvariants.cpt(soc,"7111",c); c3491 = EssaisInvariants.cpt(soc,"3491",c);
            c1111 = EssaisInvariants.cpt(soc,"1111",c); c1191 = EssaisInvariants.cpt(soc,"1191",c); c8100 = EssaisInvariants.cpt(soc,"8100",c);
            c2332 = EssaisInvariants.s1(c, "select numero from compta_comptes where livre_id=? and numero like '2332%' and imputable order by numero limit 1", soc);
            c2833x = EssaisInvariants.s1(c, "select numero from compta_comptes where livre_id=? and numero like '2833%' and imputable order by numero limit 1", soc);
            c61933 = EssaisInvariants.cpt(soc,"61933",c); c6513 = EssaisInvariants.cpt(soc,"6513",c); c7513 = EssaisInvariants.cpt(soc,"7513",c); c3481 = EssaisInvariants.cpt(soc,"3481",c);
            c4455 = EssaisInvariants.cpt(soc,"4455",c); c34552 = EssaisInvariants.cpt(soc,"34552",c); c34551 = EssaisInvariants.cpt(soc,"34551",c); c4411 = EssaisInvariants.cpt(soc,"4411",c); c3421 = EssaisInvariants.cpt(soc,"3421",c);
            tF = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'FOURNISSEUR','FOURNISSEUR',1, null, 'F') returning id", soc);
            tC = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',1, null, 'C') returning id", soc);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero in (1,2)", e25);
            c.commit();
        }
        final long fsoc = soc, fe25 = e25; final String f5141 = c5141, f6111 = c6111, f7111 = c7111, f3491 = c3491, f1111 = c1111, f1191 = c1191, f8100 = c8100, f2332 = c2332, f2833 = c2833x, f61933 = c61933, f6513 = c6513, f7513 = c7513, f3481 = c3481, f4455 = c4455, f34552 = c34552, f34551 = c34551, f4411 = c4411, f3421 = c3421; final long ftF = tF, ftC = tC;
        LocalDate j = LocalDate.of(2025,1,15);

        // Immobilisations
        final long[] eAcq = new long[1], imm = new long[1], eDot = new long[1];
        EssaisInvariants.passe("acquisition d'une immobilisation : écriture et registre", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", fe25);
            eAcq[0] = EssaisInvariants.ecrire(c, fsoc, fe25, per, "AC", j, "COURANTE", null, new Object[][]{{f2332, null, m("100000.00"), Z},{f34551, null, m("20000.00"), Z},{f4411, ftF, Z, m("120000.00")}});
            imm[0] = l1(c, "insert into compta_immobilisations(livre_id, code, designation, compte_immo, compte_amort, compte_dotation, date_acquisition, date_mise_en_service, valeur_brute, tva_deduite, mode_amortissement, duree_mois, ecriture_acquisition_id, cree_par) values (?,'IMM1','Machine',?,?,?,?,?,100000,20000,'LINEAIRE',60,?,'t') returning id",
                    fsoc, f2332, f2833, f61933, j, j, eAcq[0]);
        });
        EssaisInvariants.refuse("immobilisation sans écriture d'acquisition", "ecriture_acquisition_id", c -> ex(c, "insert into compta_immobilisations(livre_id, code, designation, compte_immo, date_acquisition, valeur_brute, mode_amortissement, cree_par) values (?,'IMM2','X',?,?,100,'NON_AMORTISSABLE','t')", fsoc, f2332, j));
        EssaisInvariants.passe("dotation", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", fe25);
            eDot[0] = EssaisInvariants.ecrire(c, fsoc, fe25, per, "OD", j, "COURANTE", null, new Object[][]{{f61933, null, m("20000.00"), Z},{f2833, null, Z, m("20000.00")}});
            ex(c, "insert into compta_amortissements(livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) values (?,?,?,'ANNUELLE','2025-01-01','2025-12-31',20000,?)", fsoc, imm[0], fe25, eDot[0]);
        });
        EssaisInvariants.refuse("deuxième dotation annuelle sur l'exercice", "uq_compta_amo_active", c -> ex(c, "insert into compta_amortissements(livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) values (?,?,?,'ANNUELLE','2025-01-01','2025-12-31',1,?)", fsoc, imm[0], fe25, eDot[0]));
        EssaisInvariants.refuse("cumul au-delà de la valeur brute (S12.b)", "COMPTA_IMMO_SURAMORTIE", c -> ex(c, "insert into compta_amortissements(livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) values (?,?,?,'COMPLEMENT','2025-01-01','2025-12-31',90000,?)", fsoc, imm[0], fe25, eDot[0]));
        EssaisInvariants.refuse("sortie dont la plus-value contredit le prix", "ck_compta_sor_pmv", c -> ex(c, "insert into compta_immobilisation_sorties(immobilisation_id, livre_id, nature, date_sortie, prix_cession_ht, amortissements_cumules, valeur_nette, plus_ou_moins_value, ecriture_sortie_id, ecriture_prix_id, cree_par) values (?,?,'CESSION','2025-01-20',70000,20000,80000,5000,?,?,'t')", imm[0], fsoc, eDot[0], eDot[0]));
        EssaisInvariants.passe("cession avec 7513 (S11.a)", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", fe25);
            long eS = EssaisInvariants.ecrire(c, fsoc, fe25, per, "OD", LocalDate.of(2025,1,20), "COURANTE", null, new Object[][]{{f2833, null, m("20000.00"), Z},{f6513, null, m("80000.00"), Z},{f2332, null, Z, m("100000.00")}});
            long eP = EssaisInvariants.ecrire(c, fsoc, fe25, per, "OD", LocalDate.of(2025,1,20), "COURANTE", null, new Object[][]{{f3481, null, m("84000.00"), Z},{f7513, null, Z, m("70000.00")},{f4455, null, Z, m("14000.00")}});
            ex(c, "insert into compta_immobilisation_sorties(immobilisation_id, livre_id, nature, date_sortie, prix_cession_ht, tva_cession, amortissements_cumules, valeur_nette, plus_ou_moins_value, ecriture_sortie_id, ecriture_prix_id, cree_par) values (?,?,'CESSION','2025-01-20',70000,14000,20000,80000,-10000,?,?,'t')", imm[0], fsoc, eS, eP);
            ex(c, "update compta_immobilisations set statut='SORTIE' where id=?", imm[0]);
        });
        EssaisInvariants.refuse("deuxième cession (S11.c)", "uq_compta_sor_active", c -> ex(c, "insert into compta_immobilisation_sorties(immobilisation_id, livre_id, nature, date_sortie, prix_cession_ht, tva_cession, amortissements_cumules, valeur_nette, plus_ou_moins_value, ecriture_sortie_id, cree_par) values (?,?,'RETRAIT','2025-01-21',0,0,20000,80000,-80000,?,'t')", imm[0], fsoc, eDot[0]));
        EssaisInvariants.refuse("dotation après la cession (S11.d)", "COMPTA_IMMO_SORTIE", c -> ex(c, "insert into compta_amortissements(livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) values (?,?,?,'COMPLEMENT','2025-01-01','2025-01-20',100,?)", fsoc, imm[0], fe25, eDot[0]));

        // TVA
        final long[] decl = new long[2];
        EssaisInvariants.passe("deux déclarations", c -> {
            decl[0] = l1(c, "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite) values (?,2026,1,'2026-01-01','2026-01-31','MENSUELLE',1,'ENCAISSEMENT','2026-02-28') returning id", fsoc);
            decl[1] = l1(c, "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite) values (?,2026,2,'2026-02-01','2026-02-28','MENSUELLE',1,'ENCAISSEMENT','2026-03-31') returning id", fsoc);
        });
        String insDed = "insert into compta_tva_operations_deductibles(livre_id, declaration_id, source_type, source_id, facture_achat_id, point_de_vente_id, empreinte_source, numero_facture, date_facture, fournisseur_nom, designation, rubrique_code, compte_tva, ht, taux, tva, ttc, tva_deductible, mp_id, date_paiement) values (?,?,'REGLEMENT_FOURNISSEUR',1,10,1,'e','FA','2026-01-01','F','D','ACHATS',?,1000,20,200,1200,200,4,'2026-01-20')";
        EssaisInvariants.passe("déduction en janvier", c -> ex(c, insDed, fsoc, decl[0], "34552"));
        EssaisInvariants.refuse("même règlement déduit en février (S9)", "uq_compta_tvo_unique", c -> ex(c, insDed, fsoc, decl[1], "34552"));
        EssaisInvariants.refuse("déduction sur 3455 seul", "ck_compta_tvo_compte", c -> ex(c, insDed.replace(",1,10,1,'e'", ",2,10,1,'e'"), fsoc, decl[1], "3455"));
        EssaisInvariants.refuse("HT + TVA différent du TTC", "ck_compta_tvo_montants", c -> ex(c, insDed.replace(",1,10,1,'e'", ",3,10,1,'e'").replace("1000,20,200,1200", "1000,20,200,1201"), fsoc, decl[1], "34552"));
        EssaisInvariants.passe("validation de janvier", c -> ex(c, "update compta_tva_declarations set statut='VALIDEE', valide_le=now(), valide_par='t' where id=?", decl[0]));
        EssaisInvariants.refuse("opération modifiée après validation", "COMPTA_TVA_FIGEE", c -> ex(c, "update compta_tva_operations_deductibles set ht=1 where declaration_id=?", decl[0]));
        EssaisInvariants.refuse("opération supprimée après validation", "COMPTA_TVA_FIGEE", c -> ex(c, "delete from compta_tva_operations_deductibles where declaration_id=?", decl[0]));
        EssaisInvariants.refuse("TVA due et crédit à la fois", "ck_compta_tvd_resultat", c -> ex(c, "update compta_tva_declarations set tva_due=10, credit_periode=5 where id=?", decl[1]));

        // Rapprochement
        final long[] rel = new long[1], l1200 = new long[2];
        EssaisInvariants.passe("deux règlements de 1 200, un seul débit bancaire de 1 200", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=1", fe25);
            long a1 = EssaisInvariants.ecrire(c, fsoc, fe25, per, "BQ", j, "COURANTE", null, new Object[][]{{f4411, ftF, m("1200.00"), Z},{f5141, null, Z, m("1200.00")}});
            long a2 = EssaisInvariants.ecrire(c, fsoc, fe25, per, "BQ", j, "COURANTE", null, new Object[][]{{f4411, ftF, m("1200.00"), Z},{f5141, null, Z, m("1200.00")}});
            l1200[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", a1, f5141);
            l1200[1] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", a2, f5141);
            long pdv = l1(c, "select point_de_vente_id from compta_livre_points_de_vente where livre_id=?", fsoc);
            long cf = l1(c, "insert into comptes_financiers(code, nom, type, devise, point_de_vente_id) values (?, 'B', 'COMPTE_BANCAIRE', 'MAD', ?) returning id", "CF" + System.nanoTime(), pdv);
            long rb = l1(c, "insert into releves_bancaires(reference_releve, compte_financier_id, date_debut, date_fin, point_de_vente_id) values ('R',?, '2025-01-01','2025-01-31',?) returning id", cf, pdv);
            rel[0] = l1(c, "insert into lignes_releves_bancaires(releve_bancaire_id, date_operation, libelle, debit, statut_rapprochement, point_de_vente_id) values (?, '2025-01-16','CHQ',1200,'NON_RAPPROCHE',?) returning id", rb, pdv);
        });
        EssaisInvariants.passe("pointage du premier", c -> ex(c, "insert into compta_pointages(livre_id, ligne_releve_id, ligne_id, montant, date_pointage, pointe_par) values (?,?,?,1200,'2025-01-31','t')", fsoc, rel[0], l1200[0]));
        EssaisInvariants.refuse("la même ligne de relevé pointée une seconde fois (S10)", "COMPTA_POINTAGE_EXCESSIF", c -> ex(c, "insert into compta_pointages(livre_id, ligne_releve_id, ligne_id, montant, date_pointage, pointe_par) values (?,?,?,1200,'2025-01-31','t')", fsoc, rel[0], l1200[1]));

        // IS
        EssaisInvariants.refuse("impôt dû différent du maximum IS/CM", "ck_compta_isc_max", c -> ex(c, "insert into compta_is_calculs(livre_id, exercice_id, categorie_taux, resultat_comptable, resultat_fiscal, base_arrondie, taux_is, is_theorique, base_cm, cm, impot_du, parametres) values (?,?,'BN_JUSQUA_300K',100,100,100,20,20,1000,3000,20,'{}')", fsoc, fe25));
        EssaisInvariants.refuse("paramètre fiscal chevauchant", "COMPTA_PARAMETRE_CHEVAUCHEMENT", c -> ex(c, "insert into compta_parametres_fiscaux(code, cle, rattachement, valide_du, valide_au, valeur, source) values ('IS_TAUX','BN_JUSQUA_300K','OUVERTURE_EXERCICE','2025-06-01','2025-06-30',1,'t')"));

        // Cycle d'exercice, écritures d'inventaire, extourne
        final long[] eInv = new long[1], eCour = new long[1], e26 = new long[1];
        EssaisInvariants.passe("écriture courante avant la fin de l'exercice", c -> eCour[0] = EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=2", fe25), "OD", LocalDate.of(2025,2,10), "COURANTE", null, new Object[][]{{f6111, null, m("500.00"), Z},{f5141, null, Z, m("500.00")}}));
        try (Connection c = EssaisInvariants.cx()) {
            for (int k = 1; k <= 12; k++) {
                ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", fe25, k);
                if (k < 12) ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", fe25, k + 1);
                ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=?", fe25, k);
            }
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=99", fe25);
            c.commit();
        }
        LocalDate fin = LocalDate.of(2025,12,31);
        EssaisInvariants.refuse("écriture courante en période de clôture", "COMPTA_NATURE_PERIODE", c -> EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "OD", fin, "COURANTE", null, new Object[][]{{f6111, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        EssaisInvariants.passe("écriture d'inventaire en période de clôture (charge constatée d'avance)", c -> { EssaisInvariants.aExtourner = true; eInv[0] = EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "OD", fin, "INVENTAIRE", null, new Object[][]{{f3491, null, m("100.00"), Z},{f6111, null, Z, m("100.00")}}); EssaisInvariants.aExtourner = false; });
        EssaisInvariants.passe("arrêté de l'exercice", c -> ex(c, "update compta_exercices set statut='ARRETE', arrete_le=now(), arrete_par='t' where id=?", fe25));
        EssaisInvariants.refuse("écriture d'inventaire après l'arrêté", "COMPTA_EXERCICE_ARRETE", c -> EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "OD", fin, "INVENTAIRE", null, new Object[][]{{f3491, null, m("10.00"), Z},{f6111, null, Z, m("10.00")}}));
        EssaisInvariants.passe("levée de l'arrêté, procédure explicite", c -> ex(c, "update compta_exercices set statut='OUVERT' where id=?", fe25));
        EssaisInvariants.refuse("écriture de clôture sans arrêté", "COMPTA_EXERCICE_ARRETE", c -> EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "CL", fin, "CLOTURE", null, new Object[][]{{f8100, null, m("10.00"), Z},{f6111, null, Z, m("10.00")}}));
        EssaisInvariants.passe("nouvel arrêté", c -> ex(c, "update compta_exercices set statut='ARRETE', arrete_le=now(), arrete_par='t' where id=?", fe25));
        EssaisInvariants.refuse("écriture de clôture sur un compte de bilan", "ck_compta_lig_cl_classe", c -> EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "CL", fin, "CLOTURE", null, new Object[][]{{f8100, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        EssaisInvariants.passe("clôture définitive", c -> {
            EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "CL", fin, "CLOTURE", null, new Object[][]{{f8100, null, m("400.00"), Z},{f6111, null, Z, m("400.00")}});
            ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=99", fe25);
            ex(c, "update compta_exercices set statut='CLOTURE', cloture_le=now(), cloture_par='t' where id=?", fe25);
            e26[0] = EssaisInvariants.creerExercice(c, fsoc, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), fe25);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero in (1,2)", e26[0]);
        });
        EssaisInvariants.refuse("réouverture après clôture définitive", "COMPTA_EXERCICE_CLOS", c -> ex(c, "update compta_exercices set statut='OUVERT' where id=?", fe25));
        EssaisInvariants.refuse("ajout dans un exercice clos (avis 2.3 b)", "COMPTA_PERIODE_FERMEE", c -> EssaisInvariants.ecrire(c, fsoc, fe25, l1(c, "select id from compta_periodes where exercice_id=? and numero=99", fe25), "OD", fin, "INVENTAIRE", null, new Object[][]{{f3491, null, m("10.00"), Z},{f6111, null, Z, m("10.00")}}));
        EssaisInvariants.refuse("miroir d'une écriture courante d'un exercice clos", "exercice différent", c -> EssaisInvariants.ecrire(c, fsoc, e26[0], l1(c, "select id from compta_periodes where exercice_id=? and numero=1", e26[0]), "OD", LocalDate.of(2026,1,1), "COURANTE", eCour[0], new Object[][]{{f6111, null, Z, m("500.00")},{f5141, null, m("500.00"), Z}}));
        EssaisInvariants.passe("extourne à l'ouverture d'une écriture d'inventaire", c -> EssaisInvariants.ecrire(c, fsoc, e26[0], l1(c, "select id from compta_periodes where exercice_id=? and numero=1", e26[0]), "OD", LocalDate.of(2026,1,1), "COURANTE", eInv[0], new Object[][]{{f3491, null, Z, m("100.00")},{f6111, null, m("100.00"), Z}}));
        // Gardiens ajoutés : à-nouveau prématuré, période de clôture, deux écritures dans une transaction
        final long[] e27 = new long[1];
        EssaisInvariants.passe("exercice 2027 créé alors que 2026 est ouvert", c -> e27[0] = EssaisInvariants.creerExercice(c, fsoc, "2027", LocalDate.of(2027,1,1), LocalDate.of(2027,12,31), e26[0]));
        EssaisInvariants.refuse("à-nouveau alors que l'exercice précédent n'est pas en clôture", "COMPTA_AN_PREMATURE", c -> EssaisInvariants.ecrire(c, fsoc, e27[0], l1(c, "select id from compta_periodes where exercice_id=? and numero=0", e27[0]), "AN", LocalDate.of(2027,1,1), "A_NOUVEAU", null, new Object[][]{{f5141, null, m("10.00"), Z},{f1111, null, Z, m("10.00")}}));
        EssaisInvariants.refuse("période de clôture ouverte avant la fin des périodes courantes", "COMPTA_PERIODES_OUVERTES", c -> ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=99", e26[0]));
        EssaisInvariants.refuse("période de clôture de 2027 alors que 2026 n'est pas clos", "COMPTA_CLOTURE_NON_CHRONOLOGIQUE", c -> {
            ex(c, "alter table compta_periodes disable trigger trg_compta_periodes_gardien");
            ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id in (?,?) and type_periode='COURANTE'", e26[0], e27[0]);
            ex(c, "alter table compta_periodes enable trigger trg_compta_periodes_gardien");
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=99", e27[0]);
        });
        EssaisInvariants.passe("deux écritures dans une transaction, scellement vérifié après chacune", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=2", e26[0]);
            try (Statement st = c.createStatement()) {
                EssaisInvariants.ecrire(c, fsoc, e26[0], per, "OD", LocalDate.of(2026,2,3), "COURANTE", null, new Object[][]{{f6111, null, m("11.00"), Z},{f5141, null, Z, m("11.00")}});
                st.execute("SET CONSTRAINTS trg_compta_ecritures_scellement IMMEDIATE"); st.execute("SET CONSTRAINTS trg_compta_ecritures_scellement DEFERRED");
                EssaisInvariants.ecrire(c, fsoc, e26[0], per, "OD", LocalDate.of(2026,2,3), "COURANTE", null, new Object[][]{{f6111, null, m("12.00"), Z},{f5141, null, Z, m("12.00")}});
                st.execute("SET CONSTRAINTS trg_compta_ecritures_scellement IMMEDIATE"); st.execute("SET CONSTRAINTS trg_compta_ecritures_scellement DEFERRED");
            }
        });
        EssaisInvariants.refuse("scellement vérifié sur place, avant le commit", "COMPTA_ECRITURE_INCOHERENTE", c -> {
            long per = l1(c, "select id from compta_periodes where exercice_id=? and numero=2", e26[0]);
            EssaisInvariants.ecrireBrut(c, fsoc, e26[0], per, "OD", LocalDate.of(2026,2,3), "COURANTE", null, new Object[][]{{f6111, null, m("11.00"), Z},{f5141, null, Z, m("11.00")}}, 3);
            try (Statement st = c.createStatement()) { st.execute("SET CONSTRAINTS trg_compta_ecritures_scellement IMMEDIATE"); }
            System.out.println("KO   SET CONSTRAINTS n'a pas levé d'erreur");
        });
        System.out.println("----\nRéussis " + EssaisInvariants.ok + ", échecs " + EssaisInvariants.ko);
    }
}

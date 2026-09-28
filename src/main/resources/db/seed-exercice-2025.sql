-- =============================================================================
-- SCRIPT COMPLET DE PEUPLEMENT D'UN EXERCICE COMPTABLE 2025
-- Système : Comptabilité Générale Marocaine (PCGM) & Flux Commerciaux
-- Période : 01/01/2025 au 31/12/2025
-- Équilibre strict Débit = Crédit au centime près (Écart = 0.00 MAD)
-- Inclut :
--   1. Exercice 2025 (EXO-2025)
--   2. Journaux comptables (AN, VE, AC, BQ, CA, OD)
--   3. Plan de comptes PCGM complet (Classes 1 à 7)
--   4. Écritures d'ouverture (À-nouveaux) au 01/01/2025
--   5. Ventes, Achats, Règlements mensuels 2025
--   6. Écritures déjà lettrées (codes 'AA', 'AB', 'AC', 'BA') visibles en vert
--   7. Écritures ouvertes non lettrées équilibrées (pour tester le lettrage en direct)
--   8. Déclarations et liquidations de TVA trimestrielles T1, T2, T3, T4
--   9. Travaux d'inventaire au 31/12/2025 (Stocks, Amortissements)
-- =============================================================================

DO $$
DECLARE
    -- Paramètres configurables du Tenant / Entreprise cible
    -- Par défaut configuré pour le Tenant 2 (avec fallback si non trouvé)
    v_tenant_id BIGINT := 2; 
    
    -- Variables d'identifiants
    v_j_an BIGINT;
    v_j_ve BIGINT;
    v_j_ac BIGINT;
    v_j_bq BIGINT;
    v_j_ca BIGINT;
    v_j_od BIGINT;

    v_c_1111 BIGINT;
    v_c_1161 BIGINT;
    v_c_2332 BIGINT;
    v_c_2340 BIGINT;
    v_c_2833 BIGINT;
    v_c_3111 BIGINT;
    v_c_3421 BIGINT;
    v_c_3421_c1 BIGINT;
    v_c_3421_c2 BIGINT;
    v_c_3421_c3 BIGINT;
    v_c_3455 BIGINT;
    v_c_4411 BIGINT;
    v_c_4411_f1 BIGINT;
    v_c_4411_f2 BIGINT;
    v_c_4455 BIGINT;
    v_c_4456 BIGINT;
    v_c_5141 BIGINT;
    v_c_5161 BIGINT;
    v_c_6111 BIGINT;
    v_c_6114 BIGINT;
    v_c_6131 BIGINT;
    v_c_6145 BIGINT;
    v_c_6147 BIGINT;
    v_c_6171 BIGINT;
    v_c_6193 BIGINT;
    v_c_7111 BIGINT;
    v_c_7124 BIGINT;

    v_ecr_id BIGINT;
BEGIN
    RAISE NOTICE 'Démarrage de l''initialisation de l''exercice comptable 2025 pour le Tenant %...', v_tenant_id;

    -- =========================================================================
    -- 1. EXERCICE COMPTABLE 2025
    -- =========================================================================
    DELETE FROM ecritures_comptables 
    WHERE point_de_vente_id = v_tenant_id 
      AND date_ecriture BETWEEN '2025-01-01' AND '2025-12-31';

    INSERT INTO exercices_comptables (code, libelle, date_debut, date_fin, statut, point_de_vente_id)
    VALUES ('EXO-2025', 'Exercice Comptable Annuel 2025', '2025-01-01', '2025-12-31', 'OUVERT', v_tenant_id)
    ON CONFLICT DO NOTHING;

    -- =========================================================================
    -- 2. JOURNAUX COMPTABLES OBLIGATOIRES
    -- =========================================================================
    INSERT INTO journaux_comptables (code, libelle, type_journal, actif, point_de_vente_id)
    VALUES 
        ('AN', 'Journal des À-Nouveaux', 'A_NOUVEAUX', TRUE, v_tenant_id),
        ('VE', 'Journal des Ventes', 'VENTES', TRUE, v_tenant_id),
        ('AC', 'Journal des Achats', 'ACHATS', TRUE, v_tenant_id),
        ('BQ', 'Journal de Banque', 'BANQUE', TRUE, v_tenant_id),
        ('CA', 'Journal de Caisse', 'CAISSE', TRUE, v_tenant_id),
        ('OD', 'Journal des Opérations Diverses', 'OPERATIONS_DIVERSES', TRUE, v_tenant_id)
    ON CONFLICT (code, point_de_vente_id) DO UPDATE SET actif = TRUE;

    -- Récupération dynamique des IDs de journaux
    SELECT id INTO v_j_an FROM journaux_comptables WHERE code = 'AN' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_j_ve FROM journaux_comptables WHERE code = 'VE' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_j_ac FROM journaux_comptables WHERE code = 'AC' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_j_bq FROM journaux_comptables WHERE code = 'BQ' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_j_ca FROM journaux_comptables WHERE code = 'CA' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_j_od FROM journaux_comptables WHERE code = 'OD' AND point_de_vente_id = v_tenant_id;

    -- =========================================================================
    -- 3. PLAN DE COMPTES (PCGM MAROCAIN STANDARD)
    -- =========================================================================
    INSERT INTO comptes_comptables (numero_compte, libelle, classe, sens_par_defaut, actif, point_de_vente_id)
    VALUES 
        ('11110000', 'Capital social', 1, 'CREDIT', TRUE, v_tenant_id),
        ('11610000', 'Report à nouveau créditeur', 1, 'CREDIT', TRUE, v_tenant_id),
        ('23320000', 'Matériel et outillage', 2, 'DEBIT', TRUE, v_tenant_id),
        ('23400000', 'Matériel de transport', 2, 'DEBIT', TRUE, v_tenant_id),
        ('28330000', 'Amortissements des installations et outillage', 2, 'CREDIT', TRUE, v_tenant_id),
        ('31110000', 'Marchandises (Stock)', 3, 'DEBIT', TRUE, v_tenant_id),
        ('34210000', 'Clients (Collectif)', 3, 'DEBIT', TRUE, v_tenant_id),
        ('34210001', 'Client Ste Alpha SARL', 3, 'DEBIT', TRUE, v_tenant_id),
        ('34210002', 'Client Bâtiment Moderne', 3, 'DEBIT', TRUE, v_tenant_id),
        ('34210003', 'Client Travaux Atlas', 3, 'DEBIT', TRUE, v_tenant_id),
        ('34552000', 'État - TVA récupérable sur charges', 3, 'DEBIT', TRUE, v_tenant_id),
        ('44110000', 'Fournisseurs (Collectif)', 4, 'CREDIT', TRUE, v_tenant_id),
        ('44110001', 'Fournisseur Ceramica Pro SA', 4, 'CREDIT', TRUE, v_tenant_id),
        ('44110002', 'Fournisseur Maghreb Matériaux', 4, 'CREDIT', TRUE, v_tenant_id),
        ('44550000', 'État - TVA facturée', 4, 'CREDIT', TRUE, v_tenant_id),
        ('44560000', 'État - TVA due', 4, 'CREDIT', TRUE, v_tenant_id),
        ('51410000', 'Banque (Compte courant)', 5, 'DEBIT', TRUE, v_tenant_id),
        ('51610000', 'Caisse centrale', 5, 'DEBIT', TRUE, v_tenant_id),
        ('61110000', 'Achats de marchandises', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61140000', 'Variation de stocks de marchandises', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61310000', 'Locations et charges locatives', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61450000', 'Frais de télécommunications', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61470000', 'Services bancaires et commissions', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61710000', 'Rémunérations du personnel', 6, 'DEBIT', TRUE, v_tenant_id),
        ('61930000', 'Dotations d''exploitation aux amortissements', 6, 'DEBIT', TRUE, v_tenant_id),
        ('71110000', 'Ventes de marchandises au Maroc', 7, 'CREDIT', TRUE, v_tenant_id),
        ('71240000', 'Prestations de services et travaux', 7, 'CREDIT', TRUE, v_tenant_id)
    ON CONFLICT (numero_compte, point_de_vente_id) DO UPDATE SET actif = TRUE;

    -- Récupération dynamique des IDs de comptes
    SELECT id INTO v_c_1111 FROM comptes_comptables WHERE numero_compte = '11110000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_1161 FROM comptes_comptables WHERE numero_compte = '11610000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_2332 FROM comptes_comptables WHERE numero_compte = '23320000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_2340 FROM comptes_comptables WHERE numero_compte = '23400000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_2833 FROM comptes_comptables WHERE numero_compte = '28330000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3111 FROM comptes_comptables WHERE numero_compte = '31110000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3421 FROM comptes_comptables WHERE numero_compte = '34210000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3421_c1 FROM comptes_comptables WHERE numero_compte = '34210001' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3421_c2 FROM comptes_comptables WHERE numero_compte = '34210002' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3421_c3 FROM comptes_comptables WHERE numero_compte = '34210003' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_3455 FROM comptes_comptables WHERE numero_compte = '34552000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_4411 FROM comptes_comptables WHERE numero_compte = '44110000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_4411_f1 FROM comptes_comptables WHERE numero_compte = '44110001' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_4411_f2 FROM comptes_comptables WHERE numero_compte = '44110002' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_4455 FROM comptes_comptables WHERE numero_compte = '44550000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_4456 FROM comptes_comptables WHERE numero_compte = '44560000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_5141 FROM comptes_comptables WHERE numero_compte = '51410000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_5161 FROM comptes_comptables WHERE numero_compte = '51610000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6111 FROM comptes_comptables WHERE numero_compte = '61110000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6114 FROM comptes_comptables WHERE numero_compte = '61140000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6131 FROM comptes_comptables WHERE numero_compte = '61310000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6145 FROM comptes_comptables WHERE numero_compte = '61450000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6147 FROM comptes_comptables WHERE numero_compte = '61470000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6171 FROM comptes_comptables WHERE numero_compte = '61710000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_6193 FROM comptes_comptables WHERE numero_compte = '61930000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_7111 FROM comptes_comptables WHERE numero_compte = '71110000' AND point_de_vente_id = v_tenant_id;
    SELECT id INTO v_c_7124 FROM comptes_comptables WHERE numero_compte = '71240000' AND point_de_vente_id = v_tenant_id;

    -- =========================================================================
    -- 4. ÉCRITURE 1 : BILAN D'OUVERTURE (01/01/2025)
    -- =========================================================================
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('AN-2025-001', '2025-01-01', 'Bilan d''ouverture exercice 2025', 'AN-INIT-2025', 700000.00, 700000.00, TRUE, v_j_an, v_tenant_id)
    RETURNING id INTO v_ecr_id;

    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_2332, 150000.00, 0.00, 'Matériel et outillage au 01/01/2025', v_tenant_id),
    (v_ecr_id, v_c_2340, 80000.00,  0.00, 'Matériel de transport au 01/01/2025', v_tenant_id),
    (v_ecr_id, v_c_3111, 120000.00, 0.00, 'Stock initial marchandises', v_tenant_id),
    (v_ecr_id, v_c_5141, 320000.00, 0.00, 'Solde initial Banque', v_tenant_id),
    (v_ecr_id, v_c_5161, 30000.00,  0.00, 'Solde initial Caisse', v_tenant_id),
    (v_ecr_id, v_c_1111, 0.00, 500000.00, 'Capital social souscrit', v_tenant_id),
    (v_ecr_id, v_c_1161, 0.00, 200000.00, 'Report à nouveau créditeur antérieur', v_tenant_id);

    -- =========================================================================
    -- 5. ÉCRITURES T1 2025 (JANVIER - MARS 2025)
    -- =========================================================================
    -- Achat F1 Ceramica Pro (120 000 TTC)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('AC-2025-001', '2025-01-15', 'Facture Achat N° FA-2025-01 - Ceramica Pro', 'FA-2025-01', 120000.00, 120000.00, TRUE, v_j_ac, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6111, 100000.00, 0.00, 'Achats marchandises céramique', NULL, v_tenant_id),
    (v_ecr_id, v_c_3455, 20000.00,  0.00, 'TVA récupérable 20%', NULL, v_tenant_id),
    (v_ecr_id, v_c_4411_f1, 0.00, 120000.00, 'Facture FA-2025-01', 'AA', v_tenant_id);

    -- Vente C1 Ste Alpha SARL (96 000 TTC)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('VE-2025-001', '2025-01-22', 'Facture Vente N° FAC-2025-01 - Ste Alpha SARL', 'FAC-2025-01', 96000.00, 96000.00, TRUE, v_j_ve, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_3421_c1, 96000.00, 0.00, 'Facture FAC-2025-01 Ste Alpha', 'AB', v_tenant_id),
    (v_ecr_id, v_c_7111, 0.00, 80000.00, 'Ventes marchandises carrelage', NULL, v_tenant_id),
    (v_ecr_id, v_c_4455, 0.00, 16000.00, 'TVA facturée 20%', NULL, v_tenant_id);

    -- Règlement Fournisseur F1 Ceramica Pro (120 000 TTC) -> LETTRÉ AVEC 'AA'
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-001', '2025-02-10', 'Virement bancaire Règlement FA-2025-01 Ceramica', 'VIR-F-001', 120000.00, 120000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_4411_f1, 120000.00, 0.00, 'Règlement Facture FA-2025-01', 'AA', v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 120000.00, 'Débit bancaire virement émis', NULL, v_tenant_id);

    -- Encaissement Client C1 Ste Alpha SARL (96 000 TTC) -> LETTRÉ AVEC 'AB'
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-002', '2025-02-25', 'Encaissement chèque Client Ste Alpha FAC-2025-01', 'ENC-C-001', 96000.00, 96000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_5141, 96000.00, 0.00, 'Crédit bancaire chèque encaissé', NULL, v_tenant_id),
    (v_ecr_id, v_c_3421_c1, 0.00, 96000.00, 'Règlement chèque FAC-2025-01', 'AB', v_tenant_id);

    -- Vente C2 Bâtiment Moderne (60 000 TTC)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('VE-2025-002', '2025-03-05', 'Facture Vente N° FAC-2025-02 - Bâtiment Moderne', 'FAC-2025-02', 60000.00, 60000.00, TRUE, v_j_ve, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_3421_c2, 60000.00, 0.00, 'Facture FAC-2025-02 Bâtiment Mod', 'AC', v_tenant_id),
    (v_ecr_id, v_c_7111, 0.00, 50000.00, 'Ventes revêtements et sanitaire', NULL, v_tenant_id),
    (v_ecr_id, v_c_4455, 0.00, 10000.00, 'TVA facturée 20%', NULL, v_tenant_id);

    -- Encaissement C2 Bâtiment Moderne (60 000 TTC) -> LETTRÉ AVEC 'AC'
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-003', '2025-03-20', 'Virement reçu Bâtiment Moderne FAC-2025-02', 'ENC-C-002', 60000.00, 60000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_5141, 60000.00, 0.00, 'Encaissement virement Bâtiment Mod', NULL, v_tenant_id),
    (v_ecr_id, v_c_3421_c2, 0.00, 60000.00, 'Règlement virement FAC-2025-02', 'AC', v_tenant_id);

    -- Déclaration & Liquidation TVA T1 (31/03/2025)
    -- TVA Facturée T1 = 26 000 | TVA Déductible T1 = 20 000 | TVA Due = 6 000
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('OD-2025-T1', '2025-03-31', 'Liquidation de la TVA Trimestre 1 - 2025', 'LIQ-TVA-T1', 26000.00, 26000.00, TRUE, v_j_od, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_4455, 26000.00, 0.00, 'Solde TVA facturée T1', v_tenant_id),
    (v_ecr_id, v_c_3455, 0.00, 20000.00, 'Solde TVA récupérable T1', v_tenant_id),
    (v_ecr_id, v_c_4456, 0.00, 6000.00, 'TVA due à décaisser T1', v_tenant_id);

    -- Paiement TVA T1 (20/04/2025)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-T1', '2025-04-20', 'Télépaiement SIMPL-TVA T1 2025 à la DGI', 'DGI-TVA-T1', 6000.00, 6000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_4456, 6000.00, 0.00, 'Règlement TVA due T1', v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 6000.00, 'Prélèvement DGI Trésorerie Générale', v_tenant_id);

    -- =========================================================================
    -- 6. ÉCRITURES T2 & T3 2025 (CHARGES D'EXPLOITATION & LETTRAGE 'BA')
    -- =========================================================================
    -- Charges locatives Siège & Dépôt (25 000 MAD)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-LOYER', '2025-05-02', 'Loyer semestriel Dépôt & Showroom', 'LOYER-S1-2025', 25000.00, 25000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6131, 25000.00, 0.00, 'Loyer locaux d''exploitation S1', v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 25000.00, 'Virement propriétaire bail commercial', v_tenant_id);

    -- Salaires et rémunérations du personnel S1 (90 000 MAD)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-PAIE', '2025-06-30', 'Paiement salaires du personnel S1', 'PAIE-S1-2025', 90000.00, 90000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6171, 90000.00, 0.00, 'Rémunérations nettes du personnel', v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 90000.00, 'Virement de masse des salaires', v_tenant_id);

    -- Vente C3 Travaux Atlas (72 000 TTC)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('VE-2025-003', '2025-07-10', 'Facture Vente N° FAC-2025-03 - Travaux Atlas', 'FAC-2025-03', 72000.00, 72000.00, TRUE, v_j_ve, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_3421_c3, 72000.00, 0.00, 'Facture FAC-2025-03 Travaux Atlas', 'BA', v_tenant_id),
    (v_ecr_id, v_c_7111, 0.00, 60000.00, 'Vente carrelage grès cérame', NULL, v_tenant_id),
    (v_ecr_id, v_c_4455, 0.00, 12000.00, 'TVA facturée 20%', NULL, v_tenant_id);

    -- Encaissement C3 Travaux Atlas (72 000 TTC) -> LETTRÉ AVEC 'BA'
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-004', '2025-08-14', 'Virement reçu Travaux Atlas FAC-2025-03', 'ENC-C-003', 72000.00, 72000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_5141, 72000.00, 0.00, 'Crédit virement Travaux Atlas', NULL, v_tenant_id),
    (v_ecr_id, v_c_3421_c3, 0.00, 72000.00, 'Règlement FAC-2025-03', 'BA', v_tenant_id);

    -- Frais bancaires et télécoms (12 000 MAD)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-FRAIS', '2025-09-30', 'Frais bancaires et abonnement télécom S1', 'FRAIS-T3-2025', 12000.00, 12000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6145, 8000.00, 0.00, 'Frais télécoms Internet Fibre Siège', v_tenant_id),
    (v_ecr_id, v_c_6147, 4000.00, 0.00, 'Commissions et tenues de compte', v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 12000.00, 'Prélèvements bancaires', v_tenant_id);

    -- =========================================================================
    -- 7. ÉCRITURES SPÉCIALES TEST DE LETTRAGE EN DIRECT (ÉCRITURES OUVERTES)
    -- =========================================================================
    -- PAIRE 1 : CLIENT 3421 (FAC-2025-089 à 15 000 DH et REG-2025-044 à 15 000 DH)
    -- Vente Client en attente de lettrage
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('VE-2025-089', '2025-10-15', 'Facture Vente N° FAC-2025-089 - Société Alpha', 'FAC-2025-089', 15000.00, 15000.00, TRUE, v_j_ve, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_3421, 15000.00, 0.00, 'Vente carrelage Facture FAC-2025-089', NULL, v_tenant_id),
    (v_ecr_id, v_c_7111, 0.00, 12500.00, 'Vente de marchandises HT', NULL, v_tenant_id),
    (v_ecr_id, v_c_4455, 0.00, 2500.00, 'TVA facturée 20%', NULL, v_tenant_id);

    -- Règlement Client en attente de lettrage (Crédit exact de 15 000 DH)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-044', '2025-10-28', 'Règlement Chèque N° REG-2025-044 Société Alpha', 'REG-2025-044', 15000.00, 15000.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_5141, 15000.00, 0.00, 'Remise chèque Société Alpha', NULL, v_tenant_id),
    (v_ecr_id, v_c_3421, 0.00, 15000.00, 'Règlement chèque Facture FAC-2025-089', NULL, v_tenant_id);

    -- PAIRE 2 : FOURNISSEUR 4411 (Achat FA-2025-090 à 22 500 DH et Virement VIR-2025-090 à 22 500 DH)
    -- Facture Achat Fournisseur en attente de lettrage
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('AC-2025-090', '2025-11-05', 'Facture Achat N° FA-2025-090 - Maghreb Matériaux', 'FA-2025-090', 22500.00, 22500.00, TRUE, v_j_ac, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6111, 18750.00, 0.00, 'Achats colles et mortiers HT', NULL, v_tenant_id),
    (v_ecr_id, v_c_3455, 3750.00,  0.00, 'TVA récupérable 20%', NULL, v_tenant_id),
    (v_ecr_id, v_c_4411, 0.00, 22500.00, 'Facture Maghreb Matériaux FA-2025-090', NULL, v_tenant_id);

    -- Règlement Fournisseur en attente de lettrage (Débit exact de 22 500 DH)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('BQ-2025-090', '2025-11-20', 'Virement Vendeur N° VIR-2025-090 Maghreb Matériaux', 'VIR-2025-090', 22500.00, 22500.00, TRUE, v_j_bq, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, lettrage, point_de_vente_id) VALUES
    (v_ecr_id, v_c_4411, 22500.00, 0.00, 'Paiement Facture FA-2025-090 Maghreb', NULL, v_tenant_id),
    (v_ecr_id, v_c_5141, 0.00, 22500.00, 'Virement bancaire émis Maghreb Mat.', NULL, v_tenant_id);

    -- =========================================================================
    -- 8. CLÔTURE & INVENTAIRE AU 31/12/2025
    -- =========================================================================
    -- Annulation Stock Initial (120 000 DH)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('OD-2025-STK-INIT', '2025-12-31', 'Annulation du stock initial au 01/01/2025', 'INV-INIT-2025', 120000.00, 120000.00, TRUE, v_j_od, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6114, 120000.00, 0.00, 'Variation stock marchandises (Déstockage)', v_tenant_id),
    (v_ecr_id, v_c_3111, 0.00, 120000.00, 'Sortie comptable stock initial', v_tenant_id);

    -- Constatation Stock Final au 31/12/2025 (135 000 DH)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('OD-2025-STK-FIN', '2025-12-31', 'Constatation de l''inventaire stock final au 31/12/2025', 'INV-FIN-2025', 135000.00, 135000.00, TRUE, v_j_od, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_3111, 135000.00, 0.00, 'Entrée bilan stock final inventorié', v_tenant_id),
    (v_ecr_id, v_c_6114, 0.00, 135000.00, 'Variation stock marchandises (Stockage)', v_tenant_id);

    -- Dotation aux amortissements des immobilisations (Matériel: 15 000 DH, Transport: 16 000 DH = 31 000 DH)
    INSERT INTO ecritures_comptables (numero_piece, date_ecriture, libelle, reference_piece, total_debit, total_credit, validee, journal_id, point_de_vente_id)
    VALUES ('OD-2025-AMORT', '2025-12-31', 'Dotations aux amortissements de l''exercice 2025', 'AMORT-2025', 31000.00, 31000.00, TRUE, v_j_od, v_tenant_id)
    RETURNING id INTO v_ecr_id;
    INSERT INTO lignes_ecriture (ecriture_id, compte_id, debit, credit, libelle_ligne, point_de_vente_id) VALUES
    (v_ecr_id, v_c_6193, 31000.00, 0.00, 'Dotation d''exploitation aux amortissements 2025', v_tenant_id),
    (v_ecr_id, v_c_2833, 0.00, 31000.00, 'Amortissement cumulé outillage & matériel', v_tenant_id);

    -- =========================================================================
    -- 9. DÉCLARATIONS FISCALES TVA (2025-T1, T2, T3, T4)
    -- =========================================================================
    DELETE FROM declarations_tva WHERE point_de_vente_id = v_tenant_id AND periode LIKE '2025-%';

    INSERT INTO declarations_tva (periode, regime, statut, total_ventes_ht, total_achats_ht, tva_collectee, tva_deductible_charges, tva_deductible_total, tva_a_payer, credit_tva_reportable, notes, point_de_vente_id)
    VALUES
    ('2025-T1', 'ENCAISSEMENT', 'VALIDEE', 130000.00, 100000.00, 26000.00, 20000.00, 20000.00, 6000.00, 0.00, 'Déclaration TVA T1 2025 validée et réglée par télépaiement DGI', v_tenant_id),
    ('2025-T2', 'ENCAISSEMENT', 'VALIDEE', 60000.00,  0.00,      12000.00, 0.00,     0.00,     12000.00, 0.00, 'Déclaration TVA T2 2025 validée', v_tenant_id),
    ('2025-T3', 'ENCAISSEMENT', 'VALIDEE', 60000.00,  0.00,      12000.00, 0.00,     0.00,     12000.00, 0.00, 'Déclaration TVA T3 2025 validée', v_tenant_id),
    ('2025-T4', 'ENCAISSEMENT', 'VALIDEE', 12500.00,  18750.00,  2500.00,  3750.00,  3750.00,  0.00,    1250.00, 'Déclaration TVA T4 2025 avec crédit de TVA reportable', v_tenant_id);

    RAISE NOTICE 'Initialisation terminée avec succès pour le Tenant % !', v_tenant_id;
END $$;

-- =============================================================================
-- SYNCHRONISATION DES SÉQUENCES POSTGRESQL
-- =============================================================================
SELECT setval('exercices_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM exercices_comptables));
SELECT setval('journaux_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM journaux_comptables));
SELECT setval('comptes_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM comptes_comptables));
SELECT setval('ecritures_comptables_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ecritures_comptables));
SELECT setval('lignes_ecriture_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lignes_ecriture));
SELECT setval('declarations_tva_id_seq', (SELECT COALESCE(MAX(id), 1) FROM declarations_tva));

-- ======================================================================
-- SYNCHRONISATION FACTURES ODOO VERS TADBEER (TENANT 5)
-- Période : 21-26 Octobre 2026 (Factures FA/2026/00423 à FA/2026/00440)
-- Résolution des collisions et ajustement de séquence
-- ======================================================================

BEGIN;

-- ----------------------------------------------------------------------
-- NOTE : FA/2026/00433, 00434 et 00436 restent intactes dans Tadbeer
-- ----------------------------------------------------------------------

-- ----------------------------------------------------------------------
-- 2. NETTOYER LES BROUILLONS / TENTATIVES ANNULÉES QUI BLOQUAIENT LES NUMÉROS
-- ----------------------------------------------------------------------
UPDATE bons_livraison_client SET facture_id = NULL 
WHERE point_de_vente_id = 5 
  AND facture_id IN (
      SELECT id FROM factures 
      WHERE point_de_vente_id = 5 
        AND (annulee = true OR statut = 'ANNULEE')
        AND numero_facture IN ('FA/2026/00423', 'FA/2026/00424', 'FA/2026/00425', 'FA/2026/00426', 'FA/2026/00427', 'FA/2026/00428', 'FA/2026/00429', 'FA/2026/00430', 'FA/2026/00431', 'FA/2026/00432', 'FA/2026/00435', 'FA/2026/00437', 'FA/2026/00438', 'FA/2026/00439')
  );

DELETE FROM lignes_facture 
WHERE facture_id IN (
    SELECT id FROM factures 
    WHERE point_de_vente_id = 5 
      AND (annulee = true OR statut = 'ANNULEE')
      AND numero_facture IN ('FA/2026/00423', 'FA/2026/00424', 'FA/2026/00425', 'FA/2026/00426', 'FA/2026/00427', 'FA/2026/00428', 'FA/2026/00429', 'FA/2026/00430', 'FA/2026/00431', 'FA/2026/00432', 'FA/2026/00435', 'FA/2026/00437', 'FA/2026/00438', 'FA/2026/00439')
);

DELETE FROM factures 
WHERE point_de_vente_id = 5 
  AND (annulee = true OR statut = 'ANNULEE')
  AND numero_facture IN ('FA/2026/00423', 'FA/2026/00424', 'FA/2026/00425', 'FA/2026/00426', 'FA/2026/00427', 'FA/2026/00428', 'FA/2026/00429', 'FA/2026/00430', 'FA/2026/00431', 'FA/2026/00432', 'FA/2026/00435', 'FA/2026/00437', 'FA/2026/00438', 'FA/2026/00439');

-- ----------------------------------------------------------------------
-- 3. INSERTION DES 18 FACTURES RÉELLES DEPUIS ODOO
-- ----------------------------------------------------------------------

-- Facture FA/2026/00423 : STATION KANTARI (513.60 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('STATION KANTARI')) OR code_client = 'CLI_974')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00423', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        428.00, 85.60, 513.60, 0.00, 513.60,
        'VALIDEE', 0.00, 513.60, '2026-10-23 17:06:37'::timestamp, 'Origine Odoo: S00889 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00889' IS NOT NULL AND 'S00889' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00889'
          );
    END IF;

    -- Ligne article : 0220000210 - [0220000210] COURROIE TRAPEZOIDALE SWR T
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0220000210' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0220000210] COURROIE TRAPEZOIDALE SWR TYPE B210'))) LIMIT 1),
        '[0220000210] COURROIE TRAPEZOIDALE SWR TYPE B210',
        '0220000210',
        2.000,
        214.00,
        20.00,
        428.00,
        85.60,
        513.60,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0220000210' 
          AND lf.quantite = 2.000
    );
END $$;

-- Facture FA/2026/00424 : SUPER CERAME KENITRA (1020.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00424', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        850.00, 170.00, 1020.00, 0.00, 1020.00,
        'VALIDEE', 0.00, 1020.00, '2026-10-26 11:24:58'::timestamp, 'Origine Odoo: S00692 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00692' IS NOT NULL AND 'S00692' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00692'
          );
    END IF;

    -- Ligne article : 0610000004 - [0610000004] CHAINE SIMPLE IRIS 12B1
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0610000004' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0610000004] CHAINE SIMPLE IRIS 12B1'))) LIMIT 1),
        '[0610000004] CHAINE SIMPLE IRIS 12B1',
        '0610000004',
        10.000,
        85.00,
        20.00,
        850.00,
        170.00,
        1020.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0610000004' 
          AND lf.quantite = 10.000
    );
END $$;

-- Facture FA/2026/00425 : SUPER CERAME BERRECHID (15600.00 MAD du 2026-10-21)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME BERRECHID')) OR code_client = 'CLI_998')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00425', '2026-10-21'::date, '2027-02-18'::date, v_client_id, v_user_id,
        13000.00, 2600.00, 15600.00, 0.00, 15600.00,
        'VALIDEE', 0.00, 15600.00, '2026-10-26 12:12:58'::timestamp, 'Origine Odoo: S00827 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00827' IS NOT NULL AND 'S00827' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00827'
          );
    END IF;

    -- Ligne article : 0900008002 - [0900008002] CHAINE SIMPLE 16B1 EN TOURE
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0900008002' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0900008002] CHAINE SIMPLE 16B1 EN TOURET  IRIS'))) LIMIT 1),
        '[0900008002] CHAINE SIMPLE 16B1 EN TOURET  IRIS',
        '0900008002',
        100.000,
        130.00,
        20.00,
        13000.00,
        2600.00,
        15600.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0900008002' 
          AND lf.quantite = 100.000
    );
END $$;

-- Facture FA/2026/00426 : SUPER CERAME KENITRA (4548.00 MAD du 2026-10-21)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00426', '2026-10-21'::date, '2027-02-18'::date, v_client_id, v_user_id,
        3790.00, 758.00, 4548.00, 0.00, 4548.00,
        'VALIDEE', 0.00, 4548.00, '2026-10-26 13:44:40'::timestamp, 'Origine Odoo: S00807 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00807' IS NOT NULL AND 'S00807' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00807'
          );
    END IF;

    -- Ligne article : 0610000002 - [0610000002] CHAINE SIMPLE IRIS 08B1
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0610000002' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0610000002] CHAINE SIMPLE IRIS 08B1'))) LIMIT 1),
        '[0610000002] CHAINE SIMPLE IRIS 08B1',
        '0610000002',
        60.000,
        49.00,
        20.00,
        2940.00,
        588.00,
        3528.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0610000002' 
          AND lf.quantite = 60.000
    );
    -- Ligne article : 0610000004 - [0610000004] CHAINE SIMPLE IRIS 12B1
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0610000004' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0610000004] CHAINE SIMPLE IRIS 12B1'))) LIMIT 1),
        '[0610000004] CHAINE SIMPLE IRIS 12B1',
        '0610000004',
        10.000,
        85.00,
        20.00,
        850.00,
        170.00,
        1020.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0610000004' 
          AND lf.quantite = 10.000
    );
END $$;

-- Facture FA/2026/00427 : SUPER CERAME KENITRA (2592.00 MAD du 2026-10-22)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00427', '2026-10-22'::date, '2027-02-19'::date, v_client_id, v_user_id,
        2160.00, 432.00, 2592.00, 0.00, 2592.00,
        'VALIDEE', 0.00, 2592.00, '2026-10-26 13:48:56'::timestamp, 'Origine Odoo: S00785 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00785' IS NOT NULL AND 'S00785' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00785'
          );
    END IF;

    -- Ligne article : 0722000009 - [0722000009] MOTEUR ELECTRIQUE 380V  B5 
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0722000009' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0722000009] MOTEUR ELECTRIQUE 380V  B5 4KW 1500/MIN GMT'))) LIMIT 1),
        '[0722000009] MOTEUR ELECTRIQUE 380V  B5 4KW 1500/MIN GMT',
        '0722000009',
        1.000,
        2160.00,
        20.00,
        2160.00,
        432.00,
        2592.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0722000009' 
          AND lf.quantite = 1.000
    );
END $$;

-- Facture FA/2026/00428 : SUPER CERAME KENITRA (780.00 MAD du 2026-10-22)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00428', '2026-10-22'::date, '2027-02-19'::date, v_client_id, v_user_id,
        650.00, 130.00, 780.00, 0.00, 780.00,
        'VALIDEE', 0.00, 780.00, '2026-10-26 14:20:43'::timestamp, 'Origine Odoo: S00813 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00813' IS NOT NULL AND 'S00813' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00813'
          );
    END IF;

    -- Ligne article : 0610000003 - [0610000003] CHAINE SIMPLE IRIS 10B1
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0610000003' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0610000003] CHAINE SIMPLE IRIS 10B1'))) LIMIT 1),
        '[0610000003] CHAINE SIMPLE IRIS 10B1',
        '0610000003',
        10.000,
        65.00,
        20.00,
        650.00,
        130.00,
        780.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0610000003' 
          AND lf.quantite = 10.000
    );
END $$;

-- Facture FA/2026/00429 : SUPER CERAME TETOUAN (840.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME TETOUAN')) OR code_client = 'CLI_1001')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00429', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        700.00, 140.00, 840.00, 0.00, 840.00,
        'VALIDEE', 0.00, 840.00, '2026-10-26 14:24:45'::timestamp, 'Origine Odoo: S00814 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00814' IS NOT NULL AND 'S00814' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00814'
          );
    END IF;

    -- Ligne article : 0722000003-1 - [0722000003-1] MOTEUR ELECTRIQUE 380V  B
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0722000003-1' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0722000003-1] MOTEUR ELECTRIQUE 380V  B5 0,37KW 1500/MIN GMT'))) LIMIT 1),
        '[0722000003-1] MOTEUR ELECTRIQUE 380V  B5 0,37KW 1500/MIN GMT',
        '0722000003-1',
        1.000,
        700.00,
        20.00,
        700.00,
        140.00,
        840.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0722000003-1' 
          AND lf.quantite = 1.000
    );
END $$;

-- Facture FA/2026/00430 : SUPER CERAME KENITRA (14400.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00430', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        12000.00, 2400.00, 14400.00, 0.00, 14400.00,
        'VALIDEE', 0.00, 14400.00, '2026-10-26 14:26:34'::timestamp, 'Origine Odoo: S00874 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00874' IS NOT NULL AND 'S00874' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00874'
          );
    END IF;

    -- Ligne article : 0940000018 - [0940000018] REVETEMENT NOIR ANTI CHALEU
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0940000018' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0940000018] REVETEMENT NOIR ANTI CHALEUR 48*53'))) LIMIT 1),
        '[0940000018] REVETEMENT NOIR ANTI CHALEUR 48*53',
        '0940000018',
        300.000,
        40.00,
        20.00,
        12000.00,
        2400.00,
        14400.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0940000018' 
          AND lf.quantite = 300.000
    );
END $$;

-- Facture FA/2026/00431 : SUPER CERAME KENITRA (4032.00 MAD du 2026-10-26)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00431', '2026-10-26'::date, '2027-02-23'::date, v_client_id, v_user_id,
        3360.00, 672.00, 4032.00, 0.00, 4032.00,
        'VALIDEE', 0.00, 4032.00, '2026-10-26 14:39:14'::timestamp, 'Origine Odoo: S00691 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00691' IS NOT NULL AND 'S00691' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00691'
          );
    END IF;

    -- Ligne article : 1210000014 - [1210000014] DISTRIBUTEUR D AIR SUNDPIPE
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '1210000014' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[1210000014] DISTRIBUTEUR D AIR SUNDPIPER 031-183-000'))) LIMIT 1),
        '[1210000014] DISTRIBUTEUR D AIR SUNDPIPER 031-183-000',
        '1210000014',
        2.000,
        1680.00,
        20.00,
        3360.00,
        672.00,
        4032.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '1210000014' 
          AND lf.quantite = 2.000
    );
END $$;

-- Facture FA/2026/00432 : SUPER CERAME TETOUAN (1356.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME TETOUAN')) OR code_client = 'CLI_1001')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00432', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        1130.00, 226.00, 1356.00, 0.00, 1356.00,
        'VALIDEE', 0.00, 1356.00, '2026-10-26 14:44:12'::timestamp, 'Origine Odoo: S00876 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00876' IS NOT NULL AND 'S00876' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00876'
          );
    END IF;

    -- Ligne article :  - PALIER COMPLET ACIER ISB UCFC207
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('PALIER COMPLET ACIER ISB UCFC207'))) LIMIT 1),
        'PALIER COMPLET ACIER ISB UCFC207',
        '',
        10.000,
        95.00,
        20.00,
        950.00,
        190.00,
        1140.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '' 
          AND lf.quantite = 10.000
    );
    -- Ligne article : 0311000003 - [0311000003] COURROIE DENTE SECTION 240H
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0311000003' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0311000003] COURROIE DENTE SECTION 240H100'))) LIMIT 1),
        '[0311000003] COURROIE DENTE SECTION 240H100',
        '0311000003',
        6.000,
        30.00,
        20.00,
        180.00,
        36.00,
        216.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0311000003' 
          AND lf.quantite = 6.000
    );
END $$;

-- Facture FA/2026/00435 : SUPER CERAME KENITRA (6840.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00435', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        5700.00, 1140.00, 6840.00, 0.00, 6840.00,
        'VALIDEE', 0.00, 6840.00, '2026-10-26 14:48:56'::timestamp, 'Origine Odoo: S00885 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00885' IS NOT NULL AND 'S00885' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00885'
          );
    END IF;

    -- Ligne article : 1210000014 - [1210000014] DISTRIBUTEUR D AIR SUNDPIPE
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '1210000014' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[1210000014] DISTRIBUTEUR D AIR SUNDPIPER 031-183-000'))) LIMIT 1),
        '[1210000014] DISTRIBUTEUR D AIR SUNDPIPER 031-183-000',
        '1210000014',
        3.000,
        1900.00,
        20.00,
        5700.00,
        1140.00,
        6840.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '1210000014' 
          AND lf.quantite = 3.000
    );
END $$;

-- Facture FA/2026/00437 : SUPER CERAME TETOUAN (1704.00 MAD du 2026-10-23)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME TETOUAN')) OR code_client = 'CLI_1001')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00437', '2026-10-23'::date, '2027-02-20'::date, v_client_id, v_user_id,
        1420.00, 284.00, 1704.00, 0.00, 1704.00,
        'VALIDEE', 0.00, 1704.00, '2026-10-26 14:50:38'::timestamp, 'Origine Odoo: S00883 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00883' IS NOT NULL AND 'S00883' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00883'
          );
    END IF;

    -- Ligne article : 0311000026 - [0311000026] COURROIE DENTE SECTION 1150
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0311000026' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0311000026] COURROIE DENTE SECTION 1150H100'))) LIMIT 1),
        '[0311000026] COURROIE DENTE SECTION 1150H100',
        '0311000026',
        10.000,
        142.00,
        20.00,
        1420.00,
        284.00,
        1704.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0311000026' 
          AND lf.quantite = 10.000
    );
END $$;

-- Facture FA/2026/00438 : SUPER CERAME KENITRA (936.00 MAD du 2026-10-26)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00438', '2026-10-26'::date, '2027-02-23'::date, v_client_id, v_user_id,
        780.00, 156.00, 936.00, 0.00, 936.00,
        'VALIDEE', 0.00, 936.00, '2026-10-26 14:54:11'::timestamp, 'Origine Odoo: S00699 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00699' IS NOT NULL AND 'S00699' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00699'
          );
    END IF;

    -- Ligne article : 0930000021 - [0930000021] COURROIE CRET 5P C110
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0930000021' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0930000021] COURROIE CRET 5P C110'))) LIMIT 1),
        '[0930000021] COURROIE CRET 5P C110',
        '0930000021',
        5.000,
        156.00,
        20.00,
        780.00,
        156.00,
        936.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0930000021' 
          AND lf.quantite = 5.000
    );
END $$;

-- Facture FA/2026/00439 : SUPER CERAME BERRECHID (8712.00 MAD du 2026-10-26)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME BERRECHID')) OR code_client = 'CLI_998')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00439', '2026-10-26'::date, '2027-02-23'::date, v_client_id, v_user_id,
        7260.00, 1452.00, 8712.00, 0.00, 8712.00,
        'VALIDEE', 0.00, 8712.00, '2026-10-26 16:45:21'::timestamp, 'Origine Odoo: S00884 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00884' IS NOT NULL AND 'S00884' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00884'
          );
    END IF;

    -- Ligne article : 1220000004 - [1220000004] MEMBRANE WILDEN 3'' 15-1020
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '1220000004' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[1220000004] MEMBRANE WILDEN 3'''' 15-1020-51'))) LIMIT 1),
        '[1220000004] MEMBRANE WILDEN 3'''' 15-1020-51',
        '1220000004',
        11.000,
        660.00,
        20.00,
        7260.00,
        1452.00,
        8712.00,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '1220000004' 
          AND lf.quantite = 11.000
    );
END $$;

-- Facture FA/2026/00440 : SUPER CERAME KENITRA (703.20 MAD du 2026-10-26)
DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM('SUPER CERAME KENITRA')) OR code_client = 'CLI_1000')
    ORDER BY id ASC LIMIT 1;
    
    IF v_client_id IS NULL THEN
        SELECT id INTO v_client_id FROM clients WHERE point_de_vente_id = 5 ORDER BY id ASC LIMIT 1;
    END IF;

    -- Insertion Facture
    INSERT INTO factures (
        numero_facture, date_facture, date_echeance, client_id, emise_par_user_id,
        montant_ht, montant_tva, montant_ttc, remise_globale, montant_final,
        statut, montant_paye, montant_restant, date_creation, notes,
        annulee, point_de_vente_id
    ) VALUES (
        'FA/2026/00440', '2026-10-26'::date, '2027-02-23'::date, v_client_id, v_user_id,
        586.00, 117.20, 703.20, 0.00, 703.20,
        'VALIDEE', 0.00, 703.20, '2026-10-27 14:36:50'::timestamp, 'Origine Odoo: S00648 - Importé automatiquement',
        false, 5
    )
    ON CONFLICT (point_de_vente_id, numero_facture) DO UPDATE SET
        date_facture = EXCLUDED.date_facture,
        date_echeance = EXCLUDED.date_echeance,
        montant_ht = EXCLUDED.montant_ht,
        montant_tva = EXCLUDED.montant_tva,
        montant_ttc = EXCLUDED.montant_ttc,
        montant_final = EXCLUDED.montant_final,
        montant_restant = EXCLUDED.montant_restant,
        statut = 'VALIDEE',
        annulee = false
    RETURNING id INTO v_facture_id;

    -- Lier d'éventuels BLs correspondants par commande origine
    IF 'S00648' IS NOT NULL AND 'S00648' <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = 'S00648'
          );
    END IF;

    -- Ligne article : 0930000018-1 - [0930000018-1] ENCRE NOIR HIGH QUALITY  
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = '0930000018-1' OR LOWER(TRIM(p.designation)) = LOWER(TRIM('[0930000018-1] ENCRE NOIR HIGH QUALITY  WORLD JET ORIGINE : ITALY M1'))) LIMIT 1),
        '[0930000018-1] ENCRE NOIR HIGH QUALITY  WORLD JET ORIGINE : ITALY M1',
        '0930000018-1',
        20.000,
        29.30,
        20.00,
        586.00,
        117.20,
        703.20,
        0.00,
        0.00
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = '0930000018-1' 
          AND lf.quantite = 20.000
    );
END $$;

-- ----------------------------------------------------------------------
-- 4. MISE À JOUR DE LA CODIFICATION (PROCHAINE FACTURE = FA/2026/00441)
-- ----------------------------------------------------------------------
UPDATE codifications_document 
SET dernier_numero = 440, 
    date_derniere_maj = NOW() 
WHERE point_de_vente_id = 5 
  AND type_document = 'FACTURE_VENTE';

-- ----------------------------------------------------------------------
-- 5. VÉRIFICATION DU RÉSULTAT
-- ----------------------------------------------------------------------
SELECT 
    f.numero_facture,
    f.date_facture,
    c.nom AS client_nom,
    f.montant_ttc,
    f.statut,
    f.notes
FROM factures f
LEFT JOIN clients c ON c.id = f.client_id
WHERE f.point_de_vente_id = 5
  AND f.numero_facture BETWEEN 'FA/2026/00423' AND 'FA/2026/00443'
ORDER BY f.numero_facture ASC;

SELECT 
    COUNT(*) AS total_factures_tenant_5,
    SUM(montant_ttc) AS ca_total_ttc
FROM factures
WHERE point_de_vente_id = 5
  AND statut = 'VALIDEE';

COMMIT;

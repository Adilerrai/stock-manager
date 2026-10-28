-- ======================================================================
-- SCRIPT DE RÉSOLUTION AUTOMATIQUE DES 114 BLs DANS TADBEER (TENANT 5)
-- Correction des 100 inversions de variantes de produits
-- Correction des 2 écarts de quantités
-- Recalcul des montants et synchronisation parfaite avec Odoo
-- ======================================================================

BEGIN;

-- BL BER/OUT/00003 (LA MAISON DU CERAME) : Corriger inversion 0930000010-4 -> 0930000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40285;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00011' IS NOT NULL AND 'S00011' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00011'
              AND lcc.produit_id = 123307;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00004 (GRAND CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40293;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00025' IS NOT NULL AND 'S00025' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00025'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00027 (LA MAISON DU CERAME) : Corriger inversion 0930000010-4 -> 0930000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40472;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00152' IS NOT NULL AND 'S00152' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00152'
              AND lcc.produit_id = 123307;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00043 (CRISTAL CERAME) : Corriger inversion 0812000012-1 -> 0812000012
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0812000012' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40547;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00088' IS NOT NULL AND 'S00088' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00088'
              AND lcc.produit_id = 121242;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00046 (SUPER CERAME CASA) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40570;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00264' IS NOT NULL AND 'S00264' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00264'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00051 (SUPER CERAME CASA) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40580;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00264' IS NOT NULL AND 'S00264' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00264'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00055 (SUPER CERAME CASA) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41001;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00264' IS NOT NULL AND 'S00264' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00264'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00056 (SUPER CERAME KENITRA) : Corriger inversion 0940000025-1 -> 0940000025
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0940000025' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40581;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00271' IS NOT NULL AND 'S00271' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00271'
              AND lcc.produit_id = 121394;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00062 (KA PREFA) : Corriger inversion 192008033-1 -> 192008033
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '192008033' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40592;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00288' IS NOT NULL AND 'S00288' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00288'
              AND lcc.produit_id = 121208;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00063 (BRILLO CERAME) : Corriger inversion 0722000006-1 -> 0722000006
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0722000006' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40594;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00291' IS NOT NULL AND 'S00291' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00291'
              AND lcc.produit_id = 121036;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00071 (BRILLO CERAME) : Corriger inversion 200100016-1 -> 200100016
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '200100016' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41473;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 120999;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00072 (BRILLO CERAME) : Corriger inversion 200100016-1 -> 200100016
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '200100016' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40629;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00318' IS NOT NULL AND 'S00318' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00318'
              AND lcc.produit_id = 120999;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00073 (GRAND CERAME) : Corriger inversion 192008033-1 -> 192008033
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '192008033' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40630;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00313' IS NOT NULL AND 'S00313' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00313'
              AND lcc.produit_id = 121208;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00078 (SUPER CERAME CASA) : Corriger inversion 0811000012-01 -> 0811000012
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0811000012' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40641;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00326' IS NOT NULL AND 'S00326' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00326'
              AND lcc.produit_id = 121313;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00094 (BRILLO CERAME) : Corriger inversion 0734000601-12 -> 0734000601-1
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0734000601-1' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40689;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00383' IS NOT NULL AND 'S00383' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00383'
              AND lcc.produit_id = 121041;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00102 (SUPER CERAME TETOUAN) : Corriger inversion 0314000040-1 -> 0314000040
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000040' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40706;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00404' IS NOT NULL AND 'S00404' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00404'
              AND lcc.produit_id = 121129;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00108 (CERAMICA DERSA) : Corriger inversion 0722000005-2 -> 0722000005
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0722000005' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40758;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00455' IS NOT NULL AND 'S00455' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00455'
              AND lcc.produit_id = 120975;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00113 (TECHNO EMBALLE) : Corriger inversion 1310000001-06 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40777;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121624;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00114 (TECHNO EMBALLE) : Corriger inversion 1310000001-06 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40782;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00484' IS NOT NULL AND 'S00484' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00484'
              AND lcc.produit_id = 121624;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00115 (QUALIAGRO (Station Marrakech)) : Corriger inversion 1310000001-03 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40789;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00239' IS NOT NULL AND 'S00239' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00239'
              AND lcc.produit_id = 121627;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00117 (OMEGA BRAND) : Corriger inversion 1310000001-06 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40923;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00242' IS NOT NULL AND 'S00242' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00242'
              AND lcc.produit_id = 121624;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00119 (GRAND CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40787;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00492' IS NOT NULL AND 'S00492' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00492'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00124 (LYMOUNAMED S.A.R.L) : Corriger inversion 1310000001-06 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40821;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00502' IS NOT NULL AND 'S00502' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00502'
              AND lcc.produit_id = 121624;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00130 (DOMAINE ELBOURA) : Corriger inversion 132000028-1 -> 132000028
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '132000028' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40888;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00560' IS NOT NULL AND 'S00560' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00560'
              AND lcc.produit_id = 121135;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00131 (SUNPACK) : Corriger inversion 1200000018-1 -> 1200000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1200000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40906;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00564' IS NOT NULL AND 'S00564' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00564'
              AND lcc.produit_id = 126784;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00136 (CLIENT DIVERS) : Corriger inversion 0812000012-1 -> 0812000012
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0812000012' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40927;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121242;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00145 (SUPER CERAME CASA) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41522;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00264' IS NOT NULL AND 'S00264' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00264'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00147 (SUPER CERAME KENITRA) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41523;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00161 (SUPER CERAME KENITRA) : Corriger inversion 0311000014-1 -> 0311000014
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000014' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41139;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00692' IS NOT NULL AND 'S00692' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00692'
              AND lcc.produit_id = 126455;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00174 (DISTRA) : Corriger inversion 0712000002-1 -> 0712000002
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0712000002' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41188;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00723' IS NOT NULL AND 'S00723' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00723'
              AND lcc.produit_id = 121038;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00197 (SUPER CERAME BERRECHID) : Corriger inversion 0722000004-2 -> 0722000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0722000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41280;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00786' IS NOT NULL AND 'S00786' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00786'
              AND lcc.produit_id = 120977;
        END IF;
    END IF;
END $$;

-- BL BER/OUT/00212 (LA MAISON DU CERAME) : Corriger inversion 0930000010-4 -> 0930000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41342;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00797' IS NOT NULL AND 'S00797' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00797'
              AND lcc.produit_id = 123307;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00006 (CERAMICA DERSA) : Corriger inversion 0721000003-1 -> 0721000003
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0721000003' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40284;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00019' IS NOT NULL AND 'S00019' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00019'
              AND lcc.produit_id = 124266;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00029 (CLIENT DIVERS) : Corriger inversion 1410000198-1 -> 1410000198
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000198' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40352;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121163;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00031 (FACEMAG) : Corriger inversion 1410000198-1 -> 1410000198
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000198' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40356;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00084' IS NOT NULL AND 'S00084' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00084'
              AND lcc.produit_id = 121163;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00033 (CERAMICA DERSA) : Corriger inversion 0722000005-2 -> 0722000005
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0722000005' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40362;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00086' IS NOT NULL AND 'S00086' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00086'
              AND lcc.produit_id = 120975;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00033 (CERAMICA DERSA) : Corriger inversion 0722000004-2 -> 0722000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0722000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40363;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00086' IS NOT NULL AND 'S00086' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00086'
              AND lcc.produit_id = 120977;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00041 (SUPER CERAME KENITRA) : Corriger inversion 0314000041-2 -> 0314000041
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000041' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40386;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00104' IS NOT NULL AND 'S00104' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00104'
              AND lcc.produit_id = 121252;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00044 (SUPER CERAME CASA) : Corriger inversion 0314000041-2 -> 0314000041
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000041' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40396;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00113' IS NOT NULL AND 'S00113' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00113'
              AND lcc.produit_id = 121252;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00045 (CRISTAL CERAME) : Corriger inversion 0311000014-1 -> 0311000014
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000014' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40398;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00114' IS NOT NULL AND 'S00114' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00114'
              AND lcc.produit_id = 126455;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00045 (CRISTAL CERAME) : Corriger inversion 0311000022-1 -> 0311000022
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000022' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40399;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00114' IS NOT NULL AND 'S00114' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00114'
              AND lcc.produit_id = 126446;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00046 (SUPER CERAME CASA) : Corriger inversion 0811000012-01 -> 0811000012
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0811000012' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40402;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00105' IS NOT NULL AND 'S00105' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00105'
              AND lcc.produit_id = 121313;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00047 (BRILLO CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40406;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00115' IS NOT NULL AND 'S00115' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00115'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00050 (FACEMAG) : Corriger inversion 012206002-1 -> 012206002
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '012206002' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40414;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00111' IS NOT NULL AND 'S00111' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00111'
              AND lcc.produit_id = 121325;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00089 (BRILLO CERAME) : Corriger inversion 0650000011-2 -> 0650000011
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0650000011' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40501;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00163' IS NOT NULL AND 'S00163' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00163'
              AND lcc.produit_id = 121117;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00090 (BRILLO CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41454;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00091 (BRILLO CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40510;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00215' IS NOT NULL AND 'S00215' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00215'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00092 (GHORGHIZ CERAME) : Corriger inversion 1300000011-1 -> 1300000011
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1300000011' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40523;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00205' IS NOT NULL AND 'S00205' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00205'
              AND lcc.produit_id = 126837;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00095 (GHORGHIZ CERAME) : Corriger inversion 1810000027-1 -> 1810000027
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1810000027' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40528;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00222' IS NOT NULL AND 'S00222' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00222'
              AND lcc.produit_id = 121048;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00100 (MAREMBAL (les emballages marocains)) : Corriger inversion 012206202-1 -> 012206202
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '012206202' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40540;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00232' IS NOT NULL AND 'S00232' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00232'
              AND lcc.produit_id = 122128;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00104 (GRAND CERAME) : Corriger inversion 0311000022-1 -> 0311000022
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000022' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40554;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00246' IS NOT NULL AND 'S00246' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00246'
              AND lcc.produit_id = 126446;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00130 (FACEMAG) : Corriger inversion 1410000217-1 -> 1410000217
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000217' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40663;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00332' IS NOT NULL AND 'S00332' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00332'
              AND lcc.produit_id = 126988;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00139 (CRISTAL CERAME) : Corriger inversion 0311000014-1 -> 0311000014
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000014' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40686;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00381' IS NOT NULL AND 'S00381' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00381'
              AND lcc.produit_id = 126455;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00144 (GHORGHIZ CERAME) : Corriger inversion 0315000011-1 -> 0315000011
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0315000011' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40699;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00392' IS NOT NULL AND 'S00392' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00392'
              AND lcc.produit_id = 126515;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00153 (GHARB PAPIER CARTON "GPC") : Corriger inversion 1220000023-2 -> 1220000023
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1220000023' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40728;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00420' IS NOT NULL AND 'S00420' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00420'
              AND lcc.produit_id = 120946;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00170 (CRISTAL CERAME) : Corriger inversion 0950000003-1 -> 0950000003
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0950000003' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40752;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00457' IS NOT NULL AND 'S00457' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00457'
              AND lcc.produit_id = 121165;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00171 (CLIENT DIVERS) : Corriger inversion 0950000003-1 -> 0950000003
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0950000003' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40750;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121165;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00175 (NEXT SNACKING INDUSTRY) : Corriger inversion 1400000010-1 -> 1400000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1400000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41501;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121161;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00176 (NEXT SNACKING INDUSTRY) : Corriger inversion 1400000010-1 -> 1400000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1400000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40768;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00472' IS NOT NULL AND 'S00472' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00472'
              AND lcc.produit_id = 121161;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00177 (SUPER CERAME CASA) : Corriger inversion 0314000041-2 -> 0314000041
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000041' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40771;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00476' IS NOT NULL AND 'S00476' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00476'
              AND lcc.produit_id = 121252;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00183 (SUPER CERAME KENITRA) : Corriger inversion 1410000126-1 -> 1410000126
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000126' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40811;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00487' IS NOT NULL AND 'S00487' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00487'
              AND lcc.produit_id = 127239;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00192 (SUPER CERAME CASA) : Corriger inversion 0314000041-2 -> 0314000041
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000041' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41509;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00113' IS NOT NULL AND 'S00113' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00113'
              AND lcc.produit_id = 121252;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00197 (BRILLO CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40848;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00505' IS NOT NULL AND 'S00505' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00505'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00214 (CERAMICA DERSA) : Corriger inversion 0311000022-1 -> 0311000022
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000022' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40896;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00546' IS NOT NULL AND 'S00546' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00546'
              AND lcc.produit_id = 126446;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00219 (SUPER CERAME TETOUAN) : Corriger inversion 0314000040-1 -> 0314000040
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0314000040' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40915;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00568' IS NOT NULL AND 'S00568' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00568'
              AND lcc.produit_id = 121129;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00232 (CRISTAL CERAME) : Corriger inversion 0311000014-1 -> 0311000014
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000014' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40957;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00595' IS NOT NULL AND 'S00595' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00595'
              AND lcc.produit_id = 126455;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00245 (BRILLO CERAME) : Corriger inversion 0980000003-1 -> 0980000003
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0980000003' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40972;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00618' IS NOT NULL AND 'S00618' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00618'
              AND lcc.produit_id = 121548;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00249 (SUPER CERAME BERRECHID) : Corriger inversion 1980000112-14 -> 1980000112-1
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1980000112-1' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 40988;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00624' IS NOT NULL AND 'S00624' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00624'
              AND lcc.produit_id = 120952;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00259 (STATION KANTARI) : Corriger inversion 1200000014-1 -> 1200000014
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1200000014' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41026;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00661' IS NOT NULL AND 'S00661' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00661'
              AND lcc.produit_id = 126786;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00268 (EURO CERAMICA) : Corriger inversion 011206328-1 -> 011206328
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '011206328' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41039;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00655' IS NOT NULL AND 'S00655' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00655'
              AND lcc.produit_id = 121842;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00273 (FCE (Fresh Citrus Export)) : Corriger inversion 1310000001-06 -> 1310000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1310000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41057;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00355' IS NOT NULL AND 'S00355' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00355'
              AND lcc.produit_id = 121624;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00275 (HMI (Hygienic Modern Industries)) : Corriger inversion 0712000001-01 -> 0712000001
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0712000001' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41097;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00474' IS NOT NULL AND 'S00474' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00474'
              AND lcc.produit_id = 120976;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00278 (QUALIAGRO (Station Marrakech)) : Corriger inversion 1200000168-1 -> 1200000168
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1200000168' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41083;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00663' IS NOT NULL AND 'S00663' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00663'
              AND lcc.produit_id = 126756;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00283 (GHORGHIZ CERAME) : Corriger inversion 1810000027-1 -> 1810000027
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1810000027' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41101;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00689' IS NOT NULL AND 'S00689' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00689'
              AND lcc.produit_id = 121048;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00297 : Ajuster quantite de 0800000003 de 0.0 -> 300.0
UPDATE lignes_bon_livraison_client 
SET quantite_livree = 300.000,
    montant_ht = 300.000 * prix_vente,
    montant_ligne = 300.000 * prix_vente,
    montant_ttc = 300.000 * prix_vente * (1 + taux_tva / 100.0)
WHERE id = 41163;

-- BL TAC/OUT/00308 : Ajuster quantite de 0930000018-1 de 0.0 -> 130.0
UPDATE lignes_bon_livraison_client 
SET quantite_livree = 130.000,
    montant_ht = 130.000 * prix_vente,
    montant_ligne = 130.000 * prix_vente,
    montant_ttc = 130.000 * prix_vente * (1 + taux_tva / 100.0)
WHERE id = 41535;

-- BL TAC/OUT/00308 (SUPER CERAME CASA) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41535;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00331' IS NOT NULL AND 'S00331' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00331'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00310 (SUPER CERAME KENITRA) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41192;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00311 (SUPER CERAME CASA) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41193;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00734' IS NOT NULL AND 'S00734' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00734'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00312 (GHORGHIZ CERAME) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41198;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00735' IS NOT NULL AND 'S00735' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00735'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00313 (BRILLO CERAME) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41200;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00736' IS NOT NULL AND 'S00736' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00736'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00315 (SUPER CERAME KENITRA) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41197;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00741' IS NOT NULL AND 'S00741' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00741'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00317 (SUPER CERAME TETOUAN) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41216;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00745' IS NOT NULL AND 'S00745' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00745'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00318 (SUPER CERAME CASA) : Corriger inversion 0311000022-1 -> 0311000022
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0311000022' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41217;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00747' IS NOT NULL AND 'S00747' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00747'
              AND lcc.produit_id = 126446;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00319 (SUPER CERAME TETOUAN) : Corriger inversion 0930000018-1 -> 0930000018
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000018' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41543;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00745' IS NOT NULL AND 'S00745' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00745'
              AND lcc.produit_id = 123444;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00322 (JACOB DELAFON MAROC) : Corriger inversion 1820000010-1 -> 1820000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1820000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41227;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121169;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00325 (FACEMAG) : Corriger inversion 1410000217-1 -> 1410000217
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000217' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41238;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00755' IS NOT NULL AND 'S00755' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00755'
              AND lcc.produit_id = 126988;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00329 (AMA DÉTERGENT) : Corriger inversion 1410000198-1 -> 1410000198
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000198' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41250;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00611' IS NOT NULL AND 'S00611' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00611'
              AND lcc.produit_id = 121163;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00336 (GHORGHIZ CERAME) : Corriger inversion 0910000011-2 -> 0910000011
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0910000011' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41266;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00608' IS NOT NULL AND 'S00608' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00608'
              AND lcc.produit_id = 123322;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00345 (JACOB DELAFON MAROC) : Corriger inversion 1820000010-1 -> 1820000010
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1820000010' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41288;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00790' IS NOT NULL AND 'S00790' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00790'
              AND lcc.produit_id = 121169;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00349 (GRAND CERAME) : Corriger inversion 015200030-1 -> 015200030
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '015200030' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41297;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00789' IS NOT NULL AND 'S00789' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00789'
              AND lcc.produit_id = 120968;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00351 (FACEMAG) : Corriger inversion 1410000126-1 -> 1410000126
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000126' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41310;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00792' IS NOT NULL AND 'S00792' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00792'
              AND lcc.produit_id = 127239;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00355 (OLEA CAPITAL SLE BERTIN SA) : Corriger inversion 200100016-1 -> 200100016
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '200100016' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41330;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00816' IS NOT NULL AND 'S00816' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00816'
              AND lcc.produit_id = 120999;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00363 (JACOB DELAFON MAROC) : Corriger inversion 1210000005-1 -> 1210000005
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1210000005' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41391;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00860' IS NOT NULL AND 'S00860' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00860'
              AND lcc.produit_id = 123109;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00364 (DAMSA CONSERVERIE) : Corriger inversion 012203205-2 -> 012203205
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '012203205' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41590;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121442;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00366 (DAMSA CONSERVERIE) : Corriger inversion 012203205-2 -> 012203205
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '012203205' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41625;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 121442;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00367 (DAMSA CONSERVERIE) : Corriger inversion 012203205-2 -> 012203205
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '012203205' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41381;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00868' IS NOT NULL AND 'S00868' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00868'
              AND lcc.produit_id = 121442;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00373 (SUPER CERAME BERRECHID) : Corriger inversion 0940000024-1 -> 0940000024
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0940000024' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41404;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00879' IS NOT NULL AND 'S00879' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00879'
              AND lcc.produit_id = 121395;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00374 (SUPER CERAME TETOUAN) : Corriger inversion 1410000288-19 -> 1410000288-1
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '1410000288-1' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41418;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00880' IS NOT NULL AND 'S00880' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00880'
              AND lcc.produit_id = 127171;
        END IF;
    END IF;
END $$;

-- BL TAC/OUT/00379 (SUPER CERAME BERRECHID) : Corriger inversion 0940000024-1 -> 0940000024
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0940000024' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41633;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'S00879' IS NOT NULL AND 'S00879' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'S00879'
              AND lcc.produit_id = 121395;
        END IF;
    END IF;
END $$;

-- BL WH/OUT/00006 (CERAMICA DERSA) : Corriger inversion 0721000003-1 -> 0721000003
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0721000003' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41421;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 124266;
        END IF;
    END IF;
END $$;

-- BL WH/OUT/00007 (GRAND CERAME) : Corriger inversion 0930000004-1 -> 0930000004
DO $$
DECLARE
    v_target_prod_id BIGINT;
BEGIN
    SELECT id INTO v_target_prod_id FROM produits 
    WHERE point_de_vente_id = 5 AND reference = '0930000004' LIMIT 1;
    
    IF v_target_prod_id IS NOT NULL THEN
        -- Corriger la ligne du BL
        UPDATE lignes_bon_livraison_client 
        SET produit_id = v_target_prod_id
        WHERE id = 41427;
        
        -- Corriger la ligne de commande associée si elle existe
        IF 'None' IS NOT NULL AND 'None' <> '' THEN
            UPDATE lignes_commande_client lcc
            SET produit_id = v_target_prod_id
            FROM commandes_client cc
            WHERE lcc.commande_client_id = cc.id
              AND cc.point_de_vente_id = 5
              AND cc.numero_commande = 'None'
              AND lcc.produit_id = 120998;
        END IF;
    END IF;
END $$;


-- ----------------------------------------------------------------------
-- RECALCUL DES TOTAUX DES BONS DE LIVRAISON IMPACTÉS
-- ----------------------------------------------------------------------
UPDATE bons_livraison_client bl
SET montant_ht = COALESCE(sub.tot_ht, 0.0),
    montant_total = COALESCE(sub.tot_ht, 0.0),
    montant_ttc = COALESCE(sub.tot_ttc, 0.0)
FROM (
    SELECT 
        bon_livraison_client_id,
        SUM(COALESCE(montant_ht, quantite_livree * prix_vente)) AS tot_ht,
        SUM(COALESCE(montant_ttc, quantite_livree * prix_vente * 1.20)) AS tot_ttc
    FROM lignes_bon_livraison_client
    GROUP BY bon_livraison_client_id
) sub
WHERE bl.id = sub.bon_livraison_client_id
  AND bl.point_de_vente_id = 5;

-- Vérification de la commande S00484 (TECHNO EMBALLE)
SELECT 
    bl.numero_bl,
    p.reference,
    p.designation,
    lbl.quantite_livree,
    lbl.prix_vente
FROM bons_livraison_client bl
JOIN lignes_bon_livraison_client lbl ON lbl.bon_livraison_client_id = bl.id
JOIN produits p ON p.id = lbl.produit_id
WHERE bl.point_de_vente_id = 5 
  AND bl.numero_bl = 'BER/OUT/00114';

COMMIT;

BEGIN;

-- 1. Détacher les BLs de ces 15 factures pour réassigner strictement les bons
UPDATE bons_livraison_client 
SET facture_id = NULL 
WHERE point_de_vente_id = 5 
  AND facture_id IN (
      SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture IN ('FA/2026/00440', 'FA/2026/00439', 'FA/2026/00438', 'FA/2026/00431', 'FA/2026/00437', 'FA/2026/00435', 'FA/2026/00432', 'FA/2026/00430', 'FA/2026/00429', 'FA/2026/00424', 'FA/2026/00423', 'FA/2026/00428', 'FA/2026/00427', 'FA/2026/00426', 'FA/2026/00425')
  );

-- 2. Rattacher strictement les BLs réels de chaque facture
-- Facture FA/2026/00440 -> BLs : 'BER/OUT/00148'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00440' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00148');

-- Facture FA/2026/00439 -> BLs : 'TAC/OUT/00377'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00439' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00377');

-- Facture FA/2026/00438 -> BLs : 'BER/OUT/00158', 'TAC/OUT/00341'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00438' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00158', 'TAC/OUT/00341');

-- Facture FA/2026/00431 -> BLs : 'TAC/OUT/00284', 'TAC/OUT/00285', 'TAC/OUT/00290'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00431' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00284', 'TAC/OUT/00285', 'TAC/OUT/00290');

-- Facture FA/2026/00437 -> BLs : 'TAC/OUT/00376'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00437' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00376');

-- Facture FA/2026/00435 -> BLs : 'TAC/OUT/00378'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00435' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00378');

-- Facture FA/2026/00432 -> BLs : 'TAC/OUT/00371'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00432' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00371');

-- Facture FA/2026/00430 -> BLs : 'BER/OUT/00213'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00430' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00213');

-- Facture FA/2026/00429 -> BLs : 'BER/OUT/00208'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00429' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00208');

-- Facture FA/2026/00424 -> BLs : 'BER/OUT/00200'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00424' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00200');

-- Facture FA/2026/00423 -> BLs : 'TAC/OUT/00383'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00423' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('TAC/OUT/00383');

-- Facture FA/2026/00428 -> BLs : 'BER/OUT/00207'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00428' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00207');

-- Facture FA/2026/00427 -> BLs : 'BER/OUT/00196'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00427' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00196');

-- Facture FA/2026/00426 -> BLs : 'BER/OUT/00204'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00426' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00204');

-- Facture FA/2026/00425 -> BLs : 'BER/OUT/00209'
UPDATE bons_livraison_client
SET facture_id = (SELECT id FROM factures WHERE point_de_vente_id = 5 AND numero_facture = 'FA/2026/00425' LIMIT 1)
WHERE point_de_vente_id = 5
  AND numero_bl IN ('BER/OUT/00209');

-- 3. Vérification de la facture FA/2026/00424
SELECT 
    f.numero_facture,
    f.date_facture,
    c.nom AS client,
    bl.numero_bl
FROM factures f
LEFT JOIN clients c ON c.id = f.client_id
LEFT JOIN bons_livraison_client bl ON bl.facture_id = f.id
WHERE f.point_de_vente_id = 5 
  AND f.numero_facture = 'FA/2026/00424';

COMMIT;

-- =============================================================================
-- SYNCHRONISATION DES LIVRAISONS PARTIELLES, RELIQUATS ET STATUTS TADBEER
-- Tenant : #5 (SIT TRANSMISSION SARL)
-- =============================================================================
\set ON_ERROR_STOP on
BEGIN;

-- -----------------------------------------------------------------------------
-- 1. PASSAGE DES 29 BLs EN ATTENTE (NON EXPÉDIÉS) AU STATUT 'EN_ATTENTE'
-- -----------------------------------------------------------------------------

UPDATE bons_livraison_client
SET statut = 'EN_ATTENTE', montant_total = 0.00
WHERE numero_bl IN ('BER/OUT/00040', 'BER/OUT/00050', 'BER/OUT/00066', 'BER/OUT/00096', 'BER/OUT/00145', 'BER/OUT/00155', 'BER/OUT/00162', 'BER/OUT/00181', 'BER/OUT/00191', 'BER/OUT/00194', 'BER/OUT/00206', 'BER/OUT/00214', 'BER/OUT/00215', 'BER/OUT/00216', 'TAC/OUT/00021', 'TAC/OUT/00241', 'TAC/OUT/00242', 'TAC/OUT/00243', 'TAC/OUT/00265', 'TAC/OUT/00267', 'TAC/OUT/00291', 'TAC/OUT/00319', 'TAC/OUT/00330', 'TAC/OUT/00333', 'TAC/OUT/00348', 'TAC/OUT/00354', 'TAC/OUT/00375', 'TAC/OUT/00379', 'TAC/OUT/00382') AND point_de_vente_id = 5;

UPDATE lignes_bon_livraison_client
SET quantite_livree = 0.000, montant_ligne = 0.00
WHERE bon_livraison_client_id IN (
    SELECT id FROM bons_livraison_client 
    WHERE numero_bl IN ('BER/OUT/00040', 'BER/OUT/00050', 'BER/OUT/00066', 'BER/OUT/00096', 'BER/OUT/00145', 'BER/OUT/00155', 'BER/OUT/00162', 'BER/OUT/00181', 'BER/OUT/00191', 'BER/OUT/00194', 'BER/OUT/00206', 'BER/OUT/00214', 'BER/OUT/00215', 'BER/OUT/00216', 'TAC/OUT/00021', 'TAC/OUT/00241', 'TAC/OUT/00242', 'TAC/OUT/00243', 'TAC/OUT/00265', 'TAC/OUT/00267', 'TAC/OUT/00291', 'TAC/OUT/00319', 'TAC/OUT/00330', 'TAC/OUT/00333', 'TAC/OUT/00348', 'TAC/OUT/00354', 'TAC/OUT/00375', 'TAC/OUT/00379', 'TAC/OUT/00382') AND point_de_vente_id = 5
);


-- -----------------------------------------------------------------------------
-- 2. PASSAGE DES 40 BLs ANNULÉS AU STATUT 'ANNULEE'
-- -----------------------------------------------------------------------------

UPDATE bons_livraison_client
SET statut = 'ANNULEE', montant_total = 0.00
WHERE numero_bl IN ('BER/OUT/00025', 'BER/OUT/00071', 'BER/OUT/00106', 'BER/OUT/00134', 'BER/OUT/00141', 'BER/OUT/00147', 'BER/OUT/00183', 'BER/OUT/00201', 'TAC/OUT/00011', 'TAC/OUT/00026', 'TAC/OUT/00064', 'TAC/OUT/00072', 'TAC/OUT/00084', 'TAC/OUT/00087', 'TAC/OUT/00090', 'TAC/OUT/00106', 'TAC/OUT/00126', 'TAC/OUT/00135', 'TAC/OUT/00150', 'TAC/OUT/00162', 'TAC/OUT/00175', 'TAC/OUT/00191', 'TAC/OUT/00192', 'TAC/OUT/00200', 'TAC/OUT/00233', 'TAC/OUT/00262', 'TAC/OUT/00308', 'TAC/OUT/00364', 'TAC/OUT/00366', 'WH/OUT/00006', 'WH/OUT/00007', 'WH/OUT/00009', 'WH/OUT/00010', 'WH/OUT/00011', 'WH/OUT/00019', 'WH/OUT/00023', 'WH/OUT/00027', 'WH/OUT/00033', 'WH/OUT/00038', 'WH/OUT/00050') AND point_de_vente_id = 5;

UPDATE lignes_bon_livraison_client
SET quantite_livree = 0.000, montant_ligne = 0.00
WHERE bon_livraison_client_id IN (
    SELECT id FROM bons_livraison_client 
    WHERE numero_bl IN ('BER/OUT/00025', 'BER/OUT/00071', 'BER/OUT/00106', 'BER/OUT/00134', 'BER/OUT/00141', 'BER/OUT/00147', 'BER/OUT/00183', 'BER/OUT/00201', 'TAC/OUT/00011', 'TAC/OUT/00026', 'TAC/OUT/00064', 'TAC/OUT/00072', 'TAC/OUT/00084', 'TAC/OUT/00087', 'TAC/OUT/00090', 'TAC/OUT/00106', 'TAC/OUT/00126', 'TAC/OUT/00135', 'TAC/OUT/00150', 'TAC/OUT/00162', 'TAC/OUT/00175', 'TAC/OUT/00191', 'TAC/OUT/00192', 'TAC/OUT/00200', 'TAC/OUT/00233', 'TAC/OUT/00262', 'TAC/OUT/00308', 'TAC/OUT/00364', 'TAC/OUT/00366', 'WH/OUT/00006', 'WH/OUT/00007', 'WH/OUT/00009', 'WH/OUT/00010', 'WH/OUT/00011', 'WH/OUT/00019', 'WH/OUT/00023', 'WH/OUT/00027', 'WH/OUT/00033', 'WH/OUT/00038', 'WH/OUT/00050') AND point_de_vente_id = 5
);


-- -----------------------------------------------------------------------------
-- 3. CONFIRMATION DU STATUT 'LIVREE' POUR LES 513 BLs EFFECTIVEMENT EXPÉDIÉS
-- -----------------------------------------------------------------------------

UPDATE bons_livraison_client
SET statut = 'LIVREE'
WHERE numero_bl IN ('BER/OUT/00001', 'BER/OUT/00002', 'BER/OUT/00003', 'BER/OUT/00004', 'BER/OUT/00005', 'BER/OUT/00006', 'BER/OUT/00007', 'BER/OUT/00008', 'BER/OUT/00009', 'BER/OUT/00010', 'BER/OUT/00011', 'BER/OUT/00012', 'BER/OUT/00014', 'BER/OUT/00015', 'BER/OUT/00016', 'BER/OUT/00017', 'BER/OUT/00018', 'BER/OUT/00019', 'BER/OUT/00020', 'BER/OUT/00021', 'BER/OUT/00022', 'BER/OUT/00023', 'BER/OUT/00024', 'BER/OUT/00026', 'BER/OUT/00027', 'BER/OUT/00028', 'BER/OUT/00029', 'BER/OUT/00030', 'BER/OUT/00031', 'BER/OUT/00032', 'BER/OUT/00033', 'BER/OUT/00034', 'BER/OUT/00035', 'BER/OUT/00037', 'BER/OUT/00038', 'BER/OUT/00039', 'BER/OUT/00041', 'BER/OUT/00042', 'BER/OUT/00043', 'BER/OUT/00044', 'BER/OUT/00045', 'BER/OUT/00046', 'BER/OUT/00047', 'BER/OUT/00048', 'BER/OUT/00049', 'BER/OUT/00051', 'BER/OUT/00052', 'BER/OUT/00053', 'BER/OUT/00054', 'BER/OUT/00055', 'BER/OUT/00056', 'BER/OUT/00057', 'BER/OUT/00058', 'BER/OUT/00060', 'BER/OUT/00061', 'BER/OUT/00062', 'BER/OUT/00063', 'BER/OUT/00064', 'BER/OUT/00067', 'BER/OUT/00068', 'BER/OUT/00070', 'BER/OUT/00072', 'BER/OUT/00073', 'BER/OUT/00074', 'BER/OUT/00076', 'BER/OUT/00077', 'BER/OUT/00078', 'BER/OUT/00079', 'BER/OUT/00080', 'BER/OUT/00081', 'BER/OUT/00082', 'BER/OUT/00083', 'BER/OUT/00084', 'BER/OUT/00085', 'BER/OUT/00086', 'BER/OUT/00088', 'BER/OUT/00089', 'BER/OUT/00090', 'BER/OUT/00091', 'BER/OUT/00092', 'BER/OUT/00093', 'BER/OUT/00094', 'BER/OUT/00097', 'BER/OUT/00098', 'BER/OUT/00099', 'BER/OUT/00100', 'BER/OUT/00101', 'BER/OUT/00102', 'BER/OUT/00103', 'BER/OUT/00104', 'BER/OUT/00107', 'BER/OUT/00108', 'BER/OUT/00110', 'BER/OUT/00111', 'BER/OUT/00112', 'BER/OUT/00113', 'BER/OUT/00114', 'BER/OUT/00115', 'BER/OUT/00116', 'BER/OUT/00117', 'BER/OUT/00119', 'BER/OUT/00120', 'BER/OUT/00121', 'BER/OUT/00122', 'BER/OUT/00123', 'BER/OUT/00124', 'BER/OUT/00125', 'BER/OUT/00126', 'BER/OUT/00127', 'BER/OUT/00128', 'BER/OUT/00129', 'BER/OUT/00130', 'BER/OUT/00131', 'BER/OUT/00133', 'BER/OUT/00135', 'BER/OUT/00136', 'BER/OUT/00137', 'BER/OUT/00138', 'BER/OUT/00139', 'BER/OUT/00140', 'BER/OUT/00142', 'BER/OUT/00143', 'BER/OUT/00144', 'BER/OUT/00146', 'BER/OUT/00148', 'BER/OUT/00149', 'BER/OUT/00150', 'BER/OUT/00151', 'BER/OUT/00152', 'BER/OUT/00153', 'BER/OUT/00154', 'BER/OUT/00157', 'BER/OUT/00158', 'BER/OUT/00159', 'BER/OUT/00160', 'BER/OUT/00161', 'BER/OUT/00164', 'BER/OUT/00165', 'BER/OUT/00167', 'BER/OUT/00168', 'BER/OUT/00169', 'BER/OUT/00170', 'BER/OUT/00171', 'BER/OUT/00172', 'BER/OUT/00173', 'BER/OUT/00174', 'BER/OUT/00175', 'BER/OUT/00176', 'BER/OUT/00177', 'BER/OUT/00178', 'BER/OUT/00179', 'BER/OUT/00180', 'BER/OUT/00184', 'BER/OUT/00185', 'BER/OUT/00186', 'BER/OUT/00187', 'BER/OUT/00188', 'BER/OUT/00189', 'BER/OUT/00190', 'BER/OUT/00192', 'BER/OUT/00193', 'BER/OUT/00195', 'BER/OUT/00196', 'BER/OUT/00197', 'BER/OUT/00198', 'BER/OUT/00199', 'BER/OUT/00200', 'BER/OUT/00202', 'BER/OUT/00203', 'BER/OUT/00204', 'BER/OUT/00205', 'BER/OUT/00207', 'BER/OUT/00208', 'BER/OUT/00209', 'BER/OUT/00210', 'BER/OUT/00211', 'BER/OUT/00212', 'BER/OUT/00213', 'TAC/OUT/00001', 'TAC/OUT/00003', 'TAC/OUT/00004', 'TAC/OUT/00005', 'TAC/OUT/00006', 'TAC/OUT/00007', 'TAC/OUT/00008', 'TAC/OUT/00009', 'TAC/OUT/00010', 'TAC/OUT/00012', 'TAC/OUT/00013', 'TAC/OUT/00014', 'TAC/OUT/00015', 'TAC/OUT/00016', 'TAC/OUT/00017', 'TAC/OUT/00018', 'TAC/OUT/00019', 'TAC/OUT/00020', 'TAC/OUT/00022', 'TAC/OUT/00023', 'TAC/OUT/00024', 'TAC/OUT/00025', 'TAC/OUT/00027', 'TAC/OUT/00028', 'TAC/OUT/00029', 'TAC/OUT/00030', 'TAC/OUT/00031', 'TAC/OUT/00032', 'TAC/OUT/00033', 'TAC/OUT/00034', 'TAC/OUT/00035', 'TAC/OUT/00036', 'TAC/OUT/00037', 'TAC/OUT/00038', 'TAC/OUT/00039', 'TAC/OUT/00040', 'TAC/OUT/00041', 'TAC/OUT/00042', 'TAC/OUT/00043', 'TAC/OUT/00044', 'TAC/OUT/00045', 'TAC/OUT/00046', 'TAC/OUT/00047', 'TAC/OUT/00048', 'TAC/OUT/00049', 'TAC/OUT/00050', 'TAC/OUT/00051', 'TAC/OUT/00052', 'TAC/OUT/00054', 'TAC/OUT/00055', 'TAC/OUT/00056', 'TAC/OUT/00057', 'TAC/OUT/00058', 'TAC/OUT/00059', 'TAC/OUT/00060', 'TAC/OUT/00061', 'TAC/OUT/00062', 'TAC/OUT/00063', 'TAC/OUT/00065', 'TAC/OUT/00066', 'TAC/OUT/00067', 'TAC/OUT/00068', 'TAC/OUT/00069', 'TAC/OUT/00070', 'TAC/OUT/00071', 'TAC/OUT/00073', 'TAC/OUT/00074', 'TAC/OUT/00075', 'TAC/OUT/00076', 'TAC/OUT/00078', 'TAC/OUT/00079', 'TAC/OUT/00081', 'TAC/OUT/00082', 'TAC/OUT/00083', 'TAC/OUT/00085', 'TAC/OUT/00086', 'TAC/OUT/00088', 'TAC/OUT/00089', 'TAC/OUT/00091', 'TAC/OUT/00092', 'TAC/OUT/00093', 'TAC/OUT/00094', 'TAC/OUT/00095', 'TAC/OUT/00097', 'TAC/OUT/00099', 'TAC/OUT/00100', 'TAC/OUT/00101', 'TAC/OUT/00102', 'TAC/OUT/00103', 'TAC/OUT/00104', 'TAC/OUT/00107', 'TAC/OUT/00108', 'TAC/OUT/00110', 'TAC/OUT/00111', 'TAC/OUT/00112', 'TAC/OUT/00113', 'TAC/OUT/00114', 'TAC/OUT/00115', 'TAC/OUT/00116', 'TAC/OUT/00117', 'TAC/OUT/00118', 'TAC/OUT/00119', 'TAC/OUT/00120', 'TAC/OUT/00121', 'TAC/OUT/00122', 'TAC/OUT/00125', 'TAC/OUT/00127', 'TAC/OUT/00128', 'TAC/OUT/00129', 'TAC/OUT/00130', 'TAC/OUT/00131', 'TAC/OUT/00132', 'TAC/OUT/00133', 'TAC/OUT/00134', 'TAC/OUT/00136', 'TAC/OUT/00137', 'TAC/OUT/00138', 'TAC/OUT/00139', 'TAC/OUT/00140', 'TAC/OUT/00141', 'TAC/OUT/00142', 'TAC/OUT/00143', 'TAC/OUT/00144', 'TAC/OUT/00145', 'TAC/OUT/00146', 'TAC/OUT/00147', 'TAC/OUT/00148', 'TAC/OUT/00149', 'TAC/OUT/00151', 'TAC/OUT/00152', 'TAC/OUT/00153', 'TAC/OUT/00154', 'TAC/OUT/00155', 'TAC/OUT/00156', 'TAC/OUT/00157', 'TAC/OUT/00158', 'TAC/OUT/00159', 'TAC/OUT/00160', 'TAC/OUT/00163', 'TAC/OUT/00164', 'TAC/OUT/00165', 'TAC/OUT/00166', 'TAC/OUT/00167', 'TAC/OUT/00168', 'TAC/OUT/00169', 'TAC/OUT/00170', 'TAC/OUT/00171', 'TAC/OUT/00172', 'TAC/OUT/00173', 'TAC/OUT/00174', 'TAC/OUT/00176', 'TAC/OUT/00177', 'TAC/OUT/00178', 'TAC/OUT/00179', 'TAC/OUT/00180', 'TAC/OUT/00181', 'TAC/OUT/00182', 'TAC/OUT/00183', 'TAC/OUT/00184', 'TAC/OUT/00186', 'TAC/OUT/00187', 'TAC/OUT/00188', 'TAC/OUT/00189', 'TAC/OUT/00193', 'TAC/OUT/00194', 'TAC/OUT/00195', 'TAC/OUT/00196', 'TAC/OUT/00197', 'TAC/OUT/00198', 'TAC/OUT/00199', 'TAC/OUT/00201', 'TAC/OUT/00202', 'TAC/OUT/00203', 'TAC/OUT/00204', 'TAC/OUT/00205', 'TAC/OUT/00206', 'TAC/OUT/00207', 'TAC/OUT/00208', 'TAC/OUT/00209', 'TAC/OUT/00210', 'TAC/OUT/00211', 'TAC/OUT/00212', 'TAC/OUT/00213', 'TAC/OUT/00214', 'TAC/OUT/00216', 'TAC/OUT/00217', 'TAC/OUT/00218', 'TAC/OUT/00219', 'TAC/OUT/00220', 'TAC/OUT/00221', 'TAC/OUT/00222', 'TAC/OUT/00223', 'TAC/OUT/00224', 'TAC/OUT/00225', 'TAC/OUT/00226', 'TAC/OUT/00227', 'TAC/OUT/00228', 'TAC/OUT/00229', 'TAC/OUT/00230', 'TAC/OUT/00231', 'TAC/OUT/00232', 'TAC/OUT/00234', 'TAC/OUT/00235', 'TAC/OUT/00236', 'TAC/OUT/00237', 'TAC/OUT/00238', 'TAC/OUT/00239', 'TAC/OUT/00240', 'TAC/OUT/00244', 'TAC/OUT/00245', 'TAC/OUT/00246', 'TAC/OUT/00247', 'TAC/OUT/00248', 'TAC/OUT/00249', 'TAC/OUT/00250', 'TAC/OUT/00251', 'TAC/OUT/00252', 'TAC/OUT/00253', 'TAC/OUT/00254', 'TAC/OUT/00255', 'TAC/OUT/00256', 'TAC/OUT/00257', 'TAC/OUT/00258', 'TAC/OUT/00259', 'TAC/OUT/00260', 'TAC/OUT/00261', 'TAC/OUT/00263', 'TAC/OUT/00264', 'TAC/OUT/00266', 'TAC/OUT/00268', 'TAC/OUT/00269', 'TAC/OUT/00270', 'TAC/OUT/00271', 'TAC/OUT/00272', 'TAC/OUT/00273', 'TAC/OUT/00274', 'TAC/OUT/00275', 'TAC/OUT/00276', 'TAC/OUT/00277', 'TAC/OUT/00278', 'TAC/OUT/00279', 'TAC/OUT/00280', 'TAC/OUT/00281', 'TAC/OUT/00282', 'TAC/OUT/00283', 'TAC/OUT/00284', 'TAC/OUT/00285', 'TAC/OUT/00287', 'TAC/OUT/00288', 'TAC/OUT/00289', 'TAC/OUT/00290', 'TAC/OUT/00292', 'TAC/OUT/00293', 'TAC/OUT/00294', 'TAC/OUT/00295', 'TAC/OUT/00296', 'TAC/OUT/00297', 'TAC/OUT/00298', 'TAC/OUT/00299', 'TAC/OUT/00300', 'TAC/OUT/00301', 'TAC/OUT/00302', 'TAC/OUT/00303', 'TAC/OUT/00304', 'TAC/OUT/00305', 'TAC/OUT/00306', 'TAC/OUT/00307', 'TAC/OUT/00309', 'TAC/OUT/00310', 'TAC/OUT/00311', 'TAC/OUT/00312', 'TAC/OUT/00313', 'TAC/OUT/00314', 'TAC/OUT/00315', 'TAC/OUT/00316', 'TAC/OUT/00317', 'TAC/OUT/00318', 'TAC/OUT/00320', 'TAC/OUT/00321', 'TAC/OUT/00322', 'TAC/OUT/00323', 'TAC/OUT/00324', 'TAC/OUT/00325', 'TAC/OUT/00326', 'TAC/OUT/00327', 'TAC/OUT/00329', 'TAC/OUT/00332', 'TAC/OUT/00334', 'TAC/OUT/00335', 'TAC/OUT/00336', 'TAC/OUT/00338', 'TAC/OUT/00339', 'TAC/OUT/00340', 'TAC/OUT/00341', 'TAC/OUT/00342', 'TAC/OUT/00343', 'TAC/OUT/00344', 'TAC/OUT/00345', 'TAC/OUT/00346', 'TAC/OUT/00347', 'TAC/OUT/00349', 'TAC/OUT/00350', 'TAC/OUT/00351', 'TAC/OUT/00352', 'TAC/OUT/00353', 'TAC/OUT/00355', 'TAC/OUT/00357', 'TAC/OUT/00358', 'TAC/OUT/00359', 'TAC/OUT/00360', 'TAC/OUT/00361', 'TAC/OUT/00362', 'TAC/OUT/00363', 'TAC/OUT/00365', 'TAC/OUT/00367', 'TAC/OUT/00368', 'TAC/OUT/00369', 'TAC/OUT/00370', 'TAC/OUT/00371', 'TAC/OUT/00372', 'TAC/OUT/00373', 'TAC/OUT/00374', 'TAC/OUT/00376', 'TAC/OUT/00377', 'TAC/OUT/00378', 'TAC/OUT/00380', 'TAC/OUT/00381', 'TAC/OUT/00383', 'WH/OUT/00001', 'WH/OUT/00004', 'WH/OUT/00026', 'WH/OUT/00029', 'WH/OUT/00034', 'WH/OUT/00039', 'WH/OUT/00040') AND point_de_vente_id = 5;


-- -----------------------------------------------------------------------------
-- 4. RECALCUL STRICT DES QUANTITÉS LIVRÉES ET RELIQUATS SUR LES LIGNES DE COMMANDES
-- -----------------------------------------------------------------------------

-- Mise à jour de la quantité livrée cumulée (uniquement basée sur les BLs ayant statut = 'LIVREE')
UPDATE lignes_commande_client lcc
SET quantite_livree = COALESCE((
    SELECT SUM(lbl.quantite_livree)
    FROM lignes_bon_livraison_client lbl
    JOIN bons_livraison_client bl ON bl.id = lbl.bon_livraison_client_id
    JOIN bons_livraison_client_commandes blc ON blc.bon_livraison_id = bl.id
    WHERE blc.commande_id = lcc.commande_client_id
      AND lbl.produit_id = lcc.produit_id
      AND bl.statut = 'LIVREE'
      AND bl.point_de_vente_id = 5
), 0.000)
WHERE lcc.commande_client_id IN (
    SELECT id FROM commandes_client WHERE point_de_vente_id = 5
);

-- Recalcul du reliquat restant à livrer
UPDATE lignes_commande_client
SET quantite_reliquat = GREATEST(0.000, quantite_commandee - quantite_livree)
WHERE commande_client_id IN (
    SELECT id FROM commandes_client WHERE point_de_vente_id = 5
);


-- -----------------------------------------------------------------------------
-- 5. MISE À JOUR DES STATUTS COMMANDES : 'LIVREE', 'LIVREE_PARTIELLE' OU 'CONFIRMEE'
-- -----------------------------------------------------------------------------

UPDATE commandes_client cc
SET statut = CASE
    -- Si toutes les quantités commandées sont livrées
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande_client WHERE commande_client_id = cc.id) >= (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande_client WHERE commande_client_id = cc.id)
         AND (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande_client WHERE commande_client_id = cc.id) > 0
    THEN 'LIVREE'
    -- Si au moins une unité a été livrée mais qu'il reste du reliquat
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande_client WHERE commande_client_id = cc.id) > 0
    THEN 'LIVREE_PARTIELLE'
    -- Si rien n'a encore été livré
    ELSE 'CONFIRMEE'
END
WHERE cc.point_de_vente_id = 5
  AND cc.statut NOT IN ('ANNULEE', 'BROUILLON');


COMMIT;

-- -----------------------------------------------------------------------------
-- 6. CONTRÔLE FINAL : DÉTAIL COMMANDE S00264 & SYNTHÈSE DES STATUTS
-- -----------------------------------------------------------------------------

\echo '========================================================================='
\echo '1. DÉTAIL COMMANDE S00264 (SUPER CERAME CASA)'
\echo '========================================================================='
SELECT 
    c.numero_commande,
    c.statut AS statut_commande,
    p.reference AS ref_produit,
    lcc.quantite_commandee,
    lcc.quantite_livree,
    lcc.quantite_reliquat
FROM commandes_client c
JOIN lignes_commande_client lcc ON lcc.commande_client_id = c.id
JOIN produits p ON p.id = lcc.produit_id
WHERE c.numero_commande = 'S00264' AND c.point_de_vente_id = 5;

\echo '========================================================================='
\echo '2. LES 4 BLS RATTACHÉS À S00264 ET LEUR STATUT'
\echo '========================================================================='
SELECT 
    bl.numero_bl,
    bl.statut AS statut_bl,
    bl.date_bl::date AS date_bl,
    lbl.quantite_livree
FROM bons_livraison_client bl
JOIN bons_livraison_client_commandes blc ON blc.bon_livraison_id = bl.id
JOIN commandes_client c ON c.id = blc.commande_id
JOIN lignes_bon_livraison_client lbl ON lbl.bon_livraison_client_id = bl.id
WHERE c.numero_commande = 'S00264' AND c.point_de_vente_id = 5
ORDER BY bl.date_bl;

\echo '========================================================================='
\echo '3. RÉPARTITION DES STATUTS DE COMMANDES CLIENTS APRÈS SYNCHRO'
\echo '========================================================================='
SELECT statut, COUNT(*) AS nb_commandes
FROM commandes_client
WHERE point_de_vente_id = 5
GROUP BY statut
ORDER BY nb_commandes DESC;


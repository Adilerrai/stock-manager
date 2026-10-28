BEGIN;

-- ----------------------------------------------------------------------
-- 1. MISE À JOUR DES DÉLAIS DE PAIEMENT DANS LA TABLE CLIENTS
-- ----------------------------------------------------------------------

-- Clients à 120 JOURS
UPDATE clients 
SET delai_paiement_jours = 120
WHERE point_de_vente_id = 5 
  AND (UPPER(nom) LIKE 'SUPER CERAME%' OR UPPER(nom) LIKE 'GRAND CERAME%' OR UPPER(nom) LIKE 'CRISTAL CERAME%' OR UPPER(nom) LIKE 'LA MAISON DU CERAME%' OR UPPER(nom) LIKE 'ESPANO CERAME%' OR UPPER(nom) LIKE 'GHORGHIZ CERAME%' OR UPPER(nom) LIKE 'CERAMICA DERSA%' OR UPPER(nom) LIKE 'BRILLO CERAME%' OR UPPER(nom) LIKE 'BRIQUETERIE BATI CHAOUIA%' OR UPPER(nom) LIKE 'TECHNO EMBALLE%' OR UPPER(nom) LIKE 'STATION KANTARI%' OR UPPER(nom) LIKE 'GHARB PAPIER CARTON%' OR UPPER(nom) LIKE 'KA PREFA%' OR UPPER(nom) LIKE 'FACEMAG%' OR UPPER(nom) LIKE 'DOMAINE ELBOURA%' OR UPPER(nom) LIKE 'FCE%' OR UPPER(nom) LIKE 'LYMOUNAMED%' OR UPPER(nom) LIKE 'M.F.P.O%' OR UPPER(nom) LIKE 'MATYSHA%' OR UPPER(nom) LIKE 'SODALMU%' OR UPPER(nom) LIKE 'DISTRA%' OR UPPER(nom) LIKE 'EURO MILL%' OR UPPER(nom) LIKE 'EXTA NOUVELLE%' OR UPPER(nom) LIKE 'AMA D%' OR UPPER(nom) LIKE 'OLEA CAPITAL%' OR UPPER(nom) LIKE 'PELAGIC PRO%' OR UPPER(nom) LIKE 'V.M.M%' OR UPPER(nom) LIKE 'VERALUB%');

-- Clients à 90 JOURS
UPDATE clients 
SET delai_paiement_jours = 90
WHERE point_de_vente_id = 5 
  AND (UPPER(nom) LIKE 'VETCAM%' OR UPPER(nom) LIKE 'MULTICERAME%' OR UPPER(nom) LIKE 'COOPERATIVE TRIFFAS%' OR UPPER(nom) LIKE 'LINA SOUSS%' OR UPPER(nom) LIKE 'MAREMBAL%' OR UPPER(nom) LIKE 'HMI%' OR UPPER(nom) LIKE 'QUALIAGRO%' OR UPPER(nom) LIKE 'EURO CERAMICA%');

-- Clients à 60 JOURS
UPDATE clients 
SET delai_paiement_jours = 60
WHERE point_de_vente_id = 5 
  AND (UPPER(nom) LIKE 'SOHOSNACKS%' OR UPPER(nom) LIKE 'TISCOP TRADING%' OR UPPER(nom) LIKE 'JACOB DELAFON%' OR UPPER(nom) LIKE 'DAMSA CONSERVERIE%' OR UPPER(nom) LIKE 'BERKANE PACKAGING%' OR UPPER(nom) LIKE 'SUNPACK%' OR UPPER(nom) LIKE 'TUNAMAX%' OR UPPER(nom) LIKE 'CONSERVERIES PELAGIQUES%' OR UPPER(nom) LIKE 'M%ASSERA BRAHIM ZNIBER%' OR UPPER(nom) LIKE 'PROVIDENCE VERTE%' OR UPPER(nom) LIKE 'NEXT SNACKING%' OR UPPER(nom) LIKE 'F.T.S.I%');

-- Autres clients (par défaut 30 jours si non renseigné)
UPDATE clients 
SET delai_paiement_jours = 30
WHERE point_de_vente_id = 5 
  AND (delai_paiement_jours IS NULL OR delai_paiement_jours <= 0);


-- ----------------------------------------------------------------------
-- 2. RECALCUL DES ÉCHÉANCES ET CONDITIONS SUR TOUTES LES FACTURES DU TENANT 5
-- ----------------------------------------------------------------------

UPDATE factures f
SET date_echeance = f.date_facture + (COALESCE(c.delai_paiement_jours, 30) || ' days')::interval,
    conditions_paiement = COALESCE(c.delai_paiement_jours, 30) || ' jours'
FROM clients c
WHERE f.client_id = c.id
  AND f.point_de_vente_id = 5;

-- ----------------------------------------------------------------------
-- 3. VÉRIFICATION DU RÉSULTAT SUR LES FACTURES RÉCENTES
-- ----------------------------------------------------------------------
SELECT 
    f.numero_facture,
    f.date_facture,
    c.nom AS client,
    c.delai_paiement_jours AS delai_client,
    f.date_echeance,
    f.conditions_paiement,
    f.montant_ttc
FROM factures f
JOIN clients c ON c.id = f.client_id
WHERE f.point_de_vente_id = 5
  AND f.numero_facture BETWEEN 'FA/2026/00420' AND 'FA/2026/00440'
ORDER BY f.numero_facture ASC;

-- Synthèse clients par délai
SELECT 
    delai_paiement_jours,
    COUNT(*) AS nombre_clients
FROM clients
WHERE point_de_vente_id = 5
GROUP BY delai_paiement_jours
ORDER BY delai_paiement_jours DESC;

COMMIT;

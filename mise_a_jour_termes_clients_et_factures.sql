-- ======================================================================
-- MISE À JOUR DES CLIENTS (TERMES DE PAIEMENT) & FACTURES TADBEER
-- Point de Vente ID : 5 (SIT TRANSMISSION SARL)
-- ======================================================================

BEGIN;

-- ----------------------------------------------------------------------
-- 1. ACTUALISATION DES DÉLAIS DE PAIEMENT DANS LA TABLE CLIENTS
-- ----------------------------------------------------------------------

-- Clients à 120 JOURS (Groupes Cimentiers/Céramiques, GPC, Agro, etc.)
UPDATE clients 
SET delai_paiement_jours = 120
WHERE point_de_vente_id = 5 
  AND (
    UPPER(nom) LIKE 'SUPER CERAME%' OR 
    UPPER(nom) LIKE 'GRAND CERAME%' OR 
    UPPER(nom) LIKE 'CRISTAL CERAME%' OR 
    UPPER(nom) LIKE 'LA MAISON DU CERAME%' OR 
    UPPER(nom) LIKE 'ESPANO CERAME%' OR 
    UPPER(nom) LIKE 'GHORGHIZ CERAME%' OR 
    UPPER(nom) LIKE 'CERAMICA DERSA%' OR 
    UPPER(nom) LIKE 'BRILLO CERAME%' OR 
    UPPER(nom) LIKE 'BRIQUETERIE BATI CHAOUIA%' OR 
    UPPER(nom) LIKE 'TECHNO EMBALLE%' OR 
    UPPER(nom) LIKE 'STATION KANTARI%' OR 
    UPPER(nom) LIKE 'GHARB PAPIER%' OR 
    UPPER(nom) LIKE 'GPC%' OR 
    UPPER(nom) LIKE 'KA PREFA%' OR 
    UPPER(nom) LIKE 'FACEMAG%' OR 
    UPPER(nom) LIKE 'DOMAINE ELBOURA%' OR 
    UPPER(nom) LIKE 'FCE%' OR 
    UPPER(nom) LIKE 'LYMOUNAMED%' OR 
    UPPER(nom) LIKE 'M.F.P.O%' OR 
    UPPER(nom) LIKE 'MFPO%' OR 
    UPPER(nom) LIKE 'MATYSHA%' OR 
    UPPER(nom) LIKE 'SODALMU%' OR 
    UPPER(nom) LIKE 'DISTRA%' OR 
    UPPER(nom) LIKE 'EURO MILL%' OR 
    UPPER(nom) LIKE 'EXTA NOUVELLE%' OR 
    UPPER(nom) LIKE 'AMA D%' OR 
    UPPER(nom) LIKE 'OLEA CAPITAL%' OR 
    UPPER(nom) LIKE 'PELAGIC PRO%' OR 
    UPPER(nom) LIKE 'V.M.M%' OR 
    UPPER(nom) LIKE 'VMM%' OR 
    UPPER(nom) LIKE 'VERALUB%'
  );

-- Clients à 90 JOURS
UPDATE clients 
SET delai_paiement_jours = 90
WHERE point_de_vente_id = 5 
  AND (
    UPPER(nom) LIKE 'VETCAM%' OR 
    UPPER(nom) LIKE 'MULTICERAME%' OR 
    UPPER(nom) LIKE 'COOPERATIVE TRIFFAS%' OR 
    UPPER(nom) LIKE 'TRIFFAS%' OR 
    UPPER(nom) LIKE 'LINA SOUSS%' OR 
    UPPER(nom) LIKE 'MAREMBAL%' OR 
    UPPER(nom) LIKE 'HMI%' OR 
    UPPER(nom) LIKE 'QUALIAGRO%' OR 
    UPPER(nom) LIKE 'EURO CERAMICA%'
  );

-- Clients à 60 JOURS
UPDATE clients 
SET delai_paiement_jours = 60
WHERE point_de_vente_id = 5 
  AND (
    UPPER(nom) LIKE 'SOHOSNACKS%' OR 
    UPPER(nom) LIKE 'TISCOP%' OR 
    UPPER(nom) LIKE 'JACOB DELAFON%' OR 
    UPPER(nom) LIKE 'DAMSA%' OR 
    UPPER(nom) LIKE 'BERKANE PACKAGING%' OR 
    UPPER(nom) LIKE 'SUNPACK%' OR 
    UPPER(nom) LIKE 'TUNAMAX%' OR 
    UPPER(nom) LIKE 'CONSERVERIES PELAGIQUES%' OR 
    UPPER(nom) LIKE 'M%ASSERA BRAHIM ZNIBER%' OR 
    UPPER(nom) LIKE 'PROVIDENCE VERTE%' OR 
    UPPER(nom) LIKE 'NEXT SNACKING%' OR 
    UPPER(nom) LIKE 'F.T.S.I%' OR 
    UPPER(nom) LIKE 'FTSI%'
  );

-- Autres clients (défaut 30 jours si non spécifié)
UPDATE clients 
SET delai_paiement_jours = 30
WHERE point_de_vente_id = 5 
  AND (delai_paiement_jours IS NULL OR delai_paiement_jours <= 0);


-- ----------------------------------------------------------------------
-- 2. RECALCUL DES ÉCHÉANCES ET CONDITIONS SUR TOUTES LES FACTURES DU TENANT 5
-- (Modifie les factures créées manuellement dans Tadbeer + factures importées)
-- ----------------------------------------------------------------------

UPDATE factures f
SET date_echeance = f.date_facture + (COALESCE(c.delai_paiement_jours, 30) || ' days')::interval,
    conditions_paiement = COALESCE(c.delai_paiement_jours, 30) || ' jours'
FROM clients c
WHERE f.client_id = c.id
  AND f.point_de_vente_id = 5;


-- ----------------------------------------------------------------------
-- 3. CALAGE DE LA SÉQUENCE DE NUMÉROTATION DES FACTURES DANS TADBEER
-- (Pour que la prochaine facture créée soit >= FA/2026/00441 sans collision)
-- ----------------------------------------------------------------------

UPDATE codifications_document 
SET dernier_numero = GREATEST(
      dernier_numero, 
      440,
      COALESCE((
        SELECT MAX(SUBSTRING(numero_facture FROM '[0-9]+$')::int) 
        FROM factures 
        WHERE point_de_vente_id = 5 AND numero_facture ~ '^FA/[0-9]{4}/[0-9]+$'
      ), 440)
    ), 
    date_derniere_maj = NOW() 
WHERE point_de_vente_id = 5 
  AND type_document = 'FACTURE_CLIENT';


-- ----------------------------------------------------------------------
-- 4. VÉRIFICATION DU RÉSULTAT
-- ----------------------------------------------------------------------

-- A. Vérification sur les factures récentes (notamment 00433, 00434, 00436 et autour)
SELECT 
    f.numero_facture,
    f.date_facture,
    c.nom AS client,
    c.delai_paiement_jours AS delai_terme_jours,
    f.date_echeance,
    f.conditions_paiement,
    f.montant_ttc
FROM factures f
JOIN clients c ON c.id = f.client_id
WHERE f.point_de_vente_id = 5
  AND f.numero_facture BETWEEN 'FA/2026/00423' AND 'FA/2026/00445'
ORDER BY f.numero_facture ASC;

-- B. Répartition globale des clients par délai de paiement
SELECT 
    COALESCE(delai_paiement_jours, 30) AS delai_jours,
    COUNT(*) AS nombre_de_clients
FROM clients
WHERE point_de_vente_id = 5
GROUP BY delai_paiement_jours
ORDER BY delai_jours DESC;

COMMIT;

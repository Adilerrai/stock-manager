-- Migration : Ajout des colonnes manquantes dans la table 'stocks'
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS quantite_disponible NUMERIC(12, 2) DEFAULT 0.00;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS quantite_reservee NUMERIC(12, 2) DEFAULT 0.00;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS seuil_alerte NUMERIC(12, 2) DEFAULT 0.00;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS derniere_maj TIMESTAMP DEFAULT NOW();

-- Synchronisation des stocks depuis les qualités si présentes
UPDATE stocks s
SET quantite_disponible = sub.total
FROM (
    SELECT stock_id, SUM(COALESCE(quantite_disponible, 0)) AS total
    FROM stock_qualites
    GROUP BY stock_id
) sub
WHERE s.id = sub.stock_id AND (s.quantite_disponible IS NULL OR s.quantite_disponible = 0);

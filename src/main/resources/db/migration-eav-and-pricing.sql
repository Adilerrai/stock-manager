-- Migration script: Transition to EAV JSONB model and pricing extensions (HT, TTC, PPV, PPH)

-- 1. Add attributes JSONB column and pricing columns
ALTER TABLE produits ADD COLUMN IF NOT EXISTS attributes jsonb DEFAULT '{}';
ALTER TABLE produits ADD COLUMN IF NOT EXISTS prix_achat_ht numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS prix_achat_ttc numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS prix_vente_ht numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS prix_vente_ttc numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS ppv_ht numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS ppv_ttc numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS pph_ht numeric(19, 2);
ALTER TABLE produits ADD COLUMN IF NOT EXISTS pph_ttc numeric(19, 2);

-- 2. Migrate existing tile data into the JSONB map
UPDATE produits
SET attributes = jsonb_strip_nulls(jsonb_build_object(
    'longueurCm', longueur_cm,
    'largeurCm', largeur_cm,
    'epaisseurMm', epaisseur_mm,
    'format', format,
    'couleur', couleur,
    'texture', texture,
    'finition', finition,
    'origine', origine,
    'serie', serie,
    'surfaceParBoiteM2', surface_par_boite_m2,
    'quantiteParBoite', quantite_par_boite
))
WHERE (longueur_cm IS NOT NULL 
   OR largeur_cm IS NOT NULL 
   OR format IS NOT NULL 
   OR couleur IS NOT NULL 
   OR finition IS NOT NULL 
   OR surface_par_boite_m2 IS NOT NULL);

-- 3. Migrate existing price columns to new TTC columns
UPDATE produits 
SET prix_achat_ttc = prix_achat, 
    prix_vente_ttc = prix_vente 
WHERE prix_achat IS NOT NULL OR prix_vente IS NOT NULL;

-- 4. Clean up old ceramic specific columns
ALTER TABLE produits DROP COLUMN IF EXISTS longueur_cm;
ALTER TABLE produits DROP COLUMN IF EXISTS largeur_cm;
ALTER TABLE produits DROP COLUMN IF EXISTS epaisseur_mm;
ALTER TABLE produits DROP COLUMN IF EXISTS format;
ALTER TABLE produits DROP COLUMN IF EXISTS couleur;
ALTER TABLE produits DROP COLUMN IF EXISTS texture;
ALTER TABLE produits DROP COLUMN IF EXISTS finition;
ALTER TABLE produits DROP COLUMN IF EXISTS origine;
ALTER TABLE produits DROP COLUMN IF EXISTS serie;
ALTER TABLE produits DROP COLUMN IF EXISTS surface_par_boite_m2;
ALTER TABLE produits DROP COLUMN IF EXISTS quantite_par_boite;

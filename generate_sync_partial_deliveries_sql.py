import json
import os

OUT_SQL = "sync_reliquats_livraisons_partielles_tenant5.sql"
TENANT_ID = 5


def main():
    with open("odoo_all_bls_detailed.json", "r", encoding="utf-8") as f:
        bls = json.load(f)

    waiting_bls = [b["numero_bl"] for b in bls if b.get("statut_odoo") in ("confirmed", "assigned", "waiting", "draft")]
    cancel_bls = [b["numero_bl"] for b in bls if b.get("statut_odoo") == "cancel"]
    done_bls = [b["numero_bl"] for b in bls if b.get("statut_odoo") == "done"]

    print(f"Total BLs Odoo: {len(bls)}")
    print(f"  - Terminé (done) : {len(done_bls)}")
    print(f"  - En attente (waiting/confirmed/assigned) : {len(waiting_bls)}")
    print(f"  - Annulé (cancel) : {len(cancel_bls)}")

    lines = []
    lines.append("-- =============================================================================")
    lines.append("-- SYNCHRONISATION DES LIVRAISONS PARTIELLES, RELIQUATS ET STATUTS TADBEER")
    lines.append(f"-- Tenant : #{TENANT_ID} (SIT TRANSMISSION SARL)")
    lines.append("-- =============================================================================")
    lines.append("\\set ON_ERROR_STOP on")
    lines.append("BEGIN;\n")

    # 1. Mettre à jour les BLs En Attente
    waiting_sql_list = ", ".join(f"'{n}'" for n in sorted(waiting_bls))
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"-- 1. PASSAGE DES {len(waiting_bls)} BLs EN ATTENTE (NON EXPÉDIÉS) AU STATUT 'EN_ATTENTE'")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
UPDATE bons_livraison_client
SET statut = 'EN_ATTENTE', montant_total = 0.00
WHERE numero_bl IN ({waiting_sql_list}) AND point_de_vente_id = {TENANT_ID};

UPDATE lignes_bon_livraison_client
SET quantite_livree = 0.000, montant_ligne = 0.00
WHERE bon_livraison_client_id IN (
    SELECT id FROM bons_livraison_client 
    WHERE numero_bl IN ({waiting_sql_list}) AND point_de_vente_id = {TENANT_ID}
);
""")

    # 2. Mettre à jour les BLs Annulés
    cancel_sql_list = ", ".join(f"'{n}'" for n in sorted(cancel_bls))
    lines.append("\n-- -----------------------------------------------------------------------------")
    lines.append(f"-- 2. PASSAGE DES {len(cancel_bls)} BLs ANNULÉS AU STATUT 'ANNULEE'")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
UPDATE bons_livraison_client
SET statut = 'ANNULEE', montant_total = 0.00
WHERE numero_bl IN ({cancel_sql_list}) AND point_de_vente_id = {TENANT_ID};

UPDATE lignes_bon_livraison_client
SET quantite_livree = 0.000, montant_ligne = 0.00
WHERE bon_livraison_client_id IN (
    SELECT id FROM bons_livraison_client 
    WHERE numero_bl IN ({cancel_sql_list}) AND point_de_vente_id = {TENANT_ID}
);
""")

    # 3. Mettre à jour les BLs Livrés (Terminés)
    done_sql_list = ", ".join(f"'{n}'" for n in sorted(done_bls))
    lines.append("\n-- -----------------------------------------------------------------------------")
    lines.append(f"-- 3. CONFIRMATION DU STATUT 'LIVREE' POUR LES {len(done_bls)} BLs EFFECTIVEMENT EXPÉDIÉS")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
UPDATE bons_livraison_client
SET statut = 'LIVREE'
WHERE numero_bl IN ({done_sql_list}) AND point_de_vente_id = {TENANT_ID};
""")

    # 4. Recalculer les quantités livrées réelles et les reliquats sur chaque ligne de commande client
    lines.append("\n-- -----------------------------------------------------------------------------")
    lines.append("-- 4. RECALCUL STRICT DES QUANTITÉS LIVRÉES ET RELIQUATS SUR LES LIGNES DE COMMANDES")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
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
      AND bl.point_de_vente_id = {TENANT_ID}
), 0.000)
WHERE lcc.commande_client_id IN (
    SELECT id FROM commandes_client WHERE point_de_vente_id = {TENANT_ID}
);

-- Recalcul du reliquat restant à livrer
UPDATE lignes_commande_client
SET quantite_reliquat = GREATEST(0.000, quantite_commandee - quantite_livree)
WHERE commande_client_id IN (
    SELECT id FROM commandes_client WHERE point_de_vente_id = {TENANT_ID}
);
""")

    # 5. Mettre à jour le statut réel de chaque commande client
    lines.append("\n-- -----------------------------------------------------------------------------")
    lines.append("-- 5. MISE À JOUR DES STATUTS COMMANDES : 'LIVREE', 'LIVREE_PARTIELLE' OU 'CONFIRMEE'")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
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
WHERE cc.point_de_vente_id = {TENANT_ID}
  AND cc.statut NOT IN ('ANNULEE', 'BROUILLON');
""")

    lines.append("\nCOMMIT;\n")

    # 6. Vérifications
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append("-- 6. CONTRÔLE FINAL : DÉTAIL COMMANDE S00264 & SYNTHÈSE DES STATUTS")
    lines.append("-- -----------------------------------------------------------------------------")
    lines.append(f"""
\\echo '========================================================================='
\\echo '1. DÉTAIL COMMANDE S00264 (SUPER CERAME CASA)'
\\echo '========================================================================='
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
WHERE c.numero_commande = 'S00264' AND c.point_de_vente_id = {TENANT_ID};

\\echo '========================================================================='
\\echo '2. LES 4 BLS RATTACHÉS À S00264 ET LEUR STATUT'
\\echo '========================================================================='
SELECT 
    bl.numero_bl,
    bl.statut AS statut_bl,
    bl.date_bl::date AS date_bl,
    lbl.quantite_livree
FROM bons_livraison_client bl
JOIN bons_livraison_client_commandes blc ON blc.bon_livraison_id = bl.id
JOIN commandes_client c ON c.id = blc.commande_id
JOIN lignes_bon_livraison_client lbl ON lbl.bon_livraison_client_id = bl.id
WHERE c.numero_commande = 'S00264' AND c.point_de_vente_id = {TENANT_ID}
ORDER BY bl.date_bl;

\\echo '========================================================================='
\\echo '3. RÉPARTITION DES STATUTS DE COMMANDES CLIENTS APRÈS SYNCHRO'
\\echo '========================================================================='
SELECT statut, COUNT(*) AS nb_commandes
FROM commandes_client
WHERE point_de_vente_id = {TENANT_ID}
GROUP BY statut
ORDER BY nb_commandes DESC;
""")

    with open(OUT_SQL, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Generated {OUT_SQL}")


if __name__ == "__main__":
    main()


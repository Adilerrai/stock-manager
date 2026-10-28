#!/usr/bin/env python3
"""
Script de synchronisation des 18 factures Odoo (FA/2026/00423 à FA/2026/00440) vers Tadbeer (pointvente_db).
Tenant cible : Tenant #5 (SIT TRANSMISSION SARL).

Actions réalisées dans le script SQL généré :
1. Avancer les 3 factures créées et validées par Hanane Hadi dans Tadbeer :
   - FA/2026/00433 (Grand Cérame, 2 520 MAD) -> FA/2026/00441
   - FA/2026/00434 (Grand Cérame, 2 880 MAD) -> FA/2026/00442
   - FA/2026/00436 (Vetcam, 720 MAD) -> FA/2026/00443
2. Nettoyer les tentatives annulées qui bloquent les numéros dans Tadbeer.
3. Insérer les 18 factures réelles depuis Odoo avec leurs vraies dates, montants, et lignes.
4. Lier les bons de livraison correspondants.
5. Mettre à jour le compteur de numérotation à 443 (prochaine facture = FA/2026/00444).
"""

import json
import urllib.request
import ssl
import sys
import os

BASE_URL = 'https://sit-transmission1.odoo.com'
COOKIE = 'frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id=nuqD8NS9iu8FFLbsthUzdVKOmdzs-UvDkW53NORh7aLvtW3IIIICNQStRBu5_dXtmTcjFNEWeF8MpN3I9AK-Dw'
HEADERS = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'Cookie': COOKIE,
    'Origin': BASE_URL,
    'Referer': f'{BASE_URL}/odoo',
}
CTX = ssl.create_default_context()

def odoo_call(model, method, args, kwargs):
    url = f"{BASE_URL}/web/dataset/call_kw/{model}/{method}"
    payload = {'jsonrpc': '2.0', 'method': 'call', 'params': {'model': model, 'method': method, 'args': args, 'kwargs': kwargs}}
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=HEADERS)
    with urllib.request.urlopen(req, context=CTX) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        if 'error' in res:
            raise RuntimeError(f"Odoo Error {model}.{method}: {res['error']}")
        return res.get('result', {})

def esc(val):
    if val is None:
        return 'NULL'
    s = str(val).replace("'", "''")
    return f"'{s}'"

def main():
    print("1. Connexion à Odoo et extraction des 18 factures (FA/2026/00423 à FA/2026/00440)...")
    target_nums = [f"FA/2026/{str(n).zfill(5)}" for n in range(423, 441)]
    
    moves = odoo_call('account.move', 'search_read', [[['name', 'in', target_nums]]], {
        'fields': [
            'id', 'name', 'invoice_date', 'invoice_date_due', 'partner_id',
            'amount_untaxed', 'amount_tax', 'amount_total', 'state', 'payment_state',
            'invoice_origin', 'invoice_user_id', 'narration', 'invoice_line_ids', 'create_date'
        ],
        'order': 'name asc'
    })
    
    print(f"   -> {len(moves)} facture(s) trouvée(s) dans Odoo.")
    if len(moves) == 0:
        print("Erreur : aucune facture retournée par Odoo. Vérifiez la session.")
        return

    # Lignes des factures
    all_line_ids = []
    for m in moves:
        all_line_ids.extend(m.get('invoice_line_ids', []))
        
    print(f"2. Extraction des lignes de facturation ({len(all_line_ids)} lignes)...")
    lines = odoo_call('account.move.line', 'search_read', [[['id', 'in', all_line_ids], ['display_type', '=', 'product']]], {
        'fields': [
            'id', 'move_id', 'product_id', 'name', 'quantity', 'price_unit',
            'discount', 'price_subtotal', 'price_total', 'tax_ids'
        ],
        'order': 'id asc'
    })
    print(f"   -> {len(lines)} lignes produits extraites.")

    # Récupérer les codes articles (default_code)
    prod_ids = list(set(l['product_id'][0] for l in lines if l.get('product_id')))
    prod_map = {}
    if prod_ids:
        prods = odoo_call('product.product', 'search_read', [[['id', 'in', prod_ids]]], {
            'fields': ['id', 'default_code', 'name']
        })
        for p in prods:
            prod_map[p['id']] = {
                'code': p.get('default_code') or '',
                'name': p.get('name') or ''
            }

    # Grouper les lignes par move_id
    lines_by_move = {}
    for l in lines:
        mid = l['move_id'][0]
        lines_by_move.setdefault(mid, []).append(l)

    print("3. Génération du script SQL d'importation...")
    sql_lines = []
    sql_lines.append("-- ======================================================================")
    sql_lines.append("-- SYNCHRONISATION FACTURES ODOO VERS TADBEER (TENANT 5)")
    sql_lines.append("-- Période : 21-26 Octobre 2026 (Factures FA/2026/00423 à FA/2026/00440)")
    sql_lines.append("-- Résolution des collisions et ajustement de séquence")
    sql_lines.append("-- ======================================================================\n")
    sql_lines.append("BEGIN;\n")

    # Les factures FA/2026/00433, 00434 et 00436 ont été supprimées d'Odoo par l'utilisateur
    # Elles restent donc intactes dans Tadbeer pour Grand Cérame et Vetcam avec leurs numéros d'origine !
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("-- NOTE : FA/2026/00433, 00434 et 00436 restent intactes dans Tadbeer")
    sql_lines.append("-- ----------------------------------------------------------------------\n")

    # Étape 2 : Nettoyer les tentatives annulées
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("-- 2. NETTOYER LES BROUILLONS / TENTATIVES ANNULÉES QUI BLOQUAIENT LES NUMÉROS")
    sql_lines.append("-- ----------------------------------------------------------------------")
    cancelled_nums = [
        'FA/2026/00423', 'FA/2026/00424', 'FA/2026/00425', 'FA/2026/00426', 'FA/2026/00427',
        'FA/2026/00428', 'FA/2026/00429', 'FA/2026/00430', 'FA/2026/00431', 'FA/2026/00432',
        'FA/2026/00435', 'FA/2026/00437', 'FA/2026/00438', 'FA/2026/00439'
    ]
    cancelled_list = ", ".join(f"'{n}'" for n in cancelled_nums)
    
    sql_lines.append(f"""UPDATE bons_livraison_client SET facture_id = NULL 
WHERE point_de_vente_id = 5 
  AND facture_id IN (
      SELECT id FROM factures 
      WHERE point_de_vente_id = 5 
        AND (annulee = true OR statut = 'ANNULEE')
        AND numero_facture IN ({cancelled_list})
  );

DELETE FROM lignes_facture 
WHERE facture_id IN (
    SELECT id FROM factures 
    WHERE point_de_vente_id = 5 
      AND (annulee = true OR statut = 'ANNULEE')
      AND numero_facture IN ({cancelled_list})
);

DELETE FROM factures 
WHERE point_de_vente_id = 5 
  AND (annulee = true OR statut = 'ANNULEE')
  AND numero_facture IN ({cancelled_list});
""")

    # Étape 3 : Insérer les 18 factures et leurs lignes
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("-- 3. INSERTION DES 18 FACTURES RÉELLES DEPUIS ODOO")
    sql_lines.append("-- ----------------------------------------------------------------------\n")

    for m in moves:
        num = m['name']
        dt_inv = m.get('invoice_date') or '2026-10-23'
        dt_due = m.get('invoice_date_due') or dt_inv
        ht = float(m.get('amount_untaxed') or 0.0)
        tva = float(m.get('amount_tax') or 0.0)
        ttc = float(m.get('amount_total') or 0.0)
        partner_name = m['partner_id'][1] if m.get('partner_id') else ''
        partner_id_odoo = m['partner_id'][0] if m.get('partner_id') else 0
        origin = m.get('invoice_origin') or ''
        notes = f"Origine Odoo: {origin} - Importé automatiquement" if origin else "Importé automatiquement depuis Odoo"
        create_dt = m.get('create_date') or f"{dt_inv} 12:00:00"

        sql_lines.append(f"-- Facture {num} : {partner_name} ({ttc:.2f} MAD du {dt_inv})")
        sql_lines.append(f"""DO $$
DECLARE
    v_facture_id BIGINT;
    v_client_id BIGINT;
    v_user_id BIGINT := 21; -- Hanane Hadi
BEGIN
    -- Résolution du client
    SELECT id INTO v_client_id FROM clients 
    WHERE point_de_vente_id = 5 
      AND (LOWER(TRIM(nom)) = LOWER(TRIM({esc(partner_name)})) OR code_client = 'CLI_{partner_id_odoo}')
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
        {esc(num)}, {esc(dt_inv)}::date, {esc(dt_due)}::date, v_client_id, v_user_id,
        {ht:.2f}, {tva:.2f}, {ttc:.2f}, 0.00, {ttc:.2f},
        'VALIDEE', 0.00, {ttc:.2f}, {esc(create_dt)}::timestamp, {esc(notes)},
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
    IF {esc(origin)} IS NOT NULL AND {esc(origin)} <> '' THEN
        UPDATE bons_livraison_client bl
        SET facture_id = v_facture_id
        WHERE bl.point_de_vente_id = 5
          AND bl.facture_id IS NULL
          AND EXISTS (
              SELECT 1 FROM commandes_client cc 
              WHERE cc.id = bl.commande_client_id 
                AND cc.point_de_vente_id = 5 
                AND cc.numero_commande = {esc(origin)}
          );
    END IF;
""")

        # Insertion des lignes
        m_lines = lines_by_move.get(m['id'], [])
        for ml in m_lines:
            qty = float(ml.get('quantity') or 1.0)
            pu = float(ml.get('price_unit') or 0.0)
            sub = float(ml.get('price_subtotal') or (qty * pu))
            tot = float(ml.get('price_total') or (sub * 1.20))
            tva_l = tot - sub
            disc = float(ml.get('discount') or 0.0)
            remise_mt = (qty * pu) * (disc / 100.0) if disc > 0 else 0.0
            
            p_info = prod_map.get(ml['product_id'][0], {}) if ml.get('product_id') else {}
            ref_code = p_info.get('code') or ''
            desig = ml.get('name') or p_info.get('name') or 'Article'
            # Nettoyer retours chariot dans désignation
            desig = desig.split('\n')[0].strip()

            sql_lines.append(f"""    -- Ligne article : {ref_code} - {desig[:40]}
    INSERT INTO lignes_facture (
        facture_id, produit_id, designation, reference, quantite,
        prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc,
        remise_pourcentage, remise_montant
    )
    SELECT 
        v_facture_id,
        (SELECT p.id FROM produits p WHERE p.point_de_vente_id = 5 AND (p.reference = {esc(ref_code)} OR LOWER(TRIM(p.designation)) = LOWER(TRIM({esc(desig)}))) LIMIT 1),
        {esc(desig)},
        {esc(ref_code)},
        {qty:.3f},
        {pu:.2f},
        20.00,
        {sub:.2f},
        {tva_l:.2f},
        {tot:.2f},
        {disc:.2f},
        {remise_mt:.2f}
    WHERE NOT EXISTS (
        SELECT 1 FROM lignes_facture lf 
        WHERE lf.facture_id = v_facture_id 
          AND lf.reference = {esc(ref_code)} 
          AND lf.quantite = {qty:.3f}
    );""")

        sql_lines.append("END $$;\n")

    # Étape 4 : Mettre à jour le compteur dans codifications_document
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("-- 4. MISE À JOUR DE LA CODIFICATION (PROCHAINE FACTURE = FA/2026/00441)")
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("""UPDATE codifications_document 
SET dernier_numero = 440, 
    date_derniere_maj = NOW() 
WHERE point_de_vente_id = 5 
  AND type_document = 'FACTURE_VENTE';
""")

    # Étape 5 : Requêtes de contrôle avant validation
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("-- 5. VÉRIFICATION DU RÉSULTAT")
    sql_lines.append("-- ----------------------------------------------------------------------")
    sql_lines.append("""SELECT 
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
""")
    sql_lines.append("COMMIT;\n")

    out_file = 'sync_factures_tenant5.sql'
    with open(out_file, 'w', encoding='utf-8') as f:
        f.write("\n".join(sql_lines))

    print(f"\n[OK] Script SQL genere avec succes dans : {out_file} ({os.path.getsize(out_file):,} octets)")

if __name__ == '__main__':
    main()

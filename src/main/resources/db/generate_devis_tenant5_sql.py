import os
import sys
import re
import json
import urllib.request
import ssl
from datetime import datetime

# Windows console UTF-8
sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')

BASE_URL = 'https://sit-transmission1.odoo.com'
CTX = ssl.create_default_context()

# Valeur par défaut (à renouveler au besoin)
DEFAULT_COOKIE = 'frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id=nuqD8NS9iu8FFLbsthUzdVKOmdzs-UvDkW53NORh7aLvtW3IIIICNQStRBu5_dXtmTcjFNEWeF8MpN3I9AK-Dw'

def get_headers(cookie):
    return {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36',
        'Cookie': cookie,
        'Origin': BASE_URL,
        'Referer': f'{BASE_URL}/odoo/sales',
    }

def odoo_call(model, method, args, kwargs, headers):
    url = f"{BASE_URL}/web/dataset/call_kw/{model}/{method}"
    payload = {
        "jsonrpc": "2.0",
        "method": "call",
        "params": {
            "model": model,
            "method": method,
            "args": args,
            "kwargs": kwargs
        }
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers)
    try:
        with urllib.request.urlopen(req, context=CTX) as resp:
            res = json.loads(resp.read().decode('utf-8'))
            if "error" in res:
                err = res["error"]
                if isinstance(err, dict) and (err.get("code") == 100 or "Session Expired" in str(err)):
                    raise RuntimeError("SESSION ODOO EXPIRÉE ! Veuillez fournir une nouvelle session_id.")
                print(f"[ERREUR Odoo {model}.{method}]: {err}")
                return []
            return res.get("result", {})
    except urllib.error.HTTPError as e:
        print(f"[HTTP Error {e.code}]: {e.read().decode('utf-8', errors='ignore')}")
        return []

def sql_escape(val, max_len=None):
    if val is None or val is False:
        return "NULL"
    s = str(val).strip()
    if s == "" or s.lower() == "none" or s.lower() == "false" or s == '-':
        return "NULL"
    if max_len and len(s) > max_len:
        s = s[:max_len]
    s = s.replace("'", "''")
    return f"'{s}'"

def date_val(val):
    if val is None or val is False:
        return "CURRENT_DATE"
    s = str(val).strip()
    if not s or s.lower() == 'none' or s == '-':
        return "CURRENT_DATE"
    m = re.match(r'^(\d{4}-\d{2}-\d{2})', s)
    if m:
        return f"'{m.group(1)}'"
    return "CURRENT_DATE"

def timestamp_val(val):
    if val is None or val is False:
        return "CURRENT_TIMESTAMP"
    s = str(val).strip()
    if not s or s.lower() == 'none' or s == '-':
        return "CURRENT_TIMESTAMP"
    m = re.match(r'^(\d{4}-\d{2}-\d{2})[T ]?(\d{2}:\d{2}:\d{2})?', s)
    if m:
        d = m.group(1)
        t = m.group(2) if m.group(2) else "00:00:00"
        return f"'{d} {t}'"
    return "CURRENT_TIMESTAMP"

def parse_code_nom(display_name):
    code = ""
    nom = display_name or ""
    if "[" in nom and "]" in nom:
        try:
            code = nom.split("]")[0].replace("[", "").strip()
            nom = nom.split("]", 1)[1].strip()
        except:
            pass
    return code, nom

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Extraction des DEVIS Odoo (uniquement state in draft/sent) et génération du script SQL pour Tenant 5")
    parser.add_argument("--cookie", help="Cookie de session complet", default=None)
    parser.add_argument("--session-id", help="Session ID Odoo (ex: nuqD8NS9...)", default=None)
    parser.add_argument("--output", help="Chemin du fichier SQL de sortie", default=None)
    args = parser.parse_args()

    # Détermination du cookie
    if args.cookie:
        cookie = args.cookie
    elif args.session_id:
        cookie = f"frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id={args.session_id.strip()}"
    else:
        cookie = DEFAULT_COOKIE

    headers = get_headers(cookie)
    TENANT_ID = 5
    TENANT_NAME = "SIT TRANSMISSION SARL"

    output_sql_path = args.output or os.path.join(
        os.path.dirname(__file__),
        "import_sit_transmission_devis_tenant5.sql"
    )

    print("=" * 70)
    print("EXTRACTION DES DEVIS ODOO (UNIQUEMENT DEVIS - PAS DE COMMANDES)")
    print(f"Cible : Tenant #{TENANT_ID} ({TENANT_NAME})")
    print(f"Filtre Odoo : state IN ('draft', 'sent')")
    print("=" * 70)

    # 1. Test de connexion & Récupération des Devis
    domain_devis = [["state", "in", ["draft", "sent"]]]
    
    print("\n1. Récupération des devis depuis Odoo (sale.order state in ['draft', 'sent'])...")
    try:
        devis_data = odoo_call(
            "sale.order", "search_read",
            [domain_devis],
            {
                "fields": [
                    "name", "partner_id", "date_order", "create_date", "validity_date",
                    "amount_untaxed", "amount_tax", "amount_total", "state",
                    "x_studio_n_de_commande_client", "client_order_ref", "note",
                    "order_line", "user_id"
                ],
                "limit": 10000,
                "order": "date_order asc, id asc"
            },
            headers
        )
    except RuntimeError as e:
        print(f"\n[ERREUR] : {e}")
        print("Pour lancer le script avec une session fraîche :")
        print("python generate_devis_tenant5_sql.py --session-id VOTRE_NOUVELLE_SESSION_ID")
        sys.exit(1)

    if not isinstance(devis_data, list):
        print("[ERREUR] Impossible de récupérer les devis depuis Odoo.")
        sys.exit(1)

    print(f"Devis Odoo récupérés : {len(devis_data)}")

    if not devis_data:
        print("Aucun devis en état draft/sent trouvé dans Odoo.")
        sys.exit(0)

    # 2. Récupération des lignes de devis (sale.order.line)
    so_ids = [d["id"] for d in devis_data]
    print(f"\n2. Récupération des lignes pour les {len(so_ids)} devis...")
    lines_by_so = {}
    total_lines_count = 0

    for i in range(0, len(so_ids), 250):
        batch = so_ids[i:i + 250]
        lines_batch = odoo_call(
            "sale.order.line", "search_read",
            [[["order_id", "in", batch]]],
            {
                "fields": [
                    "order_id", "product_id", "name", "product_uom_qty",
                    "price_unit", "discount", "price_subtotal", "price_total",
                    "display_type"
                ],
                "limit": 10000
            },
            headers
        )
        if isinstance(lines_batch, list):
            for l in lines_batch:
                oid = l.get("order_id")[0] if isinstance(l.get("order_id"), (list, tuple)) else l.get("order_id")
                if oid not in lines_by_so:
                    lines_by_so[oid] = []
                lines_by_so[oid].append(l)
                total_lines_count += 1

    print(f"Lignes de devis récupérées : {total_lines_count}")

    # 3. Construction du script SQL
    print(f"\n3. Génération du script SQL d'insertion dans le Tenant #{TENANT_ID}...")
    sql_lines = []
    sql_lines.append("\\set ON_ERROR_STOP on\n")
    sql_lines.append("-- =============================================================================")
    sql_lines.append(f"-- SCRIPT D'IMPORTATION DES DEVIS ODOO -> TENANT #{TENANT_ID} : {TENANT_NAME}")
    sql_lines.append(f"-- Généré le : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_lines.append(f"-- Filtre strict : Uniquement les Devis (state IN ('draft', 'sent'))")
    sql_lines.append("-- Sécurité : Isolation stricte Tenant #5. Aucune altération des autres tenants.")
    sql_lines.append("-- =============================================================================\n")
    sql_lines.append("BEGIN;\n")

    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 1. INSERTION SÉCURISÉE DES DEVIS CLIENTS (AUCUNE SUPPRESSION - TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    nb_devis = 0
    nb_lignes_inserees = 0

    for d in devis_data:
        d_id = d["id"]
        num_devis = str(d.get("name") or f"DEV-{d_id}").strip()
        e_num = num_devis[:100].replace("'", "''")

        dt_devis = date_val(d.get("date_order") or d.get("create_date"))
        dt_val = date_val(d.get("validity_date"))
        dt_creation = timestamp_val(d.get("create_date") or d.get("date_order"))

        p_obj = d.get("partner_id")
        p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
        e_pname = p_name[:200].replace("'", "''")
        client_sub = f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        ht = float(d.get("amount_untaxed") or 0.0)
        tva = float(d.get("amount_tax") or 0.0)
        ttc = float(d.get("amount_total") or (ht + tva))

        st_raw = str(d.get("state") or "").lower()
        statut_devis = "ENVOYE" if st_raw == "sent" else "BROUILLON"

        ref_achat = d.get("x_studio_n_de_commande_client") or d.get("client_order_ref") or ""
        e_ref_achat = sql_escape(ref_achat, 100)
        notes = d.get("note") or ""
        e_notes = sql_escape(notes)

        # Insertion entête du devis sans écraser ni supprimer
        sql_lines.append(
            f"INSERT INTO devis (numero_devis, date_devis, date_validite, client_id, cree_par_user_id, "
            f"montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, "
            f"notes, numero_achat_client, point_de_vente_id, date_creation) "
            f"VALUES ('{e_num}', {dt_devis}, {dt_val}, COALESCE({client_sub}, (SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
            f"(SELECT id FROM users LIMIT 1), {ht:.2f}, {tva:.2f}, {ttc:.2f}, 0.00, {ttc:.2f}, '{statut_devis}', "
            f"{e_notes}, {e_ref_achat}, {TENANT_ID}, {dt_creation}) "
            f"ON CONFLICT (point_de_vente_id, numero_devis) DO NOTHING;"
        )
        nb_devis += 1

        # Insertion des lignes du devis uniquement si non déjà présentes
        for l in lines_by_so.get(d_id, []):
            if l.get("display_type"):
                continue  # Titre de section ou note textuelle Odoo

            prod_obj = l.get("product_id")
            p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(l.get("name") or "")
            c_code, c_nom = parse_code_nom(p_display)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            qte = float(l.get("product_uom_qty") or 1.0)
            pu = float(l.get("price_unit") or 0.0)
            remise_pct = float(l.get("discount") or 0.0)
            mt_ht = float(l.get("price_subtotal") or round(qte * pu * (1.0 - remise_pct / 100.0), 2))
            mt_tva = round(mt_ht * 0.20, 2)
            mt_ttc = round(mt_ht + mt_tva, 2)
            desc = str(l.get("name") or "")[:255].replace("'", "''")

            resolved_prod = f"COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1))"

            sql_lines.append(
                f"INSERT INTO lignes_devis (devis_id, produit_id, quantite, prix_unitaire_ht, taux_tva, "
                f"taux_remise, montant_ht, montant_tva, montant_ttc, description, statut_precommande) "
                f"SELECT d.id, {resolved_prod}, "
                f"{qte:.3f}, {pu:.2f}, 20.00, {remise_pct:.2f}, {mt_ht:.2f}, {mt_tva:.2f}, {mt_ttc:.2f}, '{desc}', 'NON_ENGAGE' "
                f"FROM devis d "
                f"WHERE d.numero_devis = '{e_num}' AND d.point_de_vente_id = {TENANT_ID} "
                f"AND NOT EXISTS ("
                f"  SELECT 1 FROM lignes_devis ld "
                f"  WHERE ld.devis_id = d.id AND ld.produit_id = {resolved_prod} AND ld.quantite = {qte:.3f}"
                f");"
            )
            nb_lignes_inserees += 1

    # Recalcul final des totaux depuis les lignes insérées
    sql_lines.append(f"""
-- Synchronisation finale des totaux des devis à partir des lignes réelles
UPDATE devis d
SET montant_ht = COALESCE((SELECT SUM(ld.montant_ht) FROM lignes_devis ld WHERE ld.devis_id = d.id), d.montant_ht),
    montant_tva = COALESCE((SELECT SUM(ld.montant_tva) FROM lignes_devis ld WHERE ld.devis_id = d.id), d.montant_tva),
    montant_ttc = COALESCE((SELECT SUM(ld.montant_ttc) FROM lignes_devis ld WHERE ld.devis_id = d.id), d.montant_ttc),
    montant_final = COALESCE((SELECT SUM(ld.montant_ttc) FROM lignes_devis ld WHERE ld.devis_id = d.id), d.montant_ttc)
WHERE d.point_de_vente_id = {TENANT_ID};

-- =============================================================================
-- VÉRIFICATION FINALE DES LIGNES ET TOTAUX (TENANT #{TENANT_ID})
-- =============================================================================
SELECT 
    COUNT(DISTINCT d.id) AS total_devis_tenant5,
    COUNT(ld.id) AS total_lignes_devis_tenant5,
    ROUND(COALESCE(SUM(ld.montant_ht), 0.00), 2) AS total_ht_mad,
    ROUND(COALESCE(SUM(ld.montant_ttc), 0.00), 2) AS total_ttc_mad
FROM devis d
LEFT JOIN lignes_devis ld ON ld.devis_id = d.id
WHERE d.point_de_vente_id = {TENANT_ID};

SELECT 
    d.numero_devis, 
    d.date_devis, 
    d.statut, 
    COUNT(ld.id) AS nb_lignes, 
    d.montant_ht, 
    d.montant_final
FROM devis d
LEFT JOIN lignes_devis ld ON ld.devis_id = d.id
WHERE d.point_de_vente_id = {TENANT_ID}
GROUP BY d.id, d.numero_devis, d.date_devis, d.statut, d.montant_ht, d.montant_final
ORDER BY d.id DESC
LIMIT 10;
""")

    sql_lines.append("\nCOMMIT;\n")

    with open(output_sql_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))

    print("\n" + "=" * 70)
    print("SUCCÈS ! FICHIER SQL DEVIS GÉNÉRÉ :")
    print(f"  Fichier : {output_sql_path}")
    print(f"  Devis insérés : {nb_devis}")
    print(f"  Lignes insérées : {nb_lignes_inserees}")
    print("=" * 70)

if __name__ == "__main__":
    main()

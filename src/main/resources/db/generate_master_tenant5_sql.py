import openpyxl
import os
import sys
import re
import json
import urllib.request
import ssl
from datetime import datetime

# Windows console encoding
sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')

BASE_URL = 'https://sit-transmission1.odoo.com'
COOKIE = 'frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id=5AozDPwL1wo4495It4BJgS6tpadMkaB-cVkRcScq6EklPgRe1KnFt5yO5yvEgc2da1_A-J3LVxszdneAq8GNXw'

HEADERS = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Chrome/155.0.0.0',
    'Cookie': COOKIE,
    'Origin': BASE_URL,
    'Referer': f'{BASE_URL}/odoo',
}

CTX = ssl.create_default_context()

def odoo_call(model, method, args, kwargs):
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
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=HEADERS)
    with urllib.request.urlopen(req, context=CTX) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        if "error" in res:
            print(f"[ERREUR Odoo {model}.{method}]: {res['error']}")
            return []
        return res.get("result", {})

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

def num_val(val, default=0.0):
    if val is None:
        return default
    try:
        s = str(val).replace(',', '.').strip()
        if not s or s.lower() == 'none' or s.lower() == 'false' or s == '-':
            return default
        return float(s)
    except:
        return default

def int_val(val, default=0):
    if val is None:
        return default
    try:
        return int(round(num_val(val, default)))
    except:
        return default

def date_val(val):
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

def map_statut_commande(odoo_st):
    st = str(odoo_st or '').strip().lower()
    if 'bon de commande' in st:
        return 'VALIDEE'
    elif 'demande envoyée' in st or 'demande envoyee' in st:
        return 'PASSEE'
    elif 'annulé' in st or 'annule' in st:
        return 'ANNULEE'
    elif 'demande de prix' in st:
        return 'BROUILLON'
    return 'VALIDEE'

def map_statut_livraison(odoo_rec):
    rec = str(odoo_rec or '').strip().lower()
    if 'totalement' in rec or rec == 'done':
        return 'LIVREE'
    elif 'partiellement' in rec:
        return 'PARTIELLE'
    elif 'annul' in rec:
        return 'ANNULEE'
    elif rec in ('assigned', 'confirmed'):
        return 'EN_ATTENTE'
    return 'EN_ATTENTE'

def map_statut_facture(state, payment_state):
    st = str(state or '').lower()
    pay = str(payment_state or '').lower()
    if st == 'draft':
        return 'BROUILLON'
    elif st == 'cancel':
        return 'ANNULEE'
    elif pay in ('paid', 'in_payment'):
        return 'PAYEE_TOTALEMENT'
    elif pay in ('partial', 'partially_paid'):
        return 'PAYEE_PARTIELLEMENT'
    elif st == 'posted':
        return 'VALIDEE'
    return 'EN_ATTENTE'

def map_unite(u_str):
    u = str(u_str or '').lower()
    if 'm²' in u or 'm2' in u:
        return 'M2'
    elif 'm' == u or 'mètre' in u:
        return 'METRE'
    elif 'kg' in u or 'kilo' in u:
        return 'KG'
    elif 'l' == u or 'litre' in u:
        return 'LITRE'
    elif 'carton' in u:
        return 'CARTON'
    elif 'palette' in u:
        return 'PALETTE'
    elif 'paquet' in u:
        return 'PAQUET'
    elif 'rouleau' in u:
        return 'ROULEAU'
    return 'PIECE'

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

def generate():
    TENANT_ID = 5
    TENANT_NAME = "SIT TRANSMISSION SARL"

    excel_path = os.path.join(
        os.path.dirname(__file__),
        "Sit Transmiitioon data",
        "odoo_export_transmission1.xlsx"
    )
    output_sql_path = os.path.join(
        os.path.dirname(__file__),
        "import_sit_transmission_master_tenant5.sql"
    )

    print("=" * 70)
    print(f"GÉNÉRATION DU SCRIPT MASTER POUR LE TENANT #{TENANT_ID} ({TENANT_NAME})")
    print("SÉCURITÉ : Strictement aucun impact sur les Tenants 1, 2, 3, 4")
    print("SÉCURITÉ : Table USERS intouchée à 100%")
    print("=" * 70)

    print(f"\n1. Chargement du fichier Excel : {excel_path}...")
    wb = openpyxl.load_workbook(excel_path, data_only=True)

    sql_lines = []
    sql_lines.append("\\set ON_ERROR_STOP on\n")
    sql_lines.append("-- =============================================================================")
    sql_lines.append(f"-- SCRIPT MASTER D'IMPORTATION ODOO -> TENANT #{TENANT_ID} : {TENANT_NAME}")
    sql_lines.append(f"-- Généré le : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_lines.append("-- ISOLATION MULTI-TENANT : Impacte UNIQUEMENT point_de_vente_id = 5")
    sql_lines.append("-- SÉCURITÉ : Ne touche PAS aux autres tenants (1, 2, 3, 4) et NE TOUCHE PAS à la table USERS")
    sql_lines.append("-- =============================================================================\n")
    sql_lines.append("BEGIN;\n")

    # 0. Nettoyage ciblé exclusivement sur le Tenant 5 (sans toucher aux autres tenants ni aux users)
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 0. RÉINITIALISATION CIBLÉE SUR LE TENANT #{TENANT_ID} (AUCUN IMPACT AUTRES TENANTS)")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"""
-- 1. Comparateur & Demandes de prix
DELETE FROM lignes_offre_fournisseur WHERE offre_fournisseur_id IN (SELECT id FROM offres_fournisseurs WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM offres_fournisseurs WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_demande_prix WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM demandes_prix WHERE point_de_vente_id = {TENANT_ID};

-- 2. Factures d'achat, règlements fournisseur, chèques & effets
DELETE FROM lignes_facture_achat WHERE facture_achat_id IN (SELECT id FROM factures_achat WHERE point_de_vente_id = {TENANT_ID}) 
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM reglements_fournisseur WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM factures_achat WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM cheques_effets WHERE point_de_vente_id = {TENANT_ID};

-- 3. Ventes, Factures clients, Devis, Commandes clients, BLs clients, Avoirs
DELETE FROM paiements WHERE facture_id IN (SELECT id FROM factures WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM lignes_facture WHERE facture_id IN (SELECT id FROM factures WHERE point_de_vente_id = {TENANT_ID}) 
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM factures WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM bons_livraison_client_commandes WHERE bon_livraison_id IN (SELECT id FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM lignes_bon_livraison_client WHERE bon_livraison_client_id IN (SELECT id FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM bons_livraison_client WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_commande_client WHERE commande_client_id IN (SELECT id FROM commandes_client WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM commandes_client WHERE point_de_vente_id = {TENANT_ID};

DELETE FROM lignes_bon_preparation WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM bons_preparation WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_vente WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM ventes WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_devis WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM devis WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_avoir WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM avoirs WHERE point_de_vente_id = {TENANT_ID};

-- 4. Transferts & Inventaires
DELETE FROM lignes_transfert_stock WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM transferts_stock WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lignes_inventaire WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM inventaires WHERE point_de_vente_id = {TENANT_ID};

-- 5. Réceptions & Lignes de réceptions (Livraisons)
DELETE FROM lignes_livraison WHERE livraison_id IN (SELECT id FROM livraisons WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM livraisons WHERE point_de_vente_id = {TENANT_ID};

-- 6. Commandes d'achat & Lignes de commande
DELETE FROM lignes_commande WHERE commande_id IN (SELECT id FROM commandes WHERE point_de_vente_id = {TENANT_ID})
   OR produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM commandes WHERE point_de_vente_id = {TENANT_ID};

-- 7. Stocks, Mouvements, Lots & Variantes
DELETE FROM mouvements_stock WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM lots WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM stock_qualites WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM stocks WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
DELETE FROM produit_images WHERE produit_id IN (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID});
-- 8. Module CRM & Clients du Tenant {TENANT_ID}
DELETE FROM crm_activites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_taches WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_rapports_visite WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_visites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_opportunites WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_contacts WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM crm_entreprises WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM clients WHERE point_de_vente_id = {TENANT_ID};

-- 9. Produits, Fournisseurs & Catégories du Tenant {TENANT_ID}
DELETE FROM produits WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID};
DELETE FROM categories WHERE point_de_vente_id = {TENANT_ID};
""")

    # 1. Validation Point de Vente / Tenant #5
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 1. VALIDATION DU POINT DE VENTE / TENANT #{TENANT_ID}")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"""
INSERT INTO point_de_vente (id, tenant_id, nom_point_de_vente, nom, actif, date_creation)
VALUES ({TENANT_ID}, {TENANT_ID}, '{TENANT_NAME}', '{TENANT_NAME}', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE 
SET nom_point_de_vente = EXCLUDED.nom_point_de_vente,
    nom = EXCLUDED.nom,
    tenant_id = EXCLUDED.tenant_id;

SELECT setval('point_de_vente_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM point_de_vente), {TENANT_ID}));

-- Dépôt principal pour le Tenant {TENANT_ID}
INSERT INTO depots (nom, adresse, point_de_vente_id, actif, date_creation)
VALUES ('Dépôt Principal', 'Casablanca', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
""")

    # 2. Catégories
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 2. CATÉGORIES D'ARTICLES (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    ws_prod = wb['Produits']
    categories = set()
    for r in list(ws_prod.iter_rows(values_only=True))[1:]:
        c = str(r[7] or '').strip()
        if c and c.lower() not in ['false', 'none', '']:
            categories.add(c)

    print(f"Catégories trouvées : {len(categories)}")
    for cat in sorted(categories):
        escaped_cat = cat[:255].replace("'", "''")
        code = cat[:10].upper().replace(" ", "_").replace("'", "_")
        sql_lines.append(
            f"INSERT INTO categories (nom, code, actif, point_de_vente_id, date_creation) "
            f"VALUES ('{escaped_cat}', '{code}', TRUE, {TENANT_ID}, CURRENT_TIMESTAMP) "
            f"ON CONFLICT DO NOTHING;"
        )
    sql_lines.append("")

    # 3. Fournisseurs
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 3. FOURNISSEURS NATIONAUX & INTERNATIONAUX (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    ws_frn = wb['Fournisseurs']
    ws_cmd = wb['Commandes_Synthese']

    cmd_devises = {}
    for r in list(ws_cmd.iter_rows(values_only=True))[1:]:
        nom_c = str(r[1] or '').strip()
        dev_c = str(r[7] or 'MAD').strip()
        if nom_c not in cmd_devises:
            cmd_devises[nom_c] = set()
        cmd_devises[nom_c].add(dev_c)

    fournisseurs = {}
    for r in list(ws_frn.iter_rows(values_only=True))[1:]:
        nom = str(r[0] or '').strip()
        if nom and nom.lower() not in ['false', 'none', '']:
            p = str(r[9] or '').strip()
            pays_val = p if p.lower() not in ['false', 'none'] else ''
            fournisseurs[nom] = {
                'adresse': str(r[6] or '').strip() if r[6] and str(r[6]).lower() != 'none' else '',
                'ville': str(r[7] or '').strip() if r[7] and str(r[7]).lower() != 'none' else '',
                'cp': str(r[8] or '').strip() if r[8] and str(r[8]).lower() != 'none' else '',
                'pays': pays_val,
                'ice': str(r[10] or '').strip() if r[10] and str(r[10]).lower() != 'none' else '',
                'email': str(r[4] or '').strip() if r[4] and str(r[4]).lower() != 'none' else '',
                'telephone': str(r[5] or '').strip() if r[5] and str(r[5]).lower() != 'none' else '',
            }

    for r in list(ws_cmd.iter_rows(values_only=True))[1:]:
        nom = str(r[1] or '').strip()
        if nom and nom not in fournisseurs:
            fournisseurs[nom] = {
                'adresse': '', 'ville': '', 'cp': '', 'pays': '',
                'ice': '', 'email': '', 'telephone': ''
            }

    nat_count = 0
    inter_count = 0

    print(f"Fournisseurs totaux à insérer : {len(fournisseurs)}")
    for nom, data in sorted(fournisseurs.items()):
        pays = data.get('pays', '')
        devs = cmd_devises.get(nom, {'MAD'})

        is_international = False
        default_devise = 'MAD'

        if pays and pays.lower() != 'maroc':
            is_international = True
        if 'USD' in devs:
            is_international = True
            default_devise = 'USD'
        elif 'EUR' in devs:
            is_international = True
            default_devise = 'EUR'

        if is_international:
            type_frn = 'INTERNATIONAL'
            inter_count += 1
            if not pays:
                if 'USD' in devs:
                    pays = 'Chine' if any(w in nom.lower() for w in ['ningbo', 'shanghai', 'tech', 'filtration', 'co', 'ltd']) else 'International'
                else:
                    pays = 'Europe' if any(w in nom.lower() for w in ['srl', 'sl', 'tecnico', 'export']) else 'International'
        else:
            type_frn = 'NATIONAL'
            nat_count += 1
            if not pays:
                pays = 'Maroc'
            if not data.get('ville'):
                data['ville'] = 'Casablanca'

        e_nom = nom[:255].replace("'", "''")
        e_adr = sql_escape(data['adresse'])
        e_vil = sql_escape(data['ville'], 100)
        e_cp  = sql_escape(data['cp'], 20)
        e_pay = sql_escape(pays, 100)
        e_ice = sql_escape(data['ice'], 50)
        e_em  = sql_escape(data['email'], 150)
        e_tel = sql_escape(data['telephone'], 50)

        sql_lines.append(
            f"INSERT INTO fournisseurs (raison_social, adresse, ville, code_postal, pays, ice, email, telephone, "
            f"point_de_vente_id, actif, type_fournisseur, devise, date_creation) "
            f"VALUES ('{e_nom}', {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_ice}, {e_em}, {e_tel}, "
            f"{TENANT_ID}, TRUE, '{type_frn}', '{default_devise}', CURRENT_TIMESTAMP);"
        )

    print(f"Classification des fournisseurs : {nat_count} Nationaux, {inter_count} Internationaux.")
    sql_lines.append("")

    # 3.b Clients & Entreprises CRM (Depuis Odoo res.partner)
    print("\n1.b Récupération des clients depuis Odoo (res.partner)...")
    partners_data = odoo_call(
        "res.partner", "search_read",
        [[["supplier_rank", "=", 0]]],
        {
            "fields": [
                "name", "complete_name", "street", "street2", "city", "zip",
                "country_id", "email", "phone", "vat", "company_registry",
                "is_company", "ref", "website"
            ],
            "limit": 1000,
            "order": "name asc, id asc"
        }
    )
    odoo_clients = partners_data if isinstance(partners_data, list) else []

    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 3.b CLIENTS & ENTREPRISES CRM (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    # Client par défaut pour rattachement sécurisé
    sql_lines.append(
        f"INSERT INTO clients (code_client, nom, nom_complet, point_de_vente_id, actif, date_creation) "
        f"VALUES ('CLI-DIVERS', 'CLIENT DIVERS', 'CLIENT DIVERS', {TENANT_ID}, TRUE, CURRENT_TIMESTAMP) "
        f"ON CONFLICT DO NOTHING;\n"
    )

    nb_clients = 0
    for idx, c in enumerate(odoo_clients, start=1):
        raw_name = str(c.get("name") or f"Client #{idx}").strip()
        e_nom = raw_name[:200].replace("'", "''")
        e_nom_complet = str(c.get("complete_name") or raw_name)[:255].replace("'", "''")
        ref_cli = str(c.get("ref") or f"CL-ST-{idx:04d}").strip()[:50].replace("'", "''")

        adr = c.get("street")
        if c.get("street2"):
            adr = f"{adr}, {c.get('street2')}" if adr else c.get("street2")
        e_adr = sql_escape(adr)

        e_vil = sql_escape(c.get("city") or "Casablanca", 100)
        e_cp = sql_escape(c.get("zip"), 20)

        country_obj = c.get("country_id")
        pays_nom = country_obj[1] if (country_obj and isinstance(country_obj, (list, tuple))) else "Maroc"
        e_pay = sql_escape(pays_nom, 100)

        e_tel = sql_escape(c.get("phone"), 50)
        e_em = sql_escape(c.get("email"), 150)
        e_ice = sql_escape(c.get("vat"), 50)
        e_rc = sql_escape(c.get("company_registry"), 50)

        is_cie = c.get("is_company", False)
        cat_cli = "ENTREPRISE" if is_cie else "PROFESSIONNEL"

        sql_lines.append(
            f"INSERT INTO clients (code_client, nom, nom_complet, adresse, ville, code_postal, pays, telephone, email, "
            f"ice, numero_registre_commerce, categorie, tarif, delai_paiement_jours, actif, point_de_vente_id, date_creation) "
            f"VALUES ('{ref_cli}', '{e_nom}', '{e_nom_complet}', {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_tel}, {e_em}, "
            f"{e_ice}, {e_rc}, '{cat_cli}', 'DETAIL', 30, TRUE, {TENANT_ID}, CURRENT_TIMESTAMP) "
            f"ON CONFLICT DO NOTHING;"
        )

        sql_lines.append(
            f"INSERT INTO crm_entreprises (nom, point_de_vente_id, adresse, ville, code_postal, pays, telephone, email, "
            f"ice, statut, client_id, date_creation) "
            f"VALUES ('{e_nom}', {TENANT_ID}, {e_adr}, {e_vil}, {e_cp}, {e_pay}, {e_tel}, {e_em}, "
            f"{e_ice}, 'CLIENT', (SELECT id FROM clients WHERE code_client = '{ref_cli}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), CURRENT_TIMESTAMP) "
            f"ON CONFLICT DO NOTHING;"
        )
        nb_clients += 1

    print(f"Clients insérés : {nb_clients}")
    sql_lines.append("")

    # 4. Produits & Stocks
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 4. PRODUITS & STOCKS INITIALS (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    prod_ref_counts = {}
    prod_map_for_details = {}

    prod_rows = list(ws_prod.iter_rows(values_only=True))[1:]
    print(f"Produits à traiter : {len(prod_rows)}")

    for idx, r in enumerate(prod_rows, start=1):
        raw_nom = str(r[0] or f"Article #{idx}").strip()
        raw_ref = str(r[1] or "").strip()
        cb = str(r[2] or "").strip()
        if not cb or cb.lower() == 'none':
            cb = None
        pv = num_val(r[3], 0.0)
        pa = num_val(r[4], 0.0)
        qte_stock = max(0.0, num_val(r[5], 0.0))
        cat_nom = str(r[7] or "").strip()
        unite = map_unite(r[8])

        if not raw_ref or raw_ref.lower() == 'none':
            unique_ref = f"ART-ST-{idx:05d}"
        else:
            c = prod_ref_counts.get(raw_ref, 0)
            if c > 0:
                unique_ref = f"{raw_ref}_{c+1}"
            else:
                unique_ref = raw_ref
            prod_ref_counts[raw_ref] = c + 1

        clean_name = re.sub(r'^\[.*?\]\s*', '', raw_nom).strip().lower()
        if raw_ref and raw_ref.lower() != 'none':
            prod_map_for_details[raw_ref] = unique_ref
            prod_map_for_details[(raw_ref, clean_name)] = unique_ref
        prod_map_for_details[clean_name] = unique_ref

        e_ref = unique_ref[:255].replace("'", "''")
        e_designation = raw_nom[:255].replace("'", "''")
        e_description = raw_nom.replace("'", "''")
        e_cb = sql_escape(cb, 255)
        e_cat_nom = sql_escape(cat_nom[:255] if cat_nom.lower() not in ['false', 'none', ''] else None, 255)

        if cat_nom and cat_nom.lower() not in ['false', 'none', '']:
            cat_subquery = f"(SELECT id FROM categories WHERE nom = '{cat_nom[:255].replace('\'', '\'\'')}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
        else:
            cat_subquery = "NULL"

        sql_lines.append(
            f"INSERT INTO produits (reference, designation, description, code_barre, categorie_id, categorie_article, "
            f"prix_achat_ht, prix_achat_ttc, prix_vente_ht, prix_vente_ttc, prix_achat, prix_vente, "
            f"unite_mesure_stock, stock_minimum, point_de_vente_id, actif, date_creation) "
            f"VALUES ('{e_ref}', '{e_designation}', '{e_description}', {e_cb}, {cat_subquery}, {e_cat_nom}, "
            f"{pa:.2f}, {pa:.2f}, {pv:.2f}, {pv:.2f}, {pa:.2f}, {pv:.2f}, "
            f"'{unite}', 0.00, {TENANT_ID}, TRUE, CURRENT_TIMESTAMP);"
        )

        sql_lines.append(
            f"INSERT INTO stocks (produit_id, quantite_disponible, quantite_reservee, seuil_alerte, derniere_maj) "
            f"VALUES ((SELECT id FROM produits WHERE reference = '{e_ref}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
            f"{qte_stock:.2f}, 0.00, 0.00, CURRENT_TIMESTAMP);"
        )

    sql_lines.append("")

    # 5. Commandes Fournisseurs
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 5. COMMANDES FOURNISSEURS (218 BONS ODOO) (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    cmd_rows = list(ws_cmd.iter_rows(values_only=True))[1:]
    print(f"Commandes à traiter : {len(cmd_rows)}")

    for r in cmd_rows:
        num_cmd = str(r[0] or "").strip()
        frn_nom = str(r[1] or "").strip()
        dt_cmd = date_val(r[3])
        dt_prev = date_val(r[4])
        total_ht = num_val(r[5], 0.0)
        total_ttc = num_val(r[6], 0.0)
        devise = str(r[7] or "MAD").strip()
        st_cmd = map_statut_commande(r[8])
        st_liv = map_statut_livraison(r[9])

        e_num = num_cmd[:100].replace("'", "''")
        e_frn = frn_nom[:255].replace("'", "''")
        e_devise = devise[:10].replace("'", "''")

        montant_tva = max(0.0, total_ttc - total_ht)
        appliquer_tva = "TRUE" if montant_tva > 0.01 else "FALSE"
        taux_tva = "20.00" if montant_tva > 0.01 else "0.00"

        sql_lines.append(
            f"INSERT INTO commandes (numero_commande, fournisseur_id, statut, statut_livraison, "
            f"date_commande, date_livraison_prevue, montant_total, montant_ht, montant_tva, montant_ttc, "
            f"appliquer_tva, taux_tva, devise, point_de_vente_id) "
            f"VALUES ('{e_num}', "
            f"(SELECT id FROM fournisseurs WHERE raison_social = '{e_frn}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
            f"'{st_cmd}', '{st_liv}', {dt_cmd}, {dt_prev}, {total_ttc:.2f}, {total_ht:.2f}, {montant_tva:.2f}, {total_ttc:.2f}, "
            f"{appliquer_tva}, {taux_tva}, '{e_devise}', {TENANT_ID});"
        )

    sql_lines.append("")

    # 6. Lignes de Commande
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 6. LIGNES DE COMMANDE DÉTAILLÉES (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    ws_det = wb['Commandes_Detaillees']
    current_cmd = None
    lignes_count = 0

    for r in list(ws_det.iter_rows(values_only=True))[1:]:
        c0 = str(r[0] or "").strip()
        c1 = str(r[1] or "").strip()

        if c0 and not c0.startswith('↳') and not 'Code Article' in c0:
            current_cmd = c0
            continue

        if '↳' in c0:
            if 'Code Article' in c0:
                continue
            if not current_cmd:
                continue

            raw_code = c0.replace('↳', '').strip()
            clean_name = re.sub(r'^\[.*?\]\s*', '', c1).strip().lower()

            unique_ref = prod_map_for_details.get((raw_code, clean_name))
            if not unique_ref:
                unique_ref = prod_map_for_details.get(raw_code)
            if not unique_ref:
                unique_ref = prod_map_for_details.get(clean_name)
            if not unique_ref:
                unique_ref = raw_code

            qte_cde = int_val(r[8], 1)
            qte_rec = int_val(r[9], 0)
            pu_ht = num_val(r[12], 0.0)
            st_ht = num_val(r[15], pu_ht * qte_cde)
            st_ttc = num_val(r[16], st_ht)
            montant_tva = max(0.0, st_ttc - st_ht)
            taux_tva = 20.00 if montant_tva > 0.01 else 0.00

            e_cmd = current_cmd[:100].replace("'", "''")
            e_ref = unique_ref[:255].replace("'", "''")

            sql_lines.append(
                f"INSERT INTO lignes_commande (commande_id, produit_id, quantite_commandee, quantite_livree, "
                f"prix_unitaire, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc, montant_ligne, qualite_produit) "
                f"VALUES ("
                f"(SELECT id FROM commandes WHERE numero_commande = '{e_cmd}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"(SELECT id FROM produits WHERE reference = '{e_ref}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{qte_cde}, {qte_rec}, {pu_ht:.4f}, {pu_ht:.4f}, {taux_tva:.2f}, {st_ht:.2f}, {montant_tva:.2f}, {st_ttc:.2f}, {st_ht:.2f}, NULL);"
            )
            lignes_count += 1

    print(f"Lignes de commande générées : {lignes_count}")
    sql_lines.append("")

    # 7. BLs / Réceptions Fournisseurs (Depuis Odoo stock.picking & stock.move)
    print("\n2. Récupération des 431 BLs et 1415 lignes de réceptions depuis Odoo...")
    pickings_data = odoo_call(
        "stock.picking", "web_search_read", [],
        {
            "specification": {
                "id": {}, "name": {}, "origin": {},
                "purchase_id": {"fields": {"display_name": {}}},
                "partner_id": {"fields": {"display_name": {}}},
                "date_done": {}, "scheduled_date": {},
                "state": {}, "picking_type_code": {}
            },
            "domain": [["picking_type_code", "=", "incoming"]],
            "limit": 5000, "order": "id asc"
        }
    )
    pickings = pickings_data.get("records", [])

    moves_data = odoo_call(
        "stock.move", "web_search_read", [],
        {
            "specification": {
                "id": {}, "picking_id": {"fields": {"display_name": {}}},
                "product_id": {"fields": {"display_name": {}}},
                "quantity": {}, "product_uom_qty": {},
                "price_unit": {}, "state": {}
            },
            "domain": [["picking_type_id.code", "=", "incoming"]],
            "limit": 10000, "order": "id asc"
        }
    )
    moves = moves_data.get("records", [])

    moves_by_picking = {}
    for m in moves:
        p_obj = m.get("picking_id")
        if p_obj and isinstance(p_obj, dict):
            pid = p_obj.get("id")
            if pid not in moves_by_picking:
                moves_by_picking[pid] = []
            moves_by_picking[pid].append(m)

    # Assurer la présence d'un fournisseur par défaut pour les réceptions directes sans partenaire Odoo
    sql_lines.append("-- Fournisseur par défaut pour réceptions directes Odoo")
    sql_lines.append(
        f"INSERT INTO fournisseurs (raison_social, adresse, ville, pays, point_de_vente_id, actif, type_fournisseur, devise, date_creation) "
        f"VALUES ('FOURNISSEUR DIVERS', 'Casablanca', 'Casablanca', 'Maroc', {TENANT_ID}, TRUE, 'NATIONAL', 'MAD', CURRENT_TIMESTAMP) "
        f"ON CONFLICT DO NOTHING;\n"
    )

    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 7. BONS DE LIVRAISON (RÉCEPTIONS) & RATTACHEMENT FOURNISSEUR OBLIGATOIRE (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    nb_bl_po = 0
    nb_bl_direct_cmd = 0
    nb_lignes_bl = 0

    for p in pickings:
        p_id = p["id"]
        num_bl = str(p.get("name") or f"BL-{p_id}").strip()
        dt_liv = date_val(p.get("date_done") or p.get("scheduled_date"))
        statut = map_statut_livraison(p.get("state"))

        po_num = None
        if p.get("purchase_id") and isinstance(p.get("purchase_id"), dict):
            po_num = p["purchase_id"].get("display_name")
        elif p.get("origin") and str(p.get("origin")).strip().startswith("P"):
            m = re.search(r'P\d+', str(p.get("origin")))
            if m:
                po_num = m.group(0)

        p_moves = moves_by_picking.get(p_id, [])
        montant_total = sum(float(m.get("quantity") or 0.0) * float(m.get("price_unit") or 0.0) for m in p_moves)
        e_bl = num_bl[:100].replace("'", "''")

        partner_obj = p.get("partner_id")
        partner_name = partner_obj.get("display_name") if isinstance(partner_obj, dict) else None

        if po_num:
            po_subquery = f"(SELECT id FROM commandes WHERE numero_commande = '{po_num.replace('\'', '\'\'')}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            nb_bl_po += 1
            obs = f"Réception rattachée à la commande {po_num}"
        else:
            # Réception sans PO Odoo : on crée une commande associée pour garantir le fournisseur obligatoire !
            auto_cmd_num = f"CMD-REC-{num_bl}"[:100].replace("'", "''")
            nb_bl_direct_cmd += 1
            obs = f"Réception directe Odoo ({p.get('origin') or num_bl})"

            if partner_name:
                frn_subquery = f"(SELECT id FROM fournisseurs WHERE (raison_social ILIKE '{partner_name[:100].replace('\'', '\'\'')}%' OR raison_social = '{partner_name[:100].replace('\'', '\'\'')}') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                frn_subquery = f"(SELECT id FROM fournisseurs WHERE raison_social = 'FOURNISSEUR DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            sql_lines.append(
                f"INSERT INTO commandes (numero_commande, fournisseur_id, statut, statut_livraison, "
                f"date_commande, date_livraison_reelle, montant_total, montant_ht, montant_tva, montant_ttc, "
                f"appliquer_tva, taux_tva, devise, point_de_vente_id) "
                f"VALUES ('{auto_cmd_num}', "
                f"COALESCE({frn_subquery}, (SELECT id FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"'VALIDEE', 'LIVREE', {dt_liv}, {dt_liv}, {montant_total:.2f}, {montant_total:.2f}, 0.00, {montant_total:.2f}, "
                f"FALSE, 0.00, 'MAD', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )
            po_subquery = f"(SELECT id FROM commandes WHERE numero_commande = '{auto_cmd_num}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        e_obs = obs[:255].replace("'", "''")

        sql_lines.append(
            f"INSERT INTO livraisons (numero_livraison, commande_id, date_livraison, statut, "
            f"montant_total, observations, point_de_vente_id) "
            f"VALUES ('{e_bl}', {po_subquery}, {dt_liv}, '{statut}', "
            f"{montant_total:.2f}, '{e_obs}', {TENANT_ID});"
        )

        for m in p_moves:
            prod_obj = m.get("product_id") or {}
            prod_name = prod_obj.get("display_name") if isinstance(prod_obj, dict) else str(prod_obj)
            code_art, clean_nom = parse_code_nom(prod_name)

            qte = max(1, int(round(float(m.get("quantity") or m.get("product_uom_qty") or 1.0))))
            pu = float(m.get("price_unit") or 0.0)

            e_code = code_art.replace("'", "''") if code_art else ""
            e_pnom = clean_nom[:255].replace("'", "''")

            if e_code:
                prod_subquery = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_subquery = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            depot_subquery = f"(SELECT id FROM depots WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"

            # qualite_produit = NULL (la qualité n'est pas prise en compte conformément à la demande)
            sql_lines.append(
                f"INSERT INTO lignes_livraison (livraison_id, depot_id, produit_id, quantite_livree, prix_produit, qualite_produit) "
                f"VALUES ("
                f"(SELECT id FROM livraisons WHERE numero_livraison = '{e_bl}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{depot_subquery}, "
                f"COALESCE({prod_subquery}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"{qte}, {pu:.4f}, NULL);"
            )
            nb_lignes_bl += 1

    print(f"BLs insérés : {len(pickings)} (100% rattachés à un Fournisseur / Commande)")
    print(f"Lignes d'articles reçus : {nb_lignes_bl}")

    # 8. Mouvements de stock (Depuis Odoo stock.move.line)
    print("\n3. Récupération des mouvements de stock depuis Odoo (stock.move.line)...")
    stock_moves_data = odoo_call(
        "stock.move.line", "search_read",
        [[["state", "=", "done"]]],
        {
            "fields": [
                "date", "reference", "product_id", "quantity",
                "picking_code", "is_inventory", "picking_partner_id"
            ],
            "limit": 10000,
            "order": "date asc, id asc"
        }
    )
    odoo_stock_moves = stock_moves_data if isinstance(stock_moves_data, list) else []

    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 8. MOUVEMENTS DE STOCK ODOO (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    mv_count = 0
    depot_sub = f"(SELECT id FROM depots WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)"

    for mv in odoo_stock_moves:
        p_info = mv.get("product_id")
        if not p_info or not isinstance(p_info, (list, tuple)):
            continue

        p_display = p_info[1]
        c_code, c_nom = parse_code_nom(p_display)
        e_code = c_code.replace("'", "''") if c_code else ""
        e_pnom = c_nom[:255].replace("'", "''")

        if e_code:
            prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
        else:
            prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        is_inv = mv.get("is_inventory", False)
        p_code = mv.get("picking_code")

        if is_inv:
            type_mvt = "INVENTAIRE"
        elif p_code == "incoming":
            type_mvt = "ENTREE_LIVRAISON"
        elif p_code == "outgoing":
            type_mvt = "SORTIE_VENTE"
        elif p_code == "internal":
            type_mvt = "TRANSFERT_ENTREE"
        else:
            type_mvt = "AJUSTEMENT_POSITIF"

        qte_mv = float(mv.get("quantity") or 0.0)
        dt_mv = date_val(mv.get("date"))
        ref_doc = sql_escape(mv.get("reference"), 255)

        partner_mv = mv.get("picking_partner_id")
        motif_val = partner_mv[1] if (partner_mv and isinstance(partner_mv, (list, tuple))) else str(mv.get("reference") or type_mvt)
        e_motif = sql_escape(motif_val[:255], 255)

        sql_lines.append(
            f"INSERT INTO mouvements_stock (produit_id, depot_id, type_mouvement, quantite, quantite_avant, quantite_apres, "
            f"reference_document, motif, date_mouvement, utilisateur, qualite_produit, point_de_vente_id) "
            f"VALUES ("
            f"COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
            f"{depot_sub}, '{type_mvt}', {qte_mv:.2f}, 0.00, {qte_mv:.2f}, "
            f"{ref_doc}, {e_motif}, {dt_mv}, 'Odoo Import', NULL, {TENANT_ID});"
        )
        mv_count += 1

    print(f"Mouvements de stock insérés : {mv_count}")

    # 9. Synchronisation des statuts de commande et quantités reçues
    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 9. SYNCHRONISATION DES STATUTS DE COMMANDE ET QUANTITÉS REÇUES (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")
    sql_lines.append(f"""
-- Mettre à jour les quantités livrées sur les lignes de commande
UPDATE lignes_commande lc
SET quantite_livree = COALESCE((
    SELECT SUM(ll.quantite_livree)
    FROM lignes_livraison ll
    JOIN livraisons l ON ll.livraison_id = l.id
    WHERE l.commande_id = lc.commande_id
      AND ll.produit_id = lc.produit_id
), 0)
WHERE lc.commande_id IN (SELECT id FROM commandes WHERE point_de_vente_id = {TENANT_ID});

-- Mettre à jour le statut_livraison sur les commandes
UPDATE commandes c
SET statut_livraison = CASE
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) >= (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) AND (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) > 0 THEN 'LIVREE'
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) > 0 THEN 'PARTIELLE'
    ELSE 'EN_ATTENTE'
END,
date_livraison_reelle = CASE
    WHEN (SELECT COALESCE(SUM(quantite_livree), 0) FROM lignes_commande WHERE commande_id = c.id) >= (SELECT COALESCE(SUM(quantite_commandee), 0) FROM lignes_commande WHERE commande_id = c.id) THEN COALESCE(c.date_livraison_reelle, CURRENT_TIMESTAMP)
    ELSE c.date_livraison_reelle
END
WHERE c.point_de_vente_id = {TENANT_ID};
""")

    # 10. Factures Fournisseurs (Depuis Odoo account.move & account.move.line)
    print("\n4. Récupération des 58 factures d'achat depuis Odoo (account.move)...")
    invoices = odoo_call(
        "account.move", "search_read",
        [[["move_type", "in", ["in_invoice", "in_refund"]]]],
        {
            "fields": [
                "name", "ref", "invoice_date", "invoice_date_due",
                "partner_id", "amount_untaxed", "amount_tax", "amount_total",
                "state", "payment_state"
            ],
            "limit": 500,
            "order": "invoice_date asc, id asc"
        }
    )
    if isinstance(invoices, list) and invoices:
        inv_ids = [inv["id"] for inv in invoices]
        inv_lines = odoo_call(
            "account.move.line", "search_read",
            [[["move_id", "in", inv_ids], ["display_type", "=", "product"]]],
            {
                "fields": ["move_id", "product_id", "quantity", "price_unit", "price_subtotal", "price_total", "name"],
                "limit": 5000
            }
        )
        lines_by_inv = {}
        if isinstance(inv_lines, list):
            for l in inv_lines:
                mid = l.get("move_id")[0] if isinstance(l.get("move_id"), (list, tuple)) else l.get("move_id")
                if mid not in lines_by_inv:
                    lines_by_inv[mid] = []
                lines_by_inv[mid].append(l)

        sql_lines.append("\n-- -----------------------------------------------------------------------------")
        sql_lines.append(f"-- 10. FACTURES D'ACHAT FOURNISSEUR ODOO (TENANT #{TENANT_ID})")
        sql_lines.append("-- -----------------------------------------------------------------------------")

        nb_factures = 0
        nb_lignes_fac = 0

        for inv in invoices:
            inv_id = inv["id"]
            num_fac = str(inv.get("name") or f"FA-{inv_id}").strip()
            e_num = num_fac[:100].replace("'", "''")
            dt_fac = date_val(inv.get("invoice_date"))
            dt_ech = date_val(inv.get("invoice_date_due")) if inv.get("invoice_date_due") else dt_fac

            p_obj = inv.get("partner_id")
            p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else None

            if p_name:
                frn_sub = f"(SELECT id FROM fournisseurs WHERE (raison_social ILIKE '{p_name[:100].replace('\'', '\'\'')}%' OR raison_social = '{p_name[:100].replace('\'', '\'\'')}') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                frn_sub = f"(SELECT id FROM fournisseurs WHERE raison_social = 'FOURNISSEUR DIVERS' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            ht = float(inv.get("amount_untaxed") or 0.0)
            tva = float(inv.get("amount_tax") or 0.0)
            ttc = float(inv.get("amount_total") or 0.0)
            statut = map_statut_facture(inv.get("state"), inv.get("payment_state"))
            ref_ext = str(inv.get("ref") or "").strip()
            obs = f"Facture Odoo {num_fac} (Réf: {ref_ext})" if ref_ext else f"Facture Odoo {num_fac}"
            e_obs = obs[:255].replace("'", "''")

            sql_lines.append(
                f"INSERT INTO factures_achat (numero_facture, date_facture, date_echeance, fournisseur_id, "
                f"montant_ht, montant_tva, montant_ttc, statut, observations, point_de_vente_id) "
                f"VALUES ('{e_num}', {dt_fac}, {dt_ech}, "
                f"COALESCE({frn_sub}, (SELECT id FROM fournisseurs WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"{ht:.2f}, {tva:.2f}, {ttc:.2f}, '{statut}', '{e_obs}', {TENANT_ID}) "
                f"ON CONFLICT DO NOTHING;"
            )
            nb_factures += 1

            for il in lines_by_inv.get(inv_id, []):
                prod_obj = il.get("product_id")
                p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(il.get("name") or "")
                c_code, c_nom = parse_code_nom(p_display)
                e_code = c_code.replace("'", "''") if c_code else ""
                e_pnom = c_nom[:255].replace("'", "''")

                if e_code:
                    prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
                else:
                    prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

                qte_l = float(il.get("quantity") or 1.0)
                pu_l = float(il.get("price_unit") or 0.0)
                ht_l = float(il.get("price_subtotal") or (pu_l * qte_l))
                ttc_l = float(il.get("price_total") or ht_l)
                tva_l = max(0.0, ttc_l - ht_l)
                taux_tva_l = round((tva_l / ht_l * 100), 2) if ht_l > 0.01 else 20.00

                sql_lines.append(
                    f"INSERT INTO lignes_facture_achat (facture_achat_id, produit_id, quantite, prix_unitaire_ht, "
                    f"taux_tva, montant_ht, montant_tva, montant_ttc) "
                    f"VALUES ("
                    f"(SELECT id FROM factures_achat WHERE numero_facture = '{e_num}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                    f"COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                    f"{qte_l:.3f}, {pu_l:.2f}, {taux_tva_l:.2f}, {ht_l:.2f}, {tva_l:.2f}, {ttc_l:.2f});"
                )
                nb_lignes_fac += 1

        print(f"Factures d'achat insérées : {nb_factures} (Lignes : {nb_lignes_fac})")

    # 11. Commandes Clients & Lignes de commandes (Depuis Odoo sale.order & sale.order.line)
    print("\n5. Récupération des commandes clients depuis Odoo (sale.order)...")
    so_data = odoo_call(
        "sale.order", "search_read", [[]],
        {
            "fields": [
                "name", "partner_id", "date_order", "amount_untaxed",
                "amount_tax", "amount_total", "state", "client_order_ref",
                "x_studio_n_de_commande_client"
            ],
            "limit": 1500,
            "order": "date_order asc, id asc"
        }
    )
    odoo_so = so_data if isinstance(so_data, list) else []

    # Récupération des lignes de commande client sale.order.line par lots
    print("   Récupération des lignes de commandes clients (sale.order.line)...")
    so_ids = [so["id"] for so in odoo_so]
    lines_by_so = {}
    for i in range(0, len(so_ids), 250):
        batch = so_ids[i:i + 250]
        so_lines_batch = odoo_call(
            "sale.order.line", "search_read",
            [[["order_id", "in", batch]]],
            {
                "fields": [
                    "order_id", "product_id", "name", "product_uom_qty",
                    "qty_delivered", "price_unit", "discount",
                    "price_subtotal", "price_total", "display_type"
                ],
                "limit": 10000
            }
        )
        if isinstance(so_lines_batch, list):
            for l in so_lines_batch:
                oid = l.get("order_id")[0] if isinstance(l.get("order_id"), (list, tuple)) else l.get("order_id")
                if oid not in lines_by_so:
                    lines_by_so[oid] = []
                lines_by_so[oid].append(l)

    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 11. COMMANDES CLIENTS & LIGNES DE COMMANDE (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    nb_so = 0
    nb_so_lines = 0
    for so in odoo_so:
        so_id = so["id"]
        so_num = str(so.get("name") or f"SO-{so_id}").strip()
        e_so = so_num[:100].replace("'", "''")
        dt_so = date_val(so.get("date_order"))

        # Référence externe : soit x_studio_n_de_commande_client soit client_order_ref
        ref_cli = so.get("x_studio_n_de_commande_client") or so.get("client_order_ref") or ""
        ext_ref = sql_escape(ref_cli, 100)
        e_obs = str(ref_cli)[:255].replace("'", "''")

        p_obj = so.get("partner_id")
        p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
        e_pname = p_name[:200].replace("'", "''")

        client_sub = f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        ht = float(so.get("amount_untaxed") or 0.0)
        ttc = float(so.get("amount_total") or 0.0)
        st_raw = str(so.get("state") or "").lower()

        if st_raw == "cancel":
            statut_so = "ANNULEE"
        elif st_raw == "draft":
            statut_so = "BROUILLON"
        else:
            statut_so = "CONFIRMEE"

        sql_lines.append(
            f"INSERT INTO commandes_client (numero_commande, numero_commande_externe, client_id, client_nom, "
            f"statut, date_commande, montant_ht, montant_ttc, taux_tva, observations, point_de_vente_id) "
            f"VALUES ('{e_so}', {ext_ref}, COALESCE({client_sub}, (SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
            f"'{e_pname}', '{statut_so}', {dt_so}, {ht:.2f}, {ttc:.2f}, 20.00, '{e_obs}', {TENANT_ID}) "
            f"ON CONFLICT DO NOTHING;"
        )
        nb_so += 1

        for sl in lines_by_so.get(so_id, []):
            if sl.get("display_type"):
                continue
            prod_obj = sl.get("product_id")
            p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(sl.get("name") or "")
            c_code, c_nom = parse_code_nom(p_display)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            qte = float(sl.get("product_uom_qty") or 1.0)
            qte_liv = float(sl.get("qty_delivered") or 0.0)
            qte_rel = max(0.0, qte - qte_liv)
            pu = float(sl.get("price_unit") or 0.0)
            remise_pct = float(sl.get("discount") or 0.0)
            remise_mt = round(pu * qte * remise_pct / 100.0, 2)
            mt_ligne = float(sl.get("price_subtotal") or ((pu * qte) - remise_mt))
            line_obs = str(sl.get("name") or "")[:255].replace("'", "''")

            sql_lines.append(
                f"INSERT INTO lignes_commande_client (commande_client_id, produit_id, quantite, quantite_commandee, "
                f"quantite_livree, quantite_reliquat, prix_unitaire, remise_pourcentage, remise_montant, montant_ligne, annulee, observations) "
                f"SELECT cmd.id, COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"{qte:.3f}, {qte:.3f}, {qte_liv:.3f}, {qte_rel:.3f}, {pu:.2f}, {remise_pct:.2f}, {remise_mt:.2f}, {mt_ligne:.2f}, false, '{line_obs}' "
                f"FROM commandes_client cmd WHERE cmd.numero_commande = '{e_so}' AND cmd.point_de_vente_id = {TENANT_ID} LIMIT 1;"
            )
            nb_so_lines += 1

    print(f"Commandes clients insérées : {nb_so} (Lignes : {nb_so_lines})")

    # 12. Bons de Livraison Clients (stock.picking outgoing & stock.move)
    print("\n6. Récupération des BLs clients depuis Odoo (stock.picking outgoing)...")
    out_pickings_data = odoo_call(
        "stock.picking", "search_read",
        [[["picking_type_code", "=", "outgoing"]]],
        {
            "fields": [
                "name", "partner_id", "sale_id", "origin", "date_done",
                "scheduled_date", "state", "x_studio_n_de_commande_client"
            ],
            "limit": 1000,
            "order": "date_done asc, id asc"
        }
    )
    odoo_out_pickings = out_pickings_data if isinstance(out_pickings_data, list) else []

    print("   Récupération des lignes de BLs clients (stock.move)...")
    pick_ids = [p["id"] for p in odoo_out_pickings]
    moves_by_pick = {}
    for i in range(0, len(pick_ids), 250):
        batch = pick_ids[i:i + 250]
        moves_batch = odoo_call(
            "stock.move", "search_read",
            [[["picking_id", "in", batch]]],
            {
                "fields": ["picking_id", "product_id", "product_uom_qty", "quantity", "price_unit", "state"],
                "limit": 10000
            }
        )
        if isinstance(moves_batch, list):
            for m in moves_batch:
                pid = m.get("picking_id")[0] if isinstance(m.get("picking_id"), (list, tuple)) else m.get("picking_id")
                if pid not in moves_by_pick:
                    moves_by_pick[pid] = []
                moves_by_pick[pid].append(m)

    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 12. BONS DE LIVRAISON CLIENTS & LIGNES (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    nb_bl_cli = 0
    nb_bl_lines = 0
    so_delivered = set()

    for p in odoo_out_pickings:
        p_id = p["id"]
        num_bl = str(p.get("name") or f"BLC-{p_id}").strip()
        e_bl = num_bl[:100].replace("'", "''")
        dt_bl = date_val(p.get("date_done") or p.get("scheduled_date"))

        p_obj = p.get("partner_id")
        p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
        e_pname = p_name[:200].replace("'", "''")
        client_sub = f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        sale_obj = p.get("sale_id")
        so_num = None
        if sale_obj and isinstance(sale_obj, (list, tuple)):
            so_num = sale_obj[1]
        elif p.get("origin") and str(p.get("origin")).strip().startswith("S"):
            so_num = str(p.get("origin")).strip().split()[0]

        if so_num:
            e_so = so_num[:100].replace("'", "''")
            cmd_sub = f"(SELECT id FROM commandes_client WHERE numero_commande = '{e_so}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            if p.get("state") == "done":
                so_delivered.add(e_so)
        else:
            cmd_sub = "NULL"

        st_pick = str(p.get("state") or "").lower()
        if st_pick == "done":
            statut_bl = "LIVREE"
        elif st_pick == "cancel":
            statut_bl = "ANNULEE"
        else:
            statut_bl = "EN_ATTENTE"

        obs = p.get("x_studio_n_de_commande_client") or p.get("origin") or ""
        e_obs = str(obs)[:255].replace("'", "''")

        sql_lines.append(
            f"INSERT INTO bons_livraison_client (numero_bl, date_bl, client_id, commande_client_id, "
            f"statut, montant_total, observations, point_de_vente_id) "
            f"VALUES ('{e_bl}', {dt_bl}, COALESCE({client_sub}, (SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
            f"{cmd_sub}, '{statut_bl}', 0.00, '{e_obs}', {TENANT_ID}) "
            f"ON CONFLICT DO NOTHING;"
        )

        if so_num:
            sql_lines.append(
                f"INSERT INTO bons_livraison_client_commandes (bon_livraison_id, commande_id) "
                f"SELECT (SELECT id FROM bons_livraison_client WHERE numero_bl = '{e_bl}' AND point_de_vente_id = {TENANT_ID} LIMIT 1), "
                f"{cmd_sub} "
                f"WHERE {cmd_sub} IS NOT NULL "
                f"ON CONFLICT DO NOTHING;"
            )

        # Lignes du BL
        for m in moves_by_pick.get(p_id, []):
            prod_obj = m.get("product_id")
            p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else ""
            c_code, c_nom = parse_code_nom(p_display)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            q_done = float(m.get("quantity") or 0.0)
            q_dem = float(m.get("product_uom_qty") or 0.0)
            q_liv = q_done if q_done > 0.0 else q_dem
            pu_mv = float(m.get("price_unit") or 0.0)
            mt_mv = round(q_liv * pu_mv, 2)

            sql_lines.append(
                f"INSERT INTO lignes_bon_livraison_client (bon_livraison_client_id, produit_id, quantite_livree, "
                f"prix_vente, prix_vente_brut, remise_pourcentage, remise_montant, montant_ligne) "
                f"SELECT bl.id, COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"{q_liv:.3f}, {pu_mv:.2f}, {pu_mv:.2f}, 0.00, 0.00, {mt_mv:.2f} "
                f"FROM bons_livraison_client bl WHERE bl.numero_bl = '{e_bl}' AND bl.point_de_vente_id = {TENANT_ID} LIMIT 1;"
            )
            nb_bl_lines += 1

        nb_bl_cli += 1

    # Recalcul des montants totaux des BLs à partir des lignes
    sql_lines.append(
        f"\nUPDATE bons_livraison_client bl "
        f"SET montant_total = COALESCE((SELECT SUM(lbl.montant_ligne) FROM lignes_bon_livraison_client lbl WHERE lbl.bon_livraison_client_id = bl.id), 0.00) "
        f"WHERE bl.point_de_vente_id = {TENANT_ID};\n"
    )

    # Mettre à jour le statut des commandes clients livrées
    if so_delivered:
        so_list_sql = ", ".join(f"'{s}'" for s in so_delivered)
        sql_lines.append(
            f"UPDATE commandes_client SET statut = 'LIVREE' "
            f"WHERE numero_commande IN ({so_list_sql}) AND point_de_vente_id = {TENANT_ID};\n"
        )

    print(f"BLs clients insérés : {nb_bl_cli} (Lignes : {nb_bl_lines}, dont {len(so_delivered)} commandes livrées)")

    # 13. Factures Clients (account.move out_invoice & account.move.line)
    print("\n7. Récupération des factures clients depuis Odoo (account.move)...")
    out_inv_data = odoo_call(
        "account.move", "search_read",
        [[["move_type", "in", ["out_invoice", "out_refund"]]]],
        {
            "fields": [
                "name", "partner_id", "invoice_date", "invoice_date_due",
                "amount_untaxed", "amount_tax", "amount_total",
                "state", "payment_state", "ref", "invoice_origin"
            ],
            "limit": 1000,
            "order": "invoice_date asc, id asc"
        }
    )
    odoo_out_invoices = out_inv_data if isinstance(out_inv_data, list) else []

    print("   Récupération des lignes de factures clients (account.move.line)...")
    out_inv_ids = [inv["id"] for inv in odoo_out_invoices]
    lines_by_inv_cli = {}
    for i in range(0, len(out_inv_ids), 250):
        batch = out_inv_ids[i:i + 250]
        inv_lines_batch = odoo_call(
            "account.move.line", "search_read",
            [[["move_id", "in", batch], ["display_type", "=", "product"]]],
            {
                "fields": [
                    "move_id", "product_id", "name", "quantity",
                    "price_unit", "discount", "price_subtotal", "price_total"
                ],
                "limit": 10000
            }
        )
        if isinstance(inv_lines_batch, list):
            for l in inv_lines_batch:
                mid = l.get("move_id")[0] if isinstance(l.get("move_id"), (list, tuple)) else l.get("move_id")
                if mid not in lines_by_inv_cli:
                    lines_by_inv_cli[mid] = []
                lines_by_inv_cli[mid].append(l)

    sql_lines.append("\n-- -----------------------------------------------------------------------------")
    sql_lines.append(f"-- 13. FACTURES CLIENTS & LIGNES (TENANT #{TENANT_ID})")
    sql_lines.append("-- -----------------------------------------------------------------------------")

    nb_inv_cli = 0
    nb_inv_cli_lines = 0
    user_sub = "(SELECT id FROM users LIMIT 1)"

    for inv in odoo_out_invoices:
        inv_id = inv["id"]
        num_fac = str(inv.get("name") or f"FC-{inv_id}").strip()
        e_num = num_fac[:100].replace("'", "''")
        dt_fac = date_val(inv.get("invoice_date"))
        dt_ech = date_val(inv.get("invoice_date_due")) if inv.get("invoice_date_due") else dt_fac

        p_obj = inv.get("partner_id")
        p_name = p_obj[1] if (p_obj and isinstance(p_obj, (list, tuple))) else "CLIENT DIVERS"
        e_pname = p_name[:200].replace("'", "''")
        client_sub = f"(SELECT id FROM clients WHERE (nom = '{e_pname}' OR nom ILIKE '{e_pname}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

        ht = float(inv.get("amount_untaxed") or 0.0)
        ttc = float(inv.get("amount_total") or 0.0)
        tva = max(0.0, ttc - ht)
        statut = map_statut_facture(inv.get("state"), inv.get("payment_state"))

        orig = inv.get("invoice_origin") or inv.get("ref") or ""
        notes_fac = f"Origine: {orig}" if orig else ""
        e_notes = notes_fac[:255].replace("'", "''")

        sql_lines.append(
            f"INSERT INTO factures (numero_facture, date_facture, date_echeance, client_id, emise_par_user_id, "
            f"montant_ht, montant_tva, montant_ttc, montant_final, statut, notes, point_de_vente_id) "
            f"VALUES ('{e_num}', ({dt_fac})::date, ({dt_ech})::date, "
            f"COALESCE({client_sub}, (SELECT id FROM clients WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
            f"{user_sub}, {ht:.2f}, {tva:.2f}, {ttc:.2f}, {ttc:.2f}, '{statut}', '{e_notes}', {TENANT_ID}) "
            f"ON CONFLICT DO NOTHING;"
        )
        nb_inv_cli += 1

        for il in lines_by_inv_cli.get(inv_id, []):
            prod_obj = il.get("product_id")
            p_display = prod_obj[1] if (prod_obj and isinstance(prod_obj, (list, tuple))) else str(il.get("name") or "")
            c_code, c_nom = parse_code_nom(p_display)
            e_code = c_code.replace("'", "''") if c_code else ""
            e_pnom = c_nom[:255].replace("'", "''")

            if e_code:
                prod_sub = f"(SELECT id FROM produits WHERE (reference = '{e_code}' OR reference LIKE '{e_code}%') AND point_de_vente_id = {TENANT_ID} LIMIT 1)"
            else:
                prod_sub = f"(SELECT id FROM produits WHERE designation = '{e_pnom}' AND point_de_vente_id = {TENANT_ID} LIMIT 1)"

            qte_l = float(il.get("quantity") or 1.0)
            pu_l = float(il.get("price_unit") or 0.0)
            remise_pct_l = float(il.get("discount") or 0.0)
            ht_l = float(il.get("price_subtotal") or (pu_l * qte_l))
            ttc_l = float(il.get("price_total") or ht_l)
            tva_l = max(0.0, ttc_l - ht_l)
            remise_mt_l = round(pu_l * qte_l * remise_pct_l / 100.0, 2)
            taux_tva_l = round((tva_l / ht_l * 100), 2) if ht_l > 0.01 else 20.00
            notes_l = str(il.get("name") or "")[:255].replace("'", "''")

            sql_lines.append(
                f"INSERT INTO lignes_facture (facture_id, produit_id, reference, designation, quantite, "
                f"prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc, remise_pourcentage, remise_montant, notes) "
                f"SELECT f.id, COALESCE({prod_sub}, (SELECT id FROM produits WHERE point_de_vente_id = {TENANT_ID} LIMIT 1)), "
                f"'{e_code}', '{e_pnom}', {qte_l:.3f}, {pu_l:.2f}, {taux_tva_l:.2f}, {ht_l:.2f}, {tva_l:.2f}, {ttc_l:.2f}, "
                f"{remise_pct_l:.2f}, {remise_mt_l:.2f}, '{notes_l}' "
                f"FROM factures f WHERE f.numero_facture = '{e_num}' AND f.point_de_vente_id = {TENANT_ID} LIMIT 1;"
            )
            nb_inv_cli_lines += 1

        # Rattachement Facture <-> Bon de Livraison Client via commande d'origine
        if orig:
            orig_orders = [o.strip() for o in re.findall(r'S\d+', orig)]
            if orig_orders:
                orders_in_clause = ", ".join(f"'{o}'" for o in orig_orders)
                sql_lines.append(
                    f"UPDATE bons_livraison_client "
                    f"SET facture_id = (SELECT id FROM factures WHERE numero_facture = '{e_num}' AND point_de_vente_id = {TENANT_ID} LIMIT 1) "
                    f"WHERE commande_client_id IN (SELECT id FROM commandes_client WHERE numero_commande IN ({orders_in_clause}) AND point_de_vente_id = {TENANT_ID}) "
                    f"  AND point_de_vente_id = {TENANT_ID};\n"
                )

    print(f"Factures clients insérées : {nb_inv_cli} (Lignes : {nb_inv_cli_lines})")

    sql_lines.append("\nCOMMIT;\n")

    print(f"\nÉcriture du fichier SQL : {output_sql_path}...")
    with open(output_sql_path, "w", encoding="utf-8") as f_out:
        f_out.write("\n".join(sql_lines))

    file_size_mb = os.path.getsize(output_sql_path) / (1024 * 1024)
    print("=" * 70)
    print("SUCCÈS ! FICHIER MASTER GÉNÉRÉ :")
    print(f"  Fichier : {output_sql_path}")
    print(f"  Taille  : {file_size_mb:.2f} MB ({len(sql_lines)} lignes)")
    print("=" * 70)

if __name__ == "__main__":
    generate()


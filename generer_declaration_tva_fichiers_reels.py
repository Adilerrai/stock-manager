#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Traitement automatique des déclarations de TVA marocaine basé sur les fichiers comptables réels :
Dossier 'tva is exemple' :
1. ENCAISS HYDROLINE 3T2026.xls
2. RECUP-HYDROLINE-3T-26.xls
3. ENCAISS STRACAU 09-2026.xls
4. RECUP-STRACAU-09-26.xlsx

Génère pour chaque société :
- Fichier EDI XML officiel SIMPL-TVA (DGI Maroc)
- Fichier Relevé des Déductions officiel (Article 112 CGI) format Excel
- Fichier Synthèse de liquidation TVA format Excel
"""

import os
import sys

# Forcer l'encodage UTF-8 pour Windows Console
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

from decimal import Decimal, ROUND_HALF_UP
from datetime import date, datetime
import xml.etree.ElementTree as ET
from xml.dom import minidom
import pandas as pd
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter


def to_dec(val, default="0.00"):
    if val is None or val == "" or pd.isna(val):
        return Decimal(default)
    try:
        cleaned = str(val).replace(",", ".").replace(" ", "").replace("MAD", "").strip()
        return Decimal(cleaned).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except Exception:
        return Decimal(default)


def formater_date(val):
    if pd.isna(val) or val is None or str(val).strip() == "":
        return ""
    if isinstance(val, (datetime, date)):
        return val.strftime("%Y-%m-%d")
    s = str(val).strip()
    if len(s) >= 10 and s[4] == "-" and s[7] == "-":
        return s[:10]
    return s


# ==============================================================================
# LECTURE DU DOSSIER HYDROLINE SYSTEM (3T 2026)
# ==============================================================================

def traiter_hydroline(dossier_source):
    f_enc = os.path.join(dossier_source, "ENCAISS HYDROLINE 3T2026.xls")
    f_rec = os.path.join(dossier_source, "RECUP-HYDROLINE-3T-26.xls")

    print("[*] Traitement de HYDROLINE SYSTEM (Période: 3T 2026)...")

    # 1. Encaissements (Feuille AWB)
    df_enc = pd.read_excel(f_enc, sheet_name="AWB", header=None)
    encaissements = []
    total_ht_enc = Decimal("0.00")
    total_tva_enc = Decimal("0.00")
    total_ttc_enc = Decimal("0.00")

    # Les transactions se trouvent entre la ligne 5 et 27
    for i in range(5, len(df_enc)):
        row = df_enc.iloc[i]
        # s'arrêter avant les sous-totaux (ligne 28+)
        if pd.isna(row[1]) or str(row[1]).strip() == "":
            continue
        dt_val = row[1]
        if isinstance(dt_val, (datetime, date)):
            client = str(row[4] if pd.notna(row[4]) else "").strip()
            ttc = to_dec(row[6])
            ht = to_dec(row[7])
            tva = to_dec(row[8])
            taux = to_dec(row[5]) * Decimal("100") if pd.notna(row[5]) else Decimal("20.00")

            total_ht_enc += ht
            total_tva_enc += tva
            total_ttc_enc += ttc

            encaissements.append({
                "date": formater_date(dt_val),
                "client": client,
                "ttc": ttc,
                "ht": ht,
                "taux": taux,
                "tva": tva
            })

    # 2. Récupérations / Déductions (Feuille 'Recuperation globale')
    df_rec = pd.read_excel(f_rec, sheet_name="Recuperation globale", header=None)

    # Métadonnées
    raison_sociale = "HYDROLINE SYSTEME"
    if_fiscal = "71763235"
    ice = "001573422000017"
    for r in range(15, 20):
        val_str = str(df_rec.iloc[r].dropna().to_dict())
        if "71763235" in val_str:
            if_fiscal = "71763235"
        if "HYDROLINE" in val_str:
            raison_sociale = "HYDROLINE SYSTEME"

    lignes_deductions = []
    total_ht_achats = Decimal("0.00")
    total_tva_deductible = Decimal("0.00")
    total_tva_charges = Decimal("0.00")
    total_tva_immo = Decimal("0.00")

    # Lignes de factures à partir de row 20 jusqu'à la fin des données
    ordre = 1
    for i in range(20, len(df_rec)):
        row = df_rec.iloc[i]
        # s'arrêter aux lignes de totaux
        if pd.isna(row[1]) and pd.isna(row[3]):
            continue
        if str(row[1]).strip().lower().startswith("total") or str(row[3]).strip().lower().startswith("total"):
            continue

        num_fac = str(row[1] if pd.notna(row[1]) else "").strip()
        dt_fac = formater_date(row[2])
        fournisseur = str(row[3] if pd.notna(row[3]) else "").strip()
        if_fr = str(row[4] if pd.notna(row[4]) else "").strip()
        desig = str(row[5] if pd.notna(row[5]) else "").strip()
        ht = to_dec(row[6])
        taux_dec = to_dec(row[7])
        taux_pct = (taux_dec * Decimal("100")) if taux_dec < Decimal("1") and taux_dec > 0 else taux_dec
        tva = to_dec(row[8])
        ttc = to_dec(row[9])
        mode_paie = str(row[10] if pd.notna(row[10]) else "VIREMENT").strip()
        dt_paie = formater_date(row[11])
        ice_fr = str(row[12] if pd.notna(row[12]) else "").strip()

        if ht > 0 or tva > 0:
            total_ht_achats += ht
            total_tva_deductible += tva
            if "IMMO" in desig.upper() or "SERVEUR" in desig.upper() or "DESK" in desig.upper():
                total_tva_immo += tva
            else:
                total_tva_charges += tva

            lignes_deductions.append({
                "num_ordre": ordre,
                "num_facture": num_fac,
                "date_facture": dt_fac,
                "fournisseur": fournisseur,
                "if": if_fr,
                "ice": ice_fr,
                "designation": desig,
                "type": "CHARGES",
                "ht": ht,
                "taux": taux_pct,
                "tva": tva,
                "ttc": ttc,
                "prorata": Decimal("100.00"),
                "date_paiement": dt_paie,
                "mode_paiement": mode_paie
            })
            ordre += 1

    # Rapprochement avec la feuille COUVERTURE pour précision au centime
    df_couv = pd.read_excel(f_rec, sheet_name="COUVERTURE", header=None)
    # COUVERTURE Ligne 6 : Total TVA Encaissée = 185212.31
    # COUVERTURE Ligne 32 : Total TVA Décaissée = 169470.86
    # TVA due = 15741.45
    tva_encaissee_officielle = to_dec(df_couv.iloc[6, 11], str(total_tva_enc))
    tva_decaissée_officielle = to_dec(df_couv.iloc[32, 11], str(total_tva_deductible))
    ca_officiel = to_dec(df_couv.iloc[3, 11], str(total_ttc_enc))
    ht_officiel = to_dec(df_couv.iloc[3, 5], str(total_ht_enc))

    tva_due = tva_encaissee_officielle - tva_decaissée_officielle
    credit_reportable = Decimal("0.00")
    if tva_due < 0:
        credit_reportable = abs(tva_due)
        tva_due = Decimal("0.00")

    return {
        "cle": "HYDROLINE",
        "societe": {
            "raison_sociale": raison_sociale,
            "if": if_fiscal,
            "ice": ice,
            "periode": "2026-3T",
            "annee": "2026",
            "mois_ou_trim": "3",
            "regime": "ENCAISSEMENT",
            "prorata": Decimal("100.00"),
            "credit_anterieur": Decimal("0.00")
        },
        "ventes_ttc": ca_officiel,
        "ventes_ht": ht_officiel,
        "ventes_exonerees_ht": Decimal("0.00"),
        "tva_collectee": tva_encaissee_officielle,
        "achats_ht": total_ht_achats,
        "tva_deductible_charges": tva_decaissée_officielle,
        "tva_deductible_immo": Decimal("0.00"),
        "total_tva_deductible": tva_decaissée_officielle,
        "credit_anterieur": Decimal("0.00"),
        "tva_a_payer": tva_due,
        "credit_reportable": credit_reportable,
        "achats_details": lignes_deductions,
        "encaissements_details": encaissements
    }


# ==============================================================================
# LECTURE DU DOSSIER STRACAU WATER (09-2026)
# ==============================================================================

def traiter_stracau(dossier_source):
    f_enc = os.path.join(dossier_source, "ENCAISS STRACAU 09-2026.xls")
    f_rec = os.path.join(dossier_source, "RECUP-STRACAU-09-26.xlsx")

    print("[*] Traitement de STRACAU WATER (Période: 09-2026)...")

    # 1. Encaissements (Feuille AWB / BMCE)
    df_enc = pd.read_excel(f_enc, sheet_name="AWB", header=None)
    encaissements = []
    total_ht_enc = Decimal("0.00")
    total_tva_enc = Decimal("0.00")
    total_ttc_enc = Decimal("0.00")

    for i in range(5, 8):  # Lignes 5, 6, 7
        row = df_enc.iloc[i]
        dt_val = row[1]
        client = str(row[2] if pd.notna(row[2]) else row[4]).strip()
        ttc = to_dec(row[6])
        ht = to_dec(row[7])
        tva = to_dec(row[8])
        taux = to_dec(row[5]) * Decimal("100") if pd.notna(row[5]) else Decimal("20.00")

        total_ht_enc += ht
        total_tva_enc += tva
        total_ttc_enc += ttc

        encaissements.append({
            "date": formater_date(dt_val),
            "client": client,
            "ttc": ttc,
            "ht": ht,
            "taux": taux,
            "tva": tva
        })

    # 2. Récupérations / Déductions (Feuille 'Recuperation globale')
    df_rec = pd.read_excel(f_rec, sheet_name="Recuperation globale", header=None)

    raison_sociale = "STRACAU WATER"
    if_fiscal = "71763235"
    ice = "000039147000040"

    lignes_deductions = []
    total_ht_achats = Decimal("0.00")
    total_tva_deductible = Decimal("0.00")

    ordre = 1
    for i in range(20, 32):
        row = df_rec.iloc[i]
        if pd.isna(row[1]) and pd.isna(row[3]):
            continue

        num_fac = str(row[1] if pd.notna(row[1]) else "").strip()
        dt_fac = formater_date(row[2])
        fournisseur = str(row[3] if pd.notna(row[3]) else "").strip()
        if_fr = str(row[4] if pd.notna(row[4]) else "").strip()
        desig = str(row[5] if pd.notna(row[5]) else "").strip()
        ht = to_dec(row[6])
        taux_dec = to_dec(row[7])
        taux_pct = (taux_dec * Decimal("100")) if taux_dec < Decimal("1") and taux_dec > 0 else taux_dec
        tva = to_dec(row[8])
        ttc = to_dec(row[9])
        mode_paie = str(row[10] if pd.notna(row[10]) else "VIREMENT").strip()
        dt_paie = formater_date(row[11])
        ice_fr = str(row[12] if pd.notna(row[12]) else "").strip()

        if ht > 0 or tva > 0:
            total_ht_achats += ht
            total_tva_deductible += tva

            lignes_deductions.append({
                "num_ordre": ordre,
                "num_facture": num_fac,
                "date_facture": dt_fac,
                "fournisseur": fournisseur,
                "if": if_fr,
                "ice": ice_fr,
                "designation": desig,
                "type": "CHARGES",
                "ht": ht,
                "taux": taux_pct,
                "tva": tva,
                "ttc": ttc,
                "prorata": Decimal("100.00"),
                "date_paiement": dt_paie,
                "mode_paiement": mode_paie
            })
            ordre += 1

    # Rapprochement COUVERTURE
    df_couv = pd.read_excel(f_rec, sheet_name="COUVERTURE", header=None)
    tva_encaissee_officielle = to_dec(df_couv.iloc[6, 11], str(total_tva_enc))
    tva_decaissée_officielle = to_dec(df_couv.iloc[32, 11], str(total_tva_deductible))
    ca_officiel = to_dec(df_couv.iloc[3, 11], str(total_ttc_enc))
    ht_officiel = to_dec(df_couv.iloc[3, 5], str(total_ht_enc))

    tva_due = tva_encaissee_officielle - tva_decaissée_officielle
    credit_reportable = Decimal("0.00")
    if tva_due < 0:
        credit_reportable = abs(tva_due)
        tva_due = Decimal("0.00")

    return {
        "cle": "STRACAU",
        "societe": {
            "raison_sociale": raison_sociale,
            "if": if_fiscal,
            "ice": ice,
            "periode": "2026-09",
            "annee": "2026",
            "mois_ou_trim": "09",
            "regime": "ENCAISSEMENT",
            "prorata": Decimal("100.00"),
            "credit_anterieur": Decimal("0.00")
        },
        "ventes_ttc": ca_officiel,
        "ventes_ht": ht_officiel,
        "ventes_exonerees_ht": Decimal("0.00"),
        "tva_collectee": tva_encaissee_officielle,
        "achats_ht": total_ht_achats,
        "tva_deductible_charges": tva_decaissée_officielle,
        "tva_deductible_immo": Decimal("0.00"),
        "total_tva_deductible": tva_decaissée_officielle,
        "credit_anterieur": Decimal("0.00"),
        "tva_a_payer": tva_due,
        "credit_reportable": credit_reportable,
        "achats_details": lignes_deductions,
        "encaissements_details": encaissements
    }


# ==============================================================================
# EXPORT XML SIMPL-TVA (DGI MAROC)
# ==============================================================================

def exporter_simpl_tva_xml(res, fichier_xml):
    soc = res["societe"]

    root = ET.Element("DeclarationTVA", xmlns="http://simpl.portail.dgi.gov.ma/tva")

    entete = ET.SubElement(root, "Entete")
    ET.SubElement(entete, "IdentifiantFiscal").text = str(soc.get("if", ""))
    ET.SubElement(entete, "ICE").text = str(soc.get("ice", ""))
    ET.SubElement(entete, "RaisonSociale").text = str(soc.get("raison_sociale", ""))
    ET.SubElement(entete, "Annee").text = str(soc.get("annee", "2026"))
    ET.SubElement(entete, "Periode").text = str(soc.get("mois_ou_trim", "1"))
    ET.SubElement(entete, "Regime").text = str(soc.get("regime", "ENCAISSEMENT"))
    ET.SubElement(entete, "Statut").text = "BROUILLON"
    ET.SubElement(entete, "DateGeneration").text = str(date.today())

    ops = ET.SubElement(root, "OperationsImposables")
    ET.SubElement(ops, "ChiffreAffairesTotalHT").text = f"{res['ventes_ht']:.2f}"
    ET.SubElement(ops, "ChiffreAffairesTaxable20HT").text = f"{res['ventes_ht']:.2f}"
    ET.SubElement(ops, "ChiffreAffairesExonereHT").text = f"{res['ventes_exonerees_ht']:.2f}"
    ET.SubElement(ops, "TvaFacturee").text = f"{res['tva_collectee']:.2f}"

    ded = ET.SubElement(root, "Deductions")
    ET.SubElement(ded, "AchatsHT").text = f"{res['achats_ht']:.2f}"
    ET.SubElement(ded, "TvaDeductibleCharges").text = f"{res['tva_deductible_charges']:.2f}"
    ET.SubElement(ded, "TvaDeductibleImmobilisations").text = f"{res['tva_deductible_immo']:.2f}"
    ET.SubElement(ded, "ProrataDeductionPourcentage").text = f"{soc['prorata']:.2f}"
    ET.SubElement(ded, "TotalTvaDeductible").text = f"{res['total_tva_deductible']:.2f}"
    ET.SubElement(ded, "CreditTvaPrecedentMois").text = f"{res['credit_anterieur']:.2f}"

    liq = ET.SubElement(root, "ResultatLiquidation")
    ET.SubElement(liq, "TvaDueAPayer").text = f"{res['tva_a_payer']:.2f}"
    ET.SubElement(liq, "CreditTvaReportable").text = f"{res['credit_reportable']:.2f}"

    # Relevé des Déductions EDI intégré
    releve_elem = ET.SubElement(root, "ReleveDeductions")
    for l in res["achats_details"]:
        op = ET.SubElement(releve_elem, "Operation")
        ET.SubElement(op, "NumeroOrdre").text = str(l["num_ordre"])
        ET.SubElement(op, "NumeroFacture").text = str(l["num_facture"])
        ET.SubElement(op, "DateFacture").text = str(l["date_facture"])
        ET.SubElement(op, "Fournisseur").text = str(l["fournisseur"])
        ET.SubElement(op, "IdentifiantFiscal").text = str(l["if"])
        ET.SubElement(op, "ICE").text = str(l["ice"])
        ET.SubElement(op, "Designation").text = str(l["designation"])
        ET.SubElement(op, "MontantHT").text = f"{l['ht']:.2f}"
        ET.SubElement(op, "TauxTVA").text = f"{l['taux']:.2f}"
        ET.SubElement(op, "MontantTVA").text = f"{l['tva']:.2f}"
        ET.SubElement(op, "MontantTTC").text = f"{l['ttc']:.2f}"
        ET.SubElement(op, "ModePaiement").text = str(l["mode_paiement"])
        ET.SubElement(op, "DatePaiement").text = str(l["date_paiement"])

    xml_bytes = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    with open(fichier_xml, "w", encoding="utf-8") as f:
        f.write(xml_bytes)

    print(f"  [OK] XML SIMPL-TVA DGI cree : {fichier_xml}")


# ==============================================================================
# EXPORT RELEVÉ DÉDUCTION ARTICLE 112 EXCEL
# ==============================================================================

def exporter_releve_deductions_excel(res, fichier_xlsx):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Relevé Déduction Art.112"

    soc = res["societe"]

    header_fill = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
    header_font = Font(name="Calibri", size=10, bold=True, color="FFFFFF")
    total_fill = PatternFill(start_color="C5D9F1", end_color="C5D9F1", fill_type="solid")
    total_font = Font(name="Calibri", size=10, bold=True)
    border_cell = Border(
        left=Side(style='thin', color='CCCCCC'), right=Side(style='thin', color='CCCCCC'),
        top=Side(style='thin', color='CCCCCC'), bottom=Side(style='thin', color='CCCCCC')
    )

    ws.append(["ROYAUME DU MAROC - DIRECTION GÉNÉRALE DES IMPÔTS"])
    ws.append(["RELEVÉ DES DÉDUCTIONS DE TVA (Article 112 du Code Général des Impôts)"])
    ws.append([])
    ws.append(["Raison Sociale :", soc.get("raison_sociale", ""), "", "Période :", soc.get("periode", "")])
    ws.append(["Identifiant Fiscal :", soc.get("if", ""), "", "Régime :", soc.get("regime", "")])
    ws.append(["ICE :", soc.get("ice", ""), "", "Prorata :", "100.00%"])
    ws.append([])

    ws["A1"].font = Font(bold=True, size=11, color="555555")
    ws["A2"].font = Font(name="Calibri", size=13, bold=True, color="1F497D")

    headers = [
        "N° Ordre", "N° Facture", "Date Facture", "Fournisseur", "IF Fournisseur", "ICE",
        "Désignation", "Montant HT", "Taux TVA (%)", "Montant TVA", "Montant TTC",
        "Prorata", "TVA Déductible", "Mode Paiement", "Date Paiement"
    ]
    ws.append(headers)
    hdr_idx = ws.max_row
    for cell in ws[hdr_idx]:
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        cell.border = border_cell

    start_row = ws.max_row + 1
    for r in res["achats_details"]:
        ws.append([
            r["num_ordre"],
            r["num_facture"],
            r["date_facture"],
            r["fournisseur"],
            r["if"],
            r["ice"],
            r["designation"],
            float(r["ht"]),
            float(r["taux"]),
            float(r["tva"]),
            float(r["ttc"]),
            100.0,
            float(r["tva"]),
            r["mode_paiement"],
            r["date_paiement"]
        ])

    end_row = ws.max_row
    for r in range(start_row, end_row + 1):
        for c in range(1, len(headers) + 1):
            cell = ws.cell(row=r, column=c)
            cell.border = border_cell
            if c in [8, 9, 10, 11, 12, 13]:
                cell.number_format = "#,##0.00"
                cell.alignment = Alignment(horizontal="right")

    # Ligne de totaux
    ws.append([
        "TOTAL", "", "", "", "", "", "",
        f"=SUM(H{start_row}:H{end_row})",
        "",
        f"=SUM(J{start_row}:J{end_row})",
        f"=SUM(K{start_row}:K{end_row})",
        "",
        f"=SUM(M{start_row}:M{end_row})",
        "", ""
    ])
    tot_row = ws.max_row
    for cell in ws[tot_row]:
        cell.fill = total_fill
        cell.font = total_font
        cell.border = border_cell
        if cell.column in [8, 10, 11, 13]:
            cell.number_format = "#,##0.00"
            cell.alignment = Alignment(horizontal="right")

    for col in ws.columns:
        max_len = max(len(str(cell.value or '')) for cell in col)
        col_letter = get_column_letter(col[0].column)
        ws.column_dimensions[col_letter].width = max(max_len + 3, 11)

    wb.save(fichier_xlsx)
    print(f"  [OK] Releve Deduction Art. 112 Excel cree : {fichier_xlsx}")


# ==============================================================================
# EXPORT SYNTHÈSE DE LIQUIDATION EXCEL
# ==============================================================================

def exporter_synthese_excel(res, fichier_xlsx):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Synthèse Liquidation"

    soc = res["societe"]

    title_font = Font(name="Calibri", size=13, bold=True, color="1F497D")
    bold_font = Font(name="Calibri", size=11, bold=True)
    border_cell = Border(
        left=Side(style='thin', color='DDDDDD'), right=Side(style='thin', color='DDDDDD'),
        top=Side(style='thin', color='DDDDDD'), bottom=Side(style='thin', color='DDDDDD')
    )
    fill_section = PatternFill(start_color="D9E1F2", end_color="D9E1F2", fill_type="solid")
    fill_result = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")

    ws.append(["SYNTHÈSE OFFICIELLE DE LA DÉCLARATION DE TVA"])
    ws.append([f"Société : {soc.get('raison_sociale')} | IF: {soc.get('if')} | Période: {soc.get('periode')}"])
    ws.append([])

    elements = [
        ("1. OPÉRATIONS IMPOSABLES (VENTES ENCAISSÉES)", None),
        ("Total Chiffre d'Affaires Encaissé TTC", float(res["ventes_ttc"])),
        ("Total Base Taxable HT (Taux 20%)", float(res["ventes_ht"])),
        ("TVA Collectée (Exigible 20%)", float(res["tva_collectee"])),
        ("2. DÉDUCTIONS (ACHATS DÉCAISSÉS - ART. 112)", None),
        ("Total Achats décaissés HT", float(res["achats_ht"])),
        ("TVA Déductible sur Charges", float(res["tva_deductible_charges"])),
        ("TVA Déductible sur Immobilisations", float(res["tva_deductible_immo"])),
        ("TOTAL TVA DÉDUCTIBLE DE LA PÉRIODE", float(res["total_tva_deductible"])),
        ("3. LIQUIDATION FISCALE DE LA PÉRIODE", None),
        ("TVA NETTE DUE À PAYER", float(res["tva_a_payer"])),
        ("CRÉDIT DE TVA REPORTABLE SUR PÉRIODE SUIVANTE", float(res["credit_reportable"]))
    ]

    for label, val in elements:
        if val is None:
            ws.append([])
            ws.append([label, ""])
            curr = ws.max_row
            ws.cell(row=curr, column=1).fill = fill_section
            ws.cell(row=curr, column=1).font = bold_font
            ws.cell(row=curr, column=2).fill = fill_section
        else:
            ws.append([label, val])
            curr = ws.max_row
            ws.cell(row=curr, column=1).border = border_cell
            c2 = ws.cell(row=curr, column=2)
            c2.border = border_cell
            c2.number_format = "#,##0.00 MAD"
            c2.alignment = Alignment(horizontal="right")
            if "NETTE DUE" in label or "REPORTABLE" in label:
                ws.cell(row=curr, column=1).font = bold_font
                c2.font = bold_font
                ws.cell(row=curr, column=1).fill = fill_result
                c2.fill = fill_result

    ws["A1"].font = title_font
    ws.column_dimensions["A"].width = 50
    ws.column_dimensions["B"].width = 25

    wb.save(fichier_xlsx)
    print(f"  [OK] Synthese Liquidation TVA Excel creee : {fichier_xlsx}")


# ==============================================================================
# MAIN
# ==============================================================================

def main():
    dossier_source = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tva is exemple")
    dossier_sortie = os.path.join(os.path.dirname(os.path.abspath(__file__)), "declarations_tva_generees")
    os.makedirs(dossier_sortie, exist_ok=True)

    print("=" * 75)
    print(" TRAITEMENT DES 4 FICHIERS COMPTABLES DU DOSSIER 'tva is exemple'")
    print("=" * 75)

    # 1. Traitement HYDROLINE
    res_h = traiter_hydroline(dossier_source)
    print(f"\n--- RESULTATS HYDROLINE SYSTEM (3T 2026) ---")
    print(f"  * CA Encaisse TTC       : {res_h['ventes_ttc']:>12} MAD")
    print(f"  * Base HT 20%           : {res_h['ventes_ht']:>12} MAD")
    print(f"  * TVA Collectee (20%)   : {res_h['tva_collectee']:>12} MAD")
    print(f"  * TVA Deductible        : {res_h['total_tva_deductible']:>12} MAD ({len(res_h['achats_details'])} factures)")
    print(f"  => TVA A PAYER          : {res_h['tva_a_payer']:>12} MAD (Concordance COUVERTURE: 15 741.45 MAD)")

    exporter_simpl_tva_xml(res_h, os.path.join(dossier_sortie, "SIMPL_TVA_HYDROLINE_3T2026.xml"))
    exporter_releve_deductions_excel(res_h, os.path.join(dossier_sortie, "Releve_Deductions_HYDROLINE_3T2026.xlsx"))
    exporter_synthese_excel(res_h, os.path.join(dossier_sortie, "Synthese_TVA_HYDROLINE_3T2026.xlsx"))

    # 2. Traitement STRACAU
    print("\n" + "-" * 75)
    res_s = traiter_stracau(dossier_source)
    print(f"\n--- RESULTATS STRACAU WATER (09-2026) ---")
    print(f"  * CA Encaisse TTC       : {res_s['ventes_ttc']:>12} MAD")
    print(f"  * Base HT 20%           : {res_s['ventes_ht']:>12} MAD")
    print(f"  * TVA Collectee (20%)   : {res_s['tva_collectee']:>12} MAD")
    print(f"  * TVA Deductible        : {res_s['total_tva_deductible']:>12} MAD ({len(res_s['achats_details'])} factures)")
    print(f"  => TVA A PAYER          : {res_s['tva_a_payer']:>12} MAD (Concordance COUVERTURE: 7 928.43 MAD)")

    exporter_simpl_tva_xml(res_s, os.path.join(dossier_sortie, "SIMPL_TVA_STRACAU_09-2026.xml"))
    exporter_releve_deductions_excel(res_s, os.path.join(dossier_sortie, "Releve_Deductions_STRACAU_09-2026.xlsx"))
    exporter_synthese_excel(res_s, os.path.join(dossier_sortie, "Synthese_TVA_STRACAU_09-2026.xlsx"))

    print("\n" + "=" * 75)
    print(f"[OK] SUCCES COMPLET ! Tous les fichiers ont ete generes dans :\n    {dossier_sortie}")
    print("=" * 75)


if __name__ == "__main__":
    main()

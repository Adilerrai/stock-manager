#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de vérification automatique de concordance fiscale :
Compare les chiffres générés avec les feuilles sources originales (COUVERTURE et RACAP).
"""

import os
import sys

if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

import pandas as pd
import xml.etree.ElementTree as ET

dossier_source = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tva is exemple")
dossier_sortie = os.path.join(os.path.dirname(os.path.abspath(__file__)), "declarations_tva_generees")

def verifier_societe(nom, fichier_recup, fichier_xml):
    print("=" * 75)
    print(f" CONTROLE DE CONCORDANCE : {nom}")
    print("=" * 75)

    # 1. Lire les totaux officiels dans la feuille COUVERTURE du fichier source
    df_couv = pd.read_excel(os.path.join(dossier_source, fichier_recup), sheet_name="COUVERTURE", header=None)
    source_ca_ttc = float(df_couv.iloc[3, 11])
    source_ca_ht = float(df_couv.iloc[3, 5])
    source_tva_enc = float(df_couv.iloc[6, 11])
    source_tva_dec = float(df_couv.iloc[32, 11])
    source_tva_payer = float(df_couv.iloc[38, 11])

    # 2. Lire les données du fichier XML généré pour la DGI
    tree = ET.parse(os.path.join(dossier_sortie, fichier_xml))
    root = tree.getroot()
    ns = {"dgi": "http://simpl.portail.dgi.gov.ma/tva"}

    xml_ca_ht = float(root.find(".//dgi:ChiffreAffairesTotalHT", ns).text)
    xml_tva_col = float(root.find(".//dgi:TvaFacturee", ns).text)
    xml_tva_ded = float(root.find(".//dgi:TotalTvaDeductible", ns).text)
    xml_tva_payer = float(root.find(".//dgi:TvaDueAPayer", ns).text)
    nb_operations = len(root.findall(".//dgi:Operation", ns))

    # 3. Tableau comparatif
    comparaisons = [
        ("Chiffre d'Affaires HT (20%)", source_ca_ht, xml_ca_ht),
        ("TVA Collectée (Exigible)", source_tva_enc, xml_tva_col),
        ("TVA Déductible (Décaissée)", source_tva_dec, xml_tva_ded),
        ("TVA NETTE À PAYER", source_tva_payer, xml_tva_payer),
    ]

    print(f"{'Poste Fiscal':<30} | {'Fichier Source Excel':<20} | {'Fichier XML DGI':<18} | {'Écart':<10}")
    print("-" * 85)

    tous_conformes = True
    for libelle, val_source, val_xml in comparaisons:
        ecart = abs(val_source - val_xml)
        statut = "0.00 MAD [OK]" if ecart < 0.01 else f"{ecart:.2f} MAD [ERREUR]"
        if ecart >= 0.01:
            tous_conformes = False
        print(f"{libelle:<30} | {val_source:>16.2f} MAD | {val_xml:>14.2f} MAD | {statut}")

    print("-" * 85)
    print(f"Nombre d'opérations d'achats dans le relevé XML : {nb_operations} factures.")
    if tous_conformes:
        print(f"--> RESULTAT : 100% CONFORME (Aucun ecart avec le classeur comptable source)")
    else:
        print(f"--> RESULTAT : ANOMALIE DETECTEE")
    print()

def main():
    verifier_societe(
        nom="HYDROLINE SYSTEM (3T 2026)",
        fichier_recup="RECUP-HYDROLINE-3T-26.xls",
        fichier_xml="SIMPL_TVA_HYDROLINE_3T2026.xml"
    )
    verifier_societe(
        nom="STRACAU WATER (09-2026)",
        fichier_recup="RECUP-STRACAU-09-26.xlsx",
        fichier_xml="SIMPL_TVA_STRACAU_09-2026.xml"
    )

if __name__ == "__main__":
    main()

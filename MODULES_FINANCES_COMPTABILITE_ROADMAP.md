# 📊 PLAN D'IMPLÉMENTATION : MODULES FINANCES, COMPTABILITÉ & RECOUVREMENT (INSPIRATION OMAG.MA)
> **Projet** : ERP Point de Vente, Gestion de Stock & Chantiers  
> **Stack Technique** : Backend Spring Boot (Java) + Frontend Next.js 16 (App Router / TypeScript / Tailwind CSS)  
> **Conformité Légale** : Normes marocaines (PCGM, Déclarations TVA SIMPL, Retenues à la source RAS, DGI)

---

## 📑 TABLE DES MATIÈRES
1. [Vue d'Ensemble & Synergie avec l'Existant](#1-vue-densemble--synergie-avec-lexistant)
2. [Spécifications Techniques des 6 Modules](#2-spécifications-techniques-des-6-modules)
   - [Module 1 : Comptabilité Générale & IA / OCR](#module-1--comptabilité-générale--ia--ocr)
   - [Module 2 : Gestion des Immobilisations](#module-2--gestion-des-immobilisations)
   - [Module 3 : Comptabilité Analytique & Chantiers](#module-3--comptabilité-analytique--chantiers)
   - [Module 4 : Gestion Budgétaire](#module-4--gestion-budgétaire)
   - [Module 5 : Révision Comptable & Audit](#module-5--révision-comptable--audit)
   - [Module 6 : Recouvrement & Impayés](#module-6--recouvrement--impayés)
3. [Architecture des Données (Entités & Modèle Relationnel)](#3-architecture-des-données-entités--modèle-relationnel)
4. [Architecture Frontend (Next.js Routes & Composants)](#4-architecture-frontend-nextjs-routes--composants)
5. [Roadmap d'Implémentation (4 Phases)](#5-roadmap-dimplémentation-4-phases)
6. [Checklist Complète de Suivi](#6-checklist-complète-de-suivi)

---

## 1. VUE D'ENSEMBLE & SYNERGIE AVEC L'EXISTANT

L'ERP dispose déjà des modules suivants :
- **Clients & Fournisseurs** (avec gestion de crédits et catégorisations)
- **Ventes / POS & Factures** (calculs HT, TVA, TTC, échéances)
- **Paiements** (Espèces, Chèques, Virements, Traçabilité)
- **Chantiers & Commandes Clients**

Les 6 nouveaux modules viennent automatiser la comptabilité et le pilotage financier :

```
[ Facturation / Ventes ] ───► [ Moteur Écritures Auto ] ───► [ Grand Livre / Balance PCGM ]
[ Achats / Fournisseurs ] ──► [ IA & OCR Factures/Tickets ] ─► [ Rapprochement Bancaire ]
[ Chantiers & Projets ]  ───► [ Analytique & Coûts ]     ───► [ États Financiers (CPC, Bilan) ]
[ Factures Échues ]      ───► [ Recouvrement & Relances ] ───► [ Révision Comptable & Clôture ]
```

---

## 2. SPÉCIFICATIONS TECHNIQUES DES 6 MODULES

### MODULE 1 : COMPTABILITÉ GÉNÉRALE & IA / OCR

#### Spécificités Marocaines
- **PCGM (Plan Comptable Général Marocain)** : Classes 1 à 8 avec plan de comptes personnalisable.
- **TVA SIMPL** : Taux usuels marocains (20%, 14%, 10%, 7%), régime des encaissements / débits.
- **Retenues à la source (RAS)** : Gestion des attestations RAS clients et déclarations fournisseurs.
- **États financiers légaux** : Bilan, CPC (Compte de Produits et Charges), ESG (État des Soldes de Gestion), Tableau de Financement.

#### Pipeline OCR & IA Documentaire
1. **Dépôt** : Upload drag-and-drop de PDF, scans ou photos de tickets/factures fournisseurs.
2. **Extraction IA** :
   - Extraction des métadonnées clés : Fournisseur, N° ICE, Date, N° Facture, Total HT, Taux TVA, Montant TVA, Total TTC.
3. **Moteur d'écritures proposées** :
   - Exemple d'écriture générée automatiquement :
     - **Débit 6111** (Achats de marchandises) : Montant HT
     - **Débit 3455** (État - TVA récupérable) : Montant TVA
     - **Crédit 4411** (Fournisseurs - Compte Tiers) : Montant TTC
4. **Validation IHM en 1 clic** : Interface split-screen (document original à gauche, écriture modifiable à droite).
5. **Relevé bancaire intelligent** : Import de fichiers bancaires (CSV, OFX, Excel), parsing automatique et assistance au rapprochement/lettrage avec les factures et règlements.

---

### MODULE 2 : GESTION DES IMMOBILISATIONS

- **Fichier des actifs** : Immobilisations corporelles et incorporelles avec date de mise en service, valeur d'acquisition et compte d'imputation (classe 2).
- **Modes d'amortissement** :
  - **Linéaire** : Calcul au *prorata temporis* ($Taux = 100 / Dur\acute{e}e$).
  - **Dégressif marocain** : Application des coefficients fiscaux légaux marocains :
    - 3 à 4 ans : Coefficient **1.5**
    - 5 à 6 ans : Coefficient **2.0**
    - Plus de 6 ans : Coefficient **3.0**
- **Automatisations comptables** :
  - Génération automatique du plan d'amortissement prévisionnel par exercice.
  - Calcul et comptabilisation automatique des dotations de fin d'année (`619x` Débit / `28xx` Crédit).
  - Gestion des cessions, mises au rebut, calcul de la VNA et génération des écritures de sortie (`6513` / `7513`).
  - Inventaire physique avec génération d'étiquettes / QR codes.

---

### MODULE 3 : COMPTABILITÉ ANALYTIQUE & CHANTIERS

- **Axes analytiques** :
  - **Axe 1 : Chantiers / Projets** (connecté directement au module Chantier existant).
  - **Axe 2 : Agences / Sites géographiques**.
  - **Axe 3 : Activités / Pôles de compétences**.
- **Ventilation des charges et produits** :
  - Imputation directe à la ligne d'écriture ou via grilles de répartition en pourcentage.
- **Reporting analytique** :
  - Grand livre et balance déclinés par section.
  - Compte de résultat dégagé par chantier pour évaluer la marge brute et nette par projet.

---

### MODULE 4 : GESTION BUDGÉTAIRE

- **Définition des budgets** :
  - Par exercice comptable et par poste de charge/produit.
  - Ventilation mensuelle avec grille de saisie rapide et report automatique.
- **Comparatif Temps Réel Budget vs Réalisé** :
  - Réalisé calculé automatiquement depuis les écritures comptables validées.
  - Écarts calculés en valeur absolue et en pourcentage.
- **Tableau de bord de pilotage** :
  - Indicateurs clés (KPIs) : Total alloué, Consommé, Solde disponible.
  - Alertes visuelles de surconsommation budgétaire (seuils 80%, 100%, dépassements).
  - Graphiques de cumul et de répartition mensuelle.

---

### MODULE 5 : RÉVISION COMPTABLE & AUDIT

- **Dossier structuré par cycles d'audit PCGM** :
  - Cycle 1 : Capitaux propres, dettes financières & trésorerie
  - Cycle 2 : Immobilisations
  - Cycle 3 : Achats & Fournisseurs
  - Cycle 4 : Ventes & Clients
  - Cycle 5 : Personnel & Organismes sociaux (CNSS, CIMR, AMO)
  - Cycle 6 : État & Fiscalité (TVA, IS, Taxe pro, Retenues)
  - Cycle 7 : Stocks & En-cours
- **Justification & Diligences** :
  - Diligences d'audit prédéfinies à cocher.
  - Attachement direct des pièces justificatives (contrats, attestations bancaires, états des tiers) aux comptes audités.
- **Workflow de visa à 3 niveaux** :
  1. Visa Collaborateur / Préparateur
  2. Visa Chef de Mission / Réviseur
  3. Visa Associé / Expert-comptable avant clôture définitive de l'exercice

---

### MODULE 6 : RECOUVREMENT & IMPAYÉS

- **Centralisation des impayés** : Détection en temps réel des factures échues non soldées.
- **Tableau de bord de recouvrement** :
  - En-cours total, Reste à recouvrer, Montants échus par tranche (0-30j, 31-60j, 61-90j, >90j).
  - Balance âgée interactive clients.
- **Scénarios de relances automatisés** :
  - Niveau 1 : Relance préventive / courtoise (Email / SMS automatique).
  - Niveau 2 : Relance ferme avec état de compte des factures impayées.
  - Niveau 3 : Mise en demeure formelle avec génération de courrier PDF prêt pour recommandé.
- **Suivi des dossiers** :
  - Gestion des promesses de paiement avec date d'engagement et montant.
  - Échéanciers d'apurement échelonnés.
  - Bascule en dossier contentieux avec journal d'audit complet de toutes les actions passées.

---

## 3. ARCHITECTURE DES DONNÉES (ENTITÉS BACKEND)

### Nouveaux Packages Java proposés dans `stock-manager` :
- `com.gestion.comptabilite.entity`
- `com.gestion.comptabilite.repository`
- `com.gestion.comptabilite.service`
- `com.gestion.comptabilite.controller`
- `com.gestion.immobilisation.*`
- `com.gestion.analytique.*`
- `com.gestion.budget.*`
- `com.gestion.revision.*`
- `com.gestion.recouvrement.*`
- `com.gestion.ocr.*`

### Schéma des Entités Clés :

```java
// 1. Comptabilité Générale
@Entity CompteComptable {
    Long id;
    String numeroCompte; // Ex: 61110000
    String libelle;       // Ex: Achats de marchandises
    Integer classe;       // 1 à 8
    Boolean actif;
}

@Entity JournalComptable {
    Long id;
    String code;         // VT, AC, BQ, CA, OD
    String libelle;
}

@Entity EcritureComptable {
    Long id;
    String numeroPiece;
    LocalDate dateEcriture;
    String libelle;
    Boolean validee;     // Immuable une fois validée
    @ManyToOne JournalComptable journal;
    @OneToMany List<LigneEcriture> lignes;
}

@Entity LigneEcriture {
    Long id;
    @ManyToOne CompteComptable compte;
    BigDecimal debit;
    BigDecimal credit;
    String referencePiece;
    String lettrage;     // Ex: "AA" pour les rapprochements
    @ManyToOne SectionAnalytique sectionAnalytique; // Lien Analytique optionnel
}

// 2. Immobilisations
@Entity Immobilisation {
    Long id;
    String code;
    String designation;
    LocalDate dateAcquisition;
    LocalDate dateMiseEnService;
    BigDecimal valeurAcquisition;
    Integer dureeAnnees;
    @Enumerated ModeAmortissement mode; // LINEAIRE, DEGRESSIF_MAROCAIN
    @ManyToOne CompteComptable compteImmobilisation; // 2xxx
    @ManyToOne CompteComptable compteAmortissement;   // 28xx
    @ManyToOne CompteComptable compteDotation;        // 619x
}

// 3. Analytique
@Entity AxeAnalytique {
    Long id;
    String nom;          // CHANTIER, SITE, POLE
}

@Entity SectionAnalytique {
    Long id;
    String code;
    String libelle;
    @ManyToOne AxeAnalytique axe;
    @ManyToOne Chantier chantier; // Lien natif vers votre entité Chantier
}

// 4. Budget
@Entity EnveloppeBudgetaire {
    Long id;
    Integer anneeExercice;
    @ManyToOne CompteComptable compte;
    @ManyToOne SectionAnalytique section;
    BigDecimal montantAnnuel;
    BigDecimal m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12;
}

// 5. Recouvrement
@Entity DossierRecouvrement {
    Long id;
    @ManyToOne Client client;
    @Enumerated StatutRecouvrement statut; // AMIABLE, CONTENTIEUX, SOLDE
    BigDecimal montantTotalEchu;
    @OneToMany List<ActionRelance> actions;
    @OneToMany List<PromessePaiement> promesses;
}

// 6. Révision Comptable
@Entity DossierRevision {
    Long id;
    Integer anneeExercice;
    @Enumerated StatutRevision statut; // EN_COURS, CLOTURE
    @OneToMany List<CycleRevision> cycles;
}
```

---

## 4. ARCHITECTURE FRONTEND (NEXT.JS ROUTES & COMPOSANTS)

Structure d'arborescence recommandée dans `agenceweb/app/(dashboard)/` :

```
agenceweb/
├── app/(dashboard)/
│   ├── comptabilite/
│   │   ├── plan-comptable/page.tsx      # Gestion du PCGM marocain
│   │   ├── ecritures/page.tsx           # Saisie au journal & consultation
│   │   ├── grand-livre/page.tsx         # Grand Livre interactif
│   │   ├── balance/page.tsx             # Balance générale (débit/crédit/solde)
│   │   ├── ocr-saisie-ia/page.tsx       # Scanner IA & validation d'écritures
│   │   └── banque-rapprochement/page.tsx # Import relevés & lettrage bancaire
│   ├── immobilisations/
│   │   ├── page.tsx                     # Fichier des immo & tableaux d'amortissement
│   │   └── cessions/page.tsx            # Sorties d'actifs & écritures fiscales
│   ├── analytique/
│   │   ├── axes/page.tsx                # Configuration des axes & sections
│   │   └── rentabilite-chantiers/page.tsx # Analyse de marges par chantier
│   ├── budget/
│   │   ├── page.tsx                     # Grille de prévisions mensuelles
│   │   └── dashboard/page.tsx           # Comparateur graphique Réalisé vs Budget
│   ├── revision/
│   │   ├── dossier/page.tsx             # Audit par cycles & pièces jointes
│   │   └── visas/page.tsx               # Workflow de signature à 3 niveaux
│   └── recouvrement/
│       ├── page.tsx                     # Tableau de bord encours & balance âgée
│       ├── relances/page.tsx            # Historique des relances (SMS/Email/Courrier)
│       └── promesses/page.tsx           # Suivi des échéanciers clients
```

---

## 5. ROADMAP D'IMPLÉMENTATION (4 PHASES)

### 🟢 Phase 1 : Fondations Comptables PCGM & Banque (Semaines 1-3)
- **Objectif** : Avoir un moteur comptable complet et la passerelle automatique depuis les ventes et paiements actuels.
- **Livrables** :
  1. Base de données initialisée avec le PCGM standard marocain.
  2. Génération automatique d'écritures lors de la validation d'une facture ou d'un encaissement.
  3. Écrans Consultation du Journal, du Grand Livre et de la Balance.
  4. Import de relevé bancaire et module de rapprochement.

### 🟡 Phase 2 : Automatisation IA/OCR & Recouvrement (Semaines 4-6)
- **Objectif** : Éliminer la saisie manuelle des charges et sécuriser la trésorerie client.
- **Livrables** :
  1. Interface d'upload et service OCR pour tickets et factures d'achat.
  2. Proposition automatique d'écritures (6111 / 3455 / 4411) avec écran de validation split-view.
  3. Module complet de recouvrement avec balance âgée et scénarios de relance.
  4. Suivi des promesses de paiement et bascule en contentieux.

### 🟠 Phase 3 : Immobilisations & Analytique Chantiers (Semaines 7-8)
- **Objectif** : Suivi rigoureux du patrimoine et maîtrise du coût de revient par chantier.
- **Livrables** :
  1. Module des immobilisations (amortissements linéaires et dégressifs marocains).
  2. Génération des dotations d'inventaire de clôture.
  3. Axe analytique Chantier branché sur l'entité Chantier existante.
  4. États de rentabilité par chantier.

### 🟣 Phase 4 : Gestion Budgétaire & Révision Comptable (Semaines 9-10)
- **Objectif** : Pilotage prévisionnel de l'entreprise et préparation de la clôture fiscale.
- **Livrables** :
  1. Grille de saisie budgétaire mensuelle par poste.
  2. Tableaux de bord graphiques Recharts (Budget vs Réalisé).
  3. Dossier de révision comptable découpé par cycles d'audit avec pièces justificatives.
  4. Système de visa de validation à 3 niveaux avant clôture de l'exercice.

---

## 6. CHECKLIST COMPLÈTE DE SUIVI

### Phase 1 : Comptabilité & Banque
- [ ] Création de la table `compte_comptable` et script d'insertion du PCGM marocain.
- [ ] Création des tables `journal_comptable`, `ecriture_comptable`, `ligne_ecriture`.
- [ ] Service de validation comptable (règle stricte Total Débit = Total Crédit).
- [ ] Connecteur Ventes : génération de l'écriture lors de la validation d'une facture client (Débit 3421 / Crédit 7111 & 4455).
- [ ] Connecteur Paiements : génération de l'écriture lors de l'encaissement (Débit 5141 ou 5161 / Crédit 3421).
- [ ] Écran Frontend Next.js : Table du Plan Comptable avec recherche et filtre par classe.
- [ ] Écran Frontend Next.js : Journal des Écritures avec filtres par date et journal.
- [ ] Écran Frontend Next.js : Grand Livre et Balance Générale avec export Excel/PDF.
- [ ] Table `releve_bancaire` et parsing des fichiers CSV bancaires.
- [ ] Écran Frontend Next.js : Interface de pointage et lettrage bancaire à deux colonnes.

### Phase 2 : IA OCR & Recouvrement
- [ ] Endpoint Spring Boot `POST /api/comptabilite/ocr/upload` recevant PDF / images.
- [ ] Intégration du prompt d'extraction IA des mentions fiscales marocaines (ICE, HT, TVA, TTC).
- [ ] Moteur de mapping automatique Fournisseur $\rightarrow$ Compte 4411 et Charges $\rightarrow$ Compte 61xx.
- [ ] Composant Next.js Split-View : prévisualisation du document à gauche, formulaire d'écriture à droite.
- [ ] Entité `DossierRecouvrement` et synchronisation avec les factures échues.
- [ ] Calcul automatique des tranches d'ancienneté (balance âgée client).
- [ ] Templates de relance prédéfinis (Niveau 1, Niveau 2, Mise en demeure).
- [ ] Écran Frontend Next.js : Tableau de bord de recouvrement avec KPIs et actions rapides.
- [ ] Gestion des promesses de paiement avec alertes de date d'échéance.

### Phase 3 : Immobilisations & Analytique
- [ ] Entité `Immobilisation` et tables associées.
- [ ] Algorithmes de calcul : Amortissement linéaire et dégressif marocain.
- [ ] Génération automatique de l'échéancier prévisionnel de dotations.
- [ ] Bouton de comptabilisation des dotations de fin d'année (Écritures au journal des OD).
- [ ] Gestion des cessions d'actifs avec calcul automatique de la VNA.
- [ ] Entités `AxeAnalytique` et `SectionAnalytique`.
- [ ] Liaison directe des sections analytiques aux Chantiers existants.
- [ ] Ajout de la colonne "Section Analytique" sur les lignes de factures et d'écritures.
- [ ] Écran Frontend Next.js : Tableau de bord de rentabilité par chantier.

### Phase 4 : Budget & Révision Comptable
- [ ] Entité `EnveloppeBudgetaire` avec répartition sur les 12 mois.
- [ ] Calcul dynamique du réalisé comptable par rapport aux lignes budgétaires.
- [ ] Écran Frontend Next.js : Grille de saisie type Excel pour les prévisions budgétaires.
- [ ] Écran Frontend Next.js : Graphiques comparatifs réalisés vs budget avec jauges d'alerte.
- [ ] Entités `DossierRevision`, `CycleRevision`, `Diligence`.
- [ ] Arborescence des 7 cycles d'audit PCGM pré-paramétrés.
- [ ] Module d'attachement de fichiers justificatifs par compte révisé.
- [ ] Workflow de signature à 3 niveaux (Collaborateur $\rightarrow$ Réviseur $\rightarrow$ Expert).
- [ ] Génération des états de synthèse légaux marocains (Bilan, CPC).

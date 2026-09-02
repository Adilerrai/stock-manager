import type { Client, Fournisseur, Produit, Depot, QualiteProduit } from "../types/api";

// ─── Devis ───────────────────────────────────────────────────────────────────

export enum StatutDevis {
  BROUILLON = "BROUILLON",
  ENVOYE = "ENVOYE",
  ACCEPTE = "ACCEPTE",
  REFUSE = "REFUSE",
  EXPIRE = "EXPIRE",
  TRANSFORME_EN_COMMANDE = "TRANSFORME_EN_COMMANDE",
}

export interface LigneDevis {
  id?: number;
  produit?: Produit;
  produitId?: number;
  quantite: number;
  prixUnitaireHT: number;
  tauxTVA?: number;
  tauxRemise?: number;
  montantHT?: number;
  montantTVA?: number;
  montantTTC?: number;
  description?: string;
}

export interface Devis {
  id?: number;
  numeroDevis?: string;
  dateDevis?: string;
  dateValidite?: string;
  client?: Client;
  creePar?: any;
  lignes?: LigneDevis[];
  montantHT?: number;
  montantTVA?: number;
  montantTTC?: number;
  remiseGlobale?: number;
  montantFinal?: number;
  statut?: StatutDevis;
  notes?: string;
  conditionsPaiement?: string;
  pointDeVenteId?: number;
  commandeGenereeId?: number;
  factureGenereeId?: number;
  dateCreation?: string;
}

// ─── Avoirs ──────────────────────────────────────────────────────────────────

export enum TypeAvoir {
  CLIENT = "CLIENT",
  FOURNISSEUR = "FOURNISSEUR",
}

export enum StatutAvoir {
  BROUILLON = "BROUILLON",
  VALIDE = "VALIDE",
  UTILISE = "UTILISE",
  REMBOURSE = "REMBOURSE",
  ANNULE = "ANNULE",
}

export interface LigneAvoir {
  id?: number;
  produit?: Produit;
  quantite: number;
  prixUnitaireHT: number;
  tauxTVA?: number;
  montantHT?: number;
  montantTVA?: number;
  montantTTC?: number;
  remettreEnStock?: boolean;
  motif?: string;
}

export interface Avoir {
  id?: number;
  numeroAvoir?: string;
  typeAvoir: TypeAvoir;
  factureOrigineId?: number;
  numeroFactureOrigine?: string;
  client?: Client;
  fournisseur?: Fournisseur;
  dateAvoir?: string;
  lignes?: LigneAvoir[];
  montantHT?: number;
  montantTVA?: number;
  montantTTC?: number;
  statut?: StatutAvoir;
  motif?: string;
  notes?: string;
  dateCreation?: string;
}

// ─── Inventaires ─────────────────────────────────────────────────────────────

export enum StatutInventaire {
  BROUILLON = "BROUILLON",
  EN_COURS = "EN_COURS",
  VALIDE = "VALIDE",
  ANNULE = "ANNULE",
}

export interface LigneInventaire {
  id?: number;
  produit?: Produit;
  qualite?: QualiteProduit;
  quantiteTheorique: number;
  quantiteReelle: number;
  ecart?: number;
  prixUnitaire?: number;
  valeurEcart?: number;
}

export interface Inventaire {
  id?: number;
  reference?: string;
  dateInventaire?: string;
  depot?: Depot;
  responsable?: any;
  statut?: StatutInventaire;
  lignes?: LigneInventaire[];
  totalEcartPositif?: number;
  totalEcartNegatif?: number;
  valeurTotaleEcart?: number;
  notes?: string;
  dateCreation?: string;
  dateValidation?: string;
}

// ─── Variantes Produit ───────────────────────────────────────────────────────

export interface VarianteProduit {
  id?: number;
  sku: string;
  codeBarre?: string;
  nomVariante: string;
  taille?: string;
  couleur?: string;
  dimension?: string;
  prixVente?: number;
  prixAchat?: number;
  quantiteStock?: number;
  actif?: boolean;
  dateCreation?: string;
}

// ─── Session Caisse ──────────────────────────────────────────────────────────

export enum StatutSessionCaisse {
  OUVERTE = "OUVERTE",
  CLOTUREE = "CLOTUREE",
}

export interface SessionCaisse {
  id?: number;
  reference?: string;
  dateOuverture?: string;
  dateCloture?: string;
  caissier?: any;
  fondDeCaisseInitial: number;
  totalVentes?: number;
  totalEspeces?: number;
  totalCarte?: number;
  totalCheque?: number;
  totalVirement?: number;
  totalCredit?: number;
  montantTheoriqueCloture?: number;
  montantReelCloture?: number;
  ecartCaisse?: number;
  statut?: StatutSessionCaisse;
  notes?: string;
}

// ─── Trésorerie, Remises & Balance Âgée ───────────────────────────────────────

export enum StatutRemise {
  BROUILLON = "BROUILLON",
  REMIS_EN_BANQUE = "REMIS_EN_BANQUE",
  ENCAISSE = "ENCAISSE",
  REJETE = "REJETE",
  ANNULE = "ANNULE",
}

export interface BordereauRemise {
  id?: number;
  numeroBordereau?: string;
  dateRemise?: string;
  nomBanque: string;
  compteBancaire?: string;
  typeValeur?: string;
  montantTotal?: number;
  nombreValeurs?: number;
  statut?: StatutRemise;
  notes?: string;
  dateCreation?: string;
}

export interface LigneReleve {
  date: string;
  typeOperation: "FACTURE" | "PAIEMENT" | "AVOIR";
  reference: string;
  libelle: string;
  debit: number;
  credit: number;
  soldeProgressif: number;
}

export interface ReleveClient {
  clientId: number;
  clientNom: string;
  telephone?: string;
  email?: string;
  ice?: string;
  creditAutorise?: number;
  soldeActuel: number;
  totalFactures: number;
  totalPaiements: number;
  totalAvoirs: number;
  operations: LigneReleve[];
}

export interface LigneBalanceAgee {
  tiersId: number;
  tiersNom: string;
  telephone?: string;
  totalDu: number;
  nonEchu: number;
  moins30J: number;
  de30A60J: number;
  de60A90J: number;
  plus90J: number;
}

export interface BalanceAgee {
  totalCreances: number;
  totalNonEchu: number;
  totalMoins30J: number;
  total30A60J: number;
  total60A90J: number;
  totalPlus90J: number;
  tiers: LigneBalanceAgee[];
}

export interface LigneEcheance {
  dateEcheance: string;
  sens: "ENCAISSEMENT" | "DECAISSEMENT";
  tiersNom: string;
  typeDocument: string;
  reference: string;
  montant: number;
  statut: string;
}

export interface Echeancier {
  totalAEncaisser: number;
  totalAPayer: number;
  soldePrevisionnel: number;
  echeances: LigneEcheance[];
}

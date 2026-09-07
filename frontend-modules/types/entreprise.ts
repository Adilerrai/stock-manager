export interface Entreprise {
  id: number;
  tenantId: number;
  nomEntreprise: string;
  activite?: string;
  adresse?: string;
  ville?: string;
  codePostal?: string;
  telephone?: string;
  email?: string;
  siteWeb?: string;
  devise?: string;
  registreCommerce?: string;
  numeroIdentificationFiscale?: string;
  numeroIdentificationStatistique?: string;
  articleImposition?: string;
  compteBancaireRib?: string;
  nomBanque?: string;
  actif: boolean;
  dateCreation: string;
  nombreUtilisateurs: number;
  adminUserId?: number;
  adminEmail?: string;
  adminUsername?: string;
  adminNomComplet?: string;
}

export interface EntrepriseRegistrationRequest {
  nomEntreprise: string;
  activite?: string;
  adresse?: string;
  ville?: string;
  codePostal?: string;
  telephone?: string;
  email?: string;
  siteWeb?: string;
  devise?: string;
  registreCommerce?: string;
  numeroIdentificationFiscale?: string;
  numeroIdentificationStatistique?: string;
  articleImposition?: string;
  compteBancaireRib?: string;
  nomBanque?: string;
  adminEmail: string;
  adminUsername: string;
  adminPassword: string;
  adminNomComplet: string;
  adminTelephone?: string;
}

export interface EntrepriseUpdateRequest {
  nomEntreprise: string;
  activite?: string;
  adresse?: string;
  ville?: string;
  codePostal?: string;
  telephone?: string;
  email?: string;
  siteWeb?: string;
  devise?: string;
  registreCommerce?: string;
  numeroIdentificationFiscale?: string;
  numeroIdentificationStatistique?: string;
  articleImposition?: string;
  compteBancaireRib?: string;
  nomBanque?: string;
  actif?: boolean;
}

export interface UserDTO {
  id: number;
  email: string;
  username: string;
  nomComplet: string;
  telephone?: string;
  genre?: "HOMME" | "FEMME";
  role: string;
  pointDeVenteId: number;
  enabled: boolean;
}

export interface UserCreationRequest {
  email: string;
  username: string;
  password: string;
  nomComplet: string;
  telephone?: string;
  genre?: "HOMME" | "FEMME";
  role: string;
  pointDeVenteId?: number;
}

export interface UserUpdateRequest {
  nomComplet?: string;
  email?: string;
  username?: string;
  telephone?: string;
  genre?: "HOMME" | "FEMME";
  role?: string;
  enabled?: boolean;
}

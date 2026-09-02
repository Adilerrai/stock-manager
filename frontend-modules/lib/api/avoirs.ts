import { apiClient } from "./client";
import type { Avoir, TypeAvoir } from "@/types/commercial";

export const avoirsApi = {
  getAllAvoirs(type?: TypeAvoir): Promise<Avoir[]> {
    const query = type ? `?type=${type}` : "";
    return apiClient.get<Avoir[]>(`/v1/avoirs${query}`);
  },

  getAvoirById(id: number): Promise<Avoir> {
    return apiClient.get<Avoir>(`/v1/avoirs/${id}`);
  },

  getAvoirsByClient(clientId: number): Promise<Avoir[]> {
    return apiClient.get<Avoir[]>(`/v1/avoirs/client/${clientId}`);
  },

  getAvoirsByFournisseur(fournisseurId: number): Promise<Avoir[]> {
    return apiClient.get<Avoir[]>(`/v1/avoirs/fournisseur/${fournisseurId}`);
  },

  creerAvoir(avoir: Partial<Avoir>, userId?: number): Promise<Avoir> {
    const query = userId ? `?userId=${userId}` : "";
    return apiClient.post<Avoir>(`/v1/avoirs${query}`, avoir);
  },

  creerDepuisFacture(factureId: number, motif?: string, userId?: number): Promise<Avoir> {
    const params = new URLSearchParams();
    if (motif) params.append("motif", motif);
    if (userId) params.append("userId", userId.toString());
    const query = params.toString() ? `?${params.toString()}` : "";
    return apiClient.post<Avoir>(`/v1/avoirs/depuis-facture/${factureId}${query}`);
  },

  creerDepuisFactureAchat(factureAchatId: number, motif?: string, userId?: number): Promise<Avoir> {
    const params = new URLSearchParams();
    if (motif) params.append("motif", motif);
    if (userId) params.append("userId", userId.toString());
    const query = params.toString() ? `?${params.toString()}` : "";
    return apiClient.post<Avoir>(`/v1/avoirs/depuis-facture-achat/${factureAchatId}${query}`);
  },

  validerAvoir(id: number, depotId?: number): Promise<Avoir> {
    const query = depotId ? `?depotId=${depotId}` : "";
    return apiClient.post<Avoir>(`/v1/avoirs/${id}/valider${query}`);
  },

  supprimerAvoir(id: number): Promise<void> {
    return apiClient.delete<void>(`/v1/avoirs/${id}`);
  },
};

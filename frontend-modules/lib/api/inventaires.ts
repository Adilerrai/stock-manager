import { apiClient } from "./client";
import type { Inventaire, LigneInventaire } from "@/types/commercial";

export const inventairesApi = {
  getAllInventaires(): Promise<Inventaire[]> {
    return apiClient.get<Inventaire[]>("/v1/inventaires");
  },

  getInventaireById(id: number): Promise<Inventaire> {
    return apiClient.get<Inventaire>(`/v1/inventaires/${id}`);
  },

  getInventairesByDepot(depotId: number): Promise<Inventaire[]> {
    return apiClient.get<Inventaire[]>(`/v1/inventaires/depot/${depotId}`);
  },

  demarrerInventaire(depotId: number, notes?: string, userId?: number): Promise<Inventaire> {
    const params = new URLSearchParams();
    params.append("depotId", depotId.toString());
    if (notes) params.append("notes", notes);
    if (userId) params.append("userId", userId.toString());
    return apiClient.post<Inventaire>(`/v1/inventaires/demarrer?${params.toString()}`);
  },

  mettreAJourLignes(id: number, lignes: LigneInventaire[]): Promise<Inventaire> {
    return apiClient.put<Inventaire>(`/v1/inventaires/${id}/lignes`, lignes);
  },

  validerInventaire(id: number, userId?: number): Promise<Inventaire> {
    const query = userId ? `?userId=${userId}` : "";
    return apiClient.post<Inventaire>(`/v1/inventaires/${id}/valider${query}`);
  },

  annulerInventaire(id: number): Promise<void> {
    return apiClient.post<void>(`/v1/inventaires/${id}/annuler`);
  },
};

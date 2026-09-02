import { apiClient } from "./client";
import type { Devis, StatutDevis } from "@/types/commercial";
import type { CommandeClientDTO, Facture } from "@/types/api";

export const devisApi = {
  getAllDevis(): Promise<Devis[]> {
    return apiClient.get<Devis[]>("/v1/devis");
  },

  getDevisById(id: number): Promise<Devis> {
    return apiClient.get<Devis>(`/v1/devis/${id}`);
  },

  getDevisByClient(clientId: number): Promise<Devis[]> {
    return apiClient.get<Devis[]>(`/v1/devis/client/${clientId}`);
  },

  getDevisByStatut(statut: StatutDevis): Promise<Devis[]> {
    return apiClient.get<Devis[]>(`/v1/devis/statut/${statut}`);
  },

  creerDevis(devis: Partial<Devis>, userId?: number): Promise<Devis> {
    const query = userId ? `?userId=${userId}` : "";
    return apiClient.post<Devis>(`/v1/devis${query}`, devis);
  },

  modifierDevis(id: number, devis: Partial<Devis>): Promise<Devis> {
    return apiClient.put<Devis>(`/v1/devis/${id}`, devis);
  },

  changerStatut(id: number, statut: StatutDevis): Promise<Devis> {
    return apiClient.patch<Devis>(`/v1/devis/${id}/statut?statut=${statut}`);
  },

  convertirEnCommande(id: number, userId?: number): Promise<CommandeClientDTO> {
    const query = userId ? `?userId=${userId}` : "";
    return apiClient.post<CommandeClientDTO>(`/v1/devis/${id}/convertir-commande${query}`);
  },

  convertirEnFacture(id: number, userId?: number): Promise<Facture> {
    const query = userId ? `?userId=${userId}` : "";
    return apiClient.post<Facture>(`/v1/devis/${id}/convertir-facture${query}`);
  },

  supprimerDevis(id: number): Promise<void> {
    return apiClient.delete<void>(`/v1/devis/${id}`);
  },
};

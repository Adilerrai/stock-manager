import { apiClient } from "./client";
import type {
  ReleveClient,
  BalanceAgee,
  Echeancier,
  BordereauRemise,
  StatutRemise,
} from "@/types/commercial";

export const tresorerieApi = {
  getReleveClient(clientId: number, dateDebut?: string, dateFin?: string): Promise<ReleveClient> {
    const params = new URLSearchParams();
    if (dateDebut) params.append("dateDebut", dateDebut);
    if (dateFin) params.append("dateFin", dateFin);
    const query = params.toString() ? `?${params.toString()}` : "";
    return apiClient.get<ReleveClient>(`/v1/tresorerie/releve-client/${clientId}${query}`);
  },

  getBalanceAgeeClients(): Promise<BalanceAgee> {
    return apiClient.get<BalanceAgee>("/v1/tresorerie/balance-agee-clients");
  },

  getBalanceAgeeFournisseurs(): Promise<BalanceAgee> {
    return apiClient.get<BalanceAgee>("/v1/tresorerie/balance-agee-fournisseurs");
  },

  getEcheancier(dateDebut?: string, dateFin?: string): Promise<Echeancier> {
    const params = new URLSearchParams();
    if (dateDebut) params.append("dateDebut", dateDebut);
    if (dateFin) params.append("dateFin", dateFin);
    const query = params.toString() ? `?${params.toString()}` : "";
    return apiClient.get<Echeancier>(`/v1/tresorerie/echeancier${query}`);
  },

  getAllBordereaux(): Promise<BordereauRemise[]> {
    return apiClient.get<BordereauRemise[]>("/v1/tresorerie/remises");
  },

  creerBordereau(bordereau: Partial<BordereauRemise>): Promise<BordereauRemise> {
    return apiClient.post<BordereauRemise>("/v1/tresorerie/remises", bordereau);
  },

  changerStatutRemise(id: number, statut: StatutRemise): Promise<BordereauRemise> {
    return apiClient.patch<BordereauRemise>(`/v1/tresorerie/remises/${id}/statut?statut=${statut}`);
  },
};

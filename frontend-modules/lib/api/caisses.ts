import { apiClient } from "./client";
import type { SessionCaisse } from "@/types/commercial";

export const caissesApi = {
  getSessionActive(userId: number): Promise<SessionCaisse | null> {
    return apiClient.get<SessionCaisse | null>(`/v1/caisses/active?userId=${userId}`);
  },

  ouvrirSession(userId: number, fondDeCaisseInitial?: number, notes?: string): Promise<SessionCaisse> {
    const params = new URLSearchParams();
    params.append("userId", userId.toString());
    if (fondDeCaisseInitial !== undefined) params.append("fondDeCaisseInitial", fondDeCaisseInitial.toString());
    if (notes) params.append("notes", notes);
    return apiClient.post<SessionCaisse>(`/v1/caisses/ouvrir?${params.toString()}`);
  },

  cloturerSession(sessionId: number, montantReel: number, notes?: string): Promise<SessionCaisse> {
    const params = new URLSearchParams();
    params.append("montantReel", montantReel.toString());
    if (notes) params.append("notes", notes);
    return apiClient.post<SessionCaisse>(`/v1/caisses/${sessionId}/cloturer?${params.toString()}`);
  },

  getSessionById(id: number): Promise<SessionCaisse> {
    return apiClient.get<SessionCaisse>(`/v1/caisses/${id}`);
  },

  getAllSessions(): Promise<SessionCaisse[]> {
    return apiClient.get<SessionCaisse[]>("/v1/caisses");
  },
};

import { apiClient } from "./client";
import type {
  Entreprise,
  EntrepriseRegistrationRequest,
  EntrepriseUpdateRequest,
  UserDTO,
  UserCreationRequest,
  UserUpdateRequest,
} from "@/types/entreprise";

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const entreprisesApi = {
  listerEntreprises(params?: {
    search?: string;
    actif?: boolean;
    page?: number;
    size?: number;
  }): Promise<PageResult<Entreprise>> {
    const query = new URLSearchParams();
    if (params?.search) query.append("search", params.search);
    if (params?.actif !== undefined) query.append("actif", String(params.actif));
    if (params?.page !== undefined) query.append("page", String(params.page));
    if (params?.size !== undefined) query.append("size", String(params.size));
    const qs = query.toString() ? `?${query.toString()}` : "";
    return apiClient.get<PageResult<Entreprise>>(`/admin/entreprises${qs}`);
  },

  getEntrepriseById(id: number): Promise<Entreprise> {
    return apiClient.get<Entreprise>(`/admin/entreprises/${id}`);
  },

  creerEntreprise(data: EntrepriseRegistrationRequest): Promise<Entreprise> {
    return apiClient.post<Entreprise>("/admin/entreprises/register", data);
  },

  modifierEntreprise(id: number, data: EntrepriseUpdateRequest): Promise<Entreprise> {
    return apiClient.put<Entreprise>(`/admin/entreprises/${id}`, data);
  },

  toggleStatus(id: number, actif: boolean): Promise<Entreprise> {
    return apiClient.patch<Entreprise>(`/admin/entreprises/${id}/status?actif=${actif}`);
  },
};

export const usersApi = {
  getUsers(pointDeVenteId?: number): Promise<UserDTO[]> {
    const qs = pointDeVenteId ? `?pointDeVenteId=${pointDeVenteId}` : "";
    return apiClient.get<UserDTO[]>(`/users${qs}`);
  },

  getUserById(id: number): Promise<UserDTO> {
    return apiClient.get<UserDTO>(`/users/${id}`);
  },

  creerUser(data: UserCreationRequest): Promise<UserDTO> {
    return apiClient.post<UserDTO>("/users", data);
  },

  modifierUser(id: number, data: UserUpdateRequest): Promise<UserDTO> {
    return apiClient.put<UserDTO>(`/users/${id}`, data);
  },

  toggleStatus(id: number, enabled: boolean): Promise<UserDTO> {
    return apiClient.patch<UserDTO>(`/users/${id}/status?enabled=${enabled}`);
  },

  resetPassword(id: number, newPassword: string): Promise<{ message: string }> {
    return apiClient.post<{ message: string }>(`/users/${id}/reset-password`, { newPassword });
  },

  changePassword(oldPassword: string, newPassword: string): Promise<{ message: string }> {
    return apiClient.post<{ message: string }>("/users/change-password", {
      oldPassword,
      newPassword,
    });
  },

  supprimerUser(id: number): Promise<{ message: string }> {
    return apiClient.delete<{ message: string }>(`/users/${id}`);
  },
};

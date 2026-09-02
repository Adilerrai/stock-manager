import { apiClient } from "./client";

export interface CategorieDTO {
  id?: number;
  nom: string;
  code?: string;
  description?: string;
  couleur?: string;
  icone?: string;
  actif?: boolean;
  pointDeVenteId?: number;
  dateCreation?: string;
}

export const categoriesApi = {
  getAllCategories(): Promise<CategorieDTO[]> {
    return apiClient.get<CategorieDTO[]>("/v1/categories");
  },

  getCategorieById(id: number): Promise<CategorieDTO> {
    return apiClient.get<CategorieDTO>(`/v1/categories/${id}`);
  },

  creerCategorie(categorie: Partial<CategorieDTO>): Promise<CategorieDTO> {
    return apiClient.post<CategorieDTO>("/v1/categories", categorie);
  },

  modifierCategorie(id: number, categorie: Partial<CategorieDTO>): Promise<CategorieDTO> {
    return apiClient.put<CategorieDTO>(`/v1/categories/${id}`, categorie);
  },

  supprimerCategorie(id: number): Promise<void> {
    return apiClient.delete<void>(`/v1/categories/${id}`);
  },
};

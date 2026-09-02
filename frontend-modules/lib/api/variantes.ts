import { apiClient } from "./client";
import type { VarianteProduit } from "@/types/commercial";

export const variantesApi = {
  getVariantesByProduit(produitId: number): Promise<VarianteProduit[]> {
    return apiClient.get<VarianteProduit[]>(`/v1/variantes/produit/${produitId}`);
  },

  getVarianteById(id: number): Promise<VarianteProduit> {
    return apiClient.get<VarianteProduit>(`/v1/variantes/${id}`);
  },

  ajouterVariante(produitId: number, variante: Partial<VarianteProduit>): Promise<VarianteProduit> {
    return apiClient.post<VarianteProduit>(`/v1/variantes/produit/${produitId}`, variante);
  },

  modifierVariante(id: number, variante: Partial<VarianteProduit>): Promise<VarianteProduit> {
    return apiClient.put<VarianteProduit>(`/v1/variantes/${id}`, variante);
  },

  ajusterStock(id: number, delta: number): Promise<VarianteProduit> {
    return apiClient.patch<VarianteProduit>(`/v1/variantes/${id}/stock?delta=${delta}`);
  },

  supprimerVariante(id: number): Promise<void> {
    return apiClient.delete<void>(`/v1/variantes/${id}`);
  },
};

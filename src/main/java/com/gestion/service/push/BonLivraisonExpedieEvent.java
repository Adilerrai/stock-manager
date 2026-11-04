package com.gestion.service.push;

/** Porte le tenant du BL : le thread d'envoi n'a ni TenantContext ni utilisateur connecté. */
public record BonLivraisonExpedieEvent(Long blId, String numeroBl, Long pointDeVenteId) {
}

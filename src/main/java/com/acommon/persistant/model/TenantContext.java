package com.acommon.persistant.model;

/**
 * Classe utilitaire pour stocker et récupérer l'ID du tenant dans le thread courant.
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Empêcher l'instanciation
    }

    /**
     * Définit l'ID du tenant pour le thread courant.
     * @param tenantId L'ID du tenant
     */
    public static void setCurrentTenant(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Récupère l'ID du tenant pour le thread courant.
     * @return L'ID du tenant ou null si aucun tenant n'est défini
     */
    public static Long getCurrentTenant() {
        Long tenant = CURRENT_TENANT.get();
        if (tenant != null) {
            return tenant;
        }
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                Long t = user.getTenantId() != null ? user.getTenantId() : user.getPointDeVenteId();
                if (t != null) {
                    CURRENT_TENANT.set(t);
                    return t;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Efface l'ID du tenant pour le thread courant.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
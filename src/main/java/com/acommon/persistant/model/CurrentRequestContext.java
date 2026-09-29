package com.acommon.persistant.model;

/**
 * Contexte de requête pour stocker l'exercice comptable actif et la société
 * dans le thread courant (ThreadLocal), alimenté automatiquement par ExerciceContextFilter
 * à partir des en-têtes HTTP (X-Exercice-Year, X-Exercice-Id, X-Societe-Id).
 */
public class CurrentRequestContext {

    private static final ThreadLocal<Long> CURRENT_EXERCICE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CURRENT_EXERCICE_YEAR = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_SOCIETE_ID = new ThreadLocal<>();

    private CurrentRequestContext() {
        // Empêcher l'instanciation
    }

    public static void setExercice(Long id, Integer year) {
        CURRENT_EXERCICE_ID.set(id);
        CURRENT_EXERCICE_YEAR.set(year);
    }

    public static void setSocieteId(Long societeId) {
        CURRENT_SOCIETE_ID.set(societeId);
    }

    public static Long getId() {
        return CURRENT_EXERCICE_ID.get();
    }

    public static Long getExerciceId() {
        return CURRENT_EXERCICE_ID.get();
    }

    public static Integer getYear() {
        return CURRENT_EXERCICE_YEAR.get();
    }

    public static Long getSocieteId() {
        return CURRENT_SOCIETE_ID.get();
    }

    public static void clear() {
        CURRENT_EXERCICE_ID.remove();
        CURRENT_EXERCICE_YEAR.remove();
        CURRENT_SOCIETE_ID.remove();
    }
}

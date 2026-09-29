package com.acommon.persistant.model;

/**
 * Alias de CurrentRequestContext pour manipulation explicite de l'exercice comptable.
 */
public class CurrentExerciceContext {

    private CurrentExerciceContext() {}

    public static void setExercice(Long id, Integer year) {
        CurrentRequestContext.setExercice(id, year);
    }

    public static Long getId() {
        return CurrentRequestContext.getId();
    }

    public static Long getExerciceId() {
        return CurrentRequestContext.getExerciceId();
    }

    public static Integer getYear() {
        return CurrentRequestContext.getYear();
    }

    public static void clear() {
        CurrentRequestContext.clear();
    }
}

package com.acommon.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes qui doivent effectuer une recherche
 * en tenant compte du contexte multi-tenant.
 *
 * Cette annotation peut être utilisée avec un aspect AOP pour injecter
 * automatiquement le tenant ID dans les requêtes de recherche.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MultitenantSearchMethod {

    /**
     * Indique si la méthode doit échouer si aucun tenant n'est trouvé dans le contexte.
     * @return true si la méthode doit échouer, false sinon
     */
    boolean failIfNoTenant() default true;

    /**
     * Description de l'opération de recherche.
     * @return description de l'opération
     */
    String description() default "";
}

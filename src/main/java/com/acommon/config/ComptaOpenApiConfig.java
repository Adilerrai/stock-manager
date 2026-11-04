package com.acommon.config;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contrat de la comptabilité : un groupe à part, lu par le front pour générer ses types.
 * Le groupe "tout" garde le document complet dans Swagger UI, qui n'affiche que des groupes dès qu'il en existe un.
 */
@Configuration
public class ComptaOpenApiConfig {

    @Bean
    public GroupedOpenApi comptaOpenApi() {
        return GroupedOpenApi.builder().group("compta").pathsToMatch("/api/v1/compta/**")
                .addOpenApiCustomizer(contratStrict()).build();
    }

    @Bean
    public GroupedOpenApi toutOpenApi() {
        return GroupedOpenApi.builder().group("tout").pathsToMatch("/**").build();
    }

    /** Tous les champs requis : un champ absent devient null, jamais undefined. */
    @SuppressWarnings("rawtypes")
    private OpenApiCustomizer contratStrict() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            // @Schema(nullable = true) sur un champ objet marque tout le composant : un élément de liste passerait pour nullable.
            // Le nullable revient sur le champ, seul endroit où il a un sens.
            Set<String> nullables = new HashSet<>();
            schemas.forEach((nom, schema) -> {
                if (Boolean.TRUE.equals(schema.getNullable())) {
                    nullables.add("#/components/schemas/" + nom);
                    schema.setNullable(null);
                }
            });
            for (Schema<?> schema : schemas.values()) {
                Map<String, Schema> proprietes = schema.getProperties();
                if (proprietes != null) {
                    schema.setRequired(new ArrayList<>(new TreeSet<>(proprietes.keySet())));
                    proprietes.replaceAll((nom, p) -> nullables.contains(p.get$ref())
                            ? new ComposedSchema().addAllOfItem(new Schema<>().$ref(p.get$ref())).nullable(true)
                            : p);
                    // Une colonne sans valeur, par exemple N-1 non repris, sort à null : la carte le dit.
                    proprietes.values().stream().filter(p -> p.getAdditionalProperties() instanceof Schema).forEach(p -> ((Schema) p.getAdditionalProperties()).setNullable(true));
                    // @JsonFormat sort un montant en chaîne, mais le modèle le voit encore comme le BigDecimal du champ.
                    proprietes.values().stream().filter(p -> "number".equals(p.getType())).forEach(p -> {
                        p.setType("string");
                        p.setFormat("decimal");
                        p.setExample("1200.00");
                    });
                }
            }
        };
    }
}

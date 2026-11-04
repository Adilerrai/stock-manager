package com.gestion.persistent.dto.compta;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Un montant sort en chaîne à deux décimales : le front ne peut pas additionner de flottants. Le commercial garde ses nombres. */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonFormat(shape = JsonFormat.Shape.STRING)
@Schema(type = "string", format = "decimal", example = "1200.00")
public @interface MontantJson {
}

package com.acommon.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Un corps destiné au module comptable refuse un champ inconnu : un champ mal nommé ne s'ignore pas en silence. Ce convertisseur ne lit que les DTO du module
 * et n'écrit rien. Il n'hérite pas de MappingJackson2HttpMessageConverter, pour ne pas remplacer celui de Boot : le commercial garde son comportement.
 */
@Component
public class ComptaEntreeJsonConverter extends AbstractJackson2HttpMessageConverter {

    private static final String PAQUET = "com.gestion.persistent.dto.compta.";

    public ComptaEntreeJsonConverter(ObjectMapper mapper) {
        super(mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES), MediaType.APPLICATION_JSON);
    }

    private static boolean duModule(Type type) {
        if (type instanceof Class<?> classe) {
            return classe.getName().startsWith(PAQUET);
        }
        return type instanceof ParameterizedType parametre && Arrays.stream(parametre.getActualTypeArguments()).anyMatch(ComptaEntreeJsonConverter::duModule);
    }

    @Override
    public boolean canRead(Class<?> classe, MediaType media) {
        return duModule(classe) && super.canRead(classe, media);
    }

    @Override
    public boolean canRead(Type type, Class<?> contexte, MediaType media) {
        return duModule(type) && super.canRead(type, contexte, media);
    }

    @Override
    public boolean canWrite(Class<?> classe, MediaType media) {
        return false;
    }

    @Override
    public boolean canWrite(Type type, Class<?> classe, MediaType media) {
        return false;
    }
}

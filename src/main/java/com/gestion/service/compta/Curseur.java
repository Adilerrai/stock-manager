package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Curseur de page opaque pour le front : jamais d'OFFSET sur les listes du module. */
public final class Curseur {

    public static final int TAILLE_DEFAUT = 100;
    public static final int TAILLE_MAX = 500;

    private Curseur() {
    }

    public static String encoder(String cle) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cle.getBytes(StandardCharsets.UTF_8));
    }

    public static String decoder(String curseur) {
        if (curseur == null || curseur.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(curseur), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CommonException("Curseur de page invalide", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_CURSEUR_INVALIDE");
        }
    }

    public static int taille(Integer demandee) {
        if (demandee == null) {
            return TAILLE_DEFAUT;
        }
        return Math.max(1, Math.min(demandee, TAILLE_MAX));
    }
}

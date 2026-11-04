package com.gestion.service.compta;

import java.util.List;

/** Le périmètre d'une requête. Le commercial se lit par pdvIds, jamais par l'en-tête : un dossier sans commercial a une liste vide. */
public record LivreCourant(Long livreId, List<Long> pdvIds) {
}

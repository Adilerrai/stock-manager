package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.JournalActiviteDTO;
import com.gestion.persistent.dto.JournalActiviteDTO.Evenement;
import com.gestion.persistent.dto.JournalActiviteDTO.Resume;
import com.gestion.persistent.enums.TypeEvenementJournal;
import com.gestion.repository.JournalActiviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Journal d'activité du tableau de bord : ce qui a été fait dans la société (devis, BC, BL, factures,
 * encaissements, règlements...), reconstitué depuis les documents du point de vente courant.
 */
@Service
@Transactional(readOnly = true)
public class JournalActiviteService {

    /** Au-delà, la requête UNION devient coûteuse sans apporter de lisibilité. */
    static final int PERIODE_MAX_JOURS = 366;
    static final int TAILLE_MAX = 200;

    private final JournalActiviteRepository repository;

    public JournalActiviteService(JournalActiviteRepository repository) {
        this.repository = repository;
    }

    public JournalActiviteDTO getJournal(LocalDate dateDebut, LocalDate dateFin, List<String> types,
                                         String recherche, int page, int size) {
        int taille = Math.min(Math.max(size, 1), TAILLE_MAX);
        int numeroPage = Math.max(page, 0);

        Long pdv = TenantContext.getCurrentTenant();
        if (pdv == null || pdv <= 0) {
            // Pas de société résolue : on ne montre jamais un journal multi-sociétés.
            return new JournalActiviteDTO(List.of(), 0, numeroPage, taille, List.of());
        }

        LocalDate fin = dateFin != null ? dateFin : LocalDate.now();
        LocalDate debut = dateDebut != null ? dateDebut : fin;
        if (debut.isAfter(fin)) {
            LocalDate tmp = debut;
            debut = fin;
            fin = tmp;
        }
        if (debut.isBefore(fin.minusDays(PERIODE_MAX_JOURS))) {
            debut = fin.minusDays(PERIODE_MAX_JOURS);
        }
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.plusDays(1).atStartOfDay();

        String q = (recherche == null || recherche.isBlank()) ? null : recherche.trim();
        Set<TypeEvenementJournal> typesDemandes = parseTypes(types);

        List<Resume> resume = repository.resume(pdv, debutDT, finDT, q);
        long total = resume.stream()
                .filter(r -> typesDemandes.contains(TypeEvenementJournal.valueOf(r.type())))
                .mapToLong(Resume::nombre)
                .sum();
        List<Evenement> evenements = total == 0 ? List.of()
                : repository.evenements(pdv, debutDT, finDT, q, typesDemandes, numeroPage * taille, taille);

        return new JournalActiviteDTO(evenements, total, numeroPage, taille, resume);
    }

    /** Types inconnus ignorés ; aucun type valide = tous les types. */
    static Set<TypeEvenementJournal> parseTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return EnumSet.allOf(TypeEvenementJournal.class);
        }
        EnumSet<TypeEvenementJournal> set = EnumSet.noneOf(TypeEvenementJournal.class);
        types.stream()
                .flatMap(t -> Arrays.stream(t.split(",")))
                .map(t -> t.trim().toUpperCase(Locale.ROOT))
                .filter(t -> !t.isEmpty())
                .forEach(t -> {
                    try {
                        set.add(TypeEvenementJournal.valueOf(t));
                    } catch (IllegalArgumentException ignored) {
                        // type inconnu : ignoré
                    }
                });
        return set.isEmpty() ? EnumSet.allOf(TypeEvenementJournal.class) : set;
    }
}

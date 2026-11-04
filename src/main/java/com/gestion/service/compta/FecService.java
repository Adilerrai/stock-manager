package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.FecRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Le FEC, en flux, dans l'ordre et sous les noms de champs de l'avis. L'annexe ne fixe ni le séparateur, ni l'encodage, ni le format de date :
 * ils se choisissent, et le fichier ne se dit pas « format DGI ». Tous les journaux, à-nouveaux et clôture compris ; un brouillon n'y est pas, par construction.
 */
@Service
public class FecService {

    private static final List<String> CHAMPS = List.of("Code_Journal", "Libelle_Journal", "Num_Ecriture", "Date_Ecriture", "Num_Compte", "Libelle_Compte",
            "Num_Compte_Aux", "Libelle_Compte_Aux", "Ref_Piece", "Date_Piece", "Libelle_Ecriture", "Montant_Debit", "Montant_Credit", "Lettrage_Ecriture",
            "Date_Lettrage", "Date_Validation", "Montant_Devise", "Id_devise");
    private static final Set<String> SEPARATEURS = Set.of("|", ";", "\t");
    private static final Set<String> ENCODAGES = Set.of("UTF-8", "ISO-8859-1", "windows-1252");
    private static final Set<String> FORMATS_DATE = Set.of("yyyyMMdd", "yyyy-MM-dd", "dd/MM/yyyy");

    public record OptionsFec(String separateur, String encodage, String formatDate) {
        public static OptionsFec parDefaut() {
            return new OptionsFec("|", "UTF-8", "yyyyMMdd");
        }
    }

    /** Ce que la réponse doit savoir avant d'écrire le premier octet : une erreur ici sort encore en JSON. */
    public record ExportFec(Long livreId, Long exerciceId, String nomFichier, OptionsFec options) {
    }

    private final FecRepository repository;
    private final ExerciceRepository exercices;
    private final LivreCourantService livreCourant;
    private final LivreService livres;
    private final TransactionTemplate lecture;

    public FecService(FecRepository repository, ExerciceRepository exercices, LivreCourantService livreCourant, LivreService livres, PlatformTransactionManager transactions) {
        this.repository = repository;
        this.exercices = exercices;
        this.livreCourant = livreCourant;
        this.livres = livres;
        this.lecture = new TransactionTemplate(transactions);
        this.lecture.setReadOnly(true);
    }

    public ExportFec preparer(Long exerciceId, OptionsFec options) {
        if (!SEPARATEURS.contains(options.separateur()) || !ENCODAGES.contains(options.encodage()) || !FORMATS_DATE.contains(options.formatDate())) {
            throw new CommonException("Option du FEC non reconnue.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_FEC_OPTION");
        }
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exercices.lire(livreId, exerciceId).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", exerciceId));
        String identifiant = livres.etat().identifiantFiscal();
        return new ExportFec(livreId, exerciceId, "FEC_" + (identifiant == null ? "SANS_IF" : identifiant) + "_" + exercice.code() + ".txt", options);
    }

    public void ecrire(ExportFec export, OutputStream sortie) {
        OptionsFec options = export.options();
        DateTimeFormatter dates = DateTimeFormatter.ofPattern(options.formatDate());
        String separateur = options.separateur();
        try {
            BufferedWriter texte = new BufferedWriter(new OutputStreamWriter(sortie, Charset.forName(options.encodage())));
            texte.write(String.join(separateur, CHAMPS));
            texte.write('\n');
            lecture.executeWithoutResult(transaction -> repository.parcourir(export.livreId(), export.exerciceId(), rs -> {
                try {
                    String[] champs = {rs.getString(1), rs.getString(2), rs.getString(3), date(rs.getDate(4), dates), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                            rs.getString(9), date(rs.getDate(10), dates), rs.getString(11), montant(rs.getBigDecimal(12)), montant(rs.getBigDecimal(13)), rs.getString(14),
                            date(rs.getDate(15), dates), date(rs.getDate(16), dates), montant(rs.getBigDecimal(17)), rs.getString(18)};
                    for (int i = 0; i < champs.length; i++) {
                        if (i > 0) {
                            texte.write(separateur);
                        }
                        texte.write(propre(champs[i], separateur));
                    }
                    texte.write('\n');
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
            texte.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String date(Date valeur, DateTimeFormatter format) {
        return valeur == null ? "" : format.format(valeur.toLocalDate());
    }

    private static String montant(BigDecimal valeur) {
        return valeur == null ? "" : valeur.toPlainString();
    }

    /** Un libellé ne casse pas une colonne : le séparateur et les retours à la ligne y deviennent des espaces. */
    private static String propre(String valeur, String separateur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace(separateur, " ").replace('\n', ' ').replace('\r', ' ');
    }
}

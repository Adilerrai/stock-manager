package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceAgeeDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceAuxiliaireDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.GrandLivreCompteDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneBalanceAgeeDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneBalanceAuxiliaireDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneBalanceDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneGrandLivreDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.PageJournalDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.SyntheseResultatDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.TotalClasseDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.TotauxBalanceDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.TotauxDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.TranchesAgeesDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceCompteDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceTiersDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.ConsultationRepository;
import com.gestion.repository.compta.ConsultationRepository.Cumuls;
import com.gestion.repository.compta.ConsultationRepository.FiltreGrandLivre;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.MesuresRepository;
import com.gestion.repository.compta.MesuresRepository.Mesure;
import com.gestion.repository.compta.MesuresRepository.MesureTiers;
import com.gestion.repository.compta.TiersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ConsultationService {

    private static final String COMPTE_IMPOTS = "67";

    private final ExerciceRepository exercices;
    private final JournalRepository journal;
    private final ConsultationRepository consultation;
    private final MesuresRepository mesures;
    private final CompteRepository comptes;
    private final TiersRepository tiers;
    private final LivreCourantService livreCourant;

    public ConsultationService(ExerciceRepository exercices, JournalRepository journal, ConsultationRepository consultation, MesuresRepository mesures,
                               CompteRepository comptes, TiersRepository tiers, LivreCourantService livreCourant) {
        this.exercices = exercices;
        this.journal = journal;
        this.consultation = consultation;
        this.mesures = mesures;
        this.comptes = comptes;
        this.tiers = tiers;
        this.livreCourant = livreCourant;
    }

    @Transactional(readOnly = true)
    public List<com.gestion.persistent.dto.compta.ConsultationDtos.CentralisateurLigneDTO> centralisateur(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        exigerExercice(livreId, exerciceId);
        return journal.centralisateur(livreId, exerciceId);
    }

    @Transactional(readOnly = true)
    public PageJournalDTO journal(Long exerciceId, Long journalId, Long periodeId, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        exigerExercice(livreId, exerciceId);
        int limite = Curseur.taille(taille);
        String[] cle = cle(curseur);
        List<EcritureDTO> page = journal.page(livreId, exerciceId, journalId, periodeId, cle != null ? Long.valueOf(cle[0]) : null,
                cle != null ? Long.valueOf(cle[1]) : null, limite + 1);
        boolean suite = page.size() > limite;
        List<EcritureDTO> contenu = suite ? page.subList(0, limite) : page;
        BigDecimal debit = contenu.stream().map(EcritureDTO::totalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = contenu.stream().map(EcritureDTO::totalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal[] totaux = journal.totaux(livreId, exerciceId, journalId, periodeId);
        String suivant = suite ? Curseur.encoder(contenu.get(limite - 1).journal().id() + "|" + contenu.get(limite - 1).numero()) : null;
        return new PageJournalDTO(contenu, new TotauxDTO(debit, credit), new TotauxDTO(totaux[0], totaux[1]), suivant);
    }

    @Transactional(readOnly = true)
    public GrandLivreCompteDTO grandLivre(Long exerciceId, String compte, Long tiersId, LocalDate du, LocalDate au, boolean masquerLettrees,
                                    String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        CompteDTO c = comptes.lire(livreId, compte).orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", compte));
        ReferenceTiersDTO refTiers = null;
        if (tiersId != null) {
            TiersDTO t = tiers.lire(livreId, tiersId).orElseThrow(() -> new ResourceNotFoundException("Tiers", "id", tiersId));
            refTiers = new ReferenceTiersDTO(t.id(), t.codeAuxiliaire(), t.nom());
        }
        LocalDate debut = du != null ? du : exercice.du();
        LocalDate fin = au != null ? au : exercice.au();
        FiltreGrandLivre filtre = new FiltreGrandLivre(exerciceId, compte, tiersId, debut, fin, masquerLettrees);

        List<Long> chaine = mesures.chaineNonClose(livreId, exerciceId);
        boolean provisoire = !chaine.isEmpty();
        Cumuls cumuls = consultation.cumuls(livreId, filtre);
        BigDecimal ouverture = provisoire ? consultation.ouvertureCalculee(livreId, chaine, compte, tiersId) : cumuls.ouverture();
        BigDecimal solde = ouverture.add(cumuls.avantPeriode()).add(cumuls.debit()).subtract(cumuls.credit());

        int limite = Curseur.taille(taille);
        String[] cle = cle(curseur);
        LocalDate apresDate = cle != null ? LocalDate.parse(cle[0]) : null;
        Long apresId = cle != null ? Long.valueOf(cle[1]) : null;
        // Les lignes d'à-nouveau passées sont dans la page : seule une ouverture calculée s'ajoute au départ.
        BigDecimal depart = (provisoire ? ouverture : BigDecimal.ZERO).add(consultation.soldeAvant(livreId, filtre, apresDate, apresId));
        List<LigneGrandLivreDTO> page = consultation.lignes(livreId, filtre, depart, apresDate, apresId, limite + 1);
        boolean suite = page.size() > limite;
        List<LigneGrandLivreDTO> contenu = suite ? page.subList(0, limite) : page;
        String suivant = suite ? Curseur.encoder(contenu.get(limite - 1).date() + "|" + contenu.get(limite - 1).ligneId()) : null;
        return new GrandLivreCompteDTO(new ReferenceCompteDTO(c.numero(), c.libelle()), refTiers, ouverture, cumuls.debit(), cumuls.credit(), solde,
                provisoire, contenu, suivant);
    }

    @Transactional(readOnly = true)
    public BalanceDTO balance(Long exerciceId, LocalDate arrete, Integer classe, boolean expurgee, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        LocalDate date = arrete != null ? arrete : exercice.au();
        List<Long> chaine = mesures.chaineNonClose(livreId, exerciceId);
        List<Mesure> toutes = mesures.parCompte(livreId, exerciceId, chaine, date, expurgee);
        Map<String, String> libelles = consultation.libellesComptes(livreId);

        Map<Integer, List<Mesure>> parClasse = new TreeMap<>();
        for (Mesure m : toutes) {
            parClasse.computeIfAbsent(m.compte().charAt(0) - '0', k -> new ArrayList<>()).add(m);
        }
        List<TotalClasseDTO> totauxClasses = new ArrayList<>();
        parClasse.forEach((numero, lignes) -> totauxClasses.add(new TotalClasseDTO(numero, totaux(lignes))));

        List<Mesure> retenues = classe != null ? parClasse.getOrDefault(classe, List.of()) : toutes;
        int limite = Curseur.taille(taille);
        String apres = Curseur.decoder(curseur);
        List<LigneBalanceDTO> page = retenues.stream().filter(m -> apres == null || m.compte().compareTo(apres) > 0).limit(limite + 1L)
                .map(m -> new LigneBalanceDTO(m.compte(), libelles.getOrDefault(m.compte(), m.compte()), m.od(), m.oc(), m.md(), m.mc(), m.sd(), m.sc())).toList();
        boolean suite = page.size() > limite;
        List<LigneBalanceDTO> contenu = suite ? page.subList(0, limite) : page;
        return new BalanceDTO(date, !chaine.isEmpty(), expurgee, totaux(toutes), totauxClasses, resultat(toutes), contenu,
                suite ? Curseur.encoder(contenu.get(limite - 1).compte()) : null);
    }

    @Transactional(readOnly = true)
    public BalanceAuxiliaireDTO balanceAuxiliaire(Long exerciceId, String compte, String recherche, LocalDate arrete, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        comptes.lire(livreId, compte).orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", compte));
        LocalDate date = arrete != null ? arrete : exercice.au();
        List<Long> chaine = mesures.chaineNonClose(livreId, exerciceId);
        String texte = recherche != null ? recherche.trim().toLowerCase(Locale.ROOT) : "";
        List<MesureTiers> toutes = mesures.parTiersDuCompte(livreId, exerciceId, chaine, compte, date).stream()
                .filter(m -> texte.isEmpty() || m.codeAuxiliaire().toLowerCase(Locale.ROOT).contains(texte) || m.nom().toLowerCase(Locale.ROOT).contains(texte))
                .toList();
        int limite = Curseur.taille(taille);
        String apres = Curseur.decoder(curseur);
        List<LigneBalanceAuxiliaireDTO> page = toutes.stream().filter(m -> apres == null || m.codeAuxiliaire().compareTo(apres) > 0).limit(limite + 1L)
                .map(m -> new LigneBalanceAuxiliaireDTO(m.tiersId(), m.codeAuxiliaire(), m.nom(), m.od(), m.oc(), m.md(), m.mc(), m.sd(), m.sc())).toList();
        boolean suite = page.size() > limite;
        List<LigneBalanceAuxiliaireDTO> contenu = suite ? page.subList(0, limite) : page;
        TotauxBalanceDTO totaux = new TotauxBalanceDTO(
                somme(toutes, MesureTiers::od), somme(toutes, MesureTiers::oc), somme(toutes, MesureTiers::md),
                somme(toutes, MesureTiers::mc), somme(toutes, MesureTiers::sd), somme(toutes, MesureTiers::sc));
        return new BalanceAuxiliaireDTO(date, !chaine.isEmpty(), compte, totaux, contenu, suite ? Curseur.encoder(contenu.get(limite - 1).codeAuxiliaire()) : null);
    }

    @Transactional(readOnly = true)
    public BalanceAgeeDTO balanceAgee(Long exerciceId, String compte, LocalDate arrete, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        comptes.lire(livreId, compte).orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", compte));
        LocalDate date = arrete != null ? arrete : exercice.au();
        List<Long> perimetre = new ArrayList<>(mesures.chaineNonClose(livreId, exerciceId));
        perimetre.add(exerciceId);
        List<ConsultationRepository.LigneAgee> toutes = consultation.balanceAgee(livreId, perimetre, compte, date);
        int limite = Curseur.taille(taille);
        String apres = Curseur.decoder(curseur);
        List<LigneBalanceAgeeDTO> page = toutes.stream().filter(l -> apres == null || l.codeAuxiliaire().compareTo(apres) > 0).limit(limite + 1L)
                .map(l -> new LigneBalanceAgeeDTO(l.tiersId(), l.codeAuxiliaire(), l.nom(),
                        new TranchesAgeesDTO(l.nonEchu(), l.t0a30(), l.t31a60(), l.t61a90(), l.t90Plus(), l.total()))).toList();
        boolean suite = page.size() > limite;
        List<LigneBalanceAgeeDTO> contenu = suite ? page.subList(0, limite) : page;
        TranchesAgeesDTO totaux = new TranchesAgeesDTO(
                somme(toutes, ConsultationRepository.LigneAgee::nonEchu), somme(toutes, ConsultationRepository.LigneAgee::t0a30),
                somme(toutes, ConsultationRepository.LigneAgee::t31a60), somme(toutes, ConsultationRepository.LigneAgee::t61a90),
                somme(toutes, ConsultationRepository.LigneAgee::t90Plus), somme(toutes, ConsultationRepository.LigneAgee::total));
        return new BalanceAgeeDTO(date, compte, totaux, contenu, suite ? Curseur.encoder(contenu.get(limite - 1).codeAuxiliaire()) : null);
    }

    private static TotauxBalanceDTO totaux(List<Mesure> lignes) {
        return new TotauxBalanceDTO(somme(lignes, Mesure::od), somme(lignes, Mesure::oc), somme(lignes, Mesure::md), somme(lignes, Mesure::mc),
                somme(lignes, Mesure::sd), somme(lignes, Mesure::sc));
    }

    private static SyntheseResultatDTO resultat(List<Mesure> toutes) {
        BigDecimal produits = BigDecimal.ZERO;
        BigDecimal charges = BigDecimal.ZERO;
        BigDecimal impots = BigDecimal.ZERO;
        for (Mesure m : toutes) {
            BigDecimal solde = m.sd().subtract(m.sc());
            if (m.compte().startsWith("7")) {
                produits = produits.subtract(solde);
            } else if (m.compte().startsWith(COMPTE_IMPOTS)) {
                impots = impots.add(solde);
            } else if (m.compte().startsWith("6")) {
                charges = charges.add(solde);
            }
        }
        BigDecimal avantImpots = produits.subtract(charges);
        return new SyntheseResultatDTO(produits, charges, avantImpots, impots, avantImpots.subtract(impots));
    }

    private static <T> BigDecimal somme(List<T> lignes, java.util.function.Function<T, BigDecimal> champ) {
        return lignes.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String[] cle(String curseur) {
        String decode = Curseur.decoder(curseur);
        if (decode == null) {
            return null;
        }
        String[] parties = decode.split("\\|");
        if (parties.length != 2) {
            throw new CommonException("Curseur de page invalide", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_CURSEUR_INVALIDE");
        }
        return parties;
    }

    private ExerciceDTO exigerExercice(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }
}

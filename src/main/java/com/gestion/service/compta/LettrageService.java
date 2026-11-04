package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.LettrageRequest;
import com.gestion.persistent.dto.compta.ConsultationDtos.LigneOuverteDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.PageLignesLettrageDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.PropositionLettrageDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.CompteRepository;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.LettrageRepository;
import com.gestion.repository.compta.LettrageRepository.GroupeCree;
import com.gestion.repository.compta.LettrageRepository.PaireMontant;
import com.gestion.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("comptaLettrageService")
public class LettrageService {

    private final LettrageRepository lettrages;
    private final CompteRepository comptes;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public LettrageService(LettrageRepository lettrages, CompteRepository comptes, LivreCourantService livreCourant, AuditService audit) {
        this.lettrages = lettrages;
        this.comptes = comptes;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageLignesLettrageDTO lignes(String compte, Long tiersId, String etat, String curseur, Integer taille) {
        Long livreId = livreCourant.exiger().livreId();
        exigerLettrable(livreId, compte);
        int limite = Curseur.taille(taille);
        String cle = Curseur.decoder(curseur);
        String apresDate = cle != null ? cle.split("\\|")[0] : null;
        Long apresId = cle != null ? Long.valueOf(cle.split("\\|")[1]) : null;
        List<LigneOuverteDTO> page = lettrages.lignes(livreId, compte, tiersId, etat != null ? etat : "OUVERTES", apresDate, apresId, limite + 1);
        boolean suite = page.size() > limite;
        List<LigneOuverteDTO> contenu = suite ? page.subList(0, limite) : page;
        String suivant = suite ? Curseur.encoder(contenu.get(limite - 1).date() + "|" + contenu.get(limite - 1).ligneId()) : null;
        return new PageLignesLettrageDTO(lettrages.soldeOuvert(livreId, compte, tiersId), contenu, suivant);
    }

    @Transactional
    public LettrageDTO lettrer(LettrageRequest d) {
        Long livreId = livreCourant.exiger().livreId();
        exigerLettrable(livreId, d.compte());
        if (!Set.of("TOTAL", "PARTIEL").contains(d.statut())) {
            throw new CommonException("Statut de lettrage inconnu : " + d.statut(), HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LETTRAGE_INVALIDE");
        }
        Set<Long> lignes = new HashSet<>(d.lignes());
        if (lignes.size() < 2) {
            throw new CommonException("Un lettrage rapproche au moins deux lignes", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LETTRAGE_INVALIDE");
        }
        // Une ligne d'un autre tiers ou d'un autre compte n'est pas relue : le lettrage entre deux clients ne passe pas ce contrôle.
        List<BigDecimal[]> montants = lettrages.montantsLibres(livreId, d.compte(), d.tiersId(), lignes);
        if (montants.size() != lignes.size()) {
            throw new CommonException("Une ligne appartient à un autre compte ou un autre tiers, ou elle est déjà lettrée",
                    HttpStatus.CONFLICT, "COMPTA_LETTRAGE_LIGNE_REFUSEE");
        }
        BigDecimal debit = montants.stream().map(m -> m[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = montants.stream().map(m -> m[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ecart = debit.subtract(credit);
        if ("TOTAL".equals(d.statut()) && ecart.signum() != 0) {
            throw new CommonException("Lettrage total impossible : écart de " + ecart.abs().toPlainString(), HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LETTRAGE_ECART");
        }
        GroupeCree groupe = lettrages.creer(livreId, d.compte(), d.tiersId(), d.statut(), "MANUEL", d.dateLettrage(), livreCourant.auteur());
        lettrages.attacher(livreId, groupe.id(), d.compte(), d.tiersId(), lignes);
        audit.logAction(ActionAudit.LETTRAGE, "compta_lettrages", groupe.id(), d.compte() + " " + groupe.code());
        return new LettrageDTO(groupe.id(), groupe.code(), d.statut(), debit, credit, ecart);
    }

    @Transactional
    public void delettrer(Long id) {
        Long livreId = livreCourant.exiger().livreId();
        if (!lettrages.existe(livreId, id)) {
            throw new ResourceNotFoundException("Lettrage", "id", id);
        }
        if (lettrages.uneLigneDansUnExerciceClos(livreId, id)) {
            throw new CommonException("Ce lettrage touche un exercice clos : il ne se défait plus", HttpStatus.CONFLICT, "COMPTA_LETTRAGE_EXERCICE_CLOS");
        }
        lettrages.supprimer(livreId, id);
        audit.logAction(ActionAudit.ANNULATION_LETTRAGE, "compta_lettrages", id, "Délettrage");
    }

    /** Une proposition ne franchit jamais un tiers et n'est appliquée que sur confirmation. */
    @Transactional(readOnly = true)
    public List<PropositionLettrageDTO> propositions(String compte, Long tiersId) {
        Long livreId = livreCourant.exiger().livreId();
        exigerLettrable(livreId, compte);
        List<PropositionLettrageDTO> propositions = new ArrayList<>();
        Set<Long> prises = new HashSet<>();
        for (List<Long> groupe : groupesAffectes(livreId, compte, tiersId)) {
            prises.addAll(groupe);
            propositions.add(new PropositionLettrageDTO("AFFECTATION", groupe, "Règlement affecté à sa facture dans le commercial", BigDecimal.ZERO));
        }
        for (PaireMontant paire : lettrages.pairesDeMemeMontant(livreId, compte, tiersId)) {
            if (prises.contains(paire.debitId()) || prises.contains(paire.creditId())) {
                continue;
            }
            prises.add(paire.debitId());
            prises.add(paire.creditId());
            propositions.add(new PropositionLettrageDTO("MONTANT", List.of(paire.debitId(), paire.creditId()),
                    "Même montant et même tiers : " + MontantTexte.fr(paire.montant()), BigDecimal.ZERO));
        }
        return propositions;
    }

    /**
     * Factures et paiements reliés par les affectations du commercial, de proche en proche : un paiement peut couvrir deux factures, une facture deux paiements.
     * Seul un ensemble exactement équilibré est proposé.
     */
    private List<List<Long>> groupesAffectes(Long livreId, String compte, Long tiersId) {
        Map<Long, Long> parent = new HashMap<>();
        Map<Long, BigDecimal> debits = new HashMap<>();
        Map<Long, BigDecimal> credits = new HashMap<>();
        for (LettrageRepository.LienAffectation l : lettrages.liensAffectations(livreId, compte, tiersId)) {
            debits.put(l.ligneFacture(), l.debit());
            credits.put(l.lignePaiement(), l.credit());
            parent.putIfAbsent(l.ligneFacture(), l.ligneFacture());
            parent.putIfAbsent(l.lignePaiement(), l.lignePaiement());
            parent.put(racine(parent, l.ligneFacture()), racine(parent, l.lignePaiement()));
        }
        Map<Long, List<Long>> composants = new TreeMap<>();
        for (Long ligne : parent.keySet()) {
            composants.computeIfAbsent(racine(parent, ligne), k -> new ArrayList<>()).add(ligne);
        }
        List<List<Long>> equilibres = new ArrayList<>();
        for (List<Long> lignes : composants.values()) {
            BigDecimal d = lignes.stream().map(x -> debits.getOrDefault(x, BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal c = lignes.stream().map(x -> credits.getOrDefault(x, BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (d.compareTo(c) == 0) {
                Collections.sort(lignes);
                equilibres.add(lignes);
            }
        }
        return equilibres;
    }

    private static Long racine(Map<Long, Long> parent, Long ligne) {
        Long r = ligne;
        while (!parent.get(r).equals(r)) {
            r = parent.get(r);
        }
        return r;
    }

    /**
     * Dans la transaction de la contre-passation : une ligne d'origine déjà lettrée voit son groupe défait, avec une trace,
     * puis l'origine et son miroir sont lettrés ensemble.
     */
    public void lettrerMiroir(Long livreId, List<LigneJournal> origine, List<LigneJournal> miroir, LocalDate date) {
        String auteur = livreCourant.auteur();
        for (int i = 0; i < origine.size(); i++) {
            LigneJournal ligne = origine.get(i);
            if (ligne.tiersId() == null || !comptes.lire(livreId, ligne.compte()).map(CompteDTO::lettrable).orElse(false)) {
                continue;
            }
            lettrages.groupeDeLaLigne(livreId, ligne.id()).ifPresent(groupe -> {
                lettrages.supprimer(livreId, groupe);
                audit.logAction(ActionAudit.ANNULATION_LETTRAGE, "compta_lettrages", groupe, "Délettré par contre-passation");
            });
            GroupeCree groupe = lettrages.creer(livreId, ligne.compte(), ligne.tiersId(), "TOTAL", "CONTREPASSATION", date, auteur);
            lettrages.attacher(livreId, groupe.id(), ligne.compte(), ligne.tiersId(), List.of(ligne.id(), miroir.get(i).id()));
        }
    }

    /**
     * Dans la transaction du rejet : le groupe qui contient la ligne du paiement se défait, puis cette ligne se lettre avec celle du rejet.
     * La facture redevient due. Sans montants égaux, un rejet partiel reste ouvert : rien n'est lettré à moitié en silence.
     */
    public void lettrerRejet(Long livreId, LigneJournal lignePaiement, LigneJournal ligneRejet, LocalDate date) {
        if (lignePaiement.credit().compareTo(ligneRejet.debit()) != 0 || !lignePaiement.compte().equals(ligneRejet.compte()) || !java.util.Objects.equals(lignePaiement.tiersId(), ligneRejet.tiersId())) {
            return;
        }
        lettrages.groupeDeLaLigne(livreId, lignePaiement.id()).ifPresent(groupe -> {
            lettrages.supprimer(livreId, groupe);
            audit.logAction(ActionAudit.ANNULATION_LETTRAGE, "compta_lettrages", groupe, "Délettré par le rejet d'un effet");
        });
        GroupeCree groupe = lettrages.creer(livreId, lignePaiement.compte(), lignePaiement.tiersId(), "TOTAL", "REJET", date, livreCourant.auteur());
        lettrages.attacher(livreId, groupe.id(), lignePaiement.compte(), lignePaiement.tiersId(), List.of(lignePaiement.id(), ligneRejet.id()));
    }

    private void exigerLettrable(Long livreId, String compte) {
        CompteDTO c = comptes.lire(livreId, compte).orElseThrow(() -> new ResourceNotFoundException("Compte", "numero", compte));
        if (!c.lettrable()) {
            throw new CommonException("Le compte " + compte + " ne se lettre pas", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_COMPTE_NON_LETTRABLE");
        }
    }
}

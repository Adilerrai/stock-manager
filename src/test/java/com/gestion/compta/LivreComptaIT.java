package com.gestion.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.LivreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ComptePatchRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PageCompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import com.gestion.persistent.enums.compta.EtatLivre;
import com.gestion.persistent.enums.compta.OrigineLivre;
import com.gestion.service.compta.AbonnementComptaService;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.JournalService;
import com.gestion.service.compta.LivreService;
import com.gestion.service.compta.PeriodeService;
import com.gestion.service.compta.PlanComptableService;
import com.gestion.service.compta.TiersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LivreComptaIT extends AbstractComptaIT {

    @Autowired private LivreService livres;
    @Autowired private AbonnementComptaService abonnements;
    @Autowired private ExerciceService exercices;
    @Autowired private PeriodeService periodes;
    @Autowired private PlanComptableService plan;
    @Autowired private TiersService tiers;
    @Autowired private JournalService journaux;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    @Test
    void sansAbonnementLeLivreEstNonSouscritEtRienNestEcrit() {
        SocieteDeTest societe = SocieteDeTest.creer();
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());

        LivreDTO livre = livres.etat();

        assertEquals(EtatLivre.NON_SOUSCRIT, livre.etat());
        assertNull(livre.livreId());
        assertEquals(List.of(), livre.periodesOuvertes());
        assertEquals(0, compter("compta_abonnements WHERE point_de_vente_id = " + societe.pointDeVenteId()));
        assertRefus("COMPTA_MODULE_INACTIF", () -> exercices.lister());
        assertRefus("COMPTA_MODULE_INACTIF", () -> livres.activer(activation("2026-01-01", "2026-01-01", "2026-12-31")));
    }

    @Test
    void lActivationCopieLePlanEtOuvreDeuxPeriodesSansToucherAuCommercial() {
        SocieteDeTest societe = SocieteDeTest.creer();
        abonner(societe);
        assertEquals(EtatLivre.A_ACTIVER, livres.etat().etat());
        long societes = compter("societes");
        long pointsDeVente = compter("point_de_vente");

        LivreDTO livre = livres.activer(activation("2026-01-01", "2026-01-01", "2026-12-31"));

        assertEquals(EtatLivre.ACTIF, livre.etat());
        assertEquals(OrigineLivre.POINT_DE_VENTE, livre.origine());
        assertEquals(societes, compter("societes"));
        assertEquals(pointsDeVente, compter("point_de_vente"));
        assertEquals(723, compter("compta_comptes WHERE livre_id = " + livre.livreId()));
        assertEquals(List.of("AC", "AN", "BQ", "CA", "CL", "OD", "VT"),
                journaux.lister().stream().map(JournalDTO::code).sorted().toList());
        assertNotNull(livre.exerciceCourantId());
        assertEquals(List.of(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01")),
                livre.periodesOuvertes().stream().map(p -> p.du()).toList());
        Map<String, Long> parStatut = exercices.periodes(livre.exerciceCourantId()).stream()
                .collect(Collectors.groupingBy(p -> p.typePeriode() + "/" + p.statut(), Collectors.counting()));
        assertEquals(Map.of("OUVERTURE/OUVERTE", 1L, "COURANTE/OUVERTE", 2L, "COURANTE/A_VENIR", 10L, "CLOTURE/A_VENIR", 1L), parStatut);
        assertRefus("COMPTA_LIVRE_DEJA_ACTIF", () -> livres.activer(activation("2026-01-01", "2026-01-01", "2026-12-31")));
    }

    @Test
    void lesPeriodesAnterieuresAuDemarrageNaissentCloturees() {
        SocieteDeTest societe = SocieteDeTest.creer();
        abonner(societe);

        LivreDTO livre = livres.activer(activation("2026-03-15", "2026-01-01", "2026-12-31"));

        List<PeriodeDTO> courantes = exercices.periodes(livre.exerciceCourantId()).stream()
                .filter(p -> "COURANTE".equals(p.typePeriode())).toList();
        assertEquals(List.of("CLOTUREE", "CLOTUREE", "OUVERTE", "OUVERTE", "A_VENIR"),
                courantes.stream().limit(5).map(PeriodeDTO::statut).toList());
    }

    @Test
    void unDemarrageHorsExerciceEtUnExerciceTropLongSontRefuses() {
        SocieteDeTest societe = SocieteDeTest.creer();
        abonner(societe);

        assertRefus("COMPTA_DEMARRAGE_HORS_EXERCICE", () -> livres.activer(activation("2025-12-31", "2026-01-01", "2026-12-31")));
        assertRefus("COMPTA_EXERCICE_DUREE", () -> livres.activer(activation("2026-01-01", "2026-01-01", "2027-01-01")));
        assertEquals(EtatLivre.A_ACTIVER, livres.etat().etat());
    }

    @Test
    void lesPeriodesSeFermentDansLOrdreEtLaSuivanteSOuvre() {
        LivreDTO livre = activer();
        Map<Integer, PeriodeDTO> parNumero = exercices.periodes(livre.exerciceCourantId()).stream()
                .collect(Collectors.toMap(PeriodeDTO::numero, p -> p));

        assertRefus("COMPTA_PERIODE_NON_CLOTURABLE", () -> periodes.cloturer(parNumero.get(2).id()));
        var cloture = periodes.cloturer(parNumero.get(1).id());

        assertEquals("CLOTUREE", cloture.statut());
        assertNotNull(cloture.periodeOuverte());
        assertEquals(3, cloture.periodeOuverte().numero());
        assertRefus("COMPTA_PERIODE_NON_CLOTURABLE", () -> periodes.cloturer(parNumero.get(1).id()));
        assertEquals(List.of(2, 3), livres.etat().periodesOuvertes().stream().map(p -> p.numero()).toList());
    }

    @Test
    void lExerciceSuivantDoitEtreContigu() {
        LivreDTO livre = activer();

        assertRefus("COMPTA_EXERCICE_NON_CONTIGU", () -> exercices.creerSuivant(livre.exerciceCourantId(),
                new ExerciceDatesRequest("2027", LocalDate.parse("2027-02-01"), LocalDate.parse("2027-12-31"))));
        ExerciceDTO suivant = exercices.creerSuivant(livre.exerciceCourantId(),
                new ExerciceDatesRequest("2027", LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31")));

        assertEquals(livre.exerciceCourantId(), suivant.precedentId());
        assertTrue(suivant.ouvertureProvisoire());
        assertRefus("COMPTA_EXERCICE_SUIVANT_EXISTANT", () -> exercices.creerSuivant(livre.exerciceCourantId(),
                new ExerciceDatesRequest("2027B", LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31"))));
    }

    @Test
    void lePlanSeCompleteSansChangerSesComptesOfficiels() {
        activer();

        CompteDTO sousCompte = plan.creer(new CompteCreationRequest("514101", "Banque, agence Maarif"));

        assertEquals("SOCIETE", sousCompte.origine());
        assertTrue(sousCompte.imputable());
        assertFalse(plan.lire("5141").imputable(), "un compte divisé ne s'impute plus");
        assertRefus("COMPTA_COMPTE_INVALIDE", () -> plan.creer(new CompteCreationRequest("51410", "Trop court")));
        assertRefus("COMPTA_COMPTE_EXISTANT", () -> plan.creer(new CompteCreationRequest("514101", "Doublon")));
        assertRefus("COMPTA_COMPTE_OFFICIEL", () -> plan.modifier("5141", new ComptePatchRequest("Autre nom", null, null)));
        assertRefus("COMPTA_COMPTE_OFFICIEL", () -> plan.supprimer("5141"));
        assertTrue(plan.creer(new CompteCreationRequest("342101", "Clients export")).imputable());
        assertTrue(plan.lire("3421").imputable(), "3421 reste imputable malgré ses divisions");
        plan.supprimer("514101");
        assertThrows(ResourceNotFoundException.class, () -> plan.lire("514101"));
    }

    @Test
    void laListeDesComptesSeLitParPagesSansTrouNiDoublon() {
        activer();

        PageCompteDTO premiere = plan.lister(null, 3, null, null, 10);
        PageCompteDTO seconde = plan.lister(null, 3, null, premiere.curseurSuivant(), 10);

        assertEquals(10, premiere.contenu().size());
        assertTrue(premiere.contenu().get(9).numero().compareTo(seconde.contenu().get(0).numero()) < 0);
        assertTrue(plan.lister("3421", null, null, null, null).contenu().stream().anyMatch(c -> c.numero().equals("3421")));
        assertTrue(plan.lister(null, null, true, null, 500).contenu().stream().allMatch(CompteDTO::imputable));
    }

    @Test
    void unObjetDUnAutreLivreRepond404() {
        LivreDTO a = activer();
        TiersDTO tiersA = tiers.creer(new TiersCreationRequest("AUTRE", "Associé A", null, null, null, null, null, null));
        assertEquals("X", tiersA.codeAuxiliaire().substring(0, 1));

        LivreDTO b = activer();

        assertTrue(!a.livreId().equals(b.livreId()));
        assertThrows(ResourceNotFoundException.class, () -> tiers.lire(tiersA.id()));
        assertThrows(ResourceNotFoundException.class, () -> exercices.periodes(a.exerciceCourantId()));
        assertEquals(List.of(), tiers.lister(null, null, null, null).contenu());
    }

    @Test
    void unDossierHomonymeDUnPointDeVenteOuvreSonLivreJamaisCeluiDuPointDeVente() {
        LivreDTO duPointDeVente = activer();
        long pointDeVenteHomonyme = jdbc.queryForObject(
                "SELECT point_de_vente_id FROM compta_livre_points_de_vente WHERE livre_id = ?", Long.class, duPointDeVente.livreId());
        SocieteDeTest cabinet = SocieteDeTest.creer();
        abonnements.modifier(cabinet.pointDeVenteId(), true);
        // Les deux suites d'identifiants vivent à part : une société de ce numéro peut déjà exister.
        jdbc.update("""
                INSERT INTO societes (id, tenant_id, code, raison_sociale) VALUES (?, ?, ?, 'Dossier client')
                ON CONFLICT (id) DO UPDATE SET tenant_id = EXCLUDED.tenant_id
                """, pointDeVenteHomonyme, cabinet.pointDeVenteId(), "DOSSIER-" + pointDeVenteHomonyme);

        ContexteDeTest.surDossier(cabinet.pointDeVenteId(), pointDeVenteHomonyme);

        assertEquals(EtatLivre.A_ACTIVER, livres.etat().etat(), "le livre du point de vente homonyme n'est jamais lu");
        LivreDTO duDossier = livres.activer(activation("2026-01-01", "2026-01-01", "2026-12-31"));
        assertEquals(OrigineLivre.DOSSIER, duDossier.origine());
        assertEquals(List.of(), duDossier.pointsDeVente());
        assertTrue(!duDossier.livreId().equals(duPointDeVente.livreId()));
    }

    @Test
    void unJournalSAjouteSeulementPourLaBanqueOuLaCaisse() {
        activer();

        assertRefus("COMPTA_JOURNAL_TYPE_REFUSE", () -> journaux.creer(new JournalCreationRequest("OD2", "OD bis", "OD", "5141")));
        assertRefus("COMPTA_JOURNAL_EXISTANT", () -> journaux.creer(new JournalCreationRequest("BQ", "Banque", "BANQUE", "5141")));
        JournalDTO banque = journaux.creer(new JournalCreationRequest("BQ2", "Banque 2", "BANQUE", "5141"));
        assertEquals("5141", banque.compteTresorerie());
    }

    private LivreDTO activer() {
        SocieteDeTest societe = SocieteDeTest.creer();
        abonner(societe);
        return livres.activer(activation("2026-01-01", "2026-01-01", "2026-12-31"));
    }

    private void abonner(SocieteDeTest societe) {
        ContexteDeTest.surPointDeVente(societe.pointDeVenteId());
        abonnements.modifier(societe.pointDeVenteId(), true);
    }

    private static ActivationLivreRequest activation(String demarrage, String du, String au) {
        return new ActivationLivreRequest(LocalDate.parse(demarrage), new ExerciceDatesRequest("E" + du, LocalDate.parse(du), LocalDate.parse(au)),
                new IdentiteLivreDTO("Atlas Négoce SARL", "SARL", "99900001", "999000000000001", null, null, null, "Casablanca",
                        "Négoce", null, null, "GENERAL"));
    }

    private long compter(String deOu) {
        return jdbc.queryForObject("SELECT count(*) FROM " + deOu, Long.class);
    }

    private static void assertRefus(String code, Runnable action) {
        CommonException refus = assertThrows(CommonException.class, action::run);
        assertEquals(code, refus.getErrorCode(), refus.getMessage());
        assertTrue(refus.getStatus() != HttpStatus.UNAUTHORIZED && refus.getStatus() != HttpStatus.FORBIDDEN,
                "un refus métier ne doit pas déconnecter l'utilisateur");
    }
}

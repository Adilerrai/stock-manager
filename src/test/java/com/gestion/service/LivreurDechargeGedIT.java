package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.User;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.persistent.enums.TypeActionLivreur;
import com.gestion.persistent.enums.TypeDocumentLivreur;
import com.gestion.persistent.model.PreuveDepotLivreur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La photo du livreur doit se retrouver dans les pièces jointes du BL et de sa facture, et nulle part ailleurs. */
@SpringBootTest(properties = "storage.ged.path=target/ged-test")
class LivreurDechargeGedIT extends AbstractComptaIT {

    private static final byte[] PHOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};

    @Autowired private LivreurService livreurs;
    @Autowired private GedService ged;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    @Test
    void laPhotoDuBlApparaitDansLesPiecesJointesDuBlEtDeSaFacture() throws Exception {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);
        long client = client(tenant);
        long facture = facture(tenant, client);
        long bl = bl(tenant, client, facture);

        PreuveDepotLivreur preuve = declarer(TypeDocumentLivreur.BON_LIVRAISON, bl, TypeActionLivreur.LIVRAISON_EFFECTUEE);

        List<DocumentComptableDTO> duBl = ged.getDocumentsParBonLivraisonClient(bl);
        List<DocumentComptableDTO> deLaFacture = ged.getDocumentsParFactureVente(facture);
        assertEquals(1, duBl.size());
        assertEquals(duBl.get(0).getId(), deLaFacture.get(0).getId(), "une seule pièce pour le BL et sa facture");
        assertEquals("DECHARGE_LIVREUR", duBl.get(0).getTypePiece());
        assertEquals(duBl.get(0).getUrlVisualisation(), preuve.getPhotoUrl());
        assertArrayEquals(PHOTO, ged.chargerFichier(duBl.get(0).getId()).getInputStream().readAllBytes());
    }

    @Test
    void laPhotoDUneFactureVaSurLaFactureEtChacunDeSesBl() throws Exception {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);
        long client = client(tenant);
        long facture = facture(tenant, client);
        long bl1 = bl(tenant, client, facture);
        long bl2 = bl(tenant, client, facture);

        declarer(TypeDocumentLivreur.FACTURE, facture, TypeActionLivreur.FACTURE_DEPOSEE);

        assertEquals(1, ged.getDocumentsParFactureVente(facture).size());
        assertEquals(1, ged.getDocumentsParBonLivraisonClient(bl1).size());
        assertEquals(1, ged.getDocumentsParBonLivraisonClient(bl2).size());
    }

    @Test
    void uneFactureDUneAutreSocieteEstRefuseeSansRienModifier() throws Exception {
        long autreTenant = SocieteDeTest.creer().pointDeVenteId();
        long factureAutre = facture(autreTenant, client(autreTenant));
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);

        CommonException erreur = assertThrows(CommonException.class,
                () -> declarer(TypeDocumentLivreur.FACTURE, factureAutre, TypeActionLivreur.FACTURE_DEPOSEE));

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
        assertEquals("VALIDEE", jdbc.queryForObject("SELECT statut FROM factures WHERE id = ?", String.class, factureAutre));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM documents_comptables WHERE facture_vente_id = ?", Integer.class, factureAutre));
    }

    @Test
    void unBlDUneAutreSocieteEstRefuse() {
        long autreTenant = SocieteDeTest.creer().pointDeVenteId();
        long blAutre = bl(autreTenant, client(autreTenant), null);
        connecter(SocieteDeTest.creer().pointDeVenteId());

        CommonException erreur = assertThrows(CommonException.class,
                () -> declarer(TypeDocumentLivreur.BON_LIVRAISON, blAutre, TypeActionLivreur.LIVRAISON_EFFECTUEE));

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
        assertTrue(ged.getDocumentsParBonLivraisonClient(blAutre).isEmpty());
    }

    @Test
    void uneAutreSocieteNePeutNiLireNiEffacerLaPhoto() throws Exception {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);
        long bl = bl(tenant, client(tenant), null);
        declarer(TypeDocumentLivreur.BON_LIVRAISON, bl, TypeActionLivreur.LIVRAISON_EFFECTUEE);
        Long piece = ged.getDocumentsParBonLivraisonClient(bl).get(0).getId();

        connecter(SocieteDeTest.creer().pointDeVenteId());
        assertThrows(IllegalArgumentException.class, () -> ged.chargerFichier(piece));
        assertThrows(IllegalArgumentException.class, () -> ged.getDocument(piece));
        assertThrows(IllegalArgumentException.class, () -> ged.supprimerDocument(piece));

        connecter(tenant);
        assertArrayEquals(PHOTO, ged.chargerFichier(piece).getInputStream().readAllBytes());
    }

    private PreuveDepotLivreur declarer(TypeDocumentLivreur type, long id, TypeActionLivreur action) throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "decharge.jpg", "image/jpeg", PHOTO);
        return livreurs.declarerAction(photo, type, id, "N-" + id, "Client test", action, "Remis au magasinier", "Livreur test", null, null, null);
    }

    private static void connecter(long tenant) {
        ContexteDeTest.surPointDeVente(tenant);
        User acteur = new User();
        acteur.setEmail("livreur.test@exemple.invalid");
        acteur.setTenantId(tenant);
        acteur.setPointDeVenteId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acteur, null, List.of()));
    }

    private long client(long tenant) {
        return jdbc.queryForObject("INSERT INTO clients (nom, nom_complet, pays, point_de_vente_id) VALUES ('Client', 'Client test', 'MA', ?) RETURNING id", Long.class, tenant);
    }

    private long facture(long tenant, long client) {
        return jdbc.queryForObject("""
                INSERT INTO factures (numero_facture, date_facture, client_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, annulee, point_de_vente_id)
                VALUES (?, CURRENT_DATE, ?, 1, 100, 20, 120, 0, 120, 'VALIDEE', FALSE, ?) RETURNING id
                """, Long.class, "FV-" + UUID.randomUUID().toString().substring(0, 8), client, tenant);
    }

    private long bl(long tenant, long client, Long facture) {
        return jdbc.queryForObject("""
                INSERT INTO bons_livraison_client (numero_bl, date_bl, client_id, point_de_vente_id, statut, facture_id)
                VALUES (?, CURRENT_TIMESTAMP, ?, ?, 'EN_ATTENTE', ?) RETURNING id
                """, Long.class, "BL-" + UUID.randomUUID().toString().substring(0, 8), client, tenant, facture);
    }
}

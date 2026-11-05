package com.gestion.service.push;

import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.compta.support.AbstractComptaIT;
import com.gestion.compta.support.ContexteDeTest;
import com.gestion.compta.support.SocieteDeTest;
import com.gestion.persistent.model.PushSubscription;
import com.gestion.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sur la vraie chaîne Liquibase : qui reçoit, et à quel moment de la transaction. */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class NotificationBlExpedieIT extends AbstractComptaIT {

    private static final String P256DH = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlUls0VJXg7A8u-Ts1XbjhazAkj7I99e8QcYP7DkM";
    private static final String AUTH = "tBHItJI5svbpez7KI4CCXg";

    @Autowired private PushNotificationService notifications;
    @Autowired private PushSubscriptionRepository abonnements;
    @Autowired private ExpeditionBlNotifier notifier;
    @Autowired private UserRepository utilisateurs;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    @Autowired private org.springframework.test.web.servlet.MockMvc http;
    @Autowired private com.gestion.service.LivreurService livreurs;

    @MockBean private WebPushSender sender;

    @AfterEach
    void effacer() {
        ContexteDeTest.effacer();
    }

    @Test
    void laMigrationCreeLHabilitationEtLAccordeALAdmin() {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM habilitations WHERE nom = 'NOTIF_BL_EXPEDIE'", Integer.class));
    }

    @Test
    void seulsLAdminEtLeRoleHabiliteDuTenantRecoivent() {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        long autreTenant = SocieteDeTest.creer().pointDeVenteId();
        String roleAchat = creerRole("ROLE_ACHAT_" + UUID.randomUUID().toString().substring(0, 8), true);
        String roleSansDroit = creerRole("ROLE_SANS_" + UUID.randomUUID().toString().substring(0, 8), false);

        String admin = abonner(creerUtilisateur("ROLE_ADMIN", tenant, true, null));
        String achat = abonner(creerUtilisateur(roleAchat, tenant, true, null));
        abonner(creerUtilisateur(roleSansDroit, tenant, true, null));
        abonner(creerUtilisateur(roleAchat, tenant, false, null));
        abonner(creerUtilisateur(roleAchat, tenant, true, 99L));
        abonner(creerUtilisateur("ROLE_ADMIN", autreTenant, true, null));
        abonner(creerUtilisateur(roleAchat, autreTenant, true, null));

        Set<String> recus = abonnements.destinataires(tenant, PushNotificationService.HABILITATION_BL_EXPEDIE)
                .stream().map(PushSubscription::getEndpoint).collect(Collectors.toSet());

        assertEquals(Set.of(admin, achat), recus);
    }

    @Test
    void unAppareilReenregistreChangeDeProprietaire() {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        long premier = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        long second = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        String endpoint = endpoint();

        notifications.abonner(premier, endpoint, P256DH, AUTH);
        notifications.abonner(second, endpoint, P256DH, AUTH);

        assertTrue(abonnements.findByUserId(premier).isEmpty());
        assertEquals(1, abonnements.findByUserId(second).size());
    }

    @Test
    void unUtilisateurNePeutPasRetirerLAppareilDUnAutre() {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        long proprietaire = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        long autre = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        String endpoint = endpoint();
        notifications.abonner(proprietaire, endpoint, P256DH, AUTH);

        notifications.desabonner(autre, endpoint);
        assertEquals(1, abonnements.findByUserId(proprietaire).size());

        notifications.desabonner(proprietaire, endpoint);
        assertTrue(abonnements.findByUserId(proprietaire).isEmpty());
    }

    @Test
    void unAbonnementExpireEstSupprimeEtUnEchecTemporaireConserve() {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        long admin = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        String expire = abonner(admin);
        String enPanne = abonner(admin);
        when(sender.envoyer(eq(expire), any(), any(), any())).thenReturn(WebPushSender.Resultat.EXPIRE);
        when(sender.envoyer(eq(enPanne), any(), any(), any())).thenReturn(WebPushSender.Resultat.ECHEC);

        notifications.notifierBlExpedie(new BonLivraisonExpedieEvent(1L, "BL-1", tenant));

        List<String> restants = abonnements.findByUserId(admin).stream().map(PushSubscription::getEndpoint).toList();
        assertEquals(List.of(enPanne), restants);
    }

    @Test
    void lePushPartApresLeCommitEtJamaisApresUnRollback() {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);
        String endpoint = abonner(creerUtilisateur("ROLE_ADMIN", tenant, true, null));
        when(sender.envoyer(eq(endpoint), any(), any(), any())).thenReturn(WebPushSender.Resultat.ENVOYE);
        TransactionTemplate transaction = new TransactionTemplate(transactions);

        transaction.executeWithoutResult(etat -> {
            notifier.signaler(bl(tenant), com.gestion.persistent.enums.StatutLivraison.EN_ATTENTE);
            etat.setRollbackOnly();
        });
        verify(sender, after(1500).never()).envoyer(eq(endpoint), any(), any(), any());

        transaction.executeWithoutResult(etat ->
                notifier.signaler(bl(tenant), com.gestion.persistent.enums.StatutLivraison.EN_ATTENTE));
        verify(sender, timeout(5000)).envoyer(eq(endpoint), eq(P256DH), eq(AUTH), any());
    }

    @Test
    void uneActionDuLivreurQuiPasseLeBlAExpedieEnvoieUnMessageSansClientNiMontant() throws Exception {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        connecter(tenant);
        String endpoint = abonner(creerUtilisateur("ROLE_ADMIN", tenant, true, null));
        when(sender.envoyer(eq(endpoint), any(), any(), any())).thenReturn(WebPushSender.Resultat.ENVOYE);
        long client = jdbc.queryForObject("INSERT INTO clients (nom, nom_complet, pays, point_de_vente_id) VALUES ('Droguerie', 'Droguerie Moulay', 'MA', ?) RETURNING id", Long.class, tenant);
        String numero = "BL-PUSH-" + UUID.randomUUID().toString().substring(0, 8);
        long blId = jdbc.queryForObject("""
                INSERT INTO bons_livraison_client (numero_bl, date_bl, client_id, point_de_vente_id, statut)
                VALUES (?, CURRENT_TIMESTAMP, ?, ?, 'EN_ATTENTE') RETURNING id
                """, Long.class, numero, client, tenant);

        declarer(blId, numero);

        org.mockito.ArgumentCaptor<byte[]> message = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(sender, timeout(5000)).envoyer(eq(endpoint), eq(P256DH), eq(AUTH), message.capture());
        String texte = new String(message.getValue(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(texte.contains("BL " + numero + " expédié"), texte);
        assertTrue(texte.contains("/livraisons/expedition/" + blId), texte);
        assertTrue(!texte.contains("Droguerie"), texte);
        assertEquals("EXPEDIE", jdbc.queryForObject("SELECT statut FROM bons_livraison_client WHERE id = ?", String.class, blId));

        // Déjà expédié : une seconde déclaration ne renotifie pas
        declarer(blId, numero);
        verify(sender, after(1500).times(1)).envoyer(eq(endpoint), any(), any(), any());
    }

    @Test
    void lesRoutesLisentLUtilisateurDuTokenEtRefusentUnServiceInconnu() throws Exception {
        long tenant = SocieteDeTest.creer().pointDeVenteId();
        long id = creerUtilisateur("ROLE_ADMIN", tenant, true, null);
        User utilisateur = utilisateurs.findById(id).orElseThrow();
        var connecte = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(utilisateur);
        String endpoint = endpoint();
        when(sender.actif()).thenReturn(true);
        when(sender.clePublique()).thenReturn("CLE-PUBLIQUE");

        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/push/configuration"))
                .andExpect(result -> assertTrue(List.of(401, 403).contains(result.getResponse().getStatus()), "anonyme refusé"));

        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/push/configuration").with(connecte))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.clePublique").value("CLE-PUBLIQUE"));

        http.perform(abonnement(endpoint).with(connecte))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        assertEquals(1, abonnements.findByUserId(id).size());

        http.perform(abonnement("https://169.254.169.254/latest/meta-data").with(connecte))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        assertEquals(1, abonnements.findByUserId(id).size());

        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/push/abonnements/retrait").with(connecte)
                        .contentType("application/json").content("{\"endpoint\":\"" + endpoint + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        assertTrue(abonnements.findByUserId(id).isEmpty());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder abonnement(String endpoint) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/push/abonnements").contentType("application/json")
                .content("{\"endpoint\":\"" + endpoint + "\",\"p256dh\":\"" + P256DH + "\",\"auth\":\"" + AUTH + "\"}");
    }

    private void declarer(long blId, String numero) throws Exception {
        livreurs.declarerAction(null, com.gestion.persistent.enums.TypeDocumentLivreur.BON_LIVRAISON, blId, numero, "Droguerie",
                com.gestion.persistent.enums.TypeActionLivreur.FACTURE_DEPOSEE, null, "Livreur", null, null, null);
    }

    private static com.gestion.persistent.model.BonLivraisonClient bl(long tenant) {
        com.gestion.persistent.model.BonLivraisonClient bl = new com.gestion.persistent.model.BonLivraisonClient();
        bl.setId(1L);
        bl.setNumeroBl("BL-TEST");
        bl.setStatut(com.gestion.persistent.enums.StatutLivraison.EXPEDIE);
        bl.setPointDeVenteId(tenant);
        return bl;
    }

    private static void connecter(long tenant) {
        User acteur = new User();
        acteur.setTenantId(tenant);
        acteur.setPointDeVenteId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acteur, null, List.of()));
    }

    private String abonner(long userId) {
        String endpoint = endpoint();
        notifications.abonner(userId, endpoint, P256DH, AUTH);
        return endpoint;
    }

    private static String endpoint() {
        return "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
    }

    private String creerRole(String nom, boolean habilite) {
        jdbc.update("INSERT INTO roles (nom) VALUES (?)", nom);
        if (habilite) {
            jdbc.update("""
                    INSERT INTO roles_habilitations (role_id, habilitation_id)
                    SELECT r.id, h.id FROM roles r, habilitations h WHERE r.nom = ? AND h.nom = 'NOTIF_BL_EXPEDIE'
                    """, nom);
        }
        return nom;
    }

    private long creerUtilisateur(String role, long tenant, boolean actif, Long clientId) {
        String nom = "push-" + UUID.randomUUID();
        return jdbc.queryForObject("""
                INSERT INTO users (username, email, password, role_id, tenant_id, point_de_vente_id, enabled, client_id,
                                   account_non_expired, account_non_locked, credentials_non_expired)
                SELECT ?, ?, 'x', r.id, ?, ?, ?, ?, TRUE, TRUE, TRUE FROM roles r WHERE r.nom = ?
                RETURNING id
                """, Long.class, nom, nom + "@exemple.invalid", tenant, tenant, actif, clientId, role);
    }
}

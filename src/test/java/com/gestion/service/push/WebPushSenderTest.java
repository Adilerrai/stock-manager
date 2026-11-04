package com.gestion.service.push;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Joue le service de push et le navigateur : le déchiffrement suit la RFC 8291 avec le seul JDK, sans la librairie testée. */
class WebPushSenderTest {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private HttpServer serveur;
    private final AtomicInteger statutARepondre = new AtomicInteger(201);
    private final AtomicReference<byte[]> corpsRecu = new AtomicReference<>();
    private final AtomicReference<String> encodageRecu = new AtomicReference<>();
    private final AtomicReference<String> autorisationRecue = new AtomicReference<>();
    private final AtomicReference<String> ttlRecu = new AtomicReference<>();

    private KeyPair clesNavigateur;
    private byte[] secretAuth;
    private String p256dh;
    private String auth;
    private String endpoint;

    @BeforeEach
    void demarrer() throws Exception {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/push/abc", echange -> {
            corpsRecu.set(echange.getRequestBody().readAllBytes());
            encodageRecu.set(echange.getRequestHeaders().getFirst("Content-Encoding"));
            autorisationRecue.set(echange.getRequestHeaders().getFirst("Authorization"));
            ttlRecu.set(echange.getRequestHeaders().getFirst("TTL"));
            echange.sendResponseHeaders(statutARepondre.get(), -1);
            echange.close();
        });
        serveur.start();
        endpoint = "http://127.0.0.1:" + serveur.getAddress().getPort() + "/push/abc";

        clesNavigateur = pairesEc();
        secretAuth = new byte[16];
        new SecureRandom().nextBytes(secretAuth);
        p256dh = B64.encodeToString(pointNonCompresse((ECPublicKey) clesNavigateur.getPublic()));
        auth = B64.encodeToString(secretAuth);
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    @Test
    void leMessageArriveChiffreEtSigneEtLeNavigateurPeutLeLire() throws Exception {
        byte[] message = "{\"titre\":\"BL BL-2026-0012 expédié\"}".getBytes(StandardCharsets.UTF_8);

        WebPushSender.Resultat resultat = expediteur().envoyer(endpoint, p256dh, auth, message);

        assertEquals(WebPushSender.Resultat.ENVOYE, resultat);
        assertEquals("aes128gcm", encodageRecu.get());
        assertEquals("86400", ttlRecu.get());
        assertNotNull(autorisationRecue.get());
        assertTrue(autorisationRecue.get().startsWith("vapid t="), autorisationRecue.get());
        assertFalse(new String(corpsRecu.get(), StandardCharsets.ISO_8859_1).contains("BL-2026-0012"), "le corps doit être chiffré");
        assertArrayEquals(message, dechiffrer(corpsRecu.get()));
    }

    @Test
    void unAbonnementDisparuEstSignaleCommeExpire() throws Exception {
        statutARepondre.set(410);
        assertEquals(WebPushSender.Resultat.EXPIRE, expediteur().envoyer(endpoint, p256dh, auth, new byte[]{1}));
        statutARepondre.set(404);
        assertEquals(WebPushSender.Resultat.EXPIRE, expediteur().envoyer(endpoint, p256dh, auth, new byte[]{1}));
    }

    @Test
    void uneErreurDuServiceNEffacePasLAbonnement() throws Exception {
        statutARepondre.set(500);
        assertEquals(WebPushSender.Resultat.ECHEC, expediteur().envoyer(endpoint, p256dh, auth, new byte[]{1}));
    }

    @Test
    void sansClesLeServiceResteInactifSansLever() {
        WebPushSender inactif = new WebPushSender("", "", "");
        assertFalse(inactif.actif());
        assertEquals(WebPushSender.Resultat.ECHEC, inactif.envoyer(endpoint, p256dh, auth, new byte[]{1}));
    }

    @Test
    void desClesInvalidesCoupentLePushSansEmpecherLeDemarrage() {
        assertFalse(new WebPushSender("pas-une-cle", "pas-une-cle", "mailto:test@exemple.invalid").actif());
    }

    private WebPushSender expediteur() throws Exception {
        KeyPair vapid = pairesEc();
        byte[] prive = ((ECPrivateKey) vapid.getPrivate()).getS().toByteArray();
        byte[] prive32 = new byte[32];
        int taille = Math.min(prive.length, 32);
        System.arraycopy(prive, prive.length - taille, prive32, 32 - taille, taille);
        return new WebPushSender(B64.encodeToString(pointNonCompresse((ECPublicKey) vapid.getPublic())),
                B64.encodeToString(prive32), "mailto:test@exemple.invalid");
    }

    /** RFC 8188 et RFC 8291, côté navigateur. */
    private byte[] dechiffrer(byte[] corps) throws Exception {
        ByteBuffer entete = ByteBuffer.wrap(corps);
        byte[] sel = new byte[16];
        entete.get(sel);
        entete.getInt();
        int longueurCle = entete.get() & 0xff;
        byte[] cleServeur = new byte[longueurCle];
        entete.get(cleServeur);
        byte[] chiffre = new byte[entete.remaining()];
        entete.get(chiffre);

        KeyAgreement accord = KeyAgreement.getInstance("ECDH");
        accord.init(clesNavigateur.getPrivate());
        accord.doPhase(clePublique(cleServeur), true);
        byte[] secretPartage = accord.generateSecret();

        ByteArrayOutputStream infoCle = new ByteArrayOutputStream();
        infoCle.write("WebPush: info\0".getBytes(StandardCharsets.US_ASCII));
        infoCle.write(pointNonCompresse((ECPublicKey) clesNavigateur.getPublic()));
        infoCle.write(cleServeur);
        byte[] ikm = hkdf(secretAuth, secretPartage, infoCle.toByteArray(), 32);

        byte[] cek = hkdf(sel, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(sel, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] clair = aes.doFinal(chiffre);

        int fin = clair.length - 1;
        while (fin >= 0 && clair[fin] == 0) {
            fin--;
        }
        assertEquals(2, clair[fin], "délimiteur du dernier enregistrement");
        return Arrays.copyOf(clair, fin);
    }

    private static byte[] hkdf(byte[] sel, byte[] ikm, byte[] info, int taille) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(sel, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(info);
        mac.update((byte) 1);
        return Arrays.copyOf(mac.doFinal(), taille);
    }

    private static KeyPair pairesEc() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("EC");
        generateur.initialize(new ECGenParameterSpec("secp256r1"));
        return generateur.generateKeyPair();
    }

    private static PublicKey clePublique(byte[] point) throws Exception {
        AlgorithmParameters parametres = AlgorithmParameters.getInstance("EC");
        parametres.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = parametres.getParameterSpec(ECParameterSpec.class);
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(point, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(point, 33, 65));
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), spec));
    }

    private static byte[] pointNonCompresse(ECPublicKey cle) {
        byte[] sortie = new byte[65];
        sortie[0] = 4;
        copier(cle.getW().getAffineX(), sortie, 1);
        copier(cle.getW().getAffineY(), sortie, 33);
        return sortie;
    }

    private static void copier(BigInteger valeur, byte[] cible, int position) {
        byte[] octets = valeur.toByteArray();
        int taille = Math.min(octets.length, 32);
        System.arraycopy(octets, octets.length - taille, cible, position + 32 - taille, taille);
    }
}

package com.gestion.service;

import com.gestion.mapper.LivraisonMapper;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivraisonStatutCommandeTest {

    @Mock
    private LivraisonRepository livraisonRepository;
    @Mock
    private LivraisonMapper livraisonMapper;
    @Mock
    private LigneLivraisonRepository ligneLivraisonRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private StockService stockService;
    @Mock
    private DepotRepository depotRepository;
    @Mock
    private MouvementStockService mouvementStockService;
    @Mock
    private LotService lotService;
    @Mock
    private CommandeRepository commandeRepository;
    @Mock
    private LigneCommandeRepository ligneCommandeRepository;

    private LivraisonService livraisonService;

    @BeforeEach
    void setUp() {
        livraisonService = new LivraisonService(
                livraisonRepository,
                livraisonMapper,
                ligneLivraisonRepository,
                produitRepository,
                stockService,
                depotRepository,
                mouvementStockService,
                lotService,
                commandeRepository,
                ligneCommandeRepository
        );
    }

    @Test
    @DisplayName("Réception partielle: commande passe en StatutCommande.PARTIELLE et ligne mise à jour")
    void testMettreAJourStatutLivraisonCommande_Partielle() {
        Long commandeId = 10L;
        Commande commande = new Commande();
        commande.setId(commandeId);
        commande.setStatut(StatutCommande.PASSEE);
        commande.setStatutLivraison(StatutLivraison.EN_ATTENTE);

        Produit produit = new Produit();
        produit.setId(100L);

        LigneCommande ligne = new LigneCommande();
        ligne.setId(1000L);
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(10);
        ligne.setQuantiteLivree(0);

        when(ligneCommandeRepository.findByCommande_Id(commandeId))
                .thenReturn(List.of(ligne));

        // Livraison validée de 4 sur 10
        Livraison livraison = new Livraison();
        livraison.setId(500L);
        livraison.setStatut(StatutLivraison.LIVREE);

        LigneLivraison ll = new LigneLivraison();
        ll.setProduit(produit);
        ll.setQuantiteLivree(4L);
        livraison.setLignesLivraison(List.of(ll));

        when(livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE))
                .thenReturn(List.of(livraison));

        // When
        livraisonService.mettreAJourStatutLivraisonCommande(commande);

        // Then
        assertEquals(StatutCommande.PARTIELLE, commande.getStatut());
        assertEquals(StatutLivraison.PARTIELLE, commande.getStatutLivraison());
        assertEquals(4, ligne.getQuantiteLivree());
        verify(ligneCommandeRepository).save(ligne);
        verify(commandeRepository).save(commande);
    }

    @Test
    @DisplayName("Réception totale: commande passe en StatutCommande.LIVREE et date livraison réelle renseignée")
    void testMettreAJourStatutLivraisonCommande_Totale() {
        Long commandeId = 20L;
        Commande commande = new Commande();
        commande.setId(commandeId);
        commande.setStatut(StatutCommande.PARTIELLE);
        commande.setStatutLivraison(StatutLivraison.PARTIELLE);

        Produit produit = new Produit();
        produit.setId(200L);

        LigneCommande ligne = new LigneCommande();
        ligne.setId(2000L);
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(10);
        ligne.setQuantiteLivree(4);

        when(ligneCommandeRepository.findByCommande_Id(commandeId))
                .thenReturn(List.of(ligne));

        // Deux livraisons : 4 + 6 = 10
        Livraison liv1 = new Livraison();
        liv1.setStatut(StatutLivraison.LIVREE);
        LigneLivraison ll1 = new LigneLivraison();
        ll1.setProduit(produit);
        ll1.setQuantiteLivree(4L);
        liv1.setLignesLivraison(List.of(ll1));

        Livraison liv2 = new Livraison();
        liv2.setStatut(StatutLivraison.LIVREE);
        LigneLivraison ll2 = new LigneLivraison();
        ll2.setProduit(produit);
        ll2.setQuantiteLivree(6L);
        liv2.setLignesLivraison(List.of(ll2));

        when(livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE))
                .thenReturn(List.of(liv1, liv2));

        // When
        livraisonService.mettreAJourStatutLivraisonCommande(commande);

        // Then
        assertEquals(StatutCommande.LIVREE, commande.getStatut());
        assertEquals(StatutLivraison.LIVREE, commande.getStatutLivraison());
        assertEquals(10, ligne.getQuantiteLivree());
        assertNotNull(commande.getDateLivraisonReelle());
        verify(ligneCommandeRepository).save(ligne);
        verify(commandeRepository).save(commande);
    }
}

package com.gestion.service;

import com.gestion.mapper.LivraisonMapper;
import com.gestion.mapper.ProduitMapper;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandeReceptionTest {

    @Mock
    private CommandeRepository commandeRepository;
    @Mock
    private LigneCommandeRepository ligneCommandeRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private LivraisonService livraisonService;
    @Mock
    private LivraisonRepository livraisonRepository;
    @Mock
    private LivraisonMapper livraisonMapper;
    @Mock
    private ProduitMapper produitMapper;

    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        commandeService = new CommandeService(
                commandeRepository,
                ligneCommandeRepository,
                fournisseurRepository,
                produitRepository,
                livraisonService,
                livraisonRepository,
                livraisonMapper,
                produitMapper
        );
    }

    @Test
    @DisplayName("Cas 1: Réception totale de la commande - statut passe à LIVREE")
    void testReceptionTotaleCommande() {
        // Given
        Long commandeId = 1L;
        Commande commande = new Commande();
        commande.setId(commandeId);
        commande.setNumeroCommande("CMD-001");
        commande.setStatut(StatutCommande.PASSEE);
        commande.setStatutLivraison(StatutLivraison.EN_ATTENTE);

        Produit produit = new Produit();
        produit.setId(10L);
        produit.setNom("Produit Test");

        LigneCommande ligne = new LigneCommande();
        ligne.setId(100L);
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(10);
        ligne.setQuantiteLivree(0);
        ligne.setPrixUnitaire(BigDecimal.valueOf(50));
        ligne.setMontantLigne(BigDecimal.valueOf(500));

        commande.setLignesCommande(List.of(ligne));

        when(commandeRepository.findByIdAndPointDeVenteId(eq(commandeId), anyLong()))
                .thenReturn(Optional.of(commande));
        when(livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE))
                .thenReturn(Collections.emptyList());

        ProduitDTO produitDTO = new ProduitDTO();
        produitDTO.setId(10L);
        when(produitMapper.toDto(produit)).thenReturn(produitDTO);

        Livraison mockLivraison = new Livraison();
        mockLivraison.setId(50L);
        when(livraisonMapper.toEntity(any(LivraisonDTO.class))).thenReturn(mockLivraison);
        when(livraisonService.creerLivraison(any(Livraison.class))).thenReturn(mockLivraison);

        // When
        Commande result = commandeService.receptionnerCommande(commandeId, null);

        // Then
        assertNotNull(result);
        verify(livraisonService).creerLivraison(mockLivraison);
        verify(livraisonService).validerLivraison(50L);

        ArgumentCaptor<LivraisonDTO> captor = ArgumentCaptor.forClass(LivraisonDTO.class);
        verify(livraisonMapper).toEntity(captor.capture());
        LivraisonDTO createdLivraison = captor.getValue();
        assertEquals(commandeId, createdLivraison.getCommandeId());
        assertEquals(1, createdLivraison.getLignesLivraison().size());
        assertEquals(10L, createdLivraison.getLignesLivraison().get(0).getQuantiteLivree());
    }

    @Test
    @DisplayName("Cas 2: Réception partielle de la commande - enregistre la quantité reçue")
    void testReceptionPartielleCommande() {
        // Given
        Long commandeId = 2L;
        Commande commande = new Commande();
        commande.setId(commandeId);
        commande.setNumeroCommande("CMD-002");
        commande.setStatut(StatutCommande.PASSEE);
        commande.setStatutLivraison(StatutLivraison.EN_ATTENTE);

        Produit produit = new Produit();
        produit.setId(20L);
        produit.setNom("Produit Partiel");

        LigneCommande ligne = new LigneCommande();
        ligne.setId(200L);
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(20);
        ligne.setQuantiteLivree(0);
        ligne.setPrixUnitaire(BigDecimal.valueOf(15));
        ligne.setMontantLigne(BigDecimal.valueOf(300));

        commande.setLignesCommande(List.of(ligne));

        when(commandeRepository.findByIdAndPointDeVenteId(eq(commandeId), anyLong()))
                .thenReturn(Optional.of(commande));
        when(livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE))
                .thenReturn(Collections.emptyList());

        ProduitDTO produitDTO = new ProduitDTO();
        produitDTO.setId(20L);
        when(produitMapper.toDto(produit)).thenReturn(produitDTO);

        Livraison mockLivraison = new Livraison();
        mockLivraison.setId(51L);
        when(livraisonMapper.toEntity(any(LivraisonDTO.class))).thenReturn(mockLivraison);
        when(livraisonService.creerLivraison(any(Livraison.class))).thenReturn(mockLivraison);

        // Réception de 8 sur 20
        ReceptionCommandeDTO dto = new ReceptionCommandeDTO();
        LigneReceptionDTO item = new LigneReceptionDTO(200L, 20L, 8);
        dto.setLignes(List.of(item));

        // When
        Commande result = commandeService.receptionnerCommande(commandeId, dto);

        // Then
        assertNotNull(result);
        verify(livraisonService).validerLivraison(51L);

        ArgumentCaptor<LivraisonDTO> captor = ArgumentCaptor.forClass(LivraisonDTO.class);
        verify(livraisonMapper).toEntity(captor.capture());
        LivraisonDTO createdLivraison = captor.getValue();
        assertEquals(8L, createdLivraison.getLignesLivraison().get(0).getQuantiteLivree());
    }

    @Test
    @DisplayName("Erreur si la quantité reçue dépasse la quantité restante")
    void testQuantiteDepassementErreur() {
        Long commandeId = 3L;
        Commande commande = new Commande();
        commande.setId(commandeId);
        commande.setStatut(StatutCommande.PASSEE);

        Produit produit = new Produit();
        produit.setId(30L);
        produit.setNom("Produit Test Excès");

        LigneCommande ligne = new LigneCommande();
        ligne.setId(300L);
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(5);
        ligne.setQuantiteLivree(0);

        commande.setLignesCommande(List.of(ligne));

        when(commandeRepository.findByIdAndPointDeVenteId(eq(commandeId), anyLong()))
                .thenReturn(Optional.of(commande));
        when(livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE))
                .thenReturn(Collections.emptyList());

        // Demande 10 alors qu'il n'y a que 5
        ReceptionCommandeDTO dto = new ReceptionCommandeDTO();
        LigneReceptionDTO item = new LigneReceptionDTO(300L, 30L, 10);
        dto.setLignes(List.of(item));

        assertThrows(IllegalArgumentException.class, () ->
                commandeService.receptionnerCommande(commandeId, dto));
    }
}

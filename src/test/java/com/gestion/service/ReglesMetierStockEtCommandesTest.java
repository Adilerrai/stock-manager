package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.dto.PointDeVenteRequest;
import com.acommon.persistant.dto.PointDeVenteResponse;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import com.gestion.service.EntrepriseProfileService;
import com.acommon.service.PointDeVenteService;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.LigneBonLivraisonClientDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReglesMetierStockEtCommandesTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockQualiteRepository stockQualiteRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private EntrepriseProfileService entrepriseProfileService;
    @Mock
    private CommandeClientRepository commandeClientRepository;
    @Mock
    private BonLivraisonClientRepository bonLivraisonClientRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DepotRepository depotRepository;
    @Mock
    private PointDeVenteRepository pointDeVenteRepository;
    @Mock
    private UserRepository userRepository;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockRepository, stockQualiteRepository, produitRepository, entrepriseProfileService);
    }

    @Test
    @DisplayName("Stock: Si total=10, dispo=4, réservé=6, retirer 6 depuis réservation réussit et ne bloque pas")
    void testRetirerStockDepuisReservation() {
        Long produitId = 1L;
        Produit produit = new Produit();
        produit.setId(produitId);
        produit.setNom("Produit Test");

        Stock stock = new Stock();
        stock.setProduit(produit);
        stock.setQuantiteDisponible(new BigDecimal("4.00"));
        stock.setQuantiteReservee(new BigDecimal("6.00"));

        when(entrepriseProfileService.isVenteStockNegatifAutorisee()).thenReturn(false);
        when(stockRepository.findByProduitId(produitId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Déstockage lors de l'expédition du BL pour la commande de 6
        Stock result = stockService.retirerStock(produitId, new BigDecimal("6.00"), true);

        // Assert: La réserve passe de 6 à 0, le disponible reste 4
        assertEquals(0, result.getQuantiteReservee().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getQuantiteDisponible().compareTo(new BigDecimal("4.00")));
    }

    @Test
    @DisplayName("Stock: Retirer hors réservation alors que dispo < demandé doit lever IllegalArgumentException")
    void testRetirerStockHorsReservationBloque() {
        Long produitId = 1L;
        Produit produit = new Produit();
        produit.setId(produitId);
        produit.setNom("Produit Test");

        Stock stock = new Stock();
        stock.setProduit(produit);
        stock.setQuantiteDisponible(new BigDecimal("4.00"));
        stock.setQuantiteReservee(new BigDecimal("6.00"));

        when(entrepriseProfileService.isVenteStockNegatifAutorisee()).thenReturn(false);
        when(stockRepository.findByProduitId(produitId)).thenReturn(Optional.of(stock));

        // Act & Assert: Sans réservation, 4 < 6 => Bloqué
        assertThrows(IllegalArgumentException.class, () -> {
            stockService.retirerStock(produitId, new BigDecimal("6.00"), false);
        });
    }

    @Test
    @DisplayName("PointDeVente: Création automatique du Dépôt Principal pour chaque Point de Vente")
    void testCreationDepotPrincipalAutomatique() {
        PointDeVenteService pdvService = new PointDeVenteService(pointDeVenteRepository, userRepository, depotRepository);

        PointDeVenteRequest request = new PointDeVenteRequest();
        request.setNomPointDeVente("Boutique Tanger");
        request.setAdresse("Boulevard Pasteur");

        PointDeVente savedPdv = new PointDeVente();
        savedPdv.setId(42L);
        savedPdv.setNomPointDeVente("Boutique Tanger");
        savedPdv.setAdresse("Boulevard Pasteur");
        savedPdv.setTenantId(42L);

        when(pointDeVenteRepository.save(any(PointDeVente.class))).thenReturn(savedPdv);
        when(depotRepository.existsByNomAndPointDeVenteId("Dépôt Principal", 42L)).thenReturn(false);

        PointDeVenteResponse response = pdvService.createPointDeVente(request);

        assertNotNull(response);
        ArgumentCaptor<Depot> depotCaptor = ArgumentCaptor.forClass(Depot.class);
        verify(depotRepository).save(depotCaptor.capture());
        Depot createdDepot = depotCaptor.getValue();
        assertEquals("Dépôt Principal", createdDepot.getNom());
        assertEquals(42L, createdDepot.getPointDeVenteId());
        assertTrue(createdDepot.getActif());
    }
}

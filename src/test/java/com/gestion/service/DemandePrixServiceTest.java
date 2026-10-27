package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.dto.FusionOffresFournisseurDTO;
import com.gestion.persistent.dto.LigneDemandePrixDTO;
import com.gestion.persistent.enums.Incoterm;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutDemandePrix;
import com.gestion.persistent.enums.StatutOffreFournisseur;
import com.gestion.persistent.enums.TypeAchat;
import com.gestion.persistent.model.*;
import com.gestion.repository.DemandePrixRepository;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.OffreFournisseurRepository;
import com.gestion.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandePrixServiceTest {

    @Mock
    private DemandePrixRepository demandePrixRepository;
    @Mock
    private OffreFournisseurRepository offreFournisseurRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private CodificationService codificationService;
    @Mock
    private CommandeService commandeService;
    @Mock
    private EntrepriseProfileService entrepriseProfileService;

    private DemandePrixService demandePrixService;

    @BeforeEach
    void setUp() {
        demandePrixService = new DemandePrixService(
                demandePrixRepository,
                offreFournisseurRepository,
                fournisseurRepository,
                produitRepository,
                codificationService,
                commandeService,
                entrepriseProfileService
        );
    }

    @Test
    @DisplayName("Modification réussie d'une demande de prix en brouillon (champs, lignes, fournisseurs)")
    void testUpdateDemandePrix_Success() {
        DemandePrix ddp = new DemandePrix();
        ddp.setId(1L);
        ddp.setNumeroDdp("DDP-2026-0001");
        ddp.setObjet("Ancien objet");
        ddp.setStatut(StatutDemandePrix.BROUILLON);
        ddp.setPointDeVenteId(1L);

        LigneDemandePrix ligne1 = new LigneDemandePrix();
        ligne1.setId(101L);
        ligne1.setDesignation("Article Ancien 1");
        ligne1.setQuantiteDemandee(new BigDecimal("5.00"));
        ligne1.setDemandePrix(ddp);
        ddp.getLignes().add(ligne1);

        LigneDemandePrix ligne2 = new LigneDemandePrix();
        ligne2.setId(102L);
        ligne2.setDesignation("Article à supprimer");
        ligne2.setQuantiteDemandee(new BigDecimal("2.00"));
        ligne2.setDemandePrix(ddp);
        ddp.getLignes().add(ligne2);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(1L), any())).thenReturn(Optional.of(ddp));
        when(demandePrixRepository.save(any(DemandePrix.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DemandePrixDTO updateDTO = new DemandePrixDTO();
        updateDTO.setObjet("Nouvel objet mis à jour");
        updateDTO.setTypeDemandePrix(TypeAchat.INTERNATIONAL);
        updateDTO.setIncotermSouhaite(Incoterm.FOB);
        updateDTO.setDeviseSouhaitee("EUR");
        updateDTO.setLieuLivraison("Port de Casablanca");

        // Ligne 1 modifiée, ligne 2 omise (suppression), ligne 3 nouvelle
        List<LigneDemandePrixDTO> newLignes = new ArrayList<>();
        LigneDemandePrixDTO l1Dto = new LigneDemandePrixDTO();
        l1Dto.setId(101L);
        l1Dto.setDesignation("Article Modifié 1");
        l1Dto.setQuantiteDemandee(new BigDecimal("15.00"));
        l1Dto.setUnite("KG");
        newLignes.add(l1Dto);

        LigneDemandePrixDTO l3Dto = new LigneDemandePrixDTO();
        l3Dto.setDesignation("Nouvel Article 3");
        l3Dto.setQuantiteDemandee(new BigDecimal("50.00"));
        l3Dto.setUnite("U");
        newLignes.add(l3Dto);
        updateDTO.setLignes(newLignes);

        // Fournisseurs
        Fournisseur frn1 = new Fournisseur();
        frn1.setId(50L);
        frn1.setNom("Fournisseur 1");
        when(fournisseurRepository.findById(50L)).thenReturn(Optional.of(frn1));
        updateDTO.setFournisseurIds(Arrays.asList(50L));

        DemandePrixDTO result = demandePrixService.updateDemandePrix(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Nouvel objet mis à jour", result.getObjet());
        assertEquals(TypeAchat.INTERNATIONAL, result.getTypeDemandePrix());
        assertEquals("EUR", result.getDeviseSouhaitee());
        assertEquals(2, result.getLignes().size());
        assertEquals(1, result.getFournisseurIds().size());
        assertTrue(result.getFournisseurIds().contains(50L));
    }

    @Test
    @DisplayName("Blocage de la modification si la demande de prix est CLOTUREE")
    void testUpdateDemandePrix_ThrowsWhenCloturee() {
        DemandePrix ddp = new DemandePrix();
        ddp.setId(2L);
        ddp.setStatut(StatutDemandePrix.CLOTUREE);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(2L), any())).thenReturn(Optional.of(ddp));

        DemandePrixDTO dto = new DemandePrixDTO();
        dto.setObjet("Tentative");

        CommonException ex = assertThrows(CommonException.class, () -> demandePrixService.updateDemandePrix(2L, dto));
        assertTrue(ex.getMessage().contains("clôturée ou annulée"));
    }

    @Test
    @DisplayName("Blocage de la modification si une offre fournisseur est déjà RETENUE")
    void testUpdateDemandePrix_ThrowsWhenOffreRetenue() {
        DemandePrix ddp = new DemandePrix();
        ddp.setId(3L);
        ddp.setStatut(StatutDemandePrix.REPONSES_RECUES);

        OffreFournisseur offre = new OffreFournisseur();
        offre.setStatut(StatutOffreFournisseur.RETENUE);
        ddp.getOffres().add(offre);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(3L), any())).thenReturn(Optional.of(ddp));

        DemandePrixDTO dto = new DemandePrixDTO();
        dto.setObjet("Tentative");

        CommonException ex = assertThrows(CommonException.class, () -> demandePrixService.updateDemandePrix(3L, dto));
        assertTrue(ex.getMessage().contains("retenue et commandée"));
    }

    @Test
    @DisplayName("Suppression réussie d'une demande de prix")
    void testDeleteDemandePrix_Success() {
        DemandePrix ddp = new DemandePrix();
        ddp.setId(4L);
        ddp.setStatut(StatutDemandePrix.BROUILLON);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(4L), any())).thenReturn(Optional.of(ddp));

        assertDoesNotThrow(() -> demandePrixService.deleteDemandePrix(4L));
        verify(demandePrixRepository, times(1)).delete(ddp);
    }

    @Test
    @DisplayName("Validation et ajout réussi d'une offre à une commande fournisseur brouillon existante")
    void testValiderEtTransformerEnCommande_AjoutCommandeBrouillonExistante() {
        Fournisseur frn = new Fournisseur();
        frn.setId(10L);
        frn.setNom("ESPANO CERAME");

        DemandePrix ddp = new DemandePrix();
        ddp.setId(50L);
        ddp.setNumeroDdp("DDP-2026-0050");
        ddp.setStatut(StatutDemandePrix.REPONSES_RECUES);

        Produit p1 = new Produit();
        p1.setId(1001L);
        p1.setNom("Produit A");

        LigneDemandePrix ldp = new LigneDemandePrix();
        ldp.setId(501L);
        ldp.setProduit(p1);
        ldp.setDesignation("Produit A");
        ldp.setReference("REF-A");

        OffreFournisseur offre = new OffreFournisseur();
        offre.setId(80L);
        offre.setFournisseur(frn);
        offre.setNumeroDevis("DEV-80");
        offre.setDemandePrix(ddp);

        LigneOffreFournisseur lof = new LigneOffreFournisseur();
        lof.setId(801L);
        lof.setOffreFournisseur(offre);
        lof.setLigneDemandePrix(ldp);
        lof.setQuantiteProposee(new BigDecimal("10.00"));
        lof.setPrixUnitaireHT(new BigDecimal("150.00"));
        lof.setTauxTVA(new BigDecimal("20.00"));
        offre.getLignes().add(lof);

        ddp.getOffres().add(offre);

        Commande commandeBrouillon = new Commande();
        commandeBrouillon.setId(900L);
        commandeBrouillon.setNumeroCommande("CMD-2026-0900");
        commandeBrouillon.setFournisseur(frn);
        commandeBrouillon.setStatut(StatutCommande.BROUILLON);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(50L), any())).thenReturn(Optional.of(ddp));
        when(commandeService.getCommandeById(900L)).thenReturn(commandeBrouillon);
        when(commandeService.ajouterLignesACommandeBrouillon(eq(900L), anyList(), anyString())).thenReturn(commandeBrouillon);

        Commande result = demandePrixService.validerEtTransformerEnCommande(50L, 80L, 900L);

        assertNotNull(result);
        assertEquals(StatutDemandePrix.CLOTUREE, ddp.getStatut());
        assertEquals(StatutOffreFournisseur.RETENUE, offre.getStatut());
        assertTrue(lof.getEstRetenue());
        verify(commandeService, times(1)).ajouterLignesACommandeBrouillon(eq(900L), anyList(), contains("DDP-2026-0050"));
    }

    @Test
    @DisplayName("Échec si le fournisseur de la commande brouillon ne correspond pas à celui de l'offre")
    void testValiderEtTransformerEnCommande_ThrowsWhenFournisseurDifferent() {
        Fournisseur frn1 = new Fournisseur();
        frn1.setId(10L);
        frn1.setNom("Fournisseur Offre");

        Fournisseur frn2 = new Fournisseur();
        frn2.setId(20L);
        frn2.setNom("Autre Fournisseur Commande");

        DemandePrix ddp = new DemandePrix();
        ddp.setId(51L);
        ddp.setNumeroDdp("DDP-2026-0051");

        OffreFournisseur offre = new OffreFournisseur();
        offre.setId(81L);
        offre.setFournisseur(frn1);
        offre.setDemandePrix(ddp);
        ddp.getOffres().add(offre);

        Commande commandeBrouillon = new Commande();
        commandeBrouillon.setId(901L);
        commandeBrouillon.setFournisseur(frn2);
        commandeBrouillon.setStatut(StatutCommande.BROUILLON);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(51L), any())).thenReturn(Optional.of(ddp));
        when(commandeService.getCommandeById(901L)).thenReturn(commandeBrouillon);

        CommonException ex = assertThrows(CommonException.class, 
                () -> demandePrixService.validerEtTransformerEnCommande(51L, 81L, 901L));
        assertTrue(ex.getMessage().contains("ne correspond pas au fournisseur de l'offre"));
    }

    @Test
    @DisplayName("Échec si la commande existante n'est pas au statut BROUILLON")
    void testValiderEtTransformerEnCommande_ThrowsWhenCommandeNonBrouillon() {
        Fournisseur frn = new Fournisseur();
        frn.setId(10L);
        frn.setNom("Fournisseur 10");

        DemandePrix ddp = new DemandePrix();
        ddp.setId(52L);
        ddp.setNumeroDdp("DDP-2026-0052");

        OffreFournisseur offre = new OffreFournisseur();
        offre.setId(82L);
        offre.setFournisseur(frn);
        offre.setDemandePrix(ddp);
        ddp.getOffres().add(offre);

        Commande commandeValidee = new Commande();
        commandeValidee.setId(902L);
        commandeValidee.setFournisseur(frn);
        commandeValidee.setStatut(StatutCommande.VALIDEE);

        when(demandePrixRepository.findByIdAndPointDeVenteId(eq(52L), any())).thenReturn(Optional.of(ddp));
        when(commandeService.getCommandeById(902L)).thenReturn(commandeValidee);

        CommonException ex = assertThrows(CommonException.class, 
                () -> demandePrixService.validerEtTransformerEnCommande(52L, 82L, 902L));
        assertTrue(ex.getMessage().contains("pas au statut BROUILLON"));
    }

    @Test
    @DisplayName("Fusion de plusieurs offres fournisseurs vers une commande brouillon existante")
    void testAjouterOffresACommandeBrouillon_Success() {
        Fournisseur frn = new Fournisseur();
        frn.setId(10L);
        frn.setNom("ESPANO CERAME");

        DemandePrix ddp1 = new DemandePrix();
        ddp1.setId(101L);
        ddp1.setNumeroDdp("DDP-101");
        OffreFournisseur offre1 = new OffreFournisseur();
        offre1.setId(1L);
        offre1.setFournisseur(frn);
        offre1.setDemandePrix(ddp1);
        ddp1.getOffres().add(offre1);

        DemandePrix ddp2 = new DemandePrix();
        ddp2.setId(102L);
        ddp2.setNumeroDdp("DDP-102");
        OffreFournisseur offre2 = new OffreFournisseur();
        offre2.setId(2L);
        offre2.setFournisseur(frn);
        offre2.setDemandePrix(ddp2);
        ddp2.getOffres().add(offre2);

        Commande cmdBrouillon = new Commande();
        cmdBrouillon.setId(500L);
        cmdBrouillon.setNumeroCommande("CMD-500");
        cmdBrouillon.setFournisseur(frn);
        cmdBrouillon.setStatut(StatutCommande.BROUILLON);

        when(commandeService.getCommandeById(500L)).thenReturn(cmdBrouillon);
        when(offreFournisseurRepository.findByIdAndPointDeVenteId(eq(1L), any())).thenReturn(Optional.of(offre1));
        when(offreFournisseurRepository.findByIdAndPointDeVenteId(eq(2L), any())).thenReturn(Optional.of(offre2));
        when(commandeService.ajouterLignesACommandeBrouillon(eq(500L), anyList(), anyString())).thenReturn(cmdBrouillon);

        Commande res = demandePrixService.ajouterOffresACommandeBrouillon(500L, Arrays.asList(1L, 2L));

        assertNotNull(res);
        assertEquals(StatutOffreFournisseur.RETENUE, offre1.getStatut());
        assertEquals(StatutOffreFournisseur.RETENUE, offre2.getStatut());
        assertEquals(StatutDemandePrix.CLOTUREE, ddp1.getStatut());
        assertEquals(StatutDemandePrix.CLOTUREE, ddp2.getStatut());
        verify(commandeService, times(1)).ajouterLignesACommandeBrouillon(eq(500L), anyList(), contains("DDP-101"));
    }
}

package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.dto.LigneDemandePrixDTO;
import com.gestion.persistent.enums.Incoterm;
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
}

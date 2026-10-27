package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.dto.EntrepriseProfileDTO;
import com.gestion.persistent.dto.FactureDTO;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.EntrepriseProfile;
import com.gestion.persistent.model.Facture;
import com.gestion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentNumeroModificationTest {

    @Mock
    private EntrepriseProfileRepository entrepriseProfileRepository;
    @Mock
    private com.gestion.mapper.EntrepriseProfileMapper entrepriseProfileMapper;
    @Mock
    private CommandeClientRepository commandeClientRepository;
    @Mock
    private BonLivraisonClientRepository bonLivraisonClientRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private LigneFactureRepository ligneFactureRepository;
    @Mock
    private com.gestion.mapper.FactureMapper factureMapper;
    @Mock
    private com.gestion.mapper.BonLivraisonClientMapper bonLivraisonClientMapper;
    @Mock
    private CodificationService codificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private GedService gedService;
    @Mock
    private DepotRepository depotRepository;
    @Mock
    private MouvementStockService mouvementStockService;
    @Mock
    private StockRepository stockRepository;

    private EntrepriseProfileService entrepriseProfileService;
    private FactureService factureService;
    private BonLivraisonClientService bonLivraisonClientService;

    @BeforeEach
    void setUp() {
        entrepriseProfileService = new EntrepriseProfileService(
                entrepriseProfileRepository,
                entrepriseProfileMapper,
                null,
                commandeClientRepository
        );

        factureService = new FactureService(
                factureRepository,
                ligneFactureRepository,
                bonLivraisonClientRepository,
                clientRepository,
                userRepository,
                produitRepository,
                factureMapper,
                bonLivraisonClientMapper,
                null,
                codificationService,
                auditService,
                entrepriseProfileService
        );

        bonLivraisonClientService = new BonLivraisonClientService(
                bonLivraisonClientRepository,
                clientRepository,
                commandeClientRepository,
                produitRepository,
                depotRepository,
                mouvementStockService,
                bonLivraisonClientMapper,
                codificationService,
                stockRepository,
                auditService,
                gedService,
                entrepriseProfileService
        );
    }

    @Test
    @DisplayName("Paramètre désactivé : bloquer la modification manuelle du numéro de facture")
    void testModifierNumeroFactureBloqueQuandDesactive() {
        EntrepriseProfile profile = new EntrepriseProfile();
        profile.setModificationNumeroDocumentActive(false);
        when(entrepriseProfileRepository.findByPointDeVenteId(anyLong())).thenReturn(Optional.of(profile));

        Facture facture = new Facture();
        facture.setId(10L);
        facture.setNumeroFacture("FAC-2026-0001");
        facture.setPointDeVenteId(1L);
        when(factureRepository.findByIdAndPointDeVenteId(eq(10L), anyLong())).thenReturn(Optional.of(facture));

        FactureDTO dto = new FactureDTO();
        dto.setNumeroFacture("CUSTOM-FAC-999");

        CommonException ex = assertThrows(CommonException.class, () -> factureService.modifierFacture(10L, dto));
        assertTrue(ex.getMessage().contains("n'est pas autorisée"));
    }

    @Test
    @DisplayName("Paramètre activé : autoriser la modification du numéro de facture si unique")
    void testModifierNumeroFactureAutoriseQuandActive() {
        EntrepriseProfile profile = new EntrepriseProfile();
        profile.setModificationNumeroDocumentActive(true);
        when(entrepriseProfileRepository.findByPointDeVenteId(anyLong())).thenReturn(Optional.of(profile));

        Facture facture = new Facture();
        facture.setId(10L);
        facture.setNumeroFacture("FAC-2026-0001");
        facture.setPointDeVenteId(1L);
        when(factureRepository.findByIdAndPointDeVenteId(eq(10L), anyLong())).thenReturn(Optional.of(facture));
        when(factureRepository.findByPointDeVenteIdAndNumeroFacture(anyLong(), eq("CUSTOM-FAC-999"))).thenReturn(Optional.empty());
        when(factureRepository.save(any(Facture.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(factureMapper.toDto(any(Facture.class))).thenReturn(new FactureDTO());

        FactureDTO dto = new FactureDTO();
        dto.setNumeroFacture("CUSTOM-FAC-999");

        assertDoesNotThrow(() -> factureService.modifierFacture(10L, dto));
        assertEquals("CUSTOM-FAC-999", facture.getNumeroFacture());
    }

    @Test
    @DisplayName("Paramètre désactivé : bloquer la modification manuelle du numéro de BL")
    void testModifierNumeroBlBloqueQuandDesactive() {
        EntrepriseProfile profile = new EntrepriseProfile();
        profile.setModificationNumeroDocumentActive(false);
        when(entrepriseProfileRepository.findByPointDeVenteId(anyLong())).thenReturn(Optional.of(profile));

        BonLivraisonClient bl = new BonLivraisonClient();
        bl.setId(20L);
        bl.setNumeroBl("BL-0001");
        bl.setPointDeVenteId(1L);
        when(bonLivraisonClientRepository.findByIdAndPointDeVenteId(eq(20L), anyLong())).thenReturn(Optional.of(bl));

        BonLivraisonClientDTO dto = new BonLivraisonClientDTO();
        dto.setNumeroBl("CUSTOM-BL-888");

        CommonException ex = assertThrows(CommonException.class, () -> bonLivraisonClientService.updateBonLivraisonClient(20L, dto));
        assertTrue(ex.getMessage().contains("n'est pas autorisée"));
    }

    @Test
    @DisplayName("Paramètre activé : autoriser la modification du numéro de BL si unique")
    void testModifierNumeroBlAutoriseQuandActive() {
        EntrepriseProfile profile = new EntrepriseProfile();
        profile.setModificationNumeroDocumentActive(true);
        when(entrepriseProfileRepository.findByPointDeVenteId(anyLong())).thenReturn(Optional.of(profile));

        BonLivraisonClient bl = new BonLivraisonClient();
        bl.setId(20L);
        bl.setNumeroBl("BL-0001");
        bl.setPointDeVenteId(1L);
        when(bonLivraisonClientRepository.findByIdAndPointDeVenteId(eq(20L), anyLong())).thenReturn(Optional.of(bl));
        when(bonLivraisonClientRepository.findByPointDeVenteIdAndNumeroBl(anyLong(), eq("CUSTOM-BL-888"))).thenReturn(Optional.empty());
        when(bonLivraisonClientRepository.save(any(BonLivraisonClient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bonLivraisonClientMapper.toDto(any(BonLivraisonClient.class))).thenReturn(new BonLivraisonClientDTO());

        BonLivraisonClientDTO dto = new BonLivraisonClientDTO();
        dto.setNumeroBl("CUSTOM-BL-888");

        assertDoesNotThrow(() -> bonLivraisonClientService.updateBonLivraisonClient(20L, dto));
        assertEquals("CUSTOM-BL-888", bl.getNumeroBl());
    }
}

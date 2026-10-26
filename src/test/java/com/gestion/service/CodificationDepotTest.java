package com.gestion.service;

import com.gestion.persistent.dto.CodificationDocumentDTO;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.Depot;
import com.gestion.repository.CodificationDocumentRepository;
import com.gestion.repository.DepotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodificationDepotTest {

    @Mock
    private CodificationDocumentRepository repository;

    @Mock
    private DepotRepository depotRepository;

    private CodificationService codificationService;

    @BeforeEach
    void setUp() {
        codificationService = new CodificationService(repository, depotRepository);
    }

    @Test
    @DisplayName("Formatage BL Achat avec code dépôt : TAC/IN/00319")
    void testFormaterCode_BlAchatTacharouk() {
        String modele = "{DEPOT}/{PREFIX}/{NUM}";
        String prefixe = "IN";
        String codeDepot = "TAC";
        long numero = 319L;
        int longueur = 5;

        String resultat = codificationService.formaterCode(modele, prefixe, codeDepot, 2026, 10, numero, longueur);
        assertEquals("TAC/IN/00319", resultat);
    }

    @Test
    @DisplayName("Formatage BL Vente avec code dépôt : TAC/OUT/00319")
    void testFormaterCode_BlVenteTacharouk() {
        String modele = "{DEPOT}/{PREFIX}/{NUM}";
        String prefixe = "OUT";
        String codeDepot = "TAC";
        long numero = 319L;
        int longueur = 5;

        String resultat = codificationService.formaterCode(modele, prefixe, codeDepot, 2026, 10, numero, longueur);
        assertEquals("TAC/OUT/00319", resultat);
    }

    @Test
    @DisplayName("Formatage avec alias {CODE_DEPOT} : TAC/IN/00319")
    void testFormaterCode_AliasCodeDepot() {
        String modele = "{CODE_DEPOT}/{PREFIX}/{NUM}";
        String resultat = codificationService.formaterCode(modele, "IN", "TAC", 2026, 10, 319L, 5);
        assertEquals("TAC/IN/00319", resultat);
    }

    @Test
    @DisplayName("Aperçu codification DTO avec code dépôt TAC")
    void testGenererApercu_AvecDepotDto() {
        CodificationDocumentDTO dto = new CodificationDocumentDTO();
        dto.setTypeDocument(TypeDocumentCodification.BL_FOURNISSEUR);
        dto.setPrefixe("IN");
        dto.setModeleFormat("{DEPOT}/{PREFIX}/{NUM}");
        dto.setLongueurSequence(5);
        dto.setProchainNumero(319L);
        dto.setCodeDepot("TAC");

        String apercu = codificationService.genererApercu(dto);
        assertEquals("TAC/IN/00319", apercu);
    }

    @Test
    @DisplayName("Résolution code par défaut d'un dépôt : nom contenant TACHAROUK donne TAC")
    void testDepot_GetCodeOrDefault() {
        Depot d1 = new Depot();
        d1.setNom("MAGASIN TACHAROUK");
        assertEquals("TAC", d1.getCodeOrDefault());

        Depot d2 = new Depot();
        d2.setNom("DÉPÔT BERRECHID");
        assertEquals("BER", d2.getCodeOrDefault());

        Depot d3 = new Depot();
        d3.setNom("SIT TRANSMISSION");
        assertEquals("WH", d3.getCodeOrDefault());

        Depot d4 = new Depot();
        d4.setNom("Autre Dépôt");
        d4.setCode("XYZ");
        assertEquals("XYZ", d4.getCodeOrDefault());
    }

    @Test
    @DisplayName("Récupération code dépôt depuis le repository")
    void testGetCodeDepot_DepuisRepo() {
        Depot depot = new Depot();
        depot.setId(10L);
        depot.setNom("MAGASIN TACHAROUK");
        depot.setCode("TAC");

        when(depotRepository.findById(10L)).thenReturn(Optional.of(depot));

        String code = codificationService.getCodeDepot(10L, 1L);
        assertEquals("TAC", code);
    }
}

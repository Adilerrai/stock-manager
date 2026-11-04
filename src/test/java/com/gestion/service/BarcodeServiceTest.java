package com.gestion.service;

import com.gestion.persistent.dto.EtiquetteCodeBarreRequest;
import com.gestion.persistent.dto.ProduitCodeBarreDTO;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BarcodeServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private BarcodeService barcodeService;

    private Produit p1;
    private Produit p2;

    @BeforeEach
    void setUp() {
        p1 = new Produit();
        p1.setId(1L);
        p1.setReference("011106206-1");
        p1.setDesignation("ROULEMENT A BILLE SKF 2RS C4 6206");
        p1.setPrixVenteTtc(new BigDecimal("185.00"));
        p1.setActif(true);

        p2 = new Produit();
        p2.setId(2L);
        p2.setReference("093000014-1");
        p2.setDesignation("NETTOYEUR COURROIE U DOUBLE CIMES");
        p2.setPrixVenteTtc(new BigDecimal("420.00"));
        p2.setActif(true);
    }

    @Test
    void testGenerateBarcodePng() {
        byte[] png = barcodeService.generateBarcodePng("011106206-1", 200, 60);
        assertNotNull(png);
        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }

    @Test
    void testGenerateBarcodeBase64() {
        String base64 = barcodeService.generateBarcodeBase64("011106206-1");
        assertNotNull(base64);
        assertTrue(base64.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGetProduitsCodeBarres() {
        when(produitRepository.findByPointDeVenteId(anyLong())).thenReturn(Arrays.asList(p1, p2));

        List<ProduitCodeBarreDTO> dtos = barcodeService.getProduitsCodeBarres(null, null);
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("011106206-1", dtos.get(0).getReference());
        assertEquals("011106206-1", dtos.get(0).getCodeBarre());
        assertTrue(dtos.get(0).getBarcodeBase64().startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenererEtiquettesPdfGrilleParDefaut() {
        when(produitRepository.findByPointDeVenteId(anyLong())).thenReturn(Arrays.asList(p1, p2));

        EtiquetteCodeBarreRequest req = new EtiquetteCodeBarreRequest();
        req.setColonnes(2);
        req.setExemplaires(1);

        byte[] pdf = barcodeService.genererEtiquettesPdf(req);
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals((byte) '%', pdf[0]);
        assertEquals((byte) 'P', pdf[1]);
        assertEquals((byte) 'D', pdf[2]);
        assertEquals((byte) 'F', pdf[3]);
    }

    @Test
    void testGenererEtiquettesPdfSticker() {
        when(produitRepository.findByPointDeVenteId(anyLong())).thenReturn(Arrays.asList(p1, p2));

        EtiquetteCodeBarreRequest req = new EtiquetteCodeBarreRequest();
        req.setModele("sticker");
        req.setColonnes(2);
        req.setExemplaires(1);

        byte[] pdf = barcodeService.genererEtiquettesPdf(req);
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals((byte) '%', pdf[0]);
        assertEquals((byte) 'P', pdf[1]);
        assertEquals((byte) 'D', pdf[2]);
        assertEquals((byte) 'F', pdf[3]);
    }
}

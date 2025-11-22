package com.ceramique.controller;

import com.ceramique.persistent.dto.StockDTO;
import com.ceramique.persistent.dto.StockQualiteDTO;
import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.persistent.model.Stock;
import com.ceramique.persistent.model.StockQualite;
import com.ceramique.service.StockService;
import com.ceramique.mapper.StockMapper;
import com.ceramique.mapper.StockQualiteMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;
    private final StockMapper stockMapper;
    private final StockQualiteMapper stockQualiteMapper;

    public StockController(StockService stockService, StockMapper stockMapper, StockQualiteMapper stockQualiteMapper) {
        this.stockService = stockService;
        this.stockMapper = stockMapper;
        this.stockQualiteMapper = stockQualiteMapper;
    }

    @PostMapping("/initialize-with-qualities")
    public ResponseEntity<StockDTO> initializeStockWithQualities(
            @RequestParam Long produitId,
            @RequestBody Map<QualiteProduit, BigDecimal> quantitesParQualite,
            @RequestParam(required = false) BigDecimal seuilAlerte) {
        
        Stock stock = stockService.initializeStockWithQualities(produitId, quantitesParQualite, seuilAlerte);
        return ResponseEntity.ok(stockMapper.toDto(stock));
    }

    @PostMapping("/ajouter-qualite")
    public ResponseEntity<StockDTO> ajouterStockParQualite(
            @RequestParam Long produitId,
            @RequestParam QualiteProduit qualite,
            @RequestParam BigDecimal quantite) {
        
        Stock stock = stockService.ajouterStockParQualite(produitId, qualite, quantite);
        return ResponseEntity.ok(stockMapper.toDto(stock));
    }

    @PostMapping("/retirer-qualite")
    public ResponseEntity<StockDTO> retirerStockParQualite(
            @RequestParam Long produitId,
            @RequestParam QualiteProduit qualite,
            @RequestParam BigDecimal quantite) {
        
        Stock stock = stockService.retirerStockParQualite(produitId, qualite, quantite);
        return ResponseEntity.ok(stockMapper.toDto(stock));
    }

    @PostMapping("/reserver-qualite")
    public ResponseEntity<Boolean> reserverStockParQualite(
            @RequestParam Long produitId,
            @RequestParam QualiteProduit qualite,
            @RequestParam BigDecimal quantite) {
        
        boolean success = stockService.reserverStockParQualite(produitId, qualite, quantite);
        return ResponseEntity.ok(success);
    }

    @GetMapping("/with-qualities")
    public ResponseEntity<List<StockDTO>> getAllStocksWithQualities() {
        List<Stock> stocks = stockService.getAllStocksWithQualities();
        List<StockDTO> stockDTOs = stocks.stream()
                .map(stockMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDTOs);
    }

    @GetMapping("/by-qualite/{qualite}")
    public ResponseEntity<List<StockQualiteDTO>> getStocksByQualite(@PathVariable QualiteProduit qualite) {
        List<StockQualite> stocks = stockService.getStocksByQualite(qualite);
        List<StockQualiteDTO> stockDTOs = stocks.stream()
                .map(stockQualiteMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDTOs);
    }

    @GetMapping("/qualite-alerte")
    public ResponseEntity<List<StockQualiteDTO>> getStocksQualiteEnAlerte() {
        List<StockQualite> stocks = stockService.getStocksQualiteEnAlerte();
        List<StockQualiteDTO> stockDTOs = stocks.stream()
                .map(stockQualiteMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDTOs);
    }



    @GetMapping
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        List<Stock> stocks = stockService.getAllStocks();
        List<StockDTO> stockDTOs = stocks.stream()
                .map(stockMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDTOs);
    }
}
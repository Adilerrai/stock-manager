package com.gestion.controller;

import com.gestion.persistent.dto.DocumentOcrAnalysisResultDTO;
import com.gestion.service.DocumentAiExtractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr-ai")
@CrossOrigin(origins = "*")
public class DocumentOcrAiController {

    private final DocumentAiExtractionService documentAiService;

    public DocumentOcrAiController(DocumentAiExtractionService documentAiService) {
        this.documentAiService = documentAiService;
    }

    /**
     * Upload d'un document (PDF, PNG, JPG) pour extraction OCR et matching automatique via Groq AI
     */
    @PostMapping(value = "/analyser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentOcrAnalysisResultDTO> analyserDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "typeDocument", required = false) String typeDocument) {

        DocumentOcrAnalysisResultDTO result = documentAiService.extraireEtAnalyser(file, typeDocument);
        return ResponseEntity.ok(result);
    }

    /**
     * Enregistrement en base de données du document validé par l'utilisateur
     */
    @PostMapping("/enregistrer")
    public ResponseEntity<DocumentOcrAnalysisResultDTO> enregistrerDocument(
            @RequestBody DocumentOcrAnalysisResultDTO dto) {

        DocumentOcrAnalysisResultDTO saved = documentAiService.enregistrerDocument(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
}

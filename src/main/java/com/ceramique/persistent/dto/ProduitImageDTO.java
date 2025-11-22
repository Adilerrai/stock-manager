package com.ceramique.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProduitImageDTO {
    private Long id;
    private String fileName;
    
    @JsonIgnore // Ignorer les données binaires dans la sérialisation JSON
    private byte[] imageData;
    
    private String contentType;
    private String base64Data; // Pour l'affichage côté client
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { 
        this.imageData = imageData;
        // Convertir en base64 pour le frontend
        if (imageData != null) {
            this.base64Data = java.util.Base64.getEncoder().encodeToString(imageData);
        }
    }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getBase64Data() { return base64Data; }
    public void setBase64Data(String base64Data) { this.base64Data = base64Data; }
}

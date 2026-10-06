package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "produit_images")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProduitImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    @JsonIgnore
    private Produit produit;
    
    private String fileName;
    
    @Column(columnDefinition = "bytea") // Utiliser bytea au lieu de @Lob
    @JsonIgnore
    private byte[] imageData;
    
    private String contentType;
    
    @Column(name = "date_upload")
    private LocalDateTime dateUpload = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    @JsonIgnore
    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) {
        this.produit = produit;
    }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    @JsonIgnore
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public LocalDateTime getDateUpload() { return dateUpload; }
    public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }
}

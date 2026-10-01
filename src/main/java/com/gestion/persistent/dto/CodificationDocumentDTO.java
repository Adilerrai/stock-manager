package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.enums.TypeReinitialisationCodification;

public class CodificationDocumentDTO {

    private Long id;
    private TypeDocumentCodification typeDocument;
    private String libelleDocument;
    private String prefixe;
    private String modeleFormat;
    private Integer longueurSequence;
    private Long dernierNumero;
    private Long prochainNumero;
    private TypeReinitialisationCodification typeReinitialisation;
    private Boolean actif;
    private String apercu;

    public CodificationDocumentDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TypeDocumentCodification getTypeDocument() { return typeDocument; }
    public void setTypeDocument(TypeDocumentCodification typeDocument) { this.typeDocument = typeDocument; }

    public String getLibelleDocument() { return libelleDocument; }
    public void setLibelleDocument(String libelleDocument) { this.libelleDocument = libelleDocument; }

    public String getPrefixe() { return prefixe; }
    public void setPrefixe(String prefixe) { this.prefixe = prefixe; }

    public String getModeleFormat() { return modeleFormat; }
    public void setModeleFormat(String modeleFormat) { this.modeleFormat = modeleFormat; }

    public Integer getLongueurSequence() { return longueurSequence; }
    public void setLongueurSequence(Integer longueurSequence) { this.longueurSequence = longueurSequence; }

    public Long getDernierNumero() { return dernierNumero; }
    public void setDernierNumero(Long dernierNumero) { this.dernierNumero = dernierNumero; }

    public Long getProchainNumero() { return prochainNumero; }
    public void setProchainNumero(Long prochainNumero) { this.prochainNumero = prochainNumero; }

    public TypeReinitialisationCodification getTypeReinitialisation() { return typeReinitialisation; }
    public void setTypeReinitialisation(TypeReinitialisationCodification typeReinitialisation) { this.typeReinitialisation = typeReinitialisation; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getApercu() { return apercu; }
    public void setApercu(String apercu) { this.apercu = apercu; }
}

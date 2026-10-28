package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeProduit;
import com.gestion.persistent.enums.UniteMesure;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO produit destiné exclusivement au Catalogue de l'Espace Client :
 * AUCUN champ de tarification (ni prix d'achat, ni prix de vente)
 * et AUCUN champ de disponibilité ou de stock.
 */
public class CatalogueProduitClientDTO {
    private Long id;
    private String reference;
    private String designation;
    private String description;
    private Long categorieId;
    private String categorieNom;
    private String groupeArticle;
    private String codeBarre;
    private UniteMesure uniteMesure;
    private TypeProduit typeProduit;
    private Map<String, Object> attributes = new HashMap<>();
    private Boolean hasImage = false;
    private String imageContentType;
    private String imageBase64;

    public CatalogueProduitClientDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public String getGroupeArticle() { return groupeArticle; }
    public void setGroupeArticle(String groupeArticle) { this.groupeArticle = groupeArticle; }

    public String getCodeBarre() { return codeBarre; }
    public void setCodeBarre(String codeBarre) { this.codeBarre = codeBarre; }

    public UniteMesure getUniteMesure() { return uniteMesure; }
    public void setUniteMesure(UniteMesure uniteMesure) { this.uniteMesure = uniteMesure; }

    public TypeProduit getTypeProduit() { return typeProduit; }
    public void setTypeProduit(TypeProduit typeProduit) { this.typeProduit = typeProduit; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public Boolean getHasImage() { return hasImage; }
    public void setHasImage(Boolean hasImage) { this.hasImage = hasImage; }

    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}

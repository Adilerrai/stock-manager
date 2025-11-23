package com.ceramique.mapper;

import com.ceramique.persistent.dto.ProduitDTO;
import com.ceramique.persistent.dto.ProduitImageDTO;
import com.ceramique.persistent.model.Produit;
import com.ceramique.persistent.model.ProduitImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {ProduitImageMapper.class})
public abstract class ProduitMapper {

    @Autowired
    protected ProduitImageMapper produitImageMapper;

    @Mapping(target = "image", expression = "java(mapImageSafely(produit.getImage()))")
    @Mapping(target = "designation", source = "designation")
    public abstract ProduitDTO toDto(Produit produit);

    @Mapping(target = "dateCreation", ignore = true)
    public abstract Produit toEntity(ProduitDTO produitDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "image", ignore = true)
    public abstract void updateEntityFromDto(ProduitDTO produitDTO, @MappingTarget Produit produit);
    
    protected ProduitImageDTO mapImageSafely(ProduitImage image) {
        if (image == null) {
            return null;
        }
        // Laisser le mapper gérer base64 (retournera null si imageData non chargé)
        return produitImageMapper.toDto(image);
    }
}
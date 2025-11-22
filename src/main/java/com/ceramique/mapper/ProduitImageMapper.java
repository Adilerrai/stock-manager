package com.ceramique.mapper;

import com.ceramique.persistent.dto.ProduitImageDTO;
import com.ceramique.persistent.model.ProduitImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProduitImageMapper {

    @Mapping(target = "base64Data", expression = "java(convertToBase64(produitImage.getImageData()))")
    @Mapping(target = "imageData", ignore = true)
    ProduitImageDTO toDto(ProduitImage produitImage);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateUpload", ignore = true)
    @Mapping(target = "imageData", expression = "java(convertFromBase64(produitImageDTO.getBase64Data()))")
    ProduitImage toEntity(ProduitImageDTO produitImageDTO);
    
    default String convertToBase64(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        try {
            return java.util.Base64.getEncoder().encodeToString(imageData);
        } catch (Exception e) {
            System.err.println("Error converting to base64: " + e.getMessage());
            return null;
        }
    }
    
    default byte[] convertFromBase64(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return null;
        }
        try {
            return java.util.Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            System.err.println("Error decoding base64: " + e.getMessage());
            return null;
        }
    }
}
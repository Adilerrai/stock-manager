package com.gestion.mapper;

import com.gestion.persistent.dto.LigneBonLivraisonClientDTO;
import com.gestion.persistent.model.LigneBonLivraisonClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LigneBonLivraisonClientMapper {

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitReference", source = "produit.reference")
    @Mapping(target = "produitDesignation", source = "produit.designation")
    @Mapping(target = "depotId", source = "depot.id")
    @Mapping(target = "depotNom", source = "depot.nom")
    @Mapping(target = "lotId", source = "lot.id")
    @Mapping(target = "numeroLot", source = "lot.numeroLot")
    @Mapping(target = "prixVenteMin", source = "produit.prixVenteMin")
    @Mapping(target = "montantHT", expression = "java(entity.getMontantHT() != null ? entity.getMontantHT() : entity.getMontantLigne())")
    @Mapping(target = "tauxTVA", expression = "java(entity.getTauxTVA() != null ? entity.getTauxTVA() : java.math.BigDecimal.valueOf(20))")
    @Mapping(target = "montantTVA", expression = "java(entity.getMontantTVA() != null ? entity.getMontantTVA() : (entity.getMontantHT() != null ? entity.getMontantHT().multiply(java.math.BigDecimal.valueOf(0.20)).setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO))")
    @Mapping(target = "montantTTC", expression = "java(entity.getMontantTTC() != null ? entity.getMontantTTC() : (entity.getMontantLigne() != null ? entity.getMontantLigne().multiply(java.math.BigDecimal.valueOf(1.20)).setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO))")
    LigneBonLivraisonClientDTO toDto(LigneBonLivraisonClient entity);

    @Mapping(target = "bonLivraisonClient", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "depot", ignore = true)
    @Mapping(target = "lot", ignore = true)
    LigneBonLivraisonClient toEntity(LigneBonLivraisonClientDTO dto);
}

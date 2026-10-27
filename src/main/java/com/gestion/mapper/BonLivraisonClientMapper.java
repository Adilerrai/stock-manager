package com.gestion.mapper;

import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.model.BonLivraisonClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneBonLivraisonClientMapper.class, FactureMapper.class})
public interface BonLivraisonClientMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", expression = "java(entity.getClient() != null ? entity.getClient().getNomComplet() : null)")
    @Mapping(target = "clientTelephone", source = "client.telephone")
    @Mapping(target = "commandeClientId", source = "commandeClient.id")
    @Mapping(target = "commandeClientNumero", source = "commandeClient.numeroCommande")
    @Mapping(target = "numeroCommandeExterne", expression = "java(entity.getCommandeClient() != null && entity.getCommandeClient().getNumeroCommandeExterne() != null ? entity.getCommandeClient().getNumeroCommandeExterne() : (entity.getCommandes() != null && !entity.getCommandes().isEmpty() ? entity.getCommandes().stream().map(com.gestion.persistent.model.CommandeClient::getNumeroCommandeExterne).filter(java.util.Objects::nonNull).findFirst().orElse(null) : null))")
    @Mapping(target = "bcClient", expression = "java(entity.getCommandeClient() != null && entity.getCommandeClient().getNumeroCommandeExterne() != null ? entity.getCommandeClient().getNumeroCommandeExterne() : (entity.getCommandes() != null && !entity.getCommandes().isEmpty() ? entity.getCommandes().stream().map(com.gestion.persistent.model.CommandeClient::getNumeroCommandeExterne).filter(java.util.Objects::nonNull).findFirst().orElse(null) : null))")
    @Mapping(target = "commandeClientIds", expression = "java(entity.getCommandes() != null ? entity.getCommandes().stream().map(com.gestion.persistent.model.CommandeClient::getId).collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList())")
    @Mapping(target = "commandeClientNumeros", expression = "java(entity.getCommandes() != null ? entity.getCommandes().stream().map(com.gestion.persistent.model.CommandeClient::getNumeroCommande).collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList())")
    @Mapping(target = "factureId", source = "facture.id")
    @Mapping(target = "factureNumero", source = "facture.numeroFacture")
    @Mapping(target = "facturé", expression = "java(entity.getFacture() != null && !Boolean.TRUE.equals(entity.getFacture().getAnnulee()) && entity.getFacture().getStatut() != com.gestion.persistent.enums.StatutFacture.ANNULEE)")
    @Mapping(target = "facture", expression = "java(mapFactureIfNotAnnulee(entity.getFacture()))")
    @Mapping(target = "lignes", source = "lignes")
    @Mapping(target = "montantHT", expression = "java(entity.getMontantHT() != null ? entity.getMontantHT() : entity.getMontantTotal())")
    @Mapping(target = "tauxTVA", expression = "java(entity.getTauxTVA() != null ? entity.getTauxTVA() : java.math.BigDecimal.valueOf(20))")
    @Mapping(target = "montantTVA", expression = "java(entity.getMontantTVA() != null ? entity.getMontantTVA() : (entity.getMontantHT() != null ? entity.getMontantHT().multiply(java.math.BigDecimal.valueOf(0.20)).setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO))")
    @Mapping(target = "montantTTC", expression = "java(entity.getMontantTTC() != null ? entity.getMontantTTC() : (entity.getMontantTotal() != null ? entity.getMontantTotal().multiply(java.math.BigDecimal.valueOf(1.20)).setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO))")
    @Mapping(target = "depotId", expression = "java(entity.getLignes() != null && !entity.getLignes().isEmpty() && entity.getLignes().get(0).getDepot() != null ? entity.getLignes().get(0).getDepot().getId() : null)")
    @Mapping(target = "depotNom", expression = "java(entity.getLignes() != null && !entity.getLignes().isEmpty() && entity.getLignes().get(0).getDepot() != null ? entity.getLignes().get(0).getDepot().getNom() : null)")
    @Mapping(target = "piecesJointes", ignore = true)
    BonLivraisonClientDTO toDto(BonLivraisonClient entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "commandeClient", ignore = true)
    @Mapping(target = "commandes", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    BonLivraisonClient toEntity(BonLivraisonClientDTO dto);

    default com.gestion.persistent.dto.FactureDTO mapFactureIfNotAnnulee(com.gestion.persistent.model.Facture facture) {
        if (facture == null || Boolean.TRUE.equals(facture.getAnnulee()) || facture.getStatut() == com.gestion.persistent.enums.StatutFacture.ANNULEE) {
            return null;
        }
        return org.mapstruct.factory.Mappers.getMapper(FactureMapper.class).toDto(facture);
    }
}

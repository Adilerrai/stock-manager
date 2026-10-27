package com.gestion.mapper;

import com.gestion.persistent.dto.CommandeDTO;
import com.gestion.persistent.model.Commande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { LigneCommandeMapper.class, LivraisonMapper.class })
public interface CommandeMapper {

    @Mapping(target = "fournisseurId", source = "fournisseur.id")
    @Mapping(target = "fournisseurNom", source = "fournisseur.raisonSociale")
    @Mapping(target = "lieuDestinationId", source = "lieuDestination.id")
    @Mapping(target = "lieuDestinationLibelle", source = "lieuDestination.libelle")
    @Mapping(target = "conditionPaiementId", source = "conditionPaiement.id")
    @Mapping(target = "conditionPaiementLibelle", source = "conditionPaiement.libelle")
    @Mapping(target = "livraisons", expression = "java(mapLivraisonsIfNotAnnulee(commande.getLivraisons()))")
    CommandeDTO toDto(Commande commande);

    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "livraisons", ignore = true)
    @Mapping(target = "lieuDestination", ignore = true)
    @Mapping(target = "conditionPaiement", ignore = true)
    Commande toEntity(CommandeDTO commandeDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroCommande", ignore = true)
    @Mapping(target = "fournisseur", ignore = true)
    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "livraisons", ignore = true)
    @Mapping(target = "lieuDestination", ignore = true)
    @Mapping(target = "conditionPaiement", ignore = true)
    void updateEntityFromDto(CommandeDTO commandeDTO, @MappingTarget Commande commande);

    default java.util.List<com.gestion.persistent.dto.LivraisonDTO> mapLivraisonsIfNotAnnulee(
            java.util.List<com.gestion.persistent.model.Livraison> livraisons) {
        if (livraisons == null) {
            return java.util.Collections.emptyList();
        }
        com.gestion.mapper.LivraisonMapper mapper = com.acommon.config.ApplicationContextProvider
                .getBean(com.gestion.mapper.LivraisonMapper.class);
        if (mapper == null) {
            mapper = org.mapstruct.factory.Mappers.getMapper(com.gestion.mapper.LivraisonMapper.class);
        }
        final com.gestion.mapper.LivraisonMapper livraisonMapper = mapper;
        return livraisons.stream()
                .filter(l -> l.getStatut() != com.gestion.persistent.enums.StatutLivraison.ANNULEE)
                .map(livraisonMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }
}

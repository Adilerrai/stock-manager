package com.gestion.mapper;

import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.model.CommandeClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LigneCommandeClientMapper.class, BonLivraisonClientMapper.class, FactureMapper.class})
public interface CommandeClientMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "lignesCommande", source = "lignesCommande")
    @Mapping(target = "bonsLivraison", expression = "java(mapBonsLivraison(commandeClient))")
    @Mapping(target = "factures", expression = "java(mapFactures(commandeClient))")
    CommandeClientDTO toDto(CommandeClient commandeClient);

    @Mapping(target = "dateCommande", ignore = true)
    @Mapping(target = "lignesCommande", ignore = true)
    @Mapping(target = "client", ignore = true)
    CommandeClient toEntity(CommandeClientDTO commandeClientDTO);

    default java.util.List<com.gestion.persistent.dto.BonLivraisonClientDTO> mapBonsLivraison(CommandeClient commandeClient) {
        if (commandeClient == null || commandeClient.getId() == null) {
            return java.util.Collections.emptyList();
        }
        com.gestion.repository.BonLivraisonClientRepository blRepo =
                com.acommon.config.ApplicationContextProvider.getBean(com.gestion.repository.BonLivraisonClientRepository.class);
        if (blRepo == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<com.gestion.persistent.model.BonLivraisonClient> bls = blRepo.findAllByCommandeId(commandeClient.getId());
        if (bls == null || bls.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        com.gestion.mapper.BonLivraisonClientMapper mapper =
                com.acommon.config.ApplicationContextProvider.getBean(com.gestion.mapper.BonLivraisonClientMapper.class);
        if (mapper == null) {
            mapper = org.mapstruct.factory.Mappers.getMapper(com.gestion.mapper.BonLivraisonClientMapper.class);
        }
        final com.gestion.mapper.BonLivraisonClientMapper blMapper = mapper;
        return bls.stream()
                .filter(bl -> bl.getStatut() != com.gestion.persistent.enums.StatutLivraison.ANNULEE)
                .map(blMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    default java.util.List<com.gestion.persistent.dto.FactureDTO> mapFactures(CommandeClient commandeClient) {
        if (commandeClient == null || commandeClient.getId() == null) {
            return java.util.Collections.emptyList();
        }
        com.gestion.repository.BonLivraisonClientRepository blRepo =
                com.acommon.config.ApplicationContextProvider.getBean(com.gestion.repository.BonLivraisonClientRepository.class);
        if (blRepo == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<com.gestion.persistent.model.BonLivraisonClient> bls = blRepo.findAllByCommandeId(commandeClient.getId());
        if (bls == null || bls.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        com.gestion.mapper.FactureMapper mapper =
                com.acommon.config.ApplicationContextProvider.getBean(com.gestion.mapper.FactureMapper.class);
        if (mapper == null) {
            mapper = org.mapstruct.factory.Mappers.getMapper(com.gestion.mapper.FactureMapper.class);
        }
        final com.gestion.mapper.FactureMapper factureMapper = mapper;
        java.util.Map<Long, com.gestion.persistent.dto.FactureDTO> factureMap = new java.util.LinkedHashMap<>();
        for (com.gestion.persistent.model.BonLivraisonClient bl : bls) {
            if (bl.getStatut() == com.gestion.persistent.enums.StatutLivraison.ANNULEE) {
                continue;
            }
            com.gestion.persistent.model.Facture facture = bl.getFacture();
            if (facture != null && !Boolean.TRUE.equals(facture.getAnnulee()) && facture.getStatut() != com.gestion.persistent.enums.StatutFacture.ANNULEE) {
                if (!factureMap.containsKey(facture.getId())) {
                    factureMap.put(facture.getId(), factureMapper.toDto(facture));
                }
            }
        }
        return new java.util.ArrayList<>(factureMap.values());
    }
}

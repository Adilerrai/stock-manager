package com.gestion.service;

import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.VarianteProduit;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.VarianteProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class VarianteProduitService {

    private final VarianteProduitRepository varianteRepository;
    private final ProduitRepository produitRepository;

    public VarianteProduitService(VarianteProduitRepository varianteRepository,
                                  ProduitRepository produitRepository) {
        this.varianteRepository = varianteRepository;
        this.produitRepository = produitRepository;
    }

    public VarianteProduit ajouterVariante(Long produitParentId, VarianteProduit variante) {
        Produit parent = produitRepository.findById(produitParentId)
                .orElseThrow(() -> new RuntimeException("Produit parent non trouvé: " + produitParentId));

        variante.setProduitParent(parent);

        if (variante.getSku() == null || variante.getSku().trim().isEmpty()) {
            variante.setSku(parent.getReference() + "-" + (varianteRepository.findByProduitParentId(produitParentId).size() + 1));
        }

        if (variante.getPrixVente() == null) {
            variante.setPrixVente(parent.getPrixVenteHt() != null ? parent.getPrixVenteHt() : parent.getPrixVente());
        }

        if (variante.getPrixAchat() == null) {
            variante.setPrixAchat(parent.getPrixAchatHt() != null ? parent.getPrixAchatHt() : parent.getPrixAchat());
        }

        return varianteRepository.save(variante);
    }

    public List<VarianteProduit> getVariantesByProduit(Long produitParentId) {
        return varianteRepository.findByProduitParentId(produitParentId);
    }

    public VarianteProduit getVarianteById(Long id) {
        return varianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variante non trouvée: " + id));
    }

    public VarianteProduit modifierVariante(Long id, VarianteProduit maj) {
        VarianteProduit existante = getVarianteById(id);

        if (maj.getNomVariante() != null) existante.setNomVariante(maj.getNomVariante());
        if (maj.getTaille() != null) existante.setTaille(maj.getTaille());
        if (maj.getCouleur() != null) existante.setCouleur(maj.getCouleur());
        if (maj.getDimension() != null) existante.setDimension(maj.getDimension());
        if (maj.getPrixVente() != null) existante.setPrixVente(maj.getPrixVente());
        if (maj.getPrixAchat() != null) existante.setPrixAchat(maj.getPrixAchat());
        if (maj.getCodeBarre() != null) existante.setCodeBarre(maj.getCodeBarre());
        if (maj.getActif() != null) existante.setActif(maj.getActif());

        return varianteRepository.save(existante);
    }

    public VarianteProduit ajusterStock(Long id, BigDecimal delta) {
        VarianteProduit existante = getVarianteById(id);
        BigDecimal actuel = existante.getQuantiteStock() != null ? existante.getQuantiteStock() : BigDecimal.ZERO;
        existante.setQuantiteStock(actuel.add(delta));
        return varianteRepository.save(existante);
    }

    public void supprimerVariante(Long id) {
        varianteRepository.deleteById(id);
    }
}

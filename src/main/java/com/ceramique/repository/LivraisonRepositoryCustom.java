package com.ceramique.repository;

import com.ceramique.persistent.dto.LivraisonSearchCriteria;
import com.ceramique.persistent.model.Livraison;
import java.util.List;

public interface LivraisonRepositoryCustom {
    List<Livraison> findByCriteria(LivraisonSearchCriteria criteria, Long pointDeVenteId);
}
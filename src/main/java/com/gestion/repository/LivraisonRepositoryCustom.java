package com.gestion.repository;

import com.gestion.persistent.dto.LivraisonSearchCriteria;
import com.gestion.persistent.model.Livraison;
import java.util.List;

public interface LivraisonRepositoryCustom {
    List<Livraison> findByCriteria(LivraisonSearchCriteria criteria);
}

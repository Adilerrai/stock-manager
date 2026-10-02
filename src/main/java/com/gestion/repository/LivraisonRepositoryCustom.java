package com.gestion.repository;

import com.gestion.persistent.dto.LivraisonSearchCriteria;
import com.gestion.persistent.model.Livraison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LivraisonRepositoryCustom {
    List<Livraison> findByCriteria(LivraisonSearchCriteria criteria);
    Page<Livraison> findByCriteria(LivraisonSearchCriteria criteria, Pageable pageable);
}

package com.gestion.repository;

import com.gestion.persistent.dto.MouvementStockSearchCriteria;
import com.gestion.persistent.model.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MouvementStockRepositoryCustom {
    Page<MouvementStock> findByCriteria(MouvementStockSearchCriteria criteria, Pageable pageable);
}

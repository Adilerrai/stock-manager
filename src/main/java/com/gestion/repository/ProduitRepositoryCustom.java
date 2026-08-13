package com.gestion.repository;

import com.gestion.persistent.dto.ProduitSearchCriteria;
import com.gestion.persistent.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProduitRepositoryCustom {
    Page<Produit> findByCriteria(ProduitSearchCriteria criteria, Pageable pageable);
}


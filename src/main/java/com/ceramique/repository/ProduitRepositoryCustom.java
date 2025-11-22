package com.ceramique.repository;

import com.ceramique.persistent.dto.ProduitSearchCriteria;
import com.ceramique.persistent.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProduitRepositoryCustom {
    Page<Produit> findByCriteria(ProduitSearchCriteria criteria, Long pointDeVenteId, Pageable pageable);
}

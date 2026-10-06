package com.gestion.repository;

import com.gestion.persistent.dto.StockSearchCriteria;
import com.gestion.persistent.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockRepositoryCustom {
    Page<Stock> findByCriteria(StockSearchCriteria criteria, Pageable pageable);
}

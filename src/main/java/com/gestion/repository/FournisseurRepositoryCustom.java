package com.gestion.repository;

import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.model.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FournisseurRepositoryCustom {
    Page<Fournisseur> findByCriteria(FournisseurSearchCriteria criteria, Pageable pageable);
}



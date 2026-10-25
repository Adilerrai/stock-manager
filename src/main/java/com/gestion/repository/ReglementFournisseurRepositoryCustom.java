package com.gestion.repository;

import com.gestion.persistent.dto.ReglementFournisseurSearchCriteria;
import com.gestion.persistent.model.ReglementFournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReglementFournisseurRepositoryCustom {
    List<ReglementFournisseur> findByCriteria(ReglementFournisseurSearchCriteria criteria);
    Page<ReglementFournisseur> findByCriteria(ReglementFournisseurSearchCriteria criteria, Pageable pageable);
}

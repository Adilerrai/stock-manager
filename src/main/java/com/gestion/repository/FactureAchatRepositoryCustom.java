package com.gestion.repository;

import com.gestion.persistent.dto.FactureAchatSearchCriteria;
import com.gestion.persistent.model.FactureAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FactureAchatRepositoryCustom {
    List<FactureAchat> findByCriteria(FactureAchatSearchCriteria criteria);
    Page<FactureAchat> findByCriteria(FactureAchatSearchCriteria criteria, Pageable pageable);
}

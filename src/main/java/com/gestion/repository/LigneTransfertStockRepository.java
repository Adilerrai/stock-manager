package com.gestion.repository;

import com.gestion.persistent.model.LigneTransfertStock;
import com.gestion.persistent.model.TransfertStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneTransfertStockRepository extends JpaRepository<LigneTransfertStock, Long> {
    List<LigneTransfertStock> findByTransfert(TransfertStock transfert);
}

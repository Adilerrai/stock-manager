package com.ceramique.repository;

import com.ceramique.persistent.model.ProduitImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitImageRepository extends JpaRepository<ProduitImage, Long> {
}

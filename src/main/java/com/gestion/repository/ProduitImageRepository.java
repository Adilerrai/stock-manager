package com.gestion.repository;

import com.gestion.persistent.model.ProduitImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitImageRepository extends JpaRepository<ProduitImage, Long> {
}


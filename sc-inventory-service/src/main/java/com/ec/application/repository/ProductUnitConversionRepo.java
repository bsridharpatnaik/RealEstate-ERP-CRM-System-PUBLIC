package com.ec.application.repository;

import com.ec.application.model.ProductUnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductUnitConversionRepo extends JpaRepository<ProductUnitConversion, Long> {

    List<ProductUnitConversion> findByProductIdAndIsActiveTrue(Long productId);
}

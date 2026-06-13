package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.ProductUnitConversionDTO;
import com.ec.application.model.ProductUnitConversion;
import com.ec.application.repository.ProductUnitConversionRepo;
import com.ec.application.repository.PurchaseOrderLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class ProductUnitConversionService {

    private final ProductUnitConversionRepo repo;
    private final PurchaseOrderLineRepository poLineRepo;

    public List<ProductUnitConversionDTO> getConversions(Long productId) {
        return repo.findByProductIdAndIsActiveTrue(productId).stream()
                .map(c -> new ProductUnitConversionDTO(
                        c.getId(),
                        c.getProductId(),
                        c.getUnitName(),
                        c.getConversionFactor(),
                        poLineRepo.existsByProduct_ProductIdAndBillingUnitAndIsDeletedFalse(productId, c.getUnitName())
                ))
                .collect(Collectors.toList());
    }

    public ProductUnitConversion addConversion(Long productId, String unitName, Double conversionFactor) {
        ProductUnitConversion entity = new ProductUnitConversion();
        entity.setProductId(productId);
        entity.setUnitName(unitName);
        entity.setConversionFactor(conversionFactor);
        entity.setIsActive(true);
        return repo.save(entity);
    }

    public void deleteConversion(Long conversionId) {
        ProductUnitConversion entity = repo.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Unit conversion not found: " + conversionId));

        boolean usedInPO = poLineRepo.existsByProduct_ProductIdAndBillingUnitAndIsDeletedFalse(
                entity.getProductId(), entity.getUnitName());
        if (usedInPO) {
            throw new RuntimeException(
                    "Cannot delete: billing unit '" + entity.getUnitName() +
                    "' is already used in one or more purchase orders.");
        }

        entity.setIsActive(false);
        repo.save(entity);
    }
}

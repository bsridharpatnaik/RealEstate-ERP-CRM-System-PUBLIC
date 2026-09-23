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
                        c.getDisplayDirection() != null ? c.getDisplayDirection() : "BILLING_PER_BASE",
                        c.getReferenceUnit(),
                        c.getReferenceValue(),
                        poLineRepo.existsByProduct_ProductIdAndBillingUnitAndIsDeletedFalse(productId, c.getUnitName())
                ))
                .collect(Collectors.toList());
    }

    public ProductUnitConversion addConversion(Long productId, String unitName, Double conversionFactor,
                                               String displayDirection, String referenceUnit, Double referenceValue) {
        ProductUnitConversion entity = new ProductUnitConversion();
        entity.setProductId(productId);
        entity.setUnitName(unitName);
        entity.setConversionFactor(conversionFactor);
        entity.setDisplayDirection(displayDirection != null ? displayDirection : "BILLING_PER_BASE");
        entity.setReferenceUnit(referenceUnit);
        entity.setReferenceValue(referenceValue);
        entity.setIsActive(true);
        return repo.save(entity);
    }

    /**
     * Edits an existing conversion. Factor/direction changes are always allowed — existing PO lines
     * keep their own snapshot of billing qty/factor, so they are unaffected. Renaming the unit is
     * blocked while it's referenced by a PO (usedInPo is matched by unit name).
     */
    public ProductUnitConversion updateConversion(Long conversionId, String unitName, Double conversionFactor,
                                                  String displayDirection, String referenceUnit, Double referenceValue) {
        ProductUnitConversion entity = repo.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Unit conversion not found: " + conversionId));

        boolean renaming = unitName != null && !unitName.equals(entity.getUnitName());
        if (renaming) {
            boolean usedInPO = poLineRepo.existsByProduct_ProductIdAndBillingUnitAndIsDeletedFalse(
                    entity.getProductId(), entity.getUnitName());
            if (usedInPO) {
                throw new RuntimeException(
                        "Cannot rename billing unit '" + entity.getUnitName() +
                        "': it is already used in one or more purchase orders. You can still change the factor.");
            }
            entity.setUnitName(unitName);
        }
        if (conversionFactor != null) entity.setConversionFactor(conversionFactor);
        if (displayDirection != null) entity.setDisplayDirection(displayDirection);
        entity.setReferenceUnit(referenceUnit);
        entity.setReferenceValue(referenceValue);
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

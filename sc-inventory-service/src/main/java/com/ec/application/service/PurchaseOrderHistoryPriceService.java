package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.PoLineRateHistoryDTO;
import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.data.PriceScatterPointDTO;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.PurchaseOrderLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderHistoryPriceService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private PurchaseOrderLineRepository poLineRepo;

    @Autowired
    private EntityManager em;

    @UseDefaultTenant
    @Transactional(readOnly = true)
    public List<PreviousPurchaseRateDTO> getPreviousRates(Long productId) {
        List<Object[]> rows = poLineRepo.findPreviousRates(productId, STATUS_CANCELLED);
        if (rows == null) return Collections.emptyList();

        List<PreviousPurchaseRateDTO> dtos = rows.stream().map(row -> {
            PreviousPurchaseRateDTO dto = new PreviousPurchaseRateDTO();
            dto.setPurchaseOrderId((String) row[0]);
            dto.setPoDate((Date) row[1]);
            dto.setSupplierName((String) row[2]);
            dto.setQuantity(row[3] != null ? ((Number) row[3]).doubleValue() : null);
            dto.setRate(row[4] != null ? ((Number) row[4]).doubleValue() : null);
            dto.setDiscountPercent(row[5] != null ? ((Number) row[5]).doubleValue() : null);
            dto.setGstPercent(row[6] != null ? ((Number) row[6]).doubleValue() : null);
            dto.setNetRate(row[7] != null ? ((Number) row[7]).doubleValue() : null);
            dto.setSupplierId(row[8] != null ? ((Number) row[8]).longValue() : null);
            return dto;
        }).collect(Collectors.toList());

        // Batch-fetch on-time rate for all unique suppliers in this product's history
        Set<Long> supplierIds = dtos.stream()
                .map(PreviousPurchaseRateDTO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!supplierIds.isEmpty()) {
            Map<Long, Double> onTimeMap = fetchOnTimeRates(supplierIds);
            dtos.forEach(dto -> {
                if (dto.getSupplierId() != null) {
                    dto.setOnTimeRate(onTimeMap.get(dto.getSupplierId()));
                }
            });
        }

        return dtos;
    }

    private Map<Long, Double> fetchOnTimeRates(Set<Long> supplierIds) {
        String sql =
            "SELECT s.contactId," +
            "  ROUND(" +
            "    (COUNT(DISTINCT CASE WHEN po.status != 'CANCELLED' AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL THEN po.purchase_order_id END)" +
            "     - COUNT(DISTINCT CASE WHEN po.status NOT IN ('CANCELLED','COMPLETED','SHORT CLOSED','SHORT CLOSE') AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL AND DATEDIFF(CURDATE(), po.po_date) > COALESCE(p.lead_time_days, cat.lead_time_days) THEN po.purchase_order_id END)" +
            "    ) * 100.0 / NULLIF(COUNT(DISTINCT CASE WHEN po.status != 'CANCELLED' AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL THEN po.purchase_order_id END), 0)" +
            "  , 1) AS onTimeRate" +
            " FROM contacts s" +
            " JOIN purchase_order po ON po.supplier_id = s.contactId AND po.is_deleted = 0" +
            " JOIN purchase_order_line pol ON pol.po_id = po.purchase_order_id AND pol.is_deleted = 0" +
            " JOIN product p ON p.productId = pol.product_id AND p.is_deleted = 0" +
            " LEFT JOIN category cat ON cat.categoryId = p.categoryId AND cat.is_deleted = 0" +
            " WHERE s.is_deleted = 0 AND s.contactId IN (:ids)" +
            " GROUP BY s.contactId";

        Query q = em.createNativeQuery(sql);
        q.setParameter("ids", supplierIds);

        @SuppressWarnings("unchecked")
        List<Object[]> results = q.getResultList();
        Map<Long, Double> map = new HashMap<>();
        for (Object[] r : results) {
            if (r[0] != null) {
                long sid = ((Number) r[0]).longValue();
                Double rate = r[1] != null ? ((Number) r[1]).doubleValue() : null;
                map.put(sid, rate);
            }
        }
        return map;
    }

    public List<PriceScatterPointDTO> buildScatterPoints(List<PreviousPurchaseRateDTO> rates) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return rates.stream()
                .map(r -> new PriceScatterPointDTO(
                        r.getPoDate()
                                .toInstant()
                                .atZone(ZoneId.of("Asia/Kolkata"))
                                .toLocalDate()
                                .format(formatter),
                        r.getRate(),
                        r.getSupplierName(),
                        r.getPurchaseOrderId()
                ))
                .collect(Collectors.toList());
    }

    public List<PriceScatterPointDTO> getScatterTrend(Long productId) {
        List<PreviousPurchaseRateDTO> raw = getPreviousRates(productId);
        return buildScatterPoints(raw);
    }

    @UseDefaultTenant
    @Transactional(readOnly = true)
    public List<PoLineRateHistoryDTO> getRatesForAllProductsInPO(String poNumber) {
        List<PurchaseOrderLine> lines = poLineRepo.findLinesByPoNumber(poNumber);

        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        return lines.stream()
                .map(line -> {
                    Long productId = line.getProduct().getProductId();
                    List<PreviousPurchaseRateDTO> rates = getPreviousRates(productId);
                    return new PoLineRateHistoryDTO(
                            productId,
                            line.getProduct().getProductName(),
                            line.getProduct().getProductCode(),
                            rates
                    );
                })
                .collect(Collectors.toList());
    }
}

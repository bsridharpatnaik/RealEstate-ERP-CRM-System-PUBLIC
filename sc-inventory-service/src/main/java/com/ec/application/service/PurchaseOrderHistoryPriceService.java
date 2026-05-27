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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderHistoryPriceService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private PurchaseOrderLineRepository poLineRepo;

    @UseDefaultTenant
    @Transactional(readOnly = true)
    public List<PreviousPurchaseRateDTO> getPreviousRates(Long productId) {
        List<Object[]> rows = poLineRepo.findPreviousRates(productId, STATUS_CANCELLED);
        if (rows == null) return Collections.emptyList();

        return rows.stream().map(row -> {
            PreviousPurchaseRateDTO dto = new PreviousPurchaseRateDTO();
            dto.setPurchaseOrderId((String) row[0]);
            dto.setPoDate((Date) row[1]);
            dto.setSupplierName((String) row[2]);
            dto.setQuantity(row[3] != null ? ((Number) row[3]).doubleValue() : null);
            dto.setRate(row[4] != null ? ((Number) row[4]).doubleValue() : null);
            dto.setDiscountPercent(row[5] != null ? ((Number) row[5]).doubleValue() : null);
            dto.setGstPercent(row[6] != null ? ((Number) row[6]).doubleValue() : null);
            dto.setNetRate(row[7] != null ? ((Number) row[7]).doubleValue() : null);
            return dto;
        }).collect(Collectors.toList());
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

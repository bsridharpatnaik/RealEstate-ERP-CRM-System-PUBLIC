package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.data.PriceScatterPointDTO;
import com.ec.application.repository.PurchaseOrderLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderHistoryService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private PurchaseOrderLineRepository poLineRepo;

    @UseDefaultTenant
    @Transactional(readOnly = true)
    public List<PreviousPurchaseRateDTO> getPreviousRates(Long productId) {
        Pageable top15 = PageRequest.of(0, 15);
        List<PreviousPurchaseRateDTO> result = poLineRepo.findPreviousRates(productId, STATUS_CANCELLED, top15);
        return result != null ? result : Collections.emptyList();
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
}

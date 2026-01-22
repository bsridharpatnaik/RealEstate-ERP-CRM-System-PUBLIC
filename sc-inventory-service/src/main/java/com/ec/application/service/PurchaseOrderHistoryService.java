package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.repository.PurchaseOrderLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class PurchaseOrderHistoryService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private PurchaseOrderLineRepository poLineRepo;

    @UseDefaultTenant
    @Transactional(readOnly = true)
    public List<PreviousPurchaseRateDTO> getPreviousRates(Long productId, Pageable pageable) {
        List<PreviousPurchaseRateDTO> result = poLineRepo.findPreviousRates(productId, STATUS_CANCELLED, pageable);
        return result != null ? result : Collections.emptyList();
    }
}

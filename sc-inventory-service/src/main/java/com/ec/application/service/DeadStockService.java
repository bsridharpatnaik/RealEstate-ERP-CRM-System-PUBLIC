package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.DeadStockDTO;
import com.ec.application.data.DeadStockInformation;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.repository.DeadStockSummaryRepo;
import com.ec.application.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class DeadStockService {

    private final DeadStockSummaryRepo deadStockSummaryRepo;
    private final ProductRepo productRepo;
}

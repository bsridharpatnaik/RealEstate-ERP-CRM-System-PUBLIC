package com.ec.application.service;

import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.StockInformationExportDAO;
import com.ec.application.model.StockHistory;
import com.ec.application.repository.StockHistoryRepo;

@Service
@Transactional
public class StockHistoryService {
    @Autowired
    StockHistoryRepo stockHistoryRepo;

    @Autowired
    StockService stockService;

    @Transactional
    public void insertLatestStockHistory(List<StockInformationExportDAO> dataForInsertList) throws Exception {
        UUID uuid = UUID.randomUUID();

        for (StockInformationExportDAO dataForInsert : dataForInsertList) {
            StockHistory stockHistory = new StockHistory(dataForInsert, uuid);
            stockHistoryRepo.save(stockHistory);
        }

    }

}

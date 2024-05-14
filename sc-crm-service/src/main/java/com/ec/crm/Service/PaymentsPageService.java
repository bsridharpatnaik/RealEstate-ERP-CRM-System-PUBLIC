package com.ec.crm.Service;

import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.PaymentsPage;
import com.ec.crm.Repository.PaymentsPageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentsPageService {
    @Autowired
    PaymentsPageRepo paymentsPageRepo;

    public Page<PaymentsPage> findFilteredList(FilterDataList sourceFilterDataList, Pageable pageable) {
        return paymentsPageRepo.findAll(pageable);
    }
}

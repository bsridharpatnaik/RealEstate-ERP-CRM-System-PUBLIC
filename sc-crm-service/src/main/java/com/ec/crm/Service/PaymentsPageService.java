package com.ec.crm.Service;

import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Data.LeadActivityDropdownData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.PaymentPageSpecification;
import com.ec.crm.Model.PaymentsPage;
import com.ec.crm.Repository.ClosedLeadsRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.PaymentsPageRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentsPageService {
    @Autowired
    PaymentsPageRepo paymentsPageRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    ClosedLeadsRepo clRepo;

    Logger log = LoggerFactory.getLogger(PaymentsPageService.class);

    public Page<PaymentsPage> findFilteredList(FilterDataList filterDataList, Pageable pageable) throws Exception {
        Specification<PaymentsPage> spec = PaymentPageSpecification.getSpecification(filterDataList);
        return spec == null ? paymentsPageRepo.findAll(pageable) : paymentsPageRepo.findAll(spec, pageable);
    }

    public List<PaymentsPage> findFilteredListForExport(FilterDataList filterDataList, Pageable pageable) throws Exception {
        Specification<PaymentsPage> spec = PaymentPageSpecification.getSpecification(filterDataList);
        return spec == null ? paymentsPageRepo.findAll() : paymentsPageRepo.findAll(spec);
    }
    public DropdownForClosedLeads getDropDownValues() throws Exception {
        DropdownForClosedLeads dropdownValues = new DropdownForClosedLeads();
        dropdownValues.setDropdownData(populateDropdownService.fetchData("payment"));
        dropdownValues.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
        return dropdownValues;
    }

    private List<String> fetchTypeAheadForLeadGlobalSearch() {
        log.info("Invoked fetchTypeAheadForLeadGlobalSearch");
        List<String> typeAhead = new ArrayList<String>();
        typeAhead.addAll(clRepo.getLeadNames());
        typeAhead.addAll(clRepo.getLeadMobileNos());
        return typeAhead;
    }
}

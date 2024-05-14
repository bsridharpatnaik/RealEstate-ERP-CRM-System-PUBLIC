package com.ec.crm.Controller;

import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Data.LeadActivityDropdownData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.PaymentsPage;
import com.ec.crm.Service.PaymentsPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/customer/payments", produces =
        {"application/json", "text/json"})
public class PaymentsPageController {

    @Autowired
    PaymentsPageService paymentsPageService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<PaymentsPage> returnFilteredData(@RequestBody FilterDataList leadFilterDataList, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return paymentsPageService.findFilteredList(leadFilterDataList, pageable);
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.OK)
    public List<PaymentsPage> returnFilteredDataForExport(@RequestBody FilterDataList leadFilterDataList, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return paymentsPageService.findFilteredListForExport(leadFilterDataList, pageable);
    }

    @GetMapping("dropdown")
    @ResponseStatus(HttpStatus.OK)
    public DropdownForClosedLeads getDropdownValues() throws Exception {
        return paymentsPageService.getDropDownValues();
    }
}

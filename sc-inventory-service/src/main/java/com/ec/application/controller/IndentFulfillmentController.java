package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.IndentFulfillmentRow;
import com.ec.application.service.IndentFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/indent-fulfillment")
public class IndentFulfillmentController {

    @Autowired
    private IndentFulfillmentService service;

    /** Paginated filtered list. */
    @UseDefaultTenant
    @PostMapping("/list")
    public Page<IndentFulfillmentRow> list(
            @RequestBody FilterDataList filters,
            @PageableDefault(page = 0, size = 20, sort = "indentDate", direction = Sort.Direction.DESC)
            Pageable pageable) throws Exception {
        return service.getPage(filters, pageable);
    }

    /** Status summary stats. */
    @UseDefaultTenant
    @PostMapping("/stats")
    public Map<String, Long> stats(@RequestBody FilterDataList filters) throws Exception {
        return service.getSummaryStats(filters);
    }

    /** Dropdown: distinct project names. */
    @UseDefaultTenant
    @GetMapping("/projects")
    public List<String> projects() {
        return service.getDistinctProjects();
    }

    /** Export to Excel. */
    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody FilterDataList filters,
                            HttpServletResponse response) throws Exception {
        service.exportExcel(filters, response);
    }
}

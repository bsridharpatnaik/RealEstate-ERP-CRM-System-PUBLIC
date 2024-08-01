package com.ec.common.Controller;

import com.ec.common.Data.ApiLogReportDTO;
import com.ec.common.Service.ApiLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report")
public class ApiLogController {
    @Autowired
    private ApiLogService apiLogService;

    @GetMapping("/usagehistory")
    public List<ApiLogReportDTO> getApiLogReport() {
        return apiLogService.generateReport();
    }
}

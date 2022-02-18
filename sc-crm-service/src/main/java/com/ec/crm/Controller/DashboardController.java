package com.ec.crm.Controller;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ec.crm.Data.ActivitiesForDashboard;
import com.ec.crm.Data.PipelineForDashboard;
import com.ec.crm.Data.SalesFunnelDTO;
import com.ec.crm.Service.DashboardServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.DashboardData;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;

@RestController
@RequestMapping(value = "/dashboard", produces =
        {"application/json", "text/json"})
public class DashboardController {

    @Autowired
    DashboardServiceV2 dashboardService;

    @PostMapping("/customerpipeline")
    @ResponseStatus(HttpStatus.OK)
    public PipelineForDashboard getCustomerPipeline(@RequestBody DashboardData payload) throws Exception {
        return dashboardService.getPipelineForDashboard(payload);
    }

    @GetMapping("/activitystats")
    @ResponseStatus(HttpStatus.OK)
    public ActivitiesForDashboard getCustomerPipeline() throws Exception {
        return dashboardService.getActivitesForDashboard();
    }

    @GetMapping("/salesfunnel")
    public List<SalesFunnelDTO> getSalesFunnel() {
        return dashboardService.getSalesFunnel();
    }

    @GetMapping("/stagnant")
    @ResponseStatus(HttpStatus.OK)
    public List<StagnantStats> returnStagnantStats() {
        return dashboardService.returnStagnantStats();
    }

    @GetMapping("/conversionratio")
    @ResponseStatus(HttpStatus.OK)
    public List<ConversionRatio> conversionratio() throws ParseException {
        return dashboardService.conversionratio();
    }

    @GetMapping("/topperformer")
    @ResponseStatus(HttpStatus.OK)
    public Map topperformer() throws ParseException {

        return dashboardService.topperformer();

    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) ->
        {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}

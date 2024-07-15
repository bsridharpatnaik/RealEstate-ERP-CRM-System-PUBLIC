package com.ec.crm.Controller;

import com.ec.crm.Data.ActivitiesForDashboard;
import com.ec.crm.Service.AllActivitiesService;
import com.ec.crm.Service.EmailHelperService;
import com.ec.crm.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/email", produces = {"application/json", "text/json"})
public class EmailController {

    @Autowired
    EmailHelperService emailHelperService;

    @Autowired
    AllActivitiesService allActivitiesService;

    @GetMapping("/ua")
    @ResponseStatus(HttpStatus.OK)
    public void sendEmail() throws Exception {
        allActivitiesService.sendEveningEmailForLeadActivity(ThreadLocalStorage.getTenantName());
    }
}

package com.ec.crm.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ec.crm.Data.*;
import com.ec.crm.Enums.DealLostReasonEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Service.LeadActivityService;
import com.ec.crm.Service.NoteService;
import com.ec.crm.Service.SendCRMNotificationsService;
import com.ec.crm.Service.UserDetailsService;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.SneakyThrows;

@RestController
@RequestMapping(value = "/activity", produces =
        {"application/json", "text/json"})
public class LeadActivityController {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    SendCRMNotificationsService sendCRMNotificationsService;

    @Autowired
    LeadActivityService laService;

    @Autowired
    NoteService noteService;

    @Autowired
    HttpServletRequest request;

    @GetMapping
    public Page<LeadActivity> returnAllLeadActivity(Pageable pageable) {
        return laService.fetchAll(pageable);
    }

    @GetMapping("/{id}")
    public LeadActivity findLeadActivityByID(@PathVariable long id) throws Exception {
        return laService.getSingleLeadActivity(id);
    }

    @GetMapping("/allowedactivitytype/{id}")
    public List<ActivityTypeEnum> getValidActivityType(@PathVariable long id) throws Exception {
        return laService.getAllowedActiviType(id);
    }

    @GetMapping("/deallostreasons")
    public List<DealLostReasonEnum> getDealLostReasons() throws Exception {
        return laService.getDealLostReasons();
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @SneakyThrows(InvalidFormatException.class)
    public LeadActivity createLeadActivity(@RequestBody LeadActivityCreate at) throws Exception {

        return laService.createLeadActivity(at);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void closeLeadActivityWithComment(@RequestBody LeadActivityClosingComment payload, @PathVariable long id)
            throws Exception {
        if (payload.getClosingComment() == null)
            throw new Exception("Please enter closing comments");

        laService.deleteLeadActivity(id, payload.getClosingComment(), userDetailsService.getCurrentUser().getId(),
                false, "non-system",payload.getMoveToNegotiation());
    }

    @PutMapping("reschedule/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void rescheduleActivity(@RequestBody RescheduleActivityData payload, @PathVariable long id) throws Exception {
        laService.rescheduleActivity(id, payload);
    }

    @PutMapping("revert/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void revertLeadActivity(@PathVariable long id) throws Exception {
        laService.revertLeadActivity(id);
    }

    @PostMapping("/getleadactivitypage")
    @ResponseStatus(HttpStatus.OK)
    public LeadActivityListWithTypeAheadData getLeadActivityPage(@RequestBody FilterDataList leadFilterDataList,
                                                                 @PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable)
            throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();
        request.setAttribute("currentUser", currentUser);
        return laService.getLeadActivityPage(leadFilterDataList, pageable);
    }

    @PostMapping("/getleadactivitypage/export")
    @ResponseStatus(HttpStatus.OK)
    public Page<LeadActivityExportDTO> getLeadActivityPageExport(@RequestBody FilterDataList leadFilterDataList,
                                                                 @PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable)
            throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();
        request.setAttribute("currentUser", currentUser);
        return laService.getLeadActivityPageExport(leadFilterDataList, pageable);
    }

    @GetMapping("/getleadactivitypage/dropdown")
    public LeadActivityDropdownData getDropDownValues() throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();
        request.setAttribute("currentUser", currentUser);
        return laService.getDropdownForLead();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) ->
        {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = "Invalid Data. Please try again.";
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    @GetMapping("/validActivityTypes")
    public List<String> findValidActivityTypes() {
        return ActivityTypeEnum.getValidActivityTypes();
    }

    @GetMapping("/tn")
    public void triggerNotifications() {
        sendCRMNotificationsService.sendSMSNotificationForUpcomingActivities();
    }
}

package com.ec.application.controller;

import java.util.List;

import com.ec.application.aspects.AllowOnly;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.constants.RoleConstants;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.ContactExportDAO;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.model.Contact;
import com.ec.application.service.ContactService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/contact")
public class ContactController {

    @Autowired
    ContactService contactInfoService;

    @Autowired
    SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(ContactController.class);

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER, RoleConstants.STORE_INCHARGE})
    public Contact createCategory(@RequestBody Contact payload) throws Exception {
        return contactInfoService.createContact(payload);
    }

    @GetMapping("/{id}")
    public Contact findContactbyvehicleNoContacts(@PathVariable long id) throws Exception {
        return contactInfoService.findContactById(id);
    }

    @GetMapping("typeahead/globalsearch/{nameorno}")
    public List<String> findTypeAhead(@PathVariable String nameorno) throws Exception {
        return contactInfoService.typeAheadForSearch(nameorno);
    }

    @GetMapping("typeahead/namesearch/{name}")
    public List<String> findTypeAheadForName(@PathVariable String name) throws Exception {
        return contactInfoService.typeAheadForName(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<Contact> returnFilteredContacts(@RequestBody FilterDataList contactFilterDataList,
                                                @PageableDefault(page = 0, size = 10, sort = "contactId", direction = Direction.DESC) Pageable pageable) {
        return contactInfoService.findFilteredContactsWithTA(contactFilterDataList, pageable);
    }

    @ExceptionHandler(
            {JpaSystemException.class})

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }

    @PostMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<StreamingResponseBody> exportContactsExcel(@RequestBody(required = false) FilterDataList filterDataList) {
        String tenant = schemaConfig.getMasterSchema();
        StreamingResponseBody stream = outputStream -> {
            try {
                ThreadLocalStorage.setTenantName(tenant);
                contactInfoService.streamContactExcel(filterDataList, outputStream);
            } catch (Exception e) {
                log.error("Contact Excel export failed", e);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contacts-export.xlsx")
                .body(stream);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER, RoleConstants.STORE_INCHARGE})
    public ResponseEntity<?> deleteContact(@PathVariable Long id) throws Exception {
        contactInfoService.deleteContact(id);
        return ResponseEntity.ok("Contact Deleted sucessfully.");
    }

    @PutMapping("/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER, RoleConstants.STORE_INCHARGE})
    public Contact updateContact(@PathVariable Long id, @RequestBody Contact payload) throws Exception {
        return contactInfoService.updateContact(id, payload);
    }

}

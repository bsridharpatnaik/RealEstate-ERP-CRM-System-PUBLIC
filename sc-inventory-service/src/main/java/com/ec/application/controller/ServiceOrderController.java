package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.AllowOnly;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.CancelServiceOrderLineRequest;
import com.ec.application.data.CancelServiceOrderRequest;
import com.ec.application.data.CompleteServiceOrderLineRequest;
import com.ec.application.data.CreateServiceOrderRequest;
import com.ec.application.data.MarkCompleteServiceOrderRequest;
import com.ec.application.data.ReturnServiceOrderData;
import com.ec.application.data.UpdateServiceOrderRequest;
import com.ec.application.model.ServiceOrder;
import com.ec.application.service.ServiceOrderPdfService;
import com.ec.application.service.ServiceOrderService;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/service-order")
@RequiredArgsConstructor
@UseDefaultTenant
public class ServiceOrderController {

    private final Logger log = LoggerFactory.getLogger(ServiceOrderController.class);
    private final ServiceOrderService serviceOrderService;
    private final ServiceOrderPdfService serviceOrderPdfService;

    @PostMapping("/create")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOrder createServiceOrder(@RequestBody CreateServiceOrderRequest payload) throws Exception {
        return serviceOrderService.createServiceOrder(payload);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnServiceOrderData fetchServiceOrdersPage(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable
    ) throws Exception {
        return serviceOrderService.fetchServiceOrdersPage(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public ServiceOrder findServiceOrderById(@PathVariable String id) throws Exception {
        return serviceOrderService.getServiceOrderWithInit(id);
    }

    /** Distinct descriptions previously used on any service line — powers the "select existing or create new" autocomplete. */
    @GetMapping("/line-descriptions")
    public java.util.List<String> getDistinctLineDescriptions() {
        return serviceOrderService.getDistinctLineDescriptions();
    }

    /** Distinct custom-field labels previously used on any service line — powers the key picker. */
    @GetMapping("/custom-field-labels")
    public java.util.List<String> getDistinctCustomFieldLabels() {
        return serviceOrderService.getDistinctCustomFieldLabels();
    }

    /** Next-service-date reminder bucket counts (overdue / due in 7 / 30 / 90 days). */
    @GetMapping("/tiles")
    public com.ec.application.data.ServiceOrderTilesDTO getTiles() {
        return serviceOrderService.getTiles();
    }

    @PutMapping("/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder updateServiceOrder(@PathVariable String id, @RequestBody UpdateServiceOrderRequest payload) throws Exception {
        return serviceOrderService.updateServiceOrder(id, payload);
    }

    @PostMapping("/{id}/complete")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder markComplete(@PathVariable String id, @RequestBody(required = false) MarkCompleteServiceOrderRequest payload) throws Exception {
        return serviceOrderService.markComplete(id, payload);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> printServiceOrderAsPdf(@PathVariable String id) {
        ServiceOrder so;
        try {
            so = serviceOrderService.getServiceOrderWithInit(id);
        } catch (Exception e) {
            log.error("Failed to fetch SO for PDF: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String filename = so.getServiceOrderId() + ".pdf";
        StreamingResponseBody stream = outputStream -> {
            try {
                serviceOrderPdfService.generatePdf(so, outputStream);
                outputStream.flush();
            } catch (DocumentException e) {
                log.error("iText PDF generation failed for SO {}", id, e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    @PostMapping("/{id}/cancel")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder cancelServiceOrder(@PathVariable String id, @RequestBody(required = false) CancelServiceOrderRequest payload) throws Exception {
        return serviceOrderService.cancelServiceOrder(id, payload);
    }

    @PostMapping("/{id}/line/{lineId}/complete")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder completeLine(@PathVariable String id, @PathVariable Long lineId,
                                     @RequestBody(required = false) CompleteServiceOrderLineRequest payload) throws Exception {
        return serviceOrderService.completeLine(id, lineId, payload);
    }

    @PostMapping("/{id}/line/{lineId}/cancel")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder cancelLine(@PathVariable String id, @PathVariable Long lineId,
                                   @RequestBody(required = false) CancelServiceOrderLineRequest payload) throws Exception {
        return serviceOrderService.cancelLine(id, lineId, payload);
    }

    @PostMapping("/{id}/line/{lineId}/reopen")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ServiceOrder reopenLine(@PathVariable String id, @PathVariable Long lineId) throws Exception {
        return serviceOrderService.reopenLine(id, lineId);
    }
}

package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.ServiceOrderSpecification;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.ServiceOrderStatusConstants;
import com.ec.application.data.*;
import com.ec.application.model.ServiceOrder;
import com.ec.application.model.ServiceOrderLine;
import com.ec.application.model.ServiceOrderLineCustomField;
import com.ec.application.repository.ServiceOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class ServiceOrderService {

    private final ServiceOrderRepo serviceOrderRepo;
    private final FirmService firmService;
    private final SupplierService supplierService;
    private final UserDetailsService userDetailsService;
    private final ActivityLogService activityLogService;

    @Transactional
    public ServiceOrder createServiceOrder(CreateServiceOrderRequest request) throws Exception {
        if (request.getLineItems() == null || request.getLineItems().isEmpty()) {
            throw new Exception("At least one service line is required.");
        }
        if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
            throw new Exception("Project is a mandatory field");
        }

        ServiceOrder so = new ServiceOrder();
        so.setServiceDate(request.getServiceDate());
        so.setVendor(supplierService.findSingleSupplier(request.getVendorId()));
        if (request.getFirmId() != null) {
            so.setFirm(firmService.findSingleFirm(request.getFirmId()));
        }
        so.setSubject(request.getSubject());
        so.setNotes(request.getNotes());
        so.setGrandTotal(request.getGrandTotal());
        so.setOverridePhoneNumber(request.getOverridePhoneNumber());
        so.setOverrideEmail(request.getOverrideEmail());
        so.setProjectName(request.getProjectName());
        so.setSpecialDiscount(request.getSpecialDiscount());
        // nextServiceDate is intentionally not set at creation — the service hasn't happened yet,
        // so it's asked for when marking the order COMPLETED instead (see markComplete()).
        so.setStatus(ServiceOrderStatusConstants.STATUS_NEW);
        so.setLastStatusUpdatedAt(new Date());

        for (CreateServiceOrderLineRequest lineReq : request.getLineItems()) {
            so.getLines().add(buildLine(so, lineReq));
        }

        if (request.getFileInformations() != null) {
            so.setFileInformations(ReusableMethods.convertFilesListToSet(request.getFileInformations()));
        }

        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("CREATED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Service Order " + saved.getServiceOrderId() + " created by " + activityUser, activityUser);
        return saved;
    }

    private ServiceOrderLine buildLine(ServiceOrder so, CreateServiceOrderLineRequest req) throws Exception {
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new Exception("Description is required for every service line.");
        }
        ServiceOrderLine line = new ServiceOrderLine();
        line.setServiceOrder(so);
        line.setStatus(ServiceOrderStatusConstants.STATUS_NEW);
        line.setDescription(req.getDescription());
        line.setQuantity(req.getQuantity());
        line.setRate(req.getRate());
        line.setDiscountPercent(req.getDiscountPercent());
        line.setGstPercent(req.getGstPercent());
        line.setNetRate(req.getNetRate());
        line.setTotalAmount(req.getTotalAmount());
        line.setServiceType(req.getServiceType());
        line.setAssetTag(req.getAssetTag());
        applyCustomFields(line, req.getCustomFields());
        return line;
    }

    /** Replaces a line's custom fields wholesale with the supplied list — skips blank labels. */
    private void applyCustomFields(ServiceOrderLine line, List<CustomFieldRequest> customFields) {
        line.getCustomFields().clear();
        if (customFields == null) return;
        for (CustomFieldRequest cf : customFields) {
            if (cf.getLabel() == null || cf.getLabel().trim().isEmpty()) continue;
            ServiceOrderLineCustomField entity = new ServiceOrderLineCustomField();
            entity.setServiceOrderLine(line);
            entity.setFieldLabel(cf.getLabel());
            entity.setFieldValue(cf.getValue());
            line.getCustomFields().add(entity);
        }
    }

    @Transactional
    public ServiceOrder updateServiceOrder(String id, UpdateServiceOrderRequest request) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));

        boolean isAdmin = userDetailsService.getCurrentUser().getRoles().stream()
                .anyMatch(r -> r.toLowerCase().contains("admin"));
        if (!isAdmin && !ServiceOrderStatusConstants.STATUS_NEW.equals(so.getStatus())) {
            throw new Exception("Service Order cannot be edited. Only orders in NEW status can be edited.");
        }

        if (request.getServiceDate() != null) so.setServiceDate(request.getServiceDate());
        if (request.getVendorId() != null) so.setVendor(supplierService.findSingleSupplier(request.getVendorId()));
        if (request.getFirmId() != null) so.setFirm(firmService.findSingleFirm(request.getFirmId()));
        so.setSubject(request.getSubject());
        so.setNotes(request.getNotes());
        so.setOverridePhoneNumber(request.getOverridePhoneNumber());
        so.setOverrideEmail(request.getOverrideEmail());
        so.setProjectName(request.getProjectName());
        so.setGrandTotal(request.getGrandTotal());
        so.setSpecialDiscount(request.getSpecialDiscount());

        if (request.getFileInformations() != null) {
            so.setFileInformations(ReusableMethods.convertFilesListToSet(request.getFileInformations()));
        }

        if (request.getLineUpdates() != null) {
            Map<Long, UpdateServiceOrderLineRequest> updateMap = request.getLineUpdates().stream()
                    .filter(u -> u.getLineId() != null)
                    .collect(Collectors.toMap(UpdateServiceOrderLineRequest::getLineId, u -> u));
            for (ServiceOrderLine line : so.getLines()) {
                UpdateServiceOrderLineRequest update = updateMap.get(line.getId());
                if (update != null) {
                    line.setDescription(update.getDescription());
                    line.setQuantity(update.getQuantity());
                    line.setRate(update.getRate());
                    line.setDiscountPercent(update.getDiscountPercent());
                    line.setGstPercent(update.getGstPercent());
                    line.setNetRate(update.getNetRate());
                    line.setTotalAmount(update.getTotalAmount());
                    line.setServiceType(update.getServiceType());
                    line.setAssetTag(update.getAssetTag());
                    applyCustomFields(line, update.getCustomFields());
                }
            }
        }

        if (request.getRemovedLineIds() != null && !request.getRemovedLineIds().isEmpty()) {
            so.getLines().removeIf(line -> request.getRemovedLineIds().contains(line.getId()));
        }

        if (request.getNewLines() != null) {
            for (CreateServiceOrderLineRequest newLineReq : request.getNewLines()) {
                so.getLines().add(buildLine(so, newLineReq));
            }
        }

        if (so.getLines().isEmpty()) {
            throw new Exception("A Service Order must have at least one line.");
        }

        // Adding/removing lines can change the derived header status (e.g. a new NEW line on an
        // otherwise-completed order → PARTIALLY_COMPLETED).
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("UPDATED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Service Order " + saved.getServiceOrderId() + " updated by " + activityUser, activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    @Transactional
    // Whole-order complete: completes every still-open (NEW) line, applying per-line warranty from
    // the request. Header status is then derived from the lines.
    public ServiceOrder markComplete(String id, MarkCompleteServiceOrderRequest request) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        if (ServiceOrderStatusConstants.getTerminalStatuses().contains(so.getStatus())) {
            throw new Exception("Service Order is already in a terminal status: " + so.getStatus());
        }
        Map<Long, LineWarrantyRequest> lineMap = (request != null && request.getLineWarranties() != null)
                ? request.getLineWarranties().stream()
                    .filter(w -> w.getLineId() != null)
                    .collect(Collectors.toMap(LineWarrantyRequest::getLineId, w -> w, (a, b) -> a))
                : new HashMap<>();
        for (ServiceOrderLine line : so.getLines()) {
            if (ServiceOrderStatusConstants.isLineTerminal(line.getStatus())) continue; // leave already-decided lines
            line.setStatus(ServiceOrderStatusConstants.STATUS_COMPLETED);
            LineWarrantyRequest lw = lineMap.get(line.getId());
            if (lw != null) {
                if (lw.getWarrantyTill() != null) line.setWarrantyTill(lw.getWarrantyTill());
                if (lw.getNextServiceDate() != null) line.setNextServiceDate(lw.getNextServiceDate());
            }
        }
        refreshNextServiceDate(so);
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("COMPLETED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Service Order " + saved.getServiceOrderId() + " marked complete by " + activityUser, activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    // Whole-order cancel: cancels every still-open (NEW) line; already-completed lines stand.
    // Header is derived — all-cancelled → CANCELLED, some completed → COMPLETED.
    @Transactional
    public ServiceOrder cancelServiceOrder(String id, CancelServiceOrderRequest request) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        if (ServiceOrderStatusConstants.getTerminalStatuses().contains(so.getStatus())) {
            throw new Exception("Service Order is already in a terminal status: " + so.getStatus());
        }
        String reason = request != null ? request.getReason() : null;
        for (ServiceOrderLine line : so.getLines()) {
            if (ServiceOrderStatusConstants.isLineTerminal(line.getStatus())) continue;
            line.setStatus(ServiceOrderStatusConstants.STATUS_CANCELLED);
            line.setCancelReason(reason);
        }
        so.setCancelReason(reason);
        refreshNextServiceDate(so);
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("CANCELLED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Service Order " + saved.getServiceOrderId() + " cancelled by " + activityUser, activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    // ─── Per-line actions ──────────────────────────────────────────────────────

    @Transactional
    public ServiceOrder completeLine(String id, Long lineId, CompleteServiceOrderLineRequest request) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        ServiceOrderLine line = findLine(so, lineId);
        if (ServiceOrderStatusConstants.isLineTerminal(line.getStatus())) {
            throw new Exception("Line is already " + line.getStatus() + " and cannot be completed.");
        }
        line.setStatus(ServiceOrderStatusConstants.STATUS_COMPLETED);
        if (request != null) {
            if (request.getWarrantyTill() != null) line.setWarrantyTill(request.getWarrantyTill());
            if (request.getNextServiceDate() != null) line.setNextServiceDate(request.getNextServiceDate());
        }
        refreshNextServiceDate(so);
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("LINE_COMPLETED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Line \"" + safeDesc(line) + "\" completed in " + saved.getServiceOrderId() + " by " + activityUser,
                activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    @Transactional
    public ServiceOrder cancelLine(String id, Long lineId, CancelServiceOrderLineRequest request) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        ServiceOrderLine line = findLine(so, lineId);
        if (ServiceOrderStatusConstants.isLineTerminal(line.getStatus())) {
            throw new Exception("Line is already " + line.getStatus() + " and cannot be cancelled.");
        }
        line.setStatus(ServiceOrderStatusConstants.STATUS_CANCELLED);
        line.setCancelReason(request != null ? request.getReason() : null);
        refreshNextServiceDate(so); // dropping a line may remove the earliest next-service date
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("LINE_CANCELLED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Line \"" + safeDesc(line) + "\" cancelled in " + saved.getServiceOrderId() + " by " + activityUser,
                activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    // Undo a completed/cancelled line back to NEW (mis-click recovery). Completion-time data
    // (warranty / next-service) is cleared since the line is no longer done. Header re-derives —
    // reopening a line on a COMPLETED/CANCELLED order flips it back to PARTIALLY_COMPLETED / NEW.
    @Transactional
    public ServiceOrder reopenLine(String id, Long lineId) throws Exception {
        ServiceOrder so = serviceOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        ServiceOrderLine line = findLine(so, lineId);
        if (!ServiceOrderStatusConstants.isLineTerminal(line.getStatus())) {
            throw new Exception("Line is not completed or cancelled — nothing to reopen.");
        }
        line.setStatus(ServiceOrderStatusConstants.STATUS_NEW);
        line.setCancelReason(null);
        line.setWarrantyTill(null);
        line.setNextServiceDate(null);
        refreshNextServiceDate(so);
        recomputeHeaderStatus(so);
        ServiceOrder saved = serviceOrderRepo.save(so);
        String activityUser = resolveCurrentUser();
        activityLogService.record("LINE_REOPENED", "SERVICE_ORDER", saved.getServiceOrderId(),
                "Line \"" + safeDesc(line) + "\" reopened in " + saved.getServiceOrderId() + " by " + activityUser,
                activityUser);
        return getServiceOrderWithInit(saved.getServiceOrderId());
    }

    private ServiceOrderLine findLine(ServiceOrder so, Long lineId) throws Exception {
        return so.getLines().stream()
                .filter(l -> l.getId() != null && l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new Exception("Line " + lineId + " not found on service order " + so.getServiceOrderId()));
    }

    private String safeDesc(ServiceOrderLine line) {
        String d = line.getDescription();
        return d == null ? "" : (d.length() > 60 ? d.substring(0, 60) + "…" : d);
    }

    // Derives the header status from the lines (PO/Indent style):
    //   all NEW → NEW · all CANCELLED → CANCELLED · all terminal with ≥1 COMPLETED → COMPLETED
    //   · any mix with some NEW remaining → PARTIALLY_COMPLETED
    private void recomputeHeaderStatus(ServiceOrder so) {
        List<ServiceOrderLine> lines = so.getLines();
        int total = lines.size();
        long newCount = lines.stream().filter(l -> ServiceOrderStatusConstants.STATUS_NEW.equals(l.getStatus())).count();
        long cancelledCount = lines.stream().filter(l -> ServiceOrderStatusConstants.STATUS_CANCELLED.equals(l.getStatus())).count();

        String status;
        if (total == 0 || newCount == total) {
            status = ServiceOrderStatusConstants.STATUS_NEW;
        } else if (cancelledCount == total) {
            status = ServiceOrderStatusConstants.STATUS_CANCELLED;
        } else if (newCount == 0) {
            // all lines terminal, not all cancelled → at least one completed
            status = ServiceOrderStatusConstants.STATUS_COMPLETED;
        } else {
            status = ServiceOrderStatusConstants.STATUS_PARTIALLY_COMPLETED;
        }
        so.setStatus(status);
        so.setLastStatusUpdatedAt(new Date());
    }

    // SO-level nextServiceDate = earliest next-service date across NON-cancelled lines (drives the
    // list "due/overdue" tiles). Authoritative: clears to null when no active line has one, so a
    // cancelled line no longer leaves a stale date behind.
    private void refreshNextServiceDate(ServiceOrder so) {
        Date earliest = so.getLines().stream()
                .filter(l -> !ServiceOrderStatusConstants.STATUS_CANCELLED.equals(l.getStatus()))
                .map(ServiceOrderLine::getNextServiceDate)
                .filter(d -> d != null)
                .min(java.util.Comparator.naturalOrder())
                .orElse(null);
        so.setNextServiceDate(earliest);
    }

    @Transactional(readOnly = true)
    public ReturnServiceOrderData fetchServiceOrdersPage(FilterDataList filterDataList, Pageable pageable) throws Exception {
        ReturnServiceOrderData returnData = new ReturnServiceOrderData();
        Specification<ServiceOrder> spec = ServiceOrderSpecification.getSpecification(filterDataList);
        if (spec == null) spec = Specification.where(null);
        Page<ServiceOrder> page = serviceOrderRepo.findAll(spec, pageable);
        initializeLazyAssociations(page.getContent());
        returnData.setServiceOrders(page);
        return returnData;
    }

    /** findAll(spec, pageable) doesn't eager-fetch associations; touch them here while the
     *  session is still open so JSON serialization (which happens after the transaction ends)
     *  doesn't hit a LazyInitializationException. */
    private void initializeLazyAssociations(List<ServiceOrder> serviceOrders) {
        for (ServiceOrder so : serviceOrders) {
            if (so.getVendor() != null) {
                so.getVendor().getName();
            }
            if (so.getFirm() != null) {
                so.getFirm().getFirmName();
            }
            if (so.getLines() != null) {
                so.getLines().size();
                for (ServiceOrderLine line : so.getLines()) {
                    if (line.getCustomFields() != null) {
                        line.getCustomFields().size();
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctLineDescriptions() {
        return serviceOrderRepo.findDistinctLineDescriptions();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctCustomFieldLabels() {
        return serviceOrderRepo.findDistinctCustomFieldLabels();
    }

    @Transactional(readOnly = true)
    public ServiceOrder getServiceOrderWithInit(String id) throws Exception {
        ServiceOrder so = serviceOrderRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new Exception("Service Order not found: " + id));
        for (ServiceOrderLine line : so.getLines()) {
            line.getCustomFields().size();
        }
        return so;
    }

    @Transactional(readOnly = true)
    public ServiceOrderTilesDTO getTiles() {
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date today = todayCal.getTime();

        Date plus7 = addDays(today, 7);
        Date plus30 = addDays(today, 30);
        Date plus90 = addDays(today, 90);

        ServiceOrderTilesDTO dto = new ServiceOrderTilesDTO();
        dto.setOverdueCount(serviceOrderRepo.countNextServiceOverdue(today));
        dto.setNext7DaysCount(serviceOrderRepo.countNextServiceBetween(today, plus7));
        dto.setNext30DaysCount(serviceOrderRepo.countNextServiceBetween(today, plus30));
        dto.setNext90DaysCount(serviceOrderRepo.countNextServiceBetween(today, plus90));
        return dto;
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    private String resolveCurrentUser() {
        try {
            return userDetailsService.getCurrentUser().getUsername();
        } catch (Exception e) {
            return "System";
        }
    }
}

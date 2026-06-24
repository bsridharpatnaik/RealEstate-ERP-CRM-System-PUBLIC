package com.ec.application.service;

import com.ec.application.Filters.QuoteComparisonSpecification;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@UseDefaultTenant
@Transactional
public class QuoteComparisonService {

    private final QuoteComparisonRepository qcRepo;
    private final QuoteComparisonLineRepository qcLineRepo;
    private final SupplierQuoteRepository sqRepo;
    private final SupplierQuoteLineRepository sqlRepo;
    private final QuoteToPoRefRepository qtpRepo;
    private final ActivityLogService activityLogService;
    private final UserDetailsService userDetailsService;
    private final IndentInventoryListRepo indentInventoryListRepo;

    // ─── Create ───────────────────────────────────────────────────────────────

    public QuoteComparison create(QuoteComparisonCreateRequest req) {
        validateIndentLinesNotAlreadyQuoted(req.getLines());

        String user = resolveCurrentUser();

        QuoteComparison qc = new QuoteComparison();
        qc.setTitle(req.getTitle());
        qc.setNotes(req.getNotes());
        qc.setProject(req.getProject());
        qc.setComparisonDate(req.getComparisonDate() != null ? req.getComparisonDate() : new Date());
        qc.setStatus("DRAFT");
        qc.setCreatedByUser(user);
        qc.setIndentIds(req.getIndentIds() != null ? req.getIndentIds() : new ArrayList<>());

        // Add criteria
        int order = 0;
        for (QuoteComparisonCreateRequest.CriteriaRequest cr : safeList(req.getCriteria())) {
            ComparisonCriteria c = new ComparisonCriteria();
            c.setQuoteComparison(qc);
            c.setCriteriaName(cr.getCriteriaName());
            c.setCriteriaType(cr.getCriteriaType() != null ? cr.getCriteriaType() : "TEXT");
            c.setCriteriaScope(cr.getCriteriaScope() != null ? cr.getCriteriaScope() : "LINE");
            c.setIsMandatory(cr.getIsMandatory() != null && cr.getIsMandatory());
            c.setDisplayOrder(cr.getDisplayOrder() != null ? cr.getDisplayOrder() : order);
            qc.getCriteria().add(c);
            order++;
        }

        // Add lines
        for (QuoteComparisonCreateRequest.LineRequest lr : safeList(req.getLines())) {
            QuoteComparisonLine line = new QuoteComparisonLine();
            line.setQuoteComparison(qc);
            line.setIndentId(lr.getIndentId());
            line.setIndentLineId(lr.getIndentLineId());
            line.setProductId(lr.getProductId());
            line.setProductName(lr.getProductName());
            line.setUnit(lr.getUnit());
            line.setRequiredQty(lr.getRequiredQty());
            line.setSpecifications(lr.getSpecifications());
            line.setNeedByDate(lr.getNeedByDate());
            line.setLineStatus("OPEN");
            qc.getLines().add(line);
        }

        QuoteComparison saved = qcRepo.save(qc);
        markIndentLinesQuoteRequested(saved);

        activityLogService.record("CREATED", "QUOTE_COMPARISON", saved.getQcId(),
                buildDescription("Quote comparison " + saved.getQcId() + " created by " + user, null),
                user);

        return saved;
    }

    // An indent line can only be part of ONE active quote comparison at a time — if it's already
    // marked as quote-requested by a comparison that isn't CANCELLED, block creation outright
    // rather than silently overwriting the marker (which would orphan the earlier RFQ).
    private void validateIndentLinesNotAlreadyQuoted(List<QuoteComparisonCreateRequest.LineRequest> lines) {
        for (QuoteComparisonCreateRequest.LineRequest lr : safeList(lines)) {
            String code = lr.getIndentLineId();
            if (code == null || code.trim().isEmpty()) continue;
            for (IndentInventoryList entry : indentInventoryListRepo.findByLineItemCode(code)) {
                String existingQcId = entry.getQuoteRequestedQcId();
                if (existingQcId == null) continue;
                QuoteComparison existingQc = qcRepo.findById(existingQcId).orElse(null);
                if (existingQc != null && !"CANCELLED".equals(existingQc.getStatus())) {
                    throw new RuntimeException(
                            (lr.getProductName() != null ? lr.getProductName() : code) +
                            " (" + code + ") is already part of quote comparison " + existingQcId +
                            " (" + existingQc.getStatus() + "). Cancel that comparison before requesting a new quote for this line.");
                }
            }
        }
    }

    // Marks the originating indent line(s) as "quote requested" — additive only, never cleared
    // here, so it stays visible on the indent regardless of what the PO/inward pipeline later
    // does to lineItemStatus. Best-effort: a missing/renamed indent line shouldn't block QC creation.
    private void markIndentLinesQuoteRequested(QuoteComparison qc) {
        for (QuoteComparisonLine line : qc.getLines()) {
            String lineItemCode = line.getIndentLineId();
            if (lineItemCode == null || lineItemCode.trim().isEmpty()) continue;
            try {
                List<IndentInventoryList> matches = indentInventoryListRepo.findByLineItemCode(lineItemCode);
                for (IndentInventoryList entry : matches) {
                    entry.setQuoteRequestedQcId(qc.getQcId());
                    indentInventoryListRepo.save(entry);
                }
            } catch (Exception e) {
                log.warn("Could not mark indent line {} as quote-requested for {}: {}", lineItemCode, qc.getQcId(), e.getMessage());
            }
        }
    }

    // ─── Get detail ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(String qcId) {
        QuoteComparison qc = getOrThrow(qcId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("header", qc);
        result.put("lines", qcLineRepo.findByQuoteComparison_QcId(qcId));
        result.put("supplierQuotes", sqRepo.findByQuoteComparison_QcId(qcId));
        result.put("matrix", buildMatrix(qcId));
        result.put("supplierSummary", buildSupplierSummary(qcId));
        return result;
    }

    // ─── Lines for RFQ PDF ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public QuoteComparison getForRfqPdf(String qcId) {
        return getOrThrow(qcId);
    }

    @Transactional(readOnly = true)
    public List<QuoteComparisonLine> getLinesForRfqPdf(String qcId) {
        return qcLineRepo.findByQuoteComparison_QcId(qcId);
    }

    // ─── List ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<QuoteComparison> list(QuoteComparisonFilter filter, int page, int size) {
        Specification<QuoteComparison> spec = QuoteComparisonSpecification.getSpec(filter);
        return qcRepo.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "comparisonDate")));
    }

    // ─── Add supplier quote ──────────────────────────────────────────────────

    public SupplierQuote addSupplierQuote(String qcId, SupplierQuoteRequest req) {
        QuoteComparison qc = getOrThrow(qcId);
        String user = resolveCurrentUser();

        SupplierQuote sq = new SupplierQuote();
        sq.setQuoteComparison(qc);
        if (req.getRevisionLabel() == null || req.getRevisionLabel().trim().isEmpty()) {
            req.setRevisionLabel(nextRevisionLabel(qcId, req.getSupplierName()));
        }
        mapSupplierQuoteFields(sq, req, user);

        SupplierQuote saved = sqRepo.save(sq);

        // Move to OPEN after first supplier quote
        if ("DRAFT".equals(qc.getStatus())) {
            qc.setStatus("OPEN");
            qcRepo.save(qc);
        }

        activityLogService.record("SUPPLIER_QUOTE_ADDED", "QUOTE_COMPARISON", qcId,
                buildDescription("Supplier quote from " + req.getSupplierName() + " added to " + qcId + " by " + user, null),
                user);

        return saved;
    }

    // ─── Update supplier quote ────────────────────────────────────────────────

    public SupplierQuote updateSupplierQuote(String qcId, Long sqId, SupplierQuoteRequest req) {
        SupplierQuote sq = sqRepo.findById(sqId)
                .orElseThrow(() -> new RuntimeException("Supplier quote not found: " + sqId));

        // The edit path deletes and recreates every SupplierQuoteLine with new IDs. If any of
        // this quote's CURRENT lines is the winning reference for a finalized demand line,
        // editing would silently orphan that reference (the matrix/Create PO would lose track
        // of the award). Block the edit and point the user at "Reopen" first instead.
        Set<Long> thisQuotesLineIds = sq.getLines().stream().map(SupplierQuoteLine::getId).collect(Collectors.toSet());
        List<QuoteComparisonLine> qcLines = qcLineRepo.findByQuoteComparison_QcId(qcId);
        List<String> blockedProducts = qcLines.stream()
                .filter(l -> l.getFinalizedSupplierQuoteLineId() != null && thisQuotesLineIds.contains(l.getFinalizedSupplierQuoteLineId()))
                .map(QuoteComparisonLine::getProductName)
                .collect(Collectors.toList());
        if (!blockedProducts.isEmpty()) {
            throw new RuntimeException("Cannot edit this quote — it's already the winning quote for: " +
                    String.join(", ", blockedProducts) + ". Reopen those line(s) first.");
        }

        String user = resolveCurrentUser();

        // Soft-delete existing lines and header criteria values before replacing
        sq.getLines().forEach(l -> l.setDeleted(true));
        sq.getLines().clear();
        sq.getHeaderCriteriaValues().forEach(v -> v.setDeleted(true));
        sq.getHeaderCriteriaValues().clear();
        mapSupplierQuoteFields(sq, req, user);

        SupplierQuote saved = sqRepo.save(sq);

        activityLogService.record("SUPPLIER_QUOTE_UPDATED", "QUOTE_COMPARISON", qcId,
                buildDescription("Supplier quote from " + req.getSupplierName() + " updated in " + qcId + " by " + user, null),
                user);

        return saved;
    }

    // ─── Delete supplier quote ────────────────────────────────────────────────

    public void deleteSupplierQuote(String qcId, Long sqId) {
        SupplierQuote sq = sqRepo.findById(sqId)
                .orElseThrow(() -> new RuntimeException("Supplier quote not found: " + sqId));
        String user = resolveCurrentUser();
        sq.setDeleted(true);
        sqRepo.save(sq);

        activityLogService.record("SUPPLIER_QUOTE_REMOVED", "QUOTE_COMPARISON", qcId,
                buildDescription("Supplier quote " + sqId + " removed from " + qcId + " by " + user, null),
                user);

        recalcStatus(qcId);
    }

    // ─── Finalize line ────────────────────────────────────────────────────────

    public QuoteComparisonLine finalizeLine(String qcId, FinalizeLineRequest req) {
        QuoteComparison qc = getOrThrow(qcId);
        if ("CLOSED".equals(qc.getStatus()) || "CANCELLED".equals(qc.getStatus())) {
            throw new RuntimeException("Cannot finalize lines on a " + qc.getStatus() + " comparison");
        }

        QuoteComparisonLine line = qcLineRepo.findById(req.getQcLineId())
                .orElseThrow(() -> new RuntimeException("Line not found: " + req.getQcLineId()));

        SupplierQuoteLine winnerLine = sqlRepo.findById(req.getSupplierQuoteLineId())
                .orElseThrow(() -> new RuntimeException("Supplier quote line not found: " + req.getSupplierQuoteLineId()));

        boolean isNonLowest = isNonLowestSelection(line.getId(), winnerLine);
        if (isNonLowest && (req.getRemarks() == null || req.getRemarks().trim().isEmpty())) {
            throw new RuntimeException("Justification is mandatory when not selecting the lowest quoted rate");
        }

        String user = resolveCurrentUser();
        line.setLineStatus("FINALIZED");
        line.setFinalizedSupplierQuoteLineId(req.getSupplierQuoteLineId());
        line.setFinalizedBy(user);
        line.setFinalizedAt(new Date());
        line.setFinalizedRemarks(req.getRemarks());
        line.setIsNonLowestSelection(isNonLowest);

        QuoteComparisonLine saved = qcLineRepo.save(line);
        recalcStatus(qcId);

        activityLogService.record("LINE_FINALIZED", "QUOTE_COMPARISON", qcId,
                buildDescription("Line " + line.getProductName() + " finalized in " + qcId + " by " + user, null),
                user);

        return saved;
    }

    // ─── Reopen line ──────────────────────────────────────────────────────────

    public QuoteComparisonLine reopenLine(String qcId, Long lineId, String remarks) {
        QuoteComparisonLine line = qcLineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Line not found: " + lineId));

        if ("PO_LINKED".equals(line.getLineStatus())) {
            throw new RuntimeException("Cannot reopen a line that has a PO linked to it");
        }

        String user = resolveCurrentUser();
        line.setLineStatus("OPEN");
        line.setFinalizedSupplierQuoteLineId(null);
        line.setFinalizedBy(null);
        line.setFinalizedAt(null);
        line.setFinalizedRemarks(remarks);
        line.setIsNonLowestSelection(false);

        QuoteComparisonLine saved = qcLineRepo.save(line);
        recalcStatus(qcId);

        activityLogService.record("LINE_REOPENED", "QUOTE_COMPARISON", qcId,
                buildDescription("Line " + line.getProductName() + " reopened in " + qcId + " by " + user, null),
                user);

        return saved;
    }

    // ─── Close comparison ─────────────────────────────────────────────────────

    public void close(String qcId) {
        QuoteComparison qc = getOrThrow(qcId);
        String user = resolveCurrentUser();
        qc.setStatus("CLOSED");
        qcRepo.save(qc);

        activityLogService.record("CLOSED", "QUOTE_COMPARISON", qcId,
                buildDescription("Quote comparison " + qcId + " closed by " + user, null),
                user);
    }

    // ─── Reopen comparison ────────────────────────────────────────────────────
    // Only CLOSED comparisons can be reopened — CANCELLED is a deliberate dead end (and may
    // already be blocked from cancelling further work elsewhere). recalcStatus() normally
    // exits early for CLOSED/CANCELLED, so we flip the status first to let it recompute the
    // real status from the lines' actual state instead of guessing.
    public void reopen(String qcId) {
        QuoteComparison qc = getOrThrow(qcId);
        if (!"CLOSED".equals(qc.getStatus())) {
            throw new RuntimeException("Only a closed comparison can be reopened");
        }
        String user = resolveCurrentUser();
        qc.setStatus("OPEN");
        qcRepo.save(qc);
        recalcStatus(qcId);

        activityLogService.record("REOPENED", "QUOTE_COMPARISON", qcId,
                buildDescription("Quote comparison " + qcId + " reopened by " + user, null),
                user);
    }

    // ─── Cancel comparison ────────────────────────────────────────────────────

    public void cancel(String qcId) {
        QuoteComparison qc = getOrThrow(qcId);

        boolean hasPOLinked = qc.getLines().stream()
                .anyMatch(l -> "PO_LINKED".equals(l.getLineStatus()));
        if (hasPOLinked) {
            throw new RuntimeException("Cannot cancel: one or more lines are already linked to a Purchase Order");
        }

        String user = resolveCurrentUser();
        qc.setStatus("CANCELLED");
        qcRepo.save(qc);
        clearIndentLinesQuoteRequested(qc);

        activityLogService.record("CANCELLED", "QUOTE_COMPARISON", qcId,
                buildDescription("Quote comparison " + qcId + " cancelled by " + user, null),
                user);
    }

    // Cancelling withdraws the RFQ, so the "quote requested" marker on the indent line should
    // go with it — unlike Close, which just means "decided", not "withdrawn". Only clears the
    // flag when it still points at THIS comparison, so a newer QC that re-requested the same
    // line in the meantime doesn't get its marker erased by this one's cancellation.
    private void clearIndentLinesQuoteRequested(QuoteComparison qc) {
        for (QuoteComparisonLine line : qc.getLines()) {
            String lineItemCode = line.getIndentLineId();
            if (lineItemCode == null || lineItemCode.trim().isEmpty()) continue;
            try {
                List<IndentInventoryList> matches = indentInventoryListRepo.findByLineItemCode(lineItemCode);
                for (IndentInventoryList entry : matches) {
                    if (qc.getQcId().equals(entry.getQuoteRequestedQcId())) {
                        entry.setQuoteRequestedQcId(null);
                        indentInventoryListRepo.save(entry);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not clear quote-requested marker for indent line {} on cancel of {}: {}", lineItemCode, qc.getQcId(), e.getMessage());
            }
        }
    }

    // ─── Link to PO ──────────────────────────────────────────────────────────

    public QuoteToPoRef linkToPo(LinkQuoteToPoRequest req) {
        String user = resolveCurrentUser();

        // Guard against double-linking the same QC line (e.g. a double-click before the page
        // re-renders, or two tabs racing) — without this, a single demand line could end up
        // pointing at two different POs, which makes no sense once it's actually been ordered.
        QuoteComparisonLine existingLine = qcLineRepo.findById(req.getQcLineId()).orElse(null);
        if (existingLine != null && "PO_LINKED".equals(existingLine.getLineStatus())) {
            throw new RuntimeException("This line is already linked to a Purchase Order.");
        }

        // Remove any existing link for this PO line (skip when poLineId is null)
        if (req.getPoLineId() != null) {
            qtpRepo.findByPoLineId(req.getPoLineId()).ifPresent(qtpRepo::delete);
        }

        QuoteToPoRef ref = new QuoteToPoRef();
        ref.setSupplierQuoteLineId(req.getSupplierQuoteLineId());
        ref.setQcLineId(req.getQcLineId());
        ref.setQcId(req.getQcId());
        ref.setPurchaseOrderId(req.getPurchaseOrderId());
        ref.setPoLineId(req.getPoLineId());
        ref.setLinkedBy(user);
        ref.setLinkedAt(new Date());
        QuoteToPoRef saved = qtpRepo.save(ref);

        // Update line status
        qcLineRepo.findById(req.getQcLineId()).ifPresent(line -> {
            line.setLineStatus("PO_LINKED");
            qcLineRepo.save(line);
            recalcStatus(req.getQcId());
        });

        activityLogService.record("PO_LINKED", "QUOTE_COMPARISON", req.getQcId(),
                buildDescription("PO " + req.getPurchaseOrderId() + " linked to " + req.getQcId() + " by " + user, null),
                user);

        return saved;
    }

    // ─── Finalized quotes for PO dropdown ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFinalizedQuotesForPo(Long supplierId, List<String> indentIds) {
        List<SupplierQuoteLine> lines = sqlRepo.findFinalizedLinesForSupplierAndIndents(supplierId, indentIds);

        return lines.stream().map(sql -> {
            QuoteComparisonLine qcLine = qcLineRepo.findById(sql.getQcLineId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("supplierQuoteLineId", sql.getId());
            item.put("qcLineId", sql.getQcLineId());
            item.put("qcId", sql.getSupplierQuote().getQuoteComparison().getQcId());
            item.put("productId", qcLine != null ? qcLine.getProductId() : null);
            item.put("productName", qcLine != null ? qcLine.getProductName() : null);
            item.put("quotedRate", sql.getQuotedRate());
            item.put("discountPercent", sql.getDiscountPercent());
            item.put("gstPercent", sql.getGstPercent());
            item.put("landedCost", sql.getLandedCost());
            return item;
        }).collect(Collectors.toList());
    }

    // ─── Comparison matrix ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildMatrix(String qcId) {
        List<QuoteComparisonLine> lines = qcLineRepo.findByQuoteComparison_QcId(qcId);
        List<SupplierQuote> supplierQuotes = sqRepo.findByQuoteComparison_QcId(qcId);

        return lines.stream().map(line -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineId", line.getId());
            row.put("productId", line.getProductId());
            row.put("productName", line.getProductName());
            row.put("requiredQty", line.getRequiredQty());
            row.put("unit", line.getUnit());
            row.put("specifications", line.getSpecifications());
            row.put("lineStatus", line.getLineStatus());
            row.put("finalizedSupplierQuoteLineId", line.getFinalizedSupplierQuoteLineId());

            Double lowestRate = null;
            List<Map<String, Object>> supplierResponses = new ArrayList<>();

            for (SupplierQuote sq : supplierQuotes) {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("supplierQuoteId", sq.getId());
                resp.put("supplierId", sq.getSupplierId());
                resp.put("supplierName", sq.getSupplierName());
                resp.put("revisionLabel", sq.getRevisionLabel());

                Optional<SupplierQuoteLine> match = sq.getLines().stream()
                        .filter(l -> line.getId().equals(l.getQcLineId()))
                        .findFirst();

                if (match.isPresent()) {
                    SupplierQuoteLine sql = match.get();
                    resp.put("supplierQuoteLineId", sql.getId());
                    resp.put("quotedRate", sql.getQuotedRate());
                    resp.put("quotedQty", sql.getQuotedQty());
                    resp.put("discountPercent", sql.getDiscountPercent());
                    resp.put("gstPercent", sql.getGstPercent());
                    resp.put("freightAmount", sql.getFreightAmount());
                    resp.put("landedCost", sql.getLandedCost());
                    resp.put("expectedDeliveryDate", sql.getExpectedDeliveryDate());
                    resp.put("lineRemarks", sql.getLineRemarks());
                    resp.put("criteriaValues", sql.getCriteriaValues());
                    resp.put("hasResponse", true);

                    if (sql.getQuotedRate() != null &&
                            (lowestRate == null || sql.getQuotedRate() < lowestRate)) {
                        lowestRate = sql.getQuotedRate();
                    }
                } else {
                    resp.put("hasResponse", false);
                }
                supplierResponses.add(resp);
            }

            final Double finalLowest = lowestRate;
            supplierResponses.forEach(resp -> {
                Double rate = (Double) resp.get("quotedRate");
                resp.put("isLowest", rate != null && finalLowest != null && rate.equals(finalLowest));
            });

            row.put("supplierResponses", supplierResponses);
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Supplier summary (ranking, totals, header criteria) ──────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildSupplierSummary(String qcId) {
        List<SupplierQuote> quotes = sqRepo.findByQuoteComparison_QcId(qcId);
        List<Map<String, Object>> summary = new ArrayList<>();

        for (SupplierQuote sq : quotes) {
            int respondedLines = (int) sq.getLines().stream()
                    .filter(l -> l.getQuotedRate() != null)
                    .count();
            double total = sq.getLines().stream()
                    .filter(l -> l.getLandedCost() != null)
                    .mapToDouble(SupplierQuoteLine::getLandedCost)
                    .sum();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("supplierQuoteId", sq.getId());
            item.put("supplierId", sq.getSupplierId());
            item.put("supplierName", sq.getSupplierName());
            item.put("revisionLabel", sq.getRevisionLabel());
            item.put("label", sq.getSupplierName() + (sq.getRevisionLabel() != null && !sq.getRevisionLabel().isEmpty()
                    ? " (" + sq.getRevisionLabel() + ")" : ""));
            item.put("deliveryLeadDays", sq.getDeliveryLeadDays());
            item.put("paymentTerms", sq.getPaymentTerms());
            item.put("totalLandedCost", total);
            item.put("respondedLines", respondedLines);
            item.put("headerCriteriaValues", sq.getHeaderCriteriaValues());
            item.put("rank", null);
            summary.add(item);
        }

        // Rank ascending by total cost — only vendors that quoted at least one line are ranked.
        summary.stream()
                .filter(s -> (int) s.get("respondedLines") > 0)
                .sorted(Comparator.comparingDouble(s -> (Double) s.get("totalLandedCost")))
                .forEach(new java.util.function.Consumer<Map<String, Object>>() {
                    int rank = 1;
                    @Override
                    public void accept(Map<String, Object> s) {
                        s.put("rank", rank++);
                    }
                });

        return summary;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void mapSupplierQuoteFields(SupplierQuote sq, SupplierQuoteRequest req, String user) {
        sq.setSupplierId(req.getSupplierId());
        sq.setSupplierName(req.getSupplierName());
        sq.setQuotationRefNo(req.getQuotationRefNo());
        sq.setQuotationDate(req.getQuotationDate());
        sq.setValidityDate(req.getValidityDate());
        sq.setPaymentTerms(req.getPaymentTerms());
        sq.setFreightTerms(req.getFreightTerms());
        sq.setDeliveryLeadDays(req.getDeliveryLeadDays());
        sq.setHeaderNotes(req.getHeaderNotes());
        sq.setRevisionLabel(req.getRevisionLabel());
        sq.setCreatedByUser(user);

        if (req.getFileInformations() != null) {
            sq.setFileInformations(ReusableMethods.convertFilesListToSet(req.getFileInformations()));
        }

        for (SupplierQuoteRequest.QuoteLineRequest lr : safeList(req.getLines())) {
            sq.getLines().add(buildQuoteLine(sq, lr));
        }

        for (SupplierQuoteRequest.CriteriaValueRequest cv : safeList(req.getHeaderCriteriaValues())) {
            SupplierQuoteCriteriaValue val = new SupplierQuoteCriteriaValue();
            val.setSupplierQuote(sq);
            val.setCriteriaId(cv.getCriteriaId());
            val.setCriteriaName(cv.getCriteriaName());
            val.setValue(cv.getValue());
            sq.getHeaderCriteriaValues().add(val);
        }
    }

    // Auto-labels successive quotes from the same vendor as R-0, R-1, R-2... when the caller doesn't specify one.
    private String nextRevisionLabel(String qcId, String supplierName) {
        if (supplierName == null) return "R-0";
        long count = sqRepo.findByQuoteComparison_QcId(qcId).stream()
                .filter(sq -> supplierName.equalsIgnoreCase(sq.getSupplierName()))
                .count();
        return "R-" + count;
    }

    private void recalcStatus(String qcId) {
        List<QuoteComparisonLine> lines = qcLineRepo.findByQuoteComparison_QcId(qcId);
        if (lines.isEmpty()) return;

        QuoteComparison qc = getOrThrow(qcId);
        if ("CLOSED".equals(qc.getStatus()) || "CANCELLED".equals(qc.getStatus())) return;

        List<SupplierQuote> quotes = sqRepo.findByQuoteComparison_QcId(qcId);
        long openCount = lines.stream().filter(l -> "OPEN".equals(l.getLineStatus())).count();
        long poLinkedCount = lines.stream().filter(l -> "PO_LINKED".equals(l.getLineStatus())).count();

        if (quotes.isEmpty()) {
            qc.setStatus("DRAFT");
        } else if (poLinkedCount == lines.size()) {
            qc.setStatus("PO_COMPLETED");
        } else if (openCount == lines.size()) {
            qc.setStatus("OPEN");
        } else if (openCount == 0 && poLinkedCount > 0) {
            // Every line has been awarded, but only some have actually been turned into a PO yet —
            // distinct from FINALIZED (none ordered) so progress toward PO_COMPLETED is visible.
            qc.setStatus("PARTIALLY_ORDERED");
        } else if (openCount == 0) {
            qc.setStatus("FINALIZED");
        } else {
            qc.setStatus("PARTIALLY_FINALIZED");
        }
        qcRepo.save(qc);
    }

    private boolean isNonLowestSelection(Long lineId, SupplierQuoteLine selected) {
        if (selected.getQuotedRate() == null) return false;
        List<SupplierQuoteLine> allForLine = sqlRepo.findByQcLineId(lineId);
        return allForLine.stream()
                .filter(l -> l.getQuotedRate() != null)
                .anyMatch(l -> l.getQuotedRate() < selected.getQuotedRate());
    }

    private SupplierQuoteLine buildQuoteLine(SupplierQuote sq, SupplierQuoteRequest.QuoteLineRequest lr) {
        SupplierQuoteLine sql = new SupplierQuoteLine();
        sql.setSupplierQuote(sq);
        sql.setQcLineId(lr.getQcLineId());
        sql.setQuotedQty(lr.getQuotedQty());
        sql.setQuotedRate(lr.getQuotedRate());
        sql.setDiscountPercent(lr.getDiscountPercent() != null ? lr.getDiscountPercent() : 0.0);
        sql.setGstPercent(lr.getGstPercent() != null ? lr.getGstPercent() : 0.0);
        sql.setFreightAmount(lr.getFreightAmount() != null ? lr.getFreightAmount() : 0.0);
        sql.setExpectedDeliveryDate(lr.getExpectedDeliveryDate());
        sql.setLineRemarks(lr.getLineRemarks());
        sql.setLandedCost(computeLandedCost(lr));

        for (SupplierQuoteRequest.CriteriaValueRequest cv : safeList(lr.getCriteriaValues())) {
            SupplierQuoteCriteriaValue val = new SupplierQuoteCriteriaValue();
            val.setSupplierQuoteLine(sql);
            val.setCriteriaId(cv.getCriteriaId());
            val.setCriteriaName(cv.getCriteriaName());
            val.setValue(cv.getValue());
            sql.getCriteriaValues().add(val);
        }

        return sql;
    }

    private double computeLandedCost(SupplierQuoteRequest.QuoteLineRequest lr) {
        double rate = lr.getQuotedRate() != null ? lr.getQuotedRate() : 0;
        double qty = lr.getQuotedQty() != null ? lr.getQuotedQty() : 0;
        double discount = lr.getDiscountPercent() != null ? lr.getDiscountPercent() : 0;
        double gst = lr.getGstPercent() != null ? lr.getGstPercent() : 0;
        double freight = lr.getFreightAmount() != null ? lr.getFreightAmount() : 0;

        double base = rate * qty;
        double discountAmt = base * discount / 100;
        double gstAmt = (base - discountAmt) * gst / 100;
        return base - discountAmt + gstAmt + freight;
    }

    private QuoteComparison getOrThrow(String qcId) {
        return qcRepo.findById(qcId)
                .orElseThrow(() -> new RuntimeException("Quote comparison not found: " + qcId));
    }

    private String resolveCurrentUser() {
        try {
            return userDetailsService.getCurrentUser().getUsername();
        } catch (Exception e) {
            return "System";
        }
    }

    private String buildDescription(String summary, List<Map<String, String>> items) {
        StringBuilder sb = new StringBuilder("{\"summary\":\"").append(summary).append("\"");
        if (items != null && !items.isEmpty()) {
            sb.append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                Map<String, String> item = items.get(i);
                sb.append("{");
                item.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
                if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
                sb.append("}");
                if (i < items.size() - 1) sb.append(",");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}

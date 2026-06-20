package com.ec.application.controller;

import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.service.QuoteComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quote-comparison")
@RequiredArgsConstructor
public class QuoteComparisonController {

    private final QuoteComparisonService service;

    @PostMapping("/create")
    public ResponseEntity<QuoteComparison> create(@RequestBody QuoteComparisonCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/{qcId}")
    public ResponseEntity<Map<String, Object>> getDetail(@PathVariable String qcId) {
        return ResponseEntity.ok(service.getDetail(qcId));
    }

    @PostMapping("/list")
    public ResponseEntity<Page<QuoteComparison>> list(
            @RequestBody(required = false) QuoteComparisonFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(service.list(filter, page, size));
    }

    @PostMapping("/{qcId}/supplier-quote")
    public ResponseEntity<SupplierQuote> addSupplierQuote(
            @PathVariable String qcId,
            @RequestBody SupplierQuoteRequest req) {
        return ResponseEntity.ok(service.addSupplierQuote(qcId, req));
    }

    @PutMapping("/{qcId}/supplier-quote/{sqId}")
    public ResponseEntity<SupplierQuote> updateSupplierQuote(
            @PathVariable String qcId,
            @PathVariable Long sqId,
            @RequestBody SupplierQuoteRequest req) {
        return ResponseEntity.ok(service.updateSupplierQuote(qcId, sqId, req));
    }

    @DeleteMapping("/{qcId}/supplier-quote/{sqId}")
    public ResponseEntity<Void> deleteSupplierQuote(
            @PathVariable String qcId,
            @PathVariable Long sqId) {
        service.deleteSupplierQuote(qcId, sqId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qcId}/finalize-line")
    public ResponseEntity<QuoteComparisonLine> finalizeLine(
            @PathVariable String qcId,
            @RequestBody FinalizeLineRequest req) {
        return ResponseEntity.ok(service.finalizeLine(qcId, req));
    }

    @PostMapping("/{qcId}/reopen-line/{lineId}")
    public ResponseEntity<QuoteComparisonLine> reopenLine(
            @PathVariable String qcId,
            @PathVariable Long lineId,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(service.reopenLine(qcId, lineId, remarks));
    }

    @PostMapping("/{qcId}/close")
    public ResponseEntity<Void> close(@PathVariable String qcId) {
        service.close(qcId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qcId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String qcId) {
        service.cancel(qcId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/matrix/{qcId}")
    public ResponseEntity<List<Map<String, Object>>> matrix(@PathVariable String qcId) {
        return ResponseEntity.ok(service.buildMatrix(qcId));
    }

    // Called from PO creation to link a finalized quote line
    @PostMapping("/link-to-po")
    public ResponseEntity<QuoteToPoRef> linkToPo(@RequestBody LinkQuoteToPoRequest req) {
        return ResponseEntity.ok(service.linkToPo(req));
    }

    // Dropdown for PO line: finalized quotes for a given supplier + indent list
    @GetMapping("/finalized-for-po")
    public ResponseEntity<List<Map<String, Object>>> finalizedForPo(
            @RequestParam Long supplierId,
            @RequestParam List<String> indentIds) {
        return ResponseEntity.ok(service.getFinalizedQuotesForPo(supplierId, indentIds));
    }
}

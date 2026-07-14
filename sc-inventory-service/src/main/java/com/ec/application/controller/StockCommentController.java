package com.ec.application.controller;

import com.ec.application.data.MarkDeadStockRequest;
import com.ec.application.data.MoveFromDeadStockRequest;
import com.ec.application.data.StockCommentRequest;
import com.ec.application.model.StockComment;
import com.ec.application.service.MarkDeadStockService;
import com.ec.application.service.StockCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockCommentController {

    @Autowired
    private StockCommentService stockCommentService;

    @Autowired
    private MarkDeadStockService markDeadStockService;

    @GetMapping("/{productId}/comments")
    public List<StockComment> getComments(@PathVariable Long productId) {
        return stockCommentService.getComments(productId);
    }

    @PostMapping("/{productId}/comments")
    public StockComment addComment(@PathVariable Long productId,
                                   @RequestBody StockCommentRequest request) {
        return stockCommentService.addManualComment(productId, request);
    }

    @PostMapping("/{productId}/mark-dead-stock")
    public ResponseEntity<?> markAsDeadStock(@PathVariable Long productId,
                                              @RequestBody MarkDeadStockRequest request) throws Exception {
        return ResponseEntity.ok(markDeadStockService.markAsDeadStock(productId, request));
    }

    @PostMapping("/{productId}/move-from-dead-stock")
    public ResponseEntity<?> moveFromDeadStock(@PathVariable Long productId,
                                                @RequestBody MoveFromDeadStockRequest request) throws Exception {
        return ResponseEntity.ok(markDeadStockService.moveFromDeadStock(productId, request));
    }
}

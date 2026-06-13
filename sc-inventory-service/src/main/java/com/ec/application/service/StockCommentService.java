package com.ec.application.service;

import com.ec.application.data.StockCommentRequest;
import com.ec.application.model.StockComment;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockCommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCommentService {

    private static final Logger log = LoggerFactory.getLogger(StockCommentService.class);

    private final StockCommentRepository stockCommentRepository;
    private final ProductRepo productRepo;
    private final UserDetailsService userDetailsService;

    public List<StockComment> getComments(Long productId) {
        return stockCommentRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public StockComment addManualComment(Long productId, StockCommentRequest request) {
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        String productName = productRepo.findById(productId)
                .map(p -> p.getProductName())
                .orElse("Unknown Product");
        return save(productId, productName, request.getComment().trim(), "MANUAL", null);
    }

    public StockComment addSystemComment(Long productId, String productName, String comment,
                                         String commentType, Long linkedTransferId) {
        return save(productId, productName, comment, commentType, linkedTransferId);
    }

    private StockComment save(Long productId, String productName, String comment,
                               String commentType, Long linkedTransferId) {
        StockComment sc = new StockComment();
        sc.setProductId(productId);
        sc.setProductName(productName);
        sc.setComment(comment);
        sc.setCommentType(commentType);
        sc.setLinkedTransferId(linkedTransferId);
        sc.setCreatedAt(new Date());
        sc.setCreatedBy(resolveCurrentUser());
        return stockCommentRepository.save(sc);
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) {
            log.warn("Failed to resolve current user for stock comment: {}", e.getMessage());
            return "System";
        }
    }
}

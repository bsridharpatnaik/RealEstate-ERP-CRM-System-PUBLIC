package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "stock_comment", indexes = {
        @Index(name = "idx_sc_product", columnList = "productId"),
        @Index(name = "idx_sc_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class StockComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** MANUAL, DEAD_STOCK_IN, DEAD_STOCK_OUT, TRANSFER_AUTO */
    @Column(nullable = false, length = 30)
    private String commentType;

    @Column(length = 100)
    private String createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date createdAt;

    /** Transfer ID if this comment was created by an inventory transfer */
    private Long linkedTransferId;
}

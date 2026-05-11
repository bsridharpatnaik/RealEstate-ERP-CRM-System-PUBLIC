package com.ec.application.data;

import lombok.Data;

@Data
public class ProductMergeRequest {
    private Long sourceProductId;
    private Long targetProductId;
}

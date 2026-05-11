package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.aspects.AllowOnly;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.ProductMergePreviewDTO;
import com.ec.application.data.ProductMergeRequest;
import com.ec.application.data.ProductMergeResultDTO;
import com.ec.application.service.ProductMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product/merge")
@RequiredArgsConstructor
public class ProductMergeController {

    private final ProductMergeService productMergeService;

    @PostMapping("/preview")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN})
    public ResponseEntity<ProductMergePreviewDTO> preview(@RequestBody ProductMergeRequest request) {
        return ResponseEntity.ok(
            productMergeService.preview(request.getSourceProductId(), request.getTargetProductId()));
    }

    @PostMapping("/execute")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN})
    public ResponseEntity<ProductMergeResultDTO> execute(@RequestBody ProductMergeRequest request) {
        return ResponseEntity.ok(
            productMergeService.execute(request.getSourceProductId(), request.getTargetProductId()));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiOnlyMessageAndCodeError handleValidation(IllegalArgumentException ex) {
        return new ApiOnlyMessageAndCodeError(400, ex.getMessage());
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError handleJpa(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500,
            "Something went wrong during merge. Contact Administrator.");
    }
}

package com.ec.application.data;

import java.util.List;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.LocationWithTypeProjection;
import com.ec.application.ReusableClasses.ProductWithCategoryProjection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@JsonInclude(Include.NON_NULL)
@Data
public class NameAndProjectionDataForDropDown {

    List<IdNameProjections> machinery;
    List<IdNameProjections> usagelocation;
    List<IdNameProjections> warehouse;
    List<IdNameProjections> category;
    List<IdNameProjections> product;
    List<IdNameProjections> contractor;
    List<IdNameProjections> supplier;
    List<IdNameProjections> usageArea;
    List<IdNameProjections> productCodes;
    List<String> indentStatus;
    List<String> indentLineItemStatus;
    List<String> purchaseOrderStatus;
    List<String> tenants;
    List<IdNameDTO> stalebuckets;
    List<IdNameProjections> buildingtype;
    List<LocationWithTypeProjection> usagelocationWithType;
    List<ProductWithCategoryProjection> productWithCategory;
    Boolean boqEnforcementBlock;
    Boolean boqBlockWhenMissing;
    List<IdNameDTO> requestedByOptions;
    List<IdNameDTO> issuedByOptions;
    List<IdNameDTO> requiredByOptions;
}


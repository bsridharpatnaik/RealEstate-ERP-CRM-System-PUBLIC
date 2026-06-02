package com.ec.application.service;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.CreatePoLineRequest;
import com.ec.application.data.CreatePoRequest;
import com.ec.application.data.CustomChargeRequest;
import com.ec.application.data.IndentLineRefRequest;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderCustomCharge;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderBuilder {

    private final ProductService productService;
    private final FirmService firmService;
    private final SupplierService supplierService;
    private final IndentInventoryListRepo indentInventoryListRepo;

    public PurchaseOrder buildPurchaseOrder(CreatePoRequest request) throws Exception {

        PurchaseOrder po = new PurchaseOrder();
        po.setPoDate(request.getPoDate());
        po.setFirm(firmService.findSingleFirm(request.getFirmId()));
        po.setSupplier(supplierService.findSingleSupplier(request.getSupplierId()));
        po.setNotes(request.getNotes());
        po.setGrandTotal(request.getGrandTotal());
        po.setFreightCharges(request.getFreightCharges());
        po.setFreightGstPercent(request.getFreightGstPercent());
        po.setTotalFreightCharges(request.getTotalFreightCharges());
        po.setStatus(POStatusConstants.STATUS_NEW);
        po.setLastStatusUpdatedAt(new Date());
        po.setSubject(request.getSubject());
        po.setOverridePhoneNumber(request.getOverridePhoneNumber());
        po.setOverrideEmail(request.getOverrideEmail());
        po.setSpecialPo(request.isSpecialPo());
        po.setProjectName(request.getProjectName());
        for (CreatePoLineRequest itemReq : request.getLineItems()) {
            po.getLines().add(buildPoLine(po, itemReq));
        }
        if (request.getCustomCharges() != null) {
            for (CustomChargeRequest chargeReq : request.getCustomCharges()) {
                po.getCustomCharges().add(buildCustomCharge(po, chargeReq));
            }
        }
        if (request.getFileInformations() != null) {
            po.setFileInformations(ReusableMethods.convertFilesListToSet(request.getFileInformations()));
        } else {
            //throw new Exception("Invalid Payload. Please add file information in the payload.");
        }
        return po;
    }

    public PurchaseOrderLine buildPoLine(PurchaseOrder po, CreatePoLineRequest itemReq) throws Exception {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setPurchaseOrder(po);
        line.setBrand(itemReq.getBrand());
        line.setProduct(productService.findSingleProduct(itemReq.getProductId()));
        line.setGstPercent(itemReq.getGstPercent());
        line.setRate(itemReq.getRate());
        line.setDiscountPercent(itemReq.getDiscountPercent());
        line.setGrade(itemReq.getGrade());
        line.setNetRate(itemReq.getNetRate());
        line.setSpecification(itemReq.getSpecification());
        line.setQuantity(itemReq.getQuantity());
        line.setDiameter(itemReq.getDiameter());
        line.setTotalAmount(itemReq.getTotalAmount());
        line.setTolerancePercent(itemReq.getTolerancePercent() != null ? itemReq.getTolerancePercent() : 0.0);
        line.setSampleImageFileId(itemReq.getSampleImageFileId());
        for (IndentLineRefRequest indentRef : itemReq.getIndentRefs()) {
            PurchaseOrderIndentRef ref = buildIndentRef(indentRef);
            ref.setPoLine(line);
            line.getIndentRefs().add(ref);
        }
        return line;
    }

    private PurchaseOrderCustomCharge buildCustomCharge(PurchaseOrder po, CustomChargeRequest req) {
        PurchaseOrderCustomCharge charge = new PurchaseOrderCustomCharge();
        charge.setPurchaseOrder(po);
        charge.setChargeName(req.getChargeName());
        charge.setChargeAmount(req.getChargeAmount());
        charge.setChargeGstPercent(req.getChargeGstPercent());
        charge.setTotalChargeAmount(req.getTotalChargeAmount());
        return charge;
    }

    private PurchaseOrderIndentRef buildIndentRef(IndentLineRefRequest indentRef) {
        List<IndentInventoryList> items = indentInventoryListRepo.findByLineItemCode(indentRef.getIndentLineItemCode());
        if (items.isEmpty())
            throw new RuntimeException("Indent line item not found: " + indentRef.getIndentLineItemCode());
        IndentInventoryList refLineItem = items.get(0);
        PurchaseOrderIndentRef ref = new PurchaseOrderIndentRef();
        ref.setIndentLineItemCode(refLineItem.getLineItemCode());
        ref.setIndentNo(refLineItem.getIndentInventory().getIndentId());
        return ref;
    }
}
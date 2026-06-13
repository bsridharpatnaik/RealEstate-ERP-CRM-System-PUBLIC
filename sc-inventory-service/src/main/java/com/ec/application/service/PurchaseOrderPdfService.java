package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.Firm;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderCustomCharge;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.model.Supplier;
import com.ec.application.model.Product;
import com.ec.application.repository.PurchaseOrderRepo;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service to generate a Purchase Order PDF from a PurchaseOrder domain object.
 * <p>
 * Usage (inject this bean and call generatePdf):
 * byte[] pdfBytes = purchaseOrderPdfService.generatePdf(purchaseOrder);
 * <p>
 * Role-based visibility: project-manager and store-incharge will see the PDF with all
 * money-related fields (rates, amounts, totals) blanked out.
 */
@Service
@UseDefaultTenant
public class PurchaseOrderPdfService {

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("en", "IN"));

    private final Logger log = LoggerFactory.getLogger(PurchaseOrderPdfService.class);

    @Autowired
    PurchaseOrderRepo purchaseOrderRepo;

    @Autowired
    UserDetailsService userDetailsService;

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }

    private static final java.text.SimpleDateFormat DATE_FORMAT =
            new java.text.SimpleDateFormat("dd/MM/yyyy");


    // Overload with explicit flag — no indents
    public void generatePdf(PurchaseOrder po, OutputStream outputStream, boolean forceHideMoneyFields)
            throws Exception {
        generatePdf(po, outputStream, forceHideMoneyFields, java.util.Collections.emptyList());
    }

    // Primary overload — called by controller with explicit flag and optional indents
    public void generatePdf(PurchaseOrder po, OutputStream outputStream, boolean forceHideMoneyFields,
                            List<IndentInventory> indents)
            throws Exception {
        boolean hideMoneyFields = forceHideMoneyFields || userDetailsService.isPriceRestricted();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        addHeader(document, po);
        addVendorAndPoDetails(document, po);
        addSubjectAndIntro(document, po);
        addItemsTable(document, po, hideMoneyFields);
        addChargesSection(document, po, hideMoneyFields);
        addPriceTable(document, po, hideMoneyFields);
        addTermsAndSignatures(document, writer, po);

        if (indents != null && !indents.isEmpty()) {
            addIndentsSection(document, indents);
        }

        document.close();
    }
    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void generatePdf(PurchaseOrder po, OutputStream outputStream)
            throws Exception {

        // Resolve once: price-restricted roles must not see any monetary values
        boolean hideMoneyFields = userDetailsService.isPriceRestricted();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        addHeader(document, po);
        addVendorAndPoDetails(document, po);
        addSubjectAndIntro(document, po);
        addItemsTable(document, po, hideMoneyFields);
        addChargesSection(document, po, hideMoneyFields);
        addPriceTable(document, po, hideMoneyFields);
        addTermsAndSignatures(document, writer, po);

        document.close();
    }

    // -----------------------------------------------------------------------
    // Section builders
    // -----------------------------------------------------------------------

    private void addHeader(Document document, PurchaseOrder po) throws DocumentException {
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 1});

        // ---- LEFT: firm details
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(4f);

        Firm firm = po.getFirm();
        left.addElement(new Paragraph(firm.getFirmName(), companyFont));
        if (notBlank(firm.getAddr_line1()))
            left.addElement(new Paragraph(firm.getAddr_line1(), smallFont));
        if (notBlank(firm.getAddr_line2()))
            left.addElement(new Paragraph(firm.getAddr_line2(), smallFont));
        if (notBlank(firm.getCity()) || notBlank(firm.getState()))
            left.addElement(new Paragraph(
                    join(", ", firm.getCity(), firm.getState(), firm.getZip()), smallFont));
        String phoneToShow = notBlank(po.getOverridePhoneNumber())
                ? po.getOverridePhoneNumber()
                : firm.getFirmContactNumber();
        if (notBlank(phoneToShow))
            left.addElement(new Paragraph("Phone: " + phoneToShow, smallFont));
        String emailToShow = notBlank(po.getOverrideEmail())
                ? po.getOverrideEmail()
                : firm.getFirmEmail();
        if (notBlank(emailToShow))
            left.addElement(new Paragraph("Email: " + emailToShow, smallFont));
        if (notBlank(firm.getFirmGstNumber()))
            left.addElement(new Paragraph("GSTIN: " + firm.getFirmGstNumber(), smallFont));
        if (notBlank(firm.getFirmPanNumber()))
            left.addElement(new Paragraph("PAN: " + firm.getFirmPanNumber(), smallFont));
        headerTable.addCell(left);

        // ---- RIGHT: logo / firm name fallback
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        try {
            ClassPathResource logoResource = new ClassPathResource("sc-login-logo.png");
            byte[] logoBytes = org.apache.commons.io.IOUtils.toByteArray(logoResource.getInputStream());
            Image logo = Image.getInstance(logoBytes);
            logo.scaleToFit(90f, 40f);
            logo.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(logo);
        } catch (Exception e) {
            log.warn("Could not load logo for PDF: {}", e.getMessage());
            Paragraph fb = new Paragraph(firm.getFirmName(), companyFont);
            fb.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(fb);
        }
        headerTable.addCell(right);
        document.add(headerTable);

        Paragraph title = new Paragraph("PURCHASE ORDER",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(8f);
        title.setSpacingAfter(8f);
        document.add(title);
    }

    private void addVendorAndPoDetails(Document document, PurchaseOrder po) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{3, 2});
        mainTable.setSpacingBefore(6f);

        // ---- LEFT: PO info + supplier address
        PdfPCell left = new PdfPCell();
        left.setPadding(6);

        Supplier s = po.getSupplier();
        left.addElement(new Paragraph("PO No: " + po.getPurchaseOrderId(), normal));
        left.addElement(new Paragraph("PO Date: " + DATE_FORMAT.format(po.getPoDate()), normal));
        left.addElement(new Paragraph("Status: " + po.getStatus(), normal));
        if (notBlank(po.getProjectName()))
            left.addElement(new Paragraph("Project: " + po.getProjectName(), normal));

        left.addElement(new Paragraph(" "));
        left.addElement(new Paragraph(s.getName(), bold));
        if (notBlank(s.getAddr_line1())) left.addElement(new Paragraph(s.getAddr_line1(), normal));
        if (notBlank(s.getAddr_line2())) left.addElement(new Paragraph(s.getAddr_line2(), normal));
        left.addElement(new Paragraph(join(", ", s.getCity(), s.getState(), s.getZip()), normal));
        if (notBlank(s.getGstNumber())) left.addElement(new Paragraph("GSTIN: " + s.getGstNumber(), normal));
        if (notBlank(s.getContactPerson()))
            left.addElement(new Paragraph("Contact: " + s.getContactPerson(), normal));
        String vendorPhone = notBlank(s.getMobileNo()) ? s.getMobileNo() : s.getContactPersonMobileNo();
        if (notBlank(vendorPhone))
            left.addElement(new Paragraph("Phone: " + vendorPhone, normal));
        mainTable.addCell(left);

        // ---- RIGHT: Vendor Account Details
        PdfPTable accountTable = new PdfPTable(2);
        accountTable.setWidthPercentage(100);
        accountTable.setWidths(new float[]{1.2f, 2f});

        PdfPCell heading = new PdfPCell(new Phrase("Vendor Account Details", bold));
        heading.setColspan(2);
        heading.setHorizontalAlignment(Element.ALIGN_CENTER);
        heading.setBackgroundColor(new BaseColor(230, 230, 230));
        heading.setPadding(4f);
        accountTable.addCell(heading);

        addAccountRow(accountTable, "A/C Name", s.getAccountName(), normal);
        addAccountRow(accountTable, "Bank Name", s.getBankName(), normal);
        addAccountRow(accountTable, "Branch", s.getBranchName(), normal);
        addAccountRow(accountTable, "A/C No", s.getAccountNumber(), normal);
        addAccountRow(accountTable, "IFSC Code", s.getIfscCode(), normal);

        PdfPCell right = new PdfPCell(accountTable);
        right.setPadding(4);
        mainTable.addCell(right);

        document.add(mainTable);
    }

    private void addSubjectAndIntro(Document document, PurchaseOrder po) throws DocumentException {
        Font subjectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        String subjectText = notBlank(po.getSubject())
                ? po.getSubject()
                : "Purchase Order for Supply of below mentioned Product";
        Paragraph subject = new Paragraph("Subject: " + subjectText, subjectFont);
        subject.setSpacingBefore(10f);
        subject.setSpacingAfter(6f);
        document.add(subject);

        document.add(new Paragraph("Dear Sir,", normalFont));

        Paragraph intro = new Paragraph(
                "With reference to rates quoted by you, please arrange to make supplies as per the following details:",
                boldFont);
        intro.setSpacingBefore(4f);
        intro.setSpacingAfter(6f);
        document.add(intro);
    }

    /**
     * Renders the line-items table.
     *
     * @param hideMoneyFields when true, all monetary columns (rate, amounts, totals)
     *                        are rendered as blank — headers and values alike.
     */
    private void addItemsTable(Document document, PurchaseOrder po, boolean hideMoneyFields)
            throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        List<PurchaseOrderLine> lines = new ArrayList<>(po.getLines());

        // Only show the image column when at least one line has an image
        boolean hasImages = lines.stream()
                .anyMatch(l -> l.getSampleImageData() != null && l.getSampleImageData().length > 0);
        int numCols = hasImages ? 12 : 11;

        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(0f);

        if (hasImages) {
            table.setWidths(new float[]{2.5f, 1.8f, 0.85f, 0.85f, 1.0f, 1.0f, 0.9f, 0.85f, 1.0f, 1.0f, 0.75f, 0.95f, 1.2f});
        } else {
            table.setWidths(new float[]{2.5f, 0.85f, 0.85f, 1.0f, 1.0f, 0.9f, 0.85f, 1.0f, 1.0f, 0.75f, 0.95f, 1.2f});
        }

        // Column headers — money columns blanked for executives
        addHeaderCell(table, "Code & Description", headerFont);
        if (hasImages) addHeaderCell(table, "Sample Image", headerFont);
        addHeaderCell(table, "Qty", headerFont);
        addHeaderCell(table, "UOM", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Rate \u20B9", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Total \u20B9", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Discount %", headerFont);
        addHeaderCell(table, "Tolerance %", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Net Rate \u20B9", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Net Rate/Unit \u20B9", headerFont);
        addHeaderCell(table, "GST %", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "GST Amt \u20B9", headerFont);
        addHeaderCell(table, hideMoneyFields ? "" : "Amt Incl Tax \u20B9", headerFont);

        for (PurchaseOrderLine line : lines) {
            Product product = line.getProduct();
            String desc = product.getProductName()
                    + (notBlank(product.getProductCode()) ? "\n[" + product.getProductCode() + "]" : "")
                    + (notBlank(line.getBrand())          ? "\nBrand Name: " + line.getBrand() : "")
                    + (notBlank(line.getGrade())          ? "\nGrade: " + line.getGrade() : "")
                    + (notBlank(line.getDiameter())       ? "\nDia: " + line.getDiameter() : "")
                    + (notBlank(line.getSpecification()) && !"-".equals(line.getSpecification())
                    ? "\nSpec: " + line.getSpecification() : "");

            double qty = line.getQuantity() != null ? line.getQuantity() : 0.0;
            double rate = line.getRate() != null ? line.getRate() : 0.0;
            double discPct = line.getDiscountPercent() != null ? line.getDiscountPercent() : 0.0;
            double tolPct  = line.getTolerancePercent() != null ? line.getTolerancePercent() : 0.0;
            double gstPct = line.getGstPercent() != null ? line.getGstPercent() : 0.0;
            double grossTotal = qty * rate;
            double taxable = line.getNetRate() != null ? line.getNetRate() : 0.0;
            double gstAmt = taxable * gstPct / 100.0;
            double amtInclTax = line.getTotalAmount() != null ? line.getTotalAmount() : 0.0;
            String tolStr  = tolPct > 0 ? (tolPct % 1 == 0 ? String.valueOf((int) tolPct) : fmt(tolPct)) + "%" : "-";

            addBodyCell(table, desc, normalFont);

            // Sample image cell — only added when the column is shown
            if (hasImages) {
                PdfPCell imageCell = new PdfPCell();
                imageCell.setPadding(4f);
                imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                boolean imageAdded = false;
                byte[] imgBytes = line.getSampleImageData();
                if (imgBytes != null && imgBytes.length > 0) {
                    try {
                        Image img = Image.getInstance(imgBytes);
                        img.scaleToFit(58f, 58f);
                        img.setAlignment(Image.ALIGN_CENTER);
                        imageCell.addElement(img);
                        imageAdded = true;
                    } catch (Exception e) {
                        log.warn("Could not embed sample image for line {}: {}", line.getId(), e.getMessage());
                    }
                }
                if (!imageAdded) {
                    imageCell.addElement(new Phrase("-", normalFont));
                }
                table.addCell(imageCell);
            }

            addBodyCell(table, fmt(qty), normalFont);
            addBodyCell(table, product.getMeasurementUnit(), normalFont);
            addBodyCell(table, hideMoneyFields ? "" : fmt(rate), normalFont);
            addBodyCell(table, hideMoneyFields ? "" : fmt(grossTotal), normalFont);
            addBodyCell(table, hideMoneyFields ? "" : (discPct > 0 ? (discPct % 1 == 0 ? String.valueOf((int) discPct) : fmt(discPct)) + "%" : "-"), normalFont);
            addBodyCell(table, tolStr, normalFont);
            addBodyCell(table, hideMoneyFields ? "" : fmt(taxable), normalFont);
            addBodyCell(table, hideMoneyFields ? "" : (qty > 0 ? fmt(taxable / qty) : "-"), normalFont);
            addBodyCell(table, fmt(gstPct) + "%", normalFont);
            addBodyCell(table, hideMoneyFields ? "" : fmt(gstAmt), normalFont);
            addBodyCell(table, hideMoneyFields ? "" : fmt(amtInclTax), normalFont);
        }

        // Line items subtotal — single full-width cell to avoid narrow-column overflow
        double lineItemsSubtotal = lines.stream()
                .mapToDouble(line -> line.getTotalAmount() != null ? line.getTotalAmount() : 0.0)
                .sum();

        String totalText = hideMoneyFields ? "TOTAL" : "TOTAL    " + fmt(lineItemsSubtotal);
        PdfPCell totalCell = new PdfPCell(new Phrase(totalText, headerFont));
        totalCell.setColspan(numCols);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setPadding(4f);
        table.addCell(totalCell);

        document.add(table);
    }

    private void addChargesSection(Document document, PurchaseOrder po, boolean hideMoneyFields)
            throws DocumentException {
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        double freightCharges = po.getFreightCharges() != null ? po.getFreightCharges() : 0.0;
        double freightGstPct = po.getFreightGstPercent() != null ? po.getFreightGstPercent() : 0.0;
        double totalFreight = po.getTotalFreightCharges() != null ? po.getTotalFreightCharges() : 0.0;

        boolean hasCustomCharges = po.getCustomCharges() != null
                && po.getCustomCharges().stream().anyMatch(c -> c.getChargeAmount() != null && c.getChargeAmount() > 0);

        if (freightCharges <= 0 && !hasCustomCharges) return;

        PdfPTable charges = new PdfPTable(2);
        charges.setWidthPercentage(40);
        charges.setHorizontalAlignment(Element.ALIGN_RIGHT);
        charges.setSpacingBefore(0f);

        if (freightCharges > 0) {
            PdfPCell fcLabel = new PdfPCell(new Phrase("Freight Charges", normal));
            fcLabel.setPadding(4f);
            charges.addCell(fcLabel);
            PdfPCell fcVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(freightCharges), normal));
            fcVal.setPadding(4f);
            fcVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            charges.addCell(fcVal);

            PdfPCell gstLabel = new PdfPCell(new Phrase("Freight GST %", normal));
            gstLabel.setPadding(4f);
            charges.addCell(gstLabel);
            PdfPCell gstVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(freightGstPct) + "%", normal));
            gstVal.setPadding(4f);
            gstVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            charges.addCell(gstVal);

            PdfPCell freightTotalLabel = new PdfPCell(new Phrase("Total Freight Charges", bold));
            freightTotalLabel.setPadding(4f);
            charges.addCell(freightTotalLabel);
            PdfPCell freightTotalVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(totalFreight), bold));
            freightTotalVal.setPadding(4f);
            freightTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            charges.addCell(freightTotalVal);
        }

        if (hasCustomCharges) {
            for (PurchaseOrderCustomCharge cc : po.getCustomCharges()) {
                if (cc.getChargeAmount() == null || cc.getChargeAmount() <= 0) continue;
                String name = notBlank(cc.getChargeName()) ? cc.getChargeName() : "Additional Charges";
                double gstPct = cc.getChargeGstPercent() != null ? cc.getChargeGstPercent() : 0.0;
                double total = cc.getTotalChargeAmount() != null ? cc.getTotalChargeAmount() : 0.0;

                PdfPCell ccLabel = new PdfPCell(new Phrase(name, normal));
                ccLabel.setPadding(4f);
                charges.addCell(ccLabel);
                PdfPCell ccVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(cc.getChargeAmount()), normal));
                ccVal.setPadding(4f);
                ccVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                charges.addCell(ccVal);

                PdfPCell ccGstLabel = new PdfPCell(new Phrase(name + " GST %", normal));
                ccGstLabel.setPadding(4f);
                charges.addCell(ccGstLabel);
                PdfPCell ccGstVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(gstPct) + "%", normal));
                ccGstVal.setPadding(4f);
                ccGstVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                charges.addCell(ccGstVal);

                PdfPCell ccTotalLabel = new PdfPCell(new Phrase("Total " + name, bold));
                ccTotalLabel.setPadding(4f);
                charges.addCell(ccTotalLabel);
                PdfPCell ccTotalVal = new PdfPCell(new Phrase(hideMoneyFields ? "" : fmt(total), bold));
                ccTotalVal.setPadding(4f);
                ccTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                charges.addCell(ccTotalVal);
            }
        }

        document.add(charges);
    }

    /**
     * Renders the grand-total summary row beneath the charges section.
     *
     * @param hideMoneyFields when true, amount-in-words and grand total are blanked.
     */
    private void addPriceTable(Document document, PurchaseOrder po, boolean hideMoneyFields)
            throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);

        String amountInWords = hideMoneyFields || po.getGrandTotal() == null
                ? ""
                : numberToWords(po.getGrandTotal()) + " RUPEES ONLY";

        PdfPCell wordsCell = new PdfPCell(new Phrase(amountInWords.toUpperCase(), headerFont));
        wordsCell.setColspan(5);
        wordsCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        wordsCell.setPadding(4f);
        table.addCell(wordsCell);

        PdfPCell totalLabel = new PdfPCell(new Phrase("GRAND TOTAL \u20B9", headerFont));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(4f);
        table.addCell(totalLabel);

        PdfPCell totalVal = new PdfPCell(
                new Phrase(hideMoneyFields || po.getGrandTotal() == null ? "" : fmt(po.getGrandTotal()), headerFont));
        totalVal.setColspan(2);
        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalVal.setPadding(4f);
        table.addCell(totalVal);

        document.add(table);
    }

    private void addTermsAndSignatures(Document document, PdfWriter writer, PurchaseOrder po) throws DocumentException {
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);

        Paragraph termsTitle = new Paragraph("Terms & Conditions", labelFont);
        termsTitle.setSpacingBefore(8f);
        document.add(termsTitle);

        if (notBlank(po.getNotes())) {
            String notes = po.getNotes().trim();
            if (!notes.contains("<")) {
                // Plain text — render directly with iText, no XMLWorker needed
                for (String line : notes.split("\n")) {
                    document.add(new Paragraph(line.trim(), smallFont));
                }
            } else {
                // HTML content from rich text editor — strip tags and render as plain text
                for (String line : htmlToPlainLines(notes)) {
                    document.add(new Paragraph(line, smallFont));
                }
            }
        }

        // ---- Signature Section ----
        PdfPTable signTable = new PdfPTable(3);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(8f);

        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        // Signature space row (blank, gives room to sign)
        PdfPCell blankLeft = new PdfPCell(new Phrase(" ", signFont));
        PdfPCell blankMiddle = new PdfPCell(new Phrase(" ", signFont));
        PdfPCell blankRight = new PdfPCell(new Phrase(" ", signFont));
        blankLeft.setBorder(Rectangle.NO_BORDER);
        blankMiddle.setBorder(Rectangle.NO_BORDER);
        blankRight.setBorder(Rectangle.NO_BORDER);
        blankLeft.setMinimumHeight(30f);
        blankMiddle.setMinimumHeight(30f);
        blankRight.setMinimumHeight(30f);
        signTable.addCell(blankLeft);
        signTable.addCell(blankMiddle);
        signTable.addCell(blankRight);

        PdfPCell creatorNameCell = new PdfPCell(new Phrase(po.getCreatedBy() != null ? po.getCreatedBy() : "", signFont));
        creatorNameCell.setBorder(Rectangle.NO_BORDER);
        creatorNameCell.setPadding(2f);
        signTable.addCell(creatorNameCell);

        PdfPCell emptyMiddle = new PdfPCell(new Phrase("", signFont));
        emptyMiddle.setBorder(Rectangle.NO_BORDER);
        PdfPCell emptyRight = new PdfPCell(new Phrase("", signFont));
        emptyRight.setBorder(Rectangle.NO_BORDER);
        signTable.addCell(emptyMiddle);
        signTable.addCell(emptyRight);

        // Label row
        PdfPCell preparedBy = new PdfPCell(new Phrase("Prepared By", signFont));
        preparedBy.setBorder(Rectangle.NO_BORDER);
        preparedBy.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell checkedBy = new PdfPCell(new Phrase("", signFont));
        checkedBy.setBorder(Rectangle.NO_BORDER);
        checkedBy.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell authorisedBy = new PdfPCell(new Phrase("Authorised By", signFont));
        authorisedBy.setBorder(Rectangle.NO_BORDER);
        authorisedBy.setHorizontalAlignment(Element.ALIGN_RIGHT);

        signTable.addCell(preparedBy);
        signTable.addCell(checkedBy);
        signTable.addCell(authorisedBy);

        document.add(signTable);
    }

    // -----------------------------------------------------------------------
    // Indent section
    // -----------------------------------------------------------------------

    private void addIndentsSection(Document document, List<IndentInventory> indents)
            throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
        Font indentHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
        Font tableHeaderFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);
        Font normalFont       = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        // Page-level section heading
        Paragraph heading = new Paragraph("ASSOCIATED INDENTS", sectionTitleFont);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(20f);
        heading.setSpacingAfter(10f);
        document.add(heading);

        for (IndentInventory indent : indents) {
            // ── Indent header info table ──────────────────────────────────
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 2f, 1f, 2f});
            infoTable.setSpacingBefore(8f);
            infoTable.setSpacingAfter(0f);

            // Header spanning all 4 columns
            PdfPCell indentHeading = new PdfPCell(
                    new Phrase("Indent: " + (indent.getIndentId() != null ? indent.getIndentId() : "-"),
                            indentHeaderFont));
            indentHeading.setColspan(4);
            indentHeading.setBackgroundColor(new BaseColor(210, 225, 245));
            indentHeading.setPadding(5f);
            infoTable.addCell(indentHeading);

            addIndentInfoRow(infoTable, "Indent No", indent.getIndentId(), normalFont);
            addIndentInfoRow(infoTable, "Date",
                    indent.getIndentDate() != null ? DATE_FORMAT.format(indent.getIndentDate()) : "-",
                    normalFont);
            addIndentInfoRow(infoTable, "Status",
                    indent.getIndentStatus() != null ? indent.getIndentStatus() : "-", normalFont);
            addIndentInfoRow(infoTable, "Project",
                    indent.getTenant() != null ? indent.getTenant() : "-", normalFont);

            document.add(infoTable);

            // ── Line items table ──────────────────────────────────────────
            List<IndentInventoryList> lineItems = indent.getInventoryList() != null
                    ? new ArrayList<>(indent.getInventoryList())
                    : new ArrayList<>();

            PdfPTable lineTable = new PdfPTable(8);
            lineTable.setWidthPercentage(100);
            lineTable.setWidths(new float[]{2.5f, 1.2f, 0.8f, 0.9f, 0.9f, 0.9f, 1.1f, 1.2f});
            lineTable.setSpacingBefore(2f);
            lineTable.setSpacingAfter(12f);

            addHeaderCell(lineTable, "Product", tableHeaderFont);
            addHeaderCell(lineTable, "Specification", tableHeaderFont);
            addHeaderCell(lineTable, "UOM", tableHeaderFont);
            addHeaderCell(lineTable, "Qty Ordered", tableHeaderFont);
            addHeaderCell(lineTable, "Qty Received", tableHeaderFont);
            addHeaderCell(lineTable, "Qty Pending", tableHeaderFont);
            addHeaderCell(lineTable, "Need By Date", tableHeaderFont);
            addHeaderCell(lineTable, "Status", tableHeaderFont);

            if (lineItems.isEmpty()) {
                PdfPCell noData = new PdfPCell(new Phrase("No line items", normalFont));
                noData.setColspan(8);
                noData.setPadding(6f);
                noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                lineTable.addCell(noData);
            } else {
                for (IndentInventoryList item : lineItems) {
                    String productName = item.getProduct() != null
                            ? item.getProduct().getProductName()
                              + (notBlank(item.getProduct().getProductCode())
                                 ? "\n[" + item.getProduct().getProductCode() + "]" : "")
                            : "-";
                    addBodyCell(lineTable, productName, normalFont);
                    addBodyCell(lineTable, notBlank(item.getSpecification()) ? item.getSpecification() : "-", normalFont);
                    addBodyCell(lineTable, notBlank(item.getMeasurementUnit()) ? item.getMeasurementUnit() : "-", normalFont);
                    addBodyCell(lineTable, item.getQuantity() != null ? fmt(item.getQuantity()) : "-", normalFont);
                    addBodyCell(lineTable, item.getQuantityReceived() != null ? fmt(item.getQuantityReceived()) : "-", normalFont);
                    addBodyCell(lineTable, item.getQuantityPending() != null ? fmt(item.getQuantityPending()) : "-", normalFont);
                    addBodyCell(lineTable, notBlank(item.getLineItemStatus()) ? item.getLineItemStatus() : "-", normalFont);
                }
            }

            document.add(lineTable);
        }
    }

    private void addIndentInfoRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        labelCell.setPadding(4f);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", font));
        valueCell.setPadding(4f);
        table.addCell(valueCell);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PdfPCell makeBlankSignCell() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setMinimumHeight(50f);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell makeSignLabelCell(String label, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(4f);
        return cell;
    }

    private void addSignHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new BaseColor(230, 230, 230));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addSignRow(PdfPTable table, String role, String name,
                            String designation, String date, Font font) {
        PdfPCell roleCell = new PdfPCell(new Phrase(role, font));
        roleCell.setBackgroundColor(new BaseColor(245, 245, 245));
        roleCell.setPadding(5f);
        roleCell.setMinimumHeight(35f);
        roleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(roleCell);

        PdfPCell nameCell = new PdfPCell(new Phrase(name, font));
        nameCell.setPadding(5f);
        nameCell.setMinimumHeight(35f);
        table.addCell(nameCell);

        PdfPCell desigCell = new PdfPCell(new Phrase(designation, font));
        desigCell.setPadding(5f);
        desigCell.setMinimumHeight(35f);
        table.addCell(desigCell);

        PdfPCell dateCell = new PdfPCell(new Phrase(date, font));
        dateCell.setPadding(5f);
        dateCell.setMinimumHeight(35f);
        table.addCell(dateCell);

        PdfPCell signCell = new PdfPCell(new Phrase("", font));
        signCell.setPadding(5f);
        signCell.setMinimumHeight(35f);
        table.addCell(signCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new BaseColor(230, 230, 230));
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addAccountRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell k = new PdfPCell(new Phrase(label, font));
        k.setPadding(5f);
        k.setMinimumHeight(22f);
        k.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(k);

        PdfPCell v = new PdfPCell(new Phrase(value != null ? value : "", font));
        v.setPadding(5f);
        v.setMinimumHeight(22f);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(v);
    }

    private List<String> htmlToPlainLines(String html) {
        String text = html
            // block-level tags → newline
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</p>",       "\n")
            .replaceAll("(?i)</li>",      "\n")
            .replaceAll("(?i)</div>",     "\n")
            // common HTML entities
            .replace("&nbsp;",   " ")
            .replace("&amp;",    "&")
            .replace("&lt;",     "<")
            .replace("&gt;",     ">")
            .replace("&quot;",   "\"")
            .replace("&apos;",   "'")
            .replace("&ndash;",  "–")
            .replace("&mdash;",  "—")
            .replace("&ldquo;",  "“")
            .replace("&rdquo;",  "”")
            .replace("&lsquo;",  "‘")
            .replace("&rsquo;",  "’")
            .replace("&bull;",   "•")
            .replace("&hellip;", "…")
            // strip all remaining tags
            .replaceAll("<[^>]+>", "")
            // numeric entities
            .replaceAll("&#160;", " ");

        List<String> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (notBlank(p)) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private String fmt(double value) {
        return CURRENCY_FORMAT.format(value);
    }

    /**
     * Very simple number-to-words for Indian currency (up to crores).
     */
    private String numberToWords(double amount) {
        long n = Math.round(amount);
        if (n == 0) return "ZERO";

        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN",
                "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN",
                "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY",
                "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};

        StringBuilder sb = new StringBuilder();
        if (n >= 10_000_000) {
            sb.append(twoDigits(n / 10_000_000, ones, tens)).append(" CRORE ");
            n %= 10_000_000;
        }
        if (n >= 100_000) {
            sb.append(twoDigits(n / 100_000, ones, tens)).append(" LAKH ");
            n %= 100_000;
        }
        if (n >= 1_000) {
            sb.append(twoDigits(n / 1_000, ones, tens)).append(" THOUSAND ");
            n %= 1_000;
        }
        if (n >= 100) {
            sb.append(ones[(int) (n / 100)]).append(" HUNDRED ");
            n %= 100;
        }
        if (n > 0) {
            sb.append(twoDigits(n, ones, tens));
        }
        return sb.toString().trim();
    }

    private String twoDigits(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + ones[(int) (n % 10)] : "");
    }
}
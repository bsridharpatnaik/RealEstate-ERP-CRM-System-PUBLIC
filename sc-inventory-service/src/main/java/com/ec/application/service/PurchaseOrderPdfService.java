package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.Firm;
import com.ec.application.model.PurchaseOrder;
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
 *
 * Usage (inject this bean and call generatePdf):
 *   byte[] pdfBytes = purchaseOrderPdfService.generatePdf(purchaseOrder);
 */
@Service
@UseDefaultTenant
public class PurchaseOrderPdfService {

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("en", "IN"));

    private final Logger log = LoggerFactory.getLogger(PurchaseOrderPdfService.class);

    @Autowired
    PurchaseOrderRepo purchaseOrderRepo;

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void generatePdf(PurchaseOrder po, OutputStream outputStream)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        addHeader(document, po);
        addVendorAndPoDetails(document, po);
        addSubjectAndIntro(document, po);
        addItemsTable(document, po);
        addChargesSection(document);
        addPriceTable(document, po);
        addTermsAndSignatures(document, po);

        document.close();
    }

    // -----------------------------------------------------------------------
    // Section builders
    // -----------------------------------------------------------------------

    private void addHeader(Document document, PurchaseOrder po) throws DocumentException {
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.BLACK);
        Font smallFont  = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

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
        if (notBlank(firm.getFirmContactNumber()))
            left.addElement(new Paragraph("Phone: " + firm.getFirmContactNumber(), smallFont));
        if (notBlank(firm.getFirmEmail()))
            left.addElement(new Paragraph("Email: " + firm.getFirmEmail(), smallFont));
        if (notBlank(firm.getFirmGstNumber()))
            left.addElement(new Paragraph("GSTIN: " + firm.getFirmGstNumber(), smallFont));
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
        Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
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
        left.addElement(new Paragraph("PO Date: " + po.getPoDate(), normal));
        left.addElement(new Paragraph("Status: " + po.getStatus(), normal));
        left.addElement(new Paragraph(" "));
        left.addElement(new Paragraph(s.getName(), bold));
        if (notBlank(s.getAddr_line1())) left.addElement(new Paragraph(s.getAddr_line1(), normal));
        if (notBlank(s.getAddr_line2())) left.addElement(new Paragraph(s.getAddr_line2(), normal));
        left.addElement(new Paragraph(join(", ", s.getCity(), s.getState(), s.getZip()), normal));
        if (notBlank(s.getGstNumber()))  left.addElement(new Paragraph("GSTIN: " + s.getGstNumber(), normal));
        if (notBlank(s.getContactPerson()))
            left.addElement(new Paragraph("Contact: " + s.getContactPerson()
                    + (notBlank(s.getMobileNo()) ? " | " + s.getMobileNo() : ""), normal));
        mainTable.addCell(left);

        // ---- RIGHT: Vendor Account Details
        PdfPTable accountTable = new PdfPTable(2);
        accountTable.setWidthPercentage(100);

        PdfPCell heading = new PdfPCell(new Phrase("Vendor Account Details", bold));
        heading.setColspan(2);
        heading.setHorizontalAlignment(Element.ALIGN_CENTER);
        heading.setBackgroundColor(new BaseColor(230, 230, 230));
        heading.setPadding(4f);
        accountTable.addCell(heading);

        addAccountRow(accountTable, "A/C Name",   s.getAccountName(),   normal);
        addAccountRow(accountTable, "Bank Name",  s.getBankName(),      normal);
        addAccountRow(accountTable, "Branch",     s.getBranchName(),    normal);
        addAccountRow(accountTable, "A/C No",     s.getAccountNumber(), normal);
        addAccountRow(accountTable, "IFSC Code",  s.getIfscCode(),      normal);

        PdfPCell right = new PdfPCell(accountTable);
        right.setPadding(4);
        mainTable.addCell(right);

        document.add(mainTable);
    }

    private void addSubjectAndIntro(Document document, PurchaseOrder po) throws DocumentException {
        Font subjectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font boldFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normalFont  = FontFactory.getFont(FontFactory.HELVETICA, 9);

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

    private void addItemsTable(Document document, PurchaseOrder po) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(0f);
        table.setWidths(new float[]{2.8f, 0.9f, 0.9f, 1.1f, 1.1f, 0.9f, 1.1f, 0.8f, 1.0f, 1.3f});

        addHeaderCell(table, "Code & Description", headerFont);
        addHeaderCell(table, "Qty",                headerFont);
        addHeaderCell(table, "UOM",                headerFont);
        addHeaderCell(table, "Rate \u20B9",        headerFont);
        addHeaderCell(table, "Total \u20B9",       headerFont);
        addHeaderCell(table, "Discount \u20B9",    headerFont);
        addHeaderCell(table, "Taxable \u20B9",     headerFont);
        addHeaderCell(table, "GST %",              headerFont);
        addHeaderCell(table, "GST Amt \u20B9",     headerFont);
        addHeaderCell(table, "Amt Incl Tax \u20B9",headerFont);

        List<PurchaseOrderLine> lines = new ArrayList<>(po.getLines());
        for (PurchaseOrderLine line : lines) {
            Product product = line.getProduct();
            String desc = product.getProductName()
                    + (notBlank(product.getProductCode()) ? "\n[" + product.getProductCode() + "]" : "")
                    + (notBlank(line.getSpecification()) && !"-".equals(line.getSpecification())
                       ? "\n" + line.getSpecification() : "");

            double qty       = line.getQuantity();
            double rate      = line.getRate();
            double total     = qty * rate;
            double gstPct    = line.getGstPercent();
            double gstAmt    = total * gstPct / 100.0;
            double amtInclTax = total + gstAmt;

            addBodyCell(table, desc,                                    normalFont);
            addBodyCell(table, fmt(qty),                                normalFont);
            addBodyCell(table, product.getMeasurementUnit(),            normalFont);
            addBodyCell(table, fmt(rate),                               normalFont);
            addBodyCell(table, fmt(total),                              normalFont);
            addBodyCell(table, "0.00",                                  normalFont);
            addBodyCell(table, fmt(total),                              normalFont);
            addBodyCell(table, fmt(gstPct) + "%",                       normalFont);
            addBodyCell(table, fmt(gstAmt),                             normalFont);
            addBodyCell(table, fmt(amtInclTax),                        normalFont);
        }

        // TOTAL row
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL \u20B9", headerFont));
        totalLabel.setColspan(9);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(4f);
        table.addCell(totalLabel);

        PdfPCell totalVal = new PdfPCell(new Phrase(fmt(po.getGrandTotal()), headerFont));
        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalVal.setPadding(4f);
        table.addCell(totalVal);

        document.add(table);
    }

    private void addChargesSection(Document document) throws DocumentException {
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable charges = new PdfPTable(2);
        charges.setWidthPercentage(40);
        charges.setHorizontalAlignment(Element.ALIGN_RIGHT);
        charges.setSpacingBefore(0f);

        charges.addCell(new Phrase("Freight Charges",           normal)); charges.addCell(new Phrase("0.00", normal));
        charges.addCell(new Phrase("Loading & Packing Charges", normal)); charges.addCell(new Phrase("0.00", normal));
        charges.addCell(new Phrase("Insurance Charges",         normal)); charges.addCell(new Phrase("0.00", normal));

        document.add(charges);
    }

    private void addPriceTable(Document document, PurchaseOrder po) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);

        String amountInWords = numberToWords(po.getGrandTotal()) + " RUPEES ONLY";

        PdfPCell wordsCell = new PdfPCell(new Phrase(amountInWords.toUpperCase(), headerFont));
        wordsCell.setColspan(6);
        wordsCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        wordsCell.setPadding(4f);
        table.addCell(wordsCell);

        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL \u20B9", headerFont));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(4f);
        table.addCell(totalLabel);

        PdfPCell totalVal = new PdfPCell(new Phrase(fmt(po.getGrandTotal()), headerFont));
        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalVal.setPadding(4f);
        table.addCell(totalVal);

        document.add(table);
    }

    private void addTermsAndSignatures(Document document, PurchaseOrder po) throws DocumentException {
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);

        Paragraph termsTitle = new Paragraph("Terms & Conditions", labelFont);
        termsTitle.setSpacingBefore(8f);
        document.add(termsTitle);

        document.add(new Paragraph("* 100% Advance payment.", smallFont));
        document.add(new Paragraph("* Immediate delivery.", smallFont));
        document.add(new Paragraph("* Material will not be accepted without original copy of invoice.", smallFont));
        document.add(new Paragraph("* Unloaded weight/qty at our site will be final for payment/invoice.", smallFont));

        if (notBlank(po.getNotes())) {
            document.add(new Paragraph("* " + po.getNotes(), smallFont));
        }

        PdfPTable signTable = new PdfPTable(3);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(12f);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(Rectangle.NO_BORDER);
        signTable.addCell(empty);
        signTable.addCell(empty);

        PdfPCell forCompany = new PdfPCell(
                new Phrase("For " + po.getFirm().getFirmName(), smallFont));
        forCompany.setBorder(Rectangle.NO_BORDER);
        forCompany.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signTable.addCell(forCompany);

        signTable.addCell(empty);
        signTable.addCell(empty);

        PdfPCell authSign = new PdfPCell(new Phrase("Authorised Signatory", smallFont));
        authSign.setBorder(Rectangle.NO_BORDER);
        authSign.setHorizontalAlignment(Element.ALIGN_RIGHT);
        authSign.setPaddingTop(20f);
        signTable.addCell(authSign);

        document.add(signTable);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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
        k.setPadding(3f);
        PdfPCell v = new PdfPCell(new Phrase(value != null ? value : "-", font));
        v.setPadding(3f);
        table.addCell(k);
        table.addCell(v);
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

    /** Very simple number-to-words for Indian currency (up to crores). */
    private String numberToWords(double amount) {
        long n = Math.round(amount);
        if (n == 0) return "ZERO";

        String[] ones  = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN",
                          "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN",
                          "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens  = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY",
                          "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};

        StringBuilder sb = new StringBuilder();
        if (n >= 10_000_000) { sb.append(twoDigits(n / 10_000_000, ones, tens)).append(" CRORE "); n %= 10_000_000; }
        if (n >= 100_000)    { sb.append(twoDigits(n / 100_000, ones, tens)).append(" LAKH ");   n %= 100_000; }
        if (n >= 1_000)      { sb.append(twoDigits(n / 1_000, ones, tens)).append(" THOUSAND "); n %= 1_000; }
        if (n >= 100)        { sb.append(ones[(int)(n / 100)]).append(" HUNDRED ");               n %= 100; }
        if (n > 0)           { sb.append(twoDigits(n, ones, tens)); }
        return sb.toString().trim();
    }

    private String twoDigits(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int)(n / 10)] + (n % 10 != 0 ? " " + ones[(int)(n % 10)] : "");
    }
}
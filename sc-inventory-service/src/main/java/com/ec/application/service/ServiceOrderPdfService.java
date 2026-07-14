package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.Firm;
import com.ec.application.model.ServiceOrder;
import com.ec.application.model.ServiceOrderLine;
import com.ec.application.model.ServiceOrderLineCustomField;
import com.ec.application.model.Supplier;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@UseDefaultTenant
public class ServiceOrderPdfService {

    private final Logger log = LoggerFactory.getLogger(ServiceOrderPdfService.class);

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("en", "IN"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }

    public void generatePdf(ServiceOrder so, OutputStream outputStream) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        addHeader(document, so);
        addSoAndVendorDetails(document, so);
        addSubject(document, so);
        addLinesTable(document, so);
        addTotalsSection(document, so);
        addCompletionSection(document, so);
        addNotesSection(document, so);
        addSignatures(document, so);

        document.close();
    }

    // -----------------------------------------------------------------------
    // Section builders
    // -----------------------------------------------------------------------

    private void addHeader(Document document, ServiceOrder so) throws DocumentException {
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.BLACK);
        Font smallFont   = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 1});

        // LEFT: firm details
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(4f);

        Firm firm = so.getFirm();
        if (firm != null) {
            left.addElement(new Paragraph(firm.getFirmName(), companyFont));
            if (notBlank(firm.getAddr_line1()))
                left.addElement(new Paragraph(firm.getAddr_line1(), smallFont));
            if (notBlank(firm.getAddr_line2()))
                left.addElement(new Paragraph(firm.getAddr_line2(), smallFont));
            if (notBlank(firm.getCity()) || notBlank(firm.getState()))
                left.addElement(new Paragraph(
                        join(", ", firm.getCity(), firm.getState(), firm.getZip()), smallFont));
            String phoneToShow = notBlank(so.getOverridePhoneNumber())
                    ? so.getOverridePhoneNumber() : firm.getFirmContactNumber();
            if (notBlank(phoneToShow))
                left.addElement(new Paragraph("Phone: " + phoneToShow, smallFont));
            String emailToShow = notBlank(so.getOverrideEmail())
                    ? so.getOverrideEmail() : firm.getFirmEmail();
            if (notBlank(emailToShow))
                left.addElement(new Paragraph("Email: " + emailToShow, smallFont));
            if (notBlank(firm.getFirmGstNumber()))
                left.addElement(new Paragraph("GSTIN: " + firm.getFirmGstNumber(), smallFont));
            if (notBlank(firm.getFirmPanNumber()))
                left.addElement(new Paragraph("PAN: " + firm.getFirmPanNumber(), smallFont));
        } else {
            left.addElement(new Paragraph("SERVICE ORDER", companyFont));
        }
        headerTable.addCell(left);

        // RIGHT: logo
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
            log.warn("Could not load logo for SO PDF: {}", e.getMessage());
            Paragraph fb = new Paragraph(firm != null ? firm.getFirmName() : "", companyFont);
            fb.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(fb);
        }
        headerTable.addCell(right);
        document.add(headerTable);

        Paragraph title = new Paragraph("SERVICE ORDER",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(8f);
        title.setSpacingAfter(8f);
        document.add(title);
    }

    private void addSoAndVendorDetails(Document document, ServiceOrder so) throws DocumentException {
        Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{1, 1});
        mainTable.setSpacingBefore(6f);

        // LEFT: SO meta info
        PdfPCell left = new PdfPCell();
        left.setPadding(6);
        left.addElement(new Paragraph("SO No: " + so.getServiceOrderId(), normal));
        if (so.getServiceDate() != null)
            left.addElement(new Paragraph("Date: " + so.getServiceDate(), normal));
        left.addElement(new Paragraph("Status: " + so.getStatus(), normal));
        if (notBlank(so.getProjectName()))
            left.addElement(new Paragraph("Project: " + so.getProjectName(), normal));
        if (notBlank(so.getCreatedBy()))
            left.addElement(new Paragraph("Created By: " + so.getCreatedBy(), normal));
        mainTable.addCell(left);

        // RIGHT: Vendor details
        PdfPCell right = new PdfPCell();
        right.setPadding(6);
        Supplier v = so.getVendor();
        if (v != null) {
            right.addElement(new Paragraph("Vendor", bold));
            right.addElement(new Paragraph(v.getName(), bold));
            if (notBlank(v.getAddr_line1())) right.addElement(new Paragraph(v.getAddr_line1(), normal));
            if (notBlank(v.getAddr_line2())) right.addElement(new Paragraph(v.getAddr_line2(), normal));
            right.addElement(new Paragraph(join(", ", v.getCity(), v.getState(), v.getZip()), normal));
            if (notBlank(v.getContactPerson()))
                right.addElement(new Paragraph("Contact: " + v.getContactPerson(), normal));
            String phone = notBlank(v.getMobileNo()) ? v.getMobileNo() : v.getContactPersonMobileNo();
            if (notBlank(phone))
                right.addElement(new Paragraph("Phone: " + phone, normal));
        }
        mainTable.addCell(right);

        document.add(mainTable);
    }

    private void addSubject(Document document, ServiceOrder so) throws DocumentException {
        Font subjectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        if (notBlank(so.getSubject())) {
            Paragraph subject = new Paragraph("Subject: " + so.getSubject(), subjectFont);
            subject.setSpacingBefore(10f);
            subject.setSpacingAfter(6f);
            document.add(subject);
        }
    }

    private void addLinesTable(Document document, ServiceOrder so) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font smallFont  = FontFactory.getFont(FontFactory.HELVETICA, 7, new BaseColor(80, 80, 80));

        List<ServiceOrderLine> lines = so.getLines();
        if (lines == null || lines.isEmpty()) return;

        // 8 columns: #, Description, Type/Asset, Qty, Rate, Disc%, GST%, Amount
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setWidths(new float[]{0.4f, 2.5f, 1.2f, 0.6f, 1.0f, 0.6f, 0.6f, 1.1f});

        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, "Description", headerFont);
        addHeaderCell(table, "Type / Asset", headerFont);
        addHeaderCell(table, "Qty", headerFont);
        addHeaderCell(table, "Rate ₹", headerFont);
        addHeaderCell(table, "Disc %", headerFont);
        addHeaderCell(table, "GST %", headerFont);
        addHeaderCell(table, "Amount ₹", headerFont);

        int idx = 1;
        for (ServiceOrderLine line : lines) {
            addBodyCell(table, String.valueOf(idx++), normalFont);

            // Description cell — include custom fields as small key: value lines
            PdfPCell descCell = new PdfPCell();
            descCell.setPadding(3f);
            Phrase descPhrase = new Phrase(line.getDescription() != null ? line.getDescription() : "", normalFont);
            descCell.addElement(new Paragraph(descPhrase));
            List<ServiceOrderLineCustomField> cfs = line.getCustomFields();
            if (cfs != null && !cfs.isEmpty()) {
                for (ServiceOrderLineCustomField cf : cfs) {
                    if (!notBlank(cf.getFieldLabel())) continue;
                    String cfLine = cf.getFieldLabel() + ": " + (notBlank(cf.getFieldValue()) ? cf.getFieldValue() : "-");
                    descCell.addElement(new Paragraph(cfLine, smallFont));
                }
            }
            table.addCell(descCell);

            // Type / Asset
            String typeAsset = join(" / ", line.getServiceType(), line.getAssetTag());
            addBodyCell(table, notBlank(typeAsset) ? typeAsset : "-", normalFont);

            double qty = line.getQuantity() != null ? line.getQuantity() : 1.0;
            double rate = line.getRate() != null ? line.getRate() : 0.0;
            double discPct = line.getDiscountPercent() != null ? line.getDiscountPercent() : 0.0;
            double gstPct  = line.getGstPercent() != null ? line.getGstPercent() : 0.0;
            double amount  = line.getTotalAmount() != null ? line.getTotalAmount() : 0.0;

            addBodyCell(table, fmt(qty), normalFont);
            addBodyCell(table, fmt(rate), normalFont);
            addBodyCell(table, discPct > 0 ? fmt(discPct) + "%" : "-", normalFont);
            addBodyCell(table, fmt(gstPct) + "%", normalFont);

            PdfPCell amtCell = new PdfPCell(new Phrase(fmt(amount), normalFont));
            amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amtCell.setPadding(3f);
            table.addCell(amtCell);
        }

        // Lines subtotal row
        double linesTotal = lines.stream()
                .mapToDouble(l -> l.getTotalAmount() != null ? l.getTotalAmount() : 0.0)
                .sum();
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Lines Total", totalFont));
        totalLabelCell.setColspan(7);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(4f);
        table.addCell(totalLabelCell);

        PdfPCell totalValCell = new PdfPCell(new Phrase(fmt(linesTotal), totalFont));
        totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValCell.setPadding(4f);
        table.addCell(totalValCell);

        document.add(table);
    }

    private void addTotalsSection(Document document, ServiceOrder so) throws DocumentException {
        Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        double specialDiscount = so.getSpecialDiscount() != null ? so.getSpecialDiscount() : 0.0;
        double grandTotal      = so.getGrandTotal() != null ? so.getGrandTotal() : 0.0;

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(4f);

        if (specialDiscount > 0) {
            PdfPCell discLabel = new PdfPCell(new Phrase("Special Discount", normal));
            discLabel.setPadding(4f);
            table.addCell(discLabel);
            PdfPCell discVal = new PdfPCell(new Phrase("- " + fmt(specialDiscount), normal));
            discVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            discVal.setPadding(4f);
            table.addCell(discVal);
        }

        PdfPCell grandLabel = new PdfPCell(new Phrase("GRAND TOTAL ₹", bold));
        grandLabel.setPadding(4f);
        table.addCell(grandLabel);
        PdfPCell grandVal = new PdfPCell(new Phrase(fmt(grandTotal), bold));
        grandVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grandVal.setPadding(4f);
        table.addCell(grandVal);

        document.add(table);

        // Amount in words
        if (grandTotal > 0) {
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Paragraph words = new Paragraph(
                    numberToWords(grandTotal) + " RUPEES ONLY", smallFont);
            words.setSpacingBefore(2f);
            document.add(words);
        }
    }

    private void addCompletionSection(Document document, ServiceOrder so) throws DocumentException {
        List<ServiceOrderLine> lines = so.getLines();
        if (lines == null || lines.isEmpty()) return;

        boolean hasWarranty     = lines.stream().anyMatch(l -> l.getWarrantyTill() != null);
        boolean hasNextService  = lines.stream().anyMatch(l -> l.getNextServiceDate() != null);
        if (!hasWarranty && !hasNextService) return;

        Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Paragraph heading = new Paragraph("Completion Details", bold);
        heading.setSpacingBefore(10f);
        heading.setSpacingAfter(4f);
        document.add(heading);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.5f, 1.5f});

        addHeaderCell(table, "Line / Description", bold);
        addHeaderCell(table, "Warranty Till", bold);
        addHeaderCell(table, "Next Service Date", bold);

        for (ServiceOrderLine line : lines) {
            addBodyCell(table, line.getDescription() != null ? line.getDescription() : "-", normal);
            addBodyCell(table, line.getWarrantyTill() != null
                    ? DATE_FORMAT.format(line.getWarrantyTill()) : "-", normal);
            addBodyCell(table, line.getNextServiceDate() != null
                    ? DATE_FORMAT.format(line.getNextServiceDate()) : "-", normal);
        }

        if (so.getNextServiceDate() != null) {
            Font italic = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
            Paragraph nextSvc = new Paragraph(
                    "Earliest next service due: " + DATE_FORMAT.format(so.getNextServiceDate()), italic);
            nextSvc.setSpacingBefore(4f);
            document.add(table);
            document.add(nextSvc);
            return;
        }

        document.add(table);
    }

    private void addNotesSection(Document document, ServiceOrder so) throws DocumentException {
        if (!notBlank(so.getNotes())) return;

        Font labelFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        Paragraph notesTitle = new Paragraph("Notes", labelFont);
        notesTitle.setSpacingBefore(10f);
        notesTitle.setSpacingAfter(2f);
        document.add(notesTitle);

        String notes = so.getNotes().trim();
        List<String> noteLines = notes.contains("<") ? htmlToPlainLines(notes)
                : java.util.Arrays.asList(notes.split("\n"));
        for (String noteLine : noteLines) {
            if (!noteLine.trim().isEmpty())
                document.add(new Paragraph(noteLine.trim(), normalFont));
        }
    }

    private void addSignatures(Document document, ServiceOrder so) throws DocumentException {
        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        PdfPTable signTable = new PdfPTable(3);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(20f);

        // blank height rows for actual ink signatures
        for (int i = 0; i < 3; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(" ", signFont));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setMinimumHeight(30f);
            signTable.addCell(cell);
        }

        // created-by name under left column
        PdfPCell creatorCell = new PdfPCell(
                new Phrase(so.getCreatedBy() != null ? so.getCreatedBy() : "", signFont));
        creatorCell.setBorder(Rectangle.NO_BORDER);
        creatorCell.setPadding(2f);
        signTable.addCell(creatorCell);
        PdfPCell empty1 = new PdfPCell(new Phrase("", signFont)); empty1.setBorder(Rectangle.NO_BORDER);
        PdfPCell empty2 = new PdfPCell(new Phrase("", signFont)); empty2.setBorder(Rectangle.NO_BORDER);
        signTable.addCell(empty1);
        signTable.addCell(empty2);

        PdfPCell preparedBy = new PdfPCell(new Phrase("Prepared By", signFont));
        preparedBy.setBorder(Rectangle.NO_BORDER);
        preparedBy.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell checkedBy = new PdfPCell(new Phrase("", signFont));
        checkedBy.setBorder(Rectangle.NO_BORDER);

        PdfPCell authorisedBy = new PdfPCell(new Phrase("Authorised By", signFont));
        authorisedBy.setBorder(Rectangle.NO_BORDER);
        authorisedBy.setHorizontalAlignment(Element.ALIGN_RIGHT);

        signTable.addCell(preparedBy);
        signTable.addCell(checkedBy);
        signTable.addCell(authorisedBy);

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
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setPadding(3f);
        table.addCell(cell);
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

    private List<String> htmlToPlainLines(String html) {
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&ndash;", "–").replace("&mdash;", "—")
                .replace("&bull;", "•").replace("&hellip;", "…")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&#160;", " ");
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) lines.add(trimmed);
        }
        return lines;
    }

    private String numberToWords(double amount) {
        long n = Math.round(amount);
        if (n == 0) return "ZERO";
        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN",
                "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN",
                "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY",
                "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
        StringBuilder sb = new StringBuilder();
        if (n >= 10_000_000) { sb.append(twoDigits(n / 10_000_000, ones, tens)).append(" CRORE "); n %= 10_000_000; }
        if (n >= 100_000)    { sb.append(twoDigits(n / 100_000,    ones, tens)).append(" LAKH ");  n %= 100_000;    }
        if (n >= 1_000)      { sb.append(twoDigits(n / 1_000,      ones, tens)).append(" THOUSAND "); n %= 1_000;  }
        if (n >= 100)        { sb.append(ones[(int)(n / 100)]).append(" HUNDRED "); n %= 100; }
        if (n > 0)             sb.append(twoDigits(n, ones, tens));
        return sb.toString().trim();
    }

    private String twoDigits(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int)(n / 10)] + (n % 10 != 0 ? " " + ones[(int)(n % 10)] : "");
    }
}

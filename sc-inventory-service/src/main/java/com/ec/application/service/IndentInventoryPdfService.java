package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.Product;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
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
 * Generates an Indent PDF (details + line items) and appends all attachments,
 * mirroring the Purchase Order print. Attachments are merged by {@link PdfAttachmentMerger}.
 */
@Service
@UseDefaultTenant
public class IndentInventoryPdfService {

    private final Logger log = LoggerFactory.getLogger(IndentInventoryPdfService.class);

    private static final NumberFormat QTY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("en", "IN"));

    private static final java.text.SimpleDateFormat DATE_FORMAT =
            new java.text.SimpleDateFormat("dd/MM/yyyy");

    static {
        QTY_FORMAT.setMinimumFractionDigits(0);
        QTY_FORMAT.setMaximumFractionDigits(2);
    }

    @Autowired
    PdfAttachmentMerger pdfAttachmentMerger;

    public void generatePdf(IndentInventory indent, OutputStream outputStream) throws Exception {
        ByteArrayOutputStream baseOut = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baseOut);
        document.open();

        addHeader(document);
        addMetaTable(document, indent);
        addItemsTable(document, indent);

        document.close();

        pdfAttachmentMerger.appendAttachments(indent.getFileInformations(), baseOut.toByteArray(),
                outputStream, "Indent " + indent.getIndentId());
    }

    // -----------------------------------------------------------------------
    // Sections
    // -----------------------------------------------------------------------

    private void addHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        try {
            ClassPathResource logoResource = new ClassPathResource("sc-login-logo.png");
            byte[] logoBytes = org.apache.commons.io.IOUtils.toByteArray(logoResource.getInputStream());
            Image logo = Image.getInstance(logoBytes);
            logo.scaleToFit(90f, 40f);
            logo.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(logo);
        } catch (Exception e) {
            log.warn("Could not load logo for indent PDF: {}", e.getMessage());
            right.addElement(new Paragraph(" "));
        }
        headerTable.addCell(right);
        document.add(headerTable);

        Paragraph title = new Paragraph("INDENT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(8f);
        title.setSpacingAfter(8f);
        document.add(title);
    }

    private void addMetaTable(Document document, IndentInventory indent) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2f, 1f, 2f});
        table.setSpacingBefore(6f);

        int itemCount = indent.getInventoryList() != null ? indent.getInventoryList().size() : 0;
        String poNumbers = indent.getPoNumbers() != null && !indent.getPoNumbers().isEmpty()
                ? String.join(", ", indent.getPoNumbers()) : null;

        addMetaRow(table, "Indent ID", indent.getIndentId(), normalFont);
        addMetaRow(table, "Indent Date",
                indent.getIndentDate() != null ? DATE_FORMAT.format(indent.getIndentDate()) : "-", normalFont);
        addMetaRow(table, "Status", indent.getIndentStatus(), normalFont);
        addMetaRow(table, "Created By", indent.getCreatedBy(), normalFont);
        addMetaRow(table, "Project", indent.getTenant(), normalFont);
        addMetaRow(table, "Item Count", String.valueOf(itemCount), normalFont);
        if (notBlank(indent.getRequiredBy())) {
            addMetaRow(table, "Required By", indent.getRequiredBy(), normalFont);
            addMetaRow(table, "", "", normalFont);
        }
        if (poNumbers != null) {
            addMetaRow(table, "PO Number(s)", poNumbers, normalFont);
            addMetaRow(table, "", "", normalFont);
        }

        document.add(table);
    }

    private void addItemsTable(Document document, IndentInventory indent) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        List<IndentInventoryList> items = indent.getInventoryList() != null
                ? new ArrayList<>(indent.getInventoryList()) : new ArrayList<>();

        // Only show the PO column when at least one line has a PO raised against it
        boolean showPO = items.stream().anyMatch(i -> notBlank(i.getPurchaseOrderId()));
        int numCols = showPO ? 11 : 10;

        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        if (showPO) {
            table.setWidths(new float[]{0.4f, 2.2f, 1f, 0.7f, 0.6f, 2f, 1.4f, 1f, 0.8f, 0.8f, 1f});
        } else {
            table.setWidths(new float[]{0.4f, 2.2f, 1f, 0.7f, 0.6f, 2f, 1.4f, 1f, 0.8f, 0.8f});
        }

        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, "Product", headerFont);
        addHeaderCell(table, "Code", headerFont);
        addHeaderCell(table, "Qty", headerFont);
        addHeaderCell(table, "UOM", headerFont);
        addHeaderCell(table, "Specification", headerFont);
        addHeaderCell(table, "Remarks", headerFont);
        addHeaderCell(table, "Line Code", headerFont);
        addHeaderCell(table, "Qty Recd", headerFont);
        addHeaderCell(table, "Qty Pend", headerFont);
        if (showPO) addHeaderCell(table, "PO No", headerFont);

        if (items.isEmpty()) {
            PdfPCell noData = new PdfPCell(new Phrase("No line items", normalFont));
            noData.setColspan(numCols);
            noData.setPadding(6f);
            noData.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(noData);
        } else {
            int idx = 1;
            for (IndentInventoryList item : items) {
                Product p = item.getProduct();
                addBodyCell(table, String.valueOf(idx++), normalFont);
                addBodyCell(table, p != null && notBlank(p.getProductName()) ? p.getProductName() : "-", normalFont);
                addBodyCell(table, p != null && notBlank(p.getProductCode()) ? p.getProductCode() : "-", normalFont);
                addBodyCell(table, item.getQuantity() != null ? fmt(item.getQuantity()) : "-", normalFont);
                addBodyCell(table, notBlank(item.getMeasurementUnit()) ? item.getMeasurementUnit() : "-", normalFont);
                addBodyCell(table, notBlank(item.getSpecification()) ? item.getSpecification() : "-", normalFont);
                addBodyCell(table, notBlank(item.getRemarks()) ? item.getRemarks() : "-", normalFont);
                addBodyCell(table, notBlank(item.getLineItemCode()) ? item.getLineItemCode() : "-", normalFont);
                addBodyCell(table, item.getQuantityReceived() != null ? fmt(item.getQuantityReceived()) : "-", normalFont);
                addBodyCell(table, item.getQuantityPending() != null ? fmt(item.getQuantityPending()) : "-", normalFont);
                if (showPO) addBodyCell(table, notBlank(item.getPurchaseOrderId()) ? item.getPurchaseOrderId() : "-", normalFont);
            }
        }

        document.add(table);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void addMetaRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        labelCell.setPadding(4f);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        if (label == null || label.isEmpty()) labelCell.setBackgroundColor(BaseColor.WHITE);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", font));
        valueCell.setPadding(4f);
        table.addCell(valueCell);
    }

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

    private String fmt(double value) {
        return QTY_FORMAT.format(value);
    }
}

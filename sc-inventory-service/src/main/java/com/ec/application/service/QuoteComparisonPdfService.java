package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.QuoteComparison;
import com.ec.application.model.QuoteComparisonLine;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Generates a Request-for-Quote PDF from a QuoteComparison's demand lines —
 * the document a buyer sends out to vendors asking them to quote.
 * Carries no rate/cost data since none exists yet at RFQ stage.
 */
@Service
@UseDefaultTenant
public class QuoteComparisonPdfService {

    private final Logger log = LoggerFactory.getLogger(QuoteComparisonPdfService.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public void generateRfqPdf(QuoteComparison qc, List<QuoteComparisonLine> lines, OutputStream outputStream)
            throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        addHeader(document, qc);
        addIntro(document, qc);
        addLinesTable(document, lines);
        addFooterNote(document);

        document.close();
    }

    private void addHeader(Document document, QuoteComparison qc) throws DocumentException {
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.BLACK);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);

        // Branded letterhead row — logo on the right, same as the Purchase Order PDF.
        PdfPTable brandTable = new PdfPTable(2);
        brandTable.setWidthPercentage(100);
        brandTable.setWidths(new float[]{3, 1});

        PdfPCell left = new PdfPCell(new Phrase(" ", normalFont));
        left.setBorder(Rectangle.NO_BORDER);
        brandTable.addCell(left);

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
            log.warn("Could not load logo for RFQ PDF: {}", e.getMessage());
            Paragraph fb = new Paragraph("Mahavir Group", companyFont);
            fb.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(fb);
        }
        brandTable.addCell(right);
        document.add(brandTable);

        Paragraph title = new Paragraph("REQUEST FOR QUOTATION", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(8f);
        title.setSpacingAfter(10f);
        document.add(title);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1f, 1f});
        infoTable.setSpacingAfter(10f);

        addInfoCell(infoTable, "RFQ Reference", qc.getQcId(), normalFont);
        addInfoCell(infoTable, "Date", qc.getComparisonDate() != null ? DATE_FORMAT.format(qc.getComparisonDate()) : "-", normalFont);
        addInfoCell(infoTable, "Title", qc.getTitle(), normalFont);
        addInfoCell(infoTable, "Project", notBlank(qc.getProject()) ? qc.getProject() : "-", normalFont);

        document.add(infoTable);
    }

    private void addIntro(Document document, QuoteComparison qc) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
        document.add(new Paragraph("Dear Sir/Madam,", normalFont));
        Paragraph intro = new Paragraph(
                "We are inviting your best quotation for the items listed below. "
                        + "Please submit your rates along with applicable taxes, freight/delivery charges, "
                        + "payment terms, delivery lead time and quotation validity at the earliest.",
                normalFont);
        intro.setSpacingBefore(4f);
        intro.setSpacingAfter(10f);
        document.add(intro);

        if (notBlank(qc.getNotes())) {
            Paragraph notes = new Paragraph("Notes: " + qc.getNotes(), normalFont);
            notes.setSpacingAfter(10f);
            document.add(notes);
        }
    }

    private void addLinesTable(Document document, List<QuoteComparisonLine> lines) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.4f, 2.5f, 0.8f, 0.8f, 2f});
        table.setSpacingBefore(4f);

        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, "Product", headerFont);
        addHeaderCell(table, "Qty", headerFont);
        addHeaderCell(table, "Unit", headerFont);
        addHeaderCell(table, "Specification / Need By", headerFont);

        int i = 1;
        for (QuoteComparisonLine line : lines) {
            addBodyCell(table, String.valueOf(i++), normalFont);
            addBodyCell(table, line.getProductName() != null ? line.getProductName() : "-", normalFont);
            addBodyCell(table, line.getRequiredQty() != null ? fmtQty(line.getRequiredQty()) : "-", normalFont);
            addBodyCell(table, notBlank(line.getUnit()) ? line.getUnit() : "-", normalFont);

            String specAndNeedBy = "";
            if (notBlank(line.getSpecifications())) specAndNeedBy = line.getSpecifications();
            if (line.getNeedByDate() != null) {
                specAndNeedBy += (specAndNeedBy.isEmpty() ? "" : "\n") + "Need by: " + DATE_FORMAT.format(line.getNeedByDate());
            }
            addBodyCell(table, specAndNeedBy.isEmpty() ? "-" : specAndNeedBy, normalFont);
        }

        document.add(table);
    }

    private void addFooterNote(Document document) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
        Paragraph note = new Paragraph(
                "This is a request for quotation only and does not constitute a purchase order or commitment to purchase.",
                normalFont);
        note.setSpacingBefore(14f);
        document.add(note);
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font font) {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3f);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", boldFont));
        p.add(new Chunk(value != null ? value : "-", font));
        cell.addElement(p);
        table.addCell(cell);
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
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String fmtQty(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((long) qty) : String.valueOf(qty);
    }
}

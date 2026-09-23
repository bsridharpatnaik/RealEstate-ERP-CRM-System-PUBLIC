package com.ec.application.service;

import com.ec.application.model.DBFile;
import com.ec.application.model.FileInformation;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Merges document attachments onto a base PDF: PDFs are appended as-is, images are
 * rendered onto a full A4 page, other types are skipped. Shared by PO and Indent print.
 */
@Service
public class PdfAttachmentMerger {

    private final Logger log = LoggerFactory.getLogger(PdfAttachmentMerger.class);

    @Autowired
    DBFileStorageService dbFileStorageService;

    /**
     * Writes basePdfBytes to outputStream, then appends each attachment. If there are no
     * attachments the base PDF is written through unchanged.
     *
     * @param attachments  files to append (may be null/empty)
     * @param basePdfBytes the already-rendered base document
     * @param outputStream target stream
     * @param label        context for logging (e.g. "PO PO-123", "Indent IND-45")
     */
    public void appendAttachments(Collection<FileInformation> attachments, byte[] basePdfBytes,
                                  OutputStream outputStream, String label) throws Exception {
        List<FileInformation> files = attachments == null ? Collections.emptyList()
                : attachments.stream()
                        .sorted(Comparator.comparing(FileInformation::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toList());

        if (files.isEmpty()) {
            outputStream.write(basePdfBytes);
            return;
        }

        PdfReader baseReader = new PdfReader(basePdfBytes);
        Document mergedDoc = new Document(baseReader.getPageSizeWithRotation(1));
        PdfCopy copy = new PdfCopy(mergedDoc, outputStream);
        mergedDoc.open();
        copy.addDocument(baseReader);
        baseReader.close();

        for (FileInformation fi : files) {
            try {
                appendOneAttachment(fi.getFileUUId(), copy);
            } catch (Exception e) {
                log.warn("Skipping attachment {} for {} — failed to merge: {}",
                        fi.getFileUUId(), label, e.getMessage());
            }
        }

        mergedDoc.close();
    }

    private void appendOneAttachment(String fileUUId, PdfCopy copy) throws Exception {
        DBFile dbFile = dbFileStorageService.getFile(fileUUId);
        byte[] bytes = dbFileStorageService.getFileBytes(fileUUId);
        String fileType = dbFile.getFileType() == null ? "" : dbFile.getFileType().toLowerCase();

        if (fileType.contains("pdf")) {
            PdfReader attachmentReader = new PdfReader(bytes);
            copy.addDocument(attachmentReader);
            attachmentReader.close();
        } else if (fileType.startsWith("image")) {
            byte[] imagePagePdf = imageToPdfPage(bytes);
            PdfReader imageReader = new PdfReader(imagePagePdf);
            copy.addDocument(imageReader);
            imageReader.close();
        } else {
            log.warn("Unsupported attachment type '{}' for file {} — skipped", fileType, fileUUId);
        }
    }

    private byte[] imageToPdfPage(byte[] imageBytes) throws Exception {
        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        Document imgDoc = new Document(PageSize.A4, 18, 18, 18, 18);
        PdfWriter.getInstance(imgDoc, imgOut);
        imgDoc.open();

        Image image = Image.getInstance(imageBytes);
        float maxWidth = imgDoc.getPageSize().getWidth() - imgDoc.leftMargin() - imgDoc.rightMargin();
        float maxHeight = imgDoc.getPageSize().getHeight() - imgDoc.topMargin() - imgDoc.bottomMargin();
        image.scaleToFit(maxWidth, maxHeight);
        image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_MIDDLE);
        imgDoc.add(image);

        imgDoc.close();
        return imgOut.toByteArray();
    }
}

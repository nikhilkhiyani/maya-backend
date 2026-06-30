package com.MAYA.studio.service;

import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class InvoiceService {

    @Value("${file.upload-base}")
    private String uploadBase;

    @Value("${app.name:MAYA}")
    private String appName;

    public byte[] generateInvoicePdf(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(26, 34, 56));
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(26, 34, 56));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);

            Paragraph brand = new Paragraph(appName, titleFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);
            document.add(new Paragraph("Luxury Indo-Latin Fusion Wear", smallFont));
            document.add(Chunk.NEWLINE);

            Paragraph invoiceTitle = new Paragraph("TAX INVOICE", headerFont);
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceTitle);
            document.add(Chunk.NEWLINE);

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(cell("Invoice No: " + order.getInvoiceNumber(), normalFont));
            meta.addCell(cell("Order No: " + order.getOrderNumber(), normalFont));
            LocalDateTime invoiceDate = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            meta.addCell(cell("Date: " + invoiceDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), normalFont));
            meta.addCell(cell("Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")", normalFont));
            document.add(meta);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Bill To", headerFont));
            document.add(new Paragraph(order.getBillingFullName() != null ? order.getBillingFullName() : order.getShippingFullName(), normalFont));
            document.add(new Paragraph(formatAddress(order.getBillingAddressLine1(), order.getBillingCity(), order.getBillingState(), order.getBillingPincode()), normalFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Ship To", headerFont));
            document.add(new Paragraph(order.getShippingFullName(), normalFont));
            document.add(new Paragraph(formatAddress(order.getShippingAddressLine1(), order.getShippingCity(), order.getShippingState(), order.getShippingPincode()), normalFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1, 2, 2});
            table.addCell(headerCell("Product", headerFont));
            table.addCell(headerCell("Qty", headerFont));
            table.addCell(headerCell("Price", headerFont));
            table.addCell(headerCell("Total", headerFont));

            for (OrderItem item : order.getOrderItems()) {
                table.addCell(cell(item.getProduct().getName(), normalFont));
                table.addCell(cell(String.valueOf(item.getQuantity()), normalFont));
                table.addCell(cell("₹" + item.getPrice(), normalFont));
                table.addCell(cell("₹" + item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())), normalFont));
            }
            document.add(table);
            document.add(Chunk.NEWLINE);

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(50);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.addCell(cell("Shipping", normalFont));
            totals.addCell(cell("₹" + order.getShippingAmount(), normalFont));
            totals.addCell(cell("GST", normalFont));
            totals.addCell(cell("₹" + order.getTaxAmount(), normalFont));
            if (order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totals.addCell(cell("Discount", normalFont));
                totals.addCell(cell("-₹" + order.getDiscountAmount(), normalFont));
            }
            totals.addCell(cell("Grand Total", headerFont));
            totals.addCell(cell("₹" + order.getTotalAmount(), headerFont));
            document.add(totals);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Terms: All sales are subject to MAYA terms and conditions. Exchanges are permitted only for defective, damaged, or incorrect items within the policy window.", smallFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Support: support@maya.com | +91 98765 43210", smallFont));

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void generateAndStoreInvoice(Order order) {
        try {
            byte[] pdf = generateInvoicePdf(order);
            Path dir = Paths.get(uploadBase, "invoices");
            Files.createDirectories(dir);
            Path file = dir.resolve(order.getInvoiceNumber() + ".pdf");
            Files.write(file, pdf);
            log.info("Invoice stored: {}", file);
        } catch (IOException e) {
            log.error("Failed to store invoice: {}", e.getMessage());
        }
    }

    public byte[] getInvoicePdf(Order order) {
        try {
            Path file = Paths.get(uploadBase, "invoices", order.getInvoiceNumber() + ".pdf");
            if (Files.exists(file)) return Files.readAllBytes(file);
        } catch (IOException ignored) {}
        return generateInvoicePdf(order);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(248, 245, 240));
        cell.setPadding(6);
        return cell;
    }

    private String formatAddress(String line1, String city, String state, String pincode) {
        return line1 + ", " + city + ", " + state + " - " + pincode;
    }
}

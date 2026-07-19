package com.example.rapiffy.impl;

import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.lowagie.text.DocumentException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generate(InvoiceResponse invoice) {
        try {
            String template = loadTemplate();
            String html = populateTemplate(template, invoice);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF: " + e.getMessage(), e);
        }
    }

    private String loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/invoice.html");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String populateTemplate(String template, InvoiceResponse inv) {
        // Build item rows
        StringBuilder itemRows = new StringBuilder();
        int i = 1;
        for (OrderItemResponse item : inv.getItems()) {
            itemRows.append("<tr>")
                .append("<td>").append(i++).append("</td>")
                .append("<td class='left'>").append(item.getProductName())
                .append(item.getBrand() != null ? "<br/><small>" + item.getBrand() + "</small>" : "")
                .append(item.getUnit() != null ? "<br/><small>" + item.getUnitValue() + " " + item.getUnit() + "</small>" : "")
                .append("</td>")
                .append("<td>").append(item.getQuantity()).append("</td>")
                .append("<td>&#8377;").append(item.getSellingPrice()).append("</td>")
                .append("<td>").append(safe(item.getGstSlab(), "-")).append("</td>")
                .append("<td>&#8377;").append(item.getLineTotal()).append("</td>")
                .append("</tr>");
        }

        return template
            .replace("{{invoiceId}}", safe(inv.getInvoiceId(), ""))
            .replace("{{invoiceDate}}", inv.getInvoiceDate().format(DATE_FMT))
            .replace("{{orderNumber}}", safe(inv.getOrderNumber(), ""))
            .replace("{{shopName}}", safe(inv.getShopName(), ""))
            .replace("{{shopAddress}}", safe(inv.getShopAddress(), ""))
            .replace("{{shopGst}}", inv.getShopGstNumber() != null ? "GST: " + inv.getShopGstNumber() : "")
            .replace("{{shopPhone}}", inv.getShopPhone() != null ? "Phone: " + inv.getShopPhone() : "")
            .replace("{{customerName}}", safe(inv.getCustomerName(), ""))
            .replace("{{customerPhone}}", safe(inv.getCustomerPhone(), ""))
            .replace("{{deliveryAddress}}", safe(inv.getDeliveryAddress(), ""))
            .replace("{{itemRows}}", itemRows.toString())
            .replace("{{subtotal}}", String.valueOf(inv.getSubtotal()))
            .replace("{{totalGst}}", inv.getTotalGst() != null ? String.valueOf(inv.getTotalGst()) : "0.0")
            .replace("{{deliveryCharge}}", inv.getDeliveryCharge() != null ? String.valueOf(inv.getDeliveryCharge()) : "0.0")
            .replace("{{totalAmount}}", String.valueOf(inv.getTotalAmount()));
    }

    private String safe(String value, String fallback) {
        return value != null ? value : fallback;
    }
}

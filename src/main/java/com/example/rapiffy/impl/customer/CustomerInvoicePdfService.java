package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.CustomerInvoiceResponse;
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
public class CustomerInvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generate(CustomerInvoiceResponse invoice) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/customer-invoice.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String html = populate(template, invoice);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate customer invoice PDF: " + e.getMessage(), e);
        }
    }

    private String populate(String template, CustomerInvoiceResponse inv) {
        // Build one section per shop
        StringBuilder shopSections = new StringBuilder();
        int totalItems = 0;

        for (CustomerInvoiceResponse.ShopInvoiceSection shop : inv.getShops()) {
            shopSections.append("<div class='shop-section'>")
                    .append("<div class='shop-title'>").append(safe(shop.getShopName())).append("</div>");

            shopSections.append("<table class='items'>")
                    .append("<thead><tr>")
                    .append("<th>#</th><th>Product</th><th>Qty</th><th>Price</th><th>GST</th><th>Total</th>")
                    .append("</tr></thead><tbody>");

            int i = 1;
            for (OrderItemResponse item : shop.getItems()) {
                shopSections.append("<tr>")
                        .append("<td>").append(i++).append("</td>")
                        .append("<td class='left'>").append(safe(item.getProductName()))
                        .append(item.getBrand() != null ? "<br/><small>" + item.getBrand() + "</small>" : "")
                        .append(item.getUnit() != null ? "<br/><small>" + item.getUnitValue() + " " + item.getUnit() + "</small>" : "")
                        .append("</td>")
                        .append("<td>").append(item.getQuantity()).append("</td>")
                        .append("<td>&#8377;").append(item.getSellingPrice()).append("</td>")
                        .append("<td>").append(item.getGstSlab() != null ? item.getGstSlab() : "-").append("</td>")
                        .append("<td>&#8377;").append(item.getLineTotal()).append("</td>")
                        .append("</tr>");
                totalItems++;
            }

            shopSections.append("</tbody></table>")
                    .append("<div class='shop-subtotal'>Shop Total: <span>&#8377;").append(shop.getShopTotal()).append("</span></div>")
                    .append("</div>");
        }

        return template
                .replace("{{orderNumber}}", safe(inv.getOrderNumber()))
                .replace("{{orderDate}}", inv.getOrderDate() != null ? inv.getOrderDate().format(DATE_FMT) : "")
                .replace("{{customerName}}", safe(inv.getCustomerPhone()))
                .replace("{{customerPhone}}", safe(inv.getCustomerPhone()))
                .replace("{{deliveryAddress}}", safe(inv.getDeliveryAddress()))
                .replace("{{deliveryType}}", safe(inv.getDeliveryType()))
                .replace("{{totalShops}}", String.valueOf(inv.getShops().size()))
                .replace("{{totalItems}}", String.valueOf(totalItems))
                .replace("{{shopSections}}", shopSections.toString())
                .replace("{{subtotal}}", String.valueOf(inv.getSubtotal()))
                .replace("{{totalGst}}", inv.getTotalGst() != null ? String.valueOf(inv.getTotalGst()) : "0.0")
                .replace("{{deliveryCharge}}", inv.getDeliveryCharge() != null ? String.valueOf(inv.getDeliveryCharge()) : "0.0")
                .replace("{{totalAmount}}", String.valueOf(inv.getTotalAmount()));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}

package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.CustomerInvoiceResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.common.RapiffyBrand;
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
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

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
        CustomerInvoiceResponse.ShopInvoiceSection shop = inv.getShops().get(0);

        // ── PAGE 1: Item table rows ───────────────────────────────────────────
        StringBuilder itemRows = new StringBuilder();
        int sr = 1;
        for (OrderItemResponse item : shop.getItems()) {
            double netAmount = item.getSellingPrice() * item.getQuantity();
            String gstType = resolveGstType(safe(shop.getShopState()), safe(inv.getPlaceOfDelivery()));
            itemRows.append("<tr>")
                    .append("<td>").append(sr++).append("</td>")
                    .append("<td class='left'>").append(safe(item.getProductName()))
                    .append(item.getBrand() != null ? "<br/><small>" + item.getBrand() + "</small>" : "")
                    .append(item.getUnitValue() != null ? "<br/><small>" + item.getUnitValue() + " " + safe(item.getUnit()) + "</small>" : "")
                    .append("</td>")
                    .append("<td>&#8377;").append(fmt(item.getSellingPrice())).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>&#8377;").append(fmt(netAmount)).append("</td>")
                    .append("<td>").append(safe(item.getGstSlab())).append("</td>")
                    .append("<td>").append(gstType).append("</td>")
                    .append("<td>&#8377;").append(fmt(item.getGstAmount() != null ? item.getGstAmount() : 0.0)).append("</td>")
                    .append("<td>&#8377;").append(fmt(item.getLineTotal())).append("</td>")
                    .append("</tr>");
        }

        // ── PAGE 2: Platform fee row ──────────────────────────────────────────
        double platformFee      = inv.getPlatformFee() != null ? inv.getPlatformFee() : 0.0;
        double platformFeeGst   = inv.getPlatformFeeGst() != null ? inv.getPlatformFeeGst() : 0.0;
        double platformFeeTotal = inv.getPlatformFeeTotal() != null ? inv.getPlatformFeeTotal() : 0.0;

        String platformFeeRow = "<tr>"
                + "<td>1</td>"
                + "<td class='left'>Marketplace fee</td>"
                + "<td>&#8377;" + fmt(platformFee) + "</td>"
                + "<td>&#8377;" + fmt(platformFee) + "</td>"
                + "<td>18%</td>"
                + "<td>IGST</td>"
                + "<td>&#8377;" + fmt(platformFeeGst) + "</td>"
                + "<td>&#8377;" + fmt(platformFeeTotal) + "</td>"
                + "</tr>";

        return template
                .replace("{{logo}}", RapiffyBrand.LOGO_HTML)
                // Shop (left side)
                .replace("{{shopName}}", safe(shop.getShopName()))
                .replace("{{shopAddress}}", safe(shop.getShopAddress()))
                .replace("{{shopPan}}", safe(shop.getShopPan()))
                .replace("{{shopGst}}", safe(shop.getShopGstNumber()))
                .replace("{{orderNumber}}", safe(inv.getOrderNumber()))
                .replace("{{orderDate}}", inv.getOrderDate() != null ? inv.getOrderDate().format(DATE_FMT) : "")
                // Billing (right side)
                .replace("{{billingAddress}}", safe(inv.getDeliveryAddress()))
                .replace("{{placeOfSupply}}", safe(inv.getPlaceOfSupply()))
                .replace("{{placeOfDelivery}}", safe(inv.getPlaceOfDelivery()))
                .replace("{{invoiceNumber}}", safe(inv.getInvoiceNumber()))
                .replace("{{invoiceDate}}", inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATETIME_FMT) : "")
                // Items
                .replace("{{itemRows}}", itemRows.toString())
                .replace("{{totalTaxAmount}}", fmt(inv.getTotalGst() != null ? inv.getTotalGst() : 0.0))
                .replace("{{totalAmount}}", fmt(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0))
                .replace("{{amountInWords}}", toWords(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0))
                // Page 2
                .replace("{{platformFeeRow}}", platformFeeRow)
                .replace("{{platformFeeTaxAmount}}", fmt(platformFeeGst))
                .replace("{{platformFeeTotal}}", fmt(platformFeeTotal))
                .replace("{{platformFeeInWords}}", toWords(platformFeeTotal))
                .replace("{{txnId}}", safe(inv.getTxnId()));
    }

    // IGST if inter-state, CGST+SGST if intra-state
    private String resolveGstType(String shopState, String deliveryState) {
        if (shopState.isBlank() || deliveryState.isBlank()) return "IGST";
        return shopState.equalsIgnoreCase(deliveryState) ? "CGST + SGST" : "IGST";
    }

    private String fmt(double value) {
        return String.format("%.2f", value);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    // Simple amount-to-words (handles up to crores)
    private String toWords(double amount) {
        long rupees = (long) amount;
        long paise  = Math.round((amount - rupees) * 100);
        String result = rupeesToWords(rupees);
        if (paise > 0) result += " and " + rupeesToWords(paise) + " Paise";
        return result + " Only";
    }

    private String rupeesToWords(long n) {
        if (n == 0) return "Zero";
        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        if (n < 20)       return ones[(int) n];
        if (n < 100)      return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + ones[(int) (n % 10)] : "");
        if (n < 1000)     return ones[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + rupeesToWords(n % 100) : "");
        if (n < 100000)   return rupeesToWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + rupeesToWords(n % 1000) : "");
        if (n < 10000000) return rupeesToWords(n / 100000) + " Lakh" + (n % 100000 != 0 ? " " + rupeesToWords(n % 100000) : "");
        return rupeesToWords(n / 10000000) + " Crore" + (n % 10000000 != 0 ? " " + rupeesToWords(n % 10000000) : "");
    }
}

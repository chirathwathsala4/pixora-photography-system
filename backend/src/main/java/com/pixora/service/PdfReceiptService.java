package com.pixora.service;

import com.pixora.entity.Booking;
import com.pixora.entity.Payment;
import com.pixora.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PdfReceiptService {

    private final PaymentRepository paymentRepository;

    public byte[] generateReceipt(Booking booking) throws IOException {
        Optional<Payment> paymentOpt = paymentRepository.findByBookingBookingId(booking.getBookingId());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float margin = 50;
                float yStart = 780;
                float y = yStart;

                // Dark background header
                cs.setNonStrokingColor(new Color(10, 10, 10));
                cs.addRect(0, 720, pageWidth, 120);
                cs.fill();

                // Gold accent bar
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.addRect(0, 718, pageWidth, 4);
                cs.fill();

                // PIXORA title
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 28);
                cs.newLineAtOffset(margin, 760);
                cs.showText("PIXORA");
                cs.endText();

                // Subtitle
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, 742);
                cs.showText("Your Moments, Our Frame  |  Birthday Event Photography");
                cs.endText();

                // RECEIPT label on right
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(pageWidth - margin - 100, 760);
                cs.showText("RECEIPT");
                cs.endText();

                y = 700;

                // Invoice number
                cs.setNonStrokingColor(new Color(50, 50, 50));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice Number: PIXORA-" + String.format("%04d", booking.getBookingId()));
                cs.endText();

                // Section divider
                y -= 20;
                cs.setStrokingColor(new Color(212, 175, 55));
                cs.setLineWidth(1f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                y -= 30;

                // Client Details
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("CLIENT DETAILS");
                cs.endText();

                y -= 20;
                cs.setNonStrokingColor(Color.BLACK);
                drawLabelValue(cs, margin, y, "Name:", booking.getClient().getFullName());
                y -= 18;
                drawLabelValue(cs, margin, y, "Email:", booking.getClient().getEmail());
                y -= 18;
                if (booking.getClient().getPhone() != null) {
                    drawLabelValue(cs, margin, y, "Phone:", booking.getClient().getPhone());
                    y -= 18;
                }

                y -= 10;
                cs.setNonStrokingColor(new Color(220, 220, 220));
                cs.addRect(margin, y, pageWidth - 2 * margin, 1);
                cs.fill();
                y -= 20;

                // Event Details
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("EVENT DETAILS");
                cs.endText();

                y -= 20;
                cs.setNonStrokingColor(Color.BLACK);
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                drawLabelValue(cs, margin, y, "Event Date:", booking.getEventDate().format(dateFmt));
                y -= 18;
                drawLabelValue(cs, margin, y, "Event Time:", booking.getEventTime().toString());
                y -= 18;
                drawLabelValue(cs, margin, y, "Venue:", booking.getVenueAddress());
                y -= 18;
                if (booking.getPhotographer() != null) {
                    drawLabelValue(cs, margin, y, "Photographer:", booking.getPhotographer().getFullName());
                    y -= 18;
                }

                y -= 10;
                cs.setNonStrokingColor(new Color(220, 220, 220));
                cs.addRect(margin, y, pageWidth - 2 * margin, 1);
                cs.fill();
                y -= 20;

                // Package & Itemized Breakdown
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("ITEMIZED BILLING & INCLUSIONS");
                cs.endText();

                y -= 20;
                cs.setNonStrokingColor(Color.BLACK);
                NumberFormat nf = NumberFormat.getInstance(new Locale("en", "LK"));
                drawLabelValue(cs, margin, y, "Package:", booking.getPkg().getPackageName() + " (Rs. " + nf.format(booking.getPkg().getPriceLkr()) + ")");
                y -= 18;

                if (booking.getAddons() != null && !booking.getAddons().isBlank()) {
                    drawLabelValue(cs, margin, y, "Selected Add-ons:", booking.getAddons());
                    y -= 18;
                }

                if (booking.getDeliveryTier() != null && !booking.getDeliveryTier().isBlank()) {
                    String tierText = booking.getDeliveryTier();
                    if ("EXPRESS_48H".equals(tierText)) tierText = "Express Delivery (48 Hours)";
                    else if ("ULTRA_24H".equals(tierText)) tierText = "VIP Ultra Fast Delivery (24 Hours)";
                    else if ("STANDARD".equals(tierText)) tierText = "Standard Gallery Delivery (7 Days)";

                    if (booking.getDeliveryFeeLkr() != null && booking.getDeliveryFeeLkr().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        tierText += " (+Rs. " + nf.format(booking.getDeliveryFeeLkr()) + ")";
                    }
                    drawLabelValue(cs, margin, y, "Delivery Tier:", tierText);
                    y -= 18;
                }

                if (booking.getDiscountAmountLkr() != null && booking.getDiscountAmountLkr().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    String promoText = "- Rs. " + nf.format(booking.getDiscountAmountLkr());
                    if (booking.getPromoCode() != null && !booking.getPromoCode().isBlank()) {
                        promoText += " (Code: " + booking.getPromoCode() + ")";
                    }
                    drawLabelValue(cs, margin, y, "Promo Discount:", promoText);
                    y -= 18;
                }

                // Total Amount Box
                y -= 15;
                cs.setNonStrokingColor(new Color(15, 15, 18));
                cs.addRect(margin, y - 10, pageWidth - 2 * margin, 38);
                cs.fill();

                String totalStr = "Rs. " + nf.format(booking.getTotalAmountLkr());
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(margin + 12, y + 10);
                cs.showText("TOTAL AMOUNT: " + totalStr);
                cs.endText();

                y -= 45;

                // Payment Info
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    cs.setNonStrokingColor(new Color(212, 175, 55));
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("PAYMENT VERIFICATION");
                    cs.endText();

                    y -= 20;
                    cs.setNonStrokingColor(Color.BLACK);
                    drawLabelValue(cs, margin, y, "Transaction Ref:", payment.getTransactionRef());
                    y -= 18;
                    drawLabelValue(cs, margin, y, "Amount Paid:", "Rs. " + nf.format(payment.getAmountPaidLkr()));
                    y -= 18;
                    drawLabelValue(cs, margin, y, "Payment Status:", payment.getPaymentStatus().name());
                    y -= 18;
                }

                // Status
                y -= 15;
                String statusStr = "Official Status: " + booking.getStatus().name().replace("_", " ") + "  |  VERIFIED LUXURY RECEIPT";
                cs.setNonStrokingColor(new Color(22, 101, 52));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(statusStr);
                cs.endText();

                // Footer
                cs.setNonStrokingColor(new Color(212, 175, 55));
                cs.addRect(0, 30, pageWidth, 2);
                cs.fill();

                cs.setNonStrokingColor(new Color(100, 100, 100));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.newLineAtOffset(margin, 18);
                cs.showText("Pixora Photography  |  admin@pixora.lk  |  Thank you for choosing Pixora!");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void drawLabelValue(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.setNonStrokingColor(new Color(80, 80, 80));
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.setNonStrokingColor(Color.BLACK);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(x + 130, y);
        cs.showText(value != null ? value : "N/A");
        cs.endText();
    }
}

package com.MAYA.studio.service;

import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.Payment;
import com.MAYA.studio.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final InvoiceService invoiceService;

    @Value("${app.mail.from:noreply@maya.com}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.name:MAYA}")
    private String appName;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.admin-email:admin@maya.com}")
    private String adminEmail;

    @Async
    public void sendWelcomeEmail(User user) {
        send(user.getEmail(), "Welcome to " + appName,
                template("Welcome to " + appName, "Dear " + user.getName() + ",",
                        "Thank you for joining " + appName + ". Discover our curated collection of luxury Indo-Latin fusion wear.",
                        "Start Shopping", frontendUrl + "/shop"));
    }

    @Async
    public void sendOrderPlacedEmail(User user, Order order) {
        byte[] invoice = invoiceService.getInvoicePdf(order);
        sendWithAttachment(user.getEmail(), "Order Confirmed - " + order.getOrderNumber(),
                template("Your Order is Confirmed", "Dear " + user.getName() + ",",
                        "Thank you for your order <strong>" + order.getOrderNumber() + "</strong>. Total: ₹" + order.getTotalAmount() + ".",
                        "Track Order", frontendUrl + "/orders/" + order.getId()),
                order.getInvoiceNumber() + ".pdf", invoice);
    }

    @Async
    public void sendPaymentConfirmationEmail(User user, Order order, Payment payment) {
        send(user.getEmail(), "Payment Received - " + order.getOrderNumber(),
                template("Payment Confirmed", "Dear " + user.getName() + ",",
                        "We have received your payment of ₹" + payment.getAmount() + " for order <strong>" + order.getOrderNumber() + "</strong>.",
                        "View Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendPaymentFailedEmail(User user, Payment payment) {
        send(user.getEmail(), "Payment Verification Failed",
                template("Payment Not Verified", "Dear " + user.getName() + ",",
                        "We could not verify your payment. Please contact support or try again.",
                        "Contact Support", frontendUrl + "/contact"));
    }

    @Async
    public void sendOrderStatusEmail(User user, Order order) {
        send(user.getEmail(), "Order Update - " + order.getOrderNumber(),
                template("Order Status Updated", "Dear " + user.getName() + ",",
                        "Your order <strong>" + order.getOrderNumber() + "</strong> is now <strong>" +
                                order.getStatus().name().replace('_', ' ') + "</strong>.",
                        "View Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendShipmentConfirmationEmail(User user, Order order) {
        send(user.getEmail(), "Shipment Confirmed - " + order.getOrderNumber(),
                template("Your Order Has Shipped", "Dear " + user.getName() + ",",
                        "Order <strong>" + order.getOrderNumber() + "</strong> has been shipped via " +
                                (order.getCourierName() != null ? order.getCourierName() : "our courier") +
                                ". Tracking: " + order.getTrackingNumber(),
                        "Track Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendShipmentUpdateEmail(User user, Order order) {
        sendOrderStatusEmail(user, order);
    }

    @Async
    public void sendOutForDeliveryEmail(User user, Order order) {
        send(user.getEmail(), "Out for Delivery - " + order.getOrderNumber(),
                template("Out for Delivery", "Dear " + user.getName() + ",",
                        "Your order <strong>" + order.getOrderNumber() + "</strong> is out for delivery today.",
                        "Track Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendDeliveredEmail(User user, Order order) {
        send(user.getEmail(), "Delivered - " + order.getOrderNumber(),
                template("Order Delivered", "Dear " + user.getName() + ",",
                        "Your order <strong>" + order.getOrderNumber() + "</strong> has been delivered. We hope you love your purchase!",
                        "Leave a Review", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendRefundInitiatedEmail(User user, Order order) {
        send(user.getEmail(), "Refund Initiated - " + order.getOrderNumber(),
                template("Refund Initiated", "Dear " + user.getName() + ",",
                        "A refund for order <strong>" + order.getOrderNumber() + "</strong> has been initiated. It will be processed within 2–5 business days.",
                        "View Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendRefundCompletedEmail(User user, Order order) {
        send(user.getEmail(), "Refund Completed - " + order.getOrderNumber(),
                template("Refund Completed", "Dear " + user.getName() + ",",
                        "Your refund for order <strong>" + order.getOrderNumber() + "</strong> has been completed.",
                        "View Order", frontendUrl + "/orders/" + order.getId()));
    }

    @Async
    public void sendAdminNewOrderEmail(Order order) {
        send(adminEmail, "New Order - " + order.getOrderNumber(),
                template("New Order Received", "Admin,",
                        "Order <strong>" + order.getOrderNumber() + "</strong> worth ₹" + order.getTotalAmount() + " has been placed.",
                        "View in Admin", frontendUrl + "/admin/orders"));
    }

    @Async
    public void sendExchangeAcknowledgment(User user, String orderNumber) {
        send(user.getEmail(), "Exchange Request Received - " + orderNumber,
                template("Exchange Request Received", "Dear " + user.getName() + ",",
                        "We have received your exchange request for order <strong>" + orderNumber + "</strong>. Our team will review it shortly.",
                        "View Requests", frontendUrl + "/profile?tab=exchanges"));
    }

    @Async
    public void sendExchangeStatusEmail(User user, String orderNumber, String status, String remarks) {
        send(user.getEmail(), "Exchange Update - " + orderNumber,
                template("Exchange " + status, "Dear " + user.getName() + ",",
                        "Your exchange request for order <strong>" + orderNumber + "</strong> has been <strong>" + status + "</strong>." +
                                (remarks != null ? " Remarks: " + remarks : ""),
                        "View Order", frontendUrl + "/profile?tab=exchanges"));
    }

    @Async
    public void sendAdminExchangeNotification(String orderNumber) {
        send(adminEmail, "New Exchange Request - " + orderNumber,
                template("Exchange Request", "Admin,",
                        "A new exchange request has been submitted for order <strong>" + orderNumber + "</strong>.",
                        "Review", frontendUrl + "/admin/exchanges"));
    }

    @Async
    public void sendPasswordResetEmail(User user, String resetLink) {
        send(user.getEmail(), "Reset Your Password",
                template("Password Reset", "Dear " + user.getName() + ",",
                        "Click the button below to reset your password. This link expires in 1 hour.",
                        "Reset Password", resetLink));
    }

    @Async
    public void sendForgotPasswordEmail(User user) {
        send(user.getEmail(), "Password Reset Requested",
                template("Password Reset Requested", "Dear " + user.getName() + ",",
                        "If you requested a password reset, check your email for the reset link.",
                        "Sign In", frontendUrl + "/login"));
    }

    private void send(String to, String subject, String html) {
        sendWithAttachment(to, subject, html, null, null);
    }

    private void sendWithAttachment(String to, String subject, String html, String filename, byte[] attachment) {
        if (!mailEnabled) {
            log.info("Email disabled. Would send to {}: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (attachment != null && filename != null) {
                helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(attachment));
            }
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String template(String title, String greeting, String body, String ctaText, String ctaUrl) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f8f5f0;font-family:Georgia,serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;">
                <tr><td style="background:#1a2238;padding:32px;text-align:center;">
                  <h1 style="color:#c6a962;margin:0;font-size:28px;letter-spacing:0.2em;">MAYA</h1>
                </td></tr>
                <tr><td style="padding:40px 32px;">
                  <h2 style="color:#1a2238;">%s</h2>
                  <p style="color:#555;line-height:1.6;">%s</p>
                  <p style="color:#555;line-height:1.6;">%s</p>
                  <a href="%s" style="display:inline-block;margin-top:24px;padding:14px 32px;background:#1a2238;color:#fff;text-decoration:none;border-radius:8px;">%s</a>
                </td></tr>
                <tr><td style="background:#f8f5f0;padding:24px;text-align:center;color:#999;font-size:12px;">
                  © 2026 MAYA. Luxury Indo-Latin Fusion Wear.
                </td></tr>
              </table>
            </body></html>
            """.formatted(title, greeting, body, ctaUrl, ctaText);
    }
}

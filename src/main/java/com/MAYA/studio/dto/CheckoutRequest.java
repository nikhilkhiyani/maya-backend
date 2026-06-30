package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequest {
    private UUID addressId;
    private UUID billingAddressId;
    private boolean billingSameAsShipping = true;

    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;

    private String billingFullName;
    private String billingPhone;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPincode;

    private String email;
    private String gstNumber;
    private String couponCode;
    private String orderNotes;
    private boolean termsAccepted;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    private Boolean fromCart = true;
}

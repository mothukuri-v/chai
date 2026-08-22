package com.teasub.exception;

import org.springframework.http.HttpStatus;

/** Carries a machine-readable code the frontend switches on (e.g. ALREADY_REDEEMED_TODAY). */
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    // --- Common redemption-path failures, defined once so codes stay consistent ---
    public static BusinessException subscriptionInactive() {
        return new BusinessException("SUBSCRIPTION_INACTIVE", "Your subscription is not active. Please renew to get today's tea.", HttpStatus.FORBIDDEN);
    }
    public static BusinessException alreadyRedeemedToday() {
        return new BusinessException("ALREADY_REDEEMED_TODAY", "You've already redeemed your tea for today. Come back tomorrow!", HttpStatus.CONFLICT);
    }
    public static BusinessException qrExpired() {
        return new BusinessException("QR_EXPIRED", "This QR code has expired. Please generate a new one.", HttpStatus.GONE);
    }
    public static BusinessException qrInvalid() {
        return new BusinessException("QR_INVALID", "This QR code is not valid.", HttpStatus.BAD_REQUEST);
    }
    public static BusinessException qrAlreadyUsed() {
        return new BusinessException("QR_ALREADY_USED", "This QR code has already been used.", HttpStatus.CONFLICT);
    }
    public static BusinessException shopNotVerified() {
        return new BusinessException("SHOP_NOT_VERIFIED", "This shop is not verified to serve tea yet.", HttpStatus.FORBIDDEN);
    }
    public static BusinessException notFound(String what) {
        return new BusinessException("NOT_FOUND", what + " not found.", HttpStatus.NOT_FOUND);
    }
    public static BusinessException paymentAlreadyProcessed() {
        return new BusinessException("PAYMENT_ALREADY_PROCESSED", "This payment has already been processed.", HttpStatus.OK);
    }
}

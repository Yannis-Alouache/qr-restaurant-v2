package com.qrrestaurant.auth.domain;

public interface PasswordResetMailer {

    void sendResetEmail(String to, String resetUrl);
}

package com.roomkh.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("RoomKH - Password Reset OTP");
        message.setText("Hello,\n\n" +
                "Your OTP for resetting your password is: " + otpCode + "\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email or contact support if you have concerns.");
        
        mailSender.send(message);
    }
}
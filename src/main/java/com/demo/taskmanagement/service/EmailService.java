package com.demo.taskmanagement.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:TaskFlow}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Async
    public void sendTaskAssignmentEmail(String toEmail, String userName, String taskTitle, String projectName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Task Assigned: " + taskTitle);

            String htmlContent = buildTaskAssignmentEmail(userName, taskTitle, projectName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error but don't fail the task creation
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - " + appName);

            String htmlContent = buildPasswordResetEmail(resetToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private String buildTaskAssignmentEmail(String userName, String taskTitle, String projectName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".task-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #0ea5e9; }" +
                ".button { display: inline-block; padding: 12px 30px; background: #0ea5e9; color: white; text-decoration: none; border-radius: 6px; margin: 20px 0; }" +
                ".footer { text-align: center; color: #666; margin-top: 30px; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🎯 New Task Assigned</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hi <strong>" + userName + "</strong>,</p>" +
                "<p>You have been assigned a new task in " + appName + ".</p>" +
                "<div class='task-details'>" +
                "<h3>📋 " + taskTitle + "</h3>" +
                "<p><strong>Project:</strong> " + projectName + "</p>" +
                "</div>" +
                "<a href='" + appUrl + "' class='button'>View Task</a>" +
                "<p>Login to " + appName + " to see all details and start working on this task.</p>" +
                "<div class='footer'>" +
                "<p>This is an automated email from " + appName + ". Please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetEmail(String resetToken) {
        String resetUrl = appUrl + "/reset-password?token=" + resetToken;

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".alert { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }" +
                ".button { display: inline-block; padding: 12px 30px; background: #0ea5e9; color: white; text-decoration: none; border-radius: 6px; margin: 20px 0; }" +
                ".footer { text-align: center; color: #666; margin-top: 30px; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🔐 Password Reset Request</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>We received a request to reset your password for " + appName + ".</p>" +
                "<p>Click the button below to reset your password:</p>" +
                "<a href='" + resetUrl + "' class='button'>Reset Password</a>" +
                "<div class='alert'>" +
                "<strong>⚠️ Security Notice:</strong> This link will expire in 1 hour." +
                "</div>" +
                "<p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>" +
                "<p style='color: #666; font-size: 12px;'>Or copy and paste this URL into your browser:<br>" +
                "<a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
                "<div class='footer'>" +
                "<p>This is an automated email from " + appName + ". Please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}

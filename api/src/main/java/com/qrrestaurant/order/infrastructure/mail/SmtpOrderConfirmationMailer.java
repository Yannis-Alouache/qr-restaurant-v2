package com.qrrestaurant.order.infrastructure.mail;

import com.qrrestaurant.order.domain.OrderConfirmationEmail;
import com.qrrestaurant.order.domain.OrderConfirmationMailer;
import io.sentry.Sentry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Envoi réel via SMTP du reçu « ticket ». Même philosophie que le mailer de
 * réinitialisation : un échec d'envoi ne remonte jamais à l'appelant (le
 * paiement est déjà confirmé côté webhook) — il est journalisé pour les
 * opérations.
 */
@Component
public class SmtpOrderConfirmationMailer implements OrderConfirmationMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpOrderConfirmationMailer.class);

    private final JavaMailSender mailSender;
    private final OrderConfirmationEmailRenderer renderer;
    private final String from;

    public SmtpOrderConfirmationMailer(JavaMailSender mailSender,
                                       OrderConfirmationEmailRenderer renderer,
                                       @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.from = from;
    }

    @Override
    public void send(OrderConfirmationEmail email) {
        String subject = "Votre commande est confirmée · " + email.restaurantName();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email.to());
            helper.setSubject(subject);
            helper.setText(renderer.render(email), true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            // MailException est un RuntimeException : le multi-catch couvre les
            // pannes SMTP comme les erreurs de rendu. Aucun échec ne remonte à
            // l'appelant, mais chacun est signalé à Sentry (no-op sans DSN) :
            // un client qui paie sans jamais recevoir son reçu est une panne
            // silencieuse sinon.
            Sentry.captureException(e);
            log.error("Envoi du reçu de commande {} impossible vers {} : {}",
                    email.orderReference(), email.to(), e.getMessage());
        }
    }
}

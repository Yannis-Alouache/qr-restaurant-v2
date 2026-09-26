package com.qrrestaurant.auth.infrastructure.mail;

import com.qrrestaurant.auth.domain.PasswordResetMailer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Envoi réel via SMTP. Un échec d'envoi ne doit jamais remonter à
 * l'utilisateur (la réponse HTTP est identique de toute façon) : il est
 * journalisé pour les opérations, et en développement le lien peut être
 * journalisé pour pouvoir terminer le parcours sans serveur mail.
 */
@Component
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    static final String SUBJECT = "Réinitialisation de votre mot de passe";

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean logResetLinksOnFailure;

    public SmtpPasswordResetMailer(JavaMailSender mailSender,
                                   @Value("${app.mail.from}") String from,
                                   @Value("${app.mail.log-reset-links-on-failure:false}") boolean logResetLinksOnFailure) {
        this.mailSender = mailSender;
        this.from = from;
        this.logResetLinksOnFailure = logResetLinksOnFailure;
    }

    @Override
    public void sendResetEmail(String to, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(SUBJECT);
            helper.setText(buildHtmlBody(resetUrl), true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.error("Envoi de l'email de réinitialisation impossible vers {} : {}", to, e.getMessage());
            if (logResetLinksOnFailure) {
                log.warn("Lien de réinitialisation pour {} (journalisé car app.mail.log-reset-links-on-failure=true) : {}",
                        to, resetUrl);
            }
        }
    }

    private String buildHtmlBody(String resetUrl) {
        return """
                <p>Bonjour,</p>
                <p>Vous avez demandé la réinitialisation du mot de passe de votre compte restaurateur.</p>
                <p>Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe :</p>
                <p><a href="%s">Choisir un nouveau mot de passe</a></p>
                <p>Ce lien est valable une heure et ne peut être utilisé qu'une seule fois.
                Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email :
                votre mot de passe actuel reste valable.</p>
                <p>L'équipe QR Restaurant</p>
                """.formatted(resetUrl);
    }
}

package tech.apirest.mail.serviceMail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;

import java.util.Properties;

@Service
public class EmailService {

    public void sendSimpleEmail(String to, String subject, String text, String log, String pass) {
        // Créer un JavaMailSender dynamique
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost("mail.apirest.tech");
        javaMailSender.setPort(587);
        javaMailSender.setUsername(log); // Nom d'utilisateur dynamique
        javaMailSender.setPassword(pass); // Mot de passe dynamique

        // Configurer les propriétés SMTP
        Properties props = javaMailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        // Construire l'e-mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom(log); // Utiliser l'expéditeur dynamique

        try {
            // Envoyer l'e-mail
            javaMailSender.send(message);
            System.out.println("E-mail envoyé avec succès à " + to);
        } catch (MailException e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail : " + e.getMessage());
        }
    }
}

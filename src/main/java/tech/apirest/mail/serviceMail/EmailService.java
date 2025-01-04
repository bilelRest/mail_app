package tech.apirest.mail.serviceMail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class EmailService {
@Autowired
private FtpService ftpService;
    public void sendSimpleEmail(String to, String subject, String text, String log, String pass, String fileName,byte[] fileContent) throws MessagingException, IOException {
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
        //SimpleMailMessage message = new SimpleMailMessage();
        MimeMessage message=javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(message,true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        helper.setFrom(log);
        System.out.println("Path de la piece jointe recu : "+fileName);
        if (!fileName.isEmpty()||fileContent!=null) {

            helper.addAttachment(fileName, new ByteArrayResource(fileContent));



        }
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(text);
//        message.setFrom(log);
        // Utiliser l'expéditeur dynamique

        try {
            // Envoyer l'e-mail
            javaMailSender.send(message);
            System.out.println("E-mail envoyé avec succès à " + to);
        } catch (MailException e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail : " + e.getMessage());
        }
    }
}

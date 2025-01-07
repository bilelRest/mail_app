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

    public void sendSimpleEmail(String to, String subject, String text, String username, String password, String fileName, byte[] fileContent) throws MessagingException {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost("mail.apirest.tech");
        javaMailSender.setPort(587);
        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);

        Properties props = javaMailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        helper.setFrom(username);

        if (fileName != null && fileContent != null) {
            helper.addAttachment(fileName, new ByteArrayResource(fileContent));
        }

        javaMailSender.send(message);
        System.out.println("E-mail envoyé avec succès à " + to);
    }
}

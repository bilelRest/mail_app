package tech.apirest.mail.serviceMail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.apirest.mail.Entity.EmailType;
import tech.apirest.mail.Entity.MailEntity;
import tech.apirest.mail.Entity.Users;
import tech.apirest.mail.Repo.MailRepo;
import tech.apirest.mail.Repo.UsersRepo;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ImapMail {

    @Autowired
    private MailRepo mailRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private FtpService ftpService;
    @Autowired
    private EmailController emailController;

    public List<MailEntity> readEmails(String user, String password, MailEntity aSupprimer) {
        String host = "mail.apirest.tech";

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.auth", "true");
        properties.put("mail.imap.ssl.trust", host);

        Session session = Session.getInstance(properties);
        List<MailEntity> mailEntityList = new ArrayList<>();

        try (Store store = session.getStore("imap")) {
            store.connect(user, password);

            Folder inbox = store.getFolder("INBOX");
            if (!inbox.isOpen()) {
                inbox.open(Folder.READ_WRITE);
            }
            int nbMessages = inbox.getMessageCount();
            System.out.println("Nombre de messages dans inbox = " + nbMessages);

            if (nbMessages == 0) {
                try {
                    String hos = "apirest_mail@apirest.tech"; // Hôte d'envoi
                    String pas = "kittalizainnorex2013"; // Remplacer par le mot de passe réel sécurisé
                    String subject = "Bienvenue chez ApiRestMail !";
                    String message = "<html><body>" +
                            "<p>Bonjour <strong>" + findLogged().get().getRealname() + "</strong>,</p>" +
                            "<p>Nous sommes ravis de vous accueillir parmi nous ! Votre compte email a été créé avec succès, et vous pouvez désormais accéder à vos messages à tout moment.</p>" +
                            "<p><strong>Voici les informations de votre nouveau compte :</strong></p>" +
                            "<ul>" +
                            "<li><strong>Login</strong> : " + findLogged().get().getUserid() + "</li>" +
                            "<li><strong>Mot de passe</strong> : Utilisez le mot de passe que vous avez défini (mémorisez bien votre mot de passe ou enregistrez-le dans un endroit sécurisé).</li>" +
                            "</ul>" +
                            "<p>Cordialement,</p>" +
                            "<p>L'équipe ApiRestMail</p>" +
                            "</body></html>";

                    // Envoi de l'email de bienvenue
                    emailController.sendEmail(
                            findLogged().get().getUserid(),  // Destinataire
                            subject,
                            message,
                            hos,
                            pas,  // Mot de passe de l'hôte
                            null,
                            null
                    );
                    System.out.println("Email de bienvenue envoyé à " + findLogged().get().getUserid());
                } catch (Exception e) {
                    System.err.println("Erreur inattendue lors de l'envoi de l'email : " + e.getMessage());
                    e.printStackTrace();
                }
            }


            if (aSupprimer == null) {
                List<MailEntity> listMailSpring = mailRepo.findAllByMailUser(findLogged().orElse(null));
                Message[] messages = (listMailSpring.isEmpty()) ?
                        inbox.getMessages() :
                        inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                for (Message message : messages) {
                    try {
                        MailEntity mailEntity = new MailEntity();
                        mailEntity.setMailUser(findLogged().orElse(null));
                        mailEntity.setDate(message.getSentDate().toString());
                        mailEntity.setSender(message.getFrom()[0].toString());
                        mailEntity.setSubject(message.getSubject());
                        mailEntity.setBody(getTextFromMessage(message));
                        mailEntity.setIsRead(false);
                        mailEntity.setType(EmailType.RECU);

                        String[] messageIds = message.getHeader("Message-ID");
                        if (messageIds != null && messageIds.length > 0) {
                            mailEntity.setUniqueId(messageIds[0]);
                        }

                        if (message.isMimeType("multipart/*")) {
                            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                            List<Map<String, Object>> attachments = extractAttachments(mimeMultipart);

                            for (Map<String, Object> attachment : attachments) {
                                String fileName = UUID.randomUUID() + "_" + attachment.get("fileName").toString();
                                InputStream content = (InputStream) attachment.get("content");
                                ftpService.uploadFile(content, fileName);
                                mailEntity.setDeleteFtpPath(fileName);

                                String uploadsDirectory = "https://www.apirest.tech/downloads/uploads/" + findLogged().get().getUserid().split("@")[0] + "/" + fileName;
                                mailEntity.setPathJoined(uploadsDirectory);
                                mailEntity.setJoinedName((String) attachment.get("fileName"));
                            }
                        }

                        mailEntityList.add(mailEntity);
                    } catch (MessagingException e) {
                        System.err.println("Erreur lors de l'affichage d'un message : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                Message[] messages = inbox.getMessages();
                for (Message message : messages) {
                    String[] messageIds = message.getHeader("Message-ID");
                    if (messageIds != null && messageIds.length > 0 && messageIds[0].equals(aSupprimer.getUniqueId())) {
                        try {
                            message.setFlag(Flags.Flag.DELETED, true);
                            System.out.println("Email supprimé avec succès.");
                            break;
                        } catch (MessagingException e) {
                            System.err.println("Erreur lors de la suppression de l'email : " + e.getMessage());
                        }
                    }
                }
            }

            inbox.close(true); // Expunge messages marked as deleted
        } catch (MessagingException e) {
            System.err.println("Erreur de connexion ou d'accès au serveur IMAP : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur générale : " + e.getMessage());
            e.printStackTrace();
        }

        return mailEntityList;
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/html")) {
            return message.getContent().toString();
        } else if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String htmlContent = null;
        String plainTextContent = null;

        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/html")) {
                htmlContent = bodyPart.getContent().toString();
            } else if (bodyPart.isMimeType("text/plain")) {
                if (plainTextContent == null) {
                    plainTextContent = bodyPart.getContent().toString();
                }
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                String nestedContent = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
                if (nestedContent != null) {
                    htmlContent = nestedContent;
                }
            }
        }

        return htmlContent != null ? htmlContent : plainTextContent != null ? plainTextContent : "";
    }

    private List<Map<String, Object>> extractAttachments(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        List<Map<String, Object>> attachments = new ArrayList<>();
        int count = mimeMultipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                Map<String, Object> attachmentInfo = new HashMap<>();
                attachmentInfo.put("fileName", bodyPart.getFileName());
                attachmentInfo.put("content", bodyPart.getInputStream());
                attachments.add(attachmentInfo);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                attachments.addAll(extractAttachments((MimeMultipart) bodyPart.getContent()));
            }
        }

        return attachments;
    }

    public Optional<Users> findLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String utilisateur = ((UserDetails) authentication.getPrincipal()).getUsername();
            return Optional.ofNullable(usersRepo.findByUserid(utilisateur));
        }
        return Optional.empty();
    }
}

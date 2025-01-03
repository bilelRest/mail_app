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
    public List<MailEntity> readEmails(String user, String password) {
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
                    mailEntity.setUniqueId(Arrays.toString(message.getHeader("Message-ID")));

                    if (message.isMimeType("multipart/*")) {
                        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                        List<Map<String, Object>> attachments = extractAttachments(mimeMultipart);

                        for (Map<String, Object> attachment : attachments) {
                            String fileName =UUID.randomUUID() +"_"+(String) attachment.get("fileName");
                            String joinName=(String)attachment.get("fileName");
                            InputStream content = (InputStream) attachment.get("content");
                           // saveAttachment(content, fileName);
                            ftpService.uploadFile(content,fileName);
                            mailEntity.setJoinedName(joinName);
                             String uploadsDirectory = "https://www.apirest.tech/downloads/uploads/"+findLogged().get().getUserid().split("@")[0]+"/"+fileName;
                            mailEntity.setPathJoined( uploadsDirectory);
                        }
                    }

                    mailEntityList.add(mailEntity);
                } catch (MessagingException e) {
                    System.err.println("Erreur lors de l'affichage d'un message : " + e.getMessage());
                    e.printStackTrace();
                }
            }

            inbox.close(false);
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

            if (bodyPart.getDisposition() != null && bodyPart.getDisposition().equalsIgnoreCase(Part.ATTACHMENT)) {
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

//    private void saveAttachment(InputStream inputStream, String fileName) throws IOException {
//        java.nio.file.Path filePath = java.nio.file.Paths.get("attachments/" + UUID.randomUUID() + "_" + fileName);
//        ftpService.uploadFile(filePath.getParent()., fileName);
//        java.nio.file.Files.createDirectories(filePath.getParent());
//        java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//        System.out.println("Pièce jointe sauvegardée : " + filePath);
//    }

    public Optional<Users> findLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String utilisateur = ((UserDetails) authentication.getPrincipal()).getUsername();
            return Optional.ofNullable(usersRepo.findByUserid(utilisateur));
        }
        return Optional.empty();
    }
}

package tech.apirest.mail.serviceMail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
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
    MailRepo mailRepo;
    public List<MailEntity> readEmails(String user, String password) {


        String host="mail.apirest.tech";

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.auth", "true"); // Force l'authentification
        properties.put("mail.imap.ssl.trust", "mail.apirest.tech"); // Confiance au certificat SSL

        Session session = Session.getInstance(properties);
        List<MailEntity> mailEntityList=new ArrayList<>();
        try (Store store = session.getStore("imap")) {
            // Connexion au serveur IMAP
            store.connect(user, password);

            // Accéder au dossier INBOX
            Folder inbox = store.getFolder("INBOX");
            if (!inbox.isOpen()) {
                inbox.open(Folder.READ_WRITE);
            }
            List<MailEntity> listMailSpring=mailRepo.findAllByMailUser(findLogged().get());
            Message[] messages = (listMailSpring.isEmpty())?inbox.getMessages():inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for (Message message : messages) {
                try {
                    MailEntity mailEntity=new MailEntity();
                    mailEntity.setMailUser(findLogged().get());
                    mailEntity.setDate(message.getSentDate().toString());
                    mailEntity.setSender(message.getFrom()[0].toString());
                    mailEntity.setSubject(message.getSubject());
                    mailEntity.setBody(getTextFromMessage(message));
                    mailEntity.setIsRead(false);
                    mailEntity.setJoinedName("");
                    mailEntity.setPathJoined("");
                    mailEntity.setType(EmailType.RECU);
                    mailEntity.setUniqueId(Arrays.toString(message.getHeader("Message-ID")));
                    mailEntityList.add(mailEntity);

                } catch (MessagingException e) {
                    System.err.println("Erreur lors de l'affichage d'un message : " + e.getMessage());
                    e.printStackTrace();
                }
            }



            // Fermer le dossier
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



    public String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/html")) {
            // Retourne directement le contenu si le message est HTML
            return message.getContent().toString();
        } else if (message.isMimeType("text/plain")) {
            // Retourne le texte brut si HTML n'est pas disponible
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            // Traite les messages multipart
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String htmlContent = null; // Priorité au HTML
        String plainTextContent = null; // Texte brut comme alternative
        StringBuilder attachmentsInfo = new StringBuilder(); // Informations sur les pièces jointes

        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/html")) {
                // Priorise le HTML
                htmlContent = bodyPart.getContent().toString();
            } else if (bodyPart.isMimeType("text/plain")) {
                // Sauvegarde le texte brut si HTML n’est pas trouvé
                if (plainTextContent == null) {
                    plainTextContent = bodyPart.getContent().toString();
                }
            } else if (bodyPart.getDisposition() != null && bodyPart.getDisposition().equalsIgnoreCase(Part.ATTACHMENT)) {
                // Traite les pièces jointes
                String fileName = bodyPart.getFileName();
                String contentType = bodyPart.getContentType();
                attachmentsInfo.append("Pièce jointe trouvée : ").append(fileName)
                        .append(" (").append(contentType).append(")\n");

                // Sauvegarde ou traitement de la pièce jointe
                InputStream inputStream = bodyPart.getInputStream();
                saveAttachment(inputStream, fileName); // Implémentez cette méthode selon vos besoins
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                // Traite les parties multipart imbriquées
                String nestedContent = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
                if (nestedContent != null) {
                    htmlContent = nestedContent; // Priorité HTML dans les parties imbriquées
                }
            }
        }

        // Combine le contenu des pièces jointes avec le texte principal
        String mainContent = htmlContent != null ? htmlContent : plainTextContent != null ? plainTextContent : "";
        return mainContent + (attachmentsInfo.length() > 0 ? "\n\n" + attachmentsInfo.toString() : "");
    }

    // Exemple de méthode pour sauvegarder les pièces jointes
    private void saveAttachment(InputStream inputStream, String fileName) throws IOException {
        java.nio.file.Path filePath = java.nio.file.Paths.get("attachments/" + fileName);
        java.nio.file.Files.createDirectories(filePath.getParent());
        java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Pièce jointe sauvegardée : " + filePath);
    }

    //    private void saveAttachment(InputStream inputStream, String fileName) throws IOException {
//        // Par exemple, vous pouvez sauvegarder l'attachement dans un répertoire spécifique
//        Path path = Paths.get("/path/to/save/attachments", fileName);
//        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
//        System.out.println("Pièce jointe enregistrée sous : " + path.toString());
//    }
    @Autowired
    UsersRepo usersRepo;
    public Optional<Users> findLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String utilisateur = ((UserDetails) authentication.getPrincipal()).getUsername();
            return Optional.ofNullable(usersRepo.findByUserid(utilisateur));
        }
        return Optional.empty();
    }

}
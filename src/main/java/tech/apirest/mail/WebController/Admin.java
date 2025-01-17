package tech.apirest.mail.WebController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.apirest.mail.Entity.*;
import tech.apirest.mail.Repo.MailRepo;
import tech.apirest.mail.Repo.TransportRepo;
import tech.apirest.mail.Repo.UsersRepo;
import tech.apirest.mail.Repo.VirtualRepo;
import tech.apirest.mail.serviceMail.EmailController;
import tech.apirest.mail.serviceMail.FtpService;
import tech.apirest.mail.serviceMail.ImapMail;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Transactional
public class Admin {
    private final TransportRepo transportRepo;
    private final VirtualRepo virtualRepo;
    private final UsersRepo usersRepo;
    private final MailRepo mailRepo;
    private EmailController emailController;
    private ImapMail imapMail;
    private FtpService ftpService;

    public Admin(TransportRepo transportRepo, VirtualRepo virtualRepo, UsersRepo usersRepo, MailRepo mailRepo, EmailController emailController, ImapMail imapMail, FtpService ftpService) {
        this.transportRepo = transportRepo;
        this.virtualRepo = virtualRepo;
        this.usersRepo = usersRepo;
        this.mailRepo = mailRepo;
        this.emailController = emailController;
        this.imapMail = imapMail;
        this.ftpService = ftpService;
    }

    public Optional<Users> findLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String utilisateur = ((UserDetails) authentication.getPrincipal()).getUsername();
            return Optional.ofNullable(usersRepo.findByUserid(utilisateur));
        }
        return Optional.empty();
    }

    @GetMapping(path = "/login")
    public String loginWeb(Model model, @RequestParam(value = "error", defaultValue = "") String error) {

        LogInfo logInfo = new LogInfo("", "");
        model.addAttribute("log", logInfo);
        boolean err = error.equals("true");
        model.addAttribute("error", err);
        return "login";
    }


    @GetMapping(path = "/CreateAccount")
    public String CreateAccount(Model model,
                                @RequestParam(value = "us", defaultValue = "") String us,
                                @RequestParam(value = "name", defaultValue = "") String name,
                                @RequestParam(value = "exist", defaultValue = "false") String exist) {
        Users users = new Users();
        model.addAttribute("exist", exist);

        model.addAttribute("users", users);
        model.addAttribute("us", us);
        model.addAttribute("name", name);
        return "createAccount";
    }

    @PostMapping(value = "/createMail")
    public String createMail(Model model, @ModelAttribute(value = "user") Users users) {
        if (users != null) {
            // Vérifier si l'utilisateur existe déjà
            Users existingUser = usersRepo.findByUserid(users.getUserid() + "@apirest.tech");
            if (existingUser != null) {
                return "redirect:/CreateAccount?us=" + users.getUserid() + "&name=" + users.getRealname() + "&exist=true";
            }

            // Créer un nouvel utilisateur
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            Users newUser = new Users();
            newUser.setUid(1000);
            newUser.setGid(1000);
            newUser.setPassword(encoder.encode(users.getPassword())); // Mot de passe encodé
            newUser.setUserid(users.getUserid() + "@apirest.tech");
            newUser.setRealname(users.getRealname());
            newUser.setMail(users.getUserid() + "@apirest.tech");
            newUser.setHome("apirest.tech/" + users.getUserid() + "/");
            newUser.setTt(users.getPassword()); // Attention au stockage en clair du mot de passe
            usersRepo.save(newUser);

            // Configurer les entrées pour Transport et Virtual
            Transport transport = new Transport();
            transport.setTransport("virtual:");
            transport.setDomain(users.getUserid() + "@apirest.tech");
            transportRepo.save(transport);

            Virtual virtual = new Virtual();
            virtual.setAddress(users.getUserid() + "@apirest.tech");
            virtual.setUserid(users.getUserid() + "@apirest.tech");
            virtualRepo.save(virtual);




        }

        return "redirect:/login";
    }


    @GetMapping(value = "/accueilMail")
    public String accueilMail(Model model,
                              @RequestParam(value = "keyword",defaultValue = "")String keyword,
                              @RequestParam(value = "page",defaultValue = "0")int page,
                              @RequestParam(value = "size",defaultValue = "10")int size,
                              @RequestParam(value = "sent",defaultValue = "",required = false)String sent) throws MessagingException, IOException {
        model.addAttribute("user", findLogged().get());
        model.addAttribute("sent",sent);
        System.out.println("Valeur recu de sent "+sent);

         List<MailEntity> mailEntityListImap=imapMail.readEmails(findLogged().get().getUserid(),findLogged().get().getTt(),null);
        if (!mailEntityListImap.isEmpty()){
            List<MailEntity> mailEntityList1=new ArrayList<>();
            Set<MailEntity> existingMails = new HashSet<>(mailRepo.findAllByMailUser(findLogged().get()));
            for (MailEntity mail : mailEntityListImap) {
                if (!existingMails.contains(mail)) {
                    mailEntityList1.add(mail);
                }
            }
            mailRepo.saveAll(mailEntityList1);
        }
        Page<MailEntity> mailEntityList2=null;
        if (!findLogged().isEmpty()) {
            mailEntityList2 = mailRepo.getBypageable(findLogged().get(), EmailType.RECU, PageRequest.of(page, size));

        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("pages", new int[mailEntityList2.getTotalPages()]);

        model.addAttribute("currentPage", page);

        model.addAttribute("keyword",keyword);

        model.addAttribute("size", nombreNonLu);
//        List<MailEntity> mailEntityList = new ArrayList<>();
//        Set<String> senders = new HashSet<>(); // Pour suivre les expéditeurs uniques
//
//        for (MailEntity mail : mailEntityList2) {
//            if(!mail.getIsRead()){
//                senders.add(mail.getSender());
//            }
//            if (senders.add(mail.getSender())) { // `add` retourne `true` si le sender est nouveau
//                mailEntityList.add(mail);
//            }
//        }


        model.addAttribute("messages", mailEntityList2);

        return "accueilMail";
    }

    @GetMapping(value = "/new")
    public String newMail(Model model, @RequestParam(value = "id", defaultValue = "") Long id) {
        if (id != null) {
            Optional<MailEntity> mail = mailRepo.findById(id);
            int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
            model.addAttribute("size", nombreNonLu);
            model.addAttribute("user", findLogged().get());
            MailDetails mailDetails = new MailDetails();
            mailDetails.setId(mail.get().getId());
            mailDetails.setTo(mail.get().getDestinataire());
            mailDetails.setSubject(mail.get().getSubject());
            mailDetails.setPathJointe( mail.get().getPathJoined());
            mailDetails.setNameJointe(mail.get().getJoinedName());
            mailDetails.setMessage(mail.get().getBody());
            System.out.println("corp du message : "+mail.get().getBody());
            model.addAttribute("mailDetails", mailDetails);
            return "newMail";
        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());
        MailDetails mailDetails = new MailDetails();
        model.addAttribute("mailDetails", mailDetails);
        return "newMail";
    }

    @GetMapping(value = "/sent")
    public String sentEmail(Model model) {
        List<MailEntity> mailEntityList2 = mailRepo.findAllByMailUser(findLogged().get());
        System.out.println("Mail  trouvé de taille "+mailEntityList2.size());
        List<MailEntity> mailEntityList = new ArrayList<>();
        for (MailEntity mail : mailEntityList2) {
            System.out.println("Type de lemail :  "+mail.getType());

            if (mail.getType() == EmailType.ENVOYEE) {
                mailEntityList.add(mail);
                System.out.println("mail envouyer ajouté "+mail.getDestinataire());
            }
        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
//        model.addAttribute("listMail",mailEntityList);

        return "sent";
    }

    @GetMapping(value = "/deconnect")
    public String logoutMail(Model model, HttpSession session, HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    @GetMapping(value = "/draft")
    public String draftMail(Model model) {

        List<MailEntity> mailEntityList2 = mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList = new ArrayList<>();
        for (MailEntity mail : mailEntityList2) {
            if (mail.getType() == EmailType.BROUILLON) {
                mailEntityList.add(mail);
            }
        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
        return "draft";
    }

    @GetMapping(value = "/recycle")
    public String recycleMail(Model model) {
        List<MailEntity> mailEntityList2 = mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList = new ArrayList<>();
        for (MailEntity mail : mailEntityList2) {
            if (mail.getType() == EmailType.DELETED) {
                mailEntityList.add(mail);
            }
        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());
        model.addAttribute("trashSize",mailEntityList.size());
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
        return "recycle";
    }


    @PostMapping(value = "/sendMail")
    public String sendMail(
            @ModelAttribute("mailDetails") MailDetails mailDetails,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "reply", defaultValue = "false") boolean reply) throws IOException {
System.out.println("Id recu = "+id);
System.out.println("reply recu : "+reply);
        boolean isDraft = mailDetails.isDraft();
        boolean sent =false;
        MailEntity mailEntity=new MailEntity();
        String newReply="";
        if(id!=null&&reply){
            Optional<MailEntity> mailEntity2=mailRepo.findById(id);
            newReply=mailDetails.fromsender+mailDetails.to+UUID.randomUUID();
            mailEntity2.get().setReplyId(newReply);
            mailEntity2.get().setIsRead(true);
            mailRepo.save(mailEntity2.get());

        }
        if (id!=null && !reply){
            Optional<MailEntity> mailEntity3=mailRepo.findById(id);
            mailEntity=mailEntity3.get();
        }

        String random = UUID.randomUUID().toString();
        String uploadsDirectory = "https://www.apirest.tech/downloads/uploads/" +
                findLogged().get().getUserid().split("@")[0] + "/";

        // Remplissage des données
        mailEntity.setMailUser(findLogged().get());
        mailEntity.setSender(findLogged().get().getUserid());
        mailEntity.setDate(LocalDateTime.now().toString());
        mailEntity.setIsRead(true);
        mailEntity.setBody(mailDetails.getMessage());
        mailEntity.setSubject(mailDetails.getSubject());
        mailEntity.setDestinataire(mailDetails.getTo());
        mailEntity.setType(isDraft ? EmailType.BROUILLON : EmailType.ENVOYEE);
        System.out.println("Type de mail capturé : "+mailEntity.getType());
        mailEntity.setReplyId(newReply);

        if (mailDetails.getJointe() != null && !mailDetails.getJointe().isEmpty()) {
            String originalFilename = mailDetails.getJointe().getOriginalFilename();
            byte[] fileContent = mailDetails.getJointe().getBytes();

            mailEntity.setJoinedName(originalFilename);
            mailEntity.setPathJoined(uploadsDirectory + random + originalFilename);
            mailEntity.setDeleteFtpPath(random + originalFilename);

            ftpService.uploadFile(new ByteArrayInputStream(fileContent), random + originalFilename);

            if (!isDraft) {
              try{  emailController.sendEmail(
                        mailDetails.getTo(),
                        mailDetails.getSubject(),
                        mailDetails.getMessage(),
                        findLogged().get().getUserid(),
                        findLogged().get().getTt(),
                        originalFilename,
                        fileContent
                );
              sent=true;
              }catch (Exception e){

              }
            }
        } else if (!isDraft) {
         try {
             emailController.sendEmail(
                     mailDetails.getTo(),
                     mailDetails.getSubject(),
                     mailDetails.getMessage(),
                     findLogged().get().getUserid(),
                     findLogged().get().getTt(),
                     null,
                     null
             );
             sent=true;
         }catch (Exception e){

         }

        }
        mailEntity.setType(isDraft ? EmailType.BROUILLON : EmailType.ENVOYEE);
        mailEntity.setBody(mailDetails.getMessage());
        System.out.println(mailEntity.getBody());

        mailEntity.setIsRead(true);

        mailRepo.save(mailEntity);
        if(isDraft){
            return "redirect:/accueilMail";
        }
        return "redirect:/accueilMail?sent="+sent;
    }



    @GetMapping(value = "/setNonLu/{id}")
    public String SetNonLu(Model model, @PathVariable(value = "id" )Long id,
                           @RequestParam(value = "page",required = false,defaultValue = "0")int page,
                           @RequestParam(value="inbox",required = false)boolean inbox) throws MessagingException {

        Optional<MailEntity> mail=mailRepo.findById(id);
        new MailEntity();
        MailEntity mailEntity;

    if (mail.isPresent()) {
        mailEntity = mail.get();

        // Afficher l'état initial
        System.out.println("Etat initial du mail recu: " + mailEntity.getIsRead());

        // Inverser l'état
        boolean newIsRead = !mailEntity.getIsRead();
        mailEntity.setIsRead(newIsRead);

        // Afficher l'état modifié
        System.out.println("Etat modifié à: " + mailEntity.getIsRead());

        // Sauvegarder dans la base de données
        mailRepo.saveAndFlush(mailEntity);

        // Relecture pour vérification
        Optional<MailEntity> updatedMail = mailRepo.findById(mailEntity.getId());
        System.out.println("Etat du mail après enregistrement: " + updatedMail.get().getIsRead());
    }

    if (inbox){
            return "redirect:/inbox/"+id;
        }
        model.addAttribute("page",page);


        return "redirect:/accueilMail?page="+page;
    }
    @GetMapping(value = "/delete/{id}")
    public String DeleteMessage(Model model,
                                @PathVariable("id") Long id,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "inbox", required = false,defaultValue = "") String inbox,
                                @RequestParam(value = "sended",required = false,defaultValue = "")String sended) throws MessagingException {
        System.out.println("Valeur inbox reçue : " + inbox);
        System.out.println("Id recu = "+id);

        Optional<MailEntity> mail = mailRepo.findById(id);

        model.addAttribute("page", page);
        if (mail.isPresent()) {
            mail.get().setType(EmailType.DELETED);
            mailRepo.save(mail.get());
        }
        if(sended.equals("true")){
            List<MailEntity> list=mailRepo.findAllByMailUser(findLogged().get());
            if (!list.isEmpty()){
                for (MailEntity mail1:list){
                    if(mail1.getType().equals(EmailType.ENVOYEE)){
                        return "redirect:/sended?id=" + mail1.getId();
                    }
                }
            }

            return "redirect:/sent";
        }


        if (inbox.equals("true")) {
            List<MailEntity> list=mailRepo.findAllByMailUser(findLogged().get());
            if(!list.isEmpty()){
                for (MailEntity mail1:list){
                    if(mail1.getType().equals(EmailType.ENVOYEE)){
                        return "redirect:/inbox/" + mail1.getId();
                    }
                }
            }
            System.out.println("Redirection vers inbox / " + id);

        }

        return "redirect:/accueilMail?page=" + page;
    }

    @PostMapping(value = "/deleteFromServer")
    public String deleteFromServer(Model model, @RequestParam(value = "id", required = true) Long id) {
        Optional<MailEntity> mail = mailRepo.findById(id);
        Optional<Users> loggedUser = findLogged();

        if (mail.isPresent() && loggedUser.isPresent()) {
            MailEntity mailEntity = mail.get();

            try {

                if(!mail.get().getDeleteFtpPath().isEmpty()){
                    ftpService.deleteFile(mail.get().getDeleteFtpPath());}
                mailRepo.delete(mailEntity);
            } catch (Exception e) {
                e.printStackTrace();
                // Ajouter un retour ou une notification à l'utilisateur en cas d'erreur
                model.addAttribute("error", "Impossible de supprimer le message.");
            }
        } else {
            // Ajouter un retour ou une notification à l'utilisateur en cas de données manquantes
            model.addAttribute("error", "Message ou utilisateur introuvable.");
        }

        return "redirect:/recycle";
    }

    @GetMapping(value = "/inbox/{id}")
    public String inboxById(Model model, @PathVariable Long id,
                            @RequestParam(value = "accueil",defaultValue = "false",required = false)boolean accueil,
                            @RequestParam(value = "sent",defaultValue = "false",required = false)boolean sent,
                            @RequestParam(value = "trash",required = false,defaultValue = "false")boolean trash) throws MessagingException {
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());
        int envoyer = 0;
        int recu = 0;
        model.addAttribute("sent",sent);
        model.addAttribute("trash",trash);

        Optional<MailEntity> mail = mailRepo.findById(id);
        MailEntity mailEntity2 = new MailEntity();
        model.addAttribute("mailEntity",mailEntity2);

        if (mail.isPresent()) {
            MailEntity mailEntity = mail.get();
            if (trash){
                model.addAttribute("mailEntity",mailEntity);

                return "inbox";
            }
            if(accueil) {
                mailEntity.setIsRead(true);
                mailRepo.save(mailEntity);
            }


            String senders = mailEntity.getSender();
            String mailSender = senders.equals(findLogged().get().getUserid()) ? mailEntity.getDestinataire() : senders;
            model.addAttribute("mailSender", mailSender);
            model.addAttribute("idMessage", id);

            List<MailEntity> relatedMails = mailRepo.trouverMail(senders);
            List<MailEntity> mailEntityList2 = new ArrayList<>();

            if (relatedMails != null && !relatedMails.isEmpty()) {
                for (MailEntity mail1 : relatedMails) {
                    if (mail1.getType() == EmailType.ENVOYEE || mail1.getType() == EmailType.RECU) {
                        mailEntityList2.add(mail1);
                        if (mail1.getType() == EmailType.ENVOYEE) {
                            envoyer++;
                        } else if (mail1.getType() == EmailType.RECU) {
                            recu++;
                        }
                    }
                }
                model.addAttribute("mails", mailEntityList2);
                System.out.println("La liste des mails n'est pas vide. Taille : " + relatedMails.size());
            } else {
                List<MailEntity> mailEntityList = new ArrayList<>();
                mailEntityList.add(mailEntity);
                model.addAttribute("mails", mailEntityList);
            }
        } else {
            model.addAttribute("mails", new ArrayList<>()); // Liste vide pour éviter les erreurs Thymeleaf
        }

        model.addAttribute("envoyer", envoyer);
        model.addAttribute("recu", recu);
        return "Inbox";
    }

    @GetMapping(value = "/sended")
    public String sended(Model model, @RequestParam(value = "id", required = true) Long id) {
        // Compte les emails non lus
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);

        // Utilisateur connecté
        var loggedUser = findLogged().get();
        model.addAttribute("user", loggedUser);

        // Initialisation
        int envoyer = 0;

        // Recherche du mail par ID
        Optional<MailEntity> mailOpt = mailRepo.findById(id);
        if (mailOpt.isEmpty()) {
            model.addAttribute("error", "Message introuvable");
            return "sended";
        }

        // Mise à jour de l'état du mail
        MailEntity mailEntity = mailOpt.get();
        mailEntity.setIsRead(true);
        mailRepo.save(mailEntity);

        // Récupère l'expéditeur
        String sender = mailEntity.getSender();
        model.addAttribute("idMessage", id);

        // Trouve les mails liés à l'expéditeur
        List<MailEntity> relatedMails = mailRepo.trouverMail(sender);
        List<MailEntity> mailEntityList2 = new ArrayList<>();

        for (MailEntity mail : relatedMails) {
            if (mail.getMailUser() != null) {
                if (mail.getType() == EmailType.ENVOYEE &&
                        mail.getMailUser().getUserid().equals(loggedUser.getUserid())) {
                    mailEntityList2.add(mail);
                    envoyer++;
                }
            } else {
                mailRepo.delete(mail);
            }
        }

        // Ajout des données au modèle
        model.addAttribute("mails", mailEntityList2);
        model.addAttribute("envoyer", envoyer);

        return "sended";
    }
    @GetMapping(value = "/check")
    public String check(Model model,@RequestParam(value = "error",required = false,defaultValue = "")String error,
                        @RequestParam(value = "success",defaultValue = "",required = false)String success){
        model.addAttribute("user", findLogged().get());
        String msg="";
        if(Objects.equals(success,"true")){
            msg="Mot de passe modifié avec succèss.";
            model.addAttribute("success",msg);
        }
        String error1="";
        if(Objects.equals(error, "oldPassword")){
            error1="Le mot de passe actuel est incorrect.";

        }if(Objects.equals(error, "confirmationMismatch")){
            error1="Les mots de passe ne correspondent pas.";
        }if(Objects.equals(error, "weakPassword")){
            error1="Le nouveau mot de passe doit contenir au moins 8 caractères.";
        }
        model.addAttribute("error",error1);
        return "check";
    }

    @PostMapping("/checkPwd")
    public String changePassword(
            @RequestParam("password") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        // Récupérer l'utilisateur actuellement connecté
        Users currentUser = findLogged().get(); // Implémentez cette méthode pour trouver l'utilisateur connecté
        PasswordEncoder passwordEncoder=new BCryptPasswordEncoder();

        // Vérifier si le mot de passe actuel est correct
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            model.addAttribute("error", "Le mot de passe actuel est incorrect.");
            return "redirect:/check?error=oldPassword";
        }

        // Vérifier si le nouveau mot de passe et la confirmation correspondent
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/check?error=confirmationMismatch";
        }

        // Vérifier la robustesse du nouveau mot de passe (optionnel)
        if (newPassword.length() < 8) {
            model.addAttribute("error", "Le nouveau mot de passe doit contenir au moins 8 caractères.");
            return "redirect:/check?error=weakPassword";
        }

        // Mettre à jour le mot de passe
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        currentUser.setTt(newPassword);
        usersRepo.save(currentUser);

        // Ajouter un message de succès
        model.addAttribute("success", "Mot de passe changé avec succès !");
        return "redirect:/check?success=true";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LogInfo {
        private String login;
        private String password;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class MailDetails{
        private Long id;
        private boolean draft=false;
        private String to;
        private String subject;
        private String message;
        private MultipartFile jointe;
        private String pathJointe;
        private String nameJointe;
        private String fromsender;

    }
}
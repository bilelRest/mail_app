package tech.apirest.mail.WebController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
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

            Users users1 = usersRepo.findByUserid(users.getUserid() + "@apirest.tech");
            if (users1 != null) {

                return "redirect:/CreateAccount?us=" + users.getUserid() + "&name=" + users.getRealname() + "&&exist=" + true;
            }
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            Users users2 = new Users();

            users2.setUid(1000);
            users2.setGid(1000);
            users2.setPassword(encoder.encode(users.getPassword()));
            users2.setUserid(users.getUserid() + "@apirest.tech");
            users2.setRealname(users.getRealname());
            users2.setMail(users.getUserid() + "@apirest.tech");
            users2.setHome("apirest.tech/" + users.getUserid() + "/");
            users2.setTt(users.getPassword());
            //System.out.println(users2);
            usersRepo.save(users2);
            Transport transport = new Transport();
            transport.setTransport("virtual:");
            transport.setDomain(users.getUserid() + "@apirest.tech");
            transportRepo.save(transport);
            //System.out.println(transport);
            Virtual virtual = new Virtual();
            virtual.setAddress(users.getUserid() + "@apirest.tech");
            virtual.setUserid(users.getUserid() + "@apirest.tech");
            // System.out.println(virtual);
            virtualRepo.save(virtual);

        }
        return "redirect:/login";
    }

    @GetMapping(value = "/accueilMail")
    public String accueilMail(Model model) throws MessagingException, IOException {
        System.out.println("tt trouvé : " + findLogged().get().getTt());
        model.addAttribute("user", findLogged().get());


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
        List<MailEntity> mailEntityList2 = mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList = new ArrayList<>();
        for (MailEntity mail : mailEntityList2) {
            if (mail.getType() == EmailType.RECU) {
                mailEntityList.add(mail);
            }
        }
        System.out.println("Taille de la table spring : " + mailEntityList.size());
//        System.out.println( " taille du tableau : " +mailEntityListImap.size());
        int nombreTotal = mailEntityList.size();

        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);

        System.out.println("Nombre non lu = " + nombreNonLu);
        model.addAttribute("total", nombreTotal);
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));

        return "accueilMail";
    }

    @GetMapping(value = "/new")
    public String newMail(Model model, @RequestParam(value = "id", defaultValue = "") Long id) {
        if (id != null) {
            Optional<MailEntity> mail = mailRepo.findById(id);
            System.out.println(mail);
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
            System.out.println("Message corps transmis a new : " + mailDetails.getMessage());
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
        List<MailEntity> mailEntityList = new ArrayList<>();
        for (MailEntity mail : mailEntityList2) {
            if (mail.getType() == EmailType.ENVOYEE) {
                mailEntityList.add(mail);
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
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
        return "recycle";
    }


    @PostMapping(value = "/sendMail")
    public String sendMail(@ModelAttribute("mailDetails") MailDetails mailDetails,
                           @RequestParam(value = "id", required = false) Long id) throws IOException {
        System.out.println("Chemin reçu : " + (mailDetails.getJointe() != null ? mailDetails.getJointe().getOriginalFilename() : "Aucune pièce jointe"));
        boolean isDraft = mailDetails.isDraft();

        MailEntity mailEntity;

        // Si un ID est fourni, mettez à jour un email existant
        if (id != null) {
            mailEntity = mailRepo.findById(id).orElse(new MailEntity());
        } else {
            mailEntity = new MailEntity();
        }
        String random=UUID.randomUUID().toString();

        // Remplissage des données communes
        mailEntity.setSender(findLogged().get().getUserid());
        mailEntity.setDate(LocalDateTime.now().toString());
        mailEntity.setIsRead(true);
        mailEntity.setBody(mailDetails.getMessage());
        mailEntity.setSubject(mailDetails.getSubject());
        mailEntity.setDestinataire(mailDetails.getTo());
        mailEntity.setType(isDraft ? EmailType.BROUILLON : EmailType.ENVOYEE);
        mailEntity.setJoinedName(mailDetails.getJointe() != null ? mailDetails.getJointe().getOriginalFilename() : null);
        String uploadsDirectory = "https://www.apirest.tech/downloads/uploads/"+findLogged().get().getUserid().split("@")[0]+"/";

        mailEntity.setPathJoined(uploadsDirectory+random+mailDetails.getJointe().getOriginalFilename());
        mailEntity.setDeleteFtpPath(random+mailDetails.getJointe().getOriginalFilename());
        mailEntity.setMailUser(findLogged().get());
        mailEntity.setUniqueId(mailDetails.getTo() + LocalDateTime.now().toString());

        // Gestion du fichier joint
        if (mailDetails.getJointe() != null && !mailDetails.getJointe().isEmpty()) {
            // Lire la pièce jointe comme un tableau d'octets
            byte[] fileContent = mailDetails.getJointe().getBytes();

            String originalFilename = mailDetails.getJointe().getOriginalFilename();
            ftpService.uploadFile( new ByteArrayInputStream(fileContent),random+mailDetails.getJointe().getOriginalFilename());

            // Envoi d'email avec pièce jointe
            if(!isDraft){
                assert originalFilename != null;
                emailController.sendEmail(
                        mailDetails.getTo(),
                        mailDetails.getSubject(),
                        mailDetails.getMessage(),
                        findLogged().get().getUserid(),
                        findLogged().get().getTt(),
                        originalFilename.isEmpty()?null:originalFilename,
                        fileContent==null?null:fileContent
                );
            }
        }

        // Sauvegarde dans la base de données
        mailRepo.save(mailEntity);

        return "redirect:/accueilMail";
    }


    @GetMapping(value = "/setNonLu/{id}")
    public String SetNonLu(Model model, @PathVariable(value = "id" )Long id) throws MessagingException {

        Optional<MailEntity> mail=mailRepo.findById(id);
        if(mail.get()!=null){
            mail.get().setIsRead(false);
        }

        return "redirect:/accueilMail";
    }
    @GetMapping(value = "/delete/{id}")
    public String DeleteMessage(Model model, @PathVariable(value = "id" )Long id) throws MessagingException {

        Optional<MailEntity> mail=mailRepo.findById(id);
        if(mail.get()!=null){
            mail.get().setType(EmailType.DELETED);
            System.out.println("enregistré comme supprimer avec success");

        }

        return "redirect:/accueilMail";
    }

    @PostMapping(value = "/deleteFromServer")
    public String deleteFromServer(Model model, @RequestParam(value = "id", required = true) Long id) {
        Optional<MailEntity> mail = mailRepo.findById(id);
        Optional<Users> loggedUser = findLogged();

        if (mail.isPresent() && loggedUser.isPresent()) {
            Users user = loggedUser.get();
            MailEntity mailEntity = mail.get();

            try {
                if(!mail.get().getDeleteFtpPath().isEmpty())
                    ftpService.deleteFile(mail.get().getDeleteFtpPath());
                System.out.println("file name : "+mail.get().getPathJoined());
                System.out.println("Utilisateur : " + user.getUserid() + ", Message supprimé : " + mailEntity.getUniqueId());
            } catch (Exception e) {
                System.err.println("Erreur lors de la suppression du message : " + e.getMessage());
                e.printStackTrace();
                // Ajouter un retour ou une notification à l'utilisateur en cas d'erreur
                model.addAttribute("error", "Impossible de supprimer le message.");
            }
        } else {
            System.err.println("Email ou utilisateur non trouvé pour l'ID : " + id);
            // Ajouter un retour ou une notification à l'utilisateur en cas de données manquantes
            model.addAttribute("error", "Message ou utilisateur introuvable.");
        }

        return "redirect:/accueilMail";
    }

    @GetMapping(value = "/inbox/{id}")
    public String inboxById(Model model, @PathVariable(value = "id" )Long id) throws MessagingException {
        int nombreNonLu= mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size",nombreNonLu);
        model.addAttribute("user",findLogged().get());
      Optional<  MailEntity> mail=mailRepo.findById(id);
        if (mail!=null){
            mail.get().setIsRead(true);
            mailRepo.save(mail.get());
        }
        model.addAttribute("mail",mail.get());

        return "Inbox";
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
    }
}
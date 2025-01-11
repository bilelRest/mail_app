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
                              @RequestParam(value = "keyword",defaultValue = "")String keyword,@RequestParam(value = "page",defaultValue = "0")int page,@RequestParam(value = "size",defaultValue = "9")int size) throws MessagingException, IOException {
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
        Page<MailEntity> mailEntityList2=null;
        if (!findLogged().isEmpty()) {
            mailEntityList2 = mailRepo.getBypageable(findLogged().get(), EmailType.RECU, PageRequest.of(page, size));

        }
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("pages", new int[mailEntityList2.getTotalPages()]);

        model.addAttribute("currentPage", page);

        model.addAttribute("keyword",keyword);

        model.addAttribute("size", nombreNonLu);

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
    public String sendMail(
            @ModelAttribute("mailDetails") MailDetails mailDetails,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "reply", defaultValue = "false") boolean reply) throws IOException {
System.out.println("Id recu = "+id);
System.out.println("reply recu : "+reply);
        boolean isDraft = mailDetails.isDraft();
        MailEntity mailEntity=new MailEntity();
        String newReply="";
        if(id!=null&&reply){
            Optional<MailEntity> mailEntity2=mailRepo.findById(id);
            newReply=mailDetails.fromsender+mailDetails.to+UUID.randomUUID();
            mailEntity2.get().setReplyId(newReply);

        }
        if (id!=null && !reply){
            Optional<MailEntity> mailEntity3=mailRepo.findById(id);
            mailEntity=mailEntity3.get();
        }

        String random = UUID.randomUUID().toString();
        String uploadsDirectory = "https://www.apirest.tech/downloads/uploads/" +
                findLogged().get().getUserid().split("@")[0] + "/";

        // Remplissage des données
        mailEntity.setSender(findLogged().get().getUserid());
        mailEntity.setDate(LocalDateTime.now().toString());
        mailEntity.setIsRead(true);
        mailEntity.setBody(mailDetails.getMessage());
        mailEntity.setSubject(mailDetails.getSubject());
        mailEntity.setDestinataire(mailDetails.getTo());
        mailEntity.setType(isDraft ? EmailType.BROUILLON : EmailType.ENVOYEE);
        mailEntity.setReplyId(newReply);

        if (mailDetails.getJointe() != null && !mailDetails.getJointe().isEmpty()) {
            String originalFilename = mailDetails.getJointe().getOriginalFilename();
            byte[] fileContent = mailDetails.getJointe().getBytes();

            mailEntity.setJoinedName(originalFilename);
            mailEntity.setPathJoined(uploadsDirectory + random + originalFilename);
            mailEntity.setDeleteFtpPath(random + originalFilename);

            ftpService.uploadFile(new ByteArrayInputStream(fileContent), random + originalFilename);

            if (!isDraft) {
                emailController.sendEmail(
                        mailDetails.getTo(),
                        mailDetails.getSubject(),
                        mailDetails.getMessage(),
                        findLogged().get().getUserid(),
                        findLogged().get().getTt(),
                        originalFilename,
                        fileContent
                );
            }
        } else if (!isDraft) {
            emailController.sendEmail(
                    mailDetails.getTo(),
                    mailDetails.getSubject(),
                    mailDetails.getMessage(),
                    findLogged().get().getUserid(),
                    findLogged().get().getTt(),
                    null,
                    null
            );
        }

        mailRepo.save(mailEntity);
        return "redirect:/accueilMail";
    }



    @GetMapping(value = "/setNonLu/{id}")
    public String SetNonLu(Model model, @PathVariable(value = "id" )Long id,@RequestParam(value = "page",defaultValue = "0")int page) throws MessagingException {

        Optional<MailEntity> mail=mailRepo.findById(id);
        model.addAttribute("page",page);
        if(mail.get()!=null){
            mail.get().setIsRead(false);
        }

        return "redirect:/accueilMail?page="+page;
    }
    @GetMapping(value = "/delete/{id}")
    public String DeleteMessage(Model model, @PathVariable(value = "id" )Long id,@RequestParam(value = "page",defaultValue = "0")int page) throws MessagingException {

        Optional<MailEntity> mail=mailRepo.findById(id);
        model.addAttribute("page",page);
        if(mail.get()!=null){
            mail.get().setType(EmailType.DELETED);

        }

        return "redirect:/accueilMail?page="+page;
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
            } catch (Exception e) {
                e.printStackTrace();
                // Ajouter un retour ou une notification à l'utilisateur en cas d'erreur
                model.addAttribute("error", "Impossible de supprimer le message.");
            }
        } else {
            // Ajouter un retour ou une notification à l'utilisateur en cas de données manquantes
            model.addAttribute("error", "Message ou utilisateur introuvable.");
        }

        return "redirect:/accueilMail";
    }

    @GetMapping(value = "/inbox/{id}")
    public String inboxById(Model model, @PathVariable Long id) throws MessagingException {
        int nombreNonLu = mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size", nombreNonLu);
        model.addAttribute("user", findLogged().get());


        Optional<MailEntity> mail = mailRepo.findById(id);

        if (mail.isPresent()) {
            MailEntity mailEntity = mail.get();
            mailEntity.setIsRead(true);
            mailRepo.save(mailEntity);
            String uniquereply=mail.get().getReplyId();
            String senders=mail.get().getSender();


            List<MailEntity> relatedMails = mailRepo.trouverMail(senders);
            if(!relatedMails.isEmpty()){

                model.addAttribute("mails", relatedMails);
                System.out.println("la liste n'est pas vide et sa longueur est "+relatedMails.size());}
            else {
                List<MailEntity> mailEntityList=new ArrayList<>();
                mailEntityList.add(mail.get());
                model.addAttribute("mails", mailEntityList);

            }

        }

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
        private String fromsender;

    }
}
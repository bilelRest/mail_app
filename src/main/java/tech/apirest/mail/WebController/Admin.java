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
import tech.apirest.mail.Entity.*;
import tech.apirest.mail.Repo.MailRepo;
import tech.apirest.mail.Repo.TransportRepo;
import tech.apirest.mail.Repo.UsersRepo;
import tech.apirest.mail.Repo.VirtualRepo;
import tech.apirest.mail.serviceMail.EmailController;
import tech.apirest.mail.serviceMail.ImapMail;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
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
    public Admin(TransportRepo transportRepo, VirtualRepo virtualRepo, UsersRepo usersRepo, MailRepo mailRepo, EmailController emailController, ImapMail imapMail) {
        this.transportRepo = transportRepo;
        this.virtualRepo = virtualRepo;
        this.usersRepo = usersRepo;
        this.mailRepo = mailRepo;
        this.emailController = emailController;
        this.imapMail = imapMail;
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
    public String loginWeb(Model model ,@RequestParam(value = "error",defaultValue = "")String error ) {

        LogInfo logInfo = new LogInfo("", "");
        model.addAttribute("log", logInfo);
        boolean err= error.equals("true");
        model.addAttribute("error", err);
        return "login";
    }


    @GetMapping(path = "/CreateAccount")
    public  String CreateAccount(Model model,
                                 @RequestParam(value = "us",defaultValue = "")String us,
                                 @RequestParam(value = "name",defaultValue = "")String name,
                                 @RequestParam(value = "exist",defaultValue = "false")String exist){
        Users users=new Users();
        model.addAttribute("exist",exist);

        model.addAttribute("users",users);
        model.addAttribute("us",us);
        model.addAttribute("name",name);
        return "createAccount";
    }
    @PostMapping(value = "/createMail")
    public String createMail(Model model,@ModelAttribute(value = "user")Users users){
        if(users!=null) {

            Users users1 = usersRepo.findByUserid(users.getUserid() + "@apirest.tech");
            if (users1 != null) {

                return "redirect:/CreateAccount?us=" + users.getUserid() + "&name=" + users.getRealname() + "&&exist=" + true;
            }
            PasswordEncoder encoder=new BCryptPasswordEncoder();
            Users users2=new Users();

            users2.setUid(1000);
            users2.setGid(1000);
            users2.setPassword(encoder.encode(users.getPassword()));
            users2.setUserid(users.getUserid() + "@apirest.tech");
            users2.setRealname(users.getRealname());
            users2.setMail(users.getUserid() + "@apirest.tech");
            users2.setHome("apirest.tech/"+users.getUserid()+"/");
            users2.setTt(users.getPassword());
            //System.out.println(users2);
            usersRepo.save(users2);
            Transport transport=new Transport();
            transport.setTransport("virtual:");
            transport.setDomain(users.getUserid() + "@apirest.tech");
            transportRepo.save(transport);
            //System.out.println(transport);
            Virtual virtual=new Virtual();
            virtual.setAddress(users.getUserid() + "@apirest.tech");
            virtual.setUserid(users.getUserid() + "@apirest.tech");
            // System.out.println(virtual);
            virtualRepo.save(virtual);

        }
        return "redirect:/login";
    }

    @GetMapping(value = "/accueilMail")
    public String accueilMail(Model model) throws MessagingException, IOException {
        System.out.println("tt trouvé : "+findLogged().get().getTt());
        model.addAttribute("user",findLogged().get());


        List<MailEntity> mailEntityListImap=imapMail.readEmails(findLogged().get().getUserid(),findLogged().get().getTt());
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
        List<MailEntity> mailEntityList2=mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList=new ArrayList<>();
        for (MailEntity mail:mailEntityList2){
            if (mail.getType()==EmailType.RECU){
                mailEntityList.add(mail);
            }
        }
        System.out.println("Taille de la table spring : "+mailEntityList.size());
        System.out.println( " taille du tableau : " +mailEntityListImap.size());
        int nombreTotal=mailEntityList.size();

        int nombreNonLu= mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size",nombreNonLu);

        System.out.println("Nombre non lu = "+nombreNonLu);
        model.addAttribute("total",nombreTotal);
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));

        return "accueilMail";
    }
    @GetMapping(value = "/new")
    public String newMail(Model model){
        int nombreNonLu= mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size",nombreNonLu);
        model.addAttribute("user",findLogged().get());
        MailDetails mailDetails=new MailDetails();
        model.addAttribute("mailDetails",mailDetails);
        return "newMail";
    }
    @GetMapping(value = "/sent")
    public String sentEmail(Model model){
        List<MailEntity> mailEntityList2=mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList=new ArrayList<>();
        for (MailEntity mail:mailEntityList2){
            if (mail.getType()==EmailType.ENVOYEE){
                mailEntityList.add(mail);
            }
        }
        int nombreNonLu= mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size",nombreNonLu);
        model.addAttribute("user",findLogged().get());
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
//        model.addAttribute("listMail",mailEntityList);

        return "sent";
    }
    @GetMapping(value = "/deconnect")
    public String logoutMail(Model model, HttpSession session, HttpServletRequest request){
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
    @GetMapping(value = "/draft")
    public String draftMail(Model model){
        List<MailEntity> mailEntityList2=mailRepo.findAllByMailUser(findLogged().get());
        List<MailEntity> mailEntityList=new ArrayList<>();
        for (MailEntity mail:mailEntityList2){
            if (mail.getType()==EmailType.BROUILLON){
                mailEntityList.add(mail);
            }
        }
        int nombreNonLu= mailRepo.countUnreadEmails(findLogged().get());
        model.addAttribute("size",nombreNonLu);
        model.addAttribute("user",findLogged().get());
        model.addAttribute("messages", mailEntityList.stream()
                .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                .collect(Collectors.toList()));
        return "draft";
    }
    @PostMapping(value = "/sendMail")
    public String sendMail(Model model,@ModelAttribute(value ="mailDetails" )MailDetails mailDetails,@RequestParam(value = "draft",defaultValue = "")String draft){
        System.out.println("User found : "+mailDetails);
        System.out.println("tt trouvé : "+findLogged().get().getTt());
        Boolean isDraft=draft.equals("true");
        System.out.println("Valeur de draft  "+draft);
        Boolean sent=false;
if (!isDraft) {
     if(emailController.sendEmail(mailDetails.to, mailDetails.subject, mailDetails.message, findLogged().get().getUserid(), findLogged().get().getTt())){
         sent=true;
     }
}
if(sent) {
              MailEntity mail = new MailEntity();
              mail.setSender(findLogged().get().getUserid());
              mail.setDate(LocalDateTime.now().toString());
              mail.setIsRead(true);
              mail.setBody(mailDetails.message);
              mail.setSubject(mailDetails.subject);
              mail.setDestinataire(mailDetails.to);
              mail.setType(EmailType.ENVOYEE);
              mail.setJoinedName(mailDetails.jointe);
              mail.setMailUser(findLogged().get());
              mail.setUniqueId(mailDetails.to + LocalDateTime.now().toString());
              System.out.println("Email enregistré" + mail);
              mailRepo.save(mail);
              return "redirect:/accueilMail";
          }else {
              MailEntity mail=new MailEntity();
              mail.setSender(findLogged().get().getUserid());
              mail.setDate(LocalDateTime.now().toString());
              mail.setIsRead(true);
              mail.setBody(mailDetails.message);
              mail.setSubject(mailDetails.subject);
              mail.setDestinataire(mailDetails.to);
              mail.setType(EmailType.BROUILLON);
              mail.setJoinedName(mailDetails.jointe);
              mail.setMailUser(findLogged().get());
              mail.setUniqueId(mailDetails.to+ LocalDateTime.now().toString());
              System.out.println("Email enregistré"+mail);
              mailRepo.save(mail);
              return "redirect:/new";
          }



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
        private String to;
        private String subject;
        private String message;
        private String jointe="";
    }
}
package tech.apirest.mail.serviceMail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.Optional;


@Service
public class EmailController {

    @Autowired
    private EmailService emailService;


    public Boolean sendEmail( String to,  String subject,  String text,String log,String pass) {
try {
    emailService.sendSimpleEmail(to, subject, text,log,pass);
    System.out.println("succes d'envoie à : " + to);

    return true;
}catch (Exception e){
    System.out.println("Echec d'envoie à  " + to);

    return false;

}
    }


}

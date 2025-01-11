package tech.apirest.mail.serviceMail;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class EmailUtils {
    public  String extractEmail(String sender) {
        if (sender == null || !sender.contains("<")) {
            return sender; // Si pas de chevrons, retourne la chaîne d'origine
        }
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(sender);
        if (matcher.find()) {
            return matcher.group(1); // Retourne l'adresse e-mail
        }
        return sender; // Retourne la chaîne d'origine si aucun match
    }
}

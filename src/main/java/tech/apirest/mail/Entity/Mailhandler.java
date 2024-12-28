package tech.apirest.mail.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.mail.Message;

@Data
@AllArgsConstructor@NoArgsConstructor
public class Mailhandler {
    private Message messages;
    private String body;
}

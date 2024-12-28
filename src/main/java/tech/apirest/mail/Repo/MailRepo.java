package tech.apirest.mail.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.apirest.mail.Entity.MailEntity;
import tech.apirest.mail.Entity.Users;

import java.util.List;

public interface MailRepo extends JpaRepository<MailEntity,Long> {
    List<MailEntity> findAllByMailUser(Users user);
    MailEntity  findByUniqueId(String messageId);
}

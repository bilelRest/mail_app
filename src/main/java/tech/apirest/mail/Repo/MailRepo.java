package tech.apirest.mail.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.apirest.mail.Entity.MailEntity;
import tech.apirest.mail.Entity.Users;

import java.util.List;

public interface MailRepo extends JpaRepository<MailEntity,Long> {
    List<MailEntity> findAllByMailUser(Users user);
    MailEntity  findByUniqueId(String messageId);
    @Query("SELECT COUNT(m) FROM MailEntity m WHERE m.isRead = false AND m.mailUser = :user")
    Integer countUnreadEmails(@Param("user") Users user);}

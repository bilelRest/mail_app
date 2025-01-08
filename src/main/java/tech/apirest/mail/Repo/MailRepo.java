package tech.apirest.mail.Repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.apirest.mail.Entity.EmailType;
import tech.apirest.mail.Entity.MailEntity;
import tech.apirest.mail.Entity.Users;

import java.util.List;

public interface MailRepo extends JpaRepository<MailEntity,Long> {
    List<MailEntity> findAllByMailUser(Users user);
    @Query("SELECT m FROM MailEntity m WHERE m.mailUser=:user AND m.type=:type ORDER BY m.id DESC")
    Page<MailEntity> getBypageable(@Param("user")Users user,@Param("type") EmailType type, Pageable pageable);
    MailEntity  findByUniqueId(String messageId);
    @Query("SELECT COUNT(m) FROM MailEntity m WHERE m.isRead = false AND m.mailUser = :user")
    Integer countUnreadEmails(@Param("user") Users user);}


package tech.apirest.mail.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.apirest.mail.Entity.Users;

public interface UsersRepo extends JpaRepository<Users,Long> {
    Users findByUserid(String login);
}

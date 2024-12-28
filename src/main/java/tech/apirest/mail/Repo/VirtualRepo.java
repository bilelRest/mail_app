package tech.apirest.mail.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.apirest.mail.Entity.Virtual;

public interface VirtualRepo extends JpaRepository<Virtual,Long> {
}

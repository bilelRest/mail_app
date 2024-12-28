package tech.apirest.mail.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.apirest.mail.Entity.Transport;

public interface TransportRepo extends JpaRepository<Transport,Long> {
}

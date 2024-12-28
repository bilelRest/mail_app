package tech.apirest.mail.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "domain",nullable = false)
    private String domain;
    @Column(name = "transport",nullable = false)
   private String transport;


    public Transport(String domain, String transport) {
        this.domain=domain;
        this.transport=transport;
    }
}

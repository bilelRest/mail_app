package tech.apirest.mail.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table
public class MailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String subject;
    private String date;

    @Lob
    @Column(columnDefinition = "text")
    private String body;
    private Boolean isRead=false;
    private String joinedName;
    private String pathJoined;
    @Enumerated(EnumType.STRING)
    private EmailType type;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users mailUser;
    @Column(unique = true)
    private String uniqueId;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailEntity that = (MailEntity) o;
        return Objects.equals(uniqueId, that.uniqueId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }
}
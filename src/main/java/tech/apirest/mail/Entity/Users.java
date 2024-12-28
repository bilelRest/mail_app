package tech.apirest.mail.Entity;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(name = "userid", nullable = false)
    private String userid;



    @Column(name = "password")
    private String password;

    @Column(name = "realname")
    private String realname;

    @Column(name = "uid", nullable = false)
    private Integer uid;

    @Column(name = "gid", nullable = false)
    private Integer gid;
    @Column(name = "tt")
    private String tt;


    @Column(name = "home")
    private String home;


    @Column(name = "mail")
    private String mail;
    @OneToMany(mappedBy = "id", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<MailEntity> mailList;

    public Users(String userid, String password, String realname, Integer uid, Integer gid, String home, String mail) {
        this.userid = userid;
        this.password = password;
        this.realname = realname;
        this.uid = uid;
        this.gid = gid;
        this.home = home;
        this.mail = mail;
    }

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", userid='" + userid + '\'' +
                ", password='" + password + '\'' +
                ", realname='" + realname + '\'' +
                ", uid=" + uid +
                ", gid=" + gid +
                ", home='" + home + '\'' +
                ", mail='" + mail + '\'' +
                ",tt='"+tt+'\''+
                '}';
    }


    // Constructor par défaut sans arguments généré par Lombok avec @NoArgsConstructor
}
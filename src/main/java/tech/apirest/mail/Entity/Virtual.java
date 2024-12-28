package tech.apirest.mail.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Virtual {
    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long id;
    @Column(name = "address",nullable = false)
   private String address;
        @Column(name = "userid")
    private String userid;


    public Virtual(String address, String userid) {
        this.address=address;
        this.userid=userid;
    }
}
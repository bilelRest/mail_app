//package tech.apirest.mail.WebController;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import tech.apirest.mail.Entity.Users;
//import tech.apirest.mail.Repo.TransportRepo;
//import tech.apirest.mail.Repo.UsersRepo;
//import tech.apirest.mail.Repo.VirtualRepo;
//
//@RestController
//public class controller {
//    @Autowired
//    UsersRepo usersRepo;
//    @Autowired
//    TransportRepo transportRepo;
//    @Autowired
//    VirtualRepo virtualRepo;
//    @GetMapping(value = "/test")
//    public boolean test(){
//        try {
//            Users users=new Users("bilel@apirest.tech", "123456", "Biel", 1000, 1000, "/apirest.tech/bilel/", "bilel@apirest.tech");
//        usersRepo.save(users);
//        return true;
//        }catch (Exception e){
//            e.printStackTrace();
//            return false;
//        }
//    }
//}

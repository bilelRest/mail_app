package tech.apirest.mail;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tech.apirest.mail.Entity.*;
import tech.apirest.mail.Repo.MailRepo;
import tech.apirest.mail.Repo.UsersRepo;
import tech.apirest.mail.Services.TransportInterfaceImpl;
import tech.apirest.mail.Services.UsersInterfaceImpl;
import tech.apirest.mail.Services.VirtualInterfaceImpl;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@SpringBootApplication
public class MailApplication {
	@Autowired
	TransportInterfaceImpl transportInterface;
	@Autowired
	UsersInterfaceImpl usersInterface;
	@Autowired
	VirtualInterfaceImpl virtualInterface;
	@Autowired
	MailRepo mailRepo;
//	@Autowired
//	ImapMail imapMail;
	@Enumerated(EnumType.STRING)
	EmailType emailType;
//	@Autowired
//	EmailService emailService;
//	@Bean
//	PasswordEncoder passwordEncoder () {
//		return new BCryptPasswordEncoder();
//	}

	private static UsersRepo usersRepo;

    public MailApplication(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;
    }


    public static void main(String[] args) {
		SpringApplication.run(MailApplication.class, args);


		//System.out.println(LocalDateTime.now().toString().replace(":","").replace(".","").replace("-",""));
	}



//
	@Bean
	CommandLineRunner start() {
		return args -> {

			//Virtual virtual=new Virtual("support@apirest.tech","bilel@apirest.tech");
			//virtualInterface.addVirtual(virtual);
//			usersRepo.deleteById(2L);
//			Users users=usersRepo.findByUserid("nissen@apirest.tech");
//		List<MailEntity>list=	mailRepo.findAllByMailUser(users);
//		for (MailEntity mail:list){
//			mailRepo.deleteById(mail.getId());
//		}
//		Users users= usersRepo.findByUserid("nissen@apirest.tech");
//
//			List<MailEntity> list=mailRepo.findAllByMailUser(users);
//			for (MailEntity mail:list){
//				if (mail.getType()==EmailType.DELETED){
//	mail.setType(EmailType.RECU);
//					mailRepo.save(mail);
//					System.out.println(" mail trouver "+mail);
//				}

//				System.out.println("Editer non supprimé");

//			}


//			List<Users> list=usersRepo.findAll();
//			for (Users users:list){
//				System.out.println(users);
//			}
		//	emailService.sendSimpleEmail("bilelbenabdallah31@gmail.com","test brypt","test bcrypt","bilel@apirest.tech","123456");

//			List<Users> list=usersRepo.findAll();
//			for (Users users:list){
//				BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
//				String pass= encoder.encode("123456");
//
//				users.setPassword(pass);
//				usersRepo.save(users);
//			}
		};}}
//
//
//
//			//System.out.println(u);
//
//
//			// Créer un nouvel utilisateur sans spécifier l'ID (il sera généré automatiquement)
////			Users users = new Users("ubuntu1@apirest.tech", "123456", "ubuntu1", 1001, 1001, "apirest.tech/ubuntu1", "ubuntu1@apirest.tech");
//
////			usersInterface.addUser(users); // Pas besoin de définir l'ID, la base de données le gère
////transportInterface.addTransport(new Transport("ubuntu1@apirest.tech","virtual:"));
////virtualInterface.addVirtual(new Virtual("ubuntu1@apirest.tech","ubuntu1@apirest.tech"));
//			// Ajouter les autres entités de manière similaire
////			Virtual virtual = new Virtual();
////			virtual.setUserid("bilel@apirest.tech");
////			virtual.setAddress("bilel@apirest.tech");
////			virtualInterface.addVirtual(virtual);
////
////			Transport transport = new Transport();
////			transport.setDomain("bilel@apirest.tech");
////			transport.setTransport("virtual:");
////			transportInterface.addTransport(transport);
//			//Message[] messages=imapMail.readEmails();
//
////			for (Message message : messages) {
////				System.out.println("Sujet : " + message.getSubject());
////				System.out.println("De : " + message.getFrom()[0]);
////				System.out.println("Date : " + message.getSentDate());
////				// System.out.println("Contenu : " + getTextFromMessage(message));
////			}
//			//System.out.println(emailType.BROUILLON);
////			Scanner scanner=new Scanner(System.in);
////			while (true){
////				String input= scanner.nextLine().toLowerCase();
////
////				if(input.equals("exit")) break;
////				if (input.contains("bonjours"))
////					System.out.println("Bonjour comment je peut vous aider");
////				else if( input.contains("date")||input.contains("heure"))
////					System.out.println(LocalDate.now());
////
////			}
////			scanner.close();
//		};
//	}
//
//}
//

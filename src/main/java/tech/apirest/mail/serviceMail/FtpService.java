package tech.apirest.mail.serviceMail;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.apirest.mail.Entity.Users;
import tech.apirest.mail.Repo.UsersRepo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
@Service
public class FtpService {

    @Autowired
    private UsersRepo usersRepo;

    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String user;

    @Value("${ftp.password}")
    private String pass;

    // Méthode pour téléverser un fichier sur le serveur FTP
    public void uploadFile(InputStream inputStream, String remoteFileName) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String uploadsDirectory = "/home/spring/ftp/uploads/" + getUsernameFromEmail();
            createDirectoryIfNotExists(ftpClient, uploadsDirectory);

            String fullRemotePath = uploadsDirectory + "/" + remoteFileName;

            boolean success = ftpClient.storeFile(fullRemotePath, inputStream);
            if (success) {
                System.out.println("Fichier téléversé : " + fullRemotePath);
            } else {
                System.out.println("Échec du téléversement : " + fullRemotePath);
            }
        } finally {
            inputStream.close();
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }


    // Méthode pour supprimer un fichier sur le serveur FTP
    public void deleteFile(String fileName) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            boolean deleted = ftpClient.deleteFile(fileName);
            if (deleted) {
                System.out.println("Fichier supprimé avec succès : " + fileName);
            } else {
                System.out.println("Échec de la suppression du fichier : " + fileName);
            }
        } finally {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    // Méthode utilitaire pour créer un répertoire si nécessaire
    private void createDirectoryIfNotExists(FTPClient ftpClient, String directory) throws IOException {
        String[] paths = directory.split("/");
        String currentPath = "";
        for (String path : paths) {
            if (!path.isEmpty()) {
                currentPath += "/" + path;
                ftpClient.makeDirectory(currentPath);
            }
        }
    }

    // Méthode pour récupérer le nom d'utilisateur sans le domaine
    private String getUsernameFromEmail() {
        String email = findLogged().get().getUserid();
        return email.split("@")[0];
    }

    // Méthode pour trouver l'utilisateur connecté
    public Optional<Users> findLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String utilisateur = ((UserDetails) authentication.getPrincipal()).getUsername();
            return Optional.ofNullable(usersRepo.findByUserid(utilisateur));
        }
        return Optional.empty();
    }
}

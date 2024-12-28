package tech.apirest.mail.Services;

import org.springframework.stereotype.Service;
import tech.apirest.mail.Entity.Users;
import tech.apirest.mail.Repo.UsersRepo;

import java.util.List;
@Service
public class UsersInterfaceImpl implements UsersInterface {
    private UsersRepo usersRepo;

    public UsersInterfaceImpl(UsersRepo usersRepo) {

        this.usersRepo = usersRepo;
    }

    @Override
    public Users findByLog(String log) {
        return usersRepo.findByUserid( log);
    }

    @Override
    public Boolean addUser(Users users) {
        try{
            usersRepo.save(users);
            System.out.println("success users");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean updateUsers(Users users) {
        try{
            usersRepo.save(users);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public Boolean deleteUsers(Long id) {
        try{
            usersRepo.deleteById(id);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public List< Users> selectUsers() {
        return usersRepo.findAll();
    }
}

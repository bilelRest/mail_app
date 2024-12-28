package tech.apirest.mail.Services;


import tech.apirest.mail.Entity.Users;

import java.util.List;

public interface UsersInterface {
    public Users findByLog(String log);
    public Boolean addUser(Users users);
    public Boolean updateUsers(Users users);
    public Boolean deleteUsers(Long id);
    public List<Users> selectUsers();
}

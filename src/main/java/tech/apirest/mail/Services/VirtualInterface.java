package tech.apirest.mail.Services;

import tech.apirest.mail.Entity.Transport;
import tech.apirest.mail.Entity.Virtual;

import java.util.List;

public interface VirtualInterface {
    public Boolean addVirtual(Virtual virtual);
    public Boolean updateVirtual(Virtual virtual);
    public Boolean deleteVirtual(Long id);
    public List<Virtual> selectTransport();
}

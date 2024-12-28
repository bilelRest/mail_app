package tech.apirest.mail.Services;

import tech.apirest.mail.Entity.Transport;

import java.util.List;

public interface TransportInterface {
public Boolean addTransport(Transport transport);
    public Boolean updateTransport(Transport transport);
    public Boolean deleteTransport(Long id);
    public List<Transport> selectTransport();

}

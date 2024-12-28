package tech.apirest.mail.Services;

import org.springframework.stereotype.Service;
import tech.apirest.mail.Entity.Transport;
import tech.apirest.mail.Repo.TransportRepo;

import java.util.List;
@Service
public class TransportInterfaceImpl implements TransportInterface {
    private TransportRepo transportRepo;

    public TransportInterfaceImpl(TransportRepo transportRepo) {
        this.transportRepo = transportRepo;
    }

    @Override
    public Boolean addTransport(Transport transport) {
        try {
            transportRepo.save(transport);
            System.out.println("Crééer avec succses");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public Boolean updateTransport(Transport transport) {
        try {

            transportRepo.save(transport);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean deleteTransport(Long id) {
        try {
            transportRepo.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Transport> selectTransport() {
        return transportRepo.findAll();
    }
}

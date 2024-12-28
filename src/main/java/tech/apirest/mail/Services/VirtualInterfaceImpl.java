package tech.apirest.mail.Services;

import org.springframework.stereotype.Service;
import tech.apirest.mail.Entity.Virtual;
import tech.apirest.mail.Repo.VirtualRepo;

import java.util.List;
@Service
public class VirtualInterfaceImpl implements VirtualInterface {
    private VirtualRepo virtualRepo;

    public VirtualInterfaceImpl(VirtualRepo virtualRepo) {
        this.virtualRepo = virtualRepo;
    }

    @Override
    public Boolean addVirtual(Virtual virtual) {
        try {
            virtualRepo.save(virtual);
            System.out.println("Success virtual");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean updateVirtual(Virtual virtual) {
        try {
            virtualRepo.save(virtual);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean deleteVirtual(Long id) {
        try {
            virtualRepo.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Virtual> selectTransport() {
        return virtualRepo.findAll();
    }
}

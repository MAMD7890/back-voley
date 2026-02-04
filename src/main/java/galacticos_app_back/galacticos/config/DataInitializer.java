package galacticos_app_back.galacticos.config;

import galacticos_app_back.galacticos.entity.Rol;
import galacticos_app_back.galacticos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles por defecto si no existen
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("USER");
        createRoleIfNotExists("PROFESOR");
        createRoleIfNotExists("STUDENT");
    }

    private void createRoleIfNotExists(String roleName) {
        if (rolRepository.findByNombre(roleName).isEmpty()) {
            Rol rol = new Rol();
            rol.setNombre(roleName);
            rolRepository.save(rol);
            System.out.println("Rol creado: " + roleName);
        }
    }
}

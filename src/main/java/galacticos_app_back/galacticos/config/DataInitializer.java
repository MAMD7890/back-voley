package galacticos_app_back.galacticos.config;

import galacticos_app_back.galacticos.entity.Rol;
import galacticos_app_back.galacticos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer {
    
    @Autowired
    private RolRepository rolRepository;
    
    /**
     * Inicializa los roles básicos al iniciar la aplicación
     * Se ejecuta después de que toda la aplicación está lista
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeRoles() {
        System.out.println("\n🔄 Inicializando roles del sistema...");
        
        // Array de roles a crear
        String[] rolesNecesarios = {"USER", "STUDENT", "PROFESOR"};
        
        for (String nombreRol : rolesNecesarios) {
            // Verificar si el rol ya existe
            if (rolRepository.findByNombre(nombreRol).isEmpty()) {
                Rol nuevoRol = new Rol();
                nuevoRol.setNombre(nombreRol);
                rolRepository.save(nuevoRol);
                System.out.println("✅ Rol creado: " + nombreRol);
            } else {
                System.out.println("⚠️  Rol ya existe: " + nombreRol);
            }
        }
        
        System.out.println("✅ Inicialización de roles completada\n");
    }
}

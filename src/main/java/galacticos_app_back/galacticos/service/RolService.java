package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Rol;
import galacticos_app_back.galacticos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RolService {
    
    @Autowired
    private RolRepository rolRepository;
    
    public List<Rol> obtenerTodos() {
        return rolRepository.findAll();
    }
    
    public Optional<Rol> obtenerPorId(Integer id) {
        return rolRepository.findById(id);
    }
    
    public Rol crear(Rol rol) {
        return rolRepository.save(rol);
    }
    
    public Rol actualizar(Integer id, Rol rol) {
        Optional<Rol> existente = rolRepository.findById(id);
        if (existente.isPresent()) {
            rol.setIdRol(id);
            return rolRepository.save(rol);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        rolRepository.deleteById(id);
    }
}

package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Sede;
import galacticos_app_back.galacticos.repository.SedeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SedeService {
    
    @Autowired
    private SedeRepository sedeRepository;
    
    public List<Sede> obtenerTodos() {
        return sedeRepository.findAll();
    }
    
    public Optional<Sede> obtenerPorId(Integer id) {
        return sedeRepository.findById(id);
    }
    
    public List<Sede> obtenerActivos() {
        return sedeRepository.findByEstado(true);
    }
    
    public Sede crear(Sede sede) {
        return sedeRepository.save(sede);
    }
    
    public Sede actualizar(Integer id, Sede sede) {
        Optional<Sede> existente = sedeRepository.findById(id);
        if (existente.isPresent()) {
            sede.setIdSede(id);
            return sedeRepository.save(sede);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        sedeRepository.deleteById(id);
    }
    
    public Sede desactivar(Integer id) {
        Optional<Sede> sede = sedeRepository.findById(id);
        if (sede.isPresent()) {
            sede.get().setEstado(false);
            return sedeRepository.save(sede.get());
        }
        return null;
    }
}

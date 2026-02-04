package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.ProfesorEquipo;
import galacticos_app_back.galacticos.repository.ProfesorEquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProfesorEquipoService {
    
    @Autowired
    private ProfesorEquipoRepository profesorEquipoRepository;
    
    public List<ProfesorEquipo> obtenerTodos() {
        return profesorEquipoRepository.findAll();
    }
    
    public Optional<ProfesorEquipo> obtenerPorId(Integer id) {
        return profesorEquipoRepository.findById(id);
    }
    
    public ProfesorEquipo crear(ProfesorEquipo profesorEquipo) {
        return profesorEquipoRepository.save(profesorEquipo);
    }
    
    public ProfesorEquipo actualizar(Integer id, ProfesorEquipo profesorEquipo) {
        Optional<ProfesorEquipo> existente = profesorEquipoRepository.findById(id);
        if (existente.isPresent()) {
            profesorEquipo.setIdProfesorEquipo(id);
            return profesorEquipoRepository.save(profesorEquipo);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        profesorEquipoRepository.deleteById(id);
    }
}

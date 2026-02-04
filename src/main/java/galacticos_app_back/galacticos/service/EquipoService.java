package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Equipo;
import galacticos_app_back.galacticos.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EquipoService {
    
    @Autowired
    private EquipoRepository equipoRepository;
    
    public List<Equipo> obtenerTodos() {
        return equipoRepository.findAll();
    }
    
    public Optional<Equipo> obtenerPorId(Integer id) {
        return equipoRepository.findById(id);
    }
    
    public List<Equipo> obtenerActivos() {
        return equipoRepository.findByEstado(true);
    }
    
    public List<Equipo> obtenerPorSede(Integer idSede) {
        return equipoRepository.findBySedeIdSede(idSede);
    }
    
    public List<Equipo> obtenerPorCategoria(Integer idCategoria) {
        return equipoRepository.findByCategoriaIdCategoria(idCategoria);
    }
    
    public Equipo crear(Equipo equipo) {
        return equipoRepository.save(equipo);
    }
    
    public Equipo actualizar(Integer id, Equipo equipo) {
        Optional<Equipo> existente = equipoRepository.findById(id);
        if (existente.isPresent()) {
            equipo.setIdEquipo(id);
            return equipoRepository.save(equipo);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        equipoRepository.deleteById(id);
    }
    
    public Equipo desactivar(Integer id) {
        Optional<Equipo> equipo = equipoRepository.findById(id);
        if (equipo.isPresent()) {
            equipo.get().setEstado(false);
            return equipoRepository.save(equipo.get());
        }
        return null;
    }
}

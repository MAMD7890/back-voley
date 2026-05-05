package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MembresiaService {
    
    @Autowired
    private MembresiaRepository membresiaRepository;
    
    public List<Membresia> obtenerTodos() {
        return membresiaRepository.findAll();
    }
    
    public Optional<Membresia> obtenerPorId(Integer id) {
        return membresiaRepository.findById(id);
    }
    
    public List<Membresia> obtenerPorEstudiante(Integer idEstudiante) {
        return membresiaRepository.findByEstudianteIdEstudiante(idEstudiante);
    }
    
    public List<Membresia> obtenerPorEquipo(Integer idEquipo) {
        return membresiaRepository.findByEquipoIdEquipo(idEquipo);
    }
    
    public List<Membresia> obtenerVigentes() {
        return membresiaRepository.findByEstado(true); // true = vigente/activo
    }
    
    public Membresia crear(Membresia membresia) {
        return membresiaRepository.save(membresia);
    }
    
    public Membresia actualizar(Integer id, Membresia membresia) {
        Optional<Membresia> existente = membresiaRepository.findById(id);
        if (existente.isPresent()) {
            Membresia actual = existente.get();
            if (membresia.getFechaInicio()  != null) actual.setFechaInicio(membresia.getFechaInicio());
            if (membresia.getFechaFin()     != null) actual.setFechaFin(membresia.getFechaFin());
            if (membresia.getEstado()       != null) actual.setEstado(membresia.getEstado());
            if (membresia.getValorMensual() != null) actual.setValorMensual(membresia.getValorMensual());
            if (membresia.getEquipo()       != null) actual.setEquipo(membresia.getEquipo());
            return membresiaRepository.save(actual);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        membresiaRepository.deleteById(id);
    }

    public Membresia actualizarEstadoMembresia(Integer idEstudiante, Boolean estado) {
        List<Membresia> membresias = membresiaRepository.findByEstudianteIdEstudiante(idEstudiante);
        if (membresias == null || membresias.isEmpty()) {
            throw new RuntimeException("No se encontró una membresía para el estudiante con ID: " + idEstudiante);
        }
        Membresia membresia = membresias.get(0); // Asumimos que un estudiante tiene una única membresía activa
        membresia.setEstado(estado);
        return membresiaRepository.save(membresia);
    }
}

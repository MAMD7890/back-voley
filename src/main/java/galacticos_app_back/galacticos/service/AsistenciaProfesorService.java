package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.ProfesorAttendanceDto;
import galacticos_app_back.galacticos.dto.ProfesorAttendanceMarkRequest;
import galacticos_app_back.galacticos.dto.ProfesorPagoDto;
import galacticos_app_back.galacticos.entity.AsistenciaProfesor;
import galacticos_app_back.galacticos.entity.Profesor;
import galacticos_app_back.galacticos.entity.ProfesorEquipo;
import galacticos_app_back.galacticos.repository.AsistenciaProfesorRepository;
import galacticos_app_back.galacticos.repository.ProfesorEquipoRepository;
import galacticos_app_back.galacticos.repository.ProfesorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AsistenciaProfesorService {

    @Autowired
    private AsistenciaProfesorRepository asistenciaProfesorRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorEquipoRepository profesorEquipoRepository;

    public List<AsistenciaProfesor> obtenerTodos() {
        return asistenciaProfesorRepository.findAll();
    }

    public Optional<AsistenciaProfesor> obtenerPorId(Integer id) {
        return asistenciaProfesorRepository.findById(id);
    }

    public AsistenciaProfesor crear(AsistenciaProfesor asistencia) {
        return asistenciaProfesorRepository.save(asistencia);
    }

    public AsistenciaProfesor actualizar(Integer id, AsistenciaProfesor asistencia) {
        Optional<AsistenciaProfesor> existente = asistenciaProfesorRepository.findById(id);
        if (existente.isPresent()) {
            asistencia.setIdAsistenciaProfesor(id);
            return asistenciaProfesorRepository.save(asistencia);
        }
        return null;
    }

    public void eliminar(Integer id) {
        asistenciaProfesorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProfesorAttendanceDto> listarProfesoresConFiltroYAsistencia(LocalDate fecha, Integer sedeId, Integer equipoId, String nombreLike) {
        List<Profesor> profesores;
        if (nombreLike != null && !nombreLike.isBlank()) {
            profesores = profesorRepository.findByNombreContainingIgnoreCaseAndEstado(nombreLike, true);
        } else {
            profesores = profesorRepository.findByEstado(true);
        }

        // si hay filtro por equipo o sede, obtener ids de profesores permitidos
        Set<Integer> permitidos = null;
        if (equipoId != null) {
            List<ProfesorEquipo> pe = profesorEquipoRepository.findByEquipoIdEquipo(equipoId);
            permitidos = pe.stream().map(p -> p.getProfesor().getIdProfesor()).collect(Collectors.toSet());
        } else if (sedeId != null) {
            List<ProfesorEquipo> pe = profesorEquipoRepository.findByEquipoSedeIdSede(sedeId);
            permitidos = pe.stream().map(p -> p.getProfesor().getIdProfesor()).collect(Collectors.toSet());
        }

        List<ProfesorAttendanceDto> resultado = new ArrayList<>();
        for (Profesor prof : profesores) {
            if (permitidos != null && !permitidos.contains(prof.getIdProfesor())) continue;
            ProfesorAttendanceDto dto = new ProfesorAttendanceDto();
            dto.setIdProfesor(prof.getIdProfesor());
            dto.setNombre(prof.getNombre());
            dto.setSalarioPorClase(prof.getSalarioPorClase());
            boolean asistio = asistenciaProfesorRepository.existsByProfesorIdProfesorAndFecha(prof.getIdProfesor(), fecha);
            dto.setAsistio(asistio);
            if (asistio) {
                List<AsistenciaProfesor> ap = asistenciaProfesorRepository.findByProfesorIdProfesorOrderByFechaDesc(prof.getIdProfesor());
                if (!ap.isEmpty()) dto.setFechaAsistencia(ap.get(0).getFecha());
            }
            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional
    public List<Map<String, Object>> registrarAsistenciasProfesores(List<ProfesorAttendanceMarkRequest> marcas) {
        List<Map<String, Object>> respuestas = new ArrayList<>();
        for (ProfesorAttendanceMarkRequest m : marcas) {
            Map<String, Object> res = new HashMap<>();
            res.put("idProfesor", m.getIdProfesor());
            Optional<Profesor> op = profesorRepository.findById(m.getIdProfesor());
            if (op.isEmpty()) {
                res.put("existio", false);
                respuestas.add(res);
                continue;
            }
            res.put("existio", true);
            Profesor p = op.get();
            boolean ya = asistenciaProfesorRepository.existsByProfesorIdProfesorAndFecha(p.getIdProfesor(), m.getFecha());
            if (ya) {
                res.put("asistioAntes", true);
                res.put("guardado", false);
            } else {
                AsistenciaProfesor a = new AsistenciaProfesor();
                a.setProfesor(p);
                a.setFecha(m.getFecha());
                asistenciaProfesorRepository.save(a);
                res.put("asistioAntes", false);
                res.put("guardado", true);
            }
            respuestas.add(res);
        }
        return respuestas;
    }

    @Transactional(readOnly = true)
    public List<ProfesorPagoDto> calcularPagoProfesores(LocalDate desde, LocalDate hasta) {
        List<Profesor> profesores = profesorRepository.findByEstado(true);
        List<ProfesorPagoDto> lista = new ArrayList<>();
        for (Profesor p : profesores) {
            List<AsistenciaProfesor> asistencias = asistenciaProfesorRepository.findByProfesorIdProfesorAndFechaBetween(p.getIdProfesor(), desde, hasta);
            int cuenta = asistencias.size();
            BigDecimal salario = p.getSalarioPorClase() == null ? BigDecimal.ZERO : p.getSalarioPorClase();
            BigDecimal total = salario.multiply(BigDecimal.valueOf(cuenta));
            ProfesorPagoDto dto = new ProfesorPagoDto();
            dto.setIdProfesor(p.getIdProfesor());
            dto.setNombre(p.getNombre());
            dto.setAsistencias(cuenta);
            dto.setSalarioPorClase(salario);
            dto.setTotalPagar(total);
            lista.add(dto);
        }
        return lista;
    }
}

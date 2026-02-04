package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.AttendanceRequest;
import galacticos_app_back.galacticos.dto.AttendanceResult;
import galacticos_app_back.galacticos.dto.StudentExistenceDto;
import galacticos_app_back.galacticos.entity.Asistencia;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.repository.AsistenciaRepository;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AsistenciaService {

    private final EstudianteRepository estudianteRepository;
    private final MembresiaRepository membresiaRepository;
    private final AsistenciaRepository asistenciaRepository;

    public AsistenciaService(EstudianteRepository estudianteRepository,
                             MembresiaRepository membresiaRepository,
                             AsistenciaRepository asistenciaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.membresiaRepository = membresiaRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional
    public List<AttendanceResult> registrarAsistencias(List<AttendanceRequest> requests) {
        List<AttendanceResult> results = new ArrayList<>();
        for (AttendanceRequest r : requests) {
            AttendanceResult res = new AttendanceResult();
            res.setIdEstudiante(r.getIdEstudiante());
            Optional<Estudiante> oe = estudianteRepository.findById(r.getIdEstudiante());
            if (oe.isEmpty()) {
                res.setExistio(false);
                res.setSaved(false);
                res.setAlreadyMarked(false);
                results.add(res);
                continue;
            }
            Estudiante e = oe.get();
            res.setExistio(true);
            res.setNombreCompleto(e.getNombreCompleto());

            // ensure membership exists
            List<Membresia> membs = membresiaRepository.findByEstudianteIdEstudiante(e.getIdEstudiante());
            if (membs == null || membs.isEmpty()) {
                Membresia m = new Membresia();
                m.setEstudiante(e);
                m.setEstado(true); // true = activo/pagado
                membresiaRepository.save(m);
                res.setMembershipStatus(m.getEstado() ? "Activo" : "Inactivo");
            } else {
                res.setMembershipStatus(membs.get(0).getEstado() ? "Activo" : "Inactivo");
            }

            LocalDate fecha = r.getFechaHora().toLocalDate();
            boolean exists = asistenciaRepository.existsByEstudianteAndFecha(e, fecha);
            if (exists) {
                res.setAlreadyMarked(true);
                res.setSaved(false);
            } else {
                Asistencia a = new Asistencia();
                a.setEstudiante(e);
                a.setFecha(fecha);
                a.setFechaHora(r.getFechaHora());
                asistenciaRepository.save(a);
                res.setAlreadyMarked(false);
                res.setSaved(true);
            }
            results.add(res);
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<Asistencia> getAsistenciasPorEstudiante(Integer idEstudiante) {
        return asistenciaRepository.findByEstudianteIdEstudianteOrderByFechaDesc(idEstudiante);
    }

    @Transactional(readOnly = true)
    public List<StudentExistenceDto> existenciasPorFecha(LocalDate fecha) {
        List<StudentExistenceDto> lista = new ArrayList<>();
        List<Estudiante> estudiantes = estudianteRepository.findByEstado(true);
        for (Estudiante e : estudiantes) {
            StudentExistenceDto dto = new StudentExistenceDto();
            dto.setIdEstudiante(e.getIdEstudiante());
            dto.setNombreCompleto(e.getNombreCompleto());
            boolean marcado = asistenciaRepository.existsByEstudianteAndFecha(e, fecha);
            dto.setMarcado(marcado);
            List<Membresia> membs = membresiaRepository.findByEstudianteIdEstudiante(e.getIdEstudiante());
            if (membs == null || membs.isEmpty()) dto.setMembershipStatus(null);
            else dto.setMembershipStatus(membs.get(0).getEstado() ? "Activo" : "Inactivo");
            lista.add(dto);
        }
        return lista;
    }
}

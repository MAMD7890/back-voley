package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.EstudianteConMembresiaDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.MembresiaCore;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaCoreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EstudiantePanelService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private MembresiaCoreRepository membresiaCoreRepository;

    /**
     * Busca estudiantes con su membresía activa, con filtros opcionales y paginación.
     *
     * @param nombre     Búsqueda parcial por nombre (case-insensitive)
     * @param documento  Búsqueda parcial por número de documento
     * @param idSede     Filtro exacto por sede
     * @param estadoPago Filtro exacto por estado de pago
     * @param estado     Filtro por estado activo/inactivo (null = todos)
     * @param pageable   Configuración de paginación y orden
     */
    @Transactional(readOnly = true)
    public Page<EstudianteConMembresiaDTO> buscarEstudiantesConMembresia(
            String nombre,
            String documento,
            Integer idSede,
            Estudiante.EstadoPago estadoPago,
            Boolean estado,
            Pageable pageable) {

        Specification<Estudiante> spec = buildSpec(nombre, documento, idSede, estadoPago, estado);
        Page<Estudiante> pagina = estudianteRepository.findAll(spec, pageable);

        return pagina.map(est -> {
            Optional<MembresiaCore> mc =
                    membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(est.getIdEstudiante());
            return EstudianteConMembresiaDTO.of(est, mc.orElse(null));
        });
    }

    /**
     * Devuelve el detalle de un estudiante junto con su membresía activa actual.
     */
    @Transactional(readOnly = true)
    public Optional<EstudianteConMembresiaDTO> obtenerEstudianteConMembresia(Integer idEstudiante) {
        return estudianteRepository.findById(idEstudiante).map(est -> {
            Optional<MembresiaCore> mc =
                    membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante);
            return EstudianteConMembresiaDTO.of(est, mc.orElse(null));
        });
    }

    /**
     * Conteos globales para el encabezado del panel.
     * @param idSede filtro opcional por sede (null = todas las sedes)
     */
    @Transactional(readOnly = true)
    public Map<String, Long> resumen(Integer idSede) {

        long total      = estudianteRepository.count(buildSpec(null, null, idSede, null,                                  null));
        long alDia      = estudianteRepository.count(buildSpec(null, null, idSede, Estudiante.EstadoPago.AL_DIA,          null));
        long pendientes = estudianteRepository.count(buildSpec(null, null, idSede, Estudiante.EstadoPago.PENDIENTE,       null));
        long enMora     = estudianteRepository.count(buildSpec(null, null, idSede, Estudiante.EstadoPago.EN_MORA,         null));
        long compromiso = estudianteRepository.count(buildSpec(null, null, idSede, Estudiante.EstadoPago.COMPROMISO_PAGO, null));
        long declinado  = estudianteRepository.count(buildSpec(null, null, idSede, Estudiante.EstadoPago.DECLINADO,       null));
        long inactivos  = estudianteRepository.count(buildSpec(null, null, idSede, null,                                  false));

        Map<String, Long> r = new java.util.LinkedHashMap<>();
        r.put("total",      total);
        r.put("alDia",      alDia);
        r.put("pendientes", pendientes);
        r.put("enMora",     enMora);
        r.put("compromiso", compromiso);
        r.put("declinado",  declinado);
        r.put("inactivos",  inactivos);
        return r;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Specification<Estudiante> buildSpec(
            String nombre,
            String documento,
            Integer idSede,
            Estudiante.EstadoPago estadoPago,
            Boolean estado) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("nombreCompleto")),
                        "%" + nombre.trim().toLowerCase() + "%"));
            }

            if (documento != null && !documento.isBlank()) {
                predicates.add(cb.like(
                        root.get("numeroDocumento"),
                        "%" + documento.trim() + "%"));
            }

            if (idSede != null) {
                predicates.add(cb.equal(root.get("sede").get("idSede"), idSede));
            }

            if (estadoPago != null) {
                predicates.add(cb.equal(root.get("estadoPago"), estadoPago));
            }

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

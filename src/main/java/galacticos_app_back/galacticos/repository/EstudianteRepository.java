package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer>, JpaSpecificationExecutor<Estudiante> {
    List<Estudiante> findByEstado(Boolean estado);
    List<Estudiante> findBySedeIdSede(Integer idSede);
    List<Estudiante> findByNivelActual(Estudiante.NivelActual nivel);
    
    // Métodos para filtrar por estado de pago
    List<Estudiante> findByEstadoPago(Estudiante.EstadoPago estadoPago);
    List<Estudiante> findByEstadoAndEstadoPago(Boolean estado, Estudiante.EstadoPago estadoPago);
    List<Estudiante> findBySedeIdSedeAndEstadoPago(Integer idSede, Estudiante.EstadoPago estadoPago);
    
    // Buscar por documento
    Optional<Estudiante> findByNumeroDocumento(String numeroDocumento);
    
    // Buscar por correo electrónico del estudiante (para vincular pagos de Wompi)
    Optional<Estudiante> findByCorreoEstudiante(String correoEstudiante);

    // Estudiantes AL_DIA pero sin membresía activa (esActiva=true) en membresia_core
    @Query("SELECT e FROM Estudiante e WHERE e.estadoPago = 'AL_DIA' AND e.estado = true " +
           "AND NOT EXISTS (SELECT m FROM MembresiaCore m WHERE m.estudiante = e AND m.esActiva = true)")
    List<Estudiante> findAlDiaSinMembresiaActiva();

    // Estudiantes AL_DIA sin membresía PAGADA vigente (fechaFin >= hoy).
    // Captura: sin membresía, con FINALIZADA, con PENDIENTE_REGISTRO, con ACUERDO_PAGO.
    @Query("SELECT e FROM Estudiante e WHERE e.estadoPago = 'AL_DIA' AND e.estado = true " +
           "AND NOT EXISTS (SELECT m FROM MembresiaCore m WHERE m.estudiante = e AND m.esActiva = true " +
           "AND m.estadoMembresia = 'PAGADA' AND m.fechaFin >= CURRENT_DATE)")
    List<Estudiante> findAlDiaSinMembresiaPagadaVigente();
}

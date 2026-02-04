package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.AsistenciaEstudianteConEstadoPagoDTO;
import galacticos_app_back.galacticos.entity.AsistenciaEstudiante;
import galacticos_app_back.galacticos.service.AsistenciaEstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/asistencia-estudiante")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AsistenciaEstudianteController {
    
    @Autowired
    private AsistenciaEstudianteService asistenciaEstudianteService;
    
    @GetMapping
    public ResponseEntity<List<AsistenciaEstudiante>> obtenerTodos() {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerTodos());
    }
    
    /**
     * Obtener todas las asistencias con estado de pago de cada estudiante
     * GET /api/asistencia-estudiante/con-estado-pago
     */
    @GetMapping("/con-estado-pago")
    public ResponseEntity<List<AsistenciaEstudianteConEstadoPagoDTO>> obtenerTodosConEstadoPago() {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerTodosConEstadoPago());
    }
    
    /**
     * Obtener asistencias de una fecha específica con estado de pago
     * GET /api/asistencia-estudiante/fecha/{fecha}/con-estado-pago
     */
    @GetMapping("/fecha/{fecha}/con-estado-pago")
    public ResponseEntity<List<AsistenciaEstudianteConEstadoPagoDTO>> obtenerPorFechaConEstadoPago(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerPorFechaConEstadoPago(fecha));
    }
    
    /**
     * Obtener asistencias de un equipo con estado de pago
     * GET /api/asistencia-estudiante/equipo/{idEquipo}/con-estado-pago
     */
    @GetMapping("/equipo/{idEquipo}/con-estado-pago")
    public ResponseEntity<List<AsistenciaEstudianteConEstadoPagoDTO>> obtenerPorEquipoConEstadoPago(
            @PathVariable Integer idEquipo) {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerPorEquipoConEstadoPago(idEquipo));
    }
    
    /**
     * Obtener asistencias de un equipo en una fecha específica con estado de pago
     * GET /api/asistencia-estudiante/equipo/{idEquipo}/fecha/{fecha}/con-estado-pago
     */
    @GetMapping("/equipo/{idEquipo}/fecha/{fecha}/con-estado-pago")
    public ResponseEntity<List<AsistenciaEstudianteConEstadoPagoDTO>> obtenerPorEquipoYFechaConEstadoPago(
            @PathVariable Integer idEquipo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerPorEquipoYFechaConEstadoPago(idEquipo, fecha));
    }
    
    /**
     * Obtener asistencias de un estudiante con estado de pago
     * GET /api/asistencia-estudiante/estudiante/{idEstudiante}/con-estado-pago
     */
    @GetMapping("/estudiante/{idEstudiante}/con-estado-pago")
    public ResponseEntity<List<AsistenciaEstudianteConEstadoPagoDTO>> obtenerPorEstudianteConEstadoPago(
            @PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(asistenciaEstudianteService.obtenerPorEstudianteConEstadoPago(idEstudiante));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AsistenciaEstudiante> obtenerPorId(@PathVariable Integer id) {
        Optional<AsistenciaEstudiante> asistencia = asistenciaEstudianteService.obtenerPorId(id);
        return asistencia.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<AsistenciaEstudiante> crear(@RequestBody AsistenciaEstudiante asistencia) {
        return ResponseEntity.ok(asistenciaEstudianteService.crear(asistencia));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaEstudiante> actualizar(@PathVariable Integer id, @RequestBody AsistenciaEstudiante asistencia) {
        AsistenciaEstudiante actualizado = asistenciaEstudianteService.actualizar(id, asistencia);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        asistenciaEstudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.asistencia.EstudianteAsistenciaDiaDTO;
import galacticos_app_back.galacticos.dto.asistencia.ReporteAsistenciaDTO;
import galacticos_app_back.galacticos.dto.asistencia.SesionAsistenciaRequestDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.service.SesionAsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
public class SesionAsistenciaController {

    @Autowired
    private SesionAsistenciaService sesionAsistenciaService;

    @Value("${app.internal.api-key:#{null}}")
    private String internalApiKey;

    // ─── Migración legado → v2 (interno) ─────────────────────────────────────


    // ─── Estudiantes con asistencia para un día ───────────────────────────────

    @GetMapping("/sesiones-asistencia/estudiantes")
    public ResponseEntity<List<EstudianteAsistenciaDiaDTO>> obtenerEstudiantesConAsistencia(
            @RequestParam Integer idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Estudiante.EstadoPago estadoPago) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerEstudiantesConAsistencia(idSede, fecha, busqueda, estadoPago));
    }

    // ─── Guardar sesión de asistencia ────────────────────────────────────────

    @PostMapping("/sesiones-asistencia")
    public ResponseEntity<?> guardarSesion(@RequestBody SesionAsistenciaRequestDTO dto) {
        try {
            Map<String, Object> resultado = sesionAsistenciaService.guardarSesion(dto);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Reporte: tarjetas del header ────────────────────────────────────────

    @GetMapping("/asistencia/reporte/stats")
    public ResponseEntity<ReporteAsistenciaDTO.Stats> obtenerStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean asistio,
            @RequestParam(required = false) Estudiante.EstadoPago estadoPago) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerStats(desde, hasta, idSede, busqueda, asistio, estadoPago));
    }

    // ─── Reporte: vista detalle (lista individual) ───────────────────────────

    @GetMapping("/asistencia/reporte/detalle")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean asistio,
            @RequestParam(required = false) Estudiante.EstadoPago estadoPago,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerDetalle(desde, hasta, idSede, busqueda, asistio, estadoPago, page, size));
    }

    // ─── Reporte: vista resumen (agrupado por estudiante) ────────────────────

    @GetMapping("/asistencia/reporte/resumen")
    public ResponseEntity<List<ReporteAsistenciaDTO.ResumenEstudiante>> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean asistio,
            @RequestParam(required = false) Estudiante.EstadoPago estadoPago) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerResumen(desde, hasta, idSede, busqueda, asistio, estadoPago));
    }

    // ─── Detalle de un estudiante específico ─────────────────────────────────

    @GetMapping("/asistencia/estudiante/{idEstudiante}")
    public ResponseEntity<?> obtenerDetalleEstudiante(@PathVariable Integer idEstudiante) {
        try {
            return ResponseEntity.ok(
                    sesionAsistenciaService.obtenerDetalleEstudiante(idEstudiante));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean validarApiKey(String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) return true;
        return internalApiKey.equals(apiKey);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "API Key inválida o ausente"));
    }
}

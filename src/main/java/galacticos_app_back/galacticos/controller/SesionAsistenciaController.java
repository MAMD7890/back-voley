package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.asistencia.ReporteAsistenciaDTO;
import galacticos_app_back.galacticos.dto.asistencia.SesionAsistenciaRequestDTO;
import galacticos_app_back.galacticos.service.SesionAsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
            @RequestParam(required = false) Boolean asistio) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerStats(desde, hasta, idSede, busqueda, asistio));
    }

    // ─── Reporte: vista detalle (lista individual) ───────────────────────────

    @GetMapping("/asistencia/reporte/detalle")
    public ResponseEntity<List<ReporteAsistenciaDTO.DetalleRegistro>> obtenerDetalle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean asistio) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerDetalle(desde, hasta, idSede, busqueda, asistio));
    }

    // ─── Reporte: vista resumen (agrupado por estudiante) ────────────────────

    @GetMapping("/asistencia/reporte/resumen")
    public ResponseEntity<List<ReporteAsistenciaDTO.ResumenEstudiante>> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean asistio) {
        return ResponseEntity.ok(
                sesionAsistenciaService.obtenerResumen(desde, hasta, idSede, busqueda, asistio));
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
}

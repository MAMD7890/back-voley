package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.EstudianteConMembresiaDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.service.EstudiantePanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Panel administrativo de estudiantes con membresías MembresiaCore.
 *
 * Endpoints:
 *   GET  /api/panel/estudiantes          — lista paginada con filtros
 *   GET  /api/panel/estudiantes/{id}     — detalle de un estudiante
 *
 * Los endpoints de edición, estado-pago, pago-efectivo, desactivar, fechas
 * permanecen en EstudianteController y MembresiaCoreController respectivamente.
 */
@RestController
@RequestMapping("/api/panel/estudiantes")
public class EstudiantePanelController {

    @Autowired
    private EstudiantePanelService estudiantePanelService;

    /**
     * Lista paginada de estudiantes con su membresía activa.
     *
     * Query params (todos opcionales):
     *   nombre     — búsqueda parcial en nombreCompleto (case-insensitive)
     *   documento  — búsqueda parcial en numeroDocumento
     *   idSede     — filtro exacto por sede
     *   estadoPago — PENDIENTE | AL_DIA | EN_MORA | COMPROMISO_PAGO | SIN_MEMBRESIA
     *   estado     — true (activos) | false (inactivos) — omitir para todos
     *   page       — número de página (default 0)
     *   size       — registros por página (default 20)
     *   sort       — campo,dirección  (ej: nombreCompleto,asc)
     *
     * Respuesta: Page<EstudianteConMembresiaDTO>
     */
    @GetMapping
    public ResponseEntity<Page<EstudianteConMembresiaDTO>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(required = false) Estudiante.EstadoPago estadoPago,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombreCompleto") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                estudiantePanelService.buscarEstudiantesConMembresia(
                        nombre, documento, idSede, estadoPago, estado, pageable));
    }

    /**
     * Detalle de un estudiante con su membresía activa.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        return estudiantePanelService.obtenerEstudianteConMembresia(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Conteos globales para el encabezado del panel.
     * Query params (todos opcionales):
     *   idSede — filtra por sede (omitir para contar todas las sedes)
     */
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Long>> resumen(
            @RequestParam(required = false) Integer idSede) {
        return ResponseEntity.ok(estudiantePanelService.resumen(idSede));
    }
}

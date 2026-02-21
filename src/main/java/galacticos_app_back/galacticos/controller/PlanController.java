package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.PlanDTO;
import galacticos_app_back.galacticos.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planes")
@CrossOrigin(origins = "*")
public class PlanController {
    
    @Autowired
    private PlanService planService;
    
    /**
     * GET /api/planes - Obtener todos los planes activos
     * Acceso: Público
     */
    @GetMapping
    public ResponseEntity<List<PlanDTO>> obtenerPlanesActivos() {
        List<PlanDTO> planes = planService.obtenerPlanesActivos();
        return ResponseEntity.ok(planes);
    }
    
    /**
     * GET /api/planes/admin/todos - Obtener todos los planes (incluidos inactivos)
     * Acceso: ADMIN y USER
     */
    @GetMapping("/admin/todos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<PlanDTO>> obtenerTodosLosPlanes() {
        List<PlanDTO> planes = planService.obtenerTodosLosPlanes();
        return ResponseEntity.ok(planes);
    }
    
    /**
     * GET /api/planes/{id} - Obtener un plan específico
     * Acceso: Público
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPlan(@PathVariable Integer id) {
        return planService.obtenerPlan(id)
                .map(plan -> ResponseEntity.ok((Object) plan))
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("error", "Plan no encontrado")));
    }
    
    /**
     * POST /api/planes - Crear un nuevo plan
     * Acceso: ADMIN y USER
     * Body: PlanDTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> crearPlan(@RequestBody PlanDTO planDTO) {
        try {
            PlanDTO planCreado = planService.crearPlan(planDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(planCreado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear el plan: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/planes/{id} - Actualizar un plan
     * Acceso: ADMIN y USER
     * Body: PlanDTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> actualizarPlan(
            @PathVariable Integer id,
            @RequestBody PlanDTO planDTO) {
        try {
            PlanDTO planActualizado = planService.actualizarPlan(id, planDTO);
            return ResponseEntity.ok(planActualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar el plan: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/planes/{id} - Eliminar un plan
     * Acceso: ADMIN y USER
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> eliminarPlan(@PathVariable Integer id) {
        try {
            planService.eliminarPlan(id);
            return ResponseEntity.ok(Map.of("mensaje", "Plan eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el plan: " + e.getMessage()));
        }
    }
    
    /**
     * PATCH /api/planes/{id}/desactivar - Desactivar un plan (soft delete)
     * Acceso: ADMIN y USER
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> desactivarPlan(@PathVariable Integer id) {
        try {
            PlanDTO planDesactivado = planService.desactivarPlan(id);
            return ResponseEntity.ok(planDesactivado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al desactivar el plan: " + e.getMessage()));
        }
    }
}

package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.ConfiguracionDTO;
import galacticos_app_back.galacticos.service.ConfiguracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionController {
    
    @Autowired
    private ConfiguracionService configuracionService;
    
    /**
     * GET /api/configuracion - Obtener todas las configuraciones
     * Acceso: ADMIN y USER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ConfiguracionDTO>> obtenerTodasLasConfiguraciones() {
        List<ConfiguracionDTO> configuraciones = configuracionService.obtenerTodasLasConfiguraciones();
        return ResponseEntity.ok(configuraciones);
    }
    
    /**
     * GET /api/configuracion/{clave} - Obtener configuración por clave
     * Acceso: Público (para ciertas configuraciones)
     */
    @GetMapping("/{clave}")
    public ResponseEntity<?> obtenerConfiguracion(@PathVariable String clave) {
        return configuracionService.obtenerConfiguracion(clave)
                .map(config -> ResponseEntity.ok((Object) config))
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("error", "Configuración no encontrada")));
    }
    
    /**
     * GET /api/configuracion/precio/matricula - Obtener precio de matrícula
     * Acceso: Público
     */
    @GetMapping("/precio/matricula")
    public ResponseEntity<?> obtenerPrecioMatricula() {
        BigDecimal precio = configuracionService.obtenerPrecioMatricula();
        return ResponseEntity.ok(Map.of(
                "clave", ConfiguracionService.PRECIO_MATRICULA,
                "valor", precio,
                "tipo", "BIGDECIMAL"
        ));
    }
    
    /**
     * POST /api/configuracion - Crear o actualizar configuración
     * Acceso: ADMIN y USER
     * Body: ConfiguracionDTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> guardarConfiguracion(@RequestBody ConfiguracionDTO configDTO) {
        try {
            ConfiguracionDTO configGuardada = configuracionService.guardarConfiguracion(configDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(configGuardada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar configuración: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/configuracion/{clave} - Actualizar configuración por clave
     * Acceso: ADMIN y USER
     * Body: ConfiguracionDTO
     */
    @PutMapping("/{clave}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> actualizarConfiguracion(
            @PathVariable String clave,
            @RequestBody ConfiguracionDTO configDTO) {
        try {
            configDTO.setClave(clave);
            ConfiguracionDTO configActualizada = configuracionService.guardarConfiguracion(configDTO);
            return ResponseEntity.ok(configActualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar configuración: " + e.getMessage()));
        }
    }
    
    /**
     * PATCH /api/configuracion/precio/matricula - Actualizar solo el precio de matrícula
     * Acceso: ADMIN y USER
     * Body: { "precio": 170000 }
     */
    @PatchMapping("/precio/matricula")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> actualizarPrecioMatricula(@RequestBody Map<String, String> body) {
        try {
            String precioStr = body.get("precio");
            if (precioStr == null || precioStr.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El campo 'precio' es requerido"));
            }
            
            BigDecimal precio = new BigDecimal(precioStr);
            ConfiguracionDTO configActualizada = configuracionService.actualizarPrecioMatricula(precio);
            return ResponseEntity.ok(configActualizada);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El precio debe ser un número válido"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar el precio: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/configuracion/{id} - Eliminar configuración
     * Acceso: ADMIN y USER
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> eliminarConfiguracion(@PathVariable Integer id) {
        try {
            configuracionService.eliminarConfiguracion(id);
            return ResponseEntity.ok(Map.of("mensaje", "Configuración eliminada correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar configuración: " + e.getMessage()));
        }
    }
}

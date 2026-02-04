package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.service.MembresiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/membresias")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MembresiaController {
    
    @Autowired
    private MembresiaService membresiaService;
    
    @GetMapping
    public ResponseEntity<List<Membresia>> obtenerTodos() {
        return ResponseEntity.ok(membresiaService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Membresia> obtenerPorId(@PathVariable Integer id) {
        Optional<Membresia> membresia = membresiaService.obtenerPorId(id);
        return membresia.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<Membresia>> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(membresiaService.obtenerPorEstudiante(idEstudiante));
    }
    
    @GetMapping("/equipo/{idEquipo}")
    public ResponseEntity<List<Membresia>> obtenerPorEquipo(@PathVariable Integer idEquipo) {
        return ResponseEntity.ok(membresiaService.obtenerPorEquipo(idEquipo));
    }
    
    @GetMapping("/vigentes/lista")
    public ResponseEntity<List<Membresia>> obtenerVigentes() {
        return ResponseEntity.ok(membresiaService.obtenerVigentes());
    }
    
    @PostMapping
    public ResponseEntity<Membresia> crear(@RequestBody Membresia membresia) {
        return ResponseEntity.ok(membresiaService.crear(membresia));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Membresia> actualizar(@PathVariable Integer id, @RequestBody Membresia membresia) {
        Membresia actualizado = membresiaService.actualizar(id, membresia);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        membresiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/actualizar-estado")
    public ResponseEntity<Membresia> actualizarEstadoMembresia(@RequestParam Integer idEstudiante, @RequestParam Boolean estado) {
        try {
            Membresia membresiaActualizada = membresiaService.actualizarEstadoMembresia(idEstudiante, estado);
            return ResponseEntity.ok(membresiaActualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        return ResponseEntity.badRequest().body("El parámetro requerido '" + name + "' no está presente en la solicitud.");
    }
}

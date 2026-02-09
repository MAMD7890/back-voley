package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.Sede;
import galacticos_app_back.galacticos.service.SedeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sedes")
public class SedeController {
    
    @Autowired
    private SedeService sedeService;
    
    @GetMapping
    public ResponseEntity<List<Sede>> obtenerTodos() {
        return ResponseEntity.ok(sedeService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Sede> obtenerPorId(@PathVariable Integer id) {
        Optional<Sede> sede = sedeService.obtenerPorId(id);
        return sede.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/activos/lista")
    public ResponseEntity<List<Sede>> obtenerActivos() {
        return ResponseEntity.ok(sedeService.obtenerActivos());
    }
    
    @PostMapping
    public ResponseEntity<Sede> crear(@RequestBody Sede sede) {
        return ResponseEntity.ok(sedeService.crear(sede));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Sede> actualizar(@PathVariable Integer id, @RequestBody Sede sede) {
        Sede actualizado = sedeService.actualizar(id, sede);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        sedeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Sede> desactivar(@PathVariable Integer id) {
        Sede desactivado = sedeService.desactivar(id);
        return desactivado != null ? ResponseEntity.ok(desactivado) : ResponseEntity.notFound().build();
    }
}

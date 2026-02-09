package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.ProfesorEquipo;
import galacticos_app_back.galacticos.service.ProfesorEquipoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profesor-equipo")
public class ProfesorEquipoController {
    
    @Autowired
    private ProfesorEquipoService profesorEquipoService;
    
    @GetMapping
    public ResponseEntity<List<ProfesorEquipo>> obtenerTodos() {
        return ResponseEntity.ok(profesorEquipoService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProfesorEquipo> obtenerPorId(@PathVariable Integer id) {
        Optional<ProfesorEquipo> profesorEquipo = profesorEquipoService.obtenerPorId(id);
        return profesorEquipo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<ProfesorEquipo> crear(@RequestBody ProfesorEquipo profesorEquipo) {
        return ResponseEntity.ok(profesorEquipoService.crear(profesorEquipo));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProfesorEquipo> actualizar(@PathVariable Integer id, @RequestBody ProfesorEquipo profesorEquipo) {
        ProfesorEquipo actualizado = profesorEquipoService.actualizar(id, profesorEquipo);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        profesorEquipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

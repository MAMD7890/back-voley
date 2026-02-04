package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.Equipo;
import galacticos_app_back.galacticos.service.EquipoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EquipoController {
    
    @Autowired
    private EquipoService equipoService;
    
    @GetMapping
    public ResponseEntity<List<Equipo>> obtenerTodos() {
        return ResponseEntity.ok(equipoService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Equipo> obtenerPorId(@PathVariable Integer id) {
        Optional<Equipo> equipo = equipoService.obtenerPorId(id);
        return equipo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/activos/lista")
    public ResponseEntity<List<Equipo>> obtenerActivos() {
        return ResponseEntity.ok(equipoService.obtenerActivos());
    }
    
    @GetMapping("/sede/{idSede}")
    public ResponseEntity<List<Equipo>> obtenerPorSede(@PathVariable Integer idSede) {
        return ResponseEntity.ok(equipoService.obtenerPorSede(idSede));
    }
    
    @GetMapping("/categoria/{idCategoria}")
    public ResponseEntity<List<Equipo>> obtenerPorCategoria(@PathVariable Integer idCategoria) {
        return ResponseEntity.ok(equipoService.obtenerPorCategoria(idCategoria));
    }
    
    @PostMapping
    public ResponseEntity<Equipo> crear(@RequestBody Equipo equipo) {
        return ResponseEntity.ok(equipoService.crear(equipo));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Equipo> actualizar(@PathVariable Integer id, @RequestBody Equipo equipo) {
        Equipo actualizado = equipoService.actualizar(id, equipo);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Equipo> desactivar(@PathVariable Integer id) {
        Equipo desactivado = equipoService.desactivar(id);
        return desactivado != null ? ResponseEntity.ok(desactivado) : ResponseEntity.notFound().build();
    }
}

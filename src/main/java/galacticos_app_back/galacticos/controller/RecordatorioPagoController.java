package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.service.RecordatorioPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recordatorios-pago")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecordatorioPagoController {
    
    @Autowired
    private RecordatorioPagoService recordatorioPagoService;
    
    @GetMapping
    public ResponseEntity<List<RecordatorioPago>> obtenerTodos() {
        return ResponseEntity.ok(recordatorioPagoService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RecordatorioPago> obtenerPorId(@PathVariable Integer id) {
        Optional<RecordatorioPago> recordatorio = recordatorioPagoService.obtenerPorId(id);
        return recordatorio.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<RecordatorioPago> crear(@RequestBody RecordatorioPago recordatorio) {
        return ResponseEntity.ok(recordatorioPagoService.crear(recordatorio));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RecordatorioPago> actualizar(@PathVariable Integer id, @RequestBody RecordatorioPago recordatorio) {
        RecordatorioPago actualizado = recordatorioPagoService.actualizar(id, recordatorio);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        recordatorioPagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

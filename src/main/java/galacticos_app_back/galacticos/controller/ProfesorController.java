package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.entity.Profesor;
import galacticos_app_back.galacticos.service.ProfesorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profesores")
public class ProfesorController {
    
    @Autowired
    private ProfesorService profesorService;
    
    @GetMapping
    public ResponseEntity<List<Profesor>> obtenerTodos() {
        return ResponseEntity.ok(profesorService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Profesor> obtenerPorId(@PathVariable Integer id) {
        Optional<Profesor> profesor = profesorService.obtenerPorId(id);
        return profesor.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/activos/lista")
    public ResponseEntity<List<Profesor>> obtenerActivos() {
        return ResponseEntity.ok(profesorService.obtenerActivos());
    }
    
    @PostMapping
    public ResponseEntity<Profesor> crear(@RequestBody Profesor profesor) {
        return ResponseEntity.ok(profesorService.crear(profesor));
    }
    
    @PostMapping("/registrar")
    public ResponseEntity<AuthResponse> registrarConUsuario(@RequestBody Profesor profesor) {
        try {
            AuthResponse response = profesorService.crearConUsuario(profesor);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @PostMapping(value = "/registrar-con-foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthResponse> registrarConUsuarioYFoto(
            @RequestParam("nombre") String nombre,
            @RequestParam("documento") String documento,
            @RequestParam(value = "telefono", required = false) String telefono,
            @RequestParam("correo") String correo,
            @RequestParam(value = "salarioPorClase", required = false) java.math.BigDecimal salarioPorClase,
            @RequestParam(value = "foto", required = false) MultipartFile foto) {
        try {
            Profesor profesor = new Profesor();
            profesor.setNombre(nombre);
            profesor.setDocumento(documento);
            profesor.setTelefono(telefono);
            profesor.setCorreo(correo);
            profesor.setSalarioPorClase(salarioPorClase);
            profesor.setEstado(true);
            
            AuthResponse response = profesorService.crearConUsuarioYFoto(profesor, foto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Profesor> actualizar(@PathVariable Integer id, @RequestBody Profesor profesor) {
        Profesor actualizado = profesorService.actualizar(id, profesor);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        profesorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Profesor> desactivar(@PathVariable Integer id) {
        Profesor desactivado = profesorService.desactivar(id);
        return desactivado != null ? ResponseEntity.ok(desactivado) : ResponseEntity.notFound().build();
    }
    
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Profesor> actualizarFoto(
            @PathVariable Integer id,
            @RequestParam("foto") MultipartFile foto) {
        try {
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            Profesor profesorActualizado = profesorService.actualizarFoto(id, foto);
            return profesorActualizado != null 
                    ? ResponseEntity.ok(profesorActualizado) 
                    : ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

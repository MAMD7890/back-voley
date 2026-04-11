package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    // ADMIN ve todos (activos e inactivos); el controlador ya exige ADMIN en clase
    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Integer id) {
        Optional<Usuario> usuario = usuarioService.obtenerPorId(id);
        return usuario.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<Usuario> obtenerPorEmail(@PathVariable String email) {
        Optional<Usuario> usuario = usuarioService.obtenerPorEmail(email);
        return usuario.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.crear(usuario));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario usuario) {
        Usuario actualizado = usuarioService.actualizar(id, usuario);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Map<String, Object>> desactivar(@PathVariable Integer id) {
        try {
            Usuario desactivado = usuarioService.desactivar(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Usuario desactivado correctamente",
                "idUsuario", desactivado.getIdUsuario(),
                "email", desactivado.getEmail(),
                "estado", desactivado.getEstado()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> obtenerActivos() {
        return usuarioRepository.findByEstado(true);
    }

    public Usuario desactivar(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        usuario.setEstado(false);
        return usuarioRepository.save(usuario);
    }
    
    public Optional<Usuario> obtenerPorId(Integer id) {
        return usuarioRepository.findById(id);
    }
    
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
    
    public Usuario crear(Usuario usuario) {
        // Encriptar password si viene en texto plano
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }
    
    public Usuario actualizar(Integer id, Usuario usuario) {
        Optional<Usuario> existente = usuarioRepository.findById(id);
        if (existente.isPresent()) {
            usuario.setIdUsuario(id);
            // Encriptar password si viene en texto plano y ha cambiado
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                // No se proporcionó contraseña nueva → conservar la existente
                usuario.setPassword(existente.get().getPassword());
            } else if (!usuario.getPassword().startsWith("$2a$")) {
                // Viene en texto plano → encriptar
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
            return usuarioRepository.save(usuario);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        usuarioRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }
}

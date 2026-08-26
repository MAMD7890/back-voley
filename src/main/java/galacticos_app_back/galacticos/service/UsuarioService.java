package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
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
    private EstudianteRepository estudianteRepository;

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
        // Si viene un idEstudiante en el body (usuario.estudiante.idEstudiante), se busca
        // el Estudiante y se usan sus datos para completar los campos que el cliente no envió.
        if (usuario.getEstudiante() != null && usuario.getEstudiante().getIdEstudiante() != null) {
            Estudiante estudiante = estudianteRepository.findById(usuario.getEstudiante().getIdEstudiante())
                    .orElseThrow(() -> new RuntimeException(
                            "Estudiante no encontrado con ID: " + usuario.getEstudiante().getIdEstudiante()));
            usuario.setEstudiante(estudiante);

            if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
                usuario.setNombre(estudiante.getNombreCompleto());
            }
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                usuario.setEmail(estudiante.getCorreoEstudiante());
            }
            if (usuario.getNumeroDocumento() == null || usuario.getNumeroDocumento().isBlank()) {
                usuario.setNumeroDocumento(estudiante.getNumeroDocumento());
            }
            if (usuario.getTipoDocumento() == null || usuario.getTipoDocumento().isBlank()) {
                usuario.setTipoDocumento(estudiante.getTipoDocumento() != null ? estudiante.getTipoDocumento().name() : null);
            }
            if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
                usuario.setTelefono(estudiante.getCelularEstudiante());
            }
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                // Password temporal por defecto: el número de documento del estudiante
                usuario.setPassword(estudiante.getNumeroDocumento());
                usuario.setRequiereChangioPassword(true);
            }
        }

        // Encriptar password si viene en texto plano
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }
    
    public Usuario actualizar(Integer id, Usuario usuario) {
        Usuario existente = usuarioRepository.findById(id).orElse(null);
        if (existente == null) {
            return null;
        }

        // Solo se sobreescriben los campos que vienen informados en el request,
        // para no borrar relaciones (p.ej. estudiante) u otros datos que el
        // cliente no haya incluido en el body.
        if (usuario.getNombre() != null) existente.setNombre(usuario.getNombre());
        if (usuario.getEmail() != null) existente.setEmail(usuario.getEmail());
        if (usuario.getFotoUrl() != null) existente.setFotoUrl(usuario.getFotoUrl());
        if (usuario.getFotoNombre() != null) existente.setFotoNombre(usuario.getFotoNombre());
        if (usuario.getTipoDocumento() != null) existente.setTipoDocumento(usuario.getTipoDocumento());
        if (usuario.getNumeroDocumento() != null) existente.setNumeroDocumento(usuario.getNumeroDocumento());
        if (usuario.getTelefono() != null) existente.setTelefono(usuario.getTelefono());
        if (usuario.getEstado() != null) existente.setEstado(usuario.getEstado());
        if (usuario.getRol() != null) existente.setRol(usuario.getRol());
        if (usuario.getUsername() != null) existente.setUsername(usuario.getUsername());
        if (usuario.getRequiereChangioPassword() != null) existente.setRequiereChangioPassword(usuario.getRequiereChangioPassword());
        if (usuario.getEstudiante() != null) existente.setEstudiante(usuario.getEstudiante());

        // Encriptar password solo si viene una nueva no vacía
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            if (usuario.getPassword().startsWith("$2a$")) {
                existente.setPassword(usuario.getPassword());
            } else {
                existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        }

        return usuarioRepository.save(existente);
    }
    
    public void eliminar(Integer id) {
        usuarioRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }
}

package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.dto.auth.RegisterRequest;
import galacticos_app_back.galacticos.entity.Profesor;
import galacticos_app_back.galacticos.repository.ProfesorRepository;
import galacticos_app_back.galacticos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

@Service
public class ProfesorService {
    
    @Autowired
    private ProfesorRepository profesorRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    public List<Profesor> obtenerTodos() {
        return profesorRepository.findAll();
    }
    
    public Optional<Profesor> obtenerPorId(Integer id) {
        return profesorRepository.findById(id);
    }
    
    public List<Profesor> obtenerActivos() {
        return profesorRepository.findByEstado(true);
    }
    
    public Profesor crear(Profesor profesor) {
        return profesorRepository.save(profesor);
    }
    
    // Crear nuevo profesor con usuario automático y foto
    @Transactional
    public AuthResponse crearConUsuarioYFoto(Profesor profesor, MultipartFile foto) {
        // Validar que el correo no esté vacío
        if (profesor.getCorreo() == null || profesor.getCorreo().isEmpty()) {
            throw new RuntimeException("El correo del profesor es obligatorio para crear el usuario");
        }
        
        // Validar que el documento no esté vacío
        if (profesor.getDocumento() == null || profesor.getDocumento().isEmpty()) {
            throw new RuntimeException("El documento del profesor es obligatorio para crear el usuario");
        }
        
        // Validar que el correo no esté registrado como usuario
        if (usuarioRepository.findByEmail(profesor.getCorreo()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario registrado con el email: " + profesor.getCorreo());
        }
        
        // Si hay foto, guardarla primero
        if (foto != null && !foto.isEmpty()) {
            FileStorageService.FileInfo fileInfo = fileStorageService.storeProfilePhoto(foto);
            profesor.setFotoUrl(fileInfo.getFileUrl());
            profesor.setFotoNombre(fileInfo.getStoredFileName());
        }
        
        // Crear el profesor
        Profesor profesorGuardado = profesorRepository.save(profesor);
        System.out.println("Profesor creado con ID: " + profesorGuardado.getIdProfesor());
        
        // Crear el registro de usuario automáticamente
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNombre(profesorGuardado.getNombre());
        registerRequest.setEmail(profesorGuardado.getCorreo());
        registerRequest.setPassword(profesorGuardado.getDocumento());
        registerRequest.setFotoUrl(profesorGuardado.getFotoUrl());
        registerRequest.setFotoNombre(profesorGuardado.getFotoNombre());
        
        // Registrar usuario con rol PROFESOR automático
        try {
            AuthResponse authResponse = authService.registerProfesor(registerRequest);
            System.out.println("Usuario profesor creado exitosamente con foto");
            
            // Construir UserInfo completo con información del profesor
            AuthResponse.UserInfo userInfoCompleto = AuthResponse.UserInfo.builder()
                .id(authResponse.getUser().getId())
                .nombre(authResponse.getUser().getNombre())
                .email(authResponse.getUser().getEmail())
                .fotoUrl(authResponse.getUser().getFotoUrl())
                .rol(authResponse.getUser().getRol())
                .build();
                
            // Construir AuthResponse completa
            return AuthResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .tokenType(authResponse.getTokenType())
                .expiresIn(authResponse.getExpiresIn())
                .user(userInfoCompleto)
                .build();
                
        } catch (Exception e) {
            System.out.println("Error creando usuario profesor: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // Crear nuevo profesor con usuario automático (sin foto)
    @Transactional
    public AuthResponse crearConUsuario(Profesor profesor) {
        // Validar que el correo no esté vacío
        if (profesor.getCorreo() == null || profesor.getCorreo().isEmpty()) {
            throw new RuntimeException("El correo del profesor es obligatorio para crear el usuario");
        }
        
        // Validar que el documento no esté vacío
        if (profesor.getDocumento() == null || profesor.getDocumento().isEmpty()) {
            throw new RuntimeException("El documento del profesor es obligatorio para crear el usuario");
        }
        
        // Validar que el correo no esté registrado como usuario
        if (usuarioRepository.findByEmail(profesor.getCorreo()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario registrado con el email: " + profesor.getCorreo());
        }
        
        // Crear el profesor
        Profesor profesorGuardado = profesorRepository.save(profesor);
        System.out.println("Profesor creado con ID: " + profesorGuardado.getIdProfesor());
        
        // Crear el registro de usuario automáticamente
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNombre(profesorGuardado.getNombre());
        registerRequest.setEmail(profesorGuardado.getCorreo());
        registerRequest.setPassword(profesorGuardado.getDocumento());
        registerRequest.setFotoUrl(profesorGuardado.getFotoUrl());
        registerRequest.setFotoNombre(profesorGuardado.getFotoNombre());
        
        // Registrar usuario con rol PROFESOR automático
        try {
            AuthResponse authResponse = authService.registerProfesor(registerRequest);
            System.out.println("Usuario profesor creado exitosamente");
            
            // Construir UserInfo completo con información del profesor
            AuthResponse.UserInfo userInfoCompleto = AuthResponse.UserInfo.builder()
                .id(authResponse.getUser().getId())
                .nombre(authResponse.getUser().getNombre())
                .email(authResponse.getUser().getEmail())
                .fotoUrl(authResponse.getUser().getFotoUrl())
                .rol(authResponse.getUser().getRol())
                .build();
                
            // Construir AuthResponse completa
            return AuthResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .tokenType(authResponse.getTokenType())
                .expiresIn(authResponse.getExpiresIn())
                .user(userInfoCompleto)
                .build();
                
        } catch (Exception e) {
            System.out.println("Error creando usuario profesor: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public Profesor actualizar(Integer id, Profesor profesor) {
        Optional<Profesor> existente = profesorRepository.findById(id);
        if (existente.isPresent()) {
            profesor.setIdProfesor(id);
            return profesorRepository.save(profesor);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        profesorRepository.deleteById(id);
    }
    
    public Profesor desactivar(Integer id) {
        Optional<Profesor> profesor = profesorRepository.findById(id);
        if (profesor.isPresent()) {
            profesor.get().setEstado(false);
            return profesorRepository.save(profesor.get());
        }
        return null;
    }
    
    // Actualizar foto del profesor
    @Transactional
    public Profesor actualizarFoto(Integer id, MultipartFile foto) {
        Optional<Profesor> profesorOpt = profesorRepository.findById(id);
        if (profesorOpt.isEmpty()) {
            return null;
        }
        
        Profesor profesor = profesorOpt.get();
        
        // Eliminar foto anterior si existe
        if (profesor.getFotoNombre() != null && !profesor.getFotoNombre().isEmpty()) {
            fileStorageService.deleteProfilePhoto(profesor.getFotoNombre());
        }
        
        // Guardar nueva foto
        FileStorageService.FileInfo fileInfo = fileStorageService.storeProfilePhoto(foto);
        profesor.setFotoUrl(fileInfo.getFileUrl());
        profesor.setFotoNombre(fileInfo.getStoredFileName());
        
        Profesor profesorGuardado = profesorRepository.save(profesor);
        
        // Actualizar también el usuario asociado para que se vea la foto al iniciar sesión
        if (profesor.getCorreo() != null && !profesor.getCorreo().isEmpty()) {
            usuarioRepository.findByEmail(profesor.getCorreo())
                .ifPresent(usuario -> {
                    usuario.setFotoUrl(fileInfo.getFileUrl());
                    usuario.setFotoNombre(fileInfo.getStoredFileName());
                    usuarioRepository.save(usuario);
                    System.out.println("Foto del usuario profesor actualizada: " + usuario.getEmail());
                });
        }
        
        return profesorGuardado;
    }
}
